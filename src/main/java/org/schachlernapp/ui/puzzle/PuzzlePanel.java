package org.schachlernapp.ui.puzzle;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.schachlernapp.puzzle.PuzzleFeedback;
import org.schachlernapp.puzzle.PuzzleOutcome;
import org.schachlernapp.puzzle.PuzzleRatingService;

/**
 * Zeigt Puzzle-Feedback + Rating an. Reine Anzeigekomponente, ohne
 * Engine-/Board-/Controller-Abhängigkeit - extern verdrahtet (gleiches Muster
 * wie {@link org.schachlernapp.ui.eval.EvalBar}/{@link org.schachlernapp.ui.learn.LearnModePanel}).
 */
public class PuzzlePanel extends VBox {

    private final Label ratingLabel = new Label("Rating: " + PuzzleRatingService.DEFAULT_STARTING_RATING);
    private final Label feedbackLabel = new Label("Klicke auf \"Neues Puzzle\", um zu starten.");
    private final Button nextButton = new Button("Neues Puzzle");

    public PuzzlePanel() {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(220);

        ratingLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        feedbackLabel.setWrapText(true);
        feedbackLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        getChildren().addAll(ratingLabel, feedbackLabel, nextButton);
    }

    public void showFeedback(PuzzleFeedback feedback) {
        feedbackLabel.setText(messageFor(feedback));
        feedbackLabel.setTextFill(colorFor(feedback.outcome()));
    }

    public void updateRating(int rating) {
        ratingLabel.setText("Rating: " + rating);
    }

    public void setOnNextPuzzleRequested(Runnable action) {
        nextButton.setOnAction(e -> action.run());
    }

    private static String messageFor(PuzzleFeedback feedback) {
        return switch (feedback.outcome()) {
            case CORRECT_CONTINUE -> "Richtig! Weiter geht's...";
            case CORRECT_SOLVED -> "Gelöst! (" + formatDelta(feedback.ratingDelta()) + ")";
            case INCORRECT -> "Falsch. Die Lösung wäre " + feedback.expectedMoveUci()
                    + " gewesen. (" + formatDelta(feedback.ratingDelta()) + ")";
            case NO_PUZZLE_FOUND -> "Kein passendes Puzzle gefunden - bitte zuerst importieren.";
        };
    }

    private static String formatDelta(int delta) {
        return (delta >= 0 ? "+" : "") + delta;
    }

    private static Color colorFor(PuzzleOutcome outcome) {
        return switch (outcome) {
            case CORRECT_CONTINUE, CORRECT_SOLVED -> Color.web("#2e7d32");
            case INCORRECT -> Color.web("#c62828");
            case NO_PUZZLE_FOUND -> Color.web("#666666");
        };
    }
}
