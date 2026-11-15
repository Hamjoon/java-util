package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.IntStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toCharArray_Test {

    // to satisfy the method signature and to illustrate typical test setup.
    @Mock
    private Converter mockConverter;

    /* -------------------------------------------------------------
     * Helper holder class for parameterized arguments
     * ------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final char[] from;

        // may be null or mock
        final Converter converter;

        final String description;

        ArgumentsHolder(char[] from, Converter converter, String description) {
            this.from = from;
            this.converter = converter;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Group: Valid char array conversion (non‑empty arrays)
     */
    static Stream<ArgumentsHolder> validCharArrayProvider() {
        return Stream.of(new ArgumentsHolder(new char[] { 'a', 'b', 'c' }, null, "Typical case: a non‑empty char[] is passed."), new ArgumentsHolder(new char[] { 'x' }, null, "Single‑element array – verifies copying works for the smallest non‑empty array."));
    }

    @Test
    void toCharArray_returnsCopyWithSameContent() {
        // given
        char[] original = new char[] { 'a', 'b', 'c' };
        // when
        char[] result = CharArrayConversions.toCharArray(original, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertNotSame(original, result, "Result should be a different array instance");
        assertArrayEquals(original, result, "Result should contain the same characters");
    }

    @Test
    void toCharArray_modifyingOriginalDoesNotAffectResult() {
        // given
        char[] original = new char[] { 'x', 'y', 'z' };
        char[] copy = CharArrayConversions.toCharArray(original, mockConverter);
        // when
        original[0] = 'q';
        // then
        assertEquals('x', copy[0], "Copy should retain original value at index 0");
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, copy, "Copy should remain unchanged after original is mutated");
    }

    @Test
    void toCharArray_withEmptyArrayReturnsEmptyCopy() {
        // given
        char[] empty = new char[0];
        // when
        char[] result = CharArrayConversions.toCharArray(empty, mockConverter);
        // then
        assertNotNull(result, "Result should not be null for empty input");
        assertEquals(0, result.length, "Result length should be zero");
        assertNotSame(empty, result, "Result should be a different instance even when empty");
    }

    @Test
    void toCharArray_invalidSourceThrowsClassCastException() {
        // given
        Object notACharArray = "not a char array";
        // when / then
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toCharArray(notACharArray, mockConverter), "Casting a non‑char[] source should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validCharArrayProvider")
    void toCharArray_validCharArrays(ArgumentsHolder holder) {
        // when
        char[] result = CharArrayConversions.toCharArray(holder.from, holder.converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertNotSame(holder.from, result, "Result must be a new array instance");
        assertArrayEquals(holder.from, result, "Result content must equal the source");
    }

    /**
     * Group: Null input handling
     */
    @Test
    void toCharArray_nullSource_throwsNullPointerException() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toCharArray(nullSource, mockConverter), "Passing null should cause NullPointerException from Arrays.copyOf");
    }

    /**
     * Group: Empty array conversion
     */
    @Test
    void toCharArray_emptyArray_returnsEmptyCopy() {
        // given
        char[] empty = new char[0];
        // when
        char[] result = CharArrayConversions.toCharArray(empty, mockConverter);
        // then
        assertNotNull(result, "Result should not be null for empty input");
        assertEquals(0, result.length, "Result length should be zero");
        assertNotSame(empty, result, "Result must be a different instance even when empty");
    }

    /**
     * Group: Large array conversion
     */
    @Test
    void toCharArray_largeArray_isCopiedCorrectly() {
        // given: 1000 characters cycling through 'a'..'z'
        char[] large = IntStream.range(0, 1000).mapToObj(i -> (char) ('a' + (i % 26))).collect(Collectors.toList()).toArray(new char[0]);
        // when
        char[] result = CharArrayConversions.toCharArray(large, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(1000, result.length);
        assertNotSame(large, result);
        assertArrayEquals(large, result);
    }

    /**
     * Group: Unicode characters
     */
    @Test
    void toCharArray_unicodeCharacters_areCopied() {
        // given: array containing ©, €, and a surrogate pair (😀)
        char[] unicode = new char[] { '\u00A9', '\u20AC', '\uD83D', '\uDE00' };
        // when
        char[] result = CharArrayConversions.toCharArray(unicode, mockConverter);
        // then
        assertArrayEquals(unicode, result);
        assertNotSame(unicode, result);
    }

    /**
     * Group: Copy independence
     */
    @Test
    void toCharArray_copyIndependence_originalMutationDoesNotAffectResult() {
        // given
        char[] original = new char[] { 'p', 'q', 'r' };
        char[] copy = CharArrayConversions.toCharArray(original, mockConverter);
        // when
        original[0] = 'z';
        // then
        assertEquals('p', copy[0], "Returned array should retain original value at index 0");
        assertArrayEquals(new char[] { 'p', 'q', 'r' }, copy, "Returned array must remain unchanged after original is mutated");
    }

    /**
     * Group: Non‑char array type error
     */
    @Test
    void toCharArray_nonCharArray_throwsClassCastException() {
        // given
        Object notCharArray = new int[] { 1, 2, 3 };
        // when / then
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toCharArray(notCharArray, mockConverter), "Casting a non‑char[] source should throw ClassCastException");
    }

    /**
     * Group: Non‑null converter parameter
     */
    @Test
    void toCharArray_nonNullConverter_isIgnored() {
        // given: a concrete (but empty) Converter implementation
        Converter realConverter = new Converter() {
        };
        char[] source = new char[] { 'm', 'n' };
        // when
        char[] result = CharArrayConversions.toCharArray(source, realConverter);
        // then
        assertNotSame(source, result);
        assertArrayEquals(source, result);
    }
}
