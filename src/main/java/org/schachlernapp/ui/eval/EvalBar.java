package org.schachlernapp.ui.eval;

import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import org.schachlernapp.engine.Evaluation;

/**
 * Vertikaler Eval-Balken (Weiß-Anteil unten, analog zur Brett-Orientierung
 * aus M2). Reine Anzeigekomponente - kennt weder Engine noch Board, nur den
 * {@link Evaluation}-Werttyp. Wird von {@link org.schachlernapp.analysis.EvaluationController}
 * über {@link #setEvaluation(Evaluation)} gefüttert.
 */
public class EvalBar extends Region {

    /** Bewertungen jenseits von ±CP_CAP füllen den Balken bereits (nahezu) komplett. */
    private static final int CP_CAP = 1000;

    private final Region blackPart = new Region();
    private final Region whitePart = new Region();
    private final Label scoreLabel = new Label("–");

    private double whiteFraction = 0.5;

    public EvalBar() {
        setPrefWidth(36);
        setMinWidth(24);
        blackPart.setStyle("-fx-background-color: #202020;");
        whitePart.setStyle("-fx-background-color: #f0f0f0;");
        scoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        scoreLabel.setMouseTransparent(true);
        getChildren().addAll(blackPart, whitePart, scoreLabel);
    }

    /** Aktualisiert die Anzeige. {@code null} setzt den Balken auf den neutralen Ausgangszustand zurück. */
    public void setEvaluation(Evaluation evaluation) {
        this.whiteFraction = toWhiteFraction(evaluation);
        scoreLabel.setText(formatLabel(evaluation));
        requestLayout();
    }

    @Override
    protected void layoutChildren() {
        double width = getWidth();
        double height = getHeight();
        double whiteHeight = height * whiteFraction;
        double blackHeight = height - whiteHeight;

        blackPart.resizeRelocate(0, 0, width, blackHeight);
        whitePart.resizeRelocate(0, blackHeight, width, whiteHeight);

        scoreLabel.autosize();
        double labelX = (width - scoreLabel.getWidth()) / 2;
        double labelY = whiteFraction >= 0.5 ? Math.max(0, height - scoreLabel.getHeight() - 4) : 4;
        scoreLabel.relocate(labelX, labelY);
    }

    private static double toWhiteFraction(Evaluation evaluation) {
        if (evaluation == null) {
            return 0.5;
        }
        if (evaluation.type() == Evaluation.EvalType.MATE) {
            return evaluation.value() >= 0 ? 0.97 : 0.03;
        }
        int clamped = Math.max(-CP_CAP, Math.min(CP_CAP, evaluation.value()));
        return 0.5 + (clamped / (double) CP_CAP) * 0.5;
    }

    private static String formatLabel(Evaluation evaluation) {
        if (evaluation == null) {
            return "–";
        }
        if (evaluation.type() == Evaluation.EvalType.MATE) {
            return "M" + Math.abs(evaluation.value());
        }
        return String.format("%.1f", evaluation.value() / 100.0);
    }
}
