package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.Piece;

import java.util.EnumMap;
import java.util.Map;

/**
 * Bildet chesslib-{@link Piece} auf Unicode-Schachsymbole ab. Einziger
 * Umschaltpunkt, falls später Bild-Assets (ImageView) statt Glyphen
 * verwendet werden sollen - BoardView/SquareView bleiben davon unberührt.
 *
 * <p>Weiß und Schwarz nutzen bewusst dieselben "gefüllten" Glyphen (♚♛♜♝♞♟)
 * statt der eigentlich für Weiß vorgesehenen Konturzeichen (♔♕♖♗♘♙) - Letztere
 * sind reine Umrisse ohne Füllfläche und wirken besonders bei kleineren
 * Feldgrößen dünn/schlecht erkennbar. Die tatsächliche Farbunterscheidung
 * übernimmt {@link SquareView} über CSS-Füllfarbe + Kontur
 * ({@code piece-white}/{@code piece-black} in {@code style.css}).</p>
 */
final class PieceGlyphs {

    /**
     * Variation Selector-15 (VS15): erzwingt die monochrome Text-Darstellung
     * statt einer evtl. farbigen Emoji-Variante. Betrifft in der Praxis vor
     * allem den Bauern (U+265F) - dessen Codepoint hat auf vielen Systemen
     * zusätzlich eine Farb-Emoji-Schriftart im Fallback (z.B. Noto Color
     * Emoji), während König/Dame/Turm/Läufer/Springer keine solche
     * Alternative haben und deshalb schon vorher konsistent gerendert wurden.
     */
    private static final String TEXT_STYLE = "︎";

    private static final Map<Piece, String> GLYPHS = new EnumMap<>(Piece.class);

    static {
        GLYPHS.put(Piece.WHITE_KING, "♚" + TEXT_STYLE);
        GLYPHS.put(Piece.WHITE_QUEEN, "♛" + TEXT_STYLE);
        GLYPHS.put(Piece.WHITE_ROOK, "♜" + TEXT_STYLE);
        GLYPHS.put(Piece.WHITE_BISHOP, "♝" + TEXT_STYLE);
        GLYPHS.put(Piece.WHITE_KNIGHT, "♞" + TEXT_STYLE);
        GLYPHS.put(Piece.WHITE_PAWN, "♟" + TEXT_STYLE);
        GLYPHS.put(Piece.BLACK_KING, "♚" + TEXT_STYLE);
        GLYPHS.put(Piece.BLACK_QUEEN, "♛" + TEXT_STYLE);
        GLYPHS.put(Piece.BLACK_ROOK, "♜" + TEXT_STYLE);
        GLYPHS.put(Piece.BLACK_BISHOP, "♝" + TEXT_STYLE);
        GLYPHS.put(Piece.BLACK_KNIGHT, "♞" + TEXT_STYLE);
        GLYPHS.put(Piece.BLACK_PAWN, "♟" + TEXT_STYLE);
    }

    private PieceGlyphs() {
    }

    static String of(Piece piece) {
        return GLYPHS.getOrDefault(piece, "");
    }
}
