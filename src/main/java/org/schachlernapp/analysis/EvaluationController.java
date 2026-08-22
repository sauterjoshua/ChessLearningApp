package org.schachlernapp.analysis;

import com.github.bhlangonijr.chesslib.Side;
import javafx.application.Platform;
import org.schachlernapp.engine.Evaluation;
import org.schachlernapp.engine.EngineEvaluator;
import org.schachlernapp.ui.board.BoardController;
import org.schachlernapp.ui.board.ChangeReason;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Bindeglied zwischen {@link BoardController} (Brettzustand) und
 * {@link EngineEvaluator} (Stockfish). Hört auf Positionsänderungen, holt bei
 * jedem echten Zug ({@link ChangeReason#MOVE}) eine neue Bewertung, vergleicht
 * sie per {@link BlunderDetector} mit der vorherigen und meldet beides an
 * registrierte Listener weiter - immer auf dem JavaFX-Application-Thread, damit
 * Abonnenten (z.B. die Eval-Balken-UI) direkt UI-Code ausführen dürfen.
 *
 * <p>Bei {@link ChangeReason#RESET} (z.B. künftig Puzzle-FEN laden) wird nur
 * neu bewertet und die Baseline gesetzt, ohne Blunder-Vergleich - die alte
 * Bewertung gehört zu einer unzusammenhängenden Stellung.</p>
 *
 * <p>Bekannte Einschränkung (M3, wie in {@link EngineEvaluator} beschrieben):
 * Da Anfragen seriell verarbeitet werden, kann bei sehr schnell aufeinander
 * folgenden Zügen die "Vorher"-Bewertung noch von einem älteren Zug stammen,
 * wenn dessen Auswertung noch nicht zurück war.</p>
 *
 * <p>M5 (Lern-Modus): {@link #addMoveFeedbackListener} liefert zusätzlich eine
 * feinere {@link MoveQuality}-Einstufung samt SAN-Zugvorschlag - berechnet aus
 * demselben Vorher/Nachher-Eval-Paar wie der Blunder-Check, ohne einen
 * zusätzlichen Engine-Aufruf auszulösen.</p>
 *
 * <p><b>Performance-Hinweis (M7, nur benannt, nicht behoben):</b> {@link #onPositionChanged}
 * löst für JEDE Positionsänderung eine neue Engine-Anfrage aus - auch für
 * {@code ChangeReason.PUZZLE} (Lichess-Setup-Zug + automatisch gespielte
 * Gegenantworten). Beim Laden eines Puzzles entstehen dadurch mindestens zwei
 * sequenzielle {@code movetimeMs}-Engine-Calls, bevor der User überhaupt
 * reagieren kann. Ließe sich vermeiden, indem Puzzle-Auto-Züge übersprungen
 * werden - aktuell bewusst nicht umgesetzt, da der Eval-Balken dadurch auch
 * während des Puzzles konsistent mitläuft.</p>
 */
public class EvaluationController {

    public static final int DEFAULT_BLUNDER_THRESHOLD_CP = 150;

    private final BoardController boardController;
    private final EngineEvaluator engineEvaluator;
    private final int blunderThresholdCp;
    private final MoveQualityThresholds moveQualityThresholds;

    private final List<Consumer<Evaluation>> evaluationListeners = new ArrayList<>();
    private final List<Consumer<BlunderJudgement>> blunderListeners = new ArrayList<>();
    private final List<Consumer<MoveFeedback>> moveFeedbackListeners = new ArrayList<>();

    private volatile Evaluation lastEvaluation;
    private volatile boolean blunderFeedbackSuppressed;

    public EvaluationController(BoardController boardController, EngineEvaluator engineEvaluator) {
        this(boardController, engineEvaluator, DEFAULT_BLUNDER_THRESHOLD_CP, MoveQualityThresholds.DEFAULT);
    }

    public EvaluationController(BoardController boardController, EngineEvaluator engineEvaluator, int blunderThresholdCp) {
        this(boardController, engineEvaluator, blunderThresholdCp, MoveQualityThresholds.DEFAULT);
    }

    public EvaluationController(BoardController boardController, EngineEvaluator engineEvaluator,
                                 int blunderThresholdCp, MoveQualityThresholds moveQualityThresholds) {
        this.boardController = boardController;
        this.engineEvaluator = engineEvaluator;
        this.blunderThresholdCp = blunderThresholdCp;
        this.moveQualityThresholds = moveQualityThresholds;
        boardController.addPositionChangedListener(this::onPositionChanged);
    }

    public void addEvaluationListener(Consumer<Evaluation> listener) {
        evaluationListeners.add(listener);
    }

    public void addBlunderListener(Consumer<BlunderJudgement> listener) {
        blunderListeners.add(listener);
    }

    /** Für M5 (Lern-Modus): kategorisiertes Feedback + SAN-Zugvorschlag pro Zug. */
    public void addMoveFeedbackListener(Consumer<MoveFeedback> listener) {
        moveFeedbackListeners.add(listener);
    }

    /**
     * Für M4 (Puzzle-Modus): unterdrückt Blunder-/Lern-Feedback, solange ein Puzzle
     * aktiv ist. Der eigene Lösungszug des Users läuft weiterhin ganz normal über
     * {@code ChangeReason.MOVE} (nicht über {@code ChangeReason.PUZZLE}) und würde
     * sonst - trotz korrekter Lösung - fälschlich als Blunder gewertet werden, wenn
     * die Puzzle-Lösung ein kurzfristiges Materialopfer ist. Der Eval-Balken
     * ({@link #addEvaluationListener}) läuft unabhängig davon weiter.
     */
    public void setBlunderFeedbackSuppressed(boolean suppressed) {
        this.blunderFeedbackSuppressed = suppressed;
    }

    /** Bewertet die aktuelle Stellung einmalig - z.B. direkt nach dem Start, um die Baseline zu setzen. */
    public void evaluateCurrentPosition() {
        requestEvaluation(boardController.currentFen(), null);
    }

    private void onPositionChanged(ChangeReason reason) {
        Side moverSide = reason == ChangeReason.MOVE ? boardController.sideToMove().flip() : null;
        requestEvaluation(boardController.currentFen(), moverSide);
    }

    private void requestEvaluation(String fen, Side moverSide) {
        Evaluation before = lastEvaluation;
        engineEvaluator.evaluateAsync(fen)
                .thenAccept(after -> Platform.runLater(() -> handleResult(before, after, moverSide)))
                .exceptionally(ex -> {
                    System.err.println("[EvaluationController] Auswertung fehlgeschlagen: " + ex.getMessage());
                    return null;
                });
    }

    private void handleResult(Evaluation before, Evaluation after, Side moverSide) {
        lastEvaluation = after;
        for (Consumer<Evaluation> listener : evaluationListeners) {
            listener.accept(after);
        }
        if (moverSide != null && before != null && !blunderFeedbackSuppressed) {
            BlunderJudgement judgement = BlunderDetector.classify(before, after, moverSide, blunderThresholdCp);
            for (Consumer<BlunderJudgement> listener : blunderListeners) {
                listener.accept(judgement);
            }

            MoveFeedback feedback = MoveFeedbackFactory.create(before, after, moverSide, moveQualityThresholds);
            for (Consumer<MoveFeedback> listener : moveFeedbackListeners) {
                listener.accept(feedback);
            }
        }
    }
}
