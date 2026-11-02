package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SvgExporter Unit Tests")
class SvgExporterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Generates valid SVG with expected XML tags and viewBox")
    void validSvgStructure() {
        char[][] grid = {{'@', '#'}};
        AsciiResult result = new AsciiResult(grid);

        String svg = SvgExporter.toSvg(result);

        assertTrue(svg.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(svg.contains("<svg xmlns=\"http://www.w3.org/2000/svg\""));
        assertTrue(svg.contains("viewBox="));
        assertTrue(svg.contains("<text"));
        assertTrue(svg.contains("xml:space=\"preserve\""));
        assertTrue(svg.contains("</svg>"));
    }

    @Test
    @DisplayName("Escapes XML special characters in SVG content")
    void xmlEntityEscaping() {
        char[][] grid = {{'<', '>', '&', '"', '\''}};
        AsciiResult result = new AsciiResult(grid);

        String svg = SvgExporter.toSvg(result);

        assertTrue(svg.contains("&lt;&gt;&amp;&quot;&apos;"));
    }

    @Test
    @DisplayName("Generates colored tspan elements for true-color results")
    void trueColorTspans() {
        char[][] grid = {{'@', '.'}};
        int[][] rgb = {{0x00FF00FF, 0x0000FFFF}}; // Magenta, Cyan
        AsciiResult colorResult = new AsciiResult(grid, rgb);

        String svg = SvgExporter.toSvg(colorResult);

        assertTrue(svg.contains("fill=\"#FF00FF\""));
        assertTrue(svg.contains("fill=\"#00FFFF\""));
    }

    @Test
    @DisplayName("Exports SVG file cleanly to disk")
    void exportToFileOnDisk() throws IOException {
        char[][] grid = {{'A', 'B'}};
        AsciiResult result = new AsciiResult(grid);

        Path svgPath = tempDir.resolve("art.svg");
        SvgExporter.exportToFile(result, svgPath, SvgExporter.SvgOptions.RETRO_GREEN);

        assertTrue(Files.exists(svgPath));
        assertTrue(Files.size(svgPath) > 0);
        String content = Files.readString(svgPath);
        assertTrue(content.contains("fill=\"#33ff33\""));
    }

    @Test
    @DisplayName("Throws NullPointerException for null arguments")
    void nullArgumentsThrow() {
        assertThrows(NullPointerException.class, () -> SvgExporter.toSvg(null));
        assertThrows(NullPointerException.class, () -> SvgExporter.toSvg(
                new AsciiResult(new char[][]{{'@'}}), null
        ));
    }
}
