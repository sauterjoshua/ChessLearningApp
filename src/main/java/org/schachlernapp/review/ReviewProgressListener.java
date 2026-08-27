package org.schachlernapp.review;

/** Fortschritts-Callback für {@link GameReviewEngine#review} - läuft auf dem aufrufenden (Hintergrund-)Thread. */
@FunctionalInterface
public interface ReviewProgressListener {

    void onProgress(int analyzedHalfMoves, int totalHalfMoves);
}
