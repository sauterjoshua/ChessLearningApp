package org.schachlernapp.analysis;

import com.github.bhlangonijr.chesslib.Side;
import org.schachlernapp.engine.Evaluation;

/**
 * Reine Vergleichslogik ohne Engine-/UI-Abhängigkeit - nimmt zwei bereits
 * berechnete {@link Evaluation}s und liefert ein Urteil. Dadurch trivial mit
 * konstruierten Evaluations testbar, ganz ohne laufenden Stockfish.
 */
public final class BlunderDetector {

    /** Mate-Bewertungen werden für den cp-Vergleich als extremer, aber endlicher Wert behandelt. */
    private static final int MATE_SCORE_MAGNITUDE = 100_000;

    private BlunderDetector() {
    }

    /**
     * Vergleicht die auf Weiß normalisierten Bewertungen vor und nach einem Zug
     * aus Sicht der ziehenden Seite ({@code moverSide}). Ein Blunder liegt vor,
     * wenn sich die Bewertung um mindestens {@code thresholdCp} zu ihren
     * Ungunsten verschlechtert hat.
     */
    public static BlunderJudgement classify(Evaluation before, Evaluation after, Side moverSide, int thresholdCp) {
        int deltaForMover = deltaForMover(before, after, moverSide);
        boolean isBlunder = deltaForMover <= -thresholdCp;
        return new BlunderJudgement(isBlunder, deltaForMover);
    }

    /**
     * Eval-Änderung in Centipawns aus Sicht der ziehenden Seite (negativ = schlechter für sie).
     * Von {@link #classify} und - für die feinere M5-Kategorisierung - von
     * {@link MoveQualityClassifier} genutzt, um die Verlust-Berechnung nicht zu duplizieren.
     */
    public static int deltaForMover(Evaluation before, Evaluation after, Side moverSide) {
        int deltaFromWhitePerspective = toComparableCp(after) - toComparableCp(before);
        return moverSide == Side.WHITE ? deltaFromWhitePerspective : -deltaFromWhitePerspective;
    }

    private static int toComparableCp(Evaluation eval) {
        if (eval.type() == Evaluation.EvalType.MATE) {
            return eval.value() >= 0 ? MATE_SCORE_MAGNITUDE : -MATE_SCORE_MAGNITUDE;
        }
        return eval.value();
    }
}
