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
 */
public enum ChangeReason {
    MOVE,
    RESET,
    PUZZLE
}
