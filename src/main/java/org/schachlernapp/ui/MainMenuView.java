package org.schachlernapp.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

/**
 * Startmenü (M10: Startmenü-Navigation) - erste Ansicht beim App-Start, ersetzt an dieser Stelle
 * das frühere {@code OptionsPanel}, das dauerhaft neben dem Brett stand. Buttons/Beschriftungen
 * sind 1:1 aus {@code OptionsPanel} übernommen.
 *
 * <p>Reine Anzeigekomponente - die eigentlichen Aktionen (inkl. eines evtl. Zugriffs auf Services,
 * die erst asynchron nach dem Stockfish-Start verfügbar sind) übergibt {@code Main} als Callback im
 * Konstruktor, da diese View bei jedem Wechsel zu {@link AppView#MAIN_MENU} neu erzeugt wird.</p>
 *
 * <p>M11: Der "Eröffnung"-Button ist jetzt aktiv (führt zu {@link AppView#OPENING_SELECT}). Der
 * "Zughinweis anzeigen"-Schalter des Eröffnungstrainers sitzt bewusst NICHT hier, sondern beim
 * Brett in {@link org.schachlernapp.ui.opening.OpeningPanel}.</p>
 */
public class MainMenuView extends VBox {

    public MainMenuView(Runnable onNewGame, Runnable onNewPuzzle, Runnable onEndgame, Runnable onOpening,
                        Runnable onImportGame) {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.CENTER);
        setPrefWidth(200);
        getStyleClass().addAll("side-panel", "menu-pane");

        Button newGameButton = new Button("Neues Spiel");
        Button newPuzzleButton = new Button("Neues Puzzle");
        Button openingButton = new Button("Eröffnung");
        Button endgameButton = new Button("Endgame");
        Button importGameButton = new Button("Partie importieren");

        for (Button button : new Button[] {newGameButton, newPuzzleButton, openingButton, endgameButton, importGameButton}) {
            button.setMaxWidth(Double.MAX_VALUE);
        }

        newGameButton.setOnAction(e -> onNewGame.run());
        newPuzzleButton.setOnAction(e -> onNewPuzzle.run());
        openingButton.setOnAction(e -> onOpening.run());
        endgameButton.setOnAction(e -> onEndgame.run());
        importGameButton.setOnAction(e -> onImportGame.run());

        getChildren().addAll(newGameButton, newPuzzleButton, openingButton, endgameButton, importGameButton);
    }
}
