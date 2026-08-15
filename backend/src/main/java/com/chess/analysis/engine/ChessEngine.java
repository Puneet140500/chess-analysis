package com.chess.analysis.engine;

// Strategy pattern: defines the contract for any chess engine
// Tomorrow you could implement LeelaCEngine or CloudEngine without changing any other code
public interface ChessEngine {

    // Given a FEN position, returns the best move in UCI format (e.g. "e2e4")
    EngineResult analyze(String fen, int moveTimeMs);

    // Check if this engine instance is available (not currently analyzing)
    boolean isAvailable();

    // Clean up resources (close the process)
    void shutdown();
}
