package org.schachlernapp.puzzle;

/**
 * Elo-ähnliche Anpassung des User-Ratings anhand von Erfolg/Misserfolg und der
 * Rating-Differenz zum Puzzle. In-Memory, wird über {@link org.schachlernapp.progress.ProgressStore}
 * (M7) persistiert.
 */
public class PuzzleRatingService {

    public static final int DEFAULT_STARTING_RATING = 1500;
    private static final int K_FACTOR = 24;

    // volatile: recordResult() läuft auf dem FX-Thread, rating() wird seit M7 zusätzlich
    // vom JVM-Shutdown-Hook-Thread gelesen (Main.saveProgress() als Fallback beim Beenden).
    private volatile int rating;

    public PuzzleRatingService() {
        this(DEFAULT_STARTING_RATING);
    }

    public PuzzleRatingService(int startingRating) {
        this.rating = startingRating;
    }

    public int rating() {
        return rating;
    }

    /** Passt das Rating an und gibt das Delta zurück. */
    public int recordResult(int puzzleRating, boolean solved) {
        double expected = 1.0 / (1.0 + Math.pow(10, (puzzleRating - rating) / 400.0));
        double actual = solved ? 1.0 : 0.0;
        int delta = (int) Math.round(K_FACTOR * (actual - expected));
        rating += delta;
        return delta;
    }
}
