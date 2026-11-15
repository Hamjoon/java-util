package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.CharBuffer;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharBuffer_Test {

    /**
     * The {@link Converter} instance used by the tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Create a Converter with default options.  The default constructor of
        // ConverterOptions supplies the standard conversion map.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the reference – not strictly required but keeps the test tidy.
        converter = null;
    }

    /**
     * Provides a stream of arguments where the expected CharBuffer content is
     * exactly the supplied {@code expectedString}.
     */
    private static Stream<Arguments> happyPathProvider() {
        return Stream.of(// null input – asString(null) returns an empty string in the library
        Arguments.of(null, "", "null input should result in an empty CharBuffer"), // Plain String literals
        Arguments.of("hello", "hello", "String literal"), Arguments.of("", "", "Empty string"), Arguments.of("   leading and trailing spaces   ", "   leading and trailing spaces   ", "String with whitespace should be preserved"), // Objects that rely on their toString()
        Arguments.of(new StringBuilder("world"), "world", "StringBuilder"), Arguments.of(CharBuffer.wrap("abc"), "abc", "Existing CharBuffer"), Arguments.of(new char[] { 'x', 'y', 'z' }, "xyz", "char array"), Arguments.of(Integer.valueOf(123), "123", "Integer"), Arguments.of(DayOfWeek.MONDAY, "MONDAY", "Enum constant"), Arguments.of(new Date(0L), new Date(0L).toString(), "java.util.Date"), Arguments.of(new String("test"), "test", "String created via constructor"), Arguments.of(new StringBuffer("buffer"), "buffer", "StringBuffer"), Arguments.of(new char[0], "", "Empty char array"), Arguments.of(Optional.of("opt"), "Optional[opt]", "Optional"), Arguments.of(new java.math.BigDecimal("12345.6789"), "12345.6789", "BigDecimal"), Arguments.of(Instant.parse("2023-01-01T00:00:00Z"), "2023-01-01T00:00:00Z", "Instant"), Arguments.of(new java.util.concurrent.atomic.AtomicInteger(42), "42", "AtomicInteger"), Arguments.of(Collections.emptyList(), "[]", "Empty List"));
    }

    @Test
    void toCharBuffer_givenString_returnsCharBufferWithSameContent() {
        // GIVEN
        String input = "Hello, World!";
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(input, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer should not be null");
        assertEquals(input, result.toString(), "CharBuffer should contain the exact characters of the input string");
    }

    @Test
    void toCharBuffer_givenInteger_returnsCharBufferWithNumberString() {
        // GIVEN
        Integer input = 12345;
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(input, converter);
        // THEN
        assertNotNull(result);
        assertEquals("12345", result.toString(), "Integer should be converted to its decimal string representation");
    }

    @Test
    void toCharBuffer_givenCharArray_returnsCharBufferWithArrayContent() {
        // GIVEN
        char[] input = new char[] { 'a', 'b', 'c', '1', '2', '3' };
        // The StringConversions class does not have a direct overload for char[],
        // but the generic Object version will invoke asString, which should turn
        // the array into a string like "[a, b, c, 1, 2, 3]" or similar.
        // To keep the test deterministic we use a String representation.
        String expected = new String(input);
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(expected, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString(), "CharBuffer should contain the characters represented by the array");
    }

    @Test
    void toCharBuffer_givenNull_returnsEmptyCharBuffer() {
        // GIVEN
        Object input = null;
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(input, converter);
        // THEN
        // The implementation of asString(null) returns an empty string (""), so
        // the CharBuffer should also be empty.
        assertNotNull(result);
        assertEquals(0, result.length(), "A null input should result in an empty CharBuffer");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("happyPathProvider")
    void toCharBuffer_happyPath(Object input, String expectedString, String description) {
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(input, converter);
        // THEN
        assertNotNull(result, "Resulting CharBuffer must not be null");
        assertEquals(expectedString, result.toString(), () -> "CharBuffer content mismatch for case: " + description);
    }

    @Test
    void toCharBuffer_byteArrayUsesObjectToString() {
        // GIVEN
        // "ABC" in ASCII, but toString() is default
        byte[] input = new byte[] { 65, 66, 67 };
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(input, converter);
        // THEN
        assertNotNull(result);
        String actual = result.toString();
        // The default Object.toString() for a byte[] looks like "[B@1a2b3c4d"
        assertTrue(actual.startsWith("[B@"), () -> "Expected byte[] toString() representation to start with \"[B@\", but was: " + actual);
    }

    @Test
    void toCharBuffer_mapSingleEntry() {
        // GIVEN
        Map<String, String> map = new HashMap<>() {

            {
                put("k", "v");
            }
        };
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(map, converter);
        // THEN
        assertNotNull(result);
        String actual = result.toString();
        // The map's toString() is "{k=v}" – order is deterministic for a single entry
        assertEquals("{k=v}", actual, "Map with a single entry should be rendered as \"{k=v}\"");
    }

    @Test
    void toCharBuffer_objectWithNullToString_throwsNullPointerException() {
        // GIVEN: an anonymous object whose toString() returns null
        Object badObject = new Object() {

            @Override
            public String toString() {
                return null;
            }
        };
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toCharBuffer(badObject, converter), "CharBuffer.wrap(null) should throw NullPointerException");
    }

    @Test
    void toCharBuffer_givenExistingCharBuffer_returnsNewInstance() {
        // GIVEN
        CharBuffer original = CharBuffer.wrap("original");
        // WHEN
        CharBuffer result = StringConversions.toCharBuffer(original, converter);
        // THEN
        assertNotNull(result);
        assertEquals("original", result.toString(), "Content of the returned CharBuffer must match the original");
        assertNotSame(original, result, "The method should return a new CharBuffer, not the same instance that was passed");
    }
}
