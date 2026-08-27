package org.schachlernapp;

import com.github.bhlangonijr.chesslib.Side;
import org.schachlernapp.analysis.EvaluationController;
import org.schachlernapp.analysis.LearnModeController;
import org.schachlernapp.analysis.MoveQuality;
import org.schachlernapp.chess.ChessLibCheck;
import org.schachlernapp.engine.EngineEvaluationException;
import org.schachlernapp.engine.EngineEvaluator;
import org.schachlernapp.engine.StockfishEngine;
import org.schachlernapp.progress.ProgressData;
import org.schachlernapp.progress.ProgressStore;
import org.schachlernapp.review.GameImportService;
import org.schachlernapp.review.GameReview;
import org.schachlernapp.review.GameReviewEngine;
import org.schachlernapp.review.ImportedGame;
import org.schachlernapp.ui.OptionsPanel;
import org.schachlernapp.ui.UiAlerts;
import org.schachlernapp.ui.board.BoardController;
import org.schachlernapp.ui.board.BoardView;
import org.schachlernapp.ui.board.ChangeReason;
import org.schachlernapp.ui.eval.EvalBar;
import org.schachlernapp.ui.learn.LearnModePanel;
import org.schachlernapp.ui.puzzle.PuzzlePanel;
import org.schachlernapp.ui.review.ImportGameDialog;
import org.schachlernapp.ui.review.ReviewPanel;
import org.schachlernapp.puzzle.PuzzleOutcome;
import org.schachlernapp.puzzle.PuzzleRatingService;
import org.schachlernapp.puzzle.PuzzleRepository;
import org.schachlernapp.puzzle.PuzzleSession;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Einstiegspunkt der Schach-Lernapp (Meilenstein 7: Politur &amp; Packaging).
 * Zeigt Eval-Balken + Brett + Lern-Modus-Panel + Puzzle-Panel; startet
 * Stockfish im Hintergrund als dauerhaften Analyse-Prozess. Ist Stockfish
 * nicht verfügbar, bleiben Eval-Balken/Blunder-Erkennung/Lern-Modus/Puzzle-
 * Feedback inaktiv (mit Fehlerdialog) - das Brett aus M2 funktioniert
 * unabhängig davon. Lädt/speichert Fortschritt (Puzzle-Rating, Lern-Modus-
 * Statistik) über {@link ProgressStore} - nach jedem Puzzle-Ergebnis/Lern-
 * Modus-Zug, in {@link #stop()} und zusätzlich über einen JVM-Shutdown-Hook
 * als Fallback für Fälle, in denen {@code stop()} nicht zuverlässig läuft
 * (z.B. {@code kill}, Terminal geschlossen, Prozessmanager statt normalem
 * Fenster-Schließen).
 */
public class Main extends Application {

    private final ProgressStore progressStore = new ProgressStore();

    private volatile EngineEvaluator engineEvaluator;
    private volatile PuzzleRepository puzzleRepository;
    private volatile PuzzleSession puzzleSession;
    private volatile LearnModeController learnModeController;
    private volatile ProgressData progress;

    /** Nur auf dem JavaFX-Thread gelesen/geschrieben (Partie-Auswahl + Graph-Klick laufen beide dort) - siehe M8. */
    private ImportedGame currentReviewedGame;

    @Override
    public void start(Stage primaryStage) {
        // Fallback für nicht-JavaFX-konformes Beenden (kill, Terminal zu, Prozessmanager) -
        // stop() wird dann u.U. nicht aufgerufen. saveProgress() ist null-sicher, falls noch
        // nichts geladen/konstruiert wurde. Läuft NICHT auf dem FX-Thread, siehe volatile-
        // Felder/PuzzleRatingService für die dafür nötige Sichtbarkeit über Threads hinweg.
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveProgress, "progress-shutdown-hook"));

        primaryStage.setTitle("Schach-Lernapp");

        BoardController boardController = new BoardController();
        BoardView boardView = new BoardView(boardController);
        EvalBar evalBar = new EvalBar();
        LearnModePanel learnModePanel = new LearnModePanel();
        PuzzlePanel puzzlePanel = new PuzzlePanel();
        OptionsPanel optionsPanel = new OptionsPanel();
        ReviewPanel reviewPanel = new ReviewPanel();
        GameImportService gameImportService = new GameImportService();

        // Feedback/Ratings (LearnModePanel + PuzzlePanel) in einer Leiste ÜBER dem Brett,
        // Optionen (OptionsPanel) als eigene Spalte RECHTS neben dem Brett.
        HBox.setHgrow(learnModePanel, Priority.ALWAYS);
        HBox.setHgrow(puzzlePanel, Priority.ALWAYS);
        learnModePanel.setMaxWidth(Double.MAX_VALUE);
        puzzlePanel.setMaxWidth(Double.MAX_VALUE);
        HBox feedbackRow = new HBox(8, learnModePanel, puzzlePanel);

        HBox boardRow = new HBox(evalBar, boardView, optionsPanel);
        boardRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(boardView, Priority.ALWAYS);

        // M8: Import-Dialog braucht keine Stockfish-Verbindung, daher schon hier verdrahtet (nicht erst
        // in runStartupChecks, das nur für Stockfish-/Puzzle-DB-abhängige Features gilt). Die Analyse
        // selbst (startGameReview) prüft engineEvaluator zur Auswahl-Zeit selbst und meldet sich mit
        // einem Fehlerdialog, falls Stockfish (noch) nicht verfügbar ist.
        optionsPanel.setOnImportGameRequested(() -> {
            ImportGameDialog dialog = new ImportGameDialog(gameImportService);
            dialog.showAndWait().ifPresent(reviewPanel::setGames);
        });
        reviewPanel.setOnGameSelected(game -> startGameReview(reviewPanel, game));
        reviewPanel.setOnMoveSelected(halfMoveIndex -> jumpToReviewPosition(boardController, halfMoveIndex));

        VBox root = new VBox(8, feedbackRow, boardRow, reviewPanel);
        VBox.setVgrow(boardRow, Priority.ALWAYS);

        Scene scene = new Scene(root, 1200, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(600);
        primaryStage.setMinHeight(320);
        primaryStage.show();

        // Läuft im Hintergrund, damit ein fehlender/langsamer Stockfish das UI nicht blockiert.
        Thread diagnostics = new Thread(
                () -> runStartupChecks(boardController, boardView, evalBar, learnModePanel, puzzlePanel, optionsPanel),
                "startup-diagnostics");
        diagnostics.setDaemon(true);
        diagnostics.start();
    }

    private void runStartupChecks(BoardController boardController, BoardView boardView, EvalBar evalBar,
                                   LearnModePanel learnModePanel, PuzzlePanel puzzlePanel, OptionsPanel optionsPanel) {
        System.out.println("=== Schach-Lernapp: Startdiagnose ===");
        ProgressData progress = progressStore.load();
        this.progress = progress;
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
            UiAlerts.showError("Stockfish nicht verfügbar",
                    "Die Schach-Engine (" + path + ") konnte nicht gestartet werden:\n" + e.getMessage()
                            + "\n\nEval-Balken, Blunder-Erkennung, Lern-Modus-Feedback und das Puzzle-Feature "
                            + "(braucht die Engine für die Live-Auswertung) bleiben deaktiviert. Das Brett aus M2 "
                            + "funktioniert trotzdem uneingeschränkt.");
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

        this.learnModeController = new LearnModeController(boardController, evaluationController);
        learnModeController.restoreTally(toMoveQualityMap(progress.getLearnModeTally()));
        learnModeController.addFeedbackListener(feedback -> {
            learnModePanel.showFeedback(feedback);
            learnModePanel.updateTally(
                    learnModeController.countOf(MoveQuality.GOOD),
                    learnModeController.countOf(MoveQuality.INACCURACY),
                    learnModeController.countOf(MoveQuality.MISTAKE),
                    learnModeController.countOf(MoveQuality.BLUNDER));
            saveProgress(); // nach jedem Lern-Modus-Zug, nicht erst bei stop() - siehe M7-Auftrag
        });
        optionsPanel.setOnNewGameRequested(learnModeController::resetSession);

        String puzzleDbPath = PuzzleRepository.resolveDefaultPath();
        try {
            PuzzleRepository repository = new PuzzleRepository(puzzleDbPath);
            this.puzzleRepository = repository;

            PuzzleRatingService ratingService = new PuzzleRatingService(progress.getPuzzleRating());
            this.puzzleSession = new PuzzleSession(boardController, repository, evaluationController,
                    ratingService, PuzzleSession.DEFAULT_RATING_RANGE);
            puzzleSession.addFeedbackListener(feedback -> {
                puzzlePanel.showFeedback(feedback);
                puzzlePanel.updateRating(puzzleSession.userRating());
                if (feedback.outcome() == PuzzleOutcome.CORRECT_SOLVED
                        || feedback.outcome() == PuzzleOutcome.INCORRECT) {
                    saveProgress(); // nach jedem gelösten/falschen Puzzle, nicht erst bei stop()
                }
            });
            puzzlePanel.setOnRetryRequested(puzzleSession::retryCurrentPuzzle);
            optionsPanel.setOnNewPuzzleRequested(puzzleSession::loadNewPuzzleAsync);
            puzzleSession.addPuzzleStartedListener(solverSide -> boardView.setFlipped(solverSide == Side.BLACK));
            System.out.println("[puzzle] DB geöffnet: " + puzzleDbPath
                    + " (importieren via PuzzleCsvImporter, falls noch leer)");
        } catch (Exception e) {
            System.out.println("[puzzle] FEHLER: Puzzle-DB \"" + puzzleDbPath + "\" konnte nicht geöffnet werden ("
                    + e.getMessage() + "). Puzzle-Feature bleibt deaktiviert.");
            UiAlerts.showError("Puzzle-Datenbank nicht verfügbar",
                    "Die Puzzle-Datenbank (" + puzzleDbPath + ") konnte nicht geöffnet werden:\n" + e.getMessage()
                            + "\n\nDas Puzzle-Feature bleibt deaktiviert. Brett, Eval-Balken und Lern-Modus "
                            + "funktionieren trotzdem uneingeschränkt.");
        }

        evaluationController.evaluateCurrentPosition();

        System.out.println("=== Startdiagnose abgeschlossen ===");
    }

    /**
     * Startet die Partie-Analyse (M8) für eine per {@link ImportGameDialog} importierte Partie.
     * Läuft in einem eigenen Hintergrund-Thread, da {@link GameReviewEngine#review} pro Halbzug
     * blockierend eine echte Stockfish-Suche macht (analog {@code PuzzleSession.loadNewPuzzleAsync}).
     * Nutzt bewusst denselben {@link #engineEvaluator} wie die Live-Auswertung (siehe
     * {@code GameReviewEngine}-Javadoc) - ist die Engine (noch) nicht verfügbar, wird das dem User
     * gemeldet statt die Analyse stillschweigend zu überspringen.
     */
    private void startGameReview(ReviewPanel reviewPanel, ImportedGame game) {
        currentReviewedGame = game;
        EngineEvaluator evaluator = this.engineEvaluator;
        if (evaluator == null) {
            UiAlerts.showError("Stockfish nicht verfügbar",
                    "Die Partie-Analyse benötigt die Schach-Engine, die aktuell nicht verfügbar ist.");
            return;
        }

        reviewPanel.showAnalysisStarted();
        GameReviewEngine reviewEngine = new GameReviewEngine(evaluator);
        Thread analysisThread = new Thread(() -> {
            try {
                GameReview review = reviewEngine.review(game,
                        (analyzed, total) -> Platform.runLater(() -> reviewPanel.updateProgress(analyzed, total)));
                Platform.runLater(() -> reviewPanel.showReview(review));
            } catch (EngineEvaluationException e) {
                UiAlerts.showError("Analyse fehlgeschlagen",
                        "Die Partie-Analyse ist fehlgeschlagen: " + e.getMessage());
            }
        }, "game-review-analysis");
        analysisThread.setDaemon(true);
        analysisThread.start();
    }

    /** Klick auf einen Punkt im Eval-Graph (M8): {@code -1} = Startstellung, sonst Halbzug-Index. */
    private void jumpToReviewPosition(BoardController boardController, int halfMoveIndex) {
        ImportedGame game = currentReviewedGame;
        if (game == null) {
            return;
        }
        List<String> fens = game.fens();
        int fenIndex = halfMoveIndex + 1;
        if (fenIndex >= 0 && fenIndex < fens.size()) {
            boardController.loadFen(fens.get(fenIndex), ChangeReason.REVIEW);
        }
    }

    @Override
    public void stop() {
        saveProgress(); // Normalfall - zusätzlich zum Speichern nach jedem Puzzle-Ergebnis/Lern-Modus-Zug
        if (engineEvaluator != null) {
            engineEvaluator.close();
        }
        if (puzzleRepository != null) {
            puzzleRepository.close();
        }
    }

    /**
     * Liest den aktuellen Stand aus PuzzleSession/LearnModeController (falls vorhanden) und speichert ihn.
     * Aktualisiert die beim Start geladene {@link ProgressData}-Instanz nur für Teilsysteme, die diese
     * Session tatsächlich aktiv sind - ist z.B. Stockfish diesmal nicht verfügbar (kein LearnModeController),
     * bleibt der zuletzt geladene Lern-Modus-Stand unverändert erhalten statt mit Default-Werten überschrieben
     * zu werden.
     */
    private void saveProgress() {
        ProgressData data = this.progress;
        if (data == null) {
            return; // noch nicht geladen (sollte hier nie eintreten, rein defensiv)
        }
        PuzzleSession session = this.puzzleSession;
        if (session != null) {
            data.setPuzzleRating(session.userRating());
        }
        LearnModeController controller = this.learnModeController;
        if (controller != null) {
            Map<String, Integer> tally = new HashMap<>();
            for (MoveQuality quality : MoveQuality.values()) {
                tally.put(quality.name(), controller.countOf(quality));
            }
            data.setLearnModeTally(tally);
        }
        progressStore.save(data);
    }

    /** Wandelt die gespeicherten String-Keys (MoveQuality.name()) zurück in Enum-Keys - unbekannte Keys werden ignoriert. */
    private static Map<MoveQuality, Integer> toMoveQualityMap(Map<String, Integer> saved) {
        Map<MoveQuality, Integer> result = new EnumMap<>(MoveQuality.class);
        if (saved == null) {
            return result;
        }
        for (Map.Entry<String, Integer> entry : saved.entrySet()) {
            try {
                result.put(MoveQuality.valueOf(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ignored) {
                // unbekannter/veralteter Key in einer alten progress.json - einfach überspringen
            }
        }
        return result;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
