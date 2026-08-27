package org.schachlernapp.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.schachlernapp.puzzle.EndgameTheme;

import java.util.function.Consumer;

/**
 * Allgemeines Aktionspanel (rechts vom Brett). Reine Anzeigekomponente, extern
 * verdrahtet (gleiches Muster wie {@link org.schachlernapp.ui.eval.EvalBar}/
 * {@link org.schachlernapp.ui.puzzle.PuzzlePanel}).
 *
 * <p>M9: Klick auf "Endgame" ersetzt das Hauptmenü (in derselben VBox, kein neues Fenster)
 * durch ein Untermenü mit den Endspiel-Themen; "Zurück" schaltet zurück. Weitere Untermenüs
 * (z.B. für "Eröffnung") lassen sich nach demselben Muster ergänzen: eine private
 * {@code buildXyzMenu()}-Methode, die eine eigene {@link VBox} liefert, ein Hauptmenü-Button,
 * der per {@link #showMenu(VBox)} dorthin wechselt, und ein "Zurück"-Button darin, der per
 * {@code showMenu(mainMenu)} zurückschaltet.</p>
 */
public class OptionsPanel extends VBox {

    private final Button newGameButton = new Button("Neues Spiel");
    private final Button newPuzzleButton = new Button("Neues Puzzle");
    private final Button openingButton = new Button("Eröffnung");
    private final Button endgameButton = new Button("Endgame");
    private final Button importGameButton = new Button("Partie importieren");

    private final VBox mainMenu;
    private final VBox endgameMenu;

    private Consumer<EndgameTheme> onEndgameThemeSelected;

    public OptionsPanel() {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(160);
        getStyleClass().add("side-panel");

        openingButton.setDisable(true); // spätere Implementierung

        for (Button button : new Button[] {newGameButton, newPuzzleButton, openingButton, endgameButton, importGameButton}) {
            button.setMaxWidth(Double.MAX_VALUE);
        }

        mainMenu = new VBox(8, newGameButton, newPuzzleButton, openingButton, endgameButton, importGameButton);
        endgameMenu = buildEndgameMenu();

        endgameButton.setOnAction(e -> showMenu(endgameMenu));

        getChildren().add(mainMenu);
    }

    private VBox buildEndgameMenu() {
        VBox menu = new VBox(8);
        for (EndgameTheme theme : EndgameTheme.values()) {
            Button themeButton = new Button(theme.label());
            themeButton.setMaxWidth(Double.MAX_VALUE);
            themeButton.setOnAction(e -> {
                if (onEndgameThemeSelected != null) {
                    onEndgameThemeSelected.accept(theme);
                }
            });
            menu.getChildren().add(themeButton);
        }

        Button backButton = new Button("Zurück");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> showMenu(mainMenu));
        menu.getChildren().add(backButton);

        return menu;
    }

    private void showMenu(VBox menu) {
        getChildren().setAll(menu);
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

    /** M9: wird mit dem gewählten Thema aufgerufen, sobald im Endgame-Untermenü ein Theme-Button geklickt wird. */
    public void setOnEndgameThemeSelected(Consumer<EndgameTheme> listener) {
        this.onEndgameThemeSelected = listener;
    }
}
