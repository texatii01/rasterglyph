package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageResizer Unit Tests")
class ImageResizerTest {

    @Test
    @DisplayName("Calculates height correctly for square image with 0.5 font aspect ratio")
    void squareImageAspectCalculation() {
        // Square image (1000 x 1000)
        // targetWidth = 100, fontRatio = 0.5 -> expected height = 100 * 1.0 * 0.5 = 50
        int height = ImageResizer.calculateHeight(1000, 1000, 100, 0.5);
        assertEquals(50, height);
    }

    @Test
    @DisplayName("Calculates height correctly for wide 16:9 image")
    void widescreenAspectCalculation() {
        // 1920 x 1080 (aspect ratio 1080/1920 = 0.5625)
        // targetWidth = 80, fontRatio = 0.5 -> height = round(0.5625 * 80 * 0.5) = round(22.5) = 23
        int height = ImageResizer.calculateHeight(1920, 1080, 80, 0.5);
        assertEquals(23, height);
    }

    @Test
    @DisplayName("Guarantees minimum height of 1 row even for extreme aspect ratios")
    void minimumHeightGuaranteed() {
        int height = ImageResizer.calculateHeight(10000, 1, 10, 0.5);
        assertEquals(1, height);
    }

    @Test
    @DisplayName("Resizes image to exact target dimensions")
    void resizeExactDimensions() {
        BufferedImage original = new BufferedImage(200, 100, BufferedImage.TYPE_INT_RGB);
        BufferedImage scaled = ImageResizer.resizeExact(original, 40, 20);

        assertEquals(40, scaled.getWidth());
        assertEquals(20, scaled.getHeight());
        // Verify original remains untouched
        assertEquals(200, original.getWidth());
        assertEquals(100, original.getHeight());
    }

    @Test
    @DisplayName("Automatic resize sets width and proportional height")
    void automaticResize() {
        // 400 x 400 square image resized to width 80
        BufferedImage original = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        BufferedImage scaled = ImageResizer.resize(original, 80);

        assertEquals(80, scaled.getWidth());
        assertEquals(40, scaled.getHeight()); // 80 * 0.5 = 40
    }

    @Test
    @DisplayName("Rejects negative or invalid arguments")
    void invalidArgumentsThrow() {
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);

        assertThrows(NullPointerException.class, () -> ImageResizer.resize(null, 10));
        assertThrows(IllegalArgumentException.class, () -> ImageResizer.resize(img, 0));
        assertThrows(IllegalArgumentException.class, () -> ImageResizer.resize(img, -5));
        assertThrows(IllegalArgumentException.class, () -> ImageResizer.calculateHeight(10, 10, 10, -0.5));
    }
}
