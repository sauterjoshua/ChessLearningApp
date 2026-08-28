package org.schachlernapp.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.schachlernapp.puzzle.EndgameTheme;

import java.util.function.Consumer;

/**
 * Endspiel-Themenauswahl (M10: Startmenü-Navigation) - eigenständige {@link AppView#ENDGAME_SELECT}-
 * Ansicht.
 *
 * <p>{@code onThemeSelected} übernimmt sowohl das Laden des Endspiels als auch den Wechsel zu
 * {@link AppView#GAME} (beides verdrahtet {@code Main}, da beides Zugriff auf App-weiten Zustand
 * braucht) - diese View kennt selbst weder Services noch andere Ansichten.</p>
 *
 * <p>Optik (M11): gleicher zentrierter Karten-Look wie {@link MainMenuView} (Klasse
 * {@code menu-card}, {@code menu-heading}, {@code menu-button}).</p>
 */
public class EndgameMenuView extends VBox {

    public EndgameMenuView(Consumer<EndgameTheme> onThemeSelected, Runnable onBack) {
        setSpacing(10);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);
        setFillWidth(true);
        setPrefWidth(420);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        getStyleClass().add("menu-card");

        Label title = new Label("Endspiel wählen");
        title.getStyleClass().add("menu-heading");
        VBox.setMargin(title, new Insets(0, 0, 8, 0));
        getChildren().add(title);

        for (EndgameTheme theme : EndgameTheme.values()) {
            getChildren().add(menuButton(theme.label(), () -> onThemeSelected.accept(theme)));
        }

        Button backButton = menuButton("Zurück", onBack);
        VBox.setMargin(backButton, new Insets(18, 0, 0, 0));
        getChildren().add(backButton);
    }

    private static Button menuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("menu-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> action.run());
        return button;
    }
}
