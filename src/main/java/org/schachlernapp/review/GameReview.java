package org.schachlernapp.review;

import org.schachlernapp.engine.Evaluation;

import java.util.List;

/**
 * Vollständiges Analyse-Ergebnis einer importierten Partie, wie es {@link GameReviewEngine#review}
 * liefert - eine {@link HalfMoveReview} pro Halbzug plus die Bewertung der Startstellung
 * (Graph-Punkt "vor dem ersten Zug").
 */
public record GameReview(ImportedGame game, Evaluation startEvaluation, List<HalfMoveReview> moves) {
}
