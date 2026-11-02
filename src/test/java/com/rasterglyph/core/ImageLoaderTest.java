package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ImageLoader Unit Tests")
class ImageLoaderTest {

    @TempDir
    Path tempDir;

    private Path createTestImage(String fileName, String format, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, width, height);
        g2d.dispose();

        Path filePath = tempDir.resolve(fileName);
        ImageIO.write(image, format, filePath.toFile());
        return filePath;
    }

    @Test
    @DisplayName("Successfully loads PNG image with correct dimensions")
    void loadPngImage() throws IOException {
        Path path = createTestImage("test.png", "png", 32, 24);
        BufferedImage loaded = ImageLoader.load(path);

        assertNotNull(loaded);
        assertEquals(32, loaded.getWidth());
        assertEquals(24, loaded.getHeight());
    }

    @Test
    @DisplayName("Successfully loads JPG image")
    void loadJpgImage() throws IOException {
        Path path = createTestImage("test.jpg", "jpg", 16, 16);
        BufferedImage loaded = ImageLoader.load(path);

        assertNotNull(loaded);
        assertEquals(16, loaded.getWidth());
        assertEquals(16, loaded.getHeight());
    }

    @Test
    @DisplayName("Successfully loads BMP image")
    void loadBmpImage() throws IOException {
        Path path = createTestImage("test.bmp", "bmp", 20, 10);
        BufferedImage loaded = ImageLoader.load(path);

        assertNotNull(loaded);
        assertEquals(20, loaded.getWidth());
        assertEquals(10, loaded.getHeight());
    }

    @Test
    @DisplayName("Throws FileNotFoundException when file does not exist")
    void nonExistentFileThrows() {
        Path nonExistent = tempDir.resolve("ghost.png");
        assertThrows(FileNotFoundException.class, () -> ImageLoader.load(nonExistent));
    }

    @Test
    @DisplayName("Throws IllegalArgumentException for unsupported formats (e.g. .txt, .gif)")
    void unsupportedFormatThrows() throws IOException {
        Path txtFile = tempDir.resolve("sample.txt");
        Files.writeString(txtFile, "hello");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ImageLoader.load(txtFile));
        assertTrue(ex.getMessage().contains("Unsupported image format"));
    }

    @Test
    @DisplayName("Throws IOException when reading corrupted image file")
    void corruptedImageFileThrows() throws IOException {
        Path fakePng = tempDir.resolve("fake.png");
        Files.writeString(fakePng, "not really a png file data");

        assertThrows(IOException.class, () -> ImageLoader.load(fakePng));
    }

    @Test
    @DisplayName("Correctly parses extensions and validates support")
    void extensionParsing() {
        assertEquals("png", ImageLoader.getFileExtension("photo.PNG"));
        assertEquals("jpeg", ImageLoader.getFileExtension("my.photo.jpeg"));
        assertEquals("", ImageLoader.getFileExtension("noextension"));
        assertEquals("", ImageLoader.getFileExtension(".hidden"));

        assertTrue(ImageLoader.isSupportedExtension("PNG"));
        assertTrue(ImageLoader.isSupportedExtension("jpg"));
        assertTrue(ImageLoader.isSupportedExtension("JPEG"));
        assertTrue(ImageLoader.isSupportedExtension("bmp"));
        assertFalse(ImageLoader.isSupportedExtension("gif"));
        assertFalse(ImageLoader.isSupportedExtension("exe"));
    }
}
