package org.schachlernapp.analysis;

/**
 * Feedback zu einem im Lern-Modus gespielten Zug.
 *
 * @param quality          Einstufung (gut/ungenau/Fehler/Blunder)
 * @param deltaCp          Eval-Änderung in Centipawns aus Sicht der ziehenden Seite
 * @param message          Feedback-Text passend zu {@code quality}
 * @param suggestedMoveSan Stockfishs bester Zug für die Stellung vor dem Zug, in SAN - {@code null} falls nicht ermittelbar
 */
public record MoveFeedback(MoveQuality quality, int deltaCp, String message, String suggestedMoveSan) {
}
