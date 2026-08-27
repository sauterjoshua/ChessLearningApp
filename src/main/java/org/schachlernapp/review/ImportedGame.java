package org.schachlernapp.review;

import com.github.bhlangonijr.chesslib.Side;

import java.time.LocalDate;
import java.util.List;

/**
 * Eine über {@link GameImportService} importierte chess.com-Partie, bereits vollständig
 * in Stellungen aufgelöst - {@link GameReviewEngine} muss dafür kein PGN mehr parsen.
 *
 * @param url               chess.com-URL der Partie (Anzeige/Debug)
 * @param date              Datum, an dem die Partie beendet wurde
 * @param opponentUsername  chess.com-Username des Gegners
 * @param userSide          Farbe, mit der der importierende User gespielt hat
 * @param outcome           Ergebnis aus Sicht des Users
 * @param sanMoves          Züge in SAN-Notation, ein Eintrag pro Halbzug
 * @param fens              Stellungen als FEN - {@code fens.size() == sanMoves.size() + 1};
 *                          {@code fens.get(0)} ist die Startstellung, {@code fens.get(i+1)}
 *                          die Stellung nach {@code sanMoves.get(i)}
 */
public record ImportedGame(String url, LocalDate date, String opponentUsername, Side userSide,
                            GameOutcome outcome, List<String> sanMoves, List<String> fens) {

    @Override
    public String toString() {
        return date + " vs. " + opponentUsername + " (" + outcome + ")";
    }
}
