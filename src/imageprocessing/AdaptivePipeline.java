package imageprocessing;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * AdaptivePipeline.java
 *
 * The full adaptive pipeline combining all three layers:
 *   Layer 1 — ComplexityAnalyzer   : pre-scan image, compute complexity scores
 *   Layer 2 — AdaptiveThreadAllocator : choose thread count based on size + complexity
 *   Layer 3 — SmartLoadBalancer    : compute variable-width strips
 *              + parallel filter execution (Gaussian → Sobel → Brightness/Contrast)
 *
 * Returns a PipelineResult containing:
 *   - elapsed time in milliseconds
 *   - thread count actually used
 *   - per-thread execution times (for load balance analysis)
 */
public class AdaptivePipeline {

    public static PipelineResult run(BufferedImage input, BufferedImage output)
            throws InterruptedException, ExecutionException {

        int w = input.getWidth();
        int h = input.getHeight();

        // ── Layer 1: Complexity Analysis ─────────────────────────────────────
        int numRegions = Math.min(32, h); // at most 32 candidate regions
        long t0 = System.nanoTime();
        float[] complexityScores = ComplexityAnalyzer.analyze(input, numRegions);
        long analyzerMs = (System.nanoTime() - t0) / 1_000_000;
        float avgComplexity = ComplexityAnalyzer.averageScore(complexityScores);

        // ── Layer 2: Adaptive Thread Allocation ───────────────────────────────
        int threadCount = AdaptiveThreadAllocator.allocate(input, avgComplexity);

        // ── Layer 3a: Smart Load Balancing ────────────────────────────────────
        int[][] strips = SmartLoadBalancer.computeStrips(complexityScores, h, threadCount);

        // ── Layer 3b: Parallel Filter Execution ───────────────────────────────
        BufferedImage tmp1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        BufferedImage tmp2 = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        long[] threadTimes = new long[threadCount];

        long pipelineStart = System.nanoTime();

        // Stage 1 — Gaussian Blur
        runStageAndTime(pool, threadCount, strips, threadTimes, (i) ->
            Filters.gaussianBlur(input, tmp1, strips[i][0], strips[i][1])
        );

        // Stage 2 — Sobel Edge Detection
        runStageAndTime(pool, threadCount, strips, threadTimes, (i) ->
            Filters.sobelEdge(tmp1, tmp2, strips[i][0], strips[i][1])
        );

        // Stage 3 — Brightness / Contrast
        runStageAndTime(pool, threadCount, strips, threadTimes, (i) ->
            Filters.brightnessContrast(tmp2, output, strips[i][0], strips[i][1], 1.2f, 15f)
        );

        long pipelineMs = (System.nanoTime() - pipelineStart) / 1_000_000;

        pool.shutdown();

        return new PipelineResult(pipelineMs, analyzerMs, threadCount, threadTimes, avgComplexity);
    }

    private static void runStageAndTime(ExecutorService pool, int threadCount,
                                        int[][] strips, long[] threadTimes,
                                        ThreadTask task)
            throws InterruptedException, ExecutionException {

        List<Future<Long>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                long start = System.nanoTime();
                task.execute(idx);
                return (System.nanoTime() - start) / 1_000_000;
            }));
        }

        for (int i = 0; i < threadCount; i++) {
            threadTimes[i] += futures.get(i).get();
        }
    }

    @FunctionalInterface
    interface ThreadTask {
        void execute(int threadIndex);
    }

    // ── Result container ──────────────────────────────────────────────────────

    public static class PipelineResult {
        public final long   totalMs;
        public final long   analyzerMs;
        public final int    threadCount;
        public final long[] threadTimes;
        public final float  avgComplexity;

        public PipelineResult(long totalMs, long analyzerMs, int threadCount,
                              long[] threadTimes, float avgComplexity) {
            this.totalMs       = totalMs;
            this.analyzerMs    = analyzerMs;
            this.threadCount   = threadCount;
            this.threadTimes   = threadTimes;
            this.avgComplexity = avgComplexity;
        }

        /** Load balance ratio: max thread time / min thread time. Ideal = 1.0 */
        public double loadBalanceRatio() {
            if (threadTimes.length <= 1) return 1.0;
            long max = 0, min = Long.MAX_VALUE;
            for (long t : threadTimes) {
                if (t > max) max = t;
                if (t < min) min = t;
            }
            return min > 0 ? (double) max / min : 1.0;
        }

        /** Analyzer overhead as percentage of total pipeline time. */
        public double analyzerOverheadPct() {
            return totalMs > 0 ? (double) analyzerMs / totalMs * 100.0 : 0;
        }
    }
}
