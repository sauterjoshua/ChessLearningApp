package org.schachlernapp.puzzle;

/**
 * Themen des Endgame-Untermenüs (M9) - deutsches Anzeigelabel (Button-Text) plus den zwei
 * Lichess-Puzzle-Theme-Schlüsseln, die {@link PuzzleRepository#randomByThemes(String, String)}
 * per UND verknüpft (meist Endspiel-Typ + {@code "mate"}, bei {@link #PAWN_PROMOTION} stattdessen
 * Endspiel-Typ + {@code "promotion"}).
 */
public enum EndgameTheme {
    PAWN("Bauernendspiel", "pawnEndgame", "mate"),
    PAWN_PROMOTION("Bauernendspiel (Promotion)", "pawnEndgame", "promotion"),
    ROOK("Turmendspiel", "rookEndgame", "mate"),
    BISHOP("Läuferendspiel", "bishopEndgame", "mate"),
    KNIGHT("Springerendspiel", "knightEndgame", "mate"),
    QUEEN("Damenendspiel", "queenEndgame", "mate"),
    BISHOP_VS_KNIGHT("Läufer vs. Springer", "bishopVsKnightEndgame", "mate"),
    GENERAL("Allgemein", "endgame", "mate");

    private final String label;
    private final String theme1;
    private final String theme2;

    EndgameTheme(String label, String theme1, String theme2) {
        this.label = label;
        this.theme1 = theme1;
        this.theme2 = theme2;
    }

    public String label() {
        return label;
    }

    public String theme1() {
        return theme1;
    }

    public String theme2() {
        return theme2;
    }
}
