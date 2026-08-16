package com.chess.analysis.engine;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EngineResult {
    private String bestMove;
    private int centipawnScore;
    private int wdlWin;    // Stockfish WDL win count (0-1000)
    private int wdlDraw;   // Stockfish WDL draw count (0-1000)
    private int wdlLoss;   // Stockfish WDL loss count (0-1000)
    private int depth;
    private boolean isMate;
    private int mateInMoves;

    // Win probability from native WDL: (W + D/2) / 1000
    // Returns -1 if no WDL data (fall back to sigmoid)
    public double wdlWinProbability() {
        if (wdlWin == 0 && wdlDraw == 0 && wdlLoss == 0) return -1.0;
        return (wdlWin + wdlDraw / 2.0) / 1000.0;
    }
}
