package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AsciiResult Unit Tests")
class AsciiResultTest {

    @Test
    @DisplayName("Initializes dimensions and character retrieval correctly")
    void initializationAndAccess() {
        char[][] grid = {
                {'@', '#', '.'},
                {'%', '*', ' '}
        };

        AsciiResult result = new AsciiResult(grid);

        assertEquals(3, result.getWidth());
        assertEquals(2, result.getHeight());
        assertEquals('@', result.getCharacter(0, 0));
        assertEquals('#', result.getCharacter(1, 0));
        assertEquals('.', result.getCharacter(2, 0));
        assertEquals('%', result.getCharacter(0, 1));
        assertEquals('*', result.getCharacter(1, 1));
        assertEquals(' ', result.getCharacter(2, 1));
        assertFalse(result.hasColor());
    }

    @Test
    @DisplayName("toPlainText formats lines with newline separators correctly")
    void stringFormatting() {
        char[][] grid = {
                {'#', '#'},
                {'.', '.'}
        };

        AsciiResult result = new AsciiResult(grid);
        String expected = "##\n..";

        assertEquals(expected, result.toPlainText());
        assertEquals(expected, result.toString());
    }

    @Test
    @DisplayName("Guarantees immutability via defensive copy")
    void immutabilityDefensiveCopy() {
        char[][] grid = {
                {'A', 'B'},
                {'C', 'D'}
        };

        AsciiResult result = new AsciiResult(grid);

        // Mutate original input array
        grid[0][0] = 'Z';
        assertEquals('A', result.getCharacter(0, 0), "Result should not reflect external modifications");

        // Mutate copy array from getGridCopy
        char[][] copy = result.getGridCopy();
        copy[0][0] = 'X';
        assertEquals('A', result.getCharacter(0, 0), "Result should not reflect mutations to getGridCopy");
    }

    @Test
    @DisplayName("Rejects null, empty or jagged grids")
    void validationRules() {
        assertThrows(NullPointerException.class, () -> new AsciiResult(null));
        assertThrows(IllegalArgumentException.class, () -> new AsciiResult(new char[0][0]));

        char[][] jaggedGrid = {
                {'A', 'B'},
                {'C'} // Row length 1 != 2
        };
        assertThrows(IllegalArgumentException.class, () -> new AsciiResult(jaggedGrid));
    }

    @Test
    @DisplayName("Throws IndexOutOfBoundsException for invalid coordinates")
    void coordinateBoundsCheck() {
        char[][] grid = {{'#'}};
        AsciiResult result = new AsciiResult(grid);

        assertThrows(IndexOutOfBoundsException.class, () -> result.getCharacter(-1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> result.getCharacter(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> result.getCharacter(0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> result.getCharacter(0, 1));
    }

    @Test
    @DisplayName("Supports optional RGB colors")
    void optionalRgbGrid() {
        char[][] grid = {{'@', '.'}};
        int[][] rgb = {{0xFFFF0000, 0xFF00FF00}}; // Red, Green

        AsciiResult result = new AsciiResult(grid, rgb);

        assertTrue(result.hasColor());
        assertEquals(0xFFFF0000, result.getRgb(0, 0));
        assertEquals(0xFF00FF00, result.getRgb(1, 0));
    }
}
