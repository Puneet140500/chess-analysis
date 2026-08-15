package com.chess.analysis.service;

import com.chess.analysis.model.Game;
import java.util.List;

// Interface so we can add LichessFetcher later without changing AnalysisService
public interface GameFetcher {
    List<Game> getRecentGames(String username, int limit);
}
