package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

/**
 * Ein einzelnes Brettfeld: Hintergrundfarbe + Figuren-Glyph. Rein visuell -
 * kennt weder Spielregeln noch andere Felder, sondern spiegelt nur den
 * Zustand wider, den {@link BoardView} ihr über {@link #setPiece(Piece)}
 * bzw. die Highlight-Methoden vorgibt. Größe wird von {@link BoardView} über
 * das umgebende {@code GridPane} vorgegeben - Schrift und Kontur skalieren
 * per Breiten-Listener automatisch mit.
 *
 * <p>Farben kommen aus {@code style.css} (Klassen {@code square-light}/
 * {@code square-dark}, Pseudo-Klassen {@code selected}/{@code check}/
 * {@code legal-target}) statt aus Java-Code - so lässt sich das Farbschema
 * zentral in einer Datei anpassen (z.B. auf chess.com-typische Farben).</p>
 *
 * <p>Die Figur ist ein {@link Text} statt eines {@code Label} - nur {@code Text}
 * unterstützt {@code -fx-fill}/{@code -fx-stroke} als CSS-Eigenschaften, was für
 * die Weiß/Schwarz-Unterscheidung über {@code piece-white}/{@code piece-black}
 * gebraucht wird (siehe {@link PieceGlyphs} für den Hintergrund dazu).</p>
 */
public class SquareView extends StackPane {

    /** Schriftgröße relativ zur Feldbreite - auch von {@link BoardDragHandler} für die gezogene Figur genutzt. */
    static final double PIECE_FONT_RATIO = 0.82;
    static final double PIECE_STROKE_RATIO = 0.035;

    private static final PseudoClass SELECTED_CLASS = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass CHECK_CLASS = PseudoClass.getPseudoClass("check");
    private static final PseudoClass LEGAL_TARGET_CLASS = PseudoClass.getPseudoClass("legal-target");

    private final Square square;
    private final Text pieceText = new Text();

    public SquareView(Square square) {
        this.square = square;
        setAlignment(Pos.CENTER);
        getStyleClass().addAll("square", square.isLightSquare() ? "square-light" : "square-dark");
        pieceText.setMouseTransparent(true); // Maus-Events sollen an der SquareView ankommen, nicht am Text
        pieceText.setTextOrigin(VPos.CENTER); // sonst zentriert StackPane über der Baseline statt der Glyphenmitte
        getChildren().add(pieceText);
        updateSize(getWidth());
        widthProperty().addListener((obs, oldWidth, newWidth) -> updateSize(newWidth.doubleValue()));
    }

    public Square getSquare() {
        return square;
    }

    void setPiece(Piece piece) {
        pieceText.setText(PieceGlyphs.of(piece));
        pieceText.getStyleClass().removeAll("piece-white", "piece-black");
        if (piece != null && piece != Piece.NONE) {
            pieceText.getStyleClass().add(piece.getPieceSide() == Side.WHITE ? "piece-white" : "piece-black");
        }
        pieceText.setVisible(true);
    }

    void setGlyphVisible(boolean visible) {
        pieceText.setVisible(visible);
    }

    void setSelected(boolean selected) {
        pseudoClassStateChanged(SELECTED_CLASS, selected);
    }

    void setInCheck(boolean inCheck) {
        pseudoClassStateChanged(CHECK_CLASS, inCheck);
    }

    void setLegalTarget(boolean legalTarget) {
        pseudoClassStateChanged(LEGAL_TARGET_CLASS, legalTarget);
    }

    private void updateSize(double squareWidth) {
        double fontSize = Math.max(8, squareWidth * PIECE_FONT_RATIO);
        pieceText.setStyle("-fx-font-size: " + fontSize + "px;");
        // Kontur relativ zur Schriftgröße statt fest - sonst bei kleinen Brettern zu dick, bei großen zu dünn.
        pieceText.setStrokeWidth(Math.max(0.6, fontSize * PIECE_STROKE_RATIO));
    }
}
