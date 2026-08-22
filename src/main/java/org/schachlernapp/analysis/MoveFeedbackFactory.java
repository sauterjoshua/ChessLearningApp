package org.schachlernapp.analysis;

import com.github.bhlangonijr.chesslib.Side;
import org.schachlernapp.engine.Evaluation;

/** Baut aus einem Vorher/Nachher-Eval-Paar das vollständige {@link MoveFeedback} für den Lern-Modus. */
final class MoveFeedbackFactory {

    private MoveFeedbackFactory() {
    }

    static MoveFeedback create(Evaluation before, Evaluation after, Side moverSide, MoveQualityThresholds thresholds) {
        MoveQuality quality = MoveQualityClassifier.classify(before, after, moverSide, thresholds);
        int delta = BlunderDetector.deltaForMover(before, after, moverSide);
        String suggestion = SanFormatter.toSan(before.fen(), before.bestMoveUci(), moverSide);
        return new MoveFeedback(quality, delta, messageFor(quality), suggestion);
    }

    private static String messageFor(MoveQuality quality) {
        return switch (quality) {
            case GOOD -> "Guter Zug!";
            case INACCURACY -> "Ungenau - es gab eine bessere Option.";
            case MISTAKE -> "Fehler - das kostet einiges an Stellung.";
            case BLUNDER -> "Blunder! Das war ein grober Fehler.";
        };
    }
}
