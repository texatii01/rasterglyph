package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CharacterMapper Unit Tests")
class CharacterMapperTest {

    @Test
    @DisplayName("Maps pure black (0) and pure white (255) to boundary characters")
    void boundaryMapping() {
        CharacterMapper mapper = CharacterMapper.standard();

        // STANDARD is "@%#*+=-:. " (0 is '@', 9 is ' ')
        assertEquals('@', mapper.map(0), "Brightness 0 should map to darkest character '@'");
        assertEquals(' ', mapper.map(255), "Brightness 255 should map to lightest character ' '");
    }

    @Test
    @DisplayName("Clamps out-of-range values to [0, 255]")
    void clampingBehavior() {
        CharacterMapper mapper = CharacterMapper.standard();

        assertEquals('@', mapper.map(-50), "Negative values should clamp to 0 ('@')");
        assertEquals(' ', mapper.map(500), "Values > 255 should clamp to 255 (' ')");
    }

    @Test
    @DisplayName("Handles floating-point brightness with rounding")
    void floatingPointMapping() {
        CharacterMapper mapper = CharacterMapper.standard();

        assertEquals('@', mapper.map(0.2));
        assertEquals(' ', mapper.map(254.8));
    }

    @Test
    @DisplayName("Inverted mapping reverses density relationship")
    void invertedMapping() {
        CharacterMapper invertedMapper = new CharacterMapper(CharacterSet.STANDARD, true);

        // In inverted mode: 0 -> lightest (' '), 255 -> darkest ('@')
        assertEquals(' ', invertedMapper.map(0));
        assertEquals('@', invertedMapper.map(255));
    }

    @Test
    @DisplayName("Maps binary character set (# and space) cleanly at midpoint")
    void binaryMapping() {
        CharacterMapper binaryMapper = new CharacterMapper(CharacterSet.BINARY);

        // Index 0 ('#') for 0..127, Index 1 (' ') for 128..255
        assertEquals('#', binaryMapper.map(0));
        assertEquals('#', binaryMapper.map(127));
        assertEquals(' ', binaryMapper.map(128));
        assertEquals(' ', binaryMapper.map(255));
    }

    @Test
    @DisplayName("Throws NullPointerException if CharacterSet is null")
    void nullCharacterSetThrows() {
        assertThrows(NullPointerException.class, () -> new CharacterMapper(null));
    }
}
