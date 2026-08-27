package org.schachlernapp.ui.review;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.schachlernapp.review.GameReview;
import org.schachlernapp.review.ImportedGame;

import java.util.List;
import java.util.function.Consumer;

/**
 * Dauerhaft sichtbare Review-Zeile (M8), unterhalb des Bretts in {@code Main}: Partie-Auswahl
 * (Datum/Gegner/Ergebnis über {@link ImportedGame#toString()}), Fortschrittsbalken während der
 * Analyse, Eval-Graph danach. Reine Anzeigekomponente, extern verdrahtet (gleiches Muster wie
 * {@code OptionsPanel}/{@code PuzzlePanel}).
 *
 * <p><b>Zugliste (M9-Redesign):</b> {@link #moveListView} wird zwar hier erzeugt/verdrahtet
 * (Auswahl-Logik bleibt an einer Stelle), aber bewusst NICHT als Kind-Node dieses Panels gerendert -
 * sonst würde sie zwischen Brett und Menü gequetscht bzw. deren Breiten verändern. Stattdessen holt
 * {@code Main} sie über {@link #moveListNode()} ab und hängt sie als eigene, gleich hohe Spalte
 * neben das {@code OptionsPanel}.</p>
 *
 * <p>Hält selbst die "aktuell ausgewählte Stellung" der geladenen Partie ({@link #currentGame}/
 * {@link #currentHalfMoveIndex}) - sowohl {@link EvalGraph}-Klicks, {@link MoveListView}-Klicks als
 * auch externe Pfeiltasten-Navigation ({@link #stepHalfMove(int)}) laufen über {@link #selectHalfMove}
 * und melden am Ende direkt die FEN der Zielstellung über {@link #setOnPositionSelected(Consumer)} -
 * der Aufrufer (Main) muss dafür keinen eigenen Partie-/Index-Zustand mehr mitführen.</p>
 */
public class ReviewPanel extends VBox {

    private final ComboBox<ImportedGame> gameSelector = new ComboBox<>();
    private final Label statusLabel = new Label("Noch keine Partie importiert.");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final EvalGraph evalGraph = new EvalGraph();
    private final MoveListView moveListView = new MoveListView();

    private Consumer<ImportedGame> onGameSelected;
    private Consumer<String> onPositionSelected;

    private ImportedGame currentGame;
    private int currentHalfMoveIndex = -1;

    public ReviewPanel() {
        setSpacing(6);
        setPadding(new Insets(8));
        getStyleClass().addAll("side-panel", "review-pane");

        statusLabel.getStyleClass().add("panel-muted");

        gameSelector.setMaxWidth(Double.MAX_VALUE);
        gameSelector.setPromptText("Partie wählen...");
        gameSelector.setDisable(true);
        gameSelector.valueProperty().addListener((obs, oldGame, newGame) -> onGameSelectorChanged(newGame));

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        evalGraph.setOnMoveSelected(this::selectHalfMove);
        moveListView.setOnMoveSelected(this::selectHalfMove);

        HBox.setHgrow(gameSelector, Priority.ALWAYS);
        HBox topRow = new HBox(8, gameSelector, statusLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(evalGraph, Priority.ALWAYS);
        getChildren().addAll(topRow, progressBar, evalGraph);
    }

    /**
     * Die Zugliste als eigenständiger Node zum Einhängen außerhalb dieses Panels (siehe
     * Klassen-Javadoc) - {@code ReviewPanel} bleibt Eigentümer/verdrahtet Auswahl-Events weiterhin
     * selbst über {@link #selectHalfMove}.
     */
    public MoveListView moveListNode() {
        return moveListView;
    }

    /** Ersetzt die Liste der wählbaren Partien (neuer Import) und leert eine ggf. angezeigte Analyse. */
    public void setGames(List<ImportedGame> games) {
        gameSelector.getItems().setAll(games);
        gameSelector.setDisable(games.isEmpty());
        resetSelection();
        statusLabel.setText(games.isEmpty()
                ? "Keine Partien im gewählten Monat gefunden."
                : games.size() + " Partie(n) importiert - zum Analysieren auswählen.");
    }

    /** Wird aufgerufen, sobald der User in der Dropdown eine Partie auswählt (soll die Analyse anstoßen). */
    public void setOnGameSelected(Consumer<ImportedGame> listener) {
        this.onGameSelected = listener;
    }

    /** M8.1/M8.2: meldet die FEN der per Pfeiltaste/Zugliste/Eval-Graph ausgewählten Stellung. */
    public void setOnPositionSelected(Consumer<String> listener) {
        this.onPositionSelected = listener;
    }

    /** Blendet den Fortschrittsbalken ein (0%) - vor dem ersten {@link #updateProgress}-Aufruf. */
    public void showAnalysisStarted() {
        evalGraph.clear();
        progressBar.setProgress(0);
        progressBar.setVisible(true);
        progressBar.setManaged(true);
        statusLabel.setText("Analysiere...");
    }

    public void updateProgress(int analyzedHalfMoves, int totalHalfMoves) {
        progressBar.setProgress(totalHalfMoves == 0 ? 0 : analyzedHalfMoves / (double) totalHalfMoves);
        statusLabel.setText("Analysiere... (" + analyzedHalfMoves + "/" + totalHalfMoves + ")");
    }

    /** Analyse abgeschlossen - blendet den Fortschrittsbalken aus und zeigt den Eval-Graph. */
    public void showReview(GameReview review) {
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        statusLabel.setText("Analyse abgeschlossen (" + review.moves().size() + " Halbzüge).");
        evalGraph.showReview(review);
    }

    /**
     * Pfeiltasten-Navigation (M8.1): einen Halbzug vor ({@code delta > 0}) oder zurück
     * ({@code delta < 0}). No-op ohne aktuell geladene Partie.
     */
    public void stepHalfMove(int delta) {
        if (currentGame == null) {
            return;
        }
        selectHalfMove(currentHalfMoveIndex + delta);
    }

    private void onGameSelectorChanged(ImportedGame newGame) {
        if (newGame == null) {
            return;
        }
        currentGame = newGame;
        currentHalfMoveIndex = -1;
        moveListView.setMoves(newGame.sanMoves());
        moveListView.selectMove(-1);
        if (onGameSelected != null) {
            onGameSelected.accept(newGame);
        }
    }

    /** Zentrale Auswahl-Logik: klemmt den Index, hält Zugliste/Zustand synchron, meldet die Ziel-FEN. */
    private void selectHalfMove(int halfMoveIndex) {
        if (currentGame == null) {
            return;
        }
        int clamped = Math.max(-1, Math.min(currentGame.sanMoves().size() - 1, halfMoveIndex));
        currentHalfMoveIndex = clamped;
        moveListView.selectMove(clamped);
        if (onPositionSelected != null) {
            onPositionSelected.accept(currentGame.fens().get(clamped + 1));
        }
    }

    private void resetSelection() {
        currentGame = null;
        currentHalfMoveIndex = -1;
        moveListView.clear();
        evalGraph.clear();
        progressBar.setVisible(false);
        progressBar.setManaged(false);
    }
}
