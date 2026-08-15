package com.chess.analysis.service;

import com.chess.analysis.model.MoveClassification;
import org.springframework.stereotype.Component;

import java.util.List;

// Accuracy calculation matching chess.com's methodology:
// 1. Convert centipawn scores to win probability (sigmoid)
// 2. Measure win probability DROP per move
// 3. Convert that drop to a 0-100 move accuracy
// 4. Average move accuracies into game accuracy
@Component
public class AccuracyCalculator {

    // Converts a centipawn score to win probability (0.0 to 1.0)
    // Sigmoid function: at 0cp → 0.5 (equal), at +500cp → ~0.84 (clearly winning)
    // The constant 0.00368 is calibrated to match chess.com's formula
    public double winProbability(int centipawns) {
        // Cap at mate values to avoid overflow
        int capped = Math.max(-3000, Math.min(3000, centipawns));
        return 1.0 / (1.0 + Math.exp(-0.00368 * capped));
    }

    // Converts win probability DROP to a 0-100 accuracy for a single move
    // winBefore: win probability before move (from current player's perspective)
    // winAfter:  win probability after move (from current player's perspective)
    public double moveAccuracy(double winBefore, double winAfter) {
        // Win probability loss (0.0 to 1.0)
        double winLoss = Math.max(0, winBefore - winAfter);
        // Convert to percentage scale and apply chess.com's exponential decay formula
        return Math.max(0, Math.min(100,
                103.1668 * Math.exp(-0.04354 * winLoss * 100) - 3.1669));
    }

    // Classifies a move based on win probability loss (converted to centipawn-equivalent)
    // We still use centipawn loss for classification thresholds (more intuitive)
    public MoveClassification classify(int centipawnLoss) {
        if (centipawnLoss <= 5)   return MoveClassification.BEST;
        if (centipawnLoss <= 10)  return MoveClassification.EXCELLENT;
        if (centipawnLoss <= 25)  return MoveClassification.GOOD;
        if (centipawnLoss <= 50)  return MoveClassification.INACCURACY;
        if (centipawnLoss <= 100) return MoveClassification.MISTAKE;
        return MoveClassification.BLUNDER;
    }

    // Game accuracy = average of move accuracies, rounded to 1 decimal
    public double gameAccuracy(List<Double> moveAccuracies) {
        if (moveAccuracies.isEmpty()) return 0.0;
        double sum = moveAccuracies.stream().mapToDouble(Double::doubleValue).sum();
        double avg = sum / moveAccuracies.size();
        return Math.round(avg * 10.0) / 10.0;
    }

}
