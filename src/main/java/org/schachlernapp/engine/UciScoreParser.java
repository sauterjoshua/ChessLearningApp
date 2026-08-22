package org.schachlernapp.engine;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reine Parsing-Funktionen für UCI-Ausgabezeilen von Stockfish. Kennt weder
 * den Prozess noch Normalisierung auf eine Spielerseite (das übernimmt
 * {@link EngineEvaluator}) - dadurch ohne laufenden Stockfish testbar.
 */
final class UciScoreParser {

    private static final Pattern SCORE_MATE = Pattern.compile("score mate (-?\\d+)");
    private static final Pattern SCORE_CP = Pattern.compile("score cp (-?\\d+)");
    private static final Pattern DEPTH = Pattern.compile("depth (\\d+)");
    private static final Pattern BESTMOVE = Pattern.compile("bestmove (\\S+)");

    private UciScoreParser() {
    }

    /** Extrahiert Score (cp oder mate) + Tiefe aus einer "info ..."-Zeile, falls vorhanden. */
    static Optional<ScoreInfo> parseInfoLine(String line) {
        int depth = 0;
        Matcher depthMatcher = DEPTH.matcher(line);
        if (depthMatcher.find()) {
            depth = Integer.parseInt(depthMatcher.group(1));
        }

        Matcher mateMatcher = SCORE_MATE.matcher(line);
        if (mateMatcher.find()) {
            return Optional.of(new ScoreInfo(Evaluation.EvalType.MATE, Integer.parseInt(mateMatcher.group(1)), depth));
        }

        Matcher cpMatcher = SCORE_CP.matcher(line);
        if (cpMatcher.find()) {
            return Optional.of(new ScoreInfo(Evaluation.EvalType.CENTIPAWNS, Integer.parseInt(cpMatcher.group(1)), depth));
        }

        return Optional.empty();
    }

    /** Extrahiert den Zug aus einer "bestmove ..."-Zeile, falls vorhanden. */
    static Optional<String> parseBestMove(String line) {
        Matcher matcher = BESTMOVE.matcher(line);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    /** Rohes Zwischenergebnis - Score noch aus Sicht der ziehenden Seite, nicht normalisiert. */
    record ScoreInfo(Evaluation.EvalType type, int value, int depth) {
    }
}
