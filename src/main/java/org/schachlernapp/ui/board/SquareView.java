package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.Square;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * Ein einzelnes Brettfeld: Hintergrundfarbe + Figuren-Glyph. Rein visuell -
 * kennt weder Spielregeln noch andere Felder, sondern spiegelt nur den
 * Zustand wider, den {@link BoardView} ihr über {@link #setGlyph(String)}
 * bzw. die Highlight-Methoden vorgibt. Größe wird von {@link BoardView} über
 * das umgebende {@code GridPane} vorgegeben - die Schrift skaliert per
 * Breiten-Listener automatisch mit.
 *
 * <p>Farben kommen aus {@code style.css} (Klassen {@code square-light}/
 * {@code square-dark}, Pseudo-Klassen {@code selected}/{@code check}/
 * {@code legal-target}) statt aus Java-Code - so lässt sich das Farbschema
 * zentral in einer Datei anpassen (z.B. auf chess.com-typische Farben).</p>
 */
public class SquareView extends StackPane {

    private static final PseudoClass SELECTED_CLASS = PseudoClass.getPseudoClass("selected");
    private static final PseudoClass CHECK_CLASS = PseudoClass.getPseudoClass("check");
    private static final PseudoClass LEGAL_TARGET_CLASS = PseudoClass.getPseudoClass("legal-target");

    private final Square square;
    private final Label pieceLabel = new Label();

    public SquareView(Square square) {
        this.square = square;
        setAlignment(Pos.CENTER);
        getStyleClass().addAll("square", square.isLightSquare() ? "square-light" : "square-dark");
        pieceLabel.setMouseTransparent(true); // Maus-Events sollen an der SquareView ankommen, nicht am Label
        getChildren().add(pieceLabel);
        updateFontSize(getWidth());
        widthProperty().addListener((obs, oldWidth, newWidth) -> updateFontSize(newWidth.doubleValue()));
    }

    public Square getSquare() {
        return square;
    }

    String getGlyph() {
        return pieceLabel.getText();
    }

    void setGlyph(String glyph) {
        pieceLabel.setText(glyph);
        pieceLabel.setVisible(true);
    }

    void setGlyphVisible(boolean visible) {
        pieceLabel.setVisible(visible);
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

    private void updateFontSize(double squareWidth) {
        double size = Math.max(8, squareWidth * 0.62);
        pieceLabel.setStyle("-fx-font-size: " + size + "px;");
    }
}
