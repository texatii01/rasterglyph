package com.rasterglyph.core;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Handles safe loading of local image files into {@link BufferedImage} instances.
 *
 * Supported formats:
 * - PNG (.png)
 * - JPEG (.jpg, .jpeg)
 * - BMP (.bmp)
 */
public final class ImageLoader {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "bmp");

    private ImageLoader() {
        // Utility class; prevent instantiation
    }

    /**
     * Loads an image from the specified file path.
     *
     * @param filePath path to the image file.
     * @return loaded {@link BufferedImage}.
     * @throws NullPointerException if filePath is null.
     * @throws FileNotFoundException if the file does not exist or is not a regular file.
     * @throws IllegalArgumentException if the file format is unsupported or file cannot be decoded.
     * @throws IOException if an error occurs during reading.
     */
    public static BufferedImage load(String filePath) throws IOException {
        Objects.requireNonNull(filePath, "filePath must not be null");
        return load(Path.of(filePath));
    }

    /**
     * Loads an image from the specified {@link File}.
     *
     * @param file image file to load.
     * @return loaded {@link BufferedImage}.
     * @throws NullPointerException if file is null.
     * @throws FileNotFoundException if the file does not exist or is not a regular file.
     * @throws IllegalArgumentException if the file format is unsupported or file cannot be decoded.
     * @throws IOException if an error occurs during reading.
     */
    public static BufferedImage load(File file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        return load(file.toPath());
    }

    /**
     * Loads an image from the specified {@link Path}.
     *
     * @param path path to the image file.
     * @return loaded {@link BufferedImage}.
     * @throws NullPointerException if path is null.
     * @throws FileNotFoundException if the file does not exist or is not a regular file.
     * @throws IllegalArgumentException if the file format is unsupported or file cannot be decoded.
     * @throws IOException if an error occurs during reading.
     */
    public static BufferedImage load(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");

        if (!Files.exists(path)) {
            throw new FileNotFoundException("Image file does not exist: " + path.toAbsolutePath());
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Path is not a regular file: " + path.toAbsolutePath());
        }

        String extension = getFileExtension(path.getFileName().toString());
        if (!isSupportedExtension(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported image format: '." + extension + "'. Supported formats are: " + SUPPORTED_EXTENSIONS
            );
        }

        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("Failed to decode image. The file may be corrupted: " + path.toAbsolutePath());
        }

        return image;
    }

    /**
     * Checks whether a file extension is supported (case-insensitive).
     *
     * @param extension file extension without dot (e.g. "png", "jpg").
     * @return true if supported, false otherwise.
     */
    public static boolean isSupportedExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    /**
     * Extracts the file extension from a file name.
     *
     * @param fileName the file name (e.g., "photo.png").
     * @return the extension in lowercase without the dot, or empty string if none.
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Returns an unmodifiable set of supported file extensions (in lowercase).
     */
    public static Set<String> getSupportedExtensions() {
        return Collections.unmodifiableSet(SUPPORTED_EXTENSIONS);
    }
}
