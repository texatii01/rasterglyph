package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HtmlExporter Unit Tests")
class HtmlExporterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Escapes HTML special characters (<, >, &, \") in ASCII output")
    void escapesSpecialHtmlCharacters() {
        char[][] grid = {{'<', '>', '&', '"'}};
        AsciiResult result = new AsciiResult(grid);

        String html = HtmlExporter.toHtml(result);

        assertTrue(html.contains("&lt;&gt;&amp;&quot;"));
        assertFalse(html.contains("<code><>&\"</code>"));
    }

    @Test
    @DisplayName("Generates true-color spans with hex color codes")
    void trueColorSpans() {
        char[][] grid = {{'@', '#'}};
        int[][] rgb = {{0x00FF00FF, 0x0000FFFF}}; // Magenta, Cyan
        AsciiResult result = new AsciiResult(grid, rgb);

        String html = HtmlExporter.toHtml(result);

        assertTrue(html.contains("<span style=\"color:#FF00FF\">@</span>"));
        assertTrue(html.contains("<span style=\"color:#00FFFF\">#</span>"));
    }

    @Test
    @DisplayName("Deduplicates identical consecutive color spans")
    void deduplicatesColorSpans() {
        char[][] grid = {{'@', '@', '@'}};
        int[][] rgb = {{0x00FF5500, 0x00FF5500, 0x00FF5500}}; // 3 identical pixels
        AsciiResult result = new AsciiResult(grid, rgb);

        String html = HtmlExporter.toHtml(result);

        // Should produce 1 span containing all 3 characters: <span style="...">@@@</span>
        assertTrue(html.contains("<span style=\"color:#FF5500\">@@@</span>"));
    }

    @Test
    @DisplayName("Exports valid HTML file directly to disk")
    void exportsFileToDisk() throws IOException {
        char[][] grid = {{'@', ' '}};
        AsciiResult result = new AsciiResult(grid);

        Path outFile = tempDir.resolve("art.html");
        HtmlExporter.exportToFile(result, outFile, HtmlExporter.HtmlOptions.RETRO_GREEN);

        assertTrue(Files.exists(outFile));
        String content = Files.readString(outFile);
        assertTrue(content.contains("<!DOCTYPE html>"));
        assertTrue(content.contains("RASTER//GLYPH — Phosphor Green"));
    }

    @Test
    @DisplayName("Throws NullPointerException on null inputs")
    void nullInputsThrow() {
        assertThrows(NullPointerException.class, () -> HtmlExporter.toHtml(null));
        assertThrows(NullPointerException.class, () -> HtmlExporter.toHtml(new AsciiResult(new char[][]{{'@'}}), null));
    }
}
