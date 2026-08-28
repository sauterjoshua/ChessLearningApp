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
import org.schachlernapp.opening.Opening;
import org.schachlernapp.opening.OpeningFeedback;
import org.schachlernapp.opening.OpeningOutcome;
import org.schachlernapp.opening.OpeningRepository;
import org.schachlernapp.opening.OpeningRole;
import org.schachlernapp.opening.OpeningTrainerService;
import org.schachlernapp.puzzle.EndgameTheme;
import org.schachlernapp.review.GameImportService;
import org.schachlernapp.review.GameReview;
import org.schachlernapp.review.GameReviewEngine;
import org.schachlernapp.review.HalfMoveReview;
import org.schachlernapp.review.ImportedGame;
import org.schachlernapp.ui.AppView;
import org.schachlernapp.ui.EndgameMenuView;
import org.schachlernapp.ui.MainMenuView;
import org.schachlernapp.ui.OpeningMenuView;
import org.schachlernapp.ui.UiAlerts;
import org.schachlernapp.ui.opening.OpeningPanel;
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
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
 *
 * <p><b>Startmenü-Navigation (M10):</b> {@link #switchTo(AppView)} tauscht den Inhalt des
 * Wurzel-{@code StackPane} zwischen {@link MainMenuView}, {@link EndgameMenuView} und der
 * Spielansicht ({@link #buildGameView()}) aus. Brett/Engine-Anbindung (Board-/Eval-/Lern-
 * Modus-/Puzzle-/Review-Komponenten) werden bewusst nur EINMAL in {@link #start} erzeugt und
 * als Felder gehalten statt bei jedem Wechsel zu {@link AppView#GAME} neu - sie sind über
 * {@link #runStartupChecks} fest an eine bestimmte {@link BoardController}-Instanz verdrahtet;
 * {@link #buildGameView()} baut bei jedem Aufruf nur das Layout (die Container-Nodes) neu aus
 * diesen bestehenden Instanzen zusammen. Aus demselben Grund lesen die Menü-Aktionen
 * ({@link #handleNewGameRequested()} etc.) die zugehörigen, ggf. erst asynchron nach dem
 * Stockfish-Start gesetzten Felder ({@link #learnModeController}/{@link #puzzleSession}) bei
 * jedem Klick frisch aus - eine feste Bindung wie zuvor bei {@code OptionsPanel} wäre hier
 * hinfällig, da {@link MainMenuView}/{@link EndgameMenuView} bei jedem Menü-Besuch neu erzeugt
 * werden.</p>
 */
public class Main extends Application {

    private final ProgressStore progressStore = new ProgressStore();

    private volatile EngineEvaluator engineEvaluator;
    private volatile PuzzleRepository puzzleRepository;
    private volatile PuzzleSession puzzleSession;
    private volatile OpeningRepository openingRepository;
    private volatile OpeningTrainerService openingTrainerService;
    private volatile LearnModeController learnModeController;
    private volatile ProgressData progress;

    private StackPane root;
    private BoardController boardController;
    private BoardView boardView;
    private EvalBar evalBar;
    private LearnModePanel learnModePanel;
    private PuzzlePanel puzzlePanel;
    private OpeningPanel openingPanel;
    private Button openingNextButton;
    private ReviewPanel reviewPanel;
    private GameImportService gameImportService;

    // M11: "Playlist" der Varianten der aktuell gewählten Eröffnungs-Familie, für den
    // "Weiter zur nächsten Variante"-Button nach durchgespielter Buchlinie.
    private List<Opening> openingPlaylist = List.of();
    private int openingPlaylistIndex = -1;
    private OpeningRole currentOpeningRole = OpeningRole.PLAY_AS;
    private Side currentOpeningColor = Side.WHITE;

    @Override
    public void start(Stage primaryStage) {
        // Fallback für nicht-JavaFX-konformes Beenden (kill, Terminal zu, Prozessmanager) -
        // stop() wird dann u.U. nicht aufgerufen. saveProgress() ist null-sicher, falls noch
        // nichts geladen/konstruiert wurde. Läuft NICHT auf dem FX-Thread, siehe volatile-
        // Felder/PuzzleRatingService für die dafür nötige Sichtbarkeit über Threads hinweg.
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveProgress, "progress-shutdown-hook"));

        primaryStage.setTitle("Schach-Lernapp");

        boardController = new BoardController();
        boardView = new BoardView(boardController);
        boardView.getStyleClass().add("board-pane");
        evalBar = new EvalBar();
        evalBar.getStyleClass().add("eval-pane");
        learnModePanel = new LearnModePanel();
        puzzlePanel = new PuzzlePanel();
        openingPanel = new OpeningPanel();
        openingPanel.setOnHintToggle(this::handleHintToggle);
        openingPanel.setOnRetryRequested(this::handleOpeningRetryRequested);
        openingNextButton = new Button();
        openingNextButton.setVisible(false);
        openingNextButton.setManaged(false);
        openingNextButton.setOnAction(e -> handleOpeningNextRequested());
        reviewPanel = new ReviewPanel();
        gameImportService = new GameImportService();

        // M8: Import-Dialog braucht keine Stockfish-Verbindung, daher schon hier verdrahtet (nicht erst
        // in runStartupChecks, das nur für Stockfish-/Puzzle-DB-abhängige Features gilt). Die Analyse
        // selbst (startGameReview) prüft engineEvaluator zur Auswahl-Zeit selbst und meldet sich mit
        // einem Fehlerdialog, falls Stockfish (noch) nicht verfügbar ist.
        reviewPanel.setOnGameSelected(game -> startGameReview(reviewPanel, learnModePanel, game));
        reviewPanel.setOnPositionSelected(fen -> boardController.loadFen(fen, ChangeReason.REVIEW));

        root = new StackPane();
        switchTo(AppView.MAIN_MENU);

        // Breite um die neue Zugliste-Spalte (M9-Redesign, ~160px + Spacing) erweitert, damit das
        // Brett nicht kleiner startet als vor der Umstrukturierung.
        Scene scene = new Scene(root, 1360, 700);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        // Dark Theme (M9-Redesign): zweites Stylesheet, NACH style.css geladen, damit seine
        // Farb-Deklarationen dessen (nur strukturell gedachte) Farben überschreiben.
        scene.getStylesheets().add(getClass().getResource("/dark-theme.css").toExternalForm());
        wireReviewArrowKeyNavigation(scene, reviewPanel);
        primaryStage.setScene(scene);
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

    /**
     * Tauscht den Inhalt von {@link #root} auf die gewünschte {@link AppView}. {@link MainMenuView}/
     * {@link EndgameMenuView} werden dabei bewusst jedes Mal neu erzeugt (kein UI-Zustand zu erhalten),
     * {@link #buildGameView()} baut sein Layout dagegen aus den dauerhaft gehaltenen Feldern (siehe
     * Klassen-Javadoc).
     */
    private void switchTo(AppView view) {
        Node content = switch (view) {
            case MAIN_MENU -> new MainMenuView(
                    this::handleNewGameRequested,
                    this::handleNewPuzzleRequested,
                    () -> switchTo(AppView.ENDGAME_SELECT),
                    this::handleOpeningRequested,
                    this::handleImportGameRequested,
                    Platform::exit);
            case ENDGAME_SELECT -> new EndgameMenuView(
                    this::handleEndgameThemeSelected,
                    () -> switchTo(AppView.MAIN_MENU));
            case OPENING_SELECT -> new OpeningMenuView(
                    openingRepository,
                    this::handleOpeningSelected,
                    () -> switchTo(AppView.MAIN_MENU));
            case GAME -> buildGameView();
        };
        root.getChildren().setAll(content);
    }

    /** Wie bisher {@code OptionsPanel.setOnNewGameRequested(learnModeController::resetSession)}. */
    private void handleNewGameRequested() {
        LearnModeController controller = learnModeController;
        if (controller != null) {
            controller.resetSession();
        }
        switchTo(AppView.GAME);
    }

    /** Wie bisher {@code OptionsPanel.setOnNewPuzzleRequested(puzzleSession::loadNewPuzzleAsync)}. */
    private void handleNewPuzzleRequested() {
        PuzzleSession session = puzzleSession;
        if (session != null) {
            session.loadNewPuzzleAsync();
        }
        switchTo(AppView.GAME);
    }

    /** Wie bisher {@code OptionsPanel.setOnEndgameThemeSelected(puzzleSession::loadEndgamePuzzleAsync)}. */
    private void handleEndgameThemeSelected(EndgameTheme theme) {
        PuzzleSession session = puzzleSession;
        if (session != null) {
            session.loadEndgamePuzzleAsync(theme);
        }
        switchTo(AppView.GAME);
    }

    /** M11: öffnet die Eröffnungs-Auswahl - nur wenn die {@code openings}-DB verfügbar ist. */
    private void handleOpeningRequested() {
        if (openingRepository == null) {
            UiAlerts.showError("Eröffnungstrainer nicht verfügbar",
                    "Die Eröffnungsdaten konnten nicht geladen werden. Der Eröffnungstrainer "
                            + "steht in dieser Sitzung nicht zur Verfügung.");
            return;
        }
        switchTo(AppView.OPENING_SELECT);
    }

    /**
     * M11: startet eine Eröffnungstrainer-Sitzung mit der im {@link OpeningMenuView} getroffenen
     * Auswahl. Merkt sich die komplette Varianten-Liste der Familie (+ Rolle/Farbe) für den
     * "Weiter zur nächsten Variante"-Button.
     */
    private void handleOpeningSelected(String family, Opening opening, OpeningRole role, Side userColor) {
        List<Opening> variations = openingRepository != null
                ? openingRepository.variationsByFamily(family) : List.of();
        int index = indexOfVariation(variations, opening);
        if (index < 0) { // Auswahl nicht in der frisch geladenen Liste - dann eben Playlist der Länge 1
            variations = List.of(opening);
            index = 0;
        }
        this.openingPlaylist = variations;
        this.openingPlaylistIndex = index;
        this.currentOpeningRole = role;
        this.currentOpeningColor = userColor;

        startOpeningVariation(opening);
        switchTo(AppView.GAME);
    }

    /** M11: "Weiter"-Button nach durchgespielter Buchlinie - startet die nächste Variante derselben Eröffnung. */
    private void handleOpeningNextRequested() {
        if (!hasNextVariation()) {
            return;
        }
        openingPlaylistIndex++;
        startOpeningVariation(openingPlaylist.get(openingPlaylistIndex));
    }

    private void startOpeningVariation(Opening opening) {
        openingNextButton.setVisible(false);
        openingNextButton.setManaged(false);
        OpeningTrainerService trainer = openingTrainerService;
        if (trainer != null) {
            trainer.start(opening, currentOpeningRole, currentOpeningColor);
        }
        openingPanel.showOpening(opening);
    }

    /**
     * M11: reagiert auf Trainer-Feedback für die "Weiter"-Navigation. Bei durchgespielter Buchlinie
     * ({@link OpeningOutcome#BOOK_FINISHED}) wird - falls es eine nächste Variante gibt - der
     * "Weiter"-Button mit deren Namen eingeblendet, sonst wieder versteckt.
     */
    private void handleOpeningFeedback(OpeningFeedback feedback) {
        if (feedback.outcome() == OpeningOutcome.BOOK_FINISHED && hasNextVariation()) {
            Opening next = openingPlaylist.get(openingPlaylistIndex + 1);
            openingNextButton.setText("Weiter ▶  " + next.variation());
            openingNextButton.setVisible(true);
            openingNextButton.setManaged(true);
            openingPanel.showNextVariation(next);
        } else {
            openingNextButton.setVisible(false);
            openingNextButton.setManaged(false);
            if (feedback.outcome() == OpeningOutcome.BOOK_FINISHED) {
                openingPanel.showNextVariation(null); // letzte Variante der Eröffnung
            }
        }
    }

    /** M11: "Nochmal versuchen" nach einem Fehlzug - nimmt den falschen Zug im Trainer zurück. */
    private void handleOpeningRetryRequested() {
        OpeningTrainerService trainer = openingTrainerService;
        if (trainer != null) {
            trainer.retryAfterDeviation();
        }
    }

    private boolean hasNextVariation() {
        return openingPlaylistIndex >= 0 && openingPlaylistIndex + 1 < openingPlaylist.size();
    }

    private static int indexOfVariation(List<Opening> variations, Opening target) {
        for (int i = 0; i < variations.size(); i++) {
            if (variations.get(i).name().equals(target.name())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * M11: "Zughinweis anzeigen"-Checkbox (in {@link OpeningPanel} beim Brett). Persistiert den Wert
     * (M7) und reicht ihn an eine laufende Eröffnungstrainer-Sitzung durch.
     */
    private void handleHintToggle(boolean enabled) {
        ProgressData data = this.progress;
        if (data != null) {
            data.setShowHintArrow(enabled);
        }
        OpeningTrainerService trainer = openingTrainerService;
        if (trainer != null) {
            trainer.setHintEnabled(enabled);
        }
        saveProgress();
    }

    /** Wie bisher der Import-Handler in {@code OptionsPanel}; öffnet zusätzlich die Spielansicht. */
    private void handleImportGameRequested() {
        ImportGameDialog dialog = new ImportGameDialog(gameImportService);
        dialog.showAndWait().ifPresent(games -> {
            reviewPanel.setGames(games);
            switchTo(AppView.GAME);
        });
    }

    /**
     * Baut die Spielansicht (Feedback-Zeile + Brett-Zeile + Review-Leiste, wie vor M10 der gesamte
     * Fensterinhalt) aus den in {@link #start} einmalig erzeugten Feldern neu zusammen, plus einem
     * "Zurück"-Button unten rechts als Overlay zurück ins Hauptmenü.
     */
    private StackPane buildGameView() {
        // Feedback/Ratings (LearnModePanel + PuzzlePanel) in einer Leiste ÜBER dem Brett.
        HBox.setHgrow(learnModePanel, Priority.ALWAYS);
        HBox.setHgrow(puzzlePanel, Priority.ALWAYS);
        HBox.setHgrow(openingPanel, Priority.ALWAYS);
        learnModePanel.setMaxWidth(Double.MAX_VALUE);
        puzzlePanel.setMaxWidth(Double.MAX_VALUE);
        openingPanel.setMaxWidth(Double.MAX_VALUE);
        HBox feedbackRow = new HBox(8, learnModePanel, puzzlePanel, openingPanel);

        // M9-Redesign: Zugliste bekommt eine eigene, feste Spalte AUSSERHALB von Brett/Menü (statt
        // zwischen beiden gequetscht zu werden). ReviewPanel bleibt Eigentümer der Zugliste (siehe
        // ReviewPanel.moveListNode()).
        HBox boardRow = new HBox(evalBar, boardView, reviewPanel.moveListNode());
        boardRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(boardView, Priority.ALWAYS);

        VBox gameBox = new VBox(8, feedbackRow, boardRow, reviewPanel);
        VBox.setVgrow(boardRow, Priority.ALWAYS);

        Button backButton = new Button("Zurück");
        backButton.setOnAction(e -> switchTo(AppView.MAIN_MENU));

        // M11: der "Weiter"-Button (nächste Eröffnungs-Variante) sitzt links neben "Zurück" und ist
        // nur sichtbar, wenn eine Buchlinie gerade durchgespielt wurde und es eine nächste Variante
        // gibt (Sichtbarkeit/Text steuert handleOpeningFeedback). openingNextButton ist ein Feld,
        // damit sein Zustand einen buildGameView()-Neuaufbau übersteht.
        HBox bottomRight = new HBox(8, openingNextButton, backButton);
        bottomRight.setAlignment(Pos.CENTER_RIGHT);
        bottomRight.setPickOnBounds(false); // Klicks in den leeren Bereich sollen durchgehen
        bottomRight.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        StackPane gameView = new StackPane(gameBox, bottomRight);
        StackPane.setAlignment(bottomRight, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(bottomRight, new Insets(8));
        return gameView;
    }

    private void runStartupChecks(BoardController boardController, BoardView boardView, EvalBar evalBar,
                                   LearnModePanel learnModePanel, PuzzlePanel puzzlePanel) {
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

        // M11: Eröffnungstrainer. Braucht selbst kein Stockfish, hängt aber hier hinter dem
        // Engine-Start (wie das Puzzle-Feature), da erst danach der evaluationController für die
        // Blunder-Unterdrückung während der Buchlinie existiert.
        String openingDbPath = OpeningRepository.resolveDefaultPath();
        try {
            OpeningRepository openingRepo = new OpeningRepository(openingDbPath);
            this.openingRepository = openingRepo;

            OpeningTrainerService trainer = new OpeningTrainerService(boardController, evaluationController);
            trainer.setHintArrowHandlers(boardView::showHintArrow, boardView::clearHintArrow);
            trainer.addTrainingStartedListener(userColor -> boardView.setFlipped(userColor == Side.BLACK));
            trainer.addFeedbackListener(feedback -> {
                openingPanel.showFeedback(feedback);
                handleOpeningFeedback(feedback);
            });
            trainer.setHintEnabled(progress.isShowHintArrow());
            Platform.runLater(() -> openingPanel.setHintEnabled(progress.isShowHintArrow()));
            this.openingTrainerService = trainer;
            System.out.println("[openings] Eröffnungstrainer bereit (DB: " + openingDbPath + ")");
        } catch (Exception e) {
            System.out.println("[openings] FEHLER: Eröffnungs-DB \"" + openingDbPath + "\" nicht verfügbar ("
                    + e.getMessage() + "). Eröffnungstrainer bleibt deaktiviert.");
            UiAlerts.showError("Eröffnungsdaten nicht verfügbar",
                    "Die Eröffnungs-Datenbank (" + openingDbPath + ") konnte nicht geöffnet/aufgebaut werden:\n"
                            + e.getMessage() + "\n\nDer Eröffnungstrainer bleibt deaktiviert. Alle anderen "
                            + "Funktionen sind davon nicht betroffen.");
        }

        evaluationController.evaluateCurrentPosition();

        System.out.println("=== Startdiagnose abgeschlossen ===");
    }

    /**
     * Pfeiltasten-Navigation für die Partie-Analyse (M8.1), auf Links/Rechts "fixiert" statt vom
     * gerade fokussierten Control abzuhängen: registriert als {@code addEventFilter} (Capturing-
     * Phase, läuft VOR dem fokussierten Node) statt {@code addEventHandler} und ruft
     * {@code event.consume()} - vorher konnte z.B. die fokussierte Partie-Auswahl-ComboBox in
     * {@link ReviewPanel} Links/Rechts selbst verarbeiten (Auswahl ändern -&gt; ungewollt eine
     * andere Partie laden), noch bevor unser Handler in der Bubbling-Phase überhaupt drankam.
     *
     * <p>Gehaltene Taste wiederholt zusätzlich beschleunigt über einen eigenen
     * {@link AnimationTimer} (erst alle 350ms, ab 1.5s alle 60ms) statt sich auf die
     * Betriebssystem-Tastenwiederholung zu verlassen, deren Rate/Vorhandensein plattformabhängig
     * ist. {@code ReviewPanel.stepHalfMove} ist ohne geladene Partie ein No-op.</p>
     */
    private void wireReviewArrowKeyNavigation(Scene scene, ReviewPanel reviewPanel) {
        int[] heldDelta = {0}; // 0 = keine Pfeiltaste gehalten, sonst -1 (links) / +1 (rechts)
        long[] holdStartNanos = {0};
        long[] lastStepNanos = {0};

        AnimationTimer repeatTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long heldMillis = (now - holdStartNanos[0]) / 1_000_000;
                long intervalMillis = heldMillis < 500 ? 350 : heldMillis < 1500 ? 150 : 60;
                if ((now - lastStepNanos[0]) / 1_000_000 >= intervalMillis) {
                    lastStepNanos[0] = now;
                    reviewPanel.stepHalfMove(heldDelta[0]);
                }
            }
        };

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            int delta = event.getCode() == KeyCode.LEFT ? -1 : event.getCode() == KeyCode.RIGHT ? 1 : 0;
            if (delta == 0) {
                return;
            }
            event.consume();
            if (heldDelta[0] == delta) {
                return; // Betriebssystem-Auto-Repeat des Tastendrucks - repeatTimer übernimmt das schon
            }
            heldDelta[0] = delta;
            long now = System.nanoTime();
            holdStartNanos[0] = now;
            lastStepNanos[0] = now;
            reviewPanel.stepHalfMove(delta);
            repeatTimer.start();
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.RIGHT) {
                event.consume();
                heldDelta[0] = 0;
                repeatTimer.stop();
            }
        });
    }

    /**
     * Startet die Partie-Analyse (M8) für eine per {@link ImportGameDialog} importierte Partie.
     * Läuft in einem eigenen Hintergrund-Thread, da {@link GameReviewEngine#review} pro Halbzug
     * blockierend eine echte Stockfish-Suche macht (analog {@code PuzzleSession.loadNewPuzzleAsync}).
     * Nutzt bewusst denselben {@link #engineEvaluator} wie die Live-Auswertung (siehe
     * {@code GameReviewEngine}-Javadoc) - ist die Engine (noch) nicht verfügbar, wird das dem User
     * gemeldet statt die Analyse stillschweigend zu überspringen.
     */
    private void startGameReview(ReviewPanel reviewPanel, LearnModePanel learnModePanel, ImportedGame game) {
        EngineEvaluator evaluator = this.engineEvaluator;
        if (evaluator == null) {
            UiAlerts.showError("Stockfish nicht verfügbar",
                    "Die Partie-Analyse benötigt die Schach-Engine, die aktuell nicht verfügbar ist.");
            return;
        }

        reviewPanel.showAnalysisStarted();
        // Ersetzt den sonst live-lern-modus-bezogenen "Mach einen Zug..."-Platzhalter/die letzte
        // Analyse-Tally, solange die neue Analyse noch läuft.
        learnModePanel.showReviewSummary("Analysiere...", 0, 0, 0, 0);
        GameReviewEngine reviewEngine = new GameReviewEngine(evaluator);
        Thread analysisThread = new Thread(() -> {
            try {
                GameReview review = reviewEngine.review(game,
                        (analyzed, total) -> Platform.runLater(() -> reviewPanel.updateProgress(analyzed, total)));
                Platform.runLater(() -> {
                    reviewPanel.showReview(review);
                    showReviewTally(learnModePanel, review);
                });
            } catch (EngineEvaluationException e) {
                UiAlerts.showError("Analyse fehlgeschlagen",
                        "Die Partie-Analyse ist fehlgeschlagen: " + e.getMessage());
            }
        }, "game-review-analysis");
        analysisThread.setDaemon(true);
        analysisThread.start();
    }

    /**
     * Zeigt die Gut/Ungenau/Fehler/Blunder-Tally der Analyse im (sonst für den Live-Lern-Modus
     * genutzten) {@link LearnModePanel} an - gezählt werden nur die Halbzüge der Farbe, mit der
     * der User laut {@link ImportedGame#userSide()} gespielt hat, nicht die des Gegners.
     */
    private static void showReviewTally(LearnModePanel learnModePanel, GameReview review) {
        Side userSide = review.game().userSide();
        Map<MoveQuality, Integer> counts = new EnumMap<>(MoveQuality.class);
        for (MoveQuality quality : MoveQuality.values()) {
            counts.put(quality, 0);
        }
        for (HalfMoveReview move : review.moves()) {
            Side moverSide = move.halfMoveIndex() % 2 == 0 ? Side.WHITE : Side.BLACK;
            if (moverSide == userSide) {
                counts.merge(move.quality(), 1, Integer::sum);
            }
        }
        learnModePanel.showReviewSummary("Partie-Analyse abgeschlossen.",
                counts.get(MoveQuality.GOOD), counts.get(MoveQuality.INACCURACY),
                counts.get(MoveQuality.MISTAKE), counts.get(MoveQuality.BLUNDER));
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
        if (openingRepository != null) {
            openingRepository.close();
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
