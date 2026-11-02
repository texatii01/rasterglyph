package com.rasterglyph.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Exports an {@link AsciiResult} into a standalone, styled HTML document.
 *
 * Features:
 * - Proper HTML entity escaping (&amp;, &lt;, &gt;, &quot;)
 * - True-color span generation with span-deduplication for fast rendering
 * - Dark retro themes (Phosphor Green, Amber, Cyberpunk)
 * - Optional subtle CRT scanline effect
 */
public final class HtmlExporter {

    private HtmlExporter() {
        // Utility class; prevent instantiation
    }

    /**
     * Configuration options for HTML export.
     */
    public record HtmlOptions(
            String title,
            String backgroundColor,
            String defaultTextColor,
            int fontSizePx,
            boolean crtScanlines
    ) {
        public static final HtmlOptions DEFAULT = new HtmlOptions(
                "RASTER//GLYPH Art",
                "#0d0d12",
                "#e0e0e0",
                10,
                false
        );

        public static final HtmlOptions RETRO_GREEN = new HtmlOptions(
                "RASTER//GLYPH — Phosphor Green",
                "#050805",
                "#33ff33",
                10,
                true
        );

        public static final HtmlOptions RETRO_AMBER = new HtmlOptions(
                "RASTER//GLYPH — Amber CRT",
                "#080500",
                "#ffb000",
                10,
                true
        );
    }

    /**
     * Converts an AsciiResult into a standalone HTML document using default options.
     *
     * @param result the ASCII conversion result.
     * @return HTML source string.
     */
    public static String toHtml(AsciiResult result) {
        return toHtml(result, HtmlOptions.DEFAULT);
    }

    /**
     * Converts an AsciiResult into a standalone HTML document with custom options.
     *
     * @param result the ASCII conversion result.
     * @param options styling and layout options.
     * @return complete HTML source string.
     */
    public static String toHtml(AsciiResult result, HtmlOptions options) {
        Objects.requireNonNull(result, "result must not be null");
        Objects.requireNonNull(options, "options must not be null");

        int width = result.getWidth();
        int height = result.getHeight();
        boolean hasColor = result.hasColor();

        StringBuilder sb = new StringBuilder(height * (width * 30 + 10));

        // HTML Head & Styling
        sb.append("<!DOCTYPE html>\n")
          .append("<html lang=\"en\">\n")
          .append("<head>\n")
          .append("  <meta charset=\"UTF-8\">\n")
          .append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
          .append("  <title>").append(escapeHtmlText(options.title())).append("</title>\n")
          .append("  <style>\n")
          .append("    body {\n")
          .append("      margin: 0;\n")
          .append("      padding: 24px;\n")
          .append("      background-color: ").append(options.backgroundColor()).append(";\n")
          .append("      display: flex;\n")
          .append("      justify-content: center;\n")
          .append("      align-items: center;\n")
          .append("      min-height: 100vh;\n")
          .append("      box-sizing: border-box;\n")
          .append("    }\n")
          .append("    pre {\n")
          .append("      font-family: 'Courier New', Courier, Consolas, monospace;\n")
          .append("      font-size: ").append(options.fontSizePx()).append("px;\n")
          .append("      line-height: ").append(options.fontSizePx()).append("px;\n")
          .append("      letter-spacing: 0;\n")
          .append("      white-space: pre;\n")
          .append("      color: ").append(options.defaultTextColor()).append(";\n")
          .append("      margin: 0;\n")
          .append("      user-select: all;\n");

        if (options.crtScanlines()) {
            sb.append("      background: linear-gradient(rgba(18, 16, 16, 0) 50%, rgba(0, 0, 0, 0.3) 50%);\n")
              .append("      background-size: 100% 3px;\n");
        }

        sb.append("    }\n")
          .append("  </style>\n")
          .append("</head>\n")
          .append("<body>\n")
          .append("  <pre><code>");

        // Generate ASCII grid
        for (int y = 0; y < height; y++) {
            int currentRgb = -1;
            boolean spanOpen = false;

            for (int x = 0; x < width; x++) {
                char ch = result.getCharacter(x, y);

                if (hasColor) {
                    int rgb = result.getRgb(x, y) & 0x00FFFFFF;

                    if (rgb != currentRgb) {
                        if (spanOpen) {
                            sb.append("</span>");
                        }
                        sb.append("<span style=\"color:#").append(toHexColor(rgb)).append("\">");
                        spanOpen = true;
                        currentRgb = rgb;
                    }
                }

                appendEscapedCharacter(sb, ch);
            }

            if (spanOpen) {
                sb.append("</span>");
            }

            if (y < height - 1) {
                sb.append('\n');
            }
        }

        sb.append("</code></pre>\n")
          .append("</body>\n")
          .append("</html>\n");

        return sb.toString();
    }

    /**
     * Exports an AsciiResult directly to an HTML file on disk.
     *
     * @param result the ASCII conversion result.
     * @param outputPath destination file path.
     * @throws IOException if writing to the file fails.
     */
    public static void exportToFile(AsciiResult result, Path outputPath) throws IOException {
        exportToFile(result, outputPath, HtmlOptions.DEFAULT);
    }

    /**
     * Exports an AsciiResult directly to an HTML file on disk with custom options.
     *
     * @param result the ASCII conversion result.
     * @param outputPath destination file path.
     * @param options styling options.
     * @throws IOException if writing to the file fails.
     */
    public static void exportToFile(AsciiResult result, Path outputPath, HtmlOptions options) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath must not be null");
        String html = toHtml(result, options);
        Files.writeString(outputPath, html);
    }

    private static void appendEscapedCharacter(StringBuilder sb, char ch) {
        switch (ch) {
            case '<' -> sb.append("&lt;");
            case '>' -> sb.append("&gt;");
            case '&' -> sb.append("&amp;");
            case '"' -> sb.append("&quot;");
            default -> sb.append(ch);
        }
    }

    private static String escapeHtmlText(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }

    private static String toHexColor(int rgb) {
        return String.format("%06X", rgb);
    }
}
