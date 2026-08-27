package org.schachlernapp.review;

import org.schachlernapp.analysis.MoveQuality;
import org.schachlernapp.engine.Evaluation;

/**
 * Analyse-Ergebnis für einen einzelnen Halbzug einer importierten Partie.
 *
 * @param halfMoveIndex 0-basierter Index in {@link ImportedGame#sanMoves()}
 * @param san           der gespielte Zug in SAN-Notation
 * @param evaluation    Engine-Bewertung der Stellung NACH diesem Halbzug (auf Weiß normalisiert)
 * @param quality       Einstufung wie im Lern-Modus (M5), aus demselben Vorher/Nachher-Eval-Paar
 * @param deltaCp       Eval-Änderung in Centipawns aus Sicht der ziehenden Seite (negativ = schlechter)
 */
public record HalfMoveReview(int halfMoveIndex, String san, Evaluation evaluation, MoveQuality quality, int deltaCp) {
}
