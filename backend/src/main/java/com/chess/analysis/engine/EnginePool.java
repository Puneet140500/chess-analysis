package com.chess.analysis.engine;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Component
public class EnginePool {

    private final BlockingQueue<ChessEngine> pool;
    private final List<ChessEngine> allEngines;
    private final int depth;
    private final int moveTimeMs;

    public EnginePool(
            @Value("${stockfish.path}") String stockfishPath,
            @Value("${stockfish.pool.size}") int poolSize,
            @Value("${stockfish.depth}") int depth,
            @Value("${stockfish.movetime.ms}") int moveTimeMs) {

        this.depth = depth;
        this.moveTimeMs = moveTimeMs;
        this.pool = new LinkedBlockingQueue<>(poolSize);
        this.allEngines = new ArrayList<>(poolSize);

        log.info("Initializing {} Stockfish engine instances...", poolSize);
        for (int i = 0; i < poolSize; i++) {
            ChessEngine engine = new StockfishEngine(stockfishPath);
            pool.offer(engine);
            allEngines.add(engine);
        }
        log.info("Engine pool ready with {} instances", poolSize);
    }

    public List<EngineResult> analyzeAll(List<String> fenPositions) {
        ExecutorService executor = Executors.newFixedThreadPool(allEngines.size());
        List<CompletableFuture<EngineResult>> futures = new ArrayList<>();

        for (String fen : fenPositions) {
            CompletableFuture<EngineResult> future = CompletableFuture.supplyAsync(() -> {
                ChessEngine engine = borrowEngine();
                try {
                    return engine.analyze(fen, depth, moveTimeMs);
                } finally {
                    returnEngine(engine);
                }
            }, executor);
            futures.add(future);
        }

        List<EngineResult> results = new ArrayList<>();
        for (CompletableFuture<EngineResult> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Engine analysis failed", e);
            }
        }
        executor.shutdown();
        return results;
    }

    private ChessEngine borrowEngine() {
        try {
            return pool.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for engine", e);
        }
    }

    private void returnEngine(ChessEngine engine) {
        pool.offer(engine);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down engine pool...");
        allEngines.forEach(ChessEngine::shutdown);
    }
}
