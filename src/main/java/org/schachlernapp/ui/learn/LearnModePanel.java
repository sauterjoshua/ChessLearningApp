package org.schachlernapp.ui.learn;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.schachlernapp.analysis.MoveFeedback;
import org.schachlernapp.analysis.MoveQuality;

/**
 * Zeigt Feedback + Zugvorschlag des Lern-Modus an. Reine Anzeigekomponente,
 * ohne Engine-/Board-/Controller-Abhängigkeit (gleiches Muster wie
 * {@link org.schachlernapp.ui.eval.EvalBar}) - wird extern verdrahtet, weil
 * der zugehörige {@code LearnModeController} erst nach erfolgreichem
 * Stockfish-Start existiert, das Panel selbst aber schon beim App-Start
 * gebaut werden muss.
 */
public class LearnModePanel extends VBox {

    private static final String[] QUALITY_STYLE_CLASSES =
            {"text-good", "text-inaccuracy", "text-mistake", "text-blunder"};

    private final Label feedbackLabel = new Label("Mach einen Zug, um Feedback zu erhalten.");
    private final Label suggestionLabel = new Label();
    private final Label tallyLabel = new Label("Gut: 0  Ungenau: 0  Fehler: 0  Blunder: 0");

    public LearnModePanel() {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(220);
        getStyleClass().add("side-panel");

        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("panel-heading");
        suggestionLabel.setWrapText(true);
        tallyLabel.getStyleClass().add("panel-muted");

        getChildren().addAll(feedbackLabel, suggestionLabel, tallyLabel);
    }

    public void showFeedback(MoveFeedback feedback) {
        feedbackLabel.setText(feedback.message());
        setQualityStyleClass(feedback.quality());
        suggestionLabel.setText(feedback.suggestedMoveSan() != null
                ? "Engine-Vorschlag: " + feedback.suggestedMoveSan()
                : "");
    }

    public void updateTally(int good, int inaccuracy, int mistake, int blunder) {
        tallyLabel.setText(String.format("Gut: %d  Ungenau: %d  Fehler: %d  Blunder: %d",
                good, inaccuracy, mistake, blunder));
    }

    /**
     * M8: Zusammenfassung einer Partie-Analyse statt Live-Lern-Modus-Feedback zu einem einzelnen
     * Zug - {@code message} ersetzt den sonst per {@link #showFeedback} gesetzten Text (der beim
     * Öffnen einer Analyse sinnfrei "Mach einen Zug..." zeigen würde), Tally-Zahlen wie gewohnt.
     */
    public void showReviewSummary(String message, int good, int inaccuracy, int mistake, int blunder) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll(QUALITY_STYLE_CLASSES);
        suggestionLabel.setText("");
        updateTally(good, inaccuracy, mistake, blunder);
    }

    private void setQualityStyleClass(MoveQuality quality) {
        feedbackLabel.getStyleClass().removeAll(QUALITY_STYLE_CLASSES);
        feedbackLabel.getStyleClass().add(switch (quality) {
            case GOOD -> "text-good";
            case INACCURACY -> "text-inaccuracy";
            case MISTAKE -> "text-mistake";
            case BLUNDER -> "text-blunder";
        });
    }
}
