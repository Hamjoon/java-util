package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Collections;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CharacterArrayConversions_toStringBuilder_Test {

    @Mock
    private Converter // Converter is not used by the method, but required as a parameter
    mockConverter;

    /* --------------------------------------------------------------
     * Group: toStringBuilder - normal and typical usage
     * -------------------------------------------------------------- */
    private static Stream<Arguments> normalUsageProvider() {
        return Stream.of(// non‑empty array
        arguments(new Character[] { 'a', 'b', 'c' }, "abc"), // empty array
        arguments(new Character[0], ""));
    }

    /* --------------------------------------------------------------
     * Group: toStringBuilder - special Unicode characters
     * -------------------------------------------------------------- */
    private static Stream<Arguments> unicodeProvider() {
        return Stream.of(arguments(new Character[] { '\u2603', '\u00A9', '\n' }, "\u2603\u00A9\n"));
    }

    @Test
    @DisplayName("toStringBuilder returns a StringBuilder with concatenated characters")
    void testToStringBuilderWithCharacters() {
        // given
        Character[] input = new Character[] { 'a', 'b', 'c', 'Ω' };
        // when
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals("abcΩ", result.toString(), "StringBuilder content should match concatenated characters");
        assertEquals(4, result.length(), "Length of StringBuilder should equal number of characters");
    }

    @Test
    @DisplayName("toStringBuilder handles an empty Character array")
    void testToStringBuilderEmptyArray() {
        // given
        Character[] input = new Character[0];
        // when
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals("", result.toString(), "StringBuilder should be empty for an empty array");
        assertEquals(0, result.length(), "Length should be zero for an empty array");
    }

    @Test
    @DisplayName("toStringBuilder throws NullPointerException when source is null")
    void testToStringBuilderNullSource() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuilder(nullSource, mockConverter), "Null source should cause NullPointerException");
    }

    @Test
    @DisplayName("toStringBuilder throws NullPointerException when array contains null element")
    void testToStringBuilderArrayWithNullElement() {
        // given
        Character[] input = new Character[] { 'x', null, 'y' };
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuilder(input, mockConverter), "Array containing null should cause NullPointerException due to autounboxing");
    }

    @ParameterizedTest(name = "normal usage: input={0}, expected=\"{1}\"")
    @MethodSource("normalUsageProvider")
    @DisplayName("toStringBuilder normal usage – non‑empty and empty arrays")
    void testToStringBuilderNormalUsage(Character[] input, String expected) {
        // when
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        // then
        assertNotNull(result, "Resulting StringBuilder should never be null");
        assertEquals(expected, result.toString(), "StringBuilder content must match the concatenated characters");
        assertEquals(expected.length(), result.length(), "Length of StringBuilder must equal number of characters");
    }

    @ParameterizedTest(name = "unicode case: expected=\"{1}\"")
    @MethodSource("unicodeProvider")
    @DisplayName("toStringBuilder correctly handles Unicode and escape characters")
    void testToStringBuilderUnicodeCharacters(Character[] input, String expected) {
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        assertNotNull(result);
        assertEquals(expected, result.toString(), "Unicode and escape characters must be preserved exactly");
    }

    /* --------------------------------------------------------------
     * Group: toStringBuilder - large array
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("toStringBuilder works with a large Character array (1000 elements)")
    void testToStringBuilderLargeArray() {
        // create an array of 1000 'a' characters
        Character[] input = Collections.nCopies(1000, 'a').toArray(new Character[0]);
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        assertNotNull(result);
        assertEquals(1000, result.length(), "Resulting StringBuilder length should be 1000");
        assertTrue(result.chars().allMatch(ch -> ch == 'a'), "All characters in the StringBuilder should be 'a'");
    }

    /* --------------------------------------------------------------
     * Group: toStringBuilder - non‑Character source type
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("toStringBuilder throws ClassCastException for non‑Character source")
    void testToStringBuilderNonCharacterSource() {
        // not a Character[]
        Object nonCharacterSource = new String[] { "a", "b" };
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toStringBuilder(nonCharacterSource, mockConverter), "Passing a non‑Character[] should result in a ClassCastException");
    }

    /* --------------------------------------------------------------
     * Group: toStringBuilder - non‑null converter argument
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("toStringBuilder ignores a non‑null Converter argument")
    void testToStringBuilderNonNullConverter() {
        Character[] input = new Character[] { 'p', 'q' };
        // mockConverter is already a non‑null mock created by Mockito
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        assertNotNull(result);
        assertEquals("pq", result.toString(), "Converter argument must not affect the conversion result");
    }
}
