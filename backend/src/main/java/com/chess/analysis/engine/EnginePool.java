package com.chess.analysis.engine;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

// Factory + Pool pattern: creates N Stockfish instances and hands them out to callers
// When a caller is done with an engine, it returns it to the pool
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

        // Create N Stockfish engine instances upfront (Factory pattern)
        log.info("Initializing {} Stockfish engine instances...", poolSize);
        for (int i = 0; i < poolSize; i++) {
            ChessEngine engine = new StockfishEngine(stockfishPath);
            pool.offer(engine);
            allEngines.add(engine);
        }
        log.info("Engine pool ready with {} instances", poolSize);
    }

    // Analyze a list of positions in parallel using all available engines
    // Uses CompletableFuture so all positions run concurrently
    public List<EngineResult> analyzeAll(List<String> fenPositions) {
        ExecutorService executor = Executors.newFixedThreadPool(allEngines.size());

        List<CompletableFuture<EngineResult>> futures = new ArrayList<>();

        for (String fen : fenPositions) {
            CompletableFuture<EngineResult> future = CompletableFuture.supplyAsync(() -> {
                ChessEngine engine = borrowEngine(); // blocks if all engines are busy
                try {
                    return engine.analyze(fen, depth, moveTimeMs);
                } finally {
                    returnEngine(engine); // always return, even if analysis throws
                }
            }, executor);
            futures.add(future);
        }

        // Wait for all analyses to complete, maintaining original order
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

    // Take an engine from the pool — blocks until one is available
    private ChessEngine borrowEngine() {
        try {
            return pool.take(); // blocks if pool is empty
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted waiting for engine", e);
        }
    }

    // Return an engine to the pool after use
    private void returnEngine(ChessEngine engine) {
        pool.offer(engine);
    }

    // Spring calls this on shutdown — clean up all Stockfish processes
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down engine pool...");
        allEngines.forEach(ChessEngine::shutdown);
    }
}
