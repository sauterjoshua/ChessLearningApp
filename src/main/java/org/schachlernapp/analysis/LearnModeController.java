package org.schachlernapp.analysis;

import com.github.bhlangonijr.chesslib.Constants;
import org.schachlernapp.ui.board.BoardController;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lern-Modus (M5): freies Spiel gegen sich selbst/einen Gegner am selben
 * Brett, ohne Puzzle-Vorgabe. Hört auf {@link EvaluationController#addMoveFeedbackListener},
 * hält eine laufende Session-Statistik (Anzahl gut/ungenau/Fehler/Blunder)
 * und erlaubt einen Reset auf die Startstellung.
 *
 * <p>Läuft komplett auf dem JavaFX-Application-Thread, da
 * {@link EvaluationController} sein Feedback bereits per {@code Platform.runLater}
 * dorthin liefert - Abonnenten dürfen also direkt UI-Code ausführen.</p>
 */
public class LearnModeController {

    private final BoardController boardController;
    private final List<Consumer<MoveFeedback>> feedbackListeners = new ArrayList<>();
    private final Map<MoveQuality, Integer> tally = new EnumMap<>(MoveQuality.class);

    private volatile MoveFeedback lastFeedback;

    public LearnModeController(BoardController boardController, EvaluationController evaluationController) {
        this.boardController = boardController;
        for (MoveQuality quality : MoveQuality.values()) {
            tally.put(quality, 0);
        }
        evaluationController.addMoveFeedbackListener(this::onFeedback);
    }

    public void addFeedbackListener(Consumer<MoveFeedback> listener) {
        feedbackListeners.add(listener);
    }

    public MoveFeedback lastFeedback() {
        return lastFeedback;
    }

    public int countOf(MoveQuality quality) {
        return tally.get(quality);
    }

    /**
     * Setzt die Session-Statistik auf zuvor gespeicherte Werte (M7-Persistenz).
     * Unbekannte {@link MoveQuality}-Werte werden ignoriert, fehlende bleiben bei 0 -
     * robust gegenüber einer älteren/neueren gespeicherten Version.
     */
    public void restoreTally(Map<MoveQuality, Integer> savedTally) {
        for (Map.Entry<MoveQuality, Integer> entry : savedTally.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                tally.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /** Setzt Brett und Session-Statistik auf die Startstellung zurück. */
    public void resetSession() {
        tally.replaceAll((quality, count) -> 0);
        lastFeedback = null;
        boardController.loadFen(Constants.startStandardFENPosition);
    }

    private void onFeedback(MoveFeedback feedback) {
        lastFeedback = feedback;
        tally.merge(feedback.quality(), 1, Integer::sum);
        for (Consumer<MoveFeedback> listener : feedbackListeners) {
            listener.accept(feedback);
        }
    }
}
