package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AnsiRenderer Unit Tests")
class AnsiRendererTest {

    @Test
    @DisplayName("Falls back to plain text when result has no color")
    void fallbackToPlainText() {
        char[][] grid = {{'@', '#'}};
        AsciiResult monochrome = new AsciiResult(grid);

        String rendered = AnsiRenderer.render(monochrome);

        assertEquals("@#", rendered);
        assertFalse(rendered.contains("\u001B["));
    }

    @Test
    @DisplayName("Emits 24-bit ANSI foreground color sequence")
    void emitsAnsiColorCodes() {
        char[][] grid = {{'@'}};
        int[][] rgb = {{0x00FF0000}}; // Pure Red (255, 0, 0)
        AsciiResult colorResult = new AsciiResult(grid, rgb);

        String rendered = AnsiRenderer.render(colorResult);

        // Expected format: \u001B[38;2;255;0;0m@\u001B[0m
        assertTrue(rendered.contains("\u001B[38;2;255;0;0m"));
        assertTrue(rendered.contains("@"));
        assertTrue(rendered.endsWith(AnsiRenderer.ANSI_RESET));
    }

    @Test
    @DisplayName("Deduplicates identical consecutive color escape codes")
    void deduplicatesConsecutiveColors() {
        char[][] grid = {{'@', '@', '@'}};
        int[][] rgb = {{0x0000FF00, 0x0000FF00, 0x0000FF00}}; // 3 identical green pixels
        AsciiResult colorResult = new AsciiResult(grid, rgb);

        String rendered = AnsiRenderer.render(colorResult);

        // Count occurrences of the green escape code
        String greenCode = "\u001B[38;2;0;255;0m";
        int firstIndex = rendered.indexOf(greenCode);
        int lastIndex = rendered.lastIndexOf(greenCode);

        // The escape sequence should only appear ONCE for all 3 characters
        assertTrue(firstIndex != -1, "Should contain the color escape code");
        assertEquals(firstIndex, lastIndex, "Should deduplicate consecutive identical color codes");
    }

    @Test
    @DisplayName("renderBlocks emits 24-bit ANSI background sequences")
    void renderBlocksBackground() {
        char[][] grid = {{'#'}};
        int[][] rgb = {{0x000000FF}}; // Pure Blue (0, 0, 255)
        AsciiResult colorResult = new AsciiResult(grid, rgb);

        String rendered = AnsiRenderer.renderBlocks(colorResult);

        assertTrue(rendered.contains("\u001B[48;2;0;0;255m"));
        assertTrue(rendered.contains(" "));
        assertTrue(rendered.endsWith(AnsiRenderer.ANSI_RESET));
    }

    @Test
    @DisplayName("Throws NullPointerException for null result")
    void nullResultThrows() {
        assertThrows(NullPointerException.class, () -> AnsiRenderer.render(null));
    }
}
