package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

// Mockito is not actually used but kept for consistency with the template
@ExtendWith(MockitoExtension.class)
class StringUtilities_getRandomString_Test {

    /* --------------------------------------------------------------------- */
    @Test
    void getRandomString_nullRandom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtilities.getRandomString(null, 1, 5));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getRandomString_zeroLength_returnsEmptyString() {
        Random rnd = new Random(123L);
        String result = StringUtilities.getRandomString(rnd, 0, 0);
        assertEquals("", result);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 10, 20 })
    void getRandomString_fixedLength_returnsStringOfExactLength(int length) {
        // deterministic seed
        Random rnd = new Random(42L);
        String result = StringUtilities.getRandomString(rnd, length, length);
        assertEquals(length, result.length(), "String length should be exactly the requested fixed length");
        if (length > 0) {
            char first = result.charAt(0);
            assertTrue(Character.isUpperCase(first) && Character.isLetter(first), "First character must be an uppercase alphabetic letter");
        }
        // All characters must be alphabetic
        for (char c : result.toCharArray()) {
            assertTrue(Character.isLetter(c), "All characters must be alphabetic, found: '" + c + "'");
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({ "1, 5", "3, 7", "0, 4", "5, 10" })
    void getRandomString_variableLength_isWithinBounds(int minLen, int maxLen) {
        // Use a fresh Random with a known seed for reproducibility.
        Random rnd = new Random(999L);
        String result = StringUtilities.getRandomString(rnd, minLen, maxLen);
        int actualLen = result.length();
        assertTrue(actualLen >= minLen && actualLen <= maxLen, () -> "Length " + actualLen + " not within [" + minLen + ", " + maxLen + "]");
        if (actualLen > 0) {
            char first = result.charAt(0);
            assertTrue(Character.isUpperCase(first) && Character.isLetter(first), "First character must be an uppercase alphabetic letter");
        }
        // Verify every character is a letter.
        for (char c : result.toCharArray()) {
            assertTrue(Character.isLetter(c), "All characters must be alphabetic, found: '" + c + "'");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getRandomString_minGreaterThanMax_throwsIllegalArgumentException() {
        Random rnd = new Random();
        assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(rnd, 10, 5));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getRandomString_sameSeedProducesSameString() {
        long seed = 12345L;
        int min = 4;
        int max = 8;
        Random rnd1 = new Random(seed);
        String first = StringUtilities.getRandomString(rnd1, min, max);
        Random rnd2 = new Random(seed);
        String second = StringUtilities.getRandomString(rnd2, min, max);
        assertEquals(first, second, "Two invocations with identical Random seed and bounds must produce identical strings");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getRandomString_extremeIntegerBounds_throwsIllegalArgumentException() {
        Random rnd = new Random();
        // (maxLen - minLen) + 1 overflows to 0 when using Integer.MIN_VALUE / Integer.MAX_VALUE
        assertThrows(IllegalArgumentException.class, () -> StringUtilities.getRandomString(rnd, Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getRandomString_largeSeedFixedLength() {
        Random rnd = new Random(Long.MAX_VALUE);
        int length = 10;
        String result = StringUtilities.getRandomString(rnd, length, length);
        assertEquals(length, result.length(), "Length must match the fixed bound");
        // First character uppercase, all characters alphabetic
        char first = result.charAt(0);
        assertTrue(Character.isUpperCase(first) && Character.isLetter(first), "First character must be uppercase");
        for (char c : result.toCharArray()) {
            assertTrue(Character.isLetter(c), "All characters must be alphabetic");
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({ "1, 2", "0, 1" })
    void getRandomString_smallRange_lengthWithinBounds(int minLen, int maxLen) {
        Random rnd = new Random(0L);
        String result = StringUtilities.getRandomString(rnd, minLen, maxLen);
        int len = result.length();
        assertTrue(len >= minLen && len <= maxLen, "Length " + len + " not within [" + minLen + ", " + maxLen + "]");
        if (len > 0) {
            char first = result.charAt(0);
            assertTrue(Character.isUpperCase(first) && Character.isLetter(first), "First character must be uppercase");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getRandomString_rangeIncludingZero_mayReturnEmptyString() {
        // Run the method many times; at least one call should hit length 0.
        Random rnd = new Random(0L);
        boolean sawEmpty = false;
        for (int i = 0; i < 200; i++) {
            String s = StringUtilities.getRandomString(rnd, 0, 5);
            if (s.isEmpty()) {
                sawEmpty = true;
                break;
            }
        }
        assertTrue(sawEmpty, "Within a reasonable number of attempts an empty string should be produced when the range includes zero");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getRandomString_firstCharacterIsAlwaysUppercase() {
        Random rnd = new Random(123L);
        String s = StringUtilities.getRandomString(rnd, 3, 7);
        assertFalse(s.isEmpty(), "String should not be empty for this test");
        char first = s.charAt(0);
        assertTrue(Character.isUpperCase(first) && Character.isLetter(first), "The first character must be an uppercase alphabetic letter");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getRandomString_allCharactersAreAlphabetic() {
        Random rnd = new Random(987654321L);
        String s = StringUtilities.getRandomString(rnd, 5, 15);
        for (char c : s.toCharArray()) {
            assertTrue(Character.isLetter(c), "Character '" + c + "' is not alphabetic");
        }
    }
}
