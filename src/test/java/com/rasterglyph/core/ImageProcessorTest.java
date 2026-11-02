package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageProcessor Unit Tests")
class ImageProcessorTest {

    private BufferedImage createSinglePixelImage(int r, int g, int b) {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        int rgb = (r << 16) | (g << 8) | b;
        img.setRGB(0, 0, rgb);
        return img;
    }

    private int getRed(BufferedImage img, int x, int y) {
        return (img.getRGB(x, y) >> 16) & 0xFF;
    }

    private int getGreen(BufferedImage img, int x, int y) {
        return (img.getRGB(x, y) >> 8) & 0xFF;
    }

    private int getBlue(BufferedImage img, int x, int y) {
        return img.getRGB(x, y) & 0xFF;
    }

    @Test
    @DisplayName("Luminance formula matches standard ITU-R BT.601 weights")
    void luminanceCalculations() {
        assertEquals(0.0, ImageProcessor.calculateLuminance(0, 0, 0), 0.001);
        assertEquals(255.0, ImageProcessor.calculateLuminance(255, 255, 255), 0.001);

        // L = 0.299 * 255 = 76.245
        assertEquals(76.245, ImageProcessor.calculateLuminance(255, 0, 0), 0.001);
        // L = 0.587 * 255 = 149.685
        assertEquals(149.685, ImageProcessor.calculateLuminance(0, 255, 0), 0.001);
        // L = 0.114 * 255 = 29.07
        assertEquals(29.07, ImageProcessor.calculateLuminance(0, 0, 255), 0.001);
    }

    @Test
    @DisplayName("toGrayscale sets R == G == B equal to calculated luminance")
    void toGrayscaleConversion() {
        BufferedImage original = createSinglePixelImage(255, 0, 0); // Pure Red
        BufferedImage gray = ImageProcessor.toGrayscale(original);

        int expectedGray = (int) Math.round(0.299 * 255); // 76
        assertEquals(expectedGray, getRed(gray, 0, 0));
        assertEquals(expectedGray, getGreen(gray, 0, 0));
        assertEquals(expectedGray, getBlue(gray, 0, 0));

        // Verify original remains unmodified
        assertEquals(255, getRed(original, 0, 0));
        assertEquals(0, getGreen(original, 0, 0));
    }

    @Test
    @DisplayName("adjustBrightness increases and decreases color values with clamping")
    void brightnessAdjustment() {
        BufferedImage original = createSinglePixelImage(100, 100, 100);

        BufferedImage brighter = ImageProcessor.adjustBrightness(original, 50);
        assertEquals(150, getRed(brighter, 0, 0));

        BufferedImage clampedHigh = ImageProcessor.adjustBrightness(original, 200);
        assertEquals(255, getRed(clampedHigh, 0, 0)); // Clamped to 255

        BufferedImage clampedLow = ImageProcessor.adjustBrightness(original, -150);
        assertEquals(0, getRed(clampedLow, 0, 0)); // Clamped to 0

        // Verify original remains untouched
        assertEquals(100, getRed(original, 0, 0));
    }

    @Test
    @DisplayName("adjustContrast scales distance from midpoint 128")
    void contrastAdjustment() {
        BufferedImage original = createSinglePixelImage(150, 100, 128);

        // Factor 1.0 (neutral, no change)
        BufferedImage unchanged = ImageProcessor.adjustContrast(original, 1.0);
        assertEquals(150, getRed(unchanged, 0, 0));
        assertEquals(100, getGreen(unchanged, 0, 0));
        assertEquals(128, getBlue(unchanged, 0, 0));

        // Factor 2.0 (higher contrast):
        // 150 -> 2.0 * (150 - 128) + 128 = 2.0 * 22 + 128 = 172
        // 100 -> 2.0 * (100 - 128) + 128 = 2.0 * (-28) + 128 = 72
        // 128 -> remains 128
        BufferedImage highContrast = ImageProcessor.adjustContrast(original, 2.0);
        assertEquals(172, getRed(highContrast, 0, 0));
        assertEquals(72, getGreen(highContrast, 0, 0));
        assertEquals(128, getBlue(highContrast, 0, 0));
    }

    @Test
    @DisplayName("invert inverts color values (255 - C)")
    void colorInversion() {
        BufferedImage original = createSinglePixelImage(255, 100, 0);
        BufferedImage inverted = ImageProcessor.invert(original);

        assertEquals(0, getRed(inverted, 0, 0));
        assertEquals(155, getGreen(inverted, 0, 0));
        assertEquals(255, getBlue(inverted, 0, 0));

        // Inverting twice restores original values
        BufferedImage doubleInverted = ImageProcessor.invert(inverted);
        assertEquals(255, getRed(doubleInverted, 0, 0));
        assertEquals(100, getGreen(doubleInverted, 0, 0));
        assertEquals(0, getBlue(doubleInverted, 0, 0));
    }
}
