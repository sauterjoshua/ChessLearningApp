package org.schachlernapp.puzzle;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

/**
 * {@link Move}&lt;-&gt;UCI-String. Verifiziert gegen echte chesslib-Aufrufe:
 * {@code Square.toString()}/{@code .value()} liefern Großschreibung ("E4"),
 * {@code Square.fromValue(...)} ist case-sensitiv (wirft bei "e4" statt "E4").
 * Wichtig: {@code Piece.NONE.getPieceType()} liefert {@code null} (nicht etwa
 * {@code PieceType.NONE}) - {@code getPieceType()} darf bei einem
 * Nicht-Umwandlungszug also nie direkt aufgerufen werden (per Smoke-Test
 * gegen alle Zugtypen der Startstellung verifiziert).
 */
final class UciMoveFormat {

    private UciMoveFormat() {
    }

    static String toUci(Move move) {
        Piece promotionPiece = move.getPromotion();
        String promotion = "";
        if (promotionPiece != null && promotionPiece != Piece.NONE) {
            promotion = switch (promotionPiece.getPieceType()) {
                case QUEEN -> "q";
                case ROOK -> "r";
                case BISHOP -> "b";
                case KNIGHT -> "n";
                default -> "";
            };
        }
        return move.getFrom().toString().toLowerCase() + move.getTo().toString().toLowerCase() + promotion;
    }

    static Square parseSquare(String uciMove, int offset) {
        return Square.fromValue(uciMove.substring(offset, offset + 2).toUpperCase());
    }
}
