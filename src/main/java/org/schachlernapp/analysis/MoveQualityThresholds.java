package org.schachlernapp.analysis;

/**
 * Grenzwerte (in Centipawns Verlust aus Sicht der ziehenden Seite) für die
 * {@link MoveQuality}-Einstufung. {@code inaccuracyCp} ist im Auftrag nicht
 * vorgegeben - 50cp ist ein gängiger Richtwert zwischen "gut" und "Fehler"
 * (Lichess/chess.com nutzen eine ähnliche Größenordnung).
 */
public record MoveQualityThresholds(int inaccuracyCp, int mistakeCp, int blunderCp) {

    public static final MoveQualityThresholds DEFAULT = new MoveQualityThresholds(50, 100, 300);
}
