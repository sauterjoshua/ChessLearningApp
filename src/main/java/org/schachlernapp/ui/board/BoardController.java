package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Constants;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.EnumSet;
import java.util.Set;

/**
 * Einzige Quelle der Wahrheit für den Brettzustand. Kapselt chesslib's
 * {@link Board} vollständig - BoardView/BoardDragHandler lesen den Zustand
 * nur über diese Klasse und lösen Züge ausschließlich über
 * {@link #tryMove(Square, Square)} aus, damit UI und Bibliotheks-Zustand nie
 * auseinanderlaufen.
 */
public class BoardController {

    private final Board board = new Board();
    private Runnable onPositionChanged;

    public BoardController() {
        board.loadFromFen(Constants.startStandardFENPosition);
    }

    /** Wird nach jedem erfolgreichen Zug bzw. FEN-Ladevorgang aufgerufen. */
    public void setOnPositionChanged(Runnable listener) {
        this.onPositionChanged = listener;
    }

    public void loadFen(String fen) {
        board.loadFromFen(fen);
        fireChanged();
    }

    public Piece pieceAt(Square square) {
        return board.getPiece(square);
    }

    public Side sideToMove() {
        return board.getSideToMove();
    }

    /** true, wenn die Seite am Zug im Schach steht (unabhängig davon, ob sie noch ziehen kann). */
    public boolean isCheck() {
        return board.isKingAttacked();
    }

    public boolean isCheckmate() {
        return board.isMated();
    }

    public boolean isStalemate() {
        return board.isStaleMate();
    }

    /** Deckt Patt, Zugwiederholung, 50-Züge-Regel und ungenügendes Material ab. */
    public boolean isDraw() {
        return board.isDraw();
    }

    public Square kingSquare(Side side) {
        return board.getKingSquare(side);
    }

    /** Zielfelder, auf die die Figur auf {@code from} laut Bibliothek legal ziehen darf. */
    public Set<Square> legalDestinations(Square from) {
        Set<Square> targets = EnumSet.noneOf(Square.class);
        for (Move move : board.legalMoves()) {
            if (move.getFrom() == from) {
                targets.add(move.getTo());
            }
        }
        return targets;
    }

    /**
     * Versucht den Zug from-&gt;to auszuführen. Existieren dafür mehrere legale
     * Varianten (Bauernumwandlung), wird die Dame bevorzugt. Gibt zurück, ob
     * ein Zug tatsächlich ausgeführt wurde - Aufrufer müssen bei {@code false}
     * die UI selbst zurücksetzen (kein Zustandswechsel erfolgt).
     */
    public boolean tryMove(Square from, Square to) {
        Move chosen = null;
        for (Move move : board.legalMoves()) {
            if (move.getFrom() == from && move.getTo() == to) {
                if (chosen == null || move.getPromotion().getPieceType() == PieceType.QUEEN) {
                    chosen = move;
                }
            }
        }
        if (chosen == null) {
            return false;
        }
        boolean applied = board.doMove(chosen);
        if (applied) {
            fireChanged();
        }
        return applied;
    }

    private void fireChanged() {
        if (onPositionChanged != null) {
            onPositionChanged.run();
        }
    }
}
