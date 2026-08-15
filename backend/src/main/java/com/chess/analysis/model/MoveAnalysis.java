package com.chess.analysis.model;

import lombok.Builder;
import lombok.Data;

// Analysis result for a single move
@Data
@Builder
public class MoveAnalysis {
    private int moveNumber;
    private String playedMove;        // move the player actually played (e.g. "e2e4")
    private String bestMove;          // move Stockfish recommends
    private int scoreBefore;          // centipawn score before this move
    private int scoreAfter;           // centipawn score after this move
    private int bestMoveScore;        // centipawn score if best move was played
    private int centipawnLoss;        // bestMoveScore - scoreAfter (always >= 0)
    private double accuracy;          // 0-100 for this move
    private MoveClassification classification;
    private boolean isWhiteMove;
    private String fenBefore;         // board position before move (for rendering)
    private String fenAfter;          // board position after move
}
