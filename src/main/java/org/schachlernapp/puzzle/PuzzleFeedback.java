package org.schachlernapp.puzzle;

/**
 * @param expectedMoveUci nur bei {@link PuzzleOutcome#INCORRECT} befüllt
 * @param ratingDelta     nur bei den beiden Endzuständen (CORRECT_SOLVED/INCORRECT) ungleich 0
 */
public record PuzzleFeedback(PuzzleOutcome outcome, String expectedMoveUci, int ratingDelta) {
}
