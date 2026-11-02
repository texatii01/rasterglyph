package com.rasterglyph.core;

import java.util.Objects;

/**
 * Renders an {@link AsciiResult} into an ANSI escape-coded String for display
 * in modern 24-bit true-color terminals.
 *
 * SGR Escape Sequences used:
 * - Foreground True-Color: {@code \u001B[38;2;R;G;Bm}
 * - Background True-Color: {@code \u001B[48;2;R;G;Bm}
 * - Reset All Attributes:  {@code \u001B[0m}
 *
 * Implements escape code deduplication: only emits an escape sequence when
 * the color changes between adjacent characters, significantly reducing terminal
 * payload size.
 */
public final class AnsiRenderer {

    public static final String ANSI_RESET = "\u001B[0m";

    private AnsiRenderer() {
        // Utility class; prevent instantiation
    }

    /**
     * Renders the AsciiResult using 24-bit RGB foreground colors.
     * If the result contains no color data, returns plain text.
     *
     * @param result the ASCII conversion result.
     * @return ANSI color-coded string.
     * @throws NullPointerException if result is null.
     */
    public static String render(AsciiResult result) {
        Objects.requireNonNull(result, "result must not be null");

        if (!result.hasColor()) {
            return result.toPlainText();
        }

        int width = result.getWidth();
        int height = result.getHeight();

        // Generous pre-allocation for string buffer with ANSI codes
        StringBuilder sb = new StringBuilder(height * (width * 20 + 8));

        for (int y = 0; y < height; y++) {
            int lastRgb = -1; // Track previous color in row to avoid redundant escape codes

            for (int x = 0; x < width; x++) {
                int rgb = result.getRgb(x, y) & 0x00FFFFFF; // Mask off alpha
                char ch = result.getCharacter(x, y);

                // Don't colorize pure whitespace spaces if unnecessary
                if (ch == ' ') {
                    sb.append(' ');
                    continue;
                }

                if (rgb != lastRgb) {
                    appendForegroundRgb(sb, rgb);
                    lastRgb = rgb;
                }

                sb.append(ch);
            }

            // Reset color at end of each row
            if (lastRgb != -1) {
                sb.append(ANSI_RESET);
            }

            if (y < height - 1) {
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    /**
     * Renders the AsciiResult as solid colored terminal blocks (using background color and spaces).
     *
     * @param result the ASCII conversion result.
     * @return ANSI block-rendered string.
     */
    public static String renderBlocks(AsciiResult result) {
        Objects.requireNonNull(result, "result must not be null");

        if (!result.hasColor()) {
            return result.toPlainText();
        }

        int width = result.getWidth();
        int height = result.getHeight();
        StringBuilder sb = new StringBuilder(height * (width * 20 + 8));

        for (int y = 0; y < height; y++) {
            int lastRgb = -1;

            for (int x = 0; x < width; x++) {
                int rgb = result.getRgb(x, y) & 0x00FFFFFF;

                if (rgb != lastRgb) {
                    appendBackgroundRgb(sb, rgb);
                    lastRgb = rgb;
                }

                sb.append(" ");
            }

            if (lastRgb != -1) {
                sb.append(ANSI_RESET);
            }

            if (y < height - 1) {
                sb.append('\n');
            }
        }

        return sb.toString();
    }

    private static void appendForegroundRgb(StringBuilder sb, int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        sb.append("\u001B[38;2;").append(r).append(';').append(g).append(';').append(b).append('m');
    }

    private static void appendBackgroundRgb(StringBuilder sb, int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        sb.append("\u001B[48;2;").append(r).append(';').append(g).append(';').append(b).append('m');
    }
}
