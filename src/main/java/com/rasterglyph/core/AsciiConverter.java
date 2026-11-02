package com.rasterglyph.core;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Coordinates the image-to-ASCII conversion pipeline.
 *
 * The pipeline:
 * 1. Takes an input {@link BufferedImage}.
 * 2. Scales it to the specified target width, automatically computing height
 *    using the character aspect ratio via {@link ImageResizer}.
 * 3. Iterates over every pixel, calculating luminance L = 0.299R + 0.587G + 0.114B
 *    via {@link ImageProcessor#calculateLuminance(int)}.
 * 4. Maps luminance to an ASCII character via {@link CharacterMapper}.
 * 5. Bundles the character grid (and optional RGB colors) into an {@link AsciiResult}.
 *
 * Instances of this class are immutable and thread-safe.
 */
public final class AsciiConverter {

    private final CharacterMapper mapper;
    private final double charAspectRatio;
    private final boolean includeColor;
    private final boolean dither;

    /**
     * Creates a default converter using standard character mapping and default 0.5 aspect ratio.
     */
    public AsciiConverter() {
        this(CharacterMapper.standard(), ImageResizer.DEFAULT_CHAR_ASPECT_RATIO, false, false);
    }

    /**
     * Creates a converter with a custom {@link CharacterMapper}.
     *
     * @param mapper the character mapper to use.
     */
    public AsciiConverter(CharacterMapper mapper) {
        this(mapper, ImageResizer.DEFAULT_CHAR_ASPECT_RATIO, false, false);
    }

    /**
     * Creates a customized converter with color support.
     */
    public AsciiConverter(CharacterMapper mapper, double charAspectRatio, boolean includeColor) {
        this(mapper, charAspectRatio, includeColor, false);
    }

    /**
     * Creates a fully customized converter with optional Floyd–Steinberg dithering.
     *
     * @param mapper character mapper.
     * @param charAspectRatio character width-to-height ratio (typically ~0.5).
     * @param includeColor true to capture RGB colors for true-color output.
     * @param dither true to apply Floyd–Steinberg error diffusion to eliminate tonal banding.
     * @throws NullPointerException if mapper is null.
     * @throws IllegalArgumentException if charAspectRatio is non-positive or NaN.
     */
    public AsciiConverter(CharacterMapper mapper, double charAspectRatio, boolean includeColor, boolean dither) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        if (charAspectRatio <= 0 || Double.isNaN(charAspectRatio) || Double.isInfinite(charAspectRatio)) {
            throw new IllegalArgumentException(
                    "Character aspect ratio must be a positive finite number: " + charAspectRatio
            );
        }
        this.charAspectRatio = charAspectRatio;
        this.includeColor = includeColor;
        this.dither = dither;
    }

    /**
     * Convenience static method to convert an image using standard settings.
     *
     * @param image source image.
     * @param targetWidth desired ASCII width in characters.
     * @return generated {@link AsciiResult}.
     */
    public static AsciiResult toAscii(BufferedImage image, int targetWidth) {
        return new AsciiConverter().convert(image, targetWidth);
    }

    /**
     * Converts an image to ASCII art at the specified target width.
     *
     * @param image input image.
     * @param targetWidth desired ASCII width in characters.
     * @return generated {@link AsciiResult}.
     * @throws NullPointerException if image is null.
     * @throws IllegalArgumentException if targetWidth is non-positive.
     */
    public AsciiResult convert(BufferedImage image, int targetWidth) {
        Objects.requireNonNull(image, "image must not be null");
        if (targetWidth <= 0) {
            throw new IllegalArgumentException("targetWidth must be positive: " + targetWidth);
        }

        // 1. Resize image with aspect ratio correction
        BufferedImage scaled = ImageResizer.resize(image, targetWidth, charAspectRatio);

        // 2. Apply Floyd–Steinberg dithering if enabled
        if (dither) {
            scaled = DitherFilter.ditherGrayscale(scaled, mapper.getCharacterSet().length());
        }

        // 3. Perform pixel-to-character conversion
        return convertDirect(scaled);
    }

    /**
     * Directly converts an already pre-scaled image to ASCII art without further resizing.
     *
     * @param scaledImage pre-scaled image whose pixel dimensions match desired character dimensions.
     * @return generated {@link AsciiResult}.
     * @throws NullPointerException if scaledImage is null.
     */
    public AsciiResult convertDirect(BufferedImage scaledImage) {
        Objects.requireNonNull(scaledImage, "scaledImage must not be null");

        int width = scaledImage.getWidth();
        int height = scaledImage.getHeight();

        char[][] grid = new char[height][width];
        int[][] rgbGrid = includeColor ? new int[height][width] : null;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = scaledImage.getRGB(x, y);

                // Calculate photometric luminance L = 0.299R + 0.587G + 0.114B
                double luminance = ImageProcessor.calculateLuminance(rgb);

                // Map luminance to character
                grid[y][x] = mapper.map(luminance);

                if (includeColor) {
                    rgbGrid[y][x] = rgb;
                }
            }
        }

        return new AsciiResult(grid, rgbGrid);
    }

    public CharacterMapper getMapper() {
        return mapper;
    }

    public double getCharAspectRatio() {
        return charAspectRatio;
    }

    public boolean isIncludeColor() {
        return includeColor;
    }

    public boolean isDither() {
        return dither;
    }
}
