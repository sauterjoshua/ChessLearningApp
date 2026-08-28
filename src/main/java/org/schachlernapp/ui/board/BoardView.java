package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.File;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Rank;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

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
    /** Transparente Overlay-Ebene für den generischen Hinweis-Pfeil ({@link #showHintArrow}) - deckungsgleich mit {@link #dragLayer}. */
    private final Pane hintLayer = new Pane();
    private final Label[] fileLabels = new Label[8]; // Index = File.ordinal() (0=A..7=H)
    private final Label[] rankLabels = new Label[8]; // Index = Rank.ordinal() (0=Rang1..7=Rang8)
    private final BoardController controller;

    private static final Duration MOVE_ANIMATION_DURATION = Duration.millis(110);

    private double squareSize = INITIAL_SQUARE_SIZE;
    private boolean flipped;

    // Aktuell angezeigter Hinweis-Pfeil (beide null = kein Pfeil). Als Square-Paar gehalten statt
    // als fertige Nodes, damit layoutChildren() den Pfeil bei jeder Größenänderung neu zeichnen kann.
    private Square hintFrom;
    private Square hintTo;

    // Zustand der laufenden Zug-Animation (siehe animateMove()) - alle drei zusammen
    // null oder alle drei gesetzt.
    private TranslateTransition activeMoveAnimation;
    private ImageView floatingMovePiece;
    private SquareView animatingTargetSquare;

    public BoardView(BoardController controller) {
        this.controller = controller;
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);

        buildGrid();
        buildLabels();
        dragLayer.setMouseTransparent(true); // Klicks sollen bei den SquareViews im Grid ankommen
        hintLayer.setMouseTransparent(true);
        getChildren().addAll(grid, hintLayer, dragLayer);
        for (Label label : fileLabels) {
            getChildren().add(label);
        }
        for (Label label : rankLabels) {
            getChildren().add(label);
        }

        controller.addPositionChangedListener(this::handlePositionChanged);
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

        hintLayer.setMinSize(side, side);
        hintLayer.setPrefSize(side, side);
        hintLayer.setMaxSize(side, side);
        hintLayer.resizeRelocate(LABEL_MARGIN, 0, side, side);
        drawHintArrow(); // Feldgröße kann sich geändert haben - Pfeil neu aufbauen

        for (int fileIndex = 0; fileIndex < 8; fileIndex++) {
            int col = columnForFile(File.allFiles[fileIndex]);
            fileLabels[fileIndex].resizeRelocate(LABEL_MARGIN + col * squareSize, side, squareSize, LABEL_MARGIN);
        }
        for (int rankIndex = 0; rankIndex < 8; rankIndex++) {
            int row = rowForRank(Rank.allRanks[rankIndex]);
            rankLabels[rankIndex].resizeRelocate(0, row * squareSize, LABEL_MARGIN, squareSize);
        }
    }

    /**
     * Reagiert auf {@link BoardController#addPositionChangedListener}: bei einem echten
     * Spielzug ({@link ChangeReason#MOVE}) oder einem Eröffnungs-Buchzug
     * ({@link ChangeReason#OPENING}) gleitet die Figur animiert von Start- zu Zielfeld
     * ({@link #animateMove()}), bei allen anderen Gründen (Reset, Puzzle-Setup,
     * Partie-Review-Sprung) springt die Stellung sofort um ({@link #render()}). Ein
     * evtl. sichtbarer Hinweis-Pfeil wird bei JEDER Änderung entfernt.
     */
    private void handlePositionChanged(ChangeReason reason) {
        // Der Hinweis-Pfeil gehört immer nur zur aktuellen Stellung - bei JEDER Änderung
        // (eigener Zug, Gegenzug, Reset, Puzzle, Review) verschwindet er, unabhängig vom Feature.
        clearHintArrow();
        if (reason == ChangeReason.MOVE || reason == ChangeReason.OPENING) {
            animateMove();
        } else {
            cancelActiveMoveAnimation();
            render();
        }
    }

    /**
     * Zeigt einen generischen Hinweis-Pfeil von {@code from} nach {@code to} auf der
     * {@link #hintLayer}-Overlay-Ebene (dieselbe Technik/Geometrie wie die schwebende Figur
     * der Zug-Animation). Ersetzt einen ggf. schon sichtbaren Pfeil. Aktuell vom
     * {@code OpeningTrainerService} genutzt, bewusst feature-neutral gehalten.
     */
    public void showHintArrow(Square from, Square to) {
        this.hintFrom = from;
        this.hintTo = to;
        drawHintArrow();
    }

    /** Entfernt den Hinweis-Pfeil, falls einer sichtbar ist. Wird zusätzlich bei jeder Positionsänderung automatisch aufgerufen. */
    public void clearHintArrow() {
        this.hintFrom = null;
        this.hintTo = null;
        hintLayer.getChildren().clear();
    }

    private void drawHintArrow() {
        hintLayer.getChildren().clear();
        if (hintFrom == null || hintTo == null || hintFrom == hintTo || squareSize <= 0) {
            return;
        }
        double fromX = columnFor(hintFrom) * squareSize + squareSize / 2;
        double fromY = rowFor(hintFrom) * squareSize + squareSize / 2;
        double toX = columnFor(hintTo) * squareSize + squareSize / 2;
        double toY = rowFor(hintTo) * squareSize + squareSize / 2;

        double angle = Math.atan2(toY - fromY, toX - fromX);
        double headLength = squareSize * 0.36;
        double headHalfWidth = squareSize * 0.17;

        double shaftEndX = toX - headLength * 0.85 * Math.cos(angle);
        double shaftEndY = toY - headLength * 0.85 * Math.sin(angle);
        Line shaft = new Line(fromX, fromY, shaftEndX, shaftEndY);
        shaft.setStrokeWidth(Math.max(3, squareSize * 0.14));
        shaft.setStrokeLineCap(StrokeLineCap.ROUND);
        shaft.getStyleClass().add("hint-arrow");

        double baseX = toX - headLength * Math.cos(angle);
        double baseY = toY - headLength * Math.sin(angle);
        double perpX = Math.cos(angle + Math.PI / 2);
        double perpY = Math.sin(angle + Math.PI / 2);
        Polygon head = new Polygon(
                toX, toY,
                baseX + headHalfWidth * perpX, baseY + headHalfWidth * perpY,
                baseX - headHalfWidth * perpX, baseY - headHalfWidth * perpY);
        head.getStyleClass().add("hint-arrow");

        hintLayer.getChildren().addAll(shaft, head);
    }

    private void render() {
        for (Map.Entry<Square, SquareView> entry : squares.entrySet()) {
            entry.getValue().setPiece(controller.pieceAt(entry.getKey()));
        }
        updateCheckHighlight();
    }

    /**
     * Animiert den zuletzt gespielten Zug: aktualisiert zunächst wie {@link #render()} alle
     * Felder (inkl. Zielfeld auf das Endbild), blendet das Zielfeld dann kurz aus und lässt
     * stattdessen eine schwebende {@link ImageView} auf der {@link #dragLayer} (dieselbe
     * Technik wie {@link BoardDragHandler} beim manuellen Ziehen) vom Start- zum Zielfeld
     * gleiten. Danach wird die Ziel-{@link SquareView} wieder sichtbar geschaltet.
     *
     * <p>Bei Rochade wird nur die König-Figur animiert (der Turm springt sofort mit
     * {@link #render()} um) - eine zweite gleichzeitige Animation für den Turm ist für den
     * gewünschten Effekt nicht nötig.</p>
     */
    private void animateMove() {
        cancelActiveMoveAnimation();

        Move move = controller.lastMove();
        Piece movedPiece = move == null ? null : controller.pieceAt(move.getTo());
        if (move == null || movedPiece == null || movedPiece == Piece.NONE || squareSize <= 0) {
            render();
            return;
        }

        Square from = move.getFrom();
        Square to = move.getTo();
        render();

        SquareView targetView = squares.get(to);
        targetView.setPieceVisible(false);

        double size = Math.max(8, squareSize * SquareView.PIECE_SIZE_RATIO);
        ImageView floating = new ImageView(PieceImages.of(movedPiece));
        floating.setFitWidth(size);
        floating.setFitHeight(size);
        floating.setPreserveRatio(true);
        floating.setMouseTransparent(true);

        double fromX = columnFor(from) * squareSize + (squareSize - size) / 2;
        double fromY = rowFor(from) * squareSize + (squareSize - size) / 2;
        double toX = columnFor(to) * squareSize + (squareSize - size) / 2;
        double toY = rowFor(to) * squareSize + (squareSize - size) / 2;
        floating.setLayoutX(fromX);
        floating.setLayoutY(fromY);
        dragLayer.getChildren().add(floating);

        TranslateTransition transition = new TranslateTransition(MOVE_ANIMATION_DURATION, floating);
        transition.setInterpolator(Interpolator.EASE_OUT);
        transition.setToX(toX - fromX);
        transition.setToY(toY - fromY);
        transition.setOnFinished(event -> finishMoveAnimation(floating, targetView));

        floatingMovePiece = floating;
        animatingTargetSquare = targetView;
        activeMoveAnimation = transition;
        transition.play();
    }

    private void finishMoveAnimation(ImageView floating, SquareView targetView) {
        dragLayer.getChildren().remove(floating);
        targetView.setPieceVisible(true);
        if (floatingMovePiece == floating) {
            activeMoveAnimation = null;
            floatingMovePiece = null;
            animatingTargetSquare = null;
        }
    }

    /** Bricht eine laufende Zug-Animation ab und stellt den sichtbaren Zustand sofort her - nötig, falls ein neuer Zug (oder Reset/Puzzle) eintrifft, bevor die vorherige Animation fertig ist. */
    private void cancelActiveMoveAnimation() {
        if (activeMoveAnimation != null) {
            activeMoveAnimation.stop();
            activeMoveAnimation = null;
        }
        if (floatingMovePiece != null) {
            dragLayer.getChildren().remove(floatingMovePiece);
            floatingMovePiece = null;
        }
        if (animatingTargetSquare != null) {
            animatingTargetSquare.setPieceVisible(true);
            animatingTargetSquare = null;
        }
    }

    private void updateCheckHighlight() {
        for (SquareView view : squares.values()) {
            view.setInCheck(false);
        }
        if (controller.isCheck()) {
            squares.get(controller.kingSquare(controller.sideToMove())).setInCheck(true);
        }
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
