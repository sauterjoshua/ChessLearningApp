package org.schachlernapp.engine;

/**
 * Ergebnis einer Stockfish-Analyse für eine Stellung. {@code value} ist immer
 * auf Weiß normalisiert (positiv = Weiß im Vorteil bzw. Weiß setzt matt),
 * unabhängig davon, wer laut FEN am Zug war - UCI liefert Scores sonst aus
 * Sicht der ziehenden Seite, was für einen stabilen Eval-Balken und für den
 * Vorher/Nachher-Vergleich in der Blunder-Erkennung unbrauchbar wäre.
 *
 * @param fen die analysierte Stellung - wird u.a. gebraucht, um
 *            {@code bestMoveUci} im Lern-Modus in SAN umzuwandeln
 */
public record Evaluation(String fen, EvalType type, int value, int depth, String bestMoveUci) {

    public enum EvalType {
        CENTIPAWNS,
        MATE
    }
}
