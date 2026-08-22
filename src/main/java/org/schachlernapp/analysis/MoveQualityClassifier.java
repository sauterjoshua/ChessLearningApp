package org.schachlernapp.analysis;

import com.github.bhlangonijr.chesslib.Side;
import org.schachlernapp.engine.Evaluation;

/** Reine Einstufungslogik, ohne Engine-/UI-Abhängigkeit - wie {@link BlunderDetector} mit konstruierten Evaluations testbar. */
public final class MoveQualityClassifier {

    private MoveQualityClassifier() {
    }

    public static MoveQuality classify(Evaluation before, Evaluation after, Side moverSide, MoveQualityThresholds thresholds) {
        int loss = Math.max(0, -BlunderDetector.deltaForMover(before, after, moverSide));
        if (loss >= thresholds.blunderCp()) {
            return MoveQuality.BLUNDER;
        }
        if (loss >= thresholds.mistakeCp()) {
            return MoveQuality.MISTAKE;
        }
        if (loss >= thresholds.inaccuracyCp()) {
            return MoveQuality.INACCURACY;
        }
        return MoveQuality.GOOD;
    }
}
