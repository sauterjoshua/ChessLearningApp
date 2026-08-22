package org.schachlernapp.puzzle;

import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import javafx.application.Platform;
import org.schachlernapp.analysis.EvaluationController;
import org.schachlernapp.ui.board.BoardController;
import org.schachlernapp.ui.board.ChangeReason;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Orchestriert ein Puzzle: lädt eine Stellung, spielt den Lichess-Setup-Zug
 * (Index 0 in {@code solutionMoves}) automatisch, vergleicht danach jeden
 * echten User-Zug ({@link ChangeReason#MOVE}) mit der Lösungssequenz.
 *
 * <p>Nutzt {@link BoardController#applyPuzzleMove} (feuert {@link ChangeReason#PUZZLE})
 * für alle programmatisch gespielten Züge - dadurch reagiert {@link #onPositionChanged}
 * nur auf echte User-Züge, ganz ohne Reentrancy-Flag. Zusätzlich wird
 * {@link EvaluationController#setBlunderFeedbackSuppressed} für die Dauer des
 * Puzzles aktiviert, damit auch der *eigene* (korrekte, aber ggf. materialopfernde)
 * Lösungszug des Users nicht fälschlich als Blunder gewertet wird.</p>
 */
public class PuzzleSession {

    private static final int DEFAULT_RATING_RANGE = 200;

    private final BoardController boardController;
    private final PuzzleRepository repository;
    private final PuzzleRatingService ratingService;
    private final EvaluationController evaluationController;
    private final int ratingRange;

    private final List<Consumer<PuzzleFeedback>> feedbackListeners = new ArrayList<>();
    private final List<Consumer<Side>> puzzleStartedListeners = new ArrayList<>();

    private Puzzle currentPuzzle;
    private int nextSolutionIndex;
    private boolean active;

    public PuzzleSession(BoardController boardController, PuzzleRepository repository,
                          EvaluationController evaluationController) {
        this(boardController, repository, evaluationController, new PuzzleRatingService(), DEFAULT_RATING_RANGE);
    }

    public PuzzleSession(BoardController boardController, PuzzleRepository repository,
                          EvaluationController evaluationController, PuzzleRatingService ratingService,
                          int ratingRange) {
        this.boardController = boardController;
        this.repository = repository;
        this.evaluationController = evaluationController;
        this.ratingService = ratingService;
        this.ratingRange = ratingRange;
        boardController.addPositionChangedListener(this::onPositionChanged);
    }

    public void addFeedbackListener(Consumer<PuzzleFeedback> listener) {
        feedbackListeners.add(listener);
    }

    /**
     * Meldet, sobald ein neues Puzzle geladen ist (nach dem automatisch gespielten
     * Setup-Zug) - Parameter ist die Seite, die jetzt lösen muss. Für die UI gedacht,
     * um das Brett passend auszurichten (lösende Seite unten).
     */
    public void addPuzzleStartedListener(Consumer<Side> listener) {
        puzzleStartedListeners.add(listener);
    }

    public int userRating() {
        return ratingService.rating();
    }

    /** Sucht adaptiv um das aktuelle User-Rating (±{@code ratingRange}) und lädt es. DB-Zugriff im Hintergrund. */
    public void loadNewPuzzleAsync() {
        PuzzleFilter filter = PuzzleFilter.aroundRating(ratingService.rating(), ratingRange);
        Thread loader = new Thread(() -> {
            Optional<Puzzle> puzzle = repository.random(filter);
            Platform.runLater(() -> {
                if (puzzle.isPresent() && puzzle.get().solutionMoves().size() >= 2) {
                    applyPuzzle(puzzle.get());
                } else {
                    active = false;
                    notifyFeedback(new PuzzleFeedback(PuzzleOutcome.NO_PUZZLE_FOUND, null, 0));
                }
            });
        }, "puzzle-loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void applyPuzzle(Puzzle puzzle) {
        currentPuzzle = puzzle;
        boardController.loadFen(puzzle.fen());
        playUciMove(puzzle.solutionMoves().get(0));
        nextSolutionIndex = 1;
        evaluationController.setBlunderFeedbackSuppressed(true);
        active = true;

        Side solverSide = boardController.sideToMove();
        for (Consumer<Side> listener : puzzleStartedListeners) {
            listener.accept(solverSide);
        }
    }

    private void onPositionChanged(ChangeReason reason) {
        if (reason != ChangeReason.MOVE || !active || currentPuzzle == null) {
            return;
        }
        Move move = boardController.lastMove();
        if (move == null) {
            return;
        }
        String playedUci = UciMoveFormat.toUci(move);
        String expectedUci = currentPuzzle.solutionMoves().get(nextSolutionIndex);

        if (!playedUci.equals(expectedUci)) {
            finishPuzzle(false);
            return;
        }

        nextSolutionIndex++;
        if (nextSolutionIndex >= currentPuzzle.solutionMoves().size()) {
            finishPuzzle(true);
            return;
        }

        // Erzwungene Gegenantwort automatisch spielen, danach ist wieder der User dran.
        playUciMove(currentPuzzle.solutionMoves().get(nextSolutionIndex));
        nextSolutionIndex++;
        notifyFeedback(new PuzzleFeedback(PuzzleOutcome.CORRECT_CONTINUE, null, 0));
    }

    private void finishPuzzle(boolean solved) {
        int delta = ratingService.recordResult(currentPuzzle.rating(), solved);
        String expectedMoveUci = solved ? null : currentPuzzle.solutionMoves().get(nextSolutionIndex);
        evaluationController.setBlunderFeedbackSuppressed(false);
        active = false;
        notifyFeedback(new PuzzleFeedback(
                solved ? PuzzleOutcome.CORRECT_SOLVED : PuzzleOutcome.INCORRECT, expectedMoveUci, delta));
    }

    private void playUciMove(String uciMove) {
        Square from = UciMoveFormat.parseSquare(uciMove, 0);
        Square to = UciMoveFormat.parseSquare(uciMove, 2);
        boardController.applyPuzzleMove(from, to);
    }

    private void notifyFeedback(PuzzleFeedback feedback) {
        for (Consumer<PuzzleFeedback> listener : feedbackListeners) {
            listener.accept(feedback);
        }
    }
}
