package com.rasterglyph.ui;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Utility bridge connecting AWT / {@link BufferedImage} processing
 * with JavaFX {@link Image} display pipelines.
 */
public final class FxImageUtils {

    private FxImageUtils() {
        // Utility class; prevent instantiation
    }

    /**
     * Converts a standard {@link BufferedImage} into a JavaFX {@link Image}.
     *
     * @param bufferedImage the source AWT image.
     * @return JavaFX Image representation.
     * @throws NullPointerException if bufferedImage is null.
     */
    public static Image toFxImage(BufferedImage bufferedImage) {
        Objects.requireNonNull(bufferedImage, "bufferedImage must not be null");
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    /**
     * Converts a JavaFX {@link Image} back into a standard {@link BufferedImage}.
     *
     * @param fxImage the source JavaFX image.
     * @return BufferedImage representation.
     * @throws NullPointerException if fxImage is null.
     */
    public static BufferedImage toBufferedImage(Image fxImage) {
        Objects.requireNonNull(fxImage, "fxImage must not be null");
        return SwingFXUtils.fromFXImage(fxImage, null);
    }
}
