package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.Piece;

import java.util.EnumMap;
import java.util.Map;

/**
 * Bildet chesslib-{@link Piece} auf Unicode-Schachsymbole ab. Einziger
 * Umschaltpunkt, falls später Bild-Assets (ImageView) statt Glyphen
 * verwendet werden sollen - BoardView/SquareView bleiben davon unberührt.
 */
final class PieceGlyphs {

    private static final Map<Piece, String> GLYPHS = new EnumMap<>(Piece.class);

    static {
        GLYPHS.put(Piece.WHITE_KING, "♔");
        GLYPHS.put(Piece.WHITE_QUEEN, "♕");
        GLYPHS.put(Piece.WHITE_ROOK, "♖");
        GLYPHS.put(Piece.WHITE_BISHOP, "♗");
        GLYPHS.put(Piece.WHITE_KNIGHT, "♘");
        GLYPHS.put(Piece.WHITE_PAWN, "♙");
        GLYPHS.put(Piece.BLACK_KING, "♚");
        GLYPHS.put(Piece.BLACK_QUEEN, "♛");
        GLYPHS.put(Piece.BLACK_ROOK, "♜");
        GLYPHS.put(Piece.BLACK_BISHOP, "♝");
        GLYPHS.put(Piece.BLACK_KNIGHT, "♞");
        GLYPHS.put(Piece.BLACK_PAWN, "♟");
    }

    private PieceGlyphs() {
    }

    static String of(Piece piece) {
        return GLYPHS.getOrDefault(piece, "");
    }
}
