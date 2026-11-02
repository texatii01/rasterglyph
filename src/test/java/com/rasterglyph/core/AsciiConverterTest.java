package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AsciiConverter End-to-End Tests")
class AsciiConverterTest {

    private BufferedImage createSolidImage(int width, int height, Color color) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();
        return img;
    }

    @Test
    @DisplayName("Solid black image converts entirely to darkest character ('@')")
    void solidBlackImageConversion() {
        BufferedImage blackImage = createSolidImage(100, 100, Color.BLACK);
        AsciiConverter converter = new AsciiConverter();

        AsciiResult result = converter.convert(blackImage, 10);

        assertEquals(10, result.getWidth());
        assertEquals(5, result.getHeight()); // 10 * 1.0 * 0.5 = 5

        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                assertEquals('@', result.getCharacter(x, y), "All pixels should map to '@'");
            }
        }
    }

    @Test
    @DisplayName("Solid white image converts entirely to lightest character (' ')")
    void solidWhiteImageConversion() {
        BufferedImage whiteImage = createSolidImage(100, 100, Color.WHITE);
        AsciiConverter converter = new AsciiConverter();

        AsciiResult result = converter.convert(whiteImage, 10);

        assertEquals(10, result.getWidth());
        assertEquals(5, result.getHeight());

        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                assertEquals(' ', result.getCharacter(x, y), "All pixels should map to ' '");
            }
        }
    }

    @Test
    @DisplayName("Half-black and half-white image produces sharp split")
    void splitImageConversion() {
        // Create 20x10 image: left 10 columns black, right 10 columns white
        BufferedImage splitImage = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = splitImage.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 10, 10);
        g.setColor(Color.WHITE);
        g.fillRect(10, 0, 10, 10);
        g.dispose();

        // Convert directly without scaling
        AsciiConverter converter = new AsciiConverter();
        AsciiResult result = converter.convertDirect(splitImage);

        assertEquals(20, result.getWidth());
        assertEquals(10, result.getHeight());

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                assertEquals('@', result.getCharacter(x, y));
            }
            for (int x = 10; x < 20; x++) {
                assertEquals(' ', result.getCharacter(x, y));
            }
        }
    }

    @Test
    @DisplayName("Inverted conversion swaps characters appropriately")
    void invertedConversion() {
        BufferedImage blackImage = createSolidImage(10, 10, Color.BLACK);
        CharacterMapper invertedMapper = new CharacterMapper(CharacterSet.STANDARD, true);
        AsciiConverter converter = new AsciiConverter(invertedMapper);

        AsciiResult result = converter.convertDirect(blackImage);

        // Inverted: black maps to ' ' instead of '@'
        assertEquals(' ', result.getCharacter(0, 0));
    }

    @Test
    @DisplayName("Integration pipeline with ImageProcessor adjustments")
    void pipelineWithImageProcessor() {
        // Start with black image
        BufferedImage black = createSolidImage(10, 10, Color.BLACK);

        // Invert to white using ImageProcessor
        BufferedImage white = ImageProcessor.invert(black);

        AsciiResult result = AsciiConverter.toAscii(white, 10);

        // Since it's now white, it should map to spaces
        assertEquals(' ', result.getCharacter(0, 0));
    }

    @Test
    @DisplayName("Captures RGB colors when includeColor is enabled")
    void capturesRgbColors() {
        BufferedImage redImage = createSolidImage(10, 10, Color.RED);
        AsciiConverter converter = new AsciiConverter(
                CharacterMapper.standard(),
                ImageResizer.DEFAULT_CHAR_ASPECT_RATIO,
                true // includeColor
        );

        AsciiResult result = converter.convertDirect(redImage);

        assertTrue(result.hasColor());
        // Color.RED is 0xFFFF0000
        assertEquals(Color.RED.getRGB(), result.getRgb(0, 0));
    }

    @Test
    @DisplayName("Applies Floyd–Steinberg dithering during conversion without error")
    void ditheringPipeline() {
        // Gradient image
        BufferedImage gradient = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 20; y++) {
            for (int x = 0; x < 20; x++) {
                int gray = (x * 255) / 19;
                gradient.setRGB(x, y, (gray << 16) | (gray << 8) | gray);
            }
        }

        AsciiConverter ditheredConverter = new AsciiConverter(
                CharacterMapper.standard(),
                ImageResizer.DEFAULT_CHAR_ASPECT_RATIO,
                false,
                true // dither = true
        );

        AsciiResult result = ditheredConverter.convert(gradient, 10);
        assertNotNull(result);
        assertEquals(10, result.getWidth());
        assertTrue(ditheredConverter.isDither());
    }

    @Test
    @DisplayName("Invalid parameters throw IllegalArgumentException")
    void invalidParametersThrow() {
        BufferedImage img = createSolidImage(10, 10, Color.BLACK);
        AsciiConverter converter = new AsciiConverter();

        assertThrows(NullPointerException.class, () -> converter.convert(null, 10));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(img, 0));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(img, -5));
    }
}
