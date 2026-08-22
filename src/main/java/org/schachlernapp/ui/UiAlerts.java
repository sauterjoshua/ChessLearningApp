package org.schachlernapp.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * Wiederverwendbarer Fehlerdialog (gleiches Muster wie
 * {@link org.schachlernapp.ui.board.BoardView}s Partie-Ende-Dialog), gedacht
 * für Start-/Initialisierungsfehler (Engine startet nicht, DB öffnet nicht).
 *
 * <p>Bewusst NICHT für einzelne fehlgeschlagene Live-Auswertungen während des
 * Spiels gedacht - das würde bei einem Engine-Absturz mitten in der Partie zu
 * einem Dialog pro gescheiterter Anfrage führen. Solche Fehler bleiben wie
 * bisher nur im Log ({@code EvaluationController}s {@code .exceptionally(...)}).</p>
 */
public final class UiAlerts {

    private UiAlerts() {
    }

    /** Zeigt einen Fehlerdialog. Sicher von jedem Thread aus aufrufbar (hüpft selbst per Platform.runLater). */
    public static void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.setHeaderText(title);
            alert.showAndWait();
        });
    }
}
