package com.rasterglyph.core;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Handles image scaling and aspect ratio correction for ASCII art rendering.
 *
 * In standard monospace fonts, terminal characters are not square; they are typically
 * twice as tall as they are wide (~1:2 width-to-height ratio, or a character aspect
 * ratio of ~0.5). Without vertical compensation, images appear severely stretched.
 *
 * All operations return new {@link BufferedImage} instances and do not mutate the input image.
 */
public final class ImageResizer {

    /**
     * Default character width-to-height ratio for standard monospace fonts (0.5).
     */
    public static final double DEFAULT_CHAR_ASPECT_RATIO = 0.5;

    private ImageResizer() {
        // Utility class; prevent instantiation
    }

    /**
     * Calculates the target output height in characters given an image's dimensions,
     * desired target width, and font character aspect ratio.
     *
     * Formula:
     * targetHeight = round((origHeight / origWidth) * targetWidth * charAspectRatio)
     *
     * @param origWidth original image width in pixels.
     * @param origHeight original image height in pixels.
     * @param targetWidth target ASCII width in characters.
     * @param charAspectRatio font character width-to-height ratio (typically ~0.5).
     * @return calculated target height in characters (minimum 1).
     * @throws IllegalArgumentException if dimensions or ratio are non-positive.
     */
    public static int calculateHeight(int origWidth, int origHeight, int targetWidth, double charAspectRatio) {
        if (origWidth <= 0 || origHeight <= 0) {
            throw new IllegalArgumentException("Original image dimensions must be positive");
        }
        if (targetWidth <= 0) {
            throw new IllegalArgumentException("Target width must be positive: " + targetWidth);
        }
        if (charAspectRatio <= 0 || Double.isNaN(charAspectRatio) || Double.isInfinite(charAspectRatio)) {
            throw new IllegalArgumentException("Character aspect ratio must be a positive finite number: " + charAspectRatio);
        }

        double imageAspectRatio = (double) origHeight / (double) origWidth;
        int calculatedHeight = (int) Math.round(imageAspectRatio * targetWidth * charAspectRatio);

        // Guarantee at least 1 character row
        return Math.max(1, calculatedHeight);
    }

    /**
     * Calculates the target output height using the default character aspect ratio (0.5).
     *
     * @param image input image.
     * @param targetWidth target ASCII width in characters.
     * @return calculated target height in characters.
     */
    public static int calculateHeight(BufferedImage image, int targetWidth) {
        Objects.requireNonNull(image, "image must not be null");
        return calculateHeight(image.getWidth(), image.getHeight(), targetWidth, DEFAULT_CHAR_ASPECT_RATIO);
    }

    /**
     * Resizes an image to the specified width, automatically calculating proportional
     * height using the default character aspect ratio (0.5).
     *
     * @param original the source image.
     * @param targetWidth target output width in characters.
     * @return a new scaled {@link BufferedImage}.
     */
    public static BufferedImage resize(BufferedImage original, int targetWidth) {
        return resize(original, targetWidth, DEFAULT_CHAR_ASPECT_RATIO);
    }

    /**
     * Resizes an image to the specified width, automatically calculating proportional
     * height using a custom character aspect ratio.
     *
     * @param original the source image.
     * @param targetWidth target output width in characters.
     * @param charAspectRatio character width-to-height ratio.
     * @return a new scaled {@link BufferedImage}.
     */
    public static BufferedImage resize(BufferedImage original, int targetWidth, double charAspectRatio) {
        Objects.requireNonNull(original, "original image must not be null");
        int targetHeight = calculateHeight(original.getWidth(), original.getHeight(), targetWidth, charAspectRatio);
        return resizeExact(original, targetWidth, targetHeight);
    }

    /**
     * Scales an image to exact target pixel dimensions with high-quality bilinear interpolation.
     * Fills the background with white in case the source image contains transparent areas.
     *
     * @param original source image.
     * @param targetWidth exact destination width.
     * @param targetHeight exact destination height.
     * @return a new scaled {@link BufferedImage}.
     */
    public static BufferedImage resizeExact(BufferedImage original, int targetWidth, int targetHeight) {
        Objects.requireNonNull(original, "original image must not be null");
        if (targetWidth <= 0 || targetHeight <= 0) {
            throw new IllegalArgumentException("Target dimensions must be positive: " + targetWidth + "x" + targetHeight);
        }

        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();

        try {
            // Paint white background to gracefully handle transparent PNG regions
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, targetWidth, targetHeight);

            // Configure rendering hints for clean, anti-aliased downsampling
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            g2d.dispose();
        }

        return scaled;
    }
}
