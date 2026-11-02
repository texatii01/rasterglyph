package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SobelEdgeDetector Unit Tests")
class SobelEdgeDetectorTest {

    @Test
    @DisplayName("Flat uniform image produces zero edge magnitude")
    void flatImageHasNoEdges() {
        BufferedImage flat = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flat.createGraphics();
        g.setColor(new Color(100, 100, 100));
        g.fillRect(0, 0, 10, 10);
        g.dispose();

        BufferedImage edges = SobelEdgeDetector.detectEdges(flat);

        // Every pixel should be 0 (black, no edge)
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                assertEquals(0, edges.getRGB(x, y) & 0x00FFFFFF);
            }
        }
    }

    @Test
    @DisplayName("Vertical split image detects vertical edge character ('|')")
    void verticalEdgeDetection() {
        // 20x10 image: Left half black, right half white
        BufferedImage img = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 10, 10);
        g.setColor(Color.WHITE);
        g.fillRect(10, 0, 10, 10);
        g.dispose();

        AsciiResult edges = SobelEdgeDetector.toEdgeAscii(img, 20, 30.0);

        // Pixels right on the boundary (x=9 or x=10) should have vertical stroke '|'
        boolean foundVerticalStroke = false;
        for (int y = 1; y < edges.getHeight() - 1; y++) {
            char ch = edges.getCharacter(9, y);
            char chNext = edges.getCharacter(10, y);
            if (ch == '|' || chNext == '|') {
                foundVerticalStroke = true;
                break;
            }
        }
        assertTrue(foundVerticalStroke, "Should detect vertical line character '|' along vertical boundary");
    }

    @Test
    @DisplayName("Horizontal split image detects horizontal edge character ('-')")
    void horizontalEdgeDetection() {
        // 10x20 image: Top half black, bottom half white
        BufferedImage img = new BufferedImage(10, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 10, 10);
        g.setColor(Color.WHITE);
        g.fillRect(0, 10, 10, 10);
        g.dispose();

        AsciiResult edges = SobelEdgeDetector.toEdgeAscii(img, 10, 30.0);

        // Boundary rows should contain horizontal stroke '-'
        boolean foundHorizontalStroke = false;
        for (int y = 0; y < edges.getHeight(); y++) {
            for (int x = 1; x < edges.getWidth() - 1; x++) {
                if (edges.getCharacter(x, y) == '-') {
                    foundHorizontalStroke = true;
                    break;
                }
            }
        }
        assertTrue(foundHorizontalStroke, "Should detect horizontal line character '-' along horizontal boundary");
    }

    @Test
    @DisplayName("overlayEdges combines shaded background with edge contours")
    void overlayEdgesCombinesBoth() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, 5, 10);
        g.setColor(Color.WHITE);
        g.fillRect(5, 0, 5, 10);
        g.dispose();

        AsciiConverter converter = new AsciiConverter();
        AsciiResult baseShaded = converter.convertDirect(img);

        AsciiResult overlaid = SobelEdgeDetector.overlayEdges(baseShaded, img, 30.0);

        assertEquals(10, overlaid.getWidth());
        assertEquals(10, overlaid.getHeight());

        // Far left should remain '@' (shaded black)
        assertEquals('@', overlaid.getCharacter(0, 5));
        // Far right should remain ' ' (shaded white)
        assertEquals(' ', overlaid.getCharacter(9, 5));

        // Center boundary should contain the edge stroke '|'
        char centerChar = overlaid.getCharacter(4, 5);
        char centerChar2 = overlaid.getCharacter(5, 5);
        assertTrue(centerChar == '|' || centerChar2 == '|', "Boundary should be overlaid with edge stroke '|'");
    }

    @Test
    @DisplayName("Throws NullPointerException on null input")
    void nullInputThrows() {
        assertThrows(NullPointerException.class, () -> SobelEdgeDetector.detectEdges(null));
        assertThrows(NullPointerException.class, () -> SobelEdgeDetector.toEdgeAscii(null, 10, 50.0));
    }
}
