package org.schachlernapp.puzzle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC-DAO für die per {@link PuzzleCsvImporter} befüllte SQLite-Datei. */
public class PuzzleRepository implements AutoCloseable {

    /** System-Property zum Überschreiben des DB-Pfads: -Dpuzzles.db.path=/pfad/zu/puzzles.db */
    public static final String PATH_PROPERTY = "puzzles.db.path";
    /** Alternative Umgebungsvariable: PUZZLES_DB_PATH */
    public static final String PATH_ENV_VAR = "PUZZLES_DB_PATH";
    /** Fallback, falls weder Property noch Env-Var gesetzt sind. */
    public static final String DEFAULT_PATH = "puzzles.db";

    private final Connection connection;

    public PuzzleRepository(String dbPath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        PuzzleSchema.ensure(connection);
    }

    /** Ermittelt den DB-Pfad aus System-Property, Umgebungsvariable oder Fallback (in dieser Reihenfolge). */
    public static String resolveDefaultPath() {
        String fromProperty = System.getProperty(PATH_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv(PATH_ENV_VAR);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return DEFAULT_PATH;
    }

    /**
     * Liefert ein zufälliges Puzzle, das den Filter erfüllt, oder {@link Optional#empty()},
     * wenn keins passt (z.B. DB leer oder noch kein Import gemacht) - bewusst kein Fehler,
     * damit die UI nur "kein Puzzle gefunden" anzeigen muss statt abzustürzen.
     *
     * <p>Bekannte Einschränkung (wie bei M3/EngineEvaluator bewusst so belassen):
     * {@code ORDER BY RANDOM()} scannt die komplette Tabelle - für die hier vorgesehene
     * gefilterte Teilmenge (nicht die volle 5-Mio-Zeilen-Lichess-Datei) unproblematisch.</p>
     */
    public Optional<Puzzle> random(PuzzleFilter filter) {
        String themeContains = filter.themeContains() == null ? "" : filter.themeContains();
        String sql = """
                SELECT puzzle_id, fen, moves, rating, themes FROM puzzles
                WHERE rating BETWEEN ? AND ?
                  AND (? = '' OR themes LIKE '%' || ? || '%')
                ORDER BY RANDOM() LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, filter.minRating());
            statement.setInt(2, filter.maxRating());
            statement.setString(3, themeContains);
            statement.setString(4, themeContains);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[PuzzleRepository] Abfrage fehlgeschlagen: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static Puzzle mapRow(ResultSet rs) throws SQLException {
        List<String> moves = splitOnWhitespace(rs.getString("moves"));
        List<String> themes = splitOnWhitespace(rs.getString("themes"));
        return new Puzzle(rs.getString("puzzle_id"), rs.getString("fen"), moves, rs.getInt("rating"), themes);
    }

    private static List<String> splitOnWhitespace(String value) {
        List<String> result = new ArrayList<>();
        if (value != null && !value.isBlank()) {
            for (String part : value.trim().split("\\s+")) {
                result.add(part);
            }
        }
        return result;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // Verbindung ist vermutlich schon zu
        }
    }
}
