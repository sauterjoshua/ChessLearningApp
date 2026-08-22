package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.File;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Rank;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;

import java.util.EnumMap;
import java.util.Map;

/**
 * 8x8-Schachbrett-Widget. Besteht aus zwei übereinanderliegenden Ebenen:
 * dem {@link GridPane} mit den 64 {@link SquareView}-Feldern und einer
 * transparenten {@link Pane} ("dragLayer"), auf der {@link BoardDragHandler}
 * die gerade gezogene Figur frei positioniert. Rendert ausschließlich aus
 * {@link BoardController} - besitzt selbst keine Schachlogik.
 *
 * <p>Standard-Orientierung: Weiß unten. Über {@link #setFlipped(boolean)} kann
 * das Brett gedreht werden (z.B. für Puzzles, bei denen die lösende Seite
 * unten stehen soll) - die 64 {@link SquareView}-Objekte werden dabei nur
 * innerhalb des Grids umpositioniert (bestehende Listener/Zustand bleiben
 * erhalten), nicht neu erzeugt.</p>
 *
 * <p>Größenanpassung: {@code grid} und {@code dragLayer} bekommen in
 * {@link #layoutChildren()} bei jedem Resize exakt dieselbe quadratische
 * Kantenlänge (= kleinere Seite der verfügbaren Fläche) als Min/Pref/Max
 * verpasst. Das hält beide Ebenen deckungsgleich - wichtig, da
 * {@link BoardDragHandler} Koordinaten zwischen ihnen umrechnet - und
 * erzwingt ein quadratisches Brett unabhängig vom Fensterformat.</p>
 */
public class BoardView extends StackPane {

    private static final int INITIAL_SQUARE_SIZE = 72;

    private final Map<Square, SquareView> squares = new EnumMap<>(Square.class);
    private final GridPane grid = new GridPane();
    private final Pane dragLayer = new Pane();
    private final BoardController controller;

    private double squareSize = INITIAL_SQUARE_SIZE;
    private boolean flipped;

    public BoardView(BoardController controller) {
        this.controller = controller;
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);

        buildGrid();
        dragLayer.setMouseTransparent(true); // Klicks sollen bei den SquareViews im Grid ankommen
        getChildren().addAll(grid, dragLayer);

        controller.addPositionChangedListener(reason -> render());
        new BoardDragHandler(this, controller);
        render();
    }

    private void buildGrid() {
        for (int i = 0; i < 8; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(100.0 / 8);
            grid.getColumnConstraints().add(col);

            RowConstraints row = new RowConstraints();
            row.setPercentHeight(100.0 / 8);
            grid.getRowConstraints().add(row);
        }
        for (Square square : Square.values()) {
            if (square == Square.NONE) {
                continue;
            }
            SquareView view = new SquareView(square);
            squares.put(square, view);
            grid.add(view, columnFor(square), rowFor(square));
        }
    }

    /**
     * Dreht das Brett um (Weiß unten &lt;-&gt; Schwarz unten). Positioniert die
     * bestehenden {@link SquareView}-Objekte nur innerhalb des Grids neu, statt
     * sie neu zu erzeugen - Drag&amp;Drop-Listener/Zustand bleiben unberührt.
     */
    public void setFlipped(boolean flipped) {
        if (this.flipped == flipped) {
            return;
        }
        this.flipped = flipped;
        for (Map.Entry<Square, SquareView> entry : squares.entrySet()) {
            Square square = entry.getKey();
            GridPane.setColumnIndex(entry.getValue(), columnFor(square));
            GridPane.setRowIndex(entry.getValue(), rowFor(square));
        }
    }

    // Weiß unten (Standard): Spalte = Linie A..H links->rechts, Zeile 0 = Reihe 8 (oben) .. Zeile 7 = Reihe 1 (unten).
    // Gedreht (Schwarz unten): beides gespiegelt.
    private int columnFor(Square square) {
        int fileIndex = square.getFile().ordinal();
        return flipped ? 7 - fileIndex : fileIndex;
    }

    private int rowFor(Square square) {
        int rankIndex = square.getRank().ordinal();
        return flipped ? rankIndex : 7 - rankIndex;
    }

    @Override
    protected void layoutChildren() {
        double side = Math.max(0, Math.floor(Math.min(getWidth(), getHeight())));
        if (side > 0) {
            squareSize = side / 8;
            grid.setMinSize(side, side);
            grid.setPrefSize(side, side);
            grid.setMaxSize(side, side);
            dragLayer.setMinSize(side, side);
            dragLayer.setPrefSize(side, side);
            dragLayer.setMaxSize(side, side);
        }
        super.layoutChildren();
    }

    private void render() {
        for (Map.Entry<Square, SquareView> entry : squares.entrySet()) {
            Piece piece = controller.pieceAt(entry.getKey());
            entry.getValue().setGlyph(PieceGlyphs.of(piece));
        }
        updateCheckHighlight();
        Platform.runLater(this::maybeShowGameOverDialog);
    }

    private void updateCheckHighlight() {
        for (SquareView view : squares.values()) {
            view.setInCheck(false);
        }
        if (controller.isCheck()) {
            squares.get(controller.kingSquare(controller.sideToMove())).setInCheck(true);
        }
    }

    private void maybeShowGameOverDialog() {
        String message;
        if (controller.isCheckmate()) {
            Side winner = controller.sideToMove() == Side.WHITE ? Side.BLACK : Side.WHITE;
            message = "Schachmatt! " + (winner == Side.WHITE ? "Weiß" : "Schwarz") + " gewinnt.";
        } else if (controller.isStalemate()) {
            message = "Patt - das Spiel endet remis.";
        } else if (controller.isDraw()) {
            message = "Remis.";
        } else {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText("Partie beendet");
        alert.showAndWait();
    }

    SquareView squareViewFor(Square square) {
        return squares.get(square);
    }

    Pane getDragLayer() {
        return dragLayer;
    }

    double getSquareSize() {
        return squareSize;
    }

    /** Wandelt Szenen-Koordinaten in Grid-lokale Koordinaten um (deckungsgleich mit dragLayer). */
    Point2D sceneToGrid(double sceneX, double sceneY) {
        return grid.sceneToLocal(sceneX, sceneY);
    }

    Point2D sceneToDragLayer(double sceneX, double sceneY) {
        return dragLayer.sceneToLocal(sceneX, sceneY);
    }

    /** Liefert das Feld unter der gegebenen Grid-lokalen Koordinate, oder {@code null} außerhalb des Bretts. */
    Square squareAt(Point2D gridLocal) {
        if (squareSize <= 0) {
            return null;
        }
        int col = (int) Math.floor(gridLocal.getX() / squareSize);
        int row = (int) Math.floor(gridLocal.getY() / squareSize);
        if (col < 0 || col > 7 || row < 0 || row > 7) {
            return null;
        }
        int fileIndex = flipped ? 7 - col : col;
        int rankIndex = flipped ? row : 7 - row;
        return Square.encode(Rank.allRanks[rankIndex], File.allFiles[fileIndex]);
    }
}
