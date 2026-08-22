package org.schachlernapp.analysis;

/**
 * Ergebnis eines Vorher/Nachher-Eval-Vergleichs für einen einzelnen Zug.
 *
 * @param isBlunder true, wenn die Verschlechterung aus Sicht der ziehenden Seite den Schwellenwert erreicht/überschreitet
 * @param deltaCp   Eval-Änderung in Centipawns aus Sicht der ziehenden Seite (negativ = schlechter für sie)
 */
public record BlunderJudgement(boolean isBlunder, int deltaCp) {
}
