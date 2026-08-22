package org.schachlernapp.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Startet Stockfish als lokalen UCI-Subprozess und kapselt die Kommunikation
 * über dessen Standard-Ein-/Ausgabe.
 */
public class StockfishEngine implements AutoCloseable {

    /** System-Property zum Überschreiben des Stockfish-Pfads: -Dstockfish.path=/pfad/zu/stockfish */
    public static final String PATH_PROPERTY = "stockfish.path";
    /** Alternative Umgebungsvariable: STOCKFISH_PATH */
    public static final String PATH_ENV_VAR = "STOCKFISH_PATH";
    /** Fallback, falls weder Property noch Env-Var gesetzt sind: Binary muss über PATH auffindbar sein. */
    public static final String DEFAULT_PATH = "stockfish";

    private static final long HANDSHAKE_TIMEOUT_MS = 5000;
    private static final long BESTMOVE_TIMEOUT_MS = 5000;

    private final String enginePath;
    private Process process;
    private BufferedWriter stdin;
    private BlockingQueue<String> stdout;
    private Thread readerThread;

    public StockfishEngine(String enginePath) {
        this.enginePath = enginePath;
    }

    /** Ermittelt den Stockfish-Pfad aus System-Property, Umgebungsvariable oder Fallback (in dieser Reihenfolge). */
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

    public void start() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(enginePath);
        builder.redirectErrorStream(true);
        process = builder.start();

        stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        stdout = new LinkedBlockingQueue<>();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.put(line);
                }
            } catch (IOException | InterruptedException ignored) {
                // Prozess wurde beendet bzw. Stream geschlossen - Thread einfach beenden
            }
        }, "stockfish-stdout-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void send(String command) throws IOException {
        stdin.write(command);
        stdin.newLine();
        stdin.flush();
    }

    /**
     * Liefert die nächste rohe UCI-Ausgabezeile (oder {@code null} bei Zeitüberschreitung),
     * ohne wie {@link #waitFor(String, long)} dazwischenliegende Zeilen zu verwerfen.
     * Wird von {@link EngineEvaluator} benötigt, um "info"-Zeilen mit Eval-Scores mitzulesen.
     */
    public String pollLine(long timeoutMs) throws InterruptedException {
        return stdout.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /** Liest Ausgabezeilen, bis eine davon mit dem gesuchten Token beginnt, oder das Timeout erreicht ist. */
    public String waitFor(String token, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (true) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return null;
            }
            String line = stdout.poll(remaining, TimeUnit.MILLISECONDS);
            if (line == null) {
                return null;
            }
            if (line.startsWith(token)) {
                return line;
            }
        }
    }

    public void quit() {
        try {
            if (stdin != null) {
                send("quit");
            }
        } catch (IOException ignored) {
            // Prozess ist vermutlich schon beendet
        }
        if (process != null) {
            process.destroy();
        }
    }

    @Override
    public void close() {
        quit();
    }

    /**
     * Führt einen kompletten UCI-Testlauf durch (Start, Handshake, Startposition,
     * Suche) und protokolliert das Ergebnis auf der Konsole.
     */
    public static void runHandshakeCheck() {
        String path = resolveDefaultPath();
        System.out.println("--- Stockfish-UCI-Test ---");
        System.out.println("[stockfish] Binary-Pfad: " + path
                + " (überschreibbar via -D" + PATH_PROPERTY + "=... oder Env " + PATH_ENV_VAR + ")");

        try (StockfishEngine engine = new StockfishEngine(path)) {
            engine.start();

            engine.send("uci");
            String uciOk = engine.waitFor("uciok", HANDSHAKE_TIMEOUT_MS);
            if (uciOk == null) {
                System.out.println("[stockfish] FEHLER: keine 'uciok'-Antwort erhalten.");
                return;
            }
            System.out.println("[stockfish] uci -> uciok OK");

            engine.send("isready");
            String readyOk = engine.waitFor("readyok", HANDSHAKE_TIMEOUT_MS);
            if (readyOk == null) {
                System.out.println("[stockfish] FEHLER: keine 'readyok'-Antwort erhalten.");
                return;
            }
            System.out.println("[stockfish] isready -> readyok OK");

            engine.send("position startpos");
            engine.send("go movetime 200");
            String bestMove = engine.waitFor("bestmove", BESTMOVE_TIMEOUT_MS);
            if (bestMove == null) {
                System.out.println("[stockfish] FEHLER: keine 'bestmove'-Antwort erhalten.");
                return;
            }
            System.out.println("[stockfish] go -> " + bestMove);
            System.out.println("[stockfish] OK - UCI-Kommunikation funktioniert.");
        } catch (IOException e) {
            System.out.println("[stockfish] FEHLER: Stockfish-Binary konnte nicht gestartet werden (" + path
                    + "): " + e.getMessage());
            System.out.println("[stockfish] Pfad über -D" + PATH_PROPERTY + "=/pfad/zu/stockfish setzen oder "
                    + PATH_ENV_VAR + " exportieren.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[stockfish] Test unterbrochen.");
        }
    }
}
