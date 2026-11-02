package com.rasterglyph.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Exports an {@link AsciiResult} into a resolution-independent Scalable Vector Graphics (SVG) document.
 *
 * Benefits of SVG export:
 * - Infinitely scalable: renders razor-sharp at any zoom level or resolution without pixelation.
 * - Human-readable XML text file.
 * - Retains actual selectable and copyable monospace text inside the graphic.
 * - Compatible with web browsers, vector editors (Illustrator, Inkscape, Figma), and plotters.
 */
public final class SvgExporter {

    private SvgExporter() {
        // Utility class; prevent instantiation
    }

    /**
     * Configuration options for SVG generation.
     */
    public record SvgOptions(
            int fontSize,
            String backgroundColor,
            String defaultTextColor,
            int padding
    ) {
        public static final SvgOptions DEFAULT = new SvgOptions(
                14,
                "#0d0d12",     // Dark slate background
                "#e0e0e0",     // Off-white text
                24             // 24px padding margin
        );

        public static final SvgOptions RETRO_GREEN = new SvgOptions(
                14,
                "#050805",     // Deep black
                "#33ff33",     // Phosphor green
                24
        );

        public static final SvgOptions RETRO_AMBER = new SvgOptions(
                14,
                "#080500",     // Warm amber-tinted black
                "#ffb000",     // Amber CRT glow
                24
        );

        public static final SvgOptions TRANSPARENT = new SvgOptions(
                14,
                "none",        // Transparent background
                "#ffffff",
                24
        );
    }

    /**
     * Converts an AsciiResult to an SVG XML string using default options.
     *
     * @param result the ASCII conversion result.
     * @return SVG XML document string.
     */
    public static String toSvg(AsciiResult result) {
        return toSvg(result, SvgOptions.DEFAULT);
    }

    /**
     * Converts an AsciiResult to an SVG XML string with custom options.
     *
     * @param result the ASCII conversion result.
     * @param options styling options.
     * @return complete SVG XML document string.
     */
    public static String toSvg(AsciiResult result, SvgOptions options) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(options, "options must not be null");

        int width = result.getWidth();
        int height = result.getHeight();
        boolean hasColor = result.hasColor();

        // Standard monospace character dimensions in SVG pixels
        double charWidth = options.fontSize() * 0.6;
        double lineHeight = options.fontSize() * 1.15;

        int totalWidth = (int) Math.ceil((width * charWidth) + (options.padding() * 2));
        int totalHeight = (int) Math.ceil((height * lineHeight) + (options.padding() * 2));

        StringBuilder sb = new StringBuilder(height * (width * 30 + 30));

        // SVG Root & ViewBox
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
          .append("<svg xmlns=\"http://www.w3.org/2000/svg\" ")
          .append("viewBox=\"0 0 ").append(totalWidth).append(" ").append(totalHeight).append("\" ")
          .append("width=\"").append(totalWidth).append("\" ")
          .append("height=\"").append(totalHeight).append("\">\n");

        // Background rectangle
        if (!"none".equalsIgnoreCase(options.backgroundColor())) {
            sb.append("  <rect width=\"100%\" height=\"100%\" fill=\"")
              .append(options.backgroundColor()).append("\" />\n");
        }

        // Monospace Text container with xml:space="preserve" to retain whitespace
        sb.append("  <text font-family=\"'Courier New', Courier, Consolas, monospace\" ")
          .append("font-size=\"").append(options.fontSize()).append("px\" ")
          .append("xml:space=\"preserve\">\n");

        for (int y = 0; y < height; y++) {
            double baselineY = options.padding() + (y * lineHeight) + options.fontSize();

            if (!hasColor) {
                // Monochrome optimization: single tspan per row
                sb.append("    <tspan x=\"").append(options.padding())
                  .append("\" y=\"").append(String.format("%.2f", baselineY))
                  .append("\" fill=\"").append(options.defaultTextColor()).append("\">");

                for (int x = 0; x < width; x++) {
                    appendEscapedXml(sb, result.getCharacter(x, y));
                }
                sb.append("</tspan>\n");
            } else {
                // True-color: group adjacent characters sharing the same color
                int currentRgb = -1;
                boolean spanOpen = false;

                for (int x = 0; x < width; x++) {
                    char ch = result.getCharacter(x, y);
                    int rgb = result.getRgb(x, y) & 0x00FFFFFF;
                    double posX = options.padding() + (x * charWidth);

                    if (rgb != currentRgb) {
                        if (spanOpen) {
                            sb.append("</tspan>");
                        }
                        sb.append("    <tspan x=\"").append(String.format("%.2f", posX))
                          .append("\" y=\"").append(String.format("%.2f", baselineY))
                          .append("\" fill=\"#").append(String.format("%06X", rgb)).append("\">");
                        spanOpen = true;
                        currentRgb = rgb;
                    }

                    appendEscapedXml(sb, ch);
                }

                if (spanOpen) {
                    sb.append("</tspan>\n");
                }
            }
        }

        sb.append("  </text>\n")
          .append("</svg>\n");

        return sb.toString();
    }

    /**
     * Exports an AsciiResult directly to an SVG file on disk.
     *
     * @param result the ASCII conversion result.
     * @param outputPath destination file path.
     * @throws IOException if writing to disk fails.
     */
    public static void exportToFile(AsciiResult result, Path outputPath) throws IOException {
        exportToFile(result, outputPath, SvgOptions.DEFAULT);
    }

    /**
     * Exports an AsciiResult directly to an SVG file on disk with custom options.
     *
     * @param result the ASCII conversion result.
     * @param outputPath destination file path.
     * @param options styling options.
     * @throws IOException if writing to disk fails.
     */
    public static void exportToFile(AsciiResult result, Path outputPath, SvgOptions options) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        String svg = toSvg(result, options);
        Files.writeString(outputPath, svg);
    }

    private static void appendEscapedXml(StringBuilder sb, char ch) {
        switch (ch) {
            case '<' -> sb.append("&lt;");
            case '>' -> sb.append("&gt;");
            case '&' -> sb.append("&amp;");
            case '"' -> sb.append("&quot;");
            case '\'' -> sb.append("&apos;");
            default -> sb.append(ch);
        }
    }
}
