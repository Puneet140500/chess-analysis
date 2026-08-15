package com.chess.analysis.parser;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import com.github.bhlangonijr.chesslib.move.MoveList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Parses PGN string into a list of FEN positions
// FEN = Forsyth–Edwards Notation — describes exactly where every piece is on the board
// We need one FEN per position so Stockfish can analyze it
@Slf4j
@Component
public class PgnParser {

    public record ParsedGame(
        List<String> fenPositions,  // FEN before each move
        List<String> moves,         // moves in UCI format ("e2e4")
        String whiteName,
        String blackName,
        String result
    ) {}

    public ParsedGame parse(String pgn) {
        try {
            // MoveList can parse a PGN move section directly
            MoveList moveList = new MoveList();
            moveList.loadFromSan(extractMoveSection(pgn));

            Board board = new Board();
            List<String> fenPositions = new ArrayList<>();
            List<String> uciMoves = new ArrayList<>();

            for (Move move : moveList) {
                // Record FEN BEFORE this move is played
                fenPositions.add(board.getFen());
                uciMoves.add(move.toString()); // e.g. "e2e4"
                board.doMove(move);
            }

            String[] headers = extractHeaders(pgn);
            return new ParsedGame(fenPositions, uciMoves, headers[0], headers[1], headers[2]);

        } catch (Exception e) {
            log.error("Failed to parse PGN", e);
            throw new RuntimeException("PGN parsing failed", e);
        }
    }

    // Extracts the move section from PGN, stripping headers and annotations
    // MoveList.loadFromSan() expects clean SAN like "1. e4 e5 2. Nf3 Nc6"
    private String extractMoveSection(String pgn) {
        // Remove header lines (lines starting with "[")
        String movesSection = pgn.replaceAll("\\[.*?\\]\n?", "").trim();

        // Remove clock annotations like {[%clk 0:10:00]}
        movesSection = movesSection.replaceAll("\\{[^}]*\\}", "");

        // Remove result at end "1-0" "0-1" "1/2-1/2" "*"
        movesSection = movesSection.replaceAll("(1-0|0-1|1/2-1/2|\\*)\\s*$", "").trim();

        return movesSection;
    }

    // Extracts White, Black, Result from PGN headers
    private String[] extractHeaders(String pgn) {
        String white = extractHeader(pgn, "White");
        String black = extractHeader(pgn, "Black");
        String result = extractHeader(pgn, "Result");
        return new String[]{white, black, result};
    }

    private String extractHeader(String pgn, String key) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\[" + key + " \"([^\"]+)\"\\]")
                .matcher(pgn);
        return m.find() ? m.group(1) : "?";
    }
}
