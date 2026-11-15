package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharArray_Test {

    @Mock
    private Converter // not used by the method, but required by signature
    converter;

    /**
     * Object whose {@code toString()} returns the literal "abc".
     */
    private static final class ToStringABC {

        @Override
        public String toString() {
            return "abc";
        }
    }

    /**
     * Object whose {@code toString()} returns {@code null}.
     */
    private static final class ToStringNull {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * Object whose {@code toString()} throws an {@link IllegalArgumentException}.
     */
    private static final class ToStringException {

        @Override
        public String toString() {
            throw new IllegalArgumentException("toString failure");
        }
    }

    /**
     * Supplies the normal test cases:
     * <ul>
     *   <li>empty string</li>
     *   <li>whitespace‑only string (treated as empty)</li>
     *   <li>object whose {@code toString()} returns "abc"</li>
     *   <li>numeric object (Integer)</li>
     * </ul>
     */
    private static Stream<Arguments> provideNormalCases() {
        return Stream.of(// Empty string → EMPTY_CHAR_ARRAY
        Arguments.of("", EMPTY_CHAR_ARRAY), // Whitespace only → EMPTY_CHAR_ARRAY (StringUtilities.isEmpty treats it as empty)
        Arguments.of("   ", EMPTY_CHAR_ARRAY), // Custom object with toString() returning "abc"
        Arguments.of(new ToStringABC(), new char[] { 'a', 'b', 'c' }), // Numeric object (Integer) – toString() yields "12345"
        Arguments.of(12345, new char[] { '1', '2', '3', '4', '5' }));
    }

    @Test
    void toCharArray_WithEmptyString_ReturnsEmptyCharArray() {
        // given
        Object from = "";
        // when
        char[] result = StringConversions.toCharArray(from, converter);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.length, "Empty string should produce an empty char array");
    }

    @Test
    void toCharArray_WithNonEmptyString_ReturnsCorrespondingCharArray() {
        // given
        String text = "Hello, World!";
        Object from = text;
        // when
        char[] result = StringConversions.toCharArray(from, converter);
        // then
        assertArrayEquals(text.toCharArray(), result, "The returned char array should match the characters of the source string");
    }

    @Test
    void toCharArray_WithNullInput_ThrowsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toCharArray(from, converter), "Passing a null source should result in a NullPointerException");
    }

    /**
     * Verifies that various inputs produce the expected character array.
     *
     * @param from     the source object passed to {@code toCharArray}
     * @param expected the expected result (empty array or specific characters)
     */
    @ParameterizedTest(name = "{index}: from={0} → expected length={1.length}")
    @MethodSource("provideNormalCases")
    void toCharArray_NormalCases(Object from, char[] expected) {
        // when
        char[] result = StringConversions.toCharArray(from, converter);
        // then
        assertNotNull(result, "Result must never be null");
        assertArrayEquals(expected, result, () -> "Unexpected char array for input: " + (from == null ? "null" : from));
    }

    @Test
    void toCharArray_WithToStringReturningNull_ReturnsEmptyCharArray() {
        // given
        Object from = new ToStringNull();
        // when
        char[] result = StringConversions.toCharArray(from, converter);
        // then
        assertNotNull(result, "Result must never be null");
        assertArrayEquals(EMPTY_CHAR_ARRAY, result, "When toString() returns null the method should treat it as empty");
    }

    @Test
    void toCharArray_WhenToStringThrows_ExceptionPropagates() {
        // given
        Object from = new ToStringException();
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toCharArray(from, converter), "Exception from toString() must propagate unchanged");
        assertEquals("toString failure", ex.getMessage(), "Exception message should be preserved");
    }
}
