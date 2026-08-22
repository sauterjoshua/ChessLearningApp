package org.schachlernapp.analysis;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveConversionException;
import com.github.bhlangonijr.chesslib.move.MoveList;

/**
 * Wandelt eine UCI-Zugnotation (z.B. "e2e4", "e7e8q" - wie sie Stockfish in
 * {@code bestmove}/bestMoveUci liefert) für eine gegebene Stellung in SAN um
 * (z.B. "e4", "exd5", "Qh4#", "e8=Q"). Gegen echte chesslib-Aufrufe verifiziert.
 */
final class SanFormatter {

    private SanFormatter() {
    }

    /** Liefert {@code null}, wenn keine gültige UCI-Zugangabe vorliegt oder die Umwandlung fehlschlägt. */
    static String toSan(String fen, String uciMove, Side sideToMove) {
        if (fen == null || uciMove == null || uciMove.length() < 4) {
            return null;
        }
        try {
            Square from = Square.fromValue(uciMove.substring(0, 2).toUpperCase());
            Square to = Square.fromValue(uciMove.substring(2, 4).toUpperCase());
            Piece promotion = uciMove.length() >= 5
                    ? Piece.make(sideToMove, promotionPieceType(uciMove.charAt(4)))
                    : Piece.NONE;

            MoveList moveList = new MoveList(fen);
            moveList.add(new Move(from, to, promotion));
            return moveList.toSanArray()[0];
        } catch (IllegalArgumentException | MoveConversionException e) {
            return null; // z.B. unerwartetes UCI-Format - UI zeigt dann einfach keinen Vorschlag
        }
    }

    private static PieceType promotionPieceType(char letter) {
        return switch (Character.toLowerCase(letter)) {
            case 'q' -> PieceType.QUEEN;
            case 'r' -> PieceType.ROOK;
            case 'b' -> PieceType.BISHOP;
            case 'n' -> PieceType.KNIGHT;
            default -> PieceType.NONE;
        };
    }
}
