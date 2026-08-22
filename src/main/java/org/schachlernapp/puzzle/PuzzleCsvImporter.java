package org.schachlernapp.puzzle;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Einmaliger Offline-Import einer Lichess-Puzzle-CSV (database.lichess.org/#puzzles)
 * in eine SQLite-Datei. Läuft NICHT beim App-Start - eigenständiges CLI-Tool.
 *
 * <p>Aufruf: {@code java -cp ... org.schachlernapp.puzzle.PuzzleCsvImporter
 * <csvPfad> <dbPfad> [--min-rating N] [--max-rating N] [--theme STRING] [--limit N]}</p>
 *
 * <p>Erwartet die CSV-Header-Spalten PuzzleId,FEN,Moves,Rating,Themes (das
 * Lichess-Original hat weitere Spalten - die werden anhand des Headers erkannt
 * und ignoriert). Liest streamend (BufferedReader, Zeile für Zeile), lädt nie
 * die komplette Datei in den Speicher - wichtig bei der ca. 5-Mio-Zeilen-Originaldatei.
 * Ein Filter (Rating-Range/Theme/Limit) wird schon beim Lesen angewendet.</p>
 */
public final class PuzzleCsvImporter {

    private static final int BATCH_SIZE = 5000;

    private PuzzleCsvImporter() {
    }

    public static void main(String[] args) throws IOException, SQLException {
        if (args.length < 2) {
            System.out.println("Aufruf: <csvPfad> <dbPfad> [--min-rating N] [--max-rating N] [--theme STRING] [--limit N]");
            return;
        }
        Path csvPath = Path.of(args[0]);
        String dbPath = args[1];
        ImportOptions options = ImportOptions.parse(args, 2);

        System.out.println("[import] CSV: " + csvPath + " -> DB: " + dbPath);
        System.out.println("[import] Filter: rating " + options.minRating() + "-" + options.maxRating()
                + (options.theme().isEmpty() ? "" : ", theme enthält \"" + options.theme() + "\"")
                + (options.limit() > 0 ? ", limit=" + options.limit() : ""));

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            PuzzleSchema.ensure(connection);
            connection.setAutoCommit(false);
            importInto(connection, csvPath, options);
            connection.commit();
        }
    }

    private static void importInto(Connection connection, Path csvPath, ImportOptions options) throws IOException, SQLException {
        String insertSql = "INSERT OR REPLACE INTO puzzles (puzzle_id, fen, moves, rating, themes) VALUES (?, ?, ?, ?, ?)";
        long read = 0;
        long imported = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
             PreparedStatement insert = connection.prepareStatement(insertSql)) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                System.out.println("[import] CSV ist leer.");
                return;
            }
            ColumnIndex columns = ColumnIndex.parse(headerLine);

            String line;
            int pendingBatch = 0;
            while ((line = reader.readLine()) != null) {
                read++;
                if (line.isBlank()) {
                    continue;
                }
                String[] fields = line.split(",", -1);
                if (fields.length <= columns.maxIndex()) {
                    continue; // defensiv: unvollständige/kaputte Zeile überspringen
                }

                int rating;
                try {
                    rating = Integer.parseInt(fields[columns.rating()].trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                String themes = fields[columns.themes()].trim();
                if (rating < options.minRating() || rating > options.maxRating()) {
                    continue;
                }
                if (!options.theme().isEmpty() && !themes.contains(options.theme())) {
                    continue;
                }

                insert.setString(1, fields[columns.puzzleId()].trim());
                insert.setString(2, fields[columns.fen()].trim());
                insert.setString(3, fields[columns.moves()].trim());
                insert.setInt(4, rating);
                insert.setString(5, themes);
                insert.addBatch();
                pendingBatch++;
                imported++;

                if (pendingBatch >= BATCH_SIZE) {
                    insert.executeBatch();
                    connection.commit();
                    pendingBatch = 0;
                    System.out.println("[import] " + imported + " Puzzles importiert (gelesen: " + read + ")...");
                }

                if (options.limit() > 0 && imported >= options.limit()) {
                    break;
                }
            }
            if (pendingBatch > 0) {
                insert.executeBatch();
            }
        }
        System.out.println("[import] Fertig: " + imported + " Puzzles importiert (gelesen: " + read + " Zeilen).");
    }

    private record ImportOptions(int minRating, int maxRating, String theme, int limit) {
        static ImportOptions parse(String[] args, int startIndex) {
            int minRating = Integer.MIN_VALUE;
            int maxRating = Integer.MAX_VALUE;
            String theme = "";
            int limit = -1;
            for (int i = startIndex; i < args.length; i++) {
                switch (args[i]) {
                    case "--min-rating" -> minRating = Integer.parseInt(args[++i]);
                    case "--max-rating" -> maxRating = Integer.parseInt(args[++i]);
                    case "--theme" -> theme = args[++i];
                    case "--limit" -> limit = Integer.parseInt(args[++i]);
                    default -> throw new IllegalArgumentException("Unbekannte Option: " + args[i]);
                }
            }
            return new ImportOptions(minRating, maxRating, theme, limit);
        }
    }

    private record ColumnIndex(int puzzleId, int fen, int moves, int rating, int themes) {
        static ColumnIndex parse(String headerLine) {
            String[] headers = headerLine.split(",", -1);
            Map<String, Integer> byName = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                byName.put(headers[i].trim(), i);
            }
            return new ColumnIndex(
                    require(byName, "PuzzleId"),
                    require(byName, "FEN"),
                    require(byName, "Moves"),
                    require(byName, "Rating"),
                    require(byName, "Themes"));
        }

        int maxIndex() {
            return Math.max(puzzleId, Math.max(fen, Math.max(moves, Math.max(rating, themes))));
        }

        private static int require(Map<String, Integer> byName, String column) {
            Integer index = byName.get(column);
            if (index == null) {
                throw new IllegalArgumentException("CSV-Header fehlt Spalte \"" + column + "\"");
            }
            return index;
        }
    }
}
