package org.schachlernapp.opening;

import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * JDBC-DAO für die Tabelle {@code openings} (M11 - Eröffnungstrainer). Liegt in derselben
 * SQLite-Datei wie die Puzzles ({@link org.schachlernapp.puzzle.PuzzleRepository}, Standard
 * {@code puzzles.db}), nutzt aber eine eigene {@link Connection}.
 *
 * <p>Anders als die ~5-Mio-Zeilen-Puzzle-CSV ist der ECO-Datensatz winzig (~3.800 Zeilen,
 * Quelle: github.com/lichess-org/chess-openings, Dateien {@code a.tsv}-{@code e.tsv}). Er wird
 * daher als Seed-Datei unter {@code data/openings/} mitgeliefert und beim ersten Start
 * automatisch importiert ({@link #seedIfEmpty}) - kein separates CLI-Tool wie beim Puzzle-Import.
 * Das Quell-PGN jeder Zeile wird dabei einmalig per chesslib nach UCI konvertiert und in der
 * Spalte {@code uci_moves} abgelegt.</p>
 */
public class OpeningRepository implements AutoCloseable {

    /** System-Property zum Überschreiben des DB-Pfads: -Dopenings.db.path=/pfad/zu/db */
    public static final String PATH_PROPERTY = "openings.db.path";
    /** Alternative Umgebungsvariable: OPENINGS_DB_PATH */
    public static final String PATH_ENV_VAR = "OPENINGS_DB_PATH";
    /** Fallback: dieselbe Datei wie die Puzzles. */
    public static final String DEFAULT_PATH = "puzzles.db";

    /** System-Property zum Überschreiben des Seed-Verzeichnisses: -Dopenings.tsv.dir=/pfad */
    public static final String TSV_DIR_PROPERTY = "openings.tsv.dir";
    /** Alternative Umgebungsvariable: OPENINGS_TSV_DIR */
    public static final String TSV_DIR_ENV_VAR = "OPENINGS_TSV_DIR";
    /** Fallback-Seed-Verzeichnis (relativ zum Arbeitsverzeichnis, analog zur Puzzle-CSV-Konvention). */
    public static final String DEFAULT_TSV_DIR = "data/openings";

    private final Connection connection;

    public OpeningRepository(String dbPath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        ensureSchema(connection);
        seedIfEmpty();
    }

    /** Ermittelt den DB-Pfad aus System-Property, Umgebungsvariable oder Fallback (in dieser Reihenfolge). */
    public static String resolveDefaultPath() {
        return resolve(PATH_PROPERTY, PATH_ENV_VAR, DEFAULT_PATH);
    }

    private static Path resolveTsvDir() {
        return Path.of(resolve(TSV_DIR_PROPERTY, TSV_DIR_ENV_VAR, DEFAULT_TSV_DIR));
    }

    private static String resolve(String property, String envVar, String fallback) {
        String fromProperty = System.getProperty(property);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv(envVar);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return fallback;
    }

    private static void ensureSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Die lichess-Daten enthalten denselben Namen mehrfach (Zugumstellungen / tiefere
            // Unterlinien), daher KEIN Primärschlüssel auf name - jede TSV-Zeile ist eine eigene
            // Zeile. Eine frühere Version hatte name als PK; die wird hier einmalig migriert.
            if (tableExists(statement) && !hasIdColumn(statement)) {
                statement.execute("DROP TABLE openings");
            }
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS openings (
                        id        INTEGER PRIMARY KEY AUTOINCREMENT,
                        eco       TEXT NOT NULL,
                        name      TEXT NOT NULL,
                        pgn       TEXT NOT NULL,
                        uci_moves TEXT NOT NULL
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_openings_name ON openings(name)");
        }
    }

    private static boolean tableExists(Statement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='openings'")) {
            return rs.next();
        }
    }

    private static boolean hasIdColumn(Statement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery("PRAGMA table_info(openings)")) {
            while (rs.next()) {
                if ("id".equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }

    // --- Seeding (einmalig beim ersten Start) -----------------------------------------------

    private void seedIfEmpty() {
        try {
            if (rowCount() > 0) {
                return;
            }
            Path tsvDir = resolveTsvDir();
            if (!Files.isDirectory(tsvDir)) {
                System.out.println("[openings] Seed-Verzeichnis " + tsvDir.toAbsolutePath()
                        + " nicht gefunden - Eröffnungstrainer bleibt ohne Daten.");
                return;
            }
            int imported = importFromTsvDir(tsvDir);
            System.out.println("[openings] " + imported + " Eröffnungen aus " + tsvDir + " importiert.");
        } catch (SQLException | IOException e) {
            System.out.println("[openings] FEHLER beim Seed-Import: " + e.getMessage()
                    + " - Eröffnungstrainer bleibt ggf. ohne Daten.");
        }
    }

    private int importFromTsvDir(Path tsvDir) throws SQLException, IOException {
        String insertSql = "INSERT INTO openings (eco, name, pgn, uci_moves) VALUES (?, ?, ?, ?)";
        int imported = 0;
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
            List<Path> files;
            try (Stream<Path> stream = Files.list(tsvDir)) {
                files = stream.filter(p -> p.getFileName().toString().endsWith(".tsv")).sorted().toList();
            }
            for (Path file : files) {
                imported += importTsvFile(file, insert);
            }
            insert.executeBatch();
            connection.commit();
        } catch (SQLException | IOException | RuntimeException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
        return imported;
    }

    private int importTsvFile(Path file, PreparedStatement insert) throws IOException, SQLException {
        int imported = 0;
        int skipped = 0;
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) { // Zeile 0 ist der Header "eco\tname\tpgn"
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }
            String[] cols = line.split("\t", -1);
            if (cols.length < 3) {
                skipped++;
                continue;
            }
            String eco = cols[0].trim();
            String name = cols[1].trim();
            String pgn = cols[2].trim();
            List<String> uci;
            try {
                uci = pgnToUci(pgn);
            } catch (MoveConversionException e) {
                skipped++;
                continue; // defensiv: einzelne unparsbare Zeile überspringen, Rest importieren
            }
            insert.setString(1, eco);
            insert.setString(2, name);
            insert.setString(3, pgn);
            insert.setString(4, String.join(" ", uci));
            insert.addBatch();
            imported++;
        }
        if (skipped > 0) {
            System.out.println("[openings] " + file.getFileName() + ": " + skipped + " Zeilen übersprungen.");
        }
        return imported;
    }

    /** Konvertiert das PGN einer ECO-Zeile ("1. e4 e5 2. Nf3 ...") in eine UCI-Zugliste. */
    static List<String> pgnToUci(String pgn) throws MoveConversionException {
        MoveList moveList = new MoveList();
        moveList.loadFromSan(pgn);
        List<String> uci = new ArrayList<>(moveList.size());
        for (Move move : moveList) {
            uci.add(move.toString());
        }
        return uci;
    }

    private int rowCount() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM openings")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // --- Abfragen für die zweistufige Menü-Auswahl (Eröffnungs-Familie -> Variante) ---

    /**
     * Alle Eröffnungs-Familiennamen ({@link Opening#family()}, Teil vor dem ersten {@code ":"}),
     * alphabetisch und ohne Duplikate - z.B. {@code "Sicilian Defense"}, {@code "Ruy Lopez"}, ...
     * (bei den lichess-Daten sind das ~150). Der ECO-Buchstabe spielt für die Auswahl keine Rolle
     * mehr, er ist nur noch eine Info am fertigen Eintrag.
     */
    public List<String> allFamilies() {
        Set<String> families = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT name FROM openings")) {
            while (rs.next()) {
                families.add(familyOf(rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("[OpeningRepository] allFamilies fehlgeschlagen: " + e.getMessage());
        }
        return new ArrayList<>(families);
    }

    /**
     * Alle Varianten einer Familie, sortiert nach ECO-Code und Name. Kommt derselbe Varianten-Name
     * in den Quelldaten mehrfach vor (Zugumstellungen / unterschiedlich tiefe Linien), wird pro
     * Name nur die LÄNGSTE Buchlinie behalten - die ist zum Üben am aussagekräftigsten.
     */
    public List<Opening> variationsByFamily(String family) {
        Map<String, Opening> longestByName = new LinkedHashMap<>();
        String sql = "SELECT eco, name, uci_moves FROM openings WHERE name = ? OR name LIKE ? ESCAPE '\\' ORDER BY eco, name";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, family);
            statement.setString(2, escapeLike(family) + ": %");
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Opening opening = mapRow(rs);
                    if (!opening.family().equals(family)) { // LIKE ist nur ein Vorfilter
                        continue;
                    }
                    Opening previous = longestByName.get(opening.name());
                    if (previous == null || opening.uciMoves().size() > previous.uciMoves().size()) {
                        longestByName.put(opening.name(), opening);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[OpeningRepository] variationsByFamily fehlgeschlagen: " + e.getMessage());
        }
        return new ArrayList<>(longestByName.values());
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Exakter Namens-Lookup (eine Zeile). */
    public Optional<Opening> byName(String name) {
        String sql = "SELECT eco, name, uci_moves FROM openings WHERE name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            System.err.println("[OpeningRepository] byName fehlgeschlagen: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static String familyOf(String name) {
        int colon = name.indexOf(':');
        return colon < 0 ? name : name.substring(0, colon).trim();
    }

    private static Opening mapRow(ResultSet rs) throws SQLException {
        List<String> moves = new ArrayList<>();
        String raw = rs.getString("uci_moves");
        if (raw != null && !raw.isBlank()) {
            for (String part : raw.trim().split("\\s+")) {
                moves.add(part);
            }
        }
        return new Opening(rs.getString("eco"), rs.getString("name"), moves);
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
