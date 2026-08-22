package org.schachlernapp.progress;

import org.schachlernapp.puzzle.PuzzleRatingService;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistenter Fortschritt (M7). Mutable POJO statt Record, damit Gson bei
 * fehlenden/zusätzlichen Feldern (ältere/neuere Dateiversion) einfach über
 * Reflection befüllt statt einen passenden Konstruktor zu brauchen.
 *
 * <p>{@code learnModeTally} ist als {@code Map<String,Integer>} statt
 * {@code Map<MoveQuality,Integer>} modelliert, damit Gson ohne einen
 * Enum-Key-TypeAdapter auskommt - Key ist {@code MoveQuality.name()}.</p>
 *
 * <p>{@code lastLessonId} ist für M6 (Lektionen) reserviert - existiert im
 * Code aktuell nicht, das Feld wird nirgends gesetzt oder gelesen.</p>
 */
public class ProgressData {

    private int puzzleRating = PuzzleRatingService.DEFAULT_STARTING_RATING;
    private Map<String, Integer> learnModeTally = new HashMap<>();
    private String lastLessonId;

    public int getPuzzleRating() {
        return puzzleRating;
    }

    public void setPuzzleRating(int puzzleRating) {
        this.puzzleRating = puzzleRating;
    }

    public Map<String, Integer> getLearnModeTally() {
        return learnModeTally;
    }

    public void setLearnModeTally(Map<String, Integer> learnModeTally) {
        this.learnModeTally = learnModeTally;
    }

    public String getLastLessonId() {
        return lastLessonId;
    }

    public void setLastLessonId(String lastLessonId) {
        this.lastLessonId = lastLessonId;
    }
}
