package org.schachlernapp.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import org.schachlernapp.puzzle.EndgameTheme;

import java.util.function.Consumer;

/**
 * Endspiel-Themenauswahl (M10: Startmenü-Navigation) - vormals das Untermenü, das
 * {@code OptionsPanel} beim Klick auf "Endgame" in derselben {@code VBox} anzeigte, jetzt eine
 * eigenständige {@link AppView#ENDGAME_SELECT}-Ansicht. Buttons/Themen-Liste 1:1 übernommen.
 *
 * <p>{@code onThemeSelected} übernimmt sowohl das Laden des Endspiels als auch den Wechsel zu
 * {@link AppView#GAME} (beides verdrahtet {@code Main}, da beides Zugriff auf App-weiten Zustand
 * braucht) - diese View kennt selbst weder Services noch andere Ansichten.</p>
 */
public class EndgameMenuView extends VBox {

    public EndgameMenuView(Consumer<EndgameTheme> onThemeSelected, Runnable onBack) {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.CENTER);
        setPrefWidth(200);
        getStyleClass().addAll("side-panel", "menu-pane");

        for (EndgameTheme theme : EndgameTheme.values()) {
            Button themeButton = new Button(theme.label());
            themeButton.setMaxWidth(Double.MAX_VALUE);
            themeButton.setOnAction(e -> onThemeSelected.accept(theme));
            getChildren().add(themeButton);
        }

        Button backButton = new Button("Zurück");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> onBack.run());
        getChildren().add(backButton);
    }
}
