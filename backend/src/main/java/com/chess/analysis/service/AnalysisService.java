package com.chess.analysis.service;

import com.chess.analysis.engine.EnginePool;
import com.chess.analysis.engine.EngineResult;
import com.chess.analysis.model.*;
import com.chess.analysis.parser.PgnParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Orchestrates the full analysis pipeline:
// Game → PGN parsing → Parallel Stockfish analysis → Accuracy calculation → GameAnalysis
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final GameFetcher gameFetcher;
    private final PgnParser pgnParser;
    private final EnginePool enginePool;
    private final AccuracyCalculator accuracyCalculator;

    // Fetch recent games for a user (no analysis yet)
    public List<Game> getRecentGames(String username, int limit) {
        return gameFetcher.getRecentGames(username, limit);
    }

    // Full analysis of a single game by PGN string
    public GameAnalysis analyzeGame(Game game) {
        log.info("Analyzing game {} ({} vs {})", game.getGameId(), game.getWhitePlayer(), game.getBlackPlayer());
        long startTime = System.currentTimeMillis();

        // Step 1: Parse PGN into list of FEN positions
        PgnParser.ParsedGame parsed = pgnParser.parse(game.getPgn());
        List<String> fenPositions = parsed.fenPositions();
        List<String> moves = parsed.moves();

        log.info("Parsed {} moves, sending to engine pool...", moves.size());

        // Step 2: Analyze all N+1 positions (before each move + final position)
        // Position[i] is before move[i], Position[N] is after the last move
        List<String> allFens = new ArrayList<>(fenPositions);
        allFens.add(buildFinalFen(moves));

        List<EngineResult> allResults = enginePool.analyzeAll(allFens);

        // Step 3: For move[i]: bestScore = allResults[i], actualScore = allResults[i+1]
        List<MoveAnalysis> moveAnalyses = buildMoveAnalyses(moves, fenPositions, allResults);

        // Step 5: Calculate overall accuracy for white and black separately
        List<Double> whiteAccuracies = new ArrayList<>();
        List<Double> blackAccuracies = new ArrayList<>();
        for (MoveAnalysis ma : moveAnalyses) {
            if (ma.isWhiteMove()) whiteAccuracies.add(ma.getAccuracy());
            else blackAccuracies.add(ma.getAccuracy());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("Analysis complete in {}ms for {} moves", elapsed, moves.size());

        return GameAnalysis.builder()
                .gameId(game.getGameId())
                .whitePlayer(game.getWhitePlayer())
                .blackPlayer(game.getBlackPlayer())
                .result(game.getResult())
                .timeControl(game.getTimeControl())
                .whiteAccuracy(accuracyCalculator.gameAccuracy(whiteAccuracies))
                .blackAccuracy(accuracyCalculator.gameAccuracy(blackAccuracies))
                .moves(moveAnalyses)
                .totalMoves(moves.size())
                .build();
    }

    // allResults[i] = best score from position before move[i]  (side-to-move POV)
    // allResults[i+1] = best score from position after move[i] (opponent's POV → negate)
    private List<MoveAnalysis> buildMoveAnalyses(
            List<String> moves,
            List<String> fenPositions,
            List<EngineResult> allResults) {

        List<MoveAnalysis> analyses = new ArrayList<>();

        for (int i = 0; i < moves.size(); i++) {
            boolean isWhiteMove = (i % 2 == 0);

            int bestScore   = allResults.get(i).getCentipawnScore();
            // after the move, opponent is to move — negate to stay in current player's POV
            int actualScore = -allResults.get(i + 1).getCentipawnScore();
            int cpLoss      = Math.max(0, bestScore - actualScore);

            double winBefore = accuracyCalculator.winProbability(bestScore);
            double winAfter  = accuracyCalculator.winProbability(actualScore);
            double accuracy  = accuracyCalculator.moveAccuracy(winBefore, winAfter);

            analyses.add(MoveAnalysis.builder()
                    .moveNumber(i / 2 + 1)
                    .playedMove(moves.get(i))
                    .bestMove(allResults.get(i).getBestMove())
                    .scoreBefore(bestScore)
                    .scoreAfter(actualScore)
                    .bestMoveScore(bestScore)
                    .centipawnLoss(cpLoss)
                    .accuracy(accuracy)
                    .classification(accuracyCalculator.classify(cpLoss))
                    .isWhiteMove(isWhiteMove)
                    .fenBefore(fenPositions.get(i))
                    .fenAfter(i + 1 < fenPositions.size() ? fenPositions.get(i + 1) : fenPositions.get(i))
                    .build());
        }
        return analyses;
    }

    private String buildFinalFen(List<String> moves) {
        com.github.bhlangonijr.chesslib.Board board = new com.github.bhlangonijr.chesslib.Board();
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        for (String uciMove : moves) {
            com.github.bhlangonijr.chesslib.move.Move move =
                    new com.github.bhlangonijr.chesslib.move.Move(uciMove, board.getSideToMove());
            board.doMove(move);
        }
        return board.getFen();
    }
}
