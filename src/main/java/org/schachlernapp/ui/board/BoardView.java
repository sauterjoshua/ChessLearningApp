package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.File;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Rank;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;

import java.util.EnumMap;
import java.util.Map;

/**
 * 8x8-Schachbrett-Widget. Besteht aus dem {@link GridPane} mit den 64
 * {@link SquareView}-Feldern, einer transparenten {@link Pane} ("dragLayer"),
 * auf der {@link BoardDragHandler} die gerade gezogene Figur frei
 * positioniert, sowie Rand-Beschriftungen (a-h/1-8) als eigenständige
 * {@link Label}-Nodes. Rendert ausschließlich aus {@link BoardController} -
 * besitzt selbst keine Schachlogik.
 *
 * <p>Standard-Orientierung: Weiß unten. Über {@link #setFlipped(boolean)} kann
 * das Brett gedreht werden (z.B. für Puzzles, bei denen die lösende Seite
 * unten stehen soll) - die 64 {@link SquareView}-Objekte werden dabei nur
 * innerhalb des Grids umpositioniert (bestehende Listener/Zustand bleiben
 * erhalten), nicht neu erzeugt; die Rand-Labels folgen über denselben
 * Spalten/Zeilen-Mechanismus.</p>
 *
 * <p>Größenanpassung: Basisklasse ist bewusst {@link Pane} statt
 * {@code StackPane} - die Beschriftung braucht einen festen Rand nur auf
 * zwei Seiten (links/unten), den eine zentrierende StackPane nicht abbilden
 * kann. {@link #layoutChildren()} positioniert Grid, Drag-Layer und Labels
 * daher vollständig manuell über {@code resizeRelocate(...)}.</p>
 */
public class BoardView extends Pane {

    private static final int INITIAL_SQUARE_SIZE = 72;
    private static final double LABEL_MARGIN = 22;

    private final Map<Square, SquareView> squares = new EnumMap<>(Square.class);
    private final GridPane grid = new GridPane();
    private final Pane dragLayer = new Pane();
    private final Label[] fileLabels = new Label[8]; // Index = File.ordinal() (0=A..7=H)
    private final Label[] rankLabels = new Label[8]; // Index = Rank.ordinal() (0=Rang1..7=Rang8)
    private final BoardController controller;

    private double squareSize = INITIAL_SQUARE_SIZE;
    private boolean flipped;

    public BoardView(BoardController controller) {
        this.controller = controller;
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);

        buildGrid();
        buildLabels();
        dragLayer.setMouseTransparent(true); // Klicks sollen bei den SquareViews im Grid ankommen
        getChildren().addAll(grid, dragLayer);
        for (Label label : fileLabels) {
            getChildren().add(label);
        }
        for (Label label : rankLabels) {
            getChildren().add(label);
        }

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

    private void buildLabels() {
        for (int fileIndex = 0; fileIndex < 8; fileIndex++) {
            Label label = new Label(File.allFiles[fileIndex].getNotation().toLowerCase());
            label.setAlignment(Pos.CENTER);
            label.getStyleClass().add("board-coordinate");
            fileLabels[fileIndex] = label;
        }
        for (int rankIndex = 0; rankIndex < 8; rankIndex++) {
            Label label = new Label(Rank.allRanks[rankIndex].getNotation());
            label.setAlignment(Pos.CENTER);
            label.getStyleClass().add("board-coordinate");
            rankLabels[rankIndex] = label;
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
        requestLayout(); // Grid-Umsortierung allein löst keinen neuen Layout-Pass auf BoardView selbst aus - Labels müssten sonst stehen bleiben
    }

    // Weiß unten (Standard): Spalte = Linie A..H links->rechts, Zeile 0 = Reihe 8 (oben) .. Zeile 7 = Reihe 1 (unten).
    // Gedreht (Schwarz unten): beides gespiegelt.
    private int columnFor(Square square) {
        return columnForFile(square.getFile());
    }

    private int rowFor(Square square) {
        return rowForRank(square.getRank());
    }

    private int columnForFile(File file) {
        int fileIndex = file.ordinal();
        return flipped ? 7 - fileIndex : fileIndex;
    }

    private int rowForRank(Rank rank) {
        int rankIndex = rank.ordinal();
        return flipped ? rankIndex : 7 - rankIndex;
    }

    @Override
    protected void layoutChildren() {
        double available = Math.max(0, Math.floor(Math.min(getWidth(), getHeight())));
        double side = Math.max(0, available - LABEL_MARGIN);
        if (side <= 0) {
            return;
        }
        squareSize = side / 8;

        grid.setMinSize(side, side);
        grid.setPrefSize(side, side);
        grid.setMaxSize(side, side);
        grid.resizeRelocate(LABEL_MARGIN, 0, side, side);

        dragLayer.setMinSize(side, side);
        dragLayer.setPrefSize(side, side);
        dragLayer.setMaxSize(side, side);
        dragLayer.resizeRelocate(LABEL_MARGIN, 0, side, side);

        for (int fileIndex = 0; fileIndex < 8; fileIndex++) {
            int col = columnForFile(File.allFiles[fileIndex]);
            fileLabels[fileIndex].resizeRelocate(LABEL_MARGIN + col * squareSize, side, squareSize, LABEL_MARGIN);
        }
        for (int rankIndex = 0; rankIndex < 8; rankIndex++) {
            int row = rowForRank(Rank.allRanks[rankIndex]);
            rankLabels[rankIndex].resizeRelocate(0, row * squareSize, LABEL_MARGIN, squareSize);
        }
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
