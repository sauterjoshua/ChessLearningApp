package org.schachlernapp;

import com.github.bhlangonijr.chesslib.Side;
import org.schachlernapp.analysis.EvaluationController;
import org.schachlernapp.analysis.LearnModeController;
import org.schachlernapp.analysis.MoveQuality;
import org.schachlernapp.chess.ChessLibCheck;
import org.schachlernapp.engine.EngineEvaluator;
import org.schachlernapp.engine.StockfishEngine;
import org.schachlernapp.ui.board.BoardController;
import org.schachlernapp.ui.board.BoardView;
import org.schachlernapp.ui.eval.EvalBar;
import org.schachlernapp.ui.learn.LearnModePanel;
import org.schachlernapp.ui.puzzle.PuzzlePanel;
import org.schachlernapp.puzzle.PuzzleRepository;
import org.schachlernapp.puzzle.PuzzleSession;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

/**
 * Einstiegspunkt der Schach-Lernapp (Meilenstein 4: Puzzle-Feature).
 * Zeigt Eval-Balken + Brett + Lern-Modus-Panel + Puzzle-Panel; startet
 * Stockfish im Hintergrund als dauerhaften Analyse-Prozess. Ist Stockfish
 * nicht verfügbar, bleiben Eval-Balken/Blunder-Erkennung/Lern-Modus/Puzzle-
 * Feedback inaktiv - das Brett aus M2 funktioniert unabhängig davon.
 */
public class Main extends Application {

    private volatile EngineEvaluator engineEvaluator;
    private volatile PuzzleRepository puzzleRepository;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Schach-Lernapp");

        BoardController boardController = new BoardController();
        BoardView boardView = new BoardView(boardController);
        EvalBar evalBar = new EvalBar();
        LearnModePanel learnModePanel = new LearnModePanel();
        PuzzlePanel puzzlePanel = new PuzzlePanel();

        HBox root = new HBox(evalBar, boardView, learnModePanel, puzzlePanel);
        root.setAlignment(Pos.CENTER);
        HBox.setHgrow(boardView, Priority.ALWAYS);

        primaryStage.setScene(new Scene(root, 1200, 600));
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(320);
        primaryStage.show();

        // Läuft im Hintergrund, damit ein fehlender/langsamer Stockfish das UI nicht blockiert.
        Thread diagnostics = new Thread(
                () -> runStartupChecks(boardController, boardView, evalBar, learnModePanel, puzzlePanel),
                "startup-diagnostics");
        diagnostics.setDaemon(true);
        diagnostics.start();
    }

    private void runStartupChecks(BoardController boardController, BoardView boardView, EvalBar evalBar,
                                   LearnModePanel learnModePanel, PuzzlePanel puzzlePanel) {
        System.out.println("=== Schach-Lernapp: Startdiagnose ===");
        ChessLibCheck.run();

        String path = StockfishEngine.resolveDefaultPath();
        System.out.println("[stockfish] Binary-Pfad: " + path
                + " (überschreibbar via -D" + StockfishEngine.PATH_PROPERTY + "=... oder Env " + StockfishEngine.PATH_ENV_VAR + ")");

        EngineEvaluator evaluator = new EngineEvaluator(path);
        try {
            evaluator.start();
        } catch (Exception e) {
            System.out.println("[stockfish] FEHLER: Engine konnte nicht gestartet werden (" + e.getMessage()
                    + "). Eval-Balken/Blunder-Erkennung bleiben deaktiviert.");
            System.out.println("=== Startdiagnose abgeschlossen ===");
            return;
        }
        System.out.println("[stockfish] UCI-Handshake OK - Engine bereit für Live-Auswertung.");
        this.engineEvaluator = evaluator;

        EvaluationController evaluationController = new EvaluationController(boardController, evaluator);
        evaluationController.addEvaluationListener(evalBar::setEvaluation);
        evaluationController.addBlunderListener(judgement -> {
            if (judgement.isBlunder()) {
                System.out.println("[analysis] Blunder erkannt: " + judgement.deltaCp() + "cp Verschlechterung.");
            }
        });

        LearnModeController learnModeController = new LearnModeController(boardController, evaluationController);
        learnModeController.addFeedbackListener(feedback -> {
            learnModePanel.showFeedback(feedback);
            learnModePanel.updateTally(
                    learnModeController.countOf(MoveQuality.GOOD),
                    learnModeController.countOf(MoveQuality.INACCURACY),
                    learnModeController.countOf(MoveQuality.MISTAKE),
                    learnModeController.countOf(MoveQuality.BLUNDER));
        });
        learnModePanel.setOnResetRequested(learnModeController::resetSession);

        String puzzleDbPath = PuzzleRepository.resolveDefaultPath();
        try {
            PuzzleRepository repository = new PuzzleRepository(puzzleDbPath);
            this.puzzleRepository = repository;

            PuzzleSession puzzleSession = new PuzzleSession(boardController, repository, evaluationController);
            puzzleSession.addFeedbackListener(feedback -> {
                puzzlePanel.showFeedback(feedback);
                puzzlePanel.updateRating(puzzleSession.userRating());
            });
            puzzlePanel.setOnNextPuzzleRequested(puzzleSession::loadNewPuzzleAsync);
            puzzleSession.addPuzzleStartedListener(solverSide -> boardView.setFlipped(solverSide == Side.BLACK));
            System.out.println("[puzzle] DB geöffnet: " + puzzleDbPath
                    + " (importieren via PuzzleCsvImporter, falls noch leer)");
        } catch (Exception e) {
            System.out.println("[puzzle] FEHLER: Puzzle-DB \"" + puzzleDbPath + "\" konnte nicht geöffnet werden ("
                    + e.getMessage() + "). Puzzle-Feature bleibt deaktiviert.");
        }

        evaluationController.evaluateCurrentPosition();

        System.out.println("=== Startdiagnose abgeschlossen ===");
    }

    @Override
    public void stop() {
        if (engineEvaluator != null) {
            engineEvaluator.close();
        }
        if (puzzleRepository != null) {
            puzzleRepository.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
