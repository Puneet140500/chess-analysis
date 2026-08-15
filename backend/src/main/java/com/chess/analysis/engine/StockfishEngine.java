package com.chess.analysis.engine;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

// Communicates with a single Stockfish process via UCI protocol over stdin/stdout
// UCI (Universal Chess Interface) is a standard text protocol all chess engines support
@Slf4j
public class StockfishEngine implements ChessEngine {

    private final Process process;
    private final BufferedWriter writer;   // we write commands TO stockfish
    private final BufferedReader reader;   // we read responses FROM stockfish
    private final AtomicBoolean available; // thread-safe flag

    public StockfishEngine(String stockfishPath) {
        try {
            this.process = new ProcessBuilder(stockfishPath)
                    .redirectErrorStream(true)
                    .start();
            this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            this.available = new AtomicBoolean(true);

            sendCommand("uci");
            waitFor("uciok");  // Stockfish confirms it's in UCI mode
            sendCommand("isready");
            waitFor("readyok"); // Stockfish is ready to receive positions
        } catch (IOException e) {
            throw new RuntimeException("Failed to start Stockfish at: " + stockfishPath, e);
        }
    }

    @Override
    public EngineResult analyze(String fen, int depth, int moveTimeMs) {
        available.set(false);
        try {
            sendCommand("position fen " + fen);
            sendCommand("go depth " + depth + " movetime " + moveTimeMs);
            return parseResult();
        } catch (IOException e) {
            throw new RuntimeException("Error analyzing position: " + fen, e);
        } finally {
            available.set(true);
        }
    }


    // Reads Stockfish output line by line until "bestmove" appears
    // Along the way, captures the last "info" line which has the score
    private EngineResult parseResult() throws IOException {
        String bestMove = null;
        int centipawnScore = 0;
        int depth = 0;
        boolean isMate = false;
        int mateInMoves = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("info depth")) {
                // Example: "info depth 20 seldepth 30 multipv 1 score cp 45 nodes ..."
                // We parse the score from these lines
                if (line.contains("score cp")) {
                    centipawnScore = parseScoreCp(line);
                    depth = parseDepth(line);
                    isMate = false;
                } else if (line.contains("score mate")) {
                    mateInMoves = parseScoreMate(line);
                    centipawnScore = mateInMoves > 0 ? 32000 : -32000;
                    isMate = true;
                    depth = parseDepth(line);
                }
            } else if (line.startsWith("bestmove")) {
                // Example: "bestmove e2e4 ponder d7d5"
                String[] parts = line.split(" ");
                bestMove = parts[1]; // "e2e4"
                break;
            }
        }

        return EngineResult.builder()
                .bestMove(bestMove)
                .centipawnScore(centipawnScore)
                .depth(depth)
                .isMate(isMate)
                .mateInMoves(mateInMoves)
                .build();
    }

    private int parseScoreCp(String line) {
        // "... score cp 45 ..." → 45
        String[] parts = line.split(" ");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("cp")) {
                return Integer.parseInt(parts[i + 1]);
            }
        }
        return 0;
    }

    private int parseScoreMate(String line) {
        String[] parts = line.split(" ");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("mate")) {
                return Integer.parseInt(parts[i + 1]);
            }
        }
        return 0;
    }

    private int parseDepth(String line) {
        String[] parts = line.split(" ");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("depth")) {
                return Integer.parseInt(parts[i + 1]);
            }
        }
        return 0;
    }

    private void sendCommand(String command) throws IOException {
        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    // Reads lines until a line containing the expected string appears
    private void waitFor(String expected) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(expected)) break;
        }
    }

    @Override
    public boolean isAvailable() {
        return available.get();
    }

    @Override
    public void shutdown() {
        try {
            sendCommand("quit");
            process.destroy();
        } catch (IOException e) {
            log.warn("Error shutting down Stockfish", e);
        }
    }
}
