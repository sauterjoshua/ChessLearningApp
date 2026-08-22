package org.schachlernapp.engine;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hochwertiger Wrapper um {@link StockfishEngine} für Live-Stellungsbewertung:
 * FEN rein, {@link Evaluation} raus. Hält den Stockfish-Prozess dauerhaft am
 * Leben (einmal starten statt pro Zug neu spawnen) und serialisiert alle
 * Anfragen über einen Single-Thread-Executor, weil ein UCI-Prozess nur eine
 * "go"-Suche gleichzeitig verarbeiten kann.
 *
 * <p><b>Bekannte Einschränkung (M3, bewusst so belassen):</b> Anfragen werden
 * strikt seriell abgearbeitet. Kommen mehrere Züge schneller als
 * {@code movetimeMs}, wird eine bereits laufende Suche nicht per "stop"
 * abgebrochen - die nächste Anfrage wartet einfach in der Queue. Für eine
 * spätere Optimierung könnte man laufende Suchen bei einer neuen, sie
 * überholenden Anfrage abbrechen.</p>
 */
public class EngineEvaluator implements AutoCloseable {

    public static final int DEFAULT_MOVETIME_MS = 300;

    private static final long HANDSHAKE_TIMEOUT_MS = 5000;
    /** Sicherheitsmarge über movetimeMs hinaus, bis "bestmove" eintreffen muss. */
    private static final long BESTMOVE_GRACE_MS = 5000;

    private final StockfishEngine engine;
    private final int movetimeMs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "stockfish-evaluator");
        thread.setDaemon(true);
        return thread;
    });

    public EngineEvaluator(String enginePath) {
        this(enginePath, DEFAULT_MOVETIME_MS);
    }

    public EngineEvaluator(String enginePath, int movetimeMs) {
        this.engine = new StockfishEngine(enginePath);
        this.movetimeMs = movetimeMs;
    }

    /** Startet den Stockfish-Prozess und führt den UCI-Handshake aus. Muss vor {@link #evaluateAsync} laufen. */
    public void start() throws IOException, InterruptedException {
        engine.start();
        engine.send("uci");
        if (engine.waitFor("uciok", HANDSHAKE_TIMEOUT_MS) == null) {
            throw new IOException("Keine 'uciok'-Antwort von Stockfish erhalten.");
        }
        engine.send("isready");
        if (engine.waitFor("readyok", HANDSHAKE_TIMEOUT_MS) == null) {
            throw new IOException("Keine 'readyok'-Antwort von Stockfish erhalten.");
        }
    }

    /** Bewertet die übergebene Stellung asynchron; schlägt die Future fehl statt die UI zu blockieren. */
    public CompletableFuture<Evaluation> evaluateAsync(String fen) {
        return CompletableFuture.supplyAsync(() -> evaluateBlocking(fen), executor);
    }

    private Evaluation evaluateBlocking(String fen) {
        boolean blackToMove = isBlackToMove(fen);
        try {
            engine.send("position fen " + fen);
            engine.send("go movetime " + movetimeMs);

            UciScoreParser.ScoreInfo lastScore = null;
            String bestMove = null;
            long deadline = System.currentTimeMillis() + movetimeMs + BESTMOVE_GRACE_MS;

            while (true) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
                String line = engine.pollLine(remaining);
                if (line == null) {
                    break;
                }
                if (line.startsWith("info")) {
                    Optional<UciScoreParser.ScoreInfo> parsed = UciScoreParser.parseInfoLine(line);
                    if (parsed.isPresent()) {
                        lastScore = parsed.get();
                    }
                } else if (line.startsWith("bestmove")) {
                    bestMove = UciScoreParser.parseBestMove(line).orElse(null);
                    break;
                }
            }

            if (lastScore == null) {
                throw new EngineEvaluationException("Keine Score-Information von Stockfish für FEN: " + fen, null);
            }
            int normalizedValue = blackToMove ? -lastScore.value() : lastScore.value();
            return new Evaluation(fen, lastScore.type(), normalizedValue, lastScore.depth(), bestMove);
        } catch (IOException | InterruptedException e) {
            throw new EngineEvaluationException("Stockfish-Auswertung fehlgeschlagen für FEN: " + fen, e);
        }
    }

    private static boolean isBlackToMove(String fen) {
        String[] parts = fen.trim().split("\\s+");
        return parts.length > 1 && "b".equals(parts[1]);
    }

    @Override
    public void close() {
        executor.shutdownNow();
        engine.close();
    }
}
