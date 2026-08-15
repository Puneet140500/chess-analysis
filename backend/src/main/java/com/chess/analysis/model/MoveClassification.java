package com.chess.analysis.model;

// Classification of a chess move based on centipawn loss
public enum MoveClassification {
    BRILLIANT,    // special move (sacrifice, engine wouldn't find easily)
    BEST,         // 0-5 cp loss
    EXCELLENT,    // 6-10 cp loss
    GOOD,         // 11-25 cp loss
    INACCURACY,   // 26-50 cp loss
    MISTAKE,      // 51-100 cp loss
    BLUNDER       // 100+ cp loss
}
