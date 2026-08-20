package org.schachlernapp;

import org.schachlernapp.chess.ChessLibCheck;
import org.schachlernapp.engine.StockfishEngine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Einstiegspunkt der Schach-Lernapp (Meilenstein 1: Grundgerüst).
 * Öffnet ein leeres JavaFX-Fenster und prüft im Hintergrund, ob chesslib
 * und die lokale Stockfish-Installation funktionieren.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Schach-Lernapp");
        primaryStage.setScene(new Scene(new StackPane(), 800, 600));
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
