package com.chess.analysis.service;

import com.chess.analysis.engine.EnginePool;
import com.chess.analysis.engine.EngineResult;
import com.chess.analysis.model.*;
import com.chess.analysis.parser.PgnParser;
import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final GameFetcher gameFetcher;
    private final PgnParser pgnParser;
    private final EnginePool enginePool;
    private final AccuracyCalculator accuracyCalculator;
    private final OpeningService openingService;

    public List<Game> getRecentGames(String username, int limit) {
        return gameFetcher.getRecentGames(username, limit);
    }

    public GameAnalysis analyzeGame(Game game) {
        log.info("Analyzing game {} ({} vs {})", game.getGameId(), game.getWhitePlayer(), game.getBlackPlayer());
        long startTime = System.currentTimeMillis();

        PgnParser.ParsedGame parsed = pgnParser.parse(game.getPgn());
        List<String> fenPositions = parsed.fenPositions(); // fen before each move
        List<String> moves        = parsed.moves();

        log.info("Parsed {} moves, sending to engine pool...", moves.size());

        // N+1 positions: position before each move + position after last move
        List<String> allFens = new ArrayList<>(fenPositions);
        allFens.add(buildFinalFen(fenPositions, moves));

        // Run engine and opening lookup concurrently
        CompletableFuture<List<EngineResult>> engineFuture =
                CompletableFuture.supplyAsync(() -> enginePool.analyzeAll(allFens));
        List<OpeningService.OpeningResult> openings = openingService.findOpenings(fenPositions);

        List<EngineResult> allResults;
        try {
            allResults = engineFuture.get();
        } catch (Exception e) {
            throw new RuntimeException("Engine analysis failed", e);
        }

        List<MoveAnalysis> moveAnalyses = buildMoveAnalyses(moves, fenPositions, allResults);

        // Tag book moves and per-move opening name
        for (int i = 0; i < moveAnalyses.size(); i++) {
            MoveAnalysis ma = moveAnalyses.get(i);
            boolean isBook = openingService.isBookMove(ma.getFenBefore(), ma.getPlayedMove());
            OpeningService.OpeningResult op = openings.get(i);
            moveAnalyses.set(i, ma.toBuilder()
                    .bookMove(isBook)
                    .openingName(op.openingName())
                    .openingEco(op.eco())
                    .build());
        }

        // Game accuracy: skip decided positions
        List<Double> whiteAccuracies = new ArrayList<>();
        List<Double> blackAccuracies = new ArrayList<>();
        for (MoveAnalysis ma : moveAnalyses) {
            double wb = ma.getWinBefore();
            double wa = ma.getWinAfter();
            if (wb > 0.98 || wb < 0.02) continue;
            if (ma.isWhiteMove()) whiteAccuracies.add(ma.getAccuracy());
            else                  blackAccuracies.add(ma.getAccuracy());
        }

        String gameOpeningName = null, gameOpeningEco = null;
        for (OpeningService.OpeningResult op : openings) {
            if (op.openingName() != null) { gameOpeningName = op.openingName(); gameOpeningEco = op.eco(); }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Analysis complete in {}ms", elapsed);

        return GameAnalysis.builder()
                .gameId(game.getGameId())
                .whitePlayer(game.getWhitePlayer())
                .blackPlayer(game.getBlackPlayer())
                .result(game.getResult())
                .timeControl(game.getTimeControl())
                .openingName(gameOpeningName)
                .openingEco(gameOpeningEco)
                .whiteAccuracy(accuracyCalculator.gameAccuracy(whiteAccuracies))
                .blackAccuracy(accuracyCalculator.gameAccuracy(blackAccuracies))
                .moves(moveAnalyses)
                .totalMoves(moves.size())
                .build();
    }

    // allResults[i]   = best score/move from position before move[i]  (side-to-move POV)
    // allResults[i+1] = best score from position after  move[i]       (opponent's POV → negate)
    private List<MoveAnalysis> buildMoveAnalyses(
            List<String> moves,
            List<String> fenBefores,
            List<EngineResult> allResults) {

        List<MoveAnalysis> analyses = new ArrayList<>();

        for (int i = 0; i < moves.size(); i++) {
            boolean isWhiteMove = (i % 2 == 0);
            EngineResult before = allResults.get(i);
            EngineResult after  = allResults.get(i + 1);

            int bestScore   = before.getCentipawnScore();
            // after the move, opponent is to move — negate to stay in current player's POV
            int playedScore = -after.getCentipawnScore();
            int cpLoss      = Math.max(0, bestScore - playedScore);

            // WDL from side-to-move's POV; after the move it's opponent's POV, so 1 - wdlWinProb
            double winBefore = before.wdlWinProbability() >= 0
                    ? before.wdlWinProbability()
                    : accuracyCalculator.winProbability(bestScore);
            double winAfterRaw = after.wdlWinProbability();
            double winAfter = winAfterRaw >= 0
                    ? (1.0 - winAfterRaw)
                    : accuracyCalculator.winProbability(playedScore);
            double winLoss   = Math.max(0.0, winBefore - winAfter);
            double accuracy  = accuracyCalculator.moveAccuracy(winBefore, winAfter);

            boolean isBestMove  = moves.get(i).equals(before.getBestMove());
            boolean isBrilliant = !isBestMove && winAfter > winBefore + 0.05;

            MoveClassification classification = accuracyCalculator.classify(winLoss, isBestMove, isBrilliant);

            log.info("Move {} [{}] {}: bestScore={} playedScore={} wdlBefore={}/{}/{} wdlAfter={}/{}/{} winBefore={} winAfter={} winLoss={} acc={} class={}",
                    i + 1, isWhiteMove ? "W" : "B", moves.get(i),
                    bestScore, playedScore,
                    before.getWdlWin(), before.getWdlDraw(), before.getWdlLoss(),
                    after.getWdlWin(), after.getWdlDraw(), after.getWdlLoss(),
                    String.format("%.3f", winBefore), String.format("%.3f", winAfter),
                    String.format("%.3f", winLoss), String.format("%.1f", accuracy), classification);

            analyses.add(MoveAnalysis.builder()
                    .moveNumber(i / 2 + 1)
                    .playedMove(moves.get(i))
                    .bestMove(before.getBestMove())
                    .scoreBefore(bestScore)
                    .scoreAfter(playedScore)
                    .bestMoveScore(bestScore)
                    .centipawnLoss(cpLoss)
                    .winBefore(winBefore)
                    .winAfter(winAfter)
                    .accuracy(accuracy)
                    .classification(classification)
                    .isWhiteMove(isWhiteMove)
                    .fenBefore(fenBefores.get(i))
                    .fenAfter(buildFenAfter(fenBefores.get(i), moves.get(i)))
                    .build());
        }
        return analyses;
    }

    private String buildFinalFen(List<String> fenBefores, List<String> uciMoves) {
        if (uciMoves.isEmpty()) return fenBefores.get(0);
        String lastFen = fenBefores.get(fenBefores.size() - 1);
        String lastMove = uciMoves.get(uciMoves.size() - 1);
        return buildFenAfter(lastFen, lastMove);
    }

    private String buildFenAfter(String fen, String uciMove) {
        try {
            Board board = new Board();
            board.loadFromFen(fen);
            board.doMove(new Move(uciMove, board.getSideToMove()));
            return board.getFen();
        } catch (Exception e) {
            log.warn("Could not build fenAfter for {} on {}: {}", uciMove, fen, e.getMessage());
            return fen;
        }
    }
}
