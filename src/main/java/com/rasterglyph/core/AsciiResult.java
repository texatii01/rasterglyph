package com.rasterglyph.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Stores the output of an ASCII conversion process.
 *
 * Encapsulates a 2-dimensional grid of characters of dimensions (width x height).
 * Array indexing internally follows row-major order: grid[y][x], where:
 * - y is the row index [0, height - 1]
 * - x is the column index [0, width - 1]
 *
 * Also provides an optional RGB color matrix for seamless future expansion
 * into true-color (ANSI / HTML) rendering.
 *
 * Instances of this class are immutable.
 */
public final class AsciiResult {

    private final int width;
    private final int height;
    private final char[][] grid;
    private final int[][] rgbGrid; // Stores ARGB / RGB packed integers, or null if monochrome

    /**
     * Creates a monochrome AsciiResult.
     *
     * @param grid 2D character array of shape [height][width].
     * @throws NullPointerException if grid is null.
     * @throws IllegalArgumentException if grid is empty or jagged (inconsistent row lengths).
     */
    public AsciiResult(char[][] grid) {
        this(grid, null);
    }

    /**
     * Creates an AsciiResult with optional color data.
     *
     * @param grid 2D character array of shape [height][width].
     * @param rgbGrid optional 2D array of packed ARGB integers of shape [height][width], or null.
     * @throws NullPointerException if grid is null.
     * @throws IllegalArgumentException if grid is empty or jagged.
     */
    public AsciiResult(char[][] grid, int[][] rgbGrid) {
        Objects.requireNonNull(grid, "grid must not be null");
        if (grid.length == 0 || grid[0].length == 0) {
            throw new IllegalArgumentException("Grid must have at least 1 row and 1 column");
        }

        this.height = grid.length;
        this.width = grid[0].length;

        // Perform defensive copy of the character grid to ensure immutability
        this.grid = new char[height][width];
        for (int y = 0; y < height; y++) {
            if (grid[y] == null || grid[y].length != width) {
                throw new IllegalArgumentException(
                        "Jagged grid: row " + y + " length does not match width " + width
                );
            }
            System.arraycopy(grid[y], 0, this.grid[y], 0, width);
        }

        // Perform defensive copy of the optional RGB grid if provided
        if (rgbGrid != null) {
            if (rgbGrid.length != height) {
                throw new IllegalArgumentException("rgbGrid height must match character grid height");
            }
            this.rgbGrid = new int[height][width];
            for (int y = 0; y < height; y++) {
                if (rgbGrid[y] == null || rgbGrid[y].length != width) {
                    throw new IllegalArgumentException(
                            "Jagged rgbGrid: row " + y + " length does not match width " + width
                    );
                }
                System.arraycopy(rgbGrid[y], 0, this.rgbGrid[y], 0, width);
            }
        } else {
            this.rgbGrid = null;
        }
    }

    /**
     * Returns the horizontal character count (columns).
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the vertical character count (rows).
     */
    public int getHeight() {
        return height;
    }

    /**
     * Retrieves the character at coordinate (x, y).
     *
     * @param x column index [0, width - 1].
     * @param y row index [0, height - 1].
     * @return the character at the specified location.
     * @throws IndexOutOfBoundsException if x or y is out of bounds.
     */
    public char getCharacter(int x, int y) {
        checkCoordinates(x, y);
        return grid[y][x];
    }

    /**
     * Retrieves the packed 32-bit ARGB/RGB color value at coordinate (x, y).
     *
     * @param x column index [0, width - 1].
     * @param y row index [0, height - 1].
     * @return packed RGB integer, or 0xFFFFFFFF (pure white) if monochrome.
     * @throws IndexOutOfBoundsException if x or y is out of bounds.
     */
    public int getRgb(int x, int y) {
        checkCoordinates(x, y);
        return rgbGrid != null ? rgbGrid[y][x] : 0xFFFFFFFF;
    }

    /**
     * Returns true if this result contains per-pixel color data.
     */
    public boolean hasColor() {
        return rgbGrid != null;
    }

    /**
     * Returns a deep copy of the underlying 2D character grid.
     */
    public char[][] getGridCopy() {
        char[][] copy = new char[height][width];
        for (int y = 0; y < height; y++) {
            System.arraycopy(grid[y], 0, copy[y], 0, width);
        }
        return copy;
    }

    /**
     * Formats the ASCII art grid into a single string with newline separators.
     *
     * @return formatted multi-line ASCII art string.
     */
    public String toPlainText() {
        // Pre-allocate StringBuilder capacity: (width chars + 1 newline) per row
        StringBuilder sb = new StringBuilder((width + 1) * height);
        for (int y = 0; y < height; y++) {
            sb.append(grid[y]);
            if (y < height - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toPlainText();
    }

    private void checkCoordinates(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException(
                    "Coordinate (" + x + ", " + y + ") is out of bounds for dimensions " + width + "x" + height
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AsciiResult that)) return false;
        return width == that.width &&
                height == that.height &&
                Arrays.deepEquals(grid, that.grid) &&
                Arrays.deepEquals(rgbGrid, that.rgbGrid);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(width, height);
        result = 31 * result + Arrays.deepHashCode(grid);
        result = 31 * result + Arrays.deepHashCode(rgbGrid);
        return result;
    }
}
