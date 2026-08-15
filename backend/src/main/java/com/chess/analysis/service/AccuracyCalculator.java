package com.chess.analysis.service;

import com.chess.analysis.model.MoveClassification;
import org.springframework.stereotype.Component;

// Converts centipawn loss into accuracy percentage and move classification
// Formula used by chess.com (approximated)
@Component
public class AccuracyCalculator {

    // Converts centipawn loss to a 0-100 accuracy for a single move
    // Uses an exponential decay — small losses matter less, big losses matter more
    public double moveAccuracy(int centipawnLoss) {
        if (centipawnLoss <= 0) return 100.0;
        // chess.com's approximate formula
        return Math.max(0, 103.1668 * Math.exp(-0.04354 * centipawnLoss) - 3.1669);
    }

    // Classifies a move based on centipawn loss
    public MoveClassification classify(int centipawnLoss) {
        if (centipawnLoss <= 5)   return MoveClassification.BEST;
        if (centipawnLoss <= 10)  return MoveClassification.EXCELLENT;
        if (centipawnLoss <= 25)  return MoveClassification.GOOD;
        if (centipawnLoss <= 50)  return MoveClassification.INACCURACY;
        if (centipawnLoss <= 100) return MoveClassification.MISTAKE;
        return MoveClassification.BLUNDER;
    }

    // Calculates overall game accuracy as the average of all move accuracies
    public double gameAccuracy(java.util.List<Double> moveAccuracies) {
        if (moveAccuracies.isEmpty()) return 0.0;
        double sum = moveAccuracies.stream().mapToDouble(Double::doubleValue).sum();
        double avg = sum / moveAccuracies.size();
        // Round to 1 decimal place
        return Math.round(avg * 10.0) / 10.0;
    }

    // Centipawn scores are from white's perspective
    // When black plays, we need to flip the sign to compute black's centipawn loss
    public int normalizeScore(int rawScore, boolean isWhiteMove) {
        return isWhiteMove ? rawScore : -rawScore;
    }
}
