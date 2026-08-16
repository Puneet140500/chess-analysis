package com.chess.analysis.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

// Full analysis result for a complete game
@Data
@Builder
public class GameAnalysis {
    private String gameId;
    private String whitePlayer;
    private String blackPlayer;
    private String result;            // "1-0", "0-1", "1/2-1/2"
    private String timeControl;
    private String openingName;
    private String openingEco;
    private double whiteAccuracy;     // 0-100
    private double blackAccuracy;     // 0-100
    private List<MoveAnalysis> moves;
    private int totalMoves;
}
