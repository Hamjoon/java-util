package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterArrayConversions_toStringBuilder_Test {

    /**
     * A mock Converter – the method under test does not invoke any Converter behaviour.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple stub implementation of {@link Converter}. The conversion logic is irrelevant for the tests.
     */
    private static class DummyConverter implements Converter {
        // No methods are required for the stub; the interface is empty in the current context.
    }

    /**
     * Container for parameterised normal‑conversion test data.
     */
    private static class NormalCase {

        final Character[] input;

        final String expected;

        final String description;

        NormalCase(Character[] input, String expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            // JUnit uses this for the test name when a custom display name is not supplied.
            return description;
        }
    }

    /* --------------------------------------------------------------
     *  Parameterised normal‑conversion tests
     * -------------------------------------------------------------- */
    private static Stream<NormalCase> normalConversionProvider() {
        return Stream.of(// empty array
        new NormalCase(new Character[] {}, "", "When the input array has length 0 the method should return an empty StringBuilder without throwing any exception."), // single element
        new NormalCase(new Character[] { 'A' }, "A", "A one‑element Character array should produce a StringBuilder containing exactly that character."), // typical multiple elements
        new NormalCase(new Character[] { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' }, "Hello World", "A typical use‑case: the method must concatenate all characters in order and return the correct StringBuilder."), // whitespace, tab and Unicode snowman
        new NormalCase(new Character[] { '\n', '\t', '\u2603' }, "\n\t\u2603", "Array containing whitespace, tab and a Unicode character should be handled correctly."), // boundary characters NUL and max char
        new NormalCase(new Character[] { '\u0000', '\uFFFF' }, "\u0000\uFFFF", "Verify that the method correctly appends the lowest and highest possible char values without alteration."), // surrogate pair forming an emoji (😀)
        new NormalCase(new Character[] { '\uD83D', '\uDE00' }, "\uD83D\uDE00", "Ensure that the method correctly appends high and low surrogate characters, resulting in a valid Unicode emoji."), // space and carriage return
        new NormalCase(new Character[] { ' ', '\r' }, " \r", "Validate handling of space and carriage‑return characters."));
    }

    @Test
    void toStringBuilder_returnsBuilderWithAllCharacters() {
        // given
        Character[] input = new Character[] { 'J', 'a', 'v', 'a' };
        // when
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals(4, result.length(), "Builder length should match the input array length");
        assertEquals("Java", result.toString(), "Builder content should be the concatenation of the characters");
    }

    @Test
    void toStringBuilder_handlesEmptyArray() {
        // given
        Character[] input = new Character[0];
        // when
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        // then
        assertNotNull(result, "Resulting StringBuilder should not be null even for empty input");
        assertEquals(0, result.length(), "Builder length should be zero for an empty array");
        assertEquals("", result.toString(), "Builder content should be empty string for an empty array");
    }

    @Test
    void toStringBuilder_throwsClassCastExceptionWhenInputIsNotCharacterArray() {
        // given
        Object notACharArray = new int[] { 1, 2, 3 };
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toStringBuilder(notACharArray, mockConverter), "Method should throw ClassCastException when the input is not a Character[]");
    }

    @Test
    void toStringBuilder_throwsNullPointerExceptionWhenInputIsNull() {
        // given
        Object nullInput = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuilder(nullInput, mockConverter), "Casting a null reference to Character[] should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalConversionProvider")
    void toStringBuilder_normalConversions(NormalCase tc) {
        // given
        Character[] input = tc.input;
        // when
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        // then
        assertNotNull(result, "Resulting StringBuilder must not be null");
        assertEquals(tc.expected, result.toString(), "Builder content does not match expected string");
        assertEquals(tc.expected.length(), result.length(), "Builder length should equal the number of characters");
    }

    /* --------------------------------------------------------------
     *  Converter argument ignored
     * -------------------------------------------------------------- */
    @Test
    void toStringBuilder_converterArgumentIsIgnored() {
        // given
        Character[] input = new Character[] { 'X', 'Y' };
        // a simple stub implementation
        Converter dummy = new DummyConverter();
        // when
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, dummy);
        // then
        assertEquals("XY", result.toString(), "Converter argument must not influence the conversion result");
    }

    @Test
    void toStringBuilder_throwsClassCastExceptionWhenInputIsPrimitiveCharArray() {
        // given
        Object primitiveCharArray = new char[] { 'a', 'b' };
        // when / then
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toStringBuilder(primitiveCharArray, mockConverter), "Passing a primitive char[] must result in ClassCastException");
    }

    @Test
    void toStringBuilder_throwsNullPointerExceptionWhenArrayContainsNullElement() {
        // given
        Character[] input = new Character[] { 'A', null, 'B' };
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuilder(input, mockConverter), "Unboxing a null Character while iterating should cause NullPointerException");
    }

    @Test
    void toStringBuilder_throwsNullPointerExceptionWhenFirstElementIsNull() {
        // given
        Character[] input = new Character[] { null, 'A' };
        // when / then
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuilder(input, mockConverter), "Null at the first position must trigger NullPointerException immediately");
    }

    /* --------------------------------------------------------------
     *  Large array stress test
     * -------------------------------------------------------------- */
    @Test
    void toStringBuilder_largeArrayOfIdenticalCharacters() {
        // given
        int size = 1000;
        Character[] input = new Character[size];
        for (int i = 0; i < size; i++) {
            input[i] = 'X';
        }
        // when
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        // then
        assertEquals(size, result.length(), "Builder length should match the size of the large input array");
        assertTrue(result.chars().allMatch(ch -> ch == 'X'), "All characters in the builder must be 'X'");
    }

    /* --------------------------------------------------------------
     *  Distinct builder instances on repeated calls
     * -------------------------------------------------------------- */
    @Test
    void toStringBuilder_returnsDistinctInstancesOnRepeatedCalls() {
        // given
        Character[] input = new Character[] { 'A', 'B', 'C' };
        // when
        StringBuilder first = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        StringBuilder second = CharacterArrayConversions.toStringBuilder(input, mockConverter);
        // then
        assertNotSame(first, second, "Each invocation must return a new StringBuilder instance");
        assertEquals("ABC", first.toString());
        assertEquals("ABC", second.toString());
    }

    /* --------------------------------------------------------------
     *  Source array immutability
     * -------------------------------------------------------------- */
    @Test
    void toStringBuilder_doesNotModifySourceArray() {
        // given
        Character[] original = new Character[] { 'J', 'a', 'v', 'a' };
        Character[] copy = original.clone();
        // when
        CharacterArrayConversions.toStringBuilder(original, mockConverter);
        // then
        assertArrayEquals(copy, original, "The source Character[] must remain unchanged after conversion");
    }
}
