package com.chess.analysis.engine;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class StockfishEngine implements ChessEngine {

    private final Process process;
    private final BufferedWriter writer;
    private final BufferedReader reader;
    private final AtomicBoolean available;

    public StockfishEngine(String stockfishPath) {
        try {
            this.process = new ProcessBuilder(stockfishPath)
                    .redirectErrorStream(true)
                    .start();
            this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            this.available = new AtomicBoolean(true);

            sendCommand("uci");
            waitFor("uciok");
            sendCommand("setoption name UCI_ShowWDL value true");
            sendCommand("isready");
            waitFor("readyok");
        } catch (IOException e) {
            throw new RuntimeException("Failed to start Stockfish at: " + stockfishPath, e);
        }
    }

    @Override
    public EngineResult analyze(String fen, int depth, int moveTimeMs) {
        available.set(false);
        try {
            sendCommand("position fen " + fen);
            sendCommand("go depth " + depth);
            return parseResult();
        } catch (IOException e) {
            throw new RuntimeException("Error analyzing position: " + fen, e);
        } finally {
            available.set(true);
        }
    }

    private EngineResult parseResult() throws IOException {
        String bestMove = null;
        int centipawnScore = 0;
        int wdlWin = 0, wdlDraw = 0, wdlLoss = 0;
        int depth = 0;
        boolean isMate = false;
        int mateInMoves = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("info depth")) {
                int d = parseToken(line, "depth");
                if (line.contains("score cp")) {
                    centipawnScore = parseToken(line, "cp");
                    depth = d;
                    isMate = false;
                    // parse WDL if present: "wdl W D L"
                    int wdlIdx = line.indexOf(" wdl ");
                    if (wdlIdx >= 0) {
                        String[] parts = line.substring(wdlIdx + 5).split(" ");
                        if (parts.length >= 3) {
                            try {
                                wdlWin  = Integer.parseInt(parts[0]);
                                wdlDraw = Integer.parseInt(parts[1]);
                                wdlLoss = Integer.parseInt(parts[2]);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                } else if (line.contains("score mate")) {
                    mateInMoves = parseToken(line, "mate");
                    centipawnScore = mateInMoves > 0 ? 32000 : -32000;
                    wdlWin  = mateInMoves > 0 ? 1000 : 0;
                    wdlDraw = 0;
                    wdlLoss = mateInMoves > 0 ? 0 : 1000;
                    isMate = true;
                    depth = d;
                }
            } else if (line.startsWith("bestmove")) {
                String[] parts = line.split(" ");
                if (parts.length > 1) bestMove = parts[1];
                break;
            }
        }

        return EngineResult.builder()
                .bestMove(bestMove)
                .centipawnScore(centipawnScore)
                .wdlWin(wdlWin)
                .wdlDraw(wdlDraw)
                .wdlLoss(wdlLoss)
                .depth(depth)
                .isMate(isMate)
                .mateInMoves(mateInMoves)
                .build();
    }

    private int parseToken(String line, String token) {
        String[] parts = line.split(" ");
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals(token)) {
                try { return Integer.parseInt(parts[i + 1]); } catch (NumberFormatException e) { return 0; }
            }
        }
        return 0;
    }

    private void sendCommand(String command) throws IOException {
        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    private void waitFor(String expected) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(expected)) break;
        }
    }

    @Override
    public boolean isAvailable() { return available.get(); }

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
