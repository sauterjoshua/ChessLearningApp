package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.Square;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import java.util.Set;

/**
 * Manuelles Maus-Drag&amp;Drop (statt JavaFX' nativer Drag&amp;Drop-Gesten-API):
 * Press auf einer Figur hebt sie in die {@link BoardView#getDragLayer()
 * dragLayer} und lässt sie dem Mauszeiger folgen; Release berechnet das
 * Zielfeld aus der Mausposition und delegiert die Legalitätsprüfung
 * vollständig an {@link BoardController#tryMove(Square, Square)}. Diese
 * Klasse trifft selbst keine Regel-Entscheidungen - sie zeigt nur an, welche
 * Zielfelder legal sind, und setzt die UI bei einem abgelehnten Zug zurück.
 *
 * <p><b>Performance-Hinweis (M7, nur benannt, nicht behoben):</b> {@link #handlePress}
 * ({@code controller.legalDestinations(...)}) und {@link #handleRelease}
 * ({@code controller.tryMove(...)}) lösen für denselben Zug je einen eigenen
 * {@code board.legalMoves()}-Aufruf aus - die volle Zuggenerierung für die
 * Stellung läuft also zweimal pro Zug. Bei chesslibs Geschwindigkeit
 * (Submillisekunden) nicht spürbar, aber algorithmisch redundant.</p>
 */
final class BoardDragHandler {

    private final BoardView boardView;
    private final BoardController controller;
    private final Pane dragLayer;

    private Square originSquare;
    private Label floatingPiece;
    private Set<Square> highlightedTargets = Set.of();

    BoardDragHandler(BoardView boardView, BoardController controller) {
        this.boardView = boardView;
        this.controller = controller;
        this.dragLayer = boardView.getDragLayer();

        for (Square square : Square.values()) {
            if (square == Square.NONE) {
                continue;
            }
            SquareView view = boardView.squareViewFor(square);
            view.setOnMousePressed(this::handlePress);
            view.setOnMouseDragged(this::handleDrag);
            view.setOnMouseReleased(this::handleRelease);
        }
    }

    private void handlePress(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return; // nur Linksklick greift eine Figur - Rechtsklick etc. soll nichts auslösen
        }
        SquareView source = (SquareView) event.getSource();
        String glyph = source.getGlyph();
        if (glyph == null || glyph.isEmpty()) {
            return; // leeres Feld - nichts zu greifen
        }

        originSquare = source.getSquare();
        highlightedTargets = controller.legalDestinations(originSquare);
        source.setSelected(true);
        for (Square target : highlightedTargets) {
            boardView.squareViewFor(target).setLegalTarget(true);
        }

        source.setGlyphVisible(false);
        floatingPiece = new Label(glyph);
        floatingPiece.setStyle("-fx-font-size: " + Math.max(8, boardView.getSquareSize() * 0.62) + "px;");
        floatingPiece.setMouseTransparent(true);
        dragLayer.getChildren().add(floatingPiece);
        // CSS/Größe synchron erzwingen - sonst liefert getBoundsInLocal() vor dem ersten
        // Layout-Pulse noch (0,0) und die Figur würde beim ersten Frame verschoben aufblitzen.
        floatingPiece.applyCss();
        floatingPiece.autosize();
        positionFloatingPiece(event);
    }

    private void handleDrag(MouseEvent event) {
        if (originSquare == null) {
            return;
        }
        positionFloatingPiece(event);
    }

    private void handleRelease(MouseEvent event) {
        if (originSquare == null) {
            return;
        }
        Point2D gridLocal = boardView.sceneToGrid(event.getSceneX(), event.getSceneY());
        Square target = boardView.squareAt(gridLocal);
        boolean moved = target != null && controller.tryMove(originSquare, target);
        cleanupDrag(moved);
    }

    private void cleanupDrag(boolean moved) {
        dragLayer.getChildren().remove(floatingPiece);
        floatingPiece = null;

        boardView.squareViewFor(originSquare).setSelected(false);
        for (Square target : highlightedTargets) {
            boardView.squareViewFor(target).setLegalTarget(false);
        }
        if (!moved) {
            // Erfolgreicher Zug lässt BoardController.render() ohnehin alle Glyphen neu setzen
            // (inkl. sichtbar machen); bei Ablehnung muss die Ursprungsfigur manuell zurückgeholt werden.
            boardView.squareViewFor(originSquare).setGlyphVisible(true);
        }

        originSquare = null;
        highlightedTargets = Set.of();
    }

    private void positionFloatingPiece(MouseEvent event) {
        Point2D local = boardView.sceneToDragLayer(event.getSceneX(), event.getSceneY());
        floatingPiece.setLayoutX(local.getX() - floatingPiece.getBoundsInLocal().getWidth() / 2);
        floatingPiece.setLayoutY(local.getY() - floatingPiece.getBoundsInLocal().getHeight() / 2);
    }
}
