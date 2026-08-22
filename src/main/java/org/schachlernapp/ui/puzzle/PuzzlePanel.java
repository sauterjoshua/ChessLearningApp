package org.schachlernapp.ui.puzzle;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.schachlernapp.puzzle.PuzzleFeedback;
import org.schachlernapp.puzzle.PuzzleOutcome;
import org.schachlernapp.puzzle.PuzzleRatingService;

/**
 * Zeigt Puzzle-Feedback + Rating an. Reine Anzeigekomponente, ohne
 * Engine-/Board-/Controller-Abhängigkeit - extern verdrahtet (gleiches Muster
 * wie {@link org.schachlernapp.ui.eval.EvalBar}/{@link org.schachlernapp.ui.learn.LearnModePanel}).
 *
 * <p>Bei richtigem Zug blinkt das Panel kurz grün auf (siehe {@link #flashSuccess()},
 * ausgelöst von {@code CORRECT_CONTINUE}/{@code CORRECT_SOLVED}); {@code PuzzleSession}
 * lädt danach selbstständig das nächste Puzzle. Bei falschem Zug wird die Lösung
 * NICHT automatisch angezeigt - stattdessen erscheinen "Nochmal versuchen" und
 * "Auflösung zeigen".</p>
 */
public class PuzzlePanel extends VBox {

    private static final Duration FLASH_DURATION = Duration.millis(400);

    private final Label ratingLabel = new Label("Rating: " + PuzzleRatingService.DEFAULT_STARTING_RATING);
    private final Label feedbackLabel = new Label("Nutze \"Neues Puzzle\" rechts, um zu starten.");
    private final Button retryButton = new Button("Nochmal versuchen");
    private final Button revealButton = new Button("Auflösung zeigen");
    private final HBox retryBox = new HBox(6, retryButton, revealButton);

    private PuzzleFeedback lastIncorrectFeedback;
    private Runnable onRetryRequested;

    public PuzzlePanel() {
        setSpacing(8);
        setPadding(new Insets(8));
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(220);
        getStyleClass().add("side-panel");

        ratingLabel.getStyleClass().add("panel-muted");
        feedbackLabel.setWrapText(true);
        feedbackLabel.getStyleClass().add("panel-heading");

        retryBox.setAlignment(Pos.CENTER);
        retryBox.setVisible(false);
        retryBox.setManaged(false);
        retryButton.setOnAction(e -> handleRetryClicked());
        revealButton.setOnAction(e -> handleRevealClicked());

        getChildren().addAll(ratingLabel, feedbackLabel, retryBox);
    }

    public void showFeedback(PuzzleFeedback feedback) {
        switch (feedback.outcome()) {
            case CORRECT_CONTINUE -> {
                setFeedbackText("Richtig! Weiter geht's...", GOOD_COLOR);
                hideRetryOptions();
                flashSuccess();
            }
            case CORRECT_SOLVED -> {
                setFeedbackText("Gelöst! (" + formatDelta(feedback.ratingDelta()) + ")", GOOD_COLOR);
                hideRetryOptions();
                flashSuccess();
            }
            case INCORRECT -> {
                lastIncorrectFeedback = feedback;
                setFeedbackText("Falsch - versuch's nochmal oder lass dir die Lösung zeigen. ("
                        + formatDelta(feedback.ratingDelta()) + ")", BAD_COLOR);
                showRetryOptions();
            }
            case NO_PUZZLE_FOUND -> {
                setFeedbackText("Kein passendes Puzzle gefunden - bitte zuerst importieren.", NEUTRAL_COLOR);
                hideRetryOptions();
            }
        }
    }

    public void updateRating(int rating) {
        ratingLabel.setText("Rating: " + rating);
    }

    /** Wird bei "Nochmal versuchen" zusätzlich zum internen UI-Reset aufgerufen (soll {@code PuzzleSession.retryCurrentPuzzle()} anstoßen). */
    public void setOnRetryRequested(Runnable action) {
        this.onRetryRequested = action;
    }

    /** Kurzes grünes Aufblinken des Panel-Rands (~400ms) - Rückmeldung für einen korrekten Zug. */
    private void flashSuccess() {
        getStyleClass().add("puzzle-flash-success");
        PauseTransition pause = new PauseTransition(FLASH_DURATION);
        pause.setOnFinished(e -> getStyleClass().remove("puzzle-flash-success"));
        pause.play();
    }

    private void handleRetryClicked() {
        hideRetryOptions();
        setFeedbackText("Nochmal von vorn - mach deinen Zug.", NEUTRAL_COLOR);
        if (onRetryRequested != null) {
            onRetryRequested.run();
        }
    }

    private void handleRevealClicked() {
        if (lastIncorrectFeedback != null) {
            setFeedbackText("Die Lösung wäre " + lastIncorrectFeedback.expectedMoveUci() + " gewesen.", BAD_COLOR);
        }
    }

    private void showRetryOptions() {
        retryBox.setVisible(true);
        retryBox.setManaged(true);
    }

    private void hideRetryOptions() {
        retryBox.setVisible(false);
        retryBox.setManaged(false);
    }

    private void setFeedbackText(String text, Color color) {
        feedbackLabel.setText(text);
        feedbackLabel.setTextFill(color);
    }

    private static String formatDelta(int delta) {
        return (delta >= 0 ? "+" : "") + delta;
    }

    private static final Color GOOD_COLOR = Color.web("#2e7d32");
    private static final Color BAD_COLOR = Color.web("#c62828");
    private static final Color NEUTRAL_COLOR = Color.web("#666666");
}
