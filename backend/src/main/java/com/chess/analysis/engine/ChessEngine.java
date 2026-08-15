package com.chess.analysis.engine;

// Strategy pattern: defines the contract for any chess engine
// Tomorrow you could implement LeelaCEngine or CloudEngine without changing any other code
public interface ChessEngine {

    // Analyze a position — returns best move + score
    EngineResult analyze(String fen, int depth, int moveTimeMs);

    // Analyze a position but restrict Stockfish to only consider one specific move
    // Used to get the score of the move the player actually played
    EngineResult analyzeMove(String fen, String move, int depth, int moveTimeMs);

    // Check if this engine instance is available (not currently analyzing)
    boolean isAvailable();

    // Clean up resources (close the process)
    void shutdown();
}
