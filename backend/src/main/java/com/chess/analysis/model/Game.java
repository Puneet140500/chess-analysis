package com.chess.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// A chess game fetched from chess.com (before analysis)
// @NoArgsConstructor + @AllArgsConstructor required so Jackson can deserialize
// AND @Builder can still construct instances
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    private String gameId;
    private String whitePlayer;
    private String blackPlayer;
    private String pgn;               // full PGN string from chess.com
    private String result;
    private String timeControl;
    private long endTime;             // unix timestamp
    private String url;               // chess.com game URL
}
