package org.schachlernapp.ui.review;

import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Zugliste einer importierten Partie (M8.2) - ein Eintrag pro Halbzug ("12. Nf3"/"12... Nc6").
 * Reine Anzeigekomponente, extern verdrahtet (gleiches Muster wie {@link EvalGraph}). Ein Klick
 * meldet den Halbzug-Index über {@link #setOnMoveSelected(Consumer)}; {@link #selectMove(int)}
 * spiegelt eine extern ausgelöste Auswahl (Pfeiltasten, Eval-Graph-Klick) zurück, ohne dadurch
 * selbst erneut den Klick-Listener zu feuern.
 */
public class MoveListView extends VBox {

    private final ListView<String> listView = new ListView<>();

    private Consumer<Integer> onMoveSelected;
    private boolean updatingSelectionProgrammatically;

    public MoveListView() {
        setPrefWidth(160);
        listView.setPrefWidth(160);
        listView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        listView.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (!updatingSelectionProgrammatically && newIndex != null && newIndex.intValue() >= 0
                    && onMoveSelected != null) {
                onMoveSelected.accept(newIndex.intValue());
            }
        });
        VBox.setVgrow(listView, Priority.ALWAYS);
        getChildren().add(listView);
    }

    /** Ersetzt die angezeigten Züge (ein Eintrag pro Halbzug: "12. Nf3" bzw. "12... Nc6"). */
    public void setMoves(List<String> sanMoves) {
        List<String> rows = new ArrayList<>(sanMoves.size());
        for (int i = 0; i < sanMoves.size(); i++) {
            int moveNumber = i / 2 + 1;
            boolean isWhiteMove = i % 2 == 0;
            rows.add(moveNumber + (isWhiteMove ? ". " : "... ") + sanMoves.get(i));
        }
        listView.getItems().setAll(rows);
    }

    public void clear() {
        listView.getItems().clear();
    }

    /** {@code -1} = keine Auswahl (Startstellung), sonst 0-basierter Halbzug-Index. */
    public void selectMove(int halfMoveIndex) {
        updatingSelectionProgrammatically = true;
        try {
            if (halfMoveIndex < 0) {
                listView.getSelectionModel().clearSelection();
            } else {
                listView.getSelectionModel().select(halfMoveIndex);
                listView.scrollTo(halfMoveIndex);
            }
        } finally {
            updatingSelectionProgrammatically = false;
        }
    }

    public void setOnMoveSelected(Consumer<Integer> listener) {
        this.onMoveSelected = listener;
    }
}
