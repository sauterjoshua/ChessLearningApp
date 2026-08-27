package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

/**
 * Ein einzelnes Brettfeld: Hintergrundfarbe + Figuren-Bild. Rein visuell -
 * kennt weder Spielregeln noch andere Felder, sondern spiegelt nur den
 * Zustand wider, den {@link BoardView} ihr über {@link #setPiece(Piece)}
 * bzw. die Highlight-Methoden vorgibt. Größe wird von {@link BoardView} über
 * das umgebende {@code GridPane} vorgegeben - das Figuren-Bild skaliert per
 * Breiten-Listener automatisch mit.
 *
 * <p>Feld-Farben kommen aus {@code style.css}/{@code dark-theme.css} (Klassen
 * {@code square-light}/{@code square-dark}, Pseudo-Klassen {@code selected}/
 * {@code check}/{@code legal-target}) statt aus Java-Code - so lässt sich das
 * Farbschema zentral in einer Datei anpassen. Die Figuren selbst ({@link PieceImages},
 * cburnett-Set) bringen ihre Farbe fest im Bild mit und werden daher bewusst
 * NICHT eingefärbt.</p>
 */
public class SquareView extends StackPane {

    /** Bildgröße relativ zur Feldbreite - auch von {@link BoardDragHandler} für die gezogene Figur genutzt. */
    static final double PIECE_SIZE_RATIO = 0.82;

    private static final PseudoClass SELECTED_CLASS = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass CHECK_CLASS = PseudoClass.getPseudoClass("check");
    private static final PseudoClass LEGAL_TARGET_CLASS = PseudoClass.getPseudoClass("legal-target");

    private final Square square;
    private final ImageView pieceView = new ImageView();

    public SquareView(Square square) {
        this.square = square;
        setAlignment(Pos.CENTER);
        getStyleClass().addAll("square", square.isLightSquare() ? "square-light" : "square-dark");
        pieceView.setMouseTransparent(true); // Maus-Events sollen an der SquareView ankommen, nicht am Bild
        pieceView.setPreserveRatio(true);
        getChildren().add(pieceView);
        updateSize(getWidth());
        widthProperty().addListener((obs, oldWidth, newWidth) -> updateSize(newWidth.doubleValue()));
    }

    public Square getSquare() {
        return square;
    }

    void setPiece(Piece piece) {
        boolean present = piece != null && piece != Piece.NONE;
        pieceView.setImage(present ? PieceImages.of(piece) : null);
        pieceView.setVisible(present);
    }

    void setPieceVisible(boolean visible) {
        pieceView.setVisible(visible);
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
        double size = Math.max(8, squareWidth * PIECE_SIZE_RATIO);
        pieceView.setFitWidth(size);
        pieceView.setFitHeight(size);
    }
}
