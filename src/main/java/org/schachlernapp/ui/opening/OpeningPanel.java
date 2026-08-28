package org.schachlernapp.ui.opening;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.schachlernapp.opening.Opening;
import org.schachlernapp.opening.OpeningFeedback;

import java.util.function.Consumer;

/**
 * Zeigt den Status des Eröffnungstrainers (M11) an - aktuelle Eröffnung/Variante, Fortschritt in
 * der Buchlinie und, sobald die Linie durchgespielt ist, welche Variante als Nächstes kommt.
 * Enthält außerdem den "Zughinweis anzeigen"-Schalter (Hint-Pfeil) direkt beim Brett. Reine
 * Anzeigekomponente, extern verdrahtet (gleiches Muster wie {@link org.schachlernapp.ui.puzzle.PuzzlePanel}).
 */
public class OpeningPanel extends VBox {

    private static final String GOOD_STYLE = "text-good";
    private static final String BAD_STYLE = "text-bad";
    private static final String NEUTRAL_STYLE = "text-neutral";

    private final Label headingLabel = new Label("Eröffnungstrainer");
    private final Label currentLabel = new Label("Keine Eröffnung aktiv.");
    private final CheckBox hintCheckBox = new CheckBox("Zughinweis anzeigen");
    private final Label statusLabel = new Label("Wähle im Menü \"Eröffnung\" eine Linie.");
    private final Label progressLabel = new Label("");
    private final Label nextLabel = new Label("");

    private Consumer<Boolean> onHintToggle;

    public OpeningPanel() {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(220);
        getStyleClass().add("side-panel");

        headingLabel.getStyleClass().add("panel-heading");
        currentLabel.setWrapText(true);
        currentLabel.getStyleClass().add("panel-heading");
        statusLabel.setWrapText(true);
        progressLabel.getStyleClass().add("panel-muted");
        nextLabel.setWrapText(true);
        nextLabel.getStyleClass().add("panel-muted");
        setNextManaged(false);

        // setOnAction feuert NUR bei echter Nutzer-Interaktion, nicht bei programmatischem
        // setSelected(...) in setHintEnabled(...) - kein Rückkopplungs-/Doppel-Speicher-Problem.
        hintCheckBox.setOnAction(e -> {
            if (onHintToggle != null) {
                onHintToggle.accept(hintCheckBox.isSelected());
            }
        });

        getChildren().addAll(headingLabel, currentLabel, hintCheckBox, statusLabel, progressLabel, nextLabel);
    }

    /** Wird bei jedem Klick auf den "Zughinweis anzeigen"-Schalter mit dem neuen Zustand aufgerufen. */
    public void setOnHintToggle(Consumer<Boolean> listener) {
        this.onHintToggle = listener;
    }

    /** Setzt den Häkchen-Zustand ohne den {@link #setOnHintToggle}-Callback auszulösen (Initialwert aus M7). */
    public void setHintEnabled(boolean enabled) {
        hintCheckBox.setSelected(enabled);
    }

    /** Zeigt an, welche Eröffnung + Variante gerade geübt wird; blendet einen evtl. "Nächste Variante"-Hinweis aus. */
    public void showOpening(Opening opening) {
        currentLabel.setText(opening == null
                ? "Keine Eröffnung aktiv."
                : opening.family() + " – " + opening.variation() + "  (" + opening.eco() + ")");
        setNextManaged(false);
        nextLabel.getStyleClass().removeAll(GOOD_STYLE);
    }

    /**
     * Nach durchgespielter Buchlinie: {@code next} = die nächste Variante derselben Eröffnung, oder
     * {@code null}, wenn es die letzte war.
     */
    public void showNextVariation(Opening next) {
        nextLabel.setText(next == null
                ? "Letzte Variante – Eröffnung komplett durchgespielt."
                : "Nächste Variante: " + next.variation() + "  (" + next.eco() + ")");
        if (next != null && !nextLabel.getStyleClass().contains(GOOD_STYLE)) {
            nextLabel.getStyleClass().add(GOOD_STYLE);
        }
        setNextManaged(true);
    }

    public void showFeedback(OpeningFeedback feedback) {
        switch (feedback.outcome()) {
            case CORRECT_CONTINUE -> setStatus("Buchzug - weiter so.", GOOD_STYLE);
            case DEVIATION -> setStatus("Abweichung von der Buchlinie. Erwartet war " + feedback.expectedUci()
                    + ". Ab hier zählt die normale Zug-Bewertung.", BAD_STYLE);
            case BOOK_FINISHED -> setStatus("Buchlinie zu Ende - ab jetzt normale Zug-Bewertung.", NEUTRAL_STYLE);
            case NO_OPENING_DATA -> setStatus("Keine Eröffnungsdaten vorhanden.", NEUTRAL_STYLE);
        }
        progressLabel.setText(feedback.movesTotal() > 0
                ? "Buchzug " + feedback.movesPlayed() + " von " + feedback.movesTotal()
                : "");
    }

    private void setStatus(String text, String styleClass) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll(GOOD_STYLE, BAD_STYLE, NEUTRAL_STYLE);
        statusLabel.getStyleClass().add(styleClass);
    }

    private void setNextManaged(boolean shown) {
        nextLabel.setVisible(shown);
        nextLabel.setManaged(shown);
    }
}
