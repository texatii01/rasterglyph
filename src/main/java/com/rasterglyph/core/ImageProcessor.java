package com.rasterglyph.core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Objects;

/**
 * Provides non-destructive pixel-level image adjustments:
 * - Luminance calculation (ITU-R BT.601)
 * - Grayscale conversion
 * - Brightness adjustment
 * - Contrast adjustment
 * - Color inversion
 *
 * All operations return new {@link BufferedImage} instances without modifying the original input.
 * Hardware-accelerated direct raster arrays are used when available for maximum throughput.
 */
public final class ImageProcessor {

    private ImageProcessor() {
        // Utility class; prevent instantiation
    }

    /**
     * Calculates perceived pixel luminance using the standard ITU-R BT.601 formula:
     * L = 0.299*R + 0.587*G + 0.114*B
     *
     * @param r red channel [0, 255].
     * @param g green channel [0, 255].
     * @param b blue channel [0, 255].
     * @return perceived luminance in range [0.0, 255.0].
     */
    public static double calculateLuminance(int r, int g, int b) {
        return 0.299 * r + 0.587 * g + 0.114 * b;
    }

    /**
     * Extracts R, G, B channels from a packed 32-bit ARGB/RGB integer and calculates luminance.
     *
     * @param rgb packed integer pixel value.
     * @return perceived luminance in range [0.0, 255.0].
     */
    public static double calculateLuminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return calculateLuminance(r, g, b);
    }

    /**
     * Converts an image to grayscale using luminance weights.
     *
     * @param source the input image.
     * @return a new grayscale {@link BufferedImage}.
     */
    public static BufferedImage toGrayscale(BufferedImage source) {
        Objects.requireNonNull(source, "source image must not be null");

        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Fast path: direct raster int[] array
        if (source.getRaster().getDataBuffer() instanceof DataBufferInt srcBuf &&
            result.getRaster().getDataBuffer() instanceof DataBufferInt dstBuf) {
            int[] srcPixels = srcBuf.getData();
            int[] dstPixels = dstBuf.getData();
            for (int i = 0; i < srcPixels.length; i++) {
                int rgb = srcPixels[i];
                int gray = (int) Math.round(calculateLuminance(rgb));
                gray = Math.clamp(gray, 0, 255);
                dstPixels[i] = (gray << 16) | (gray << 8) | gray;
            }
            return result;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int gray = (int) Math.round(calculateLuminance(rgb));
                gray = Math.clamp(gray, 0, 255);

                int grayRgb = (gray << 16) | (gray << 8) | gray;
                result.setRGB(x, y, grayRgb);
            }
        }

        return result;
    }

    /**
     * Adjusts the brightness of an image by adding an integer offset to each color channel.
     *
     * @param source the input image.
     * @param offset brightness adjustment delta (e.g. -50 for darker, +50 for brighter).
     * @return a new adjusted {@link BufferedImage}.
     */
    public static BufferedImage adjustBrightness(BufferedImage source, int offset) {
        Objects.requireNonNull(source, "source image must not be null");

        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Fast path: direct raster int[] array
        if (source.getRaster().getDataBuffer() instanceof DataBufferInt srcBuf &&
            result.getRaster().getDataBuffer() instanceof DataBufferInt dstBuf) {
            int[] srcPixels = srcBuf.getData();
            int[] dstPixels = dstBuf.getData();
            for (int i = 0; i < srcPixels.length; i++) {
                int rgb = srcPixels[i];
                int r = Math.clamp(((rgb >> 16) & 0xFF) + offset, 0, 255);
                int g = Math.clamp(((rgb >> 8) & 0xFF) + offset, 0, 255);
                int b = Math.clamp((rgb & 0xFF) + offset, 0, 255);
                dstPixels[i] = (r << 16) | (g << 8) | b;
            }
            return result;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int r = Math.clamp(((rgb >> 16) & 0xFF) + offset, 0, 255);
                int g = Math.clamp(((rgb >> 8) & 0xFF) + offset, 0, 255);
                int b = Math.clamp((rgb & 0xFF) + offset, 0, 255);

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * Adjusts the contrast of an image using a scale factor relative to midpoint 128.
     *
     * Formula for each channel C:
     * C' = clamp(factor * (C - 128) + 128, 0, 255)
     *
     * @param source the input image.
     * @param factor contrast multiplier (1.0 = unchanged, > 1.0 = higher contrast, < 1.0 = lower contrast).
     * @return a new adjusted {@link BufferedImage}.
     * @throws IllegalArgumentException if factor is negative or NaN.
     */
    public static BufferedImage adjustContrast(BufferedImage source, double factor) {
        Objects.requireNonNull(source, "source image must not be null");
        if (factor < 0.0 || Double.isNaN(factor) || Double.isInfinite(factor)) {
            throw new IllegalArgumentException("Contrast factor must be a non-negative finite number: " + factor);
        }

        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Fast path: direct raster int[] array
        if (source.getRaster().getDataBuffer() instanceof DataBufferInt srcBuf &&
            result.getRaster().getDataBuffer() instanceof DataBufferInt dstBuf) {
            int[] srcPixels = srcBuf.getData();
            int[] dstPixels = dstBuf.getData();
            for (int i = 0; i < srcPixels.length; i++) {
                int rgb = srcPixels[i];
                int r = (int) Math.round(factor * (((rgb >> 16) & 0xFF) - 128.0) + 128.0);
                int g = (int) Math.round(factor * (((rgb >> 8) & 0xFF) - 128.0) + 128.0);
                int b = (int) Math.round(factor * ((rgb & 0xFF) - 128.0) + 128.0);

                r = Math.clamp(r, 0, 255);
                g = Math.clamp(g, 0, 255);
                b = Math.clamp(b, 0, 255);

                dstPixels[i] = (r << 16) | (g << 8) | b;
            }
            return result;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);

                int r = (int) Math.round(factor * (((rgb >> 16) & 0xFF) - 128.0) + 128.0);
                int g = (int) Math.round(factor * (((rgb >> 8) & 0xFF) - 128.0) + 128.0);
                int b = (int) Math.round(factor * ((rgb & 0xFF) - 128.0) + 128.0);

                r = Math.clamp(r, 0, 255);
                g = Math.clamp(g, 0, 255);
                b = Math.clamp(b, 0, 255);

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }

    /**
     * Inverts the colors of an image (creates a photographic negative).
     *
     * Formula:
     * C' = 255 - C
     *
     * @param source the input image.
     * @return a new color-inverted {@link BufferedImage}.
     */
    public static BufferedImage invert(BufferedImage source) {
        Objects.requireNonNull(source, "source image must not be null");

        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Fast path: direct raster int[] array
        if (source.getRaster().getDataBuffer() instanceof DataBufferInt srcBuf &&
            result.getRaster().getDataBuffer() instanceof DataBufferInt dstBuf) {
            int[] srcPixels = srcBuf.getData();
            int[] dstPixels = dstBuf.getData();
            for (int i = 0; i < srcPixels.length; i++) {
                int rgb = srcPixels[i];
                int r = 255 - ((rgb >> 16) & 0xFF);
                int g = 255 - ((rgb >> 8) & 0xFF);
                int b = 255 - (rgb & 0xFF);
                dstPixels[i] = (r << 16) | (g << 8) | b;
            }
            return result;
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);

                int r = 255 - ((rgb >> 16) & 0xFF);
                int g = 255 - ((rgb >> 8) & 0xFF);
                int b = 255 - (rgb & 0xFF);

                result.setRGB(x, y, (r << 16) | (g << 8) | b);
            }
        }

        return result;
    }
}
