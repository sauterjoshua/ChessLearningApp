package org.schachlernapp.opening;

/**
 * Rückmeldung des {@link OpeningTrainerService} an die UI - Gegenstück zu
 * {@code org.schachlernapp.puzzle.PuzzleFeedback}.
 *
 * @param outcome     Art der Rückmeldung
 * @param expectedUci bei {@link OpeningOutcome#DEVIATION} der erwartete Buchzug (UCI), sonst {@code null}
 * @param movesPlayed Anzahl bereits gespielter Buch-Halbzüge (für eine "Zug x von y"-Anzeige)
 * @param movesTotal  Länge der Buchlinie in Halbzügen
 */
public record OpeningFeedback(OpeningOutcome outcome, String expectedUci, int movesPlayed, int movesTotal) {
}
