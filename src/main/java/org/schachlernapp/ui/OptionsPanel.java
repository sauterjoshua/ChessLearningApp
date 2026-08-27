package org.schachlernapp.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * Allgemeines Aktionspanel (rechts vom Brett). Reine Anzeigekomponente, extern
 * verdrahtet (gleiches Muster wie {@link org.schachlernapp.ui.eval.EvalBar}/
 * {@link org.schachlernapp.ui.puzzle.PuzzlePanel}).
 *
 * <p>"Üben" ist absichtlich deaktiviert - spätere Implementierung, noch kein
 * zugehöriger Controller vorhanden.</p>
 */
public class OptionsPanel extends VBox {

    private final Button newGameButton = new Button("Neues Spiel");
    private final Button newPuzzleButton = new Button("Neues Puzzle");
    private final Button practiceButton = new Button("Üben");
    private final Button importGameButton = new Button("Partie importieren");

    public OptionsPanel() {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(160);
        getStyleClass().add("side-panel");

        practiceButton.setDisable(true); // spätere Implementierung

        for (Button button : new Button[] {newGameButton, newPuzzleButton, practiceButton, importGameButton}) {
            button.setMaxWidth(Double.MAX_VALUE);
        }

        getChildren().addAll(newGameButton, newPuzzleButton, practiceButton, importGameButton);
    }

    public void setOnNewGameRequested(Runnable action) {
        newGameButton.setOnAction(e -> action.run());
    }

    public void setOnNewPuzzleRequested(Runnable action) {
        newPuzzleButton.setOnAction(e -> action.run());
    }

    /** M8: öffnet den chess.com-Import-Dialog. */
    public void setOnImportGameRequested(Runnable action) {
        importGameButton.setOnAction(e -> action.run());
    }
}
