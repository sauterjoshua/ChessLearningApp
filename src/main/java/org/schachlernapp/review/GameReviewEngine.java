package org.schachlernapp.review;

import com.github.bhlangonijr.chesslib.Side;
import org.schachlernapp.analysis.BlunderDetector;
import org.schachlernapp.analysis.MoveQuality;
import org.schachlernapp.analysis.MoveQualityClassifier;
import org.schachlernapp.analysis.MoveQualityThresholds;
import org.schachlernapp.engine.EngineEvaluationException;
import org.schachlernapp.engine.EngineEvaluator;
import org.schachlernapp.engine.Evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

/**
 * Analysiert eine importierte Partie Halbzug für Halbzug mit Stockfish - fachlich dieselbe
 * Vorher/Nachher-Logik wie {@link org.schachlernapp.analysis.EvaluationController} im Lern-Modus,
 * nur für eine fertige Zugliste statt Live-Züge vom Brett. Nutzt bewusst denselben
 * {@link EngineEvaluator} wie das laufende Spiel (kein zweiter Stockfish-Prozess) - Live-Anfragen
 * und Review-Anfragen teilen sich dessen serielle Queue.
 *
 * <p>Blockierend, da pro Halbzug eine echte Engine-Suche nötig ist (bei
 * {@link EngineEvaluator#DEFAULT_MOVETIME_MS} Millisekunden Bedenkzeit läuft ein Review über
 * viele Sekunden). Aufrufer müssen dies daher in einem eigenen Hintergrund-Thread ausführen und
 * für {@link ReviewProgressListener}/das Ergebnis selbst auf den JavaFX-Thread wechseln (analog
 * {@code PuzzleSession.loadNewPuzzleAsync}).</p>
 */
public class GameReviewEngine {

    private final EngineEvaluator engineEvaluator;
    private final MoveQualityThresholds thresholds;

    public GameReviewEngine(EngineEvaluator engineEvaluator) {
        this(engineEvaluator, MoveQualityThresholds.DEFAULT);
    }

    public GameReviewEngine(EngineEvaluator engineEvaluator, MoveQualityThresholds thresholds) {
        this.engineEvaluator = engineEvaluator;
        this.thresholds = thresholds;
    }

    /**
     * Wertet {@code game.fens()} vollständig aus und klassifiziert jeden Halbzug per
     * {@link MoveQualityClassifier}/{@link BlunderDetector} - exakt dieselben Methoden wie im
     * Lern-Modus. {@code progressListener} wird nach jedem abgeschlossenen Halbzug mit
     * (analysierte Halbzüge, Gesamtzahl Halbzüge) aufgerufen.
     */
    public GameReview review(ImportedGame game, ReviewProgressListener progressListener) {
        List<String> fens = game.fens();
        List<String> sanMoves = game.sanMoves();
        int totalHalfMoves = sanMoves.size();

        Evaluation startEvaluation = evaluate(fens.get(0));
        Evaluation previous = startEvaluation;

        List<HalfMoveReview> moves = new ArrayList<>(totalHalfMoves);
        for (int i = 0; i < totalHalfMoves; i++) {
            Evaluation after = evaluate(fens.get(i + 1));
            Side moverSide = moverSideAt(i);
            int deltaCp = BlunderDetector.deltaForMover(previous, after, moverSide);
            MoveQuality quality = MoveQualityClassifier.classify(previous, after, moverSide, thresholds);
            moves.add(new HalfMoveReview(i, sanMoves.get(i), after, quality, deltaCp));
            progressListener.onProgress(i + 1, totalHalfMoves);
            previous = after;
        }
        return new GameReview(game, startEvaluation, moves);
    }

    /** Halbzug 0 = Weiß, 1 = Schwarz, usw. - Partien starten laut M8-Scope immer von der Standard-Grundstellung. */
    private static Side moverSideAt(int halfMoveIndex) {
        return halfMoveIndex % 2 == 0 ? Side.WHITE : Side.BLACK;
    }

    private Evaluation evaluate(String fen) {
        try {
            return engineEvaluator.evaluateAsync(fen).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof EngineEvaluationException engineEvaluationException) {
                throw engineEvaluationException;
            }
            throw new EngineEvaluationException("Review-Auswertung fehlgeschlagen für FEN: " + fen, e.getCause());
        }
    }
}
