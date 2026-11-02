package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ColorGradient Unit Tests")
class ColorGradientTest {

    @Test
    @DisplayName("Interpolates exact endpoint colors at t = 0.0 and t = 1.0")
    void endpointInterpolation() {
        ColorGradient gradient = new ColorGradient.Builder()
                .addStop(0.0, Color.BLACK)
                .addStop(1.0, Color.WHITE)
                .build();

        assertEquals(Color.BLACK, gradient.getColorAt(0.0));
        assertEquals(Color.WHITE, gradient.getColorAt(1.0));
    }

    @Test
    @DisplayName("Interpolates exact midpoint (t = 0.5) correctly")
    void midpointInterpolation() {
        ColorGradient gradient = new ColorGradient.Builder()
                .addStop(0.0, new Color(0, 0, 0))
                .addStop(1.0, new Color(100, 200, 50))
                .build();

        Color mid = gradient.getColorAt(0.5);

        assertEquals(50, mid.getRed());
        assertEquals(100, mid.getGreen());
        assertEquals(25, mid.getBlue());
    }

    @Test
    @DisplayName("Handles multi-stop gradients with non-uniform segment lengths")
    void multiStopInterpolation() {
        ColorGradient gradient = new ColorGradient.Builder()
                .addStop(0.0, Color.RED)
                .addStop(0.2, Color.GREEN)
                .addStop(1.0, Color.BLUE)
                .build();

        assertEquals(Color.RED, gradient.getColorAt(0.0));
        assertEquals(Color.GREEN, gradient.getColorAt(0.2));
        assertEquals(Color.BLUE, gradient.getColorAt(1.0));
    }

    @Test
    @DisplayName("Clamps positions outside [0.0, 1.0] to boundaries")
    void clamping() {
        ColorGradient gradient = new ColorGradient.Builder()
                .addStop(0.0, Color.RED)
                .addStop(1.0, Color.BLUE)
                .build();

        assertEquals(Color.RED, gradient.getColorAt(-0.5));
        assertEquals(Color.BLUE, gradient.getColorAt(1.5));
    }

    @Test
    @DisplayName("Applies gradient to monochrome AsciiResult cleanly")
    void applyToAsciiResult() {
        char[][] grid = {{'@', ' '}};
        AsciiResult mono = new AsciiResult(grid);

        ColorGradient gradient = new ColorGradient.Builder()
                .addStop(0.0, Color.RED)
                .addStop(1.0, Color.BLUE)
                .build();

        AsciiResult tinted = gradient.apply(mono, CharacterSet.STANDARD);

        assertTrue(tinted.hasColor());
        assertEquals(Color.RED.getRGB(), tinted.getRgb(0, 0));
        assertEquals(Color.BLUE.getRGB(), tinted.getRgb(1, 0));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException if stops don't cover 0.0 to 1.0")
    void invalidStopsThrow() {
        assertThrows(IllegalArgumentException.class, () -> new ColorGradient.Builder()
                .addStop(0.2, Color.BLACK)
                .addStop(0.8, Color.WHITE)
                .build()
        );
    }
}
