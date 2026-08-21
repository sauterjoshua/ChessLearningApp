package org.schachlernapp;

import org.schachlernapp.chess.ChessLibCheck;
import org.schachlernapp.engine.StockfishEngine;
import org.schachlernapp.ui.board.BoardController;
import org.schachlernapp.ui.board.BoardView;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Einstiegspunkt der Schach-Lernapp (Meilenstein 2: Brett-UI).
 * Zeigt das Schachbrett in der Start-Stellung an und prüft im Hintergrund,
 * ob chesslib und die lokale Stockfish-Installation funktionieren.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Schach-Lernapp");

        BoardController boardController = new BoardController();
        BoardView boardView = new BoardView(boardController);
        StackPane root = new StackPane(boardView);
        root.setAlignment(Pos.CENTER);

        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setMinWidth(320);
        primaryStage.setMinHeight(320);
        primaryStage.show();

        // Läuft im Hintergrund, damit ein fehlender/langsamer Stockfish das UI nicht blockiert.
        Thread diagnostics = new Thread(Main::runStartupChecks, "startup-diagnostics");
        diagnostics.setDaemon(true);
        diagnostics.start();
    }

    private static void runStartupChecks() {
        System.out.println("=== Schach-Lernapp: Startdiagnose ===");
        ChessLibCheck.run();
        StockfishEngine.runHandshakeCheck();
        System.out.println("=== Startdiagnose abgeschlossen ===");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
