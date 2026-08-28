package org.schachlernapp.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
 * Brett in {@link org.schachlernapp.ui.opening.OpeningPanel}. Unten schließt ein
 * "Programm beenden"-Button die App (löst regulär {@code Main.stop()} aus - Fortschritt wird
 * gespeichert). Oben steht der Titel "Joshi's Chess Tutor" mit einer Dame (weißes cburnett-Bild).</p>
 */
public class MainMenuView extends VBox {

    public MainMenuView(Runnable onNewGame, Runnable onNewPuzzle, Runnable onEndgame, Runnable onOpening,
                        Runnable onImportGame, Runnable onQuit) {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.CENTER);
        setPrefWidth(240);
        getStyleClass().addAll("side-panel", "menu-pane");

        ImageView queen = new ImageView(new Image(getClass().getResourceAsStream("/pieces/wQ.png")));
        queen.setFitWidth(64);
        queen.setFitHeight(64);
        queen.setPreserveRatio(true);
        queen.setSmooth(true);

        Label titleLabel = new Label("Joshi's Chess Tutor");
        titleLabel.getStyleClass().add("menu-title");

        VBox header = new VBox(6, queen, titleLabel);
        header.setAlignment(Pos.CENTER);

        Button newGameButton = new Button("Neues Spiel");
        Button newPuzzleButton = new Button("Neues Puzzle");
        Button openingButton = new Button("Eröffnung");
        Button endgameButton = new Button("Endgame");
        Button importGameButton = new Button("Partie importieren");
        Button quitButton = new Button("Programm beenden");

        for (Button button : new Button[] {newGameButton, newPuzzleButton, openingButton, endgameButton,
                importGameButton, quitButton}) {
            button.setMaxWidth(Double.MAX_VALUE);
        }

        newGameButton.setOnAction(e -> onNewGame.run());
        newPuzzleButton.setOnAction(e -> onNewPuzzle.run());
        openingButton.setOnAction(e -> onOpening.run());
        endgameButton.setOnAction(e -> onEndgame.run());
        importGameButton.setOnAction(e -> onImportGame.run());
        quitButton.setOnAction(e -> onQuit.run());

        getChildren().addAll(header, new Separator(),
                newGameButton, newPuzzleButton, openingButton, endgameButton, importGameButton,
                new Separator(), quitButton);
    }
}
