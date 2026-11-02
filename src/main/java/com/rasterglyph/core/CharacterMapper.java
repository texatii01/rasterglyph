package com.rasterglyph.core;

import java.util.Objects;

/**
 * Maps numeric brightness (luminance) values in the range [0, 255] to ASCII characters
 * based on a {@link CharacterSet}.
 *
 * Mapping behavior:
 * - Default: Dark pixels (0) map to dense characters (index 0), and bright pixels (255)
 *   map to sparse characters (last index).
 * - Inverted: Dark pixels (0) map to sparse characters, and bright pixels (255) map to dense characters.
 *
 * Instances of this class are immutable and thread-safe.
 */
public final class CharacterMapper {

    private final CharacterSet characterSet;
    private final boolean inverted;
    private final CharacterSet activeSet;

    /**
     * Creates a CharacterMapper with default (non-inverted) mapping.
     *
     * @param characterSet the character palette to map into.
     * @throws NullPointerException if characterSet is null.
     */
    public CharacterMapper(CharacterSet characterSet) {
        this(characterSet, false);
    }

    /**
     * Creates a CharacterMapper with configurable inversion.
     *
     * @param characterSet the character palette to map into.
     * @param inverted true to invert mapping (light on dark backgrounds).
     * @throws NullPointerException if characterSet is null.
     */
    public CharacterMapper(CharacterSet characterSet, boolean inverted) {
        this.characterSet = Objects.requireNonNull(characterSet, "characterSet must not be null");
        this.inverted = inverted;
        this.activeSet = inverted ? characterSet.reversed() : characterSet;
    }

    /**
     * Convenience factory creating a default mapper using {@link CharacterSet#STANDARD}.
     */
    public static CharacterMapper standard() {
        return new CharacterMapper(CharacterSet.STANDARD, false);
    }

    /**
     * Maps an integer brightness value (0-255) to an ASCII character.
     * Values outside [0, 255] are clamped automatically.
     *
     * @param brightness luminance value from 0 (black) to 255 (white).
     * @return the mapped character.
     */
    public char map(int brightness) {
        int clamped = Math.clamp(brightness, 0, 255);
        int numBuckets = activeSet.length();

        // Distribute 256 discrete brightness values uniformly across all buckets:
        int index = (clamped * numBuckets) / 256;

        // Defensive guard against potential boundary overflow
        if (index >= numBuckets) {
            index = numBuckets - 1;
        }

        return activeSet.get(index);
    }

    /**
     * Maps a floating-point brightness value (0.0 to 255.0) to an ASCII character.
     *
     * @param brightness luminance value.
     * @return the mapped character.
     */
    public char map(double brightness) {
        return map((int) Math.round(brightness));
    }

    /**
     * Returns the base character set used by this mapper.
     */
    public CharacterSet getCharacterSet() {
        return characterSet;
    }

    /**
     * Returns whether character mapping is inverted.
     */
    public boolean isInverted() {
        return inverted;
    }

    @Override
    public String toString() {
        return "CharacterMapper[characterSet=" + characterSet + ", inverted=" + inverted + "]";
    }
}
