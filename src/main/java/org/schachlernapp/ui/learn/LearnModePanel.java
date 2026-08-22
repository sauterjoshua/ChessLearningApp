package org.schachlernapp.ui.learn;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
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

    private final Label feedbackLabel = new Label("Mach einen Zug, um Feedback zu erhalten.");
    private final Label suggestionLabel = new Label();
    private final Label tallyLabel = new Label("Gut: 0  Ungenau: 0  Fehler: 0  Blunder: 0");
    private final Button resetButton = new Button("Neue Runde");

    public LearnModePanel() {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(220);

        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        suggestionLabel.setWrapText(true);
        tallyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        getChildren().addAll(feedbackLabel, suggestionLabel, tallyLabel, resetButton);
    }

    public void showFeedback(MoveFeedback feedback) {
        feedbackLabel.setText(feedback.message());
        feedbackLabel.setTextFill(colorFor(feedback.quality()));
        suggestionLabel.setText(feedback.suggestedMoveSan() != null
                ? "Engine-Vorschlag: " + feedback.suggestedMoveSan()
                : "");
    }

    public void updateTally(int good, int inaccuracy, int mistake, int blunder) {
        tallyLabel.setText(String.format("Gut: %d  Ungenau: %d  Fehler: %d  Blunder: %d",
                good, inaccuracy, mistake, blunder));
    }

    public void setOnResetRequested(Runnable action) {
        resetButton.setOnAction(e -> action.run());
    }

    private static Color colorFor(MoveQuality quality) {
        return switch (quality) {
            case GOOD -> Color.web("#2e7d32");
            case INACCURACY -> Color.web("#c9a227");
            case MISTAKE -> Color.web("#e07b1f");
            case BLUNDER -> Color.web("#c62828");
        };
    }
}
