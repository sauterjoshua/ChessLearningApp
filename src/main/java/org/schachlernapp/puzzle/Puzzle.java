package org.schachlernapp.puzzle;

import java.util.List;

/**
 * Ein Lichess-Puzzle. {@code solutionMoves} ist die volle UCI-Zugliste aus dem
 * "Moves"-CSV-Feld - Index 0 ist der Gegner-Setup-Zug ab {@code fen} (noch nicht
 * die eigentliche Rätsel-Stellung!), der User löst ab Index 1, danach alternierend.
 */
public record Puzzle(String id, String fen, List<String> solutionMoves, int rating, List<String> themes) {
}
