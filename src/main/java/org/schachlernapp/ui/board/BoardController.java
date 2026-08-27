package org.schachlernapp.ui.board;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Constants;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Einzige Quelle der Wahrheit für den Brettzustand. Kapselt chesslib's
 * {@link Board} vollständig - BoardView/BoardDragHandler lesen den Zustand
 * nur über diese Klasse und lösen Züge ausschließlich über
 * {@link #tryMove(Square, Square)} aus, damit UI und Bibliotheks-Zustand nie
 * auseinanderlaufen.
 */
public class BoardController {

    private final Board board = new Board();
    private final List<Consumer<ChangeReason>> listeners = new ArrayList<>();
    private Move lastMove;

    public BoardController() {
        board.loadFromFen(Constants.startStandardFENPosition);
    }

    /**
     * Registriert einen Listener, der bei jeder Positionsänderung aufgerufen wird
     * (mehrere Abonnenten möglich, z.B. BoardView fürs Rendering und
     * EvaluationController fürs Auslösen der Engine-Analyse).
     */
    public void addPositionChangedListener(Consumer<ChangeReason> listener) {
        listeners.add(listener);
    }

    public void loadFen(String fen) {
        loadFen(fen, ChangeReason.RESET);
    }

    /** Wie {@link #loadFen(String)}, aber mit explizitem Grund (M8: {@link ChangeReason#REVIEW} für Partie-Analyse-Sprünge). */
    public void loadFen(String fen, ChangeReason reason) {
        board.loadFromFen(fen);
        fireChanged(reason);
    }

    public Piece pieceAt(Square square) {
        return board.getPiece(square);
    }

    public String currentFen() {
        return board.getFen();
    }

    public Side sideToMove() {
        return board.getSideToMove();
    }

    /** true, wenn die Seite am Zug im Schach steht (unabhängig davon, ob sie noch ziehen kann). */
    public boolean isCheck() {
        return board.isKingAttacked();
    }

    public Square kingSquare(Side side) {
        return board.getKingSquare(side);
    }

    /** Der zuletzt tatsächlich ausgeführte Zug (über {@link #tryMove} oder {@link #applyPuzzleMove}), oder {@code null}. */
    public Move lastMove() {
        return lastMove;
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
        return applyMove(from, to, ChangeReason.MOVE);
    }

    /**
     * Wie {@link #tryMove}, aber für programmatisch gespielte Puzzle-Züge
     * (Lichess-Setup-Zug + erzwungene Gegenantworten aus {@code PuzzleSession}).
     * Feuert {@link ChangeReason#PUZZLE} statt {@link ChangeReason#MOVE}, damit
     * diese Züge nicht als User-Zug in die Blunder-/Lern-Modus-Auswertung einfließen.
     */
    public boolean applyPuzzleMove(Square from, Square to) {
        return applyMove(from, to, ChangeReason.PUZZLE);
    }

    private boolean applyMove(Square from, Square to, ChangeReason reason) {
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
            lastMove = chosen;
            fireChanged(reason);
        }
        return applied;
    }

    private void fireChanged(ChangeReason reason) {
        for (Consumer<ChangeReason> listener : listeners) {
            listener.accept(reason);
        }
    }
}
