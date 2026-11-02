package com.rasterglyph.core;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Implements Floyd–Steinberg error diffusion dithering.
 *
 * In digital image processing and ASCII art, quantizing continuous tonal values
 * into a limited palette of characters often causes harsh "banding" or contouring.
 *
 * Floyd–Steinberg dithering diffuses the quantization error at each pixel to its
 * unvisited neighbors according to the classic distribution matrix:
 *
 *          [ Current Pixel (X) ]   -->  7/16 to (x+1, y)
 *    3/16 to (x-1, y+1)  |  5/16 to (x, y+1)  |  1/16 to (x+1, y+1)
 *
 * (7/16 + 3/16 + 5/16 + 1/16 = 16/16 = 1.0, preserving overall image energy).
 *
 * All operations return new {@link BufferedImage} instances and do not mutate the input image.
 */
public final class DitherFilter {

    private DitherFilter() {
        // Utility class; prevent instantiation
    }

    /**
     * Dithers an image into 1-bit binary black and white (2 levels: 0 and 255).
     *
     * @param source the input image.
     * @return a new 1-bit dithered {@link BufferedImage}.
     */
    public static BufferedImage ditherBinary(BufferedImage source) {
        return ditherGrayscale(source, 2);
    }

    /**
     * Dithers an image to a specified number of grayscale quantization levels.
     *
     * For example, passing levels = 10 matches the standard 10-character ASCII ramp,
     * ensuring smooth stippled transitions instead of abrupt character boundaries.
     *
     * @param source the input image.
     * @param levels number of discrete tonal levels (minimum 2).
     * @return a new dithered grayscale {@link BufferedImage}.
     * @throws NullPointerException if source is null.
     * @throws IllegalArgumentException if levels &lt; 2.
     */
    public static BufferedImage ditherGrayscale(BufferedImage source, int levels) {
        Objects.requireNonNull(source, "source image must not be null");
        if (levels < 2) {
            throw new IllegalArgumentException("Levels must be at least 2: " + levels);
        }

        int width = source.getWidth();
        int height = source.getHeight();

        // 1. Initialize working buffer with continuous luminance values
        double[][] buffer = new double[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer[y][x] = ImageProcessor.calculateLuminance(source.getRGB(x, y));
            }
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double step = 255.0 / (levels - 1);

        // 2. Perform Floyd–Steinberg error diffusion
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double oldVal = buffer[y][x];

                // Quantize to nearest discrete level
                int levelIndex = (int) Math.round(oldVal / step);
                levelIndex = Math.clamp(levelIndex, 0, levels - 1);
                double newVal = levelIndex * step;

                // Quantization error (difference between original and quantized value)
                double error = oldVal - newVal;

                // Set quantized pixel in result image
                int gray = (int) Math.round(newVal);
                gray = Math.clamp(gray, 0, 255);
                int rgb = (gray << 16) | (gray << 8) | gray;
                result.setRGB(x, y, rgb);

                // Diffuse error to unvisited neighboring pixels
                // Right: (x + 1, y) -> 7/16
                if (x + 1 < width) {
                    buffer[y][x + 1] += error * (7.0 / 16.0);
                }
                // Down-Left: (x - 1, y + 1) -> 3/16
                if (x - 1 >= 0 && y + 1 < height) {
                    buffer[y + 1][x - 1] += error * (3.0 / 16.0);
                }
                // Down: (x, y + 1) -> 5/16
                if (y + 1 < height) {
                    buffer[y + 1][x] += error * (5.0 / 16.0);
                }
                // Down-Right: (x + 1, y + 1) -> 1/16
                if (x + 1 < width && y + 1 < height) {
                    buffer[y + 1][x + 1] += error * (1.0 / 16.0);
                }
            }
        }

        return result;
    }

    /**
     * Dithers an image in full RGB color by diffusing errors across R, G, and B
     * channels independently.
     *
     * @param source the input image.
     * @param levelsPerChannel discrete levels per color channel (e.g. 4 or 8).
     * @return a new dithered color {@link BufferedImage}.
     * @throws NullPointerException if source is null.
     * @throws IllegalArgumentException if levelsPerChannel &lt; 2.
     */
    public static BufferedImage ditherColor(BufferedImage source, int levelsPerChannel) {
        Objects.requireNonNull(source, "source image must not be null");
        if (levelsPerChannel < 2) {
            throw new IllegalArgumentException("levelsPerChannel must be at least 2: " + levelsPerChannel);
        }

        int width = source.getWidth();
        int height = source.getHeight();

        // 3 separate channel buffers to avoid color crosstalk
        double[][] rBuf = new double[height][width];
        double[][] gBuf = new double[height][width];
        double[][] bBuf = new double[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                rBuf[y][x] = (rgb >> 16) & 0xFF;
                gBuf[y][x] = (rgb >> 8) & 0xFF;
                bBuf[y][x] = rgb & 0xFF;
            }
        }

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        double step = 255.0 / (levelsPerChannel - 1);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rQuant = quantizeChannel(rBuf[y][x], step, levelsPerChannel);
                int gQuant = quantizeChannel(gBuf[y][x], step, levelsPerChannel);
                int bQuant = quantizeChannel(bBuf[y][x], step, levelsPerChannel);

                double rErr = rBuf[y][x] - rQuant;
                double gErr = gBuf[y][x] - gQuant;
                double bErr = bBuf[y][x] - bQuant;

                result.setRGB(x, y, (rQuant << 16) | (gQuant << 8) | bQuant);

                diffuseChannel(rBuf, x, y, width, height, rErr);
                diffuseChannel(gBuf, x, y, width, height, gErr);
                diffuseChannel(bBuf, x, y, width, height, bErr);
            }
        }

        return result;
    }

    private static int quantizeChannel(double val, double step, int levels) {
        int index = (int) Math.round(val / step);
        index = Math.clamp(index, 0, levels - 1);
        int result = (int) Math.round(index * step);
        return Math.clamp(result, 0, 255);
    }

    private static void diffuseChannel(double[][] buf, int x, int y, int width, int height, double err) {
        if (x + 1 < width) {
            buf[y][x + 1] += err * (7.0 / 16.0);
        }
        if (x - 1 >= 0 && y + 1 < height) {
            buf[y + 1][x - 1] += err * (3.0 / 16.0);
        }
        if (y + 1 < height) {
            buf[y + 1][x] += err * (5.0 / 16.0);
        }
        if (x + 1 < width && y + 1 < height) {
            buf[y + 1][x + 1] += err * (1.0 / 16.0);
        }
    }
}
