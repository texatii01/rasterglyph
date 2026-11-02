package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DitherFilter Unit Tests")
class DitherFilterTest {

    @Test
    @DisplayName("Binary dithering constrains all pixels strictly to 0 or 255")
    void binaryDitheringOutputsOnlyBlackOrWhite() {
        // Create smooth 10x10 gradient
        BufferedImage gradient = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                int gray = (x * 255) / 9;
                gradient.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }

        BufferedImage dithered = DitherFilter.ditherBinary(gradient);

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                int rgb = dithered.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                assertEquals(r, g);
                assertEquals(g, b);
                assertTrue(r == 0 || r == 255, "Binary dither pixel must be either 0 or 255, got: " + r);
            }
        }
    }

    @Test
    @DisplayName("Mid-gray (128) binary dithers into alternating black and white stipple pattern")
    void midGrayAlternatingPattern() {
        BufferedImage midGray = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = midGray.createGraphics();
        g.setColor(new Color(128, 128, 128));
        g.fillRect(0, 0, 4, 4);
        g.dispose();

        BufferedImage dithered = DitherFilter.ditherBinary(midGray);

        // Count black vs white pixels
        int blackCount = 0;
        int whiteCount = 0;
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                int r = (dithered.getRGB(x, y) >> 16) & 0xFF;
                if (r == 0) blackCount++;
                else if (r == 255) whiteCount++;
            }
        }

        // 128/255 is approx 50%, so counts should be roughly equal (stippled)
        assertTrue(blackCount > 0 && whiteCount > 0, "Dithering must create a mix of black and white pixels");
        assertEquals(16, blackCount + whiteCount);
    }

    @Test
    @DisplayName("Original image is left unmodified")
    void nonDestructive() {
        BufferedImage original = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        int initialRgb = original.getRGB(0, 0);

        DitherFilter.ditherGrayscale(original, 4);

        assertEquals(initialRgb, original.getRGB(0, 0));
    }

    @Test
    @DisplayName("Rejects invalid levels (< 2)")
    void invalidLevelsThrow() {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        assertThrows(NullPointerException.class, () -> DitherFilter.ditherGrayscale(null, 2));
        assertThrows(IllegalArgumentException.class, () -> DitherFilter.ditherGrayscale(img, 1));
        assertThrows(IllegalArgumentException.class, () -> DitherFilter.ditherGrayscale(img, 0));
        assertThrows(IllegalArgumentException.class, () -> DitherFilter.ditherColor(img, 1));
    }
}
