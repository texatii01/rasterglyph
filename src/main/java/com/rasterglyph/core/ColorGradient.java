package com.rasterglyph.core;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Represents a multi-stop color gradient used to map luminance values or
 * spatial coordinates to custom color palettes.
 *
 * Provides linear interpolation (lerp) between color stops in RGB color space.
 *
 * Instances of this class are immutable.
 */
public final class ColorGradient {

    /**
     * Represents an individual color stop along the gradient.
     *
     * @param position normalized location along the gradient [0.0, 1.0].
     * @param color the color at this position.
     */
    public record ColorStop(double position, Color color) {
        public ColorStop {
            Objects.requireNonNull(color, "color must not be null");
            if (position < 0.0 || position > 1.0 || Double.isNaN(position)) {
                throw new IllegalArgumentException("Position must be between 0.0 and 1.0: " + position);
            }
        }
    }

    // --- Built-in Retro Presets ---

    /**
     * Classic 1980s green phosphor monochrome monitor.
     */
    public static final ColorGradient PHOSPHOR_GREEN = new Builder()
            .addStop(0.0, new Color(5, 20, 5))
            .addStop(0.5, new Color(40, 200, 40))
            .addStop(1.0, new Color(180, 255, 180))
            .build();

    /**
     * Warm amber CRT display.
     */
    public static final ColorGradient AMBER_CRT = new Builder()
            .addStop(0.0, new Color(20, 10, 0))
            .addStop(0.5, new Color(220, 130, 0))
            .addStop(1.0, new Color(255, 220, 140))
            .build();

    /**
     * Cyberpunk neon palette (Deep Purple -> Neon Pink -> Electric Cyan).
     */
    public static final ColorGradient CYBERPUNK = new Builder()
            .addStop(0.0, new Color(15, 5, 30))
            .addStop(0.4, new Color(220, 0, 130))
            .addStop(0.8, new Color(0, 230, 255))
            .addStop(1.0, new Color(240, 255, 255))
            .build();

    /**
     * Thermal / Heat-map gradient (Black -> Red -> Orange -> Yellow -> White).
     */
    public static final ColorGradient THERMAL = new Builder()
            .addStop(0.0, Color.BLACK)
            .addStop(0.33, new Color(220, 20, 20))
            .addStop(0.66, new Color(255, 180, 0))
            .addStop(1.0, Color.WHITE)
            .build();

    /**
     * Matrix digital rain aesthetic (Dark green -> Vivid matrix green -> Pure white highlight).
     */
    public static final ColorGradient MATRIX = new Builder()
            .addStop(0.0, new Color(0, 15, 0))
            .addStop(0.7, new Color(0, 255, 65))
            .addStop(1.0, new Color(220, 255, 220))
            .build();

    private final List<ColorStop> stops;

    private ColorGradient(List<ColorStop> stops) {
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("A ColorGradient requires at least 2 color stops");
        }
        List<ColorStop> sorted = new ArrayList<>(stops);
        sorted.sort(Comparator.comparingDouble(ColorStop::position));

        // Ensure boundary coverage
        if (sorted.get(0).position() != 0.0 || sorted.get(sorted.size() - 1).position() != 1.0) {
            throw new IllegalArgumentException("Gradient stops must start at 0.0 and end at 1.0");
        }

        this.stops = Collections.unmodifiableList(sorted);
    }

    /**
     * Evaluates the gradient at a normalized position t in [0.0, 1.0].
     *
     * @param t position in range [0.0, 1.0].
     * @return interpolated {@link Color}.
     */
    public Color getColorAt(double t) {
        double clamped = Math.clamp(t, 0.0, 1.0);

        // Find surrounding stops
        for (int i = 0; i < stops.size() - 1; i++) {
            ColorStop left = stops.get(i);
            ColorStop right = stops.get(i + 1);

            if (clamped >= left.position() && clamped <= right.position()) {
                double range = right.position() - left.position();
                double u = range == 0.0 ? 0.0 : (clamped - left.position()) / range;
                return interpolate(left.color(), right.color(), u);
            }
        }

        return stops.get(stops.size() - 1).color();
    }

    /**
     * Evaluates the gradient for an integer luminance value [0, 255].
     *
     * @param luminance brightness in range [0, 255].
     * @return packed 32-bit RGB integer (0x00RRGGBB).
     */
    public int getRgbAt(int luminance) {
        int clamped = Math.clamp(luminance, 0, 255);
        Color c = getColorAt(clamped / 255.0);
        return c.getRGB() & 0x00FFFFFF;
    }

    /**
     * Applies this color gradient across an existing {@link AsciiResult}.
     *
     * Calculates the luminance of each character position from the underlying
     * character ramp or brightness, producing a stunning true-color result.
     *
     * @param monochromeResult source ASCII result.
     * @param characterSet the character set used to interpret density.
     * @return a new true-color {@link AsciiResult} tinted with this gradient.
     */
    public AsciiResult apply(AsciiResult monochromeResult, CharacterSet characterSet) {
        Objects.requireNonNull(monochromeResult, "monochromeResult must not be null");
        Objects.requireNonNull(characterSet, "characterSet must not be null");

        int width = monochromeResult.getWidth();
        int height = monochromeResult.getHeight();
        char[][] gridCopy = monochromeResult.getGridCopy();
        int[][] rgbGrid = new int[height][width];

        int charCount = characterSet.length();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                char ch = monochromeResult.getCharacter(x, y);

                // Find index of character in set to determine tonal position
                int index = characterSet.getCharacters().indexOf(ch);
                if (index == -1) {
                    index = 0;
                }

                // Invert position so index 0 (darkest @) maps to t=0.0 (shadows)
                // and index length-1 (lightest space) maps to t=1.0 (highlights)
                double t = charCount > 1 ? (double) index / (charCount - 1) : 0.0;
                rgbGrid[y][x] = getColorAt(t).getRGB();
            }
        }

        return new AsciiResult(gridCopy, rgbGrid);
    }

    private static Color interpolate(Color c1, Color c2, double factor) {
        double f = Math.clamp(factor, 0.0, 1.0);
        int r = (int) Math.round(c1.getRed() + f * (c2.getRed() - c1.getRed()));
        int g = (int) Math.round(c1.getGreen() + f * (c2.getGreen() - c1.getGreen()));
        int b = (int) Math.round(c1.getBlue() + f * (c2.getBlue() - c1.getBlue()));

        return new Color(
                Math.clamp(r, 0, 255),
                Math.clamp(g, 0, 255),
                Math.clamp(b, 0, 255)
        );
    }

    public List<ColorStop> getStops() {
        return stops;
    }

    /**
     * Builder for constructing custom ColorGradient instances.
     */
    public static final class Builder {
        private final List<ColorStop> stops = new ArrayList<>();

        public Builder addStop(double position, Color color) {
            stops.add(new ColorStop(position, color));
            return this;
        }

        public ColorGradient build() {
            return new ColorGradient(stops);
        }
    }
}
