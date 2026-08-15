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

    // Analyze a list of FEN positions — returns best move + score for each
    public List<EngineResult> analyzeAll(List<String> fenPositions) {
        return runParallel(fenPositions.stream()
                .map(fen -> (java.util.function.Supplier<EngineResult>)
                        () -> borrowAndRun(engine -> engine.analyze(fen, depth, moveTimeMs)))
                .toList());
    }

    // For each (fen, playedMove) pair, evaluate ONLY the played move from that position
    // This gives a score consistent with analyzeAll() — both evaluated from the same position
    public List<EngineResult> analyzePlayedMoves(List<String> fenPositions, List<String> playedMoves) {
        List<java.util.function.Supplier<EngineResult>> tasks = new ArrayList<>();
        for (int i = 0; i < fenPositions.size(); i++) {
            final String fen  = fenPositions.get(i);
            final String move = playedMoves.get(i);
            tasks.add(() -> borrowAndRun(engine -> engine.analyzeMove(fen, move, depth, moveTimeMs)));
        }
        return runParallel(tasks);
    }

    private EngineResult borrowAndRun(java.util.function.Function<ChessEngine, EngineResult> task) {
        ChessEngine engine = borrowEngine();
        try {
            return task.apply(engine);
        } finally {
            returnEngine(engine);
        }
    }

    private List<EngineResult> runParallel(List<java.util.function.Supplier<EngineResult>> tasks) {
        ExecutorService executor = Executors.newFixedThreadPool(allEngines.size());
        List<CompletableFuture<EngineResult>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(task::get, executor))
                .toList();

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
