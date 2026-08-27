package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.Piece;
import javafx.scene.image.Image;

import java.util.EnumMap;
import java.util.Map;

/**
 * Bildet chesslib-{@link Piece} auf das "cburnett"-Figuren-Set ab (Colin M. L. Burnett,
 * lizenziert unter GPLv2+ / CC-BY-SA 3.0 - Attribution siehe README, Abschnitt "Grafik-Assets").
 * Ursprünglich SVG (via lichess-org/lila), hier als PNG gebündelt, da JavaFX' {@link Image} kein
 * SVG laden kann. Alle 12 Bilder werden einmalig beim Klassenladen eingelesen und wiederverwendet
 * (kein Neuladen pro Feld/Zug) - einziger Umschaltpunkt, falls später ein anderes Set genutzt
 * werden soll; {@link SquareView}/{@link BoardDragHandler} kennen nur diese Klasse.
 */
final class PieceImages {

    private static final Map<Piece, Image> IMAGES = new EnumMap<>(Piece.class);

    static {
        put(Piece.WHITE_KING, "wK");
        put(Piece.WHITE_QUEEN, "wQ");
        put(Piece.WHITE_ROOK, "wR");
        put(Piece.WHITE_BISHOP, "wB");
        put(Piece.WHITE_KNIGHT, "wN");
        put(Piece.WHITE_PAWN, "wP");
        put(Piece.BLACK_KING, "bK");
        put(Piece.BLACK_QUEEN, "bQ");
        put(Piece.BLACK_ROOK, "bR");
        put(Piece.BLACK_BISHOP, "bB");
        put(Piece.BLACK_KNIGHT, "bN");
        put(Piece.BLACK_PAWN, "bP");
    }

    private PieceImages() {
    }

    private static void put(Piece piece, String fileBaseName) {
        IMAGES.put(piece, new Image(PieceImages.class.getResourceAsStream("/pieces/" + fileBaseName + ".png")));
    }

    /** {@code null} für {@code Piece.NONE}/unbekannte Werte - Aufrufer blenden das ImageView dann aus. */
    static Image of(Piece piece) {
        return IMAGES.get(piece);
    }
}
