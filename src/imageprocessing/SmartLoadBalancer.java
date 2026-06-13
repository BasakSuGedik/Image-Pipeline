package imageprocessing;

/**
 * SmartLoadBalancer.java
 *
 * Computes variable-height strip boundaries so that each thread receives
 * approximately equal total complexity — not just equal pixel count.
 *
 * Algorithm (prefix-sum partitioning):
 *   1. Compute total complexity = sum of all region scores
 *   2. Target per thread = total / threadCount
 *   3. Walk through regions accumulating complexity; emit a boundary
 *      whenever the accumulated value reaches the target.
 *
 * This ensures that threads processing high-edge areas get FEWER rows,
 * while threads processing smooth areas get MORE rows — balancing wall-
 * clock execution time across all threads.
 *
 * Example (4 threads, 8 regions):
 *   Scores:    [0.05, 0.05, 0.30, 0.30, 0.05, 0.05, 0.10, 0.10]
 *   Equal split:   rows 0-1 | 2-3 | 4-5 | 6-7
 *   Smart split:   rows 0-3 | 4   | 5   | 6-7  (heavy regions get 1 row each)
 */
public class SmartLoadBalancer {

    /**
     * Computes strip start/end row indices for each thread.
     *
     * @param complexityScores  per-region scores from ComplexityAnalyzer
     * @param imageHeight       total image height in pixels
     * @param threadCount       number of threads
     * @return int[threadCount][2] where [i][0] = startRow, [i][1] = endRow
     */
    public static int[][] computeStrips(float[] complexityScores,
                                        int imageHeight,
                                        int threadCount) {
        int numRegions   = complexityScores.length;
        int regionHeight = Math.max(1, imageHeight / numRegions);

        // Compute total complexity
        float totalComplexity = 0;
        for (float s : complexityScores) totalComplexity += s;

        // Handle edge case: all regions have zero complexity (solid color image)
        // Fall back to equal strips
        if (totalComplexity < 1e-6f) {
            return equalStrips(imageHeight, threadCount);
        }

        float targetPerThread = totalComplexity / threadCount;

        int[] stripStartRows = new int[threadCount];
        int[] stripEndRows   = new int[threadCount];

        int threadIdx  = 0;
        float accumulated = 0;
        stripStartRows[0] = 0;

        for (int r = 0; r < numRegions && threadIdx < threadCount - 1; r++) {
            accumulated += complexityScores[r];
            if (accumulated >= targetPerThread * (threadIdx + 1)) {
                // Place boundary after this region
                int boundaryRow = Math.min((r + 1) * regionHeight, imageHeight);
                stripEndRows[threadIdx]       = boundaryRow;
                stripStartRows[threadIdx + 1] = boundaryRow;
                threadIdx++;
            }
        }

        // Last thread always goes to the end of the image
        stripEndRows[threadCount - 1] = imageHeight;

        // Fill any threads that didn't get a boundary (can happen with very uneven scores)
        for (int i = 1; i < threadCount; i++) {
            if (stripStartRows[i] == 0 && stripEndRows[i-1] == 0) {
                stripStartRows[i] = stripStartRows[i-1];
                stripEndRows[i-1] = stripStartRows[i];
            }
        }

        // Print strip layout for analysis
        System.out.println("  [SmartLoadBalancer] Strip layout:");
        for (int i = 0; i < threadCount; i++) {
            int rows = stripEndRows[i] - stripStartRows[i];
            System.out.printf("    Thread %d: rows %4d - %4d  (%4d rows)%n",
                i, stripStartRows[i], stripEndRows[i], rows);
        }

        // Pack into 2D array
        int[][] strips = new int[threadCount][2];
        for (int i = 0; i < threadCount; i++) {
            strips[i][0] = stripStartRows[i];
            strips[i][1] = stripEndRows[i];
        }
        return strips;
    }

    /** Fallback: equal-height strips (used when complexity is uniform or zero). */
    public static int[][] equalStrips(int imageHeight, int threadCount) {
        int[][] strips = new int[threadCount][2];
        int stripHeight = imageHeight / threadCount;
        for (int i = 0; i < threadCount; i++) {
            strips[i][0] = i * stripHeight;
            strips[i][1] = (i == threadCount - 1) ? imageHeight : (i + 1) * stripHeight;
        }
        return strips;
    }
}
