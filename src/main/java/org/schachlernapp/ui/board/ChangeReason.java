package org.schachlernapp.ui.board;

/**
 * Grund, warum {@link BoardController} eine Positionsänderung gemeldet hat.
 * Wichtig für M3: nur {@link #MOVE} soll einen Eval-Vergleich (Blunder-Check)
 * auslösen - {@link #RESET} (z.B. Puzzle-FEN laden) hat keine sinnvolle
 * "Vorher"-Stellung im selben Spiel und muss die Baseline nur neu setzen.
 *
 * <p>{@link #PUZZLE} (M4): programmatisch von {@code PuzzleSession} gespielte
 * Züge (Lichess-Setup-Zug + erzwungene Gegenantworten) - fällt für
 * {@code EvaluationController} automatisch in denselben "kein moverSide"-Zweig
 * wie {@link #RESET}, sodass diese Züge nie als User-Blunder gewertet werden.</p>
 *
 * <p>{@link #REVIEW} (M8): Sprung zu einer Stellung aus einer importierten/analysierten
 * Partie (Klick auf einen Zug/Punkt im Eval-Graph). Fällt aus demselben Grund wie
 * {@link #RESET}/{@link #PUZZLE} nicht in die Blunder-Auswertung.</p>
 *
 * <p>{@link #OPENING} (M11): programmatisch vom {@code OpeningTrainerService} gespielte
 * Buchzüge (Setup-Zug + automatische Gegenzüge aus der ECO-Linie). Fällt für
 * {@code EvaluationController} in denselben "kein moverSide"-Zweig wie {@link #RESET}/
 * {@link #PUZZLE}, sodass diese Züge nie als User-Blunder gewertet werden. Wird von
 * {@code BoardView} - anders als die übrigen Nicht-{@link #MOVE}-Gründe - wie ein echter
 * Zug animiert.</p>
 */
public enum ChangeReason {
    MOVE,
    RESET,
    PUZZLE,
    REVIEW,
    OPENING
}
