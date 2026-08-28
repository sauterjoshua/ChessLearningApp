package org.schachlernapp.opening;

import com.github.bhlangonijr.chesslib.Constants;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import org.schachlernapp.analysis.EvaluationController;
import org.schachlernapp.ui.board.BoardController;
import org.schachlernapp.ui.board.ChangeReason;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Steuert eine Eröffnungstrainer-Sitzung (M11): lädt die Startstellung, spielt die Buchzüge der
 * Gegenseite automatisch ({@link BoardController#applyOpeningMove}) und vergleicht jeden echten
 * User-Zug ({@link ChangeReason#MOVE}) mit dem erwarteten Buchzug der gewählten ECO-Linie.
 *
 * <p>Gegenüber {@code PuzzleSession} bewusst schlank gehalten:</p>
 * <ul>
 *   <li>Solange die Buchlinie läuft, ist {@link EvaluationController#setBlunderFeedbackSuppressed}
 *       aktiv - das normale M3/M5-Feedback würde hier nur stören, die Abweichungs-Rückmeldung
 *       kommt vom Trainer selbst.</li>
 *   <li>Ist die Buchlinie zu Ende ({@link OpeningOutcome#BOOK_FINISHED}) - oder weicht der User
 *       ab ({@link OpeningOutcome#DEVIATION}) -, wird die Sitzung beendet und die Unterdrückung
 *       aufgehoben: ab dann übernehmen die bestehenden M3/M5-Rückmeldungen die Stellung
 *       unverändert. Einen Stockfish-Gegner gibt es bewusst nicht.</li>
 *   <li>Bei Rolle {@link OpeningRole#PLAY_AGAINST} unterscheidet sich mechanisch nichts von
 *       {@link OpeningRole#PLAY_AS} - der Trainer spielt immer die Züge der Seite, die der User
 *       nicht kontrolliert. Die Rolle wird nur im Zustand gehalten (z.B. für die UI).</li>
 * </ul>
 *
 * <p>Läuft vollständig auf dem JavaFX-Application-Thread (wie {@code PuzzleSession}: die
 * Positionsänderungs-Callbacks kommen von dort).</p>
 */
public class OpeningTrainerService {

    /** Erwarteter Buchzug (Rückgabe von {@link #getExpectedMove()}). */
    public record ExpectedMove(Square from, Square to, String uci) {
    }

    private final BoardController boardController;
    private final EvaluationController evaluationController; // darf null sein (Engine nicht verfügbar)

    private final List<Consumer<OpeningFeedback>> feedbackListeners = new ArrayList<>();
    private final List<Consumer<Side>> trainingStartedListeners = new ArrayList<>();

    private BiConsumer<Square, Square> hintArrowShow = (from, to) -> { };
    private Runnable hintArrowClear = () -> { };

    private Opening opening;
    private OpeningRole role;
    private Side userColor = Side.WHITE;
    private int moveIndex;          // Index des nächsten noch zu spielenden Buch-Halbzugs
    private boolean active;
    private boolean hintEnabled;

    public OpeningTrainerService(BoardController boardController, EvaluationController evaluationController) {
        this.boardController = boardController;
        this.evaluationController = evaluationController;
        boardController.addPositionChangedListener(this::onPositionChanged);
    }

    public void addFeedbackListener(Consumer<OpeningFeedback> listener) {
        feedbackListeners.add(listener);
    }

    /** Meldet den Start einer Sitzung mit der Farbe, die der User steuert (für die Brett-Ausrichtung). */
    public void addTrainingStartedListener(Consumer<Side> listener) {
        trainingStartedListeners.add(listener);
    }

    /** Verdrahtet den generischen Hinweis-Pfeil (in {@code Main} an {@code BoardView} gebunden). */
    public void setHintArrowHandlers(BiConsumer<Square, Square> show, Runnable clear) {
        this.hintArrowShow = show != null ? show : (from, to) -> { };
        this.hintArrowClear = clear != null ? clear : () -> { };
    }

    /**
     * Schaltet den "Zughinweis anzeigen"-Modus (Checkbox im Hauptmenü, persistiert über M7).
     * Wirkt sofort auf eine laufende Sitzung.
     */
    public void setHintEnabled(boolean hintEnabled) {
        this.hintEnabled = hintEnabled;
        if (active) {
            refreshHintArrow();
        }
    }

    public boolean isActive() {
        return active;
    }

    public OpeningRole role() {
        return role;
    }

    public Opening opening() {
        return opening;
    }

    /**
     * Startet eine neue Sitzung: Startstellung laden, Buchzüge der Gegenseite bis zum ersten
     * User-Zug automatisch spielen.
     */
    public void start(Opening opening, OpeningRole role, Side userColor) {
        stop(); // eine evtl. laufende Sitzung sauber beenden (Unterdrückung/Pfeil zurücksetzen)

        this.opening = opening;
        this.role = role;
        this.userColor = userColor;
        this.moveIndex = 0;

        boardController.loadFen(Constants.startStandardFENPosition); // feuert RESET

        if (opening == null || opening.uciMoves().isEmpty()) {
            notifyFeedback(OpeningOutcome.NO_OPENING_DATA, null);
            return;
        }

        active = true;
        setSuppressed(true);
        for (Consumer<Side> listener : trainingStartedListeners) {
            listener.accept(userColor);
        }

        playTrainerMovesUntilUserTurn();
        if (active) {
            notifyFeedback(OpeningOutcome.CORRECT_CONTINUE, null);
            refreshHintArrow();
        }
    }

    /** Beendet die aktuelle Sitzung (Unterdrückung aufheben, Pfeil entfernen) - idempotent. */
    public void stop() {
        if (active) {
            setSuppressed(false);
        }
        active = false;
        hintArrowClear.run();
    }

    /**
     * Nächster erwarteter Buchzug für den User oder {@code null}, wenn die Buchlinie zu Ende ist
     * bzw. keine Sitzung läuft. (Ist der nächste Buch-Halbzug der Trainer-Seite zuzuordnen, wird
     * er nicht zurückgegeben - der Trainer spielt ihn selbst.)
     */
    public ExpectedMove getExpectedMove() {
        if (!active || opening == null || moveIndex >= opening.uciMoves().size()) {
            return null;
        }
        if (sideToPlay(moveIndex) != userColor) {
            return null;
        }
        String uci = opening.uciMoves().get(moveIndex);
        return new ExpectedMove(square(uci, 0), square(uci, 2), uci);
    }

    private void onPositionChanged(ChangeReason reason) {
        if (reason != ChangeReason.MOVE || !active) {
            return;
        }
        Move move = boardController.lastMove();
        if (move == null) {
            return;
        }
        ExpectedMove expected = getExpectedMove();
        if (expected == null) {
            return; // sollte nach playTrainerMovesUntilUserTurn nicht vorkommen - defensiv
        }

        if (!move.toString().equals(expected.uci())) {
            // Abweichung: Buchlinie ist gebrochen. Rückmeldung geben und an M3/M5 übergeben.
            notifyFeedback(OpeningOutcome.DEVIATION, expected.uci());
            stop();
            return;
        }

        moveIndex++; // korrekter User-Zug verbraucht
        notifyFeedback(OpeningOutcome.CORRECT_CONTINUE, null);

        playTrainerMovesUntilUserTurn();
        if (active) {
            refreshHintArrow();
        }
    }

    /**
     * Spielt automatische Buchzüge der Trainer-Seite, bis wieder der User am Zug ist. Ist die
     * Buchlinie dabei erschöpft, wird die Sitzung beendet ({@link OpeningOutcome#BOOK_FINISHED}).
     */
    private void playTrainerMovesUntilUserTurn() {
        while (active && moveIndex < opening.uciMoves().size() && sideToPlay(moveIndex) != userColor) {
            String uci = opening.uciMoves().get(moveIndex);
            boardController.applyOpeningMove(square(uci, 0), square(uci, 2)); // feuert OPENING
            moveIndex++;
        }
        if (active && moveIndex >= opening.uciMoves().size()) {
            notifyFeedback(OpeningOutcome.BOOK_FINISHED, null);
            stop();
        }
    }

    private void refreshHintArrow() {
        ExpectedMove expected = hintEnabled ? getExpectedMove() : null;
        if (expected != null) {
            hintArrowShow.accept(expected.from(), expected.to());
        } else {
            hintArrowClear.run();
        }
    }

    private void setSuppressed(boolean suppressed) {
        if (evaluationController != null) {
            evaluationController.setBlunderFeedbackSuppressed(suppressed);
        }
    }

    private void notifyFeedback(OpeningOutcome outcome, String expectedUci) {
        int total = opening == null ? 0 : opening.uciMoves().size();
        OpeningFeedback feedback = new OpeningFeedback(outcome, expectedUci, Math.min(moveIndex, total), total);
        for (Consumer<OpeningFeedback> listener : feedbackListeners) {
            listener.accept(feedback);
        }
    }

    /** Seite, die den Buch-Halbzug mit Index {@code i} spielt (Buchlinie startet immer aus der Grundstellung). */
    private static Side sideToPlay(int i) {
        return i % 2 == 0 ? Side.WHITE : Side.BLACK;
    }

    private static Square square(String uci, int offset) {
        return Square.fromValue(uci.substring(offset, offset + 2).toUpperCase());
    }
}
