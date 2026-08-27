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
 */
public class ReviewPanel extends VBox {

    private final ComboBox<ImportedGame> gameSelector = new ComboBox<>();
    private final Label statusLabel = new Label("Noch keine Partie importiert.");
    private final ProgressBar progressBar = new ProgressBar(0);
    private final EvalGraph evalGraph = new EvalGraph();

    private Consumer<ImportedGame> onGameSelected;

    public ReviewPanel() {
        setSpacing(6);
        setPadding(new Insets(8));
        getStyleClass().add("side-panel");

        statusLabel.getStyleClass().add("panel-muted");

        gameSelector.setMaxWidth(Double.MAX_VALUE);
        gameSelector.setPromptText("Partie wählen...");
        gameSelector.setDisable(true);
        gameSelector.valueProperty().addListener((obs, oldGame, newGame) -> {
            if (newGame != null && onGameSelected != null) {
                onGameSelected.accept(newGame);
            }
        });

        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        HBox.setHgrow(gameSelector, Priority.ALWAYS);
        HBox topRow = new HBox(8, gameSelector, statusLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(topRow, progressBar, evalGraph);
    }

    /** Ersetzt die Liste der wählbaren Partien (neuer Import) und leert eine ggf. angezeigte Analyse. */
    public void setGames(List<ImportedGame> games) {
        gameSelector.getItems().setAll(games);
        gameSelector.setDisable(games.isEmpty());
        evalGraph.clear();
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        statusLabel.setText(games.isEmpty()
                ? "Keine Partien im gewählten Monat gefunden."
                : games.size() + " Partie(n) importiert - zum Analysieren auswählen.");
    }

    /** Wird aufgerufen, sobald der User in der Dropdown eine Partie auswählt (soll die Analyse anstoßen). */
    public void setOnGameSelected(Consumer<ImportedGame> listener) {
        this.onGameSelected = listener;
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

    /** {@code -1} = Startstellung, sonst Halbzug-Index (siehe {@code HalfMoveReview#halfMoveIndex()}). */
    public void setOnMoveSelected(Consumer<Integer> listener) {
        evalGraph.setOnMoveSelected(listener);
    }
}
