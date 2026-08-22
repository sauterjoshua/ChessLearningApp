package org.schachlernapp.puzzle;

/** {@code themeContains}: leer/{@code null} = kein Themen-Filter. */
public record PuzzleFilter(int minRating, int maxRating, String themeContains) {

    public static PuzzleFilter aroundRating(int rating, int range) {
        return new PuzzleFilter(rating - range, rating + range, "");
    }
}
