package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.ArrayUtilities;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class StringConversions_toCharArray_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter instance is not used by toCharArray, but we provide a real one
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies the test data for {@link #toCharArray_nonEmptyString(Object, char[])}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> nonEmptyStringProvider() {
        return Stream.of(// Regular String
        org.junit.jupiter.params.provider.Arguments.of("Hello, World!", new char[] { 'H', 'e', 'l', 'l', 'o', ',', ' ', 'W', 'o', 'r', 'l', 'd', '!' }), // StringBuilder – its toString() returns a non‑empty String
        org.junit.jupiter.params.provider.Arguments.of(new StringBuilder("abc"), new char[] { 'a', 'b', 'c' }), // Anonymous object with custom toString()
        org.junit.jupiter.params.provider.Arguments.of(new Object() {

            @Override
            public String toString() {
                return "xyz";
            }
        }, new char[] { 'x', 'y', 'z' }));
    }

    @Test
    void toCharArray_nonEmptyString_returnsCharArray() {
        // given
        String source = "hello";
        // when
        char[] result = StringConversions.toCharArray(source, converter);
        // then
        assertNotNull(result);
        assertArrayEquals(new char[] { 'h', 'e', 'l', 'l', 'o' }, result);
    }

    @Test
    void toCharArray_emptyString_returnsEmptyCharArray() {
        // given
        String source = "";
        // when
        char[] result = StringConversions.toCharArray(source, converter);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.length, "Result length should be zero for empty input");
        // The implementation returns the constant EMPTY_CHAR_ARRAY; verify identity
        assertSame(ArrayUtilities.EMPTY_CHAR_ARRAY, result, "Empty input should return the shared EMPTY_CHAR_ARRAY instance");
    }

    @Test
    void toCharArray_objectWithCustomToString_returnsCharArray() {
        // given
        Object source = new Object() {

            @Override
            public String toString() {
                return "abc";
            }
        };
        // when
        char[] result = StringConversions.toCharArray(source, converter);
        // then
        assertNotNull(result);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    void toCharArray_nullSource_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toCharArray(source, null), "Passing null as 'from' should throw NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the "toCharArray_nonEmptyString" group
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("nonEmptyStringProvider")
    void toCharArray_nonEmptyString(Object from, char[] expected) {
        // The JSON test data specifies that the Converter argument may be null.
        // Passing null is safe because the implementation never touches it.
        char[] result = StringConversions.toCharArray(from, null);
        assertNotNull(result, "Result should never be null for non‑empty input");
        assertArrayEquals(expected, result, () -> "Unexpected char array for input: " + from);
    }
}
