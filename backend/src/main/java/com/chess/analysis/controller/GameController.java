package com.chess.analysis.controller;

import com.chess.analysis.engine.EnginePool;
import com.chess.analysis.engine.EngineResult;
import com.chess.analysis.model.Game;
import com.chess.analysis.model.GameAnalysis;
import com.chess.analysis.service.AnalysisService;
import com.chess.analysis.service.EcoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = "http://localhost:*")
@RequiredArgsConstructor
public class GameController {

    private final AnalysisService analysisService;
    private final EnginePool enginePool;
    private final EcoService ecoService;

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

    // POST /api/analyze-position
    // Body: { "fen": "..." }
    // Returns best move + eval for a single position (used by interactive board)
    @PostMapping("/analyze-position")
    public ResponseEntity<EngineResult> analyzePosition(@RequestBody PositionRequest req) {
        List<EngineResult> results = enginePool.analyzeAll(List.of(req.fen()));
        return ResponseEntity.ok(results.get(0));
    }

    // GET /api/openings?q=italian&limit=80
    // Search the ECO database — returns eco, name, moves (SAN string)
    @GetMapping("/openings")
    public ResponseEntity<List<EcoService.Opening>> searchOpenings(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String eco,
            @RequestParam(defaultValue = "120") int limit) {
        return ResponseEntity.ok(ecoService.search(q, eco, limit));
    }

    record PositionRequest(String fen) {}
}
