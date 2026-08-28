package org.schachlernapp.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Startmenü (M10: Startmenü-Navigation) - erste Ansicht beim App-Start, ersetzt an dieser Stelle
 * das frühere {@code OptionsPanel}, das dauerhaft neben dem Brett stand.
 *
 * <p>Reine Anzeigekomponente - die eigentlichen Aktionen (inkl. eines evtl. Zugriffs auf Services,
 * die erst asynchron nach dem Stockfish-Start verfügbar sind) übergibt {@code Main} als Callback im
 * Konstruktor, da diese View bei jedem Wechsel zu {@link AppView#MAIN_MENU} neu erzeugt wird.</p>
 *
 * <p>Optik (M11): zentrierte "Karte" (feste Breite, abgerundet, Schatten - Klasse {@code menu-card})
 * statt vollflächigem Panel, mit Titel "Joshi's Chess Tutor" + Dame (weißes cburnett-Bild) oben.
 * Der "Eröffnung"-Button ist aktiv ({@link AppView#OPENING_SELECT}); der "Zughinweis anzeigen"-
 * Schalter sitzt bewusst NICHT hier, sondern beim Brett in
 * {@link org.schachlernapp.ui.opening.OpeningPanel}. Unten schließt "Programm beenden" die App
 * (löst regulär {@code Main.stop()} aus - Fortschritt wird gespeichert).</p>
 */
public class MainMenuView extends VBox {

    public MainMenuView(Runnable onNewGame, Runnable onNewPuzzle, Runnable onEndgame, Runnable onOpening,
                        Runnable onImportGame, Runnable onQuit) {
        setSpacing(10);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);
        setFillWidth(true);
        setPrefWidth(420);
        // Als zentrierte Karte im Fenster stehen bleiben, statt die ganze Wurzel-StackPane zu füllen.
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        getStyleClass().add("menu-card");

        ImageView queen = new ImageView(new Image(getClass().getResourceAsStream("/pieces/wQ.png")));
        queen.setFitWidth(96);
        queen.setFitHeight(96);
        queen.setPreserveRatio(true);
        queen.setSmooth(true);

        Label titleLabel = new Label("Joshi's Chess Tutor");
        titleLabel.getStyleClass().add("menu-title");

        Label subtitleLabel = new Label("Schach lernen mit Stockfish");
        subtitleLabel.getStyleClass().add("menu-subtitle");

        VBox header = new VBox(4, queen, titleLabel, subtitleLabel);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 12, 0));

        Button newGameButton = menuButton("Neues Spiel", onNewGame);
        Button newPuzzleButton = menuButton("Neues Puzzle", onNewPuzzle);
        Button openingButton = menuButton("Eröffnung", onOpening);
        Button endgameButton = menuButton("Endgame", onEndgame);
        Button importGameButton = menuButton("Partie importieren", onImportGame);

        VBox actions = new VBox(10, newGameButton, newPuzzleButton, openingButton, endgameButton, importGameButton);
        actions.setFillWidth(true);

        Button quitButton = menuButton("Programm beenden", onQuit);
        quitButton.getStyleClass().add("menu-button-quit");
        VBox.setMargin(quitButton, new Insets(18, 0, 0, 0));

        getChildren().addAll(header, actions, quitButton);
    }

    private static Button menuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> action.run());
        return button;
    }
}
