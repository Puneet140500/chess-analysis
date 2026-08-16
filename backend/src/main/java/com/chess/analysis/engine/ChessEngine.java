package com.chess.analysis.engine;

public interface ChessEngine {

    EngineResult analyze(String fen, int depth, int moveTimeMs);

    boolean isAvailable();

    void shutdown();
}
