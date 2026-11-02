package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageExporter Unit Tests")
class ImageExporterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Renders AsciiResult into valid BufferedImage with expected dimensions")
    void renderToImage() {
        char[][] grid = {
                {'@', '#'},
                {'%', '*'}
        };
        AsciiResult result = new AsciiResult(grid);

        BufferedImage rendered = ImageExporter.renderToImage(result);

        assertNotNull(rendered);
        assertTrue(rendered.getWidth() > 0);
        assertTrue(rendered.getHeight() > 0);
    }

    @Test
    @DisplayName("Exports valid PNG file that can be decoded back by ImageIO")
    void exportToPngFile() throws IOException {
        char[][] grid = {
                {'@', ' '},
                {'#', '.'}
        };
        AsciiResult result = new AsciiResult(grid);

        Path pngPath = tempDir.resolve("exported_ascii.png");
        ImageExporter.exportToPng(result, pngPath, ImageExporter.ImageExportOptions.RETRO_GREEN);

        assertTrue(Files.exists(pngPath));
        assertTrue(Files.size(pngPath) > 0);

        // Verify the saved PNG is completely valid and readable
        BufferedImage reloaded = ImageIO.read(pngPath.toFile());
        assertNotNull(reloaded, "Saved PNG should be cleanly decodable by ImageIO");
        assertEquals(renderedWidth(result), reloaded.getWidth());
    }

    @Test
    @DisplayName("Handles True-Color AsciiResult without crashing")
    void trueColorExport() {
        char[][] grid = {{'@', '#'}};
        int[][] rgb = {{0xFFFF0000, 0xFF00FF00}};
        AsciiResult colorResult = new AsciiResult(grid, rgb);

        BufferedImage img = ImageExporter.renderToImage(colorResult);
        assertNotNull(img);
    }

    @Test
    @DisplayName("Throws NullPointerException for null arguments")
    void nullArgumentsThrow() {
        assertThrows(NullPointerException.class, () -> ImageExporter.renderToImage(null));
        assertThrows(NullPointerException.class, () -> ImageExporter.renderToImage(
                new AsciiResult(new char[][]{{'@'}}), null
        ));
    }

    private int renderedWidth(AsciiResult result) {
        return ImageExporter.renderToImage(result, ImageExporter.ImageExportOptions.RETRO_GREEN).getWidth();
    }
}
