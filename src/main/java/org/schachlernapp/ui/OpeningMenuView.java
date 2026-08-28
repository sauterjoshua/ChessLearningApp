package org.schachlernapp.ui;

import com.github.bhlangonijr.chesslib.Side;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.schachlernapp.opening.Opening;
import org.schachlernapp.opening.OpeningRepository;
import org.schachlernapp.opening.OpeningRole;

import java.util.List;

/**
 * Eröffnungs-Auswahl (M11) - eigenständige {@link AppView#OPENING_SELECT}-Ansicht, aufgebaut
 * nach demselben Muster wie {@link EndgameMenuView}: kennt selbst weder Services noch andere
 * Ansichten, gibt die getroffene Auswahl per Callback an {@code Main} zurück.
 *
 * <p>Zweistufige Auswahl aus {@link OpeningRepository}: Eröffnung (Familienname, ~150 Einträge
 * alphabetisch) -&gt; Variante. Der ECO-Code (z.B. {@code B90}) wird bei der Variante nur als
 * Info angezeigt, er ist kein Auswahlkriterium. Dazu Rolle ({@link OpeningRole}) und Farbe.</p>
 */
public class OpeningMenuView extends VBox {

    /**
     * Callback für den Start-Button: Familienname (für die "Weiter zur nächsten Variante"-Navigation
     * in {@code Main}), gewählte Variante, Rolle und die vom User gesteuerte Farbe.
     */
    public interface OnStart {
        void start(String family, Opening opening, OpeningRole role, Side userColor);
    }

    private final OpeningRepository repository;

    private final ComboBox<String> familyBox = new ComboBox<>();
    private final ComboBox<Opening> variationBox = new ComboBox<>();
    private final ToggleGroup roleGroup = new ToggleGroup();
    private final ToggleGroup colorGroup = new ToggleGroup();
    private final Button startButton = new Button("Start");

    public OpeningMenuView(OpeningRepository repository, OnStart onStart, Runnable onBack) {
        this.repository = repository;

        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.CENTER);
        setPrefWidth(260);
        getStyleClass().addAll("side-panel", "menu-pane");

        familyBox.setMaxWidth(Double.MAX_VALUE);
        variationBox.setMaxWidth(Double.MAX_VALUE);
        familyBox.setPromptText("Eröffnung");
        variationBox.setPromptText("Variante");
        familyBox.setVisibleRowCount(16);

        variationBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Opening opening) {
                return opening == null ? "" : opening.variation() + "  ·  " + opening.eco();
            }

            @Override
            public Opening fromString(String string) {
                return null;
            }
        });

        List<String> families = repository.allFamilies();
        familyBox.getItems().setAll(families);
        familyBox.valueProperty().addListener((obs, old, family) -> onFamilySelected(family));
        variationBox.valueProperty().addListener((obs, old, variation) -> updateStartEnabled());

        RadioButton playAs = new RadioButton(OpeningRole.PLAY_AS.label());
        playAs.setUserData(OpeningRole.PLAY_AS);
        playAs.setToggleGroup(roleGroup);
        playAs.setSelected(true);
        RadioButton playAgainst = new RadioButton(OpeningRole.PLAY_AGAINST.label());
        playAgainst.setUserData(OpeningRole.PLAY_AGAINST);
        playAgainst.setToggleGroup(roleGroup);

        RadioButton white = new RadioButton("Weiß");
        white.setUserData(Side.WHITE);
        white.setToggleGroup(colorGroup);
        white.setSelected(true);
        RadioButton black = new RadioButton("Schwarz");
        black.setUserData(Side.BLACK);
        black.setToggleGroup(colorGroup);

        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setDisable(true);
        startButton.setOnAction(e -> {
            Opening opening = variationBox.getValue();
            if (opening != null) {
                onStart.start(familyBox.getValue(), opening,
                        (OpeningRole) roleGroup.getSelectedToggle().getUserData(),
                        (Side) colorGroup.getSelectedToggle().getUserData());
            }
        });

        Button backButton = new Button("Zurück");
        backButton.setMaxWidth(Double.MAX_VALUE);
        backButton.setOnAction(e -> onBack.run());

        Label ecoHint = new Label("ECO-Code (z. B. B90) = Standard-Katalognummer, nur zur Info.");
        ecoHint.setWrapText(true);
        ecoHint.getStyleClass().add("panel-muted");

        getChildren().addAll(new Label("Eröffnung wählen"), familyBox, variationBox, ecoHint);
        if (families.isEmpty()) {
            Label empty = new Label("Keine Eröffnungsdaten gefunden (data/openings/*.tsv).");
            empty.setWrapText(true);
            empty.getStyleClass().add("panel-muted");
            getChildren().add(empty);
        }
        getChildren().addAll(new Label("Rolle"), playAs, playAgainst,
                new Label("Farbe"), white, black, startButton, backButton);
    }

    private void onFamilySelected(String family) {
        variationBox.getItems().clear();
        if (family != null) {
            variationBox.getItems().setAll(repository.variationsByFamily(family));
            if (!variationBox.getItems().isEmpty()) {
                variationBox.setValue(variationBox.getItems().get(0)); // sinnvolle Vorauswahl: die Hauptlinie
            }
        }
        updateStartEnabled();
    }

    private void updateStartEnabled() {
        startButton.setDisable(variationBox.getValue() == null);
    }
}
