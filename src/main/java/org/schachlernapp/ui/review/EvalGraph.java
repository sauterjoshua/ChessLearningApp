package org.schachlernapp.ui.review;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.schachlernapp.analysis.MoveQuality;
import org.schachlernapp.engine.Evaluation;
import org.schachlernapp.review.GameReview;
import org.schachlernapp.review.HalfMoveReview;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Eval-Graph über den Zugverlauf einer analysierten Partie (M8) - reine Anzeigekomponente,
 * analog {@link org.schachlernapp.ui.eval.EvalBar}: kennt nur {@link GameReview}/{@link Evaluation},
 * keine Engine-/Board-Abhängigkeit. Oben = Vorteil Weiß, unten = Vorteil Schwarz (Standard-Konvention
 * für Partie-Analyse-Graphen, unabhängig von der Brett-Orientierung aus M2).
 *
 * <p>Ein Klick meldet den nächstgelegenen Punkt über {@link #setOnMoveSelected(Consumer)}
 * ({@code -1} = Startstellung, sonst der Halbzug-Index wie in {@link HalfMoveReview#halfMoveIndex()}).</p>
 */
public class EvalGraph extends Region {

    private static final int CP_CAP = 1000;
    private static final double POINT_RADIUS = 3.5;
    private static final double BLUNDER_POINT_RADIUS = 5.5;

    private final Canvas canvas = new Canvas();
    private final List<Double> whiteAdvantageFractions = new ArrayList<>();
    /** Ein Eintrag pro Halbzug - qualities.get(i) gehört zu whiteAdvantageFractions.get(i + 1). */
    private final List<MoveQuality> qualities = new ArrayList<>();

    private Consumer<Integer> onMoveSelected;

    public EvalGraph() {
        setPrefHeight(90);
        setMinHeight(60);
        getStyleClass().add("eval-graph");
        getChildren().add(canvas);
        canvas.setOnMouseClicked(this::handleClick);
    }

    /** Zeigt die Analyse an - ersetzt einen ggf. vorher angezeigten Review vollständig. */
    public void showReview(GameReview review) {
        whiteAdvantageFractions.clear();
        qualities.clear();
        whiteAdvantageFractions.add(toWhiteAdvantageFraction(review.startEvaluation()));
        for (HalfMoveReview move : review.moves()) {
            whiteAdvantageFractions.add(toWhiteAdvantageFraction(move.evaluation()));
            qualities.add(move.quality());
        }
        redraw();
    }

    /** Setzt den Graph auf den leeren Ausgangszustand zurück (z.B. bei neuem Import ohne bisherige Analyse). */
    public void clear() {
        whiteAdvantageFractions.clear();
        qualities.clear();
        redraw();
    }

    public void setOnMoveSelected(Consumer<Integer> listener) {
        this.onMoveSelected = listener;
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        canvas.relocate(0, 0);
        redraw();
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.web("#fafafa"));
        gc.fillRect(0, 0, width, height);

        int pointCount = whiteAdvantageFractions.size();
        if (pointCount < 2 || width <= 0 || height <= 0) {
            return;
        }
        double stepX = width / (pointCount - 1);

        gc.setStroke(Color.web("#555555"));
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i < pointCount; i++) {
            double x = i * stepX;
            double y = whiteAdvantageFractions.get(i) * height;
            if (i == 0) {
                gc.moveTo(x, y);
            } else {
                gc.lineTo(x, y);
            }
        }
        gc.stroke();

        for (int i = 0; i < pointCount; i++) {
            double x = i * stepX;
            double y = whiteAdvantageFractions.get(i) * height;
            boolean isBlunder = i > 0 && qualities.get(i - 1) == MoveQuality.BLUNDER;
            double radius = isBlunder ? BLUNDER_POINT_RADIUS : POINT_RADIUS;
            gc.setFill(isBlunder ? Color.web("#c62828") : Color.web("#2e7d32"));
            gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
        }
    }

    private void handleClick(MouseEvent event) {
        int pointCount = whiteAdvantageFractions.size();
        if (pointCount < 2 || onMoveSelected == null) {
            return;
        }
        double stepX = canvas.getWidth() / (pointCount - 1);
        int nearestPoint = (int) Math.round(event.getX() / stepX);
        nearestPoint = Math.max(0, Math.min(pointCount - 1, nearestPoint));
        onMoveSelected.accept(nearestPoint - 1);
    }

    private static double toWhiteAdvantageFraction(Evaluation evaluation) {
        if (evaluation == null) {
            return 0.5;
        }
        if (evaluation.type() == Evaluation.EvalType.MATE) {
            return evaluation.value() >= 0 ? 0.03 : 0.97;
        }
        int clamped = Math.max(-CP_CAP, Math.min(CP_CAP, evaluation.value()));
        return 0.5 - (clamped / (double) CP_CAP) * 0.5;
    }
}
