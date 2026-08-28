package org.schachlernapp.opening;

/**
 * Ergebnis eines Zuges im Eröffnungstrainer (M11) - Gegenstück zu
 * {@code org.schachlernapp.puzzle.PuzzleOutcome}.
 */
public enum OpeningOutcome {
    /** User-Zug war der Buchzug; der Trainer hat (falls noch Linie übrig) den Gegenzug gespielt. */
    CORRECT_CONTINUE,
    /** User-Zug wich vom Buchzug ab - die erwartete Fortsetzung steht in {@code OpeningFeedback.expectedUci()}. */
    DEVIATION,
    /** Buchlinie ist zu Ende - ab jetzt übernehmen die normalen M3/M5-Eval-/Blunder-Rückmeldungen. */
    BOOK_FINISHED,
    /** Es konnte keine Eröffnungslinie geladen werden (z.B. leere/fehlende {@code openings}-Tabelle). */
    NO_OPENING_DATA
}
