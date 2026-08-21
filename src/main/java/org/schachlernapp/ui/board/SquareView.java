package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.Square;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Ein einzelnes Brettfeld: Hintergrundfarbe + Figuren-Glyph. Rein visuell -
 * kennt weder Spielregeln noch andere Felder, sondern spiegelt nur den
 * Zustand wider, den {@link BoardView} ihr über {@link #setGlyph(String)}
 * bzw. die Highlight-Methoden vorgibt. Größe wird von {@link BoardView} über
 * das umgebende {@code GridPane} vorgegeben - die Schrift skaliert per
 * Breiten-Listener automatisch mit.
 */
public class SquareView extends StackPane {

    private static final Color LIGHT = Color.web("#f0d9b5");
    private static final Color DARK = Color.web("#b58863");
    private static final Color SELECTED = Color.web("#f7ec6e");
    private static final Color CHECK = Color.web("#e05252");
    private static final Color TARGET_MARK = Color.web("#2e7d32");

    private final Square square;
    private final boolean lightSquare;
    private final Label pieceLabel = new Label();

    private boolean selected;
    private boolean inCheck;

    public SquareView(Square square) {
        this.square = square;
        this.lightSquare = square.isLightSquare();
        setAlignment(Pos.CENTER);
        pieceLabel.setMouseTransparent(true); // Maus-Events sollen an der SquareView ankommen, nicht am Label
        getChildren().add(pieceLabel);
        applyBackground();
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
        this.selected = selected;
        applyBackground();
    }

    void setInCheck(boolean inCheck) {
        this.inCheck = inCheck;
        applyBackground();
    }

    void setLegalTarget(boolean legalTarget) {
        setBorder(legalTarget
                ? new Border(new BorderStroke(TARGET_MARK, BorderStrokeStyle.SOLID,
                        CornerRadii.EMPTY, new BorderWidths(3)))
                : Border.EMPTY);
    }

    private void applyBackground() {
        Color color = selected ? SELECTED : inCheck ? CHECK : (lightSquare ? LIGHT : DARK);
        setBackground(new Background(new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private void updateFontSize(double squareWidth) {
        double size = Math.max(8, squareWidth * 0.62);
        pieceLabel.setStyle("-fx-font-size: " + size + "px;");
    }
}
