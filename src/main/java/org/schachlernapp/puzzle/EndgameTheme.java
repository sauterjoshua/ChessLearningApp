package org.schachlernapp.puzzle;

/**
 * Themen des Endgame-Untermenüs (M9) - deutsches Anzeigelabel (Button-Text) plus zugehöriger
 * Lichess-Puzzle-Theme-Schlüssel für {@link PuzzleRepository#randomEndgameMate(String)}.
 */
public enum EndgameTheme {
    PAWN("Bauernendspiel", "pawnEndgame"),
    ROOK("Turmendspiel", "rookEndgame"),
    BISHOP("Läuferendspiel", "bishopEndgame"),
    KNIGHT("Springerendspiel", "knightEndgame"),
    QUEEN("Damenendspiel", "queenEndgame"),
    BISHOP_VS_KNIGHT("Läufer vs. Springer", "bishopVsKnightEndgame"),
    GENERAL("Allgemein", "endgame");

    private final String label;
    private final String lichessTheme;

    EndgameTheme(String label, String lichessTheme) {
        this.label = label;
        this.lichessTheme = lichessTheme;
    }

    public String label() {
        return label;
    }

    public String lichessTheme() {
        return lichessTheme;
    }
}
