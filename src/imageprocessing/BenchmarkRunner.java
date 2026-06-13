package imageprocessing;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * BenchmarkRunner.java  —  MAIN CLASS
 *
 * Benchmarks all four execution modes for each image resolution:
 *   Mode 1 — Sequential          (1 thread, equal strips)
 *   Mode 2 — Naive Parallel      (fixed 4 threads, equal strips)
 *   Mode 3 — Adaptive Only       (auto thread count, equal strips)
 *   Mode 4 — Adaptive + Balanced (auto thread count, smart strips) <- our contribution
 *
 * For each test:
 *   - 1 warm-up run (discarded)
 *   - 5 measured runs (averaged)
 *
 * Output:
 *   - Console: live progress + summary tables
 *   - results/benchmark_results.txt : full log
 *   - results/images/               : output images for visual inspection
 *
 * HOW TO RUN:
 *   Windows: double-click run.bat
 *   Manual:  javac -d out src\imageprocessing\*.java
 *             java -cp out imageprocessing.BenchmarkRunner
 */
public class BenchmarkRunner {

    private static final int[][] RESOLUTIONS = {
        {512,  512 },
        {1024, 1024},
        {2048, 2048},
        {3840, 2160},
    };
    private static final String[] RES_LABELS = {
        "512x512", "1024x1024", "2048x2048", "3840x2160 (4K)"
    };
    private static final int RUNS = 5;

    public static void main(String[] args) throws Exception {
        new File("results/images").mkdirs();
        PrintWriter log = new PrintWriter(new FileWriter("results/benchmark_results.txt"));

        printBanner(log);

        System.out.println("================================================================");
        System.out.println("  CENG-479 Adaptive Image Pipeline Benchmark");
        System.out.println("  Basak Su Gedik (21118080072) & Salih Kirlioglu (21118080019)");
        System.out.println("================================================================");

        long[][]   times   = new long[RESOLUTIONS.length][4];
        double[][] balance = new double[RESOLUTIONS.length][4];
        int[][]    usedThreads = new int[RESOLUTIONS.length][4];

        for (int ri = 0; ri < RESOLUTIONS.length; ri++) {
            int w = RESOLUTIONS[ri][0];
            int h = RESOLUTIONS[ri][1];
            String label = RES_LABELS[ri];

            System.out.println("\n--- Resolution: " + label + " ---");
            log.println("\n--- Resolution: " + label + " ---");
            System.out.print("  Generating test image... ");
            BufferedImage input = ImageGenerator.generate(w, h);
            System.out.println("done.");

            // MODE 1: Sequential
            {
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                SequentialPipeline.run(input, out); // warm-up
                long total = 0;
                for (int r = 0; r < RUNS; r++) {
                    out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    total += SequentialPipeline.run(input, out);
                }
                times[ri][0] = total / RUNS;
                balance[ri][0] = 1.0;
                usedThreads[ri][0] = 1;
                ImageIO.write(out, "png", new File("results/images/" + w + "x" + h + "_seq.png"));
                report("Mode 1  Sequential         ", times[ri][0], 1, 1.0, log);
            }

            // MODE 2: Naive Parallel (fixed 4 threads, equal strips)
            {
                int n = Math.min(4, Runtime.getRuntime().availableProcessors());
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                ParallelPipeline.run(input, out, n); // warm-up
                long total = 0;
                for (int r = 0; r < RUNS; r++) {
                    out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    total += ParallelPipeline.run(input, out, n);
                }
                times[ri][1] = total / RUNS;
                balance[ri][1] = -1; // not measured
                usedThreads[ri][1] = n;
                report("Mode 2  Naive Parallel     ", times[ri][1], n, -1, log);
            }

            // MODE 3: Adaptive thread count, equal strips
            {
                int numRegions = Math.min(32, h);
                float[] scores = ComplexityAnalyzer.analyze(input, numRegions);
                float avgC = ComplexityAnalyzer.averageScore(scores);
                int n = AdaptiveThreadAllocator.allocate(input, avgC);
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                ParallelPipeline.run(input, out, n); // warm-up
                long total = 0;
                for (int r = 0; r < RUNS; r++) {
                    out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    total += ParallelPipeline.run(input, out, n);
                }
                times[ri][2] = total / RUNS;
                balance[ri][2] = -1;
                usedThreads[ri][2] = n;
                report("Mode 3  Adaptive Only      ", times[ri][2], n, -1, log);
            }

            // MODE 4: Adaptive + Smart Load Balancing
            {
                BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                AdaptivePipeline.run(input, out); // warm-up
                long totalMs = 0;
                double totalBalance = 0;
                int n = 0;
                for (int r = 0; r < RUNS; r++) {
                    out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    AdaptivePipeline.PipelineResult res = AdaptivePipeline.run(input, out);
                    totalMs += res.totalMs;
                    totalBalance += res.loadBalanceRatio();
                    n = res.threadCount;
                }
                times[ri][3] = totalMs / RUNS;
                balance[ri][3] = totalBalance / RUNS;
                usedThreads[ri][3] = n;
                ImageIO.write(out, "png", new File("results/images/" + w + "x" + h + "_adaptive.png"));
                report("Mode 4  Adaptive+Balanced  ", times[ri][3], n, balance[ri][3], log);
            }

            // Speedup summary
            System.out.println("\n  Speedup vs Sequential:");
            log.println("\n  Speedup vs Sequential:");
            String[] names = {"Sequential", "Naive Parallel", "Adaptive Only", "Adaptive+Balanced"};
            for (int m = 0; m < 4; m++) {
                double s = (double) times[ri][0] / times[ri][m];
                String line = String.format("    %-20s : %.2fx speedup  (%d threads)", names[m], s, usedThreads[ri][m]);
                System.out.println(line);
                log.println(line);
            }
        }

        // Final summary table
        System.out.println("\n================================================================");
        System.out.println("  SUMMARY TABLE — Average Execution Time (ms)");
        System.out.println("================================================================");
        log.println("\n================================================================");
        log.println("  SUMMARY TABLE — Average Execution Time (ms)");
        log.println("================================================================");
        String hdr = String.format("  %-20s %8s %8s %8s %8s", "Resolution","Seq","Naive","Adapt.","Adpt+Bal");
        System.out.println(hdr); log.println(hdr);
        System.out.println("  " + "-".repeat(56)); log.println("  " + "-".repeat(56));
        for (int ri = 0; ri < RESOLUTIONS.length; ri++) {
            String row = String.format("  %-20s %8d %8d %8d %8d",
                RES_LABELS[ri], times[ri][0], times[ri][1], times[ri][2], times[ri][3]);
            System.out.println(row); log.println(row);
        }

        System.out.println("\nResults saved to: results/benchmark_results.txt");
        System.out.println("Images saved to : results/images/");
        System.out.println("Benchmark complete!");
        log.println("\nBenchmark complete.");
        log.flush(); log.close();
    }

    private static void report(String label, long ms, int threads, double balance, PrintWriter log) {
        String balStr = balance < 0 ? "  N/A  " : String.format("%.3f", balance);
        String line = String.format("  %-28s avg=%5d ms  threads=%d  balance=%s",
            label, ms, threads, balStr);
        System.out.println(line);
        log.println(line);
    }

    private static void printBanner(PrintWriter log) {
        log.println("================================================================");
        log.println("  CENG-479 Parallel Programming — Submission 2");
        log.println("  Adaptive Parallel Image Processing Pipeline");
        log.println("  Basak Su Gedik   — 21118080072");
        log.println("  Salih Kirlioglu  — 21118080019");
        log.println("  Gazi Universitesi, Spring 2026");
        log.println("  Modes: Sequential | Naive | Adaptive | Adaptive+Balanced");
        log.println("  Filters: Gaussian Blur > Sobel Edge > Brightness/Contrast");
        log.println("  Runs per test: " + RUNS + " (+ 1 warm-up)");
        log.println("================================================================");
    }
}
