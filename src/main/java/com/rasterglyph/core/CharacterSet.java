package com.rasterglyph.core;

import java.util.Objects;

/**
 * Represents an ordered palette of ASCII characters used to render visual density.
 *
 * By convention:
 * - Index 0 represents the darkest / highest visual density (e.g. '@').
 * - The last index represents the brightest / lowest visual density (e.g. ' ').
 *
 * Instances of this class are immutable and thread-safe.
 */
public final class CharacterSet {

    /**
     * Standard 10-character ramp ordered from darkest to lightest.
     */
    public static final CharacterSet STANDARD = new CharacterSet("@%#*+=-:. ");

    /**
     * Extended 70-character ramp for higher tonal resolution.
     */
    public static final CharacterSet EXTENDED = new CharacterSet(
            "$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'. "
    );

    /**
     * High-contrast two-character binary ramp.
     */
    public static final CharacterSet BINARY = new CharacterSet("# ");

    /**
     * Unicode block elements for dense terminal rendering.
     */
    public static final CharacterSet BLOCKS = new CharacterSet("█▓▒░ ");

    private final String characters;

    /**
     * Creates a custom CharacterSet.
     *
     * @param characters non-empty string of characters ordered by visual density.
     * @throws NullPointerException if characters is null.
     * @throws IllegalArgumentException if characters is empty.
     */
    public CharacterSet(String characters) {
        Objects.requireNonNull(characters, "characters string must not be null");
        if (characters.isEmpty()) {
            throw new IllegalArgumentException("Character set must contain at least one character");
        }
        this.characters = characters;
    }

    /**
     * Retrieves the character at the specified density index.
     *
     * @param index 0-based index.
     * @return the character at the index.
     * @throws IndexOutOfBoundsException if index is outside [0, length() - 1].
     */
    public char get(int index) {
        if (index < 0 || index >= characters.length()) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " is out of bounds for character set of length " + characters.length()
            );
        }
        return characters.charAt(index);
    }

    /**
     * Returns the number of characters in this palette.
     */
    public int length() {
        return characters.length();
    }

    /**
     * Returns the underlying character string.
     */
    public String getCharacters() {
        return characters;
    }

    /**
     * Returns a new CharacterSet with inverted character order.
     * Useful when switching between dark-on-light and light-on-dark display modes.
     */
    public CharacterSet reversed() {
        return new CharacterSet(new StringBuilder(characters).reverse().toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CharacterSet that)) return false;
        return characters.equals(that.characters);
    }

    @Override
    public int hashCode() {
        return characters.hashCode();
    }

    @Override
    public String toString() {
        return "CharacterSet[\"" + characters + "\"]";
    }
}
