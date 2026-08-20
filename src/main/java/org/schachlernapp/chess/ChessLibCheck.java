package org.schachlernapp.chess;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Constants;

/**
 * Minimaler Funktionstest für chesslib: lädt die Standard-Startposition aus einem
 * FEN-String und gibt Brett + FEN zur Kontrolle auf der Konsole aus.
 */
public final class ChessLibCheck {

    private ChessLibCheck() {
    }

    public static void run() {
        System.out.println("--- chesslib-Test ---");
        try {
            Board board = new Board();
            board.loadFromFen(Constants.startStandardFENPosition);

            System.out.println("[chesslib] Start-FEN geladen: " + board.getFen());
            System.out.println(board);
            System.out.println("[chesslib] OK - Brettlogik einsatzbereit.");
        } catch (Exception e) {
            System.out.println("[chesslib] FEHLER: " + e);
        }
    }
}
