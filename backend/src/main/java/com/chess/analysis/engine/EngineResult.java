package com.chess.analysis.engine;

import lombok.Builder;
import lombok.Data;

// What Stockfish returns for a single position analysis
@Data
@Builder
public class EngineResult {
    private String bestMove;      // e.g. "e2e4"
    private int centipawnScore;   // positive = white is winning, negative = black
    private int depth;            // how deep Stockfish searched
    private boolean isMate;       // true if it found a forced checkmate
    private int mateInMoves;      // if isMate, how many moves to mate
}
