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

        // Step 2: Build the full list of positions — before every move PLUS the final position
        // For 40 moves: 41 positions total (indices 0..40)
        // Position[i] is the board before move[i]
        // Position[40] is the board after the last move
        List<String> allFens = new ArrayList<>(fenPositions);
        allFens.add(buildFinalFen(fenPositions, moves)); // add position after last move

        // Step 3: Analyze all N+1 positions in ONE parallel batch (was 2 batches before)
        // This halves the total Stockfish work: 41 analyses instead of 80+80=160
        List<EngineResult> allResults = enginePool.analyzeAll(allFens);

        // Step 4: Build MoveAnalysis for each move
        // For move[i]: scoreBefore = allResults[i], scoreAfter = allResults[i+1]
        List<MoveAnalysis> moveAnalyses = buildMoveAnalyses(moves, allFens, allResults);

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

    // allFens has N+1 entries: position before each move + final position after last move
    // allResults has N+1 entries: one analysis per FEN
    // For move[i]: before = allResults[i], after = allResults[i+1]
    private List<MoveAnalysis> buildMoveAnalyses(
            List<String> moves,
            List<String> allFens,
            List<EngineResult> allResults) {

        List<MoveAnalysis> analyses = new ArrayList<>();

        for (int i = 0; i < moves.size(); i++) {
            boolean isWhiteMove = (i % 2 == 0);

            EngineResult before = allResults.get(i);
            EngineResult after = allResults.get(i + 1);

            // Stockfish scores are always from side-to-move's perspective.
            // before: current player to move  → score is already from their POV
            // after:  opponent to move        → score is from opponent's POV, so negate
            int bestScore   = before.getCentipawnScore();
            int actualScore = -after.getCentipawnScore(); // negate — opponent is now to move
            int cpLoss      = Math.max(0, bestScore - actualScore);

            double winBefore = accuracyCalculator.winProbability(bestScore);
            double winAfter  = accuracyCalculator.winProbability(actualScore);

            double accuracy = accuracyCalculator.moveAccuracy(winBefore, winAfter);
            MoveClassification classification = accuracyCalculator.classify(cpLoss);

            analyses.add(MoveAnalysis.builder()
                    .moveNumber(i / 2 + 1)
                    .playedMove(moves.get(i))
                    .bestMove(before.getBestMove())
                    .scoreBefore(before.getCentipawnScore())
                    .scoreAfter(after.getCentipawnScore())
                    .bestMoveScore(before.getCentipawnScore())
                    .centipawnLoss(cpLoss)
                    .accuracy(accuracy)
                    .classification(classification)
                    .isWhiteMove(isWhiteMove)
                    .fenBefore(allFens.get(i))
                    .fenAfter(allFens.get(i + 1))
                    .build());
        }
        return analyses;
    }

    // Replays all moves and returns the FEN after the last move
    // This gives us the N+1th position we need for the final move's scoreAfter
    private String buildFinalFen(List<String> fensBefore, List<String> moves) {
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
