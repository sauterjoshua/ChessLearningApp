package org.schachlernapp.puzzle;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Geteiltes DDL - von {@link PuzzleCsvImporter} und {@link PuzzleRepository} genutzt, damit es nur an einer Stelle steht. */
final class PuzzleSchema {

    private PuzzleSchema() {
    }

    static void ensure(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS puzzles (
                        puzzle_id TEXT PRIMARY KEY,
                        fen       TEXT NOT NULL,
                        moves     TEXT NOT NULL,
                        rating    INTEGER NOT NULL,
                        themes    TEXT NOT NULL
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_puzzles_rating ON puzzles(rating)");
        }
    }
}
