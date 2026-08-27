package org.schachlernapp.ui.review;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;
import org.schachlernapp.review.GameImportException;
import org.schachlernapp.review.GameImportService;
import org.schachlernapp.review.ImportedGame;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Modaler Import-Dialog (M8): Username + Monat/Jahr. "Importieren" ruft
 * {@link GameImportService#fetchGames} in einem Hintergrund-Thread auf ({@code GameImportService}
 * ist selbst blockierend, wie {@code PuzzleRepository}) - der Dialog bleibt bei einem Fehler offen
 * und zeigt die Meldung aus {@link GameImportException} an, statt sich zu schließen.
 */
public class ImportGameDialog {

    private final GameImportService importService;
    private final Dialog<List<ImportedGame>> dialog = new Dialog<>();

    private final TextField usernameField = new TextField();
    private final ComboBox<Month> monthBox = new ComboBox<>();
    private final ComboBox<Integer> yearBox = new ComboBox<>();
    private final Label errorLabel = new Label();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    public ImportGameDialog(GameImportService importService) {
        this.importService = importService;

        dialog.setTitle("Partie importieren");
        dialog.setHeaderText("chess.com-Partien für einen Monat importieren");

        ButtonType confirmButtonType = new ButtonType("Importieren", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        dialog.setResultConverter(buttonType -> null); // Erfolg wird über setResult(...) in startImport gesetzt

        usernameField.setPromptText("chess.com-Benutzername");

        int currentYear = LocalDate.now().getYear();
        for (int year = currentYear; year >= currentYear - 15; year--) {
            yearBox.getItems().add(year);
        }
        yearBox.getSelectionModel().select(Integer.valueOf(currentYear));

        monthBox.getItems().addAll(Month.values());
        monthBox.getSelectionModel().select(LocalDate.now().getMonth());
        monthBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Month month) {
                return month == null ? "" : month.getDisplayName(TextStyle.FULL, Locale.GERMAN);
            }

            @Override
            public Month fromString(String string) {
                return null; // ComboBox ist nicht editierbar, wird nie aufgerufen
            }
        });

        errorLabel.setTextFill(Color.web("#c62828"));
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        progressIndicator.setMaxSize(20, 20);
        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(8));
        grid.addRow(0, new Label("Username:"), usernameField);
        grid.addRow(1, new Label("Monat:"), monthBox);
        grid.addRow(2, new Label("Jahr:"), yearBox);
        grid.add(progressIndicator, 1, 3);
        grid.add(errorLabel, 0, 4, 2, 1);
        dialog.getDialogPane().setContent(grid);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume(); // Dialog erst nach erfolgreichem Import schließen, nicht sofort beim Klick
            startImport(confirmButton);
        });
    }

    /** Zeigt den Dialog modal an. Leer bei Abbruch, sonst die importierten Partien (kann leer sein, wenn der Monat keine enthielt). */
    public Optional<List<ImportedGame>> showAndWait() {
        return dialog.showAndWait();
    }

    private void startImport(Button confirmButton) {
        String username = usernameField.getText();
        Month month = monthBox.getValue();
        Integer year = yearBox.getValue();
        if (username == null || username.isBlank() || month == null || year == null) {
            showError("Bitte Username und Monat/Jahr angeben.");
            return;
        }
        YearMonth yearMonth = YearMonth.of(year, month);

        setBusy(true, confirmButton);
        Thread importThread = new Thread(() -> {
            try {
                List<ImportedGame> games = importService.fetchGames(username, yearMonth);
                Platform.runLater(() -> {
                    setBusy(false, confirmButton);
                    dialog.setResult(games);
                    dialog.close();
                });
            } catch (GameImportException e) {
                Platform.runLater(() -> {
                    setBusy(false, confirmButton);
                    showError(e.getMessage());
                });
            }
        }, "chess-com-import");
        importThread.setDaemon(true);
        importThread.start();
    }

    private void setBusy(boolean busy, Button confirmButton) {
        confirmButton.setDisable(busy);
        progressIndicator.setVisible(busy);
        progressIndicator.setManaged(busy);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
