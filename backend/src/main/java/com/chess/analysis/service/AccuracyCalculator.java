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
        return 1.0 / (1.0 + Math.exp(-0.00368208 * capped));
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

    // Classifies a move based on win probability loss (0.0–1.0 scale)
    // Thresholds mirror chess.com's classification bands
    public MoveClassification classify(double winProbLoss, boolean isBestMove, boolean isBrilliant) {
        if (isBrilliant)          return MoveClassification.BRILLIANT;
        if (isBestMove || winProbLoss <= 0.005) return MoveClassification.BEST;
        if (winProbLoss <= 0.02)  return MoveClassification.EXCELLENT;
        if (winProbLoss <= 0.06)  return MoveClassification.GOOD;
        if (winProbLoss <= 0.14)  return MoveClassification.INACCURACY;
        if (winProbLoss <= 0.28)  return MoveClassification.MISTAKE;
        return MoveClassification.BLUNDER;
    }

    // Game accuracy = (arithmetic mean + harmonic mean) / 2 of per-move accuracies
    // Lichess methodology: harmonic mean penalizes blunders more than arithmetic alone
    public double gameAccuracy(List<Double> moveAccuracies) {
        if (moveAccuracies.isEmpty()) return 0.0;
        double arith = moveAccuracies.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double harm  = moveAccuracies.size() /
                moveAccuracies.stream().mapToDouble(a -> 1.0 / Math.max(a, 0.1)).sum();
        return Math.round(((arith + harm) / 2.0) * 10.0) / 10.0;
    }

}
