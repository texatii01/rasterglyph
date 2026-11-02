package com.rasterglyph.core;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Implements the Sobel Operator for edge detection and structural ASCII line art.
 *
 * Uses two 3x3 convolution kernels:
 *
 *        Gx (Horizontal gradient):             Gy (Vertical gradient):
 *          [-1   0  +1]                            [-1  -2  -1]
 *          [-2   0  +2]                            [ 0   0   0]
 *          [-1   0  +1]                            [+1  +2  +1]
 *
 * Capabilities:
 * 1. Computes edge magnitude: G = sqrt(Gx^2 + Gy^2)
 * 2. Computes edge direction: angle = atan2(Gy, Gx)
 * 3. Maps structural edge directions to ASCII stroke glyphs:
 *    - Vertical edge (Gx change):  '|'
 *    - Horizontal edge (Gy change): '-'
 *    - Diagonal positive slope:    '/'
 *    - Diagonal negative slope:    '\'
 *
 * All operations return new instances and do not mutate input images.
 */
public final class SobelEdgeDetector {

    /**
     * Default edge threshold (gradient magnitude out of 255).
     * Edges below this threshold are treated as non-edges.
     */
    public static final double DEFAULT_THRESHOLD = 50.0;

    private static final int[][] KERNEL_GX = {
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
    };

    private static final int[][] KERNEL_GY = {
            {-1, -2, -1},
            { 0,  0,  0},
            { 1,  2,  1}
    };

    private SobelEdgeDetector() {
        // Utility class; prevent instantiation
    }

    /**
     * Detects edges in an image and returns a grayscale edge-magnitude {@link BufferedImage}.
     * Edge lines appear bright on a dark background.
     *
     * @param source the input image.
     * @return a new edge-detected {@link BufferedImage}.
     */
    public static BufferedImage detectEdges(BufferedImage source) {
        return detectEdges(source, 0.0);
    }

    /**
     * Detects edges with a minimum magnitude threshold to suppress background noise.
     *
     * @param source the input image.
     * @param threshold magnitude threshold in range [0, 255].
     * @return a new edge-detected {@link BufferedImage}.
     */
    public static BufferedImage detectEdges(BufferedImage source, double threshold) {
        Objects.requireNonNull(source, "source image must not be null");

        int width = source.getWidth();
        int height = source.getHeight();

        double[][] luminance = extractLuminanceGrid(source);
        BufferedImage edgeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double magnitude = computeMagnitude(luminance, x, y, width, height);

                int intensity = 0;
                if (magnitude >= threshold) {
                    intensity = Math.clamp((int) Math.round(magnitude), 0, 255);
                }

                int rgb = (intensity << 16) | (intensity << 8) | intensity;
                edgeImage.setRGB(x, y, rgb);
            }
        }

        return edgeImage;
    }

    /**
     * Converts an image directly to structural ASCII line art strokes ('|', '-', '/', '\').
     *
     * @param image input image.
     * @param targetWidth desired ASCII width in characters.
     * @param threshold gradient magnitude threshold for edge detection.
     * @return generated line-art {@link AsciiResult}.
     */
    public static AsciiResult toEdgeAscii(BufferedImage image, int targetWidth, double threshold) {
        Objects.requireNonNull(image, "image must not be null");

        // Scale image to target ASCII dimensions first
        BufferedImage scaled = ImageResizer.resize(image, targetWidth);
        int width = scaled.getWidth();
        int height = scaled.getHeight();

        double[][] luminance = extractLuminanceGrid(scaled);
        char[][] grid = new char[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = computeEdgeCharacter(luminance, x, y, width, height, threshold);
            }
        }

        return new AsciiResult(grid);
    }

    /**
     * Overlays detected structural edge strokes on top of a shaded {@link AsciiResult},
     * producing a crisp cel-shaded or comic-book aesthetic.
     *
     * @param baseResult the underlying shaded ASCII result.
     * @param scaledImage the pre-scaled image matching the dimensions of baseResult.
     * @param threshold gradient magnitude threshold for edges.
     * @return combined cel-shaded {@link AsciiResult}.
     */
    public static AsciiResult overlayEdges(AsciiResult baseResult, BufferedImage scaledImage, double threshold) {
        Objects.requireNonNull(baseResult, "baseResult must not be null");
        Objects.requireNonNull(scaledImage, "scaledImage must not be null");

        int width = baseResult.getWidth();
        int height = baseResult.getHeight();

        if (scaledImage.getWidth() != width || scaledImage.getHeight() != height) {
            throw new IllegalArgumentException(
                    "Image dimensions (" + scaledImage.getWidth() + "x" + scaledImage.getHeight() +
                    ") must match baseResult dimensions (" + width + "x" + height + ")"
            );
        }

        double[][] luminance = extractLuminanceGrid(scaledImage);
        char[][] combined = baseResult.getGridCopy();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char edgeChar = computeEdgeCharacter(luminance, x, y, width, height, threshold);
                // If this pixel is a detected structural edge, overlay it over the background shade
                if (edgeChar != ' ') {
                    combined[y][x] = edgeChar;
                }
            }
        }

        return new AsciiResult(combined);
    }

    private static char computeEdgeCharacter(double[][] lum, int x, int y, int width, int height, double threshold) {
        double gx = computeGx(lum, x, y, width, height);
        double gy = computeGy(lum, x, y, width, height);

        // Normalize gradient magnitude: maximum possible theoretical value is sqrt(4*255^2 + 4*255^2) ≈ 1442.5
        double magnitude = Math.hypot(gx, gy) * (255.0 / 1442.5);

        if (magnitude < threshold) {
            return ' ';
        }

        // Calculate gradient angle in degrees [0, 180)
        double angle = (Math.toDegrees(Math.atan2(gy, gx)) + 180.0) % 180.0;

        // Map gradient direction to physical edge stroke:
        // Gradient vector is perpendicular to the physical edge line
        if (angle < 22.5 || angle >= 157.5) {
            return '|'; // Horizontal gradient -> Vertical edge
        } else if (angle >= 22.5 && angle < 67.5) {
            return '/'; // Diagonal gradient -> Diagonal edge (bottom-left to top-right)
        } else if (angle >= 67.5 && angle < 112.5) {
            return '-'; // Vertical gradient -> Horizontal edge
        } else {
            return '\\'; // Diagonal gradient -> Diagonal edge (top-left to bottom-right)
        }
    }

    private static double computeMagnitude(double[][] lum, int x, int y, int width, int height) {
        double gx = computeGx(lum, x, y, width, height);
        double gy = computeGy(lum, x, y, width, height);
        return Math.hypot(gx, gy) * (255.0 / 1442.5);
    }

    private static double computeGx(double[][] lum, int x, int y, int width, int height) {
        double sum = 0.0;
        for (int ky = -1; ky <= 1; ky++) {
            int py = Math.clamp(y + ky, 0, height - 1);
            for (int kx = -1; kx <= 1; kx++) {
                int px = Math.clamp(x + kx, 0, width - 1);
                sum += lum[py][px] * KERNEL_GX[ky + 1][kx + 1];
            }
        }
        return sum;
    }

    private static double computeGy(double[][] lum, int x, int y, int width, int height) {
        double sum = 0.0;
        for (int ky = -1; ky <= 1; ky++) {
            int py = Math.clamp(y + ky, 0, height - 1);
            for (int kx = -1; kx <= 1; kx++) {
                int px = Math.clamp(x + kx, 0, width - 1);
                sum += lum[py][px] * KERNEL_GY[ky + 1][kx + 1];
            }
        }
        return sum;
    }

    private static double[][] extractLuminanceGrid(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double[][] grid = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = ImageProcessor.calculateLuminance(image.getRGB(x, y));
            }
        }

        return grid;
    }
}
