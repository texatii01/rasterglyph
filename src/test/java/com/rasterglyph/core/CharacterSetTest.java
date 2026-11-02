package com.rasterglyph.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CharacterSet Unit Tests")
class CharacterSetTest {

    @Test
    @DisplayName("STANDARD character set contains expected 10 characters")
    void standardCharacterSet() {
        CharacterSet standard = CharacterSet.STANDARD;
        assertEquals(10, standard.length());
        assertEquals("@%#*+=-:. ", standard.getCharacters());
        assertEquals('@', standard.get(0));       // Darkest
        assertEquals(' ', standard.get(9));       // Lightest
    }

    @Test
    @DisplayName("Custom character set initializes correctly")
    void customCharacterSet() {
        CharacterSet custom = new CharacterSet("ABC");
        assertEquals(3, custom.length());
        assertEquals('A', custom.get(0));
        assertEquals('B', custom.get(1));
        assertEquals('C', custom.get(2));
    }

    @Test
    @DisplayName("Reversed character set inverts character order")
    void reversedCharacterSet() {
        CharacterSet original = new CharacterSet("@#.");
        CharacterSet reversed = original.reversed();

        assertEquals(".#@", reversed.getCharacters());
        assertEquals('.', reversed.get(0));
        assertEquals('@', reversed.get(2));
    }

    @Test
    @DisplayName("Null or empty character sets throw expected exceptions")
    void validationRules() {
        assertThrows(NullPointerException.class, () -> new CharacterSet(null));
        assertThrows(IllegalArgumentException.class, () -> new CharacterSet(""));
    }

    @Test
    @DisplayName("Out of bounds index throws IndexOutOfBoundsException")
    void outOfBoundsAccess() {
        CharacterSet set = new CharacterSet("ABC");
        assertThrows(IndexOutOfBoundsException.class, () -> set.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> set.get(3));
    }

    @Test
    @DisplayName("Equality and hashCode behave correctly")
    void equalityContract() {
        CharacterSet set1 = new CharacterSet("@#.");
        CharacterSet set2 = new CharacterSet("@#.");
        CharacterSet set3 = new CharacterSet(".#@");

        assertEquals(set1, set2);
        assertEquals(set1.hashCode(), set2.hashCode());
        assertNotEquals(set1, set3);
    }
}
