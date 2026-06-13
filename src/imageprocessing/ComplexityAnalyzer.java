package imageprocessing;

import java.awt.image.BufferedImage;

/**
 * ComplexityAnalyzer.java
 *
 * Performs a fast O(width * height) pre-scan of the input image to compute
 * a complexity score for each candidate horizontal strip region.
 *
 * Complexity = average absolute difference between adjacent pixels in a row.
 * High score  = lots of edges / texture = expensive to process (Sobel is heavy here).
 * Low score   = smooth / flat region    = cheap to process.
 *
 * This score is used by:
 *   1. AdaptiveThreadAllocator  — to choose how many threads to spawn
 *   2. SmartLoadBalancer        — to create variable-width strips
 */
public class ComplexityAnalyzer {

    /**
     * Splits the image into `numRegions` equal-height candidate regions,
     * computes a complexity score for each, and returns the score array.
     *
     * @param image      input image
     * @param numRegions number of candidate regions (typically 16 or 32)
     * @return float[] of length numRegions, each value in [0.0, 1.0]
     */
    public static float[] analyze(BufferedImage image, int numRegions) {
        int width  = image.getWidth();
        int height = image.getHeight();
        float[] scores = new float[numRegions];

        int regionHeight = Math.max(1, height / numRegions);

        for (int r = 0; r < numRegions; r++) {
            int startRow = r * regionHeight;
            int endRow   = (r == numRegions - 1) ? height : (r + 1) * regionHeight;

            float totalDiff = 0;
            long pixelCount = 0;

            for (int y = startRow; y < endRow; y++) {
                for (int x = 1; x < width; x++) {
                    // Convert to grayscale using luminance weights
                    int rgb1 = image.getRGB(x - 1, y);
                    int rgb2 = image.getRGB(x,     y);

                    float gray1 = 0.299f * ((rgb1 >> 16) & 0xFF)
                                + 0.587f * ((rgb1 >>  8) & 0xFF)
                                + 0.114f * ( rgb1        & 0xFF);
                    float gray2 = 0.299f * ((rgb2 >> 16) & 0xFF)
                                + 0.587f * ((rgb2 >>  8) & 0xFF)
                                + 0.114f * ( rgb2        & 0xFF);

                    totalDiff += Math.abs(gray1 - gray2);
                    pixelCount++;
                }
            }

            // Normalize to [0, 1] (max possible diff per pixel = 255)
            scores[r] = pixelCount > 0 ? (totalDiff / pixelCount) / 255.0f : 0f;
        }

        return scores;
    }

    /**
     * Returns the average complexity score across all regions.
     * Used by the adaptive thread allocator.
     */
    public static float averageScore(float[] scores) {
        float sum = 0;
        for (float s : scores) sum += s;
        return sum / scores.length;
    }

    /** Prints a visual bar chart of region complexity to the console. */
    public static void printProfile(float[] scores) {
        System.out.println("\n  [Complexity Profile — each bar = one region]");
        for (int i = 0; i < scores.length; i++) {
            int bars = (int) (scores[i] * 40);
            System.out.printf("  Region %2d [%.3f] %s%n",
                i, scores[i], "#".repeat(Math.max(0, bars)));
        }
        System.out.printf("  Average complexity: %.4f%n%n", averageScore(scores));
    }
}
