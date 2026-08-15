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

        // Step 2: For each position, get the BEST move score
        // analyze(fen) → Stockfish finds the best move and returns its score
        List<EngineResult> bestResults = enginePool.analyzeAll(fenPositions);

        // Step 3: For each position, get the score of the move ACTUALLY PLAYED
        // analyzeMove(fen, playedMove) → Stockfish evaluates ONLY the played move
        // Both steps use the same FEN, same depth → scores are directly comparable
        List<EngineResult> playedResults = enginePool.analyzePlayedMoves(fenPositions, moves);

        // Step 4: Build MoveAnalysis — cpLoss = bestScore - playedScore (same perspective, same position)
        List<MoveAnalysis> moveAnalyses = buildMoveAnalyses(moves, fenPositions, bestResults, playedResults);

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

    // bestResults[i]    = Stockfish's best move + score from fenPositions[i]
    // playedResults[i]  = score of moves[i] restricted from fenPositions[i]
    // Both from the SAME position at the SAME depth → directly comparable, no sign flip needed
    private List<MoveAnalysis> buildMoveAnalyses(
            List<String> moves,
            List<String> fenPositions,
            List<EngineResult> bestResults,
            List<EngineResult> playedResults) {

        List<MoveAnalysis> analyses = new ArrayList<>();

        // Build FEN-after-move for rendering the board after each move
        List<String> fensAfter = buildFensAfterMoves(fenPositions, moves);

        for (int i = 0; i < moves.size(); i++) {
            boolean isWhiteMove = (i % 2 == 0);

            int bestScore   = bestResults.get(i).getCentipawnScore();
            int playedScore = playedResults.get(i).getCentipawnScore();

            // Both scores are from side-to-move perspective at the same position
            // No sign flip needed — cpLoss is always >= 0
            int cpLoss = Math.max(0, bestScore - playedScore);

            double winBefore = accuracyCalculator.winProbability(bestScore);
            double winAfter  = accuracyCalculator.winProbability(playedScore);
            double accuracy  = accuracyCalculator.moveAccuracy(winBefore, winAfter);

            analyses.add(MoveAnalysis.builder()
                    .moveNumber(i / 2 + 1)
                    .playedMove(moves.get(i))
                    .bestMove(bestResults.get(i).getBestMove())
                    .scoreBefore(bestScore)
                    .scoreAfter(playedScore)
                    .bestMoveScore(bestScore)
                    .centipawnLoss(cpLoss)
                    .accuracy(accuracy)
                    .classification(accuracyCalculator.classify(cpLoss))
                    .isWhiteMove(isWhiteMove)
                    .fenBefore(fenPositions.get(i))
                    .fenAfter(fensAfter.get(i))
                    .build());
        }
        return analyses;
    }

    private List<String> buildFensAfterMoves(List<String> fensBefore, List<String> moves) {
        List<String> fensAfter = new ArrayList<>();
        com.github.bhlangonijr.chesslib.Board board = new com.github.bhlangonijr.chesslib.Board();
        board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        for (String uciMove : moves) {
            com.github.bhlangonijr.chesslib.move.Move move =
                    new com.github.bhlangonijr.chesslib.move.Move(uciMove, board.getSideToMove());
            board.doMove(move);
            fensAfter.add(board.getFen());
        }
        return fensAfter;
    }
}
