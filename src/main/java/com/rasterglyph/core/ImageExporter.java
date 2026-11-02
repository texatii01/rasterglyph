package com.rasterglyph.core;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Exports an {@link AsciiResult} into a high-resolution raster image (PNG).
 *
 * Renders each character in a monospace font with subpixel antialiasing,
 * supporting both monochrome styling and 24-bit True-Color rendering with
 * optional CRT scanline effects.
 */
public final class ImageExporter {

    private ImageExporter() {
        // Utility class; prevent instantiation
    }

    /**
     * Configuration options for image rendering and export.
     */
    public record ImageExportOptions(
            int fontSize,
            Color backgroundColor,
            Color defaultTextColor,
            int padding,
            boolean crtScanlines
    ) {
        public static final ImageExportOptions DEFAULT = new ImageExportOptions(
                14,
                new Color(14, 14, 20),       // Modern dark background
                new Color(225, 225, 230),    // Off-white text
                24,                          // 24px padding margin
                false
        );

        public static final ImageExportOptions RETRO_GREEN = new ImageExportOptions(
                14,
                new Color(5, 8, 5),          // Deep phosphor black
                new Color(51, 255, 51),      // Classic phosphor green
                24,
                true                         // CRT scanlines enabled
        );

        public static final ImageExportOptions RETRO_AMBER = new ImageExportOptions(
                14,
                new Color(8, 5, 0),          // Warm amber black
                new Color(255, 176, 0),      // Amber CRT glow
                24,
                true
        );
    }

    /**
     * Renders an AsciiResult into a {@link BufferedImage} using default options.
     *
     * @param result the ASCII conversion result.
     * @return rendered {@link BufferedImage}.
     */
    public static BufferedImage renderToImage(AsciiResult result) {
        return renderToImage(result, ImageExportOptions.DEFAULT);
    }

    /**
     * Renders an AsciiResult into a {@link BufferedImage} with custom styling options.
     *
     * @param result the ASCII conversion result.
     * @param options rendering configuration.
     * @return rendered {@link BufferedImage}.
     */
    public static BufferedImage renderToImage(AsciiResult result, ImageExportOptions options) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(options, "options must not be null");

        Font font = new Font(Font.MONOSPACED, Font.PLAIN, options.fontSize());

        // 1. Measure monospace font metrics accurately
        BufferedImage dummy = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D gDummy = dummy.createGraphics();
        gDummy.setFont(font);
        FontMetrics fm = gDummy.getFontMetrics();
        int charWidth = fm.charWidth('W');
        int charHeight = fm.getHeight();
        int ascent = fm.getAscent();
        gDummy.dispose();

        // 2. Allocate canvas with margin padding
        int imgWidth = (result.getWidth() * charWidth) + (options.padding() * 2);
        int imgHeight = (result.getHeight() * charHeight) + (options.padding() * 2);

        BufferedImage output = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();

        try {
            // High-quality text antialiasing
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setFont(font);

            // Paint background
            g.setColor(options.backgroundColor());
            g.fillRect(0, 0, imgWidth, imgHeight);

            int width = result.getWidth();
            int height = result.getHeight();
            boolean hasColor = result.hasColor();

            // 3. Render character grid
            for (int y = 0; y < height; y++) {
                int baselineY = options.padding() + (y * charHeight) + ascent;

                for (int x = 0; x < width; x++) {
                    char ch = result.getCharacter(x, y);
                    if (ch == ' ') {
                        continue;
                    }

                    int posX = options.padding() + (x * charWidth);

                    if (hasColor) {
                        int rgb = result.getRgb(x, y);
                        g.setColor(new Color(rgb));
                    } else {
                        g.setColor(options.defaultTextColor());
                    }

                    g.drawString(String.valueOf(ch), posX, baselineY);
                }
            }

            // 4. Draw optional CRT horizontal scanlines overlay
            if (options.crtScanlines()) {
                g.setColor(new Color(0, 0, 0, 45)); // Subtle 18% opacity dark scanline
                for (int sy = 0; sy < imgHeight; sy += 3) {
                    g.drawLine(0, sy, imgWidth, sy);
                }
            }
        } finally {
            g.dispose();
        }

        return output;
    }

    /**
     * Exports an AsciiResult directly to a PNG image file on disk.
     *
     * @param result the ASCII conversion result.
     * @param outputPath destination file path.
     * @throws IOException if writing to disk fails.
     */
    public static void exportToPng(AsciiResult result, Path outputPath) throws IOException {
        exportToPng(result, outputPath, ImageExportOptions.DEFAULT);
    }

    /**
     * Exports an AsciiResult directly to a PNG image file on disk with custom options.
     *
     * @param result the ASCII conversion result.
     * @param outputPath destination file path.
     * @param options rendering configuration.
     * @throws IOException if writing to disk fails.
     */
    public static void exportToPng(AsciiResult result, Path outputPath, ImageExportOptions options) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        exportToPng(result, outputPath.toFile(), options);
    }

    /**
     * Exports an AsciiResult directly to a PNG {@link File} on disk with custom options.
     *
     * @param result the ASCII conversion result.
     * @param outputFile destination file.
     * @param options rendering configuration.
     * @throws IOException if writing to disk fails.
     */
    public static void exportToPng(AsciiResult result, File outputFile, ImageExportOptions options) throws IOException {
        Objects.requireNonNull(outputFile, "outputFile must not be null");
        BufferedImage image = renderToImage(result, options);
        boolean written = ImageIO.write(image, "png", outputFile);
        if (!written) {
            throw new IOException("Failed to write PNG image format to: " + outputFile.getAbsolutePath());
        }
    }
}
