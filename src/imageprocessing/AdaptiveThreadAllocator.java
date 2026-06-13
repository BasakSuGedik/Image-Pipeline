package imageprocessing;

import java.awt.image.BufferedImage;

/**
 * AdaptiveThreadAllocator.java
 *
 * Automatically determines the optimal thread count for a given image
 * based on two factors:
 *   1. Image dimensions (pixels to process)
 *   2. Average complexity score from ComplexityAnalyzer
 *
 * Thresholds are calibrated for real photographs where complexity
 * scores typically range from 0.005 to 0.050.
 *
 * Decision table:
 * ┌──────────────┬──────────────┬──────────────────┬─────────────────┐
 * │ Width        │ Height       │ Avg Complexity   │ Thread Count    │
 * ├──────────────┼──────────────┼──────────────────┼─────────────────┤
 * │ < 512        │ < 512        │ any              │ 1 (sequential)  │
 * │ 512–1023     │ 512–1023     │ any              │ 2               │
 * │ 1024–1919    │ 1024–1079    │ < 0.008          │ 2               │
 * │ 1024–1919    │ 1024–1079    │ >= 0.008         │ 4               │
 * │ >= 1920      │ >= 1080      │ < 0.010          │ 4               │
 * │ >= 1920      │ >= 1080      │ >= 0.010         │ 8               │
 * └──────────────┴──────────────┴──────────────────┴─────────────────┘
 */
public class AdaptiveThreadAllocator {

    public static int allocate(BufferedImage image, float avgComplexity) {
        int width   = image.getWidth();
        int height  = image.getHeight();
        int maxCores = Runtime.getRuntime().availableProcessors();

        int recommended;

        if (width < 512 && height < 512) {
            // Very small — thread overhead exceeds benefit
            recommended = 1;

        } else if (width < 1024 && height < 1024) {
            // Small image
            recommended = 2;

        } else if (width < 1920 && height < 1080) {
            // Medium image (720p range)
            recommended = avgComplexity < 0.008f ? 2 : 4;

        } else {
            // Large image (1080p and above)
            recommended = avgComplexity < 0.010f ? 4 : 8;
        }

        // Never exceed available CPU cores
        int finalCount = Math.min(recommended, maxCores);

        System.out.printf("  [AdaptiveAllocator] Image: %dx%d | Complexity: %.4f | Cores: %d | Threads: %d%n",
            width, height, avgComplexity, maxCores, finalCount);

        return finalCount;
    }
}
