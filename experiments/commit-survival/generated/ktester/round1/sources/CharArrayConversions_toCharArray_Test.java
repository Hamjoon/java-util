package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharArrayConversions_toCharArray_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Supplies test data for {@link #toCharArray_copiesArrayCorrectly(char[], char[])}.
     *
     * <p>Each {@code Arguments} entry contains:
     * <ul>
     *   <li>the source {@code char[]} passed to the method</li>
     *   <li>the expected {@code char[]} that should be returned (a copy of the source)</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> normalBehaviourProvider() {
        return Stream.of(// Typical case: non‑empty array with basic ASCII characters
        org.junit.jupiter.params.provider.Arguments.of(new char[] { 'a', 'b', 'c' }, new char[] { 'a', 'b', 'c' }), // Edge case: empty array
        org.junit.jupiter.params.provider.Arguments.of(new char[] {}, new char[] {}), // Unicode characters, including a surrogate pair (😀)
        org.junit.jupiter.params.provider.Arguments.of(new char[] { '\u00E9', '\u2603', '\uD83D', '\uDE00' }, new char[] { '\u00E9', '\u2603', '\uD83D', '\uDE00' }));
    }

    @Test
    void toCharArray_returnsCopyOfOriginalArray() {
        // GIVEN
        char[] original = new char[] { 'a', 'b', 'c' };
        // WHEN
        char[] result = CharArrayConversions.toCharArray(original, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertArrayEquals(original, result, "Result array should contain the same elements");
        assertNotSame(original, result, "Result should be a different instance (a copy)");
    }

    @Test
    void toCharArray_modifyingOriginalDoesNotAffectResult() {
        // GIVEN
        char[] original = new char[] { 'x', 'y', 'z' };
        char[] result = CharArrayConversions.toCharArray(original, mockConverter);
        // WHEN
        original[0] = 'q';
        // THEN
        assertEquals('x', result[0], "Result array must remain unchanged after original is mutated");
    }

    @Test
    void toCharArray_invalidCastThrowsClassCastException() {
        // GIVEN
        Object notACharArray = "not a char array";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toCharArray(notACharArray, mockConverter), "Casting a non‑char[] object should throw ClassCastException");
    }

    @Test
    void toCharArray_nullInputThrowsNullPointerException() {
        // GIVEN
        Object nullInput = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharArrayConversions.toCharArray(nullInput, mockConverter), "Passing null should result in NullPointerException due to Arrays.copyOf");
    }

    /**
     * Verify that a non‑empty, empty or Unicode {@code char[]} is copied correctly.
     *
     * @param source   the original array supplied to the method
     * @param expected the expected content of the returned copy
     */
    @ParameterizedTest(name = "[{index}] source={0}, expected={1}")
    @MethodSource("normalBehaviourProvider")
    void toCharArray_copiesArrayCorrectly(char[] source, char[] expected) {
        // WHEN
        char[] result = CharArrayConversions.toCharArray(source, mockConverter);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertArrayEquals(expected, result, "Returned array must contain the same characters");
        assertNotSame(source, result, "Returned array must be a distinct copy");
    }

    @Test
    void toCharArray_immutabilityCheck_modifyingSourceDoesNotAffectResult() {
        // GIVEN
        char[] original = new char[] { 'x', 'y', 'z' };
        char[] result = CharArrayConversions.toCharArray(original, mockConverter);
        // WHEN – mutate the original after conversion
        original[0] = 'q';
        // THEN – the previously returned copy must stay unchanged
        assertEquals('x', result[0], "Result array must remain unchanged after the original array is mutated");
    }

    @Test
    void toCharArray_invalidCastThrowsClassCastException_stringInput() {
        // GIVEN
        Object notACharArray = "some string";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toCharArray(notACharArray, mockConverter), "Casting a non‑char[] (String) should throw ClassCastException");
    }

    @Test
    void toCharArray_invalidCastThrowsClassCastException_intArrayInput() {
        // GIVEN
        Object notACharArray = new int[] { 1, 2, 3 };
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toCharArray(notACharArray, mockConverter), "Casting a non‑char[] (int[]) should throw ClassCastException");
    }

    @Test
    void toCharArray_invalidCastThrowsClassCastException_genericObject() {
        // GIVEN
        Object notACharArray = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharArrayConversions.toCharArray(notACharArray, mockConverter), "Casting a non‑char[] (Object) should throw ClassCastException");
    }
}
