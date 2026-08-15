package com.chess.analysis.controller;

import com.chess.analysis.model.Game;
import com.chess.analysis.model.GameAnalysis;
import com.chess.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
@RequiredArgsConstructor
public class GameController {

    private final AnalysisService analysisService;

    // GET /api/games?username=hikaru&limit=10
    // Returns recent games (no analysis) so user can pick one
    @GetMapping("/games")
    public ResponseEntity<List<Game>> getRecentGames(
            @RequestParam String username,
            @RequestParam(defaultValue = "10") int limit) {
        List<Game> games = analysisService.getRecentGames(username, limit);
        return ResponseEntity.ok(games);
    }

    // POST /api/analyze
    // Body: { "gameId": "...", "pgn": "...", "whitePlayer": "...", ... }
    // Returns full analysis with move-by-move breakdown
    @PostMapping("/analyze")
    public ResponseEntity<GameAnalysis> analyzeGame(@RequestBody Game game) {
        GameAnalysis analysis = analysisService.analyzeGame(game);
        return ResponseEntity.ok(analysis);
    }
}
