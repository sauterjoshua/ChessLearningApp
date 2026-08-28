package org.schachlernapp.ui.opening;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.schachlernapp.opening.Opening;
import org.schachlernapp.opening.OpeningFeedback;

import java.util.function.Consumer;

/**
 * Zeigt den Status des Eröffnungstrainers (M11) an - aktuelle Eröffnung/Variante, Fortschritt in
 * der Buchlinie, nach einem Fehlzug einen "Nochmal versuchen"-Button und, wenn die Linie
 * durchgespielt ist, welche Variante als Nächstes kommt. Enthält außerdem den
 * "Zughinweis anzeigen"-Schalter (Hint-Pfeil) direkt beim Brett. Reine Anzeigekomponente,
 * extern verdrahtet (gleiches Muster wie {@link org.schachlernapp.ui.puzzle.PuzzlePanel}).
 *
 * <p>Feste Höhe ({@link #PANEL_HEIGHT}), damit sich das darüberliegende Brett nicht verschiebt,
 * wenn die Statuszeile länger wird (z.B. Fehlermeldung).</p>
 */
public class OpeningPanel extends VBox {

    private static final double PANEL_HEIGHT = 182;
    private static final Duration FLASH_DURATION = Duration.millis(450);

    private static final String GOOD_STYLE = "text-good";
    private static final String BAD_STYLE = "text-bad";
    private static final String NEUTRAL_STYLE = "text-neutral";

    private final Label currentLabel = new Label("Keine Eröffnung aktiv");
    private final CheckBox hintCheckBox = new CheckBox("Zughinweis anzeigen");
    private final Label statusLabel = new Label("Im Menü \"Eröffnung\" eine Linie wählen.");
    private final Label progressLabel = new Label("");
    private final Button retryButton = new Button("Nochmal versuchen");
    private final Label nextLabel = new Label("");

    private Consumer<Boolean> onHintToggle;
    private Runnable onRetryRequested;

    public OpeningPanel() {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.TOP_LEFT);
        setPrefWidth(220);
        setMinHeight(PANEL_HEIGHT);
        setPrefHeight(PANEL_HEIGHT);
        setMaxHeight(PANEL_HEIGHT);
        getStyleClass().add("side-panel");

        currentLabel.getStyleClass().add("panel-heading");
        currentLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setWrapText(true);
        progressLabel.getStyleClass().add("panel-muted");
        nextLabel.setWrapText(true);
        nextLabel.getStyleClass().add("panel-muted");

        retryButton.setMaxWidth(Double.MAX_VALUE);
        retryButton.setOnAction(e -> handleRetryClicked());
        showNode(retryButton, false);
        showNode(nextLabel, false);

        // setOnAction feuert NUR bei echter Nutzer-Interaktion, nicht bei programmatischem
        // setSelected(...) in setHintEnabled(...) - kein Rückkopplungs-/Doppel-Speicher-Problem.
        hintCheckBox.setOnAction(e -> {
            if (onHintToggle != null) {
                onHintToggle.accept(hintCheckBox.isSelected());
            }
        });

        getChildren().addAll(currentLabel, hintCheckBox, statusLabel, progressLabel, retryButton, nextLabel);
    }

    /** Wird bei jedem Klick auf den "Zughinweis anzeigen"-Schalter mit dem neuen Zustand aufgerufen. */
    public void setOnHintToggle(Consumer<Boolean> listener) {
        this.onHintToggle = listener;
    }

    /** Wird bei "Nochmal versuchen" aufgerufen (soll {@code OpeningTrainerService.retryAfterDeviation()} anstoßen). */
    public void setOnRetryRequested(Runnable action) {
        this.onRetryRequested = action;
    }

    /** Setzt den Häkchen-Zustand ohne den {@link #setOnHintToggle}-Callback auszulösen (Initialwert aus M7). */
    public void setHintEnabled(boolean enabled) {
        hintCheckBox.setSelected(enabled);
    }

    /** Zeigt an, welche Eröffnung + Variante gerade geübt wird; blendet Retry-/"Nächste Variante"-Hinweis aus. */
    public void showOpening(Opening opening) {
        currentLabel.setText(opening == null
                ? "Keine Eröffnung aktiv"
                : opening.family() + " – " + opening.variation() + " (" + opening.eco() + ")");
        setStatus("Spiele die Buchzüge.", NEUTRAL_STYLE);
        progressLabel.setText("");
        showNode(retryButton, false);
        showNode(nextLabel, false);
    }

    /**
     * Nach durchgespielter Buchlinie: {@code next} = die nächste Variante derselben Eröffnung, oder
     * {@code null}, wenn es die letzte war.
     */
    public void showNextVariation(Opening next) {
        nextLabel.setText(next == null
                ? "Letzte Variante – Eröffnung komplett durchgespielt."
                : "Nächste Variante: " + next.variation() + " (" + next.eco() + ")");
        showNode(nextLabel, true);
    }

    public void showFeedback(OpeningFeedback feedback) {
        switch (feedback.outcome()) {
            case CORRECT_CONTINUE -> {
                setStatus("Buchzug – weiter so.", GOOD_STYLE);
                showNode(retryButton, false);
            }
            case DEVIATION -> {
                setStatus("Falsch – nicht der Buchzug (erwartet: " + feedback.expectedUci() + ").", BAD_STYLE);
                showNode(retryButton, true);
                flashError();
            }
            case BOOK_FINISHED -> {
                setStatus("Buchlinie zu Ende – ab jetzt normale Zug-Bewertung.", NEUTRAL_STYLE);
                showNode(retryButton, false);
            }
            case NO_OPENING_DATA -> {
                setStatus("Keine Eröffnungsdaten vorhanden.", NEUTRAL_STYLE);
                showNode(retryButton, false);
            }
        }
        progressLabel.setText(feedback.movesTotal() > 0
                ? "Buchzug " + feedback.movesPlayed() + " von " + feedback.movesTotal()
                : "");
    }

    private void handleRetryClicked() {
        showNode(retryButton, false);
        setStatus("Nochmal – mach deinen Zug.", NEUTRAL_STYLE);
        if (onRetryRequested != null) {
            onRetryRequested.run();
        }
    }

    /** Kurzes rotes Aufblinken des Panel-Rands - Rückmeldung für einen Fehlzug. */
    private void flashError() {
        if (!getStyleClass().contains("opening-flash-error")) {
            getStyleClass().add("opening-flash-error");
        }
        PauseTransition pause = new PauseTransition(FLASH_DURATION);
        pause.setOnFinished(e -> getStyleClass().remove("opening-flash-error"));
        pause.play();
    }

    private void setStatus(String text, String styleClass) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll(GOOD_STYLE, BAD_STYLE, NEUTRAL_STYLE);
        statusLabel.getStyleClass().add(styleClass);
    }

    private static void showNode(javafx.scene.Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
