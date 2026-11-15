package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ByteBufferConversions_toCharArray_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared real Converter used for the majority of test cases.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a no‑arg constructor that provides default settings.
        converter = new Converter(new ConverterOptions());
        // The default options are sufficient for the built‑in conversions.
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the pattern tidy.
        converter = null;
        realConverter = null;
    }

    /**
     * Helper that converts a {@code char[]} to a readable {@code String} for assertion messages.
     */
    private static String asString(char[] chars) {
        return chars == null ? "null" : new String(chars);
    }

    /**
     * Parses the JSON‑style expected value from the test description
     *  (e.g. {@code ["a","b","c"]}) into a {@code char[]} .
     */
    private static char[] parseExpected(String json) {
        if ("null".equals(json)) {
            return null;
        }
        // Remove the surrounding brackets and quotes, then split on commas.
        String trimmed = json.replaceAll("[\\[\\]\"]", "").trim();
        if (trimmed.isEmpty()) {
            return new char[0];
        }
        String[] parts = trimmed.split("\\s*,\\s*");
        char[] result = new char[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parts[i].charAt(0);
        }
        return result;
    }

    /**
     * Supplies the test data for the parameterized tests.
     *  Each element is an {@code Object[]} containing:
     *  <ul>
     *      <li>the source object (the {@code from} argument)</li>
     *      <li>the {@link Converter} to use (may be {@code null})</li>
     *      <li>the expected result expressed as a JSON string (or an exception class name)</li>
     *      <li>a short description (used only for readability)</li>
     *  </ul>
     */
    private static Stream<Arguments> testCases() {
        return Stream.of(// -----------------------------------------------------------------
        // Unsupported Type with Converter Fallback
        // -----------------------------------------------------------------
        Arguments.of(new java.util.Date(0L), MockConverterThatCallsToString.INSTANCE, "[\"J\",\"a\",\"n\",\" \",\"1\",\"\",\" \",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\"0\",\" \",\"G\",\"M\",\"T\"]", "Date fallback to toString()"), // -----------------------------------------------------------------
        // String Input Direct Conversion
        // -----------------------------------------------------------------
        Arguments.of("hello", MockConverterThatCallsToString.INSTANCE, "[\"h\",\"e\",\"l\",\"l\",\"o\"]", "String should be converted without using the converter"), // -----------------------------------------------------------------
        // CharArray Input Direct Conversion
        // -----------------------------------------------------------------
        Arguments.of(new char[] { 'a', 'b', 'c' }, MockConverterThatCallsToString.INSTANCE, "[\"a\",\"b\",\"c\"]", "char[] should be returned as‑is (or a copy)"), // -----------------------------------------------------------------
        // CharBuffer Input Direct Conversion
        // -----------------------------------------------------------------
        Arguments.of(CharBuffer.wrap("world"), MockConverterThatCallsToString.INSTANCE, "[\"w\",\"o\",\"r\",\"l\",\"d\"]", "CharBuffer should be turned into a char[]"), // -----------------------------------------------------------------
        // ByteBuffer Input UTF‑8 Conversion
        // -----------------------------------------------------------------
        Arguments.of(ByteBuffer.wrap(new byte[] { 65, 66, 67 }), MockConverterThatCallsToString.INSTANCE, "[\"A\",\"B\",\"C\"]", "ASCII bytes → chars"), // -----------------------------------------------------------------
        // ByteArray Input UTF‑8 Conversion
        // -----------------------------------------------------------------
        Arguments.of(new byte[] { 68, 69, 70 }, MockConverterThatCallsToString.INSTANCE, "[\"D\",\"E\",\"F\"]", "byte[] → chars"), // -----------------------------------------------------------------
        // Empty String Input
        // -----------------------------------------------------------------
        Arguments.of("", MockConverterThatCallsToString.INSTANCE, "[]", "empty string → empty char[]"), // -----------------------------------------------------------------
        // Empty ByteArray Input
        // -----------------------------------------------------------------
        Arguments.of(new byte[] {}, MockConverterThatCallsToString.INSTANCE, "[]", "empty byte[] → empty char[]"), // -----------------------------------------------------------------
        // Null Source Input
        // -----------------------------------------------------------------
        Arguments.of(null, MockConverterThatCallsToString.INSTANCE, "null", "null source → null result"), // -----------------------------------------------------------------
        // Null Converter Argument
        // -----------------------------------------------------------------
        Arguments.of("test", null, "java.lang.NullPointerException", "null converter should cause NPE"), // -----------------------------------------------------------------
        // Unsupported Type with Converter Fallback (different object)
        // -----------------------------------------------------------------
        Arguments.of(new java.math.BigDecimal("123.45"), MockConverterThatCallsToString.INSTANCE, "[\"1\",\"2\",\"3\",\".\",\"4\",\"5\"]", "BigDecimal fallback to toString()"), // -----------------------------------------------------------------
        // Malformed UTF‑8 ByteArray Input
        // -----------------------------------------------------------------
        Arguments.of(new byte[] { (byte) 0xC3 }, MockConverterThatCallsToString.INSTANCE, "java.nio.charset.CharacterCodingException", "incomplete UTF‑8 sequence should propagate exception"), // -----------------------------------------------------------------
        // Converter Throws RuntimeException
        // -----------------------------------------------------------------
        Arguments.of("trigger", MockConverterThatThrows.INSTANCE, "java.lang.IllegalStateException", "converter throwing unchecked exception should be propagated"), // -----------------------------------------------------------------
        // Converter Returns Null CharBuffer
        // -----------------------------------------------------------------
        Arguments.of("any", MockConverterReturningNull.INSTANCE, "java.lang.NullPointerException", "null CharBuffer from converter should cause NPE"));
    }

    @Test
    void toCharArray_fromByteBuffer_returnsExpectedChars() {
        // GIVEN a ByteBuffer containing UTF‑8 bytes for "hello"
        ByteBuffer bb = ByteBuffer.wrap("hello".getBytes());
        // WHEN converting to char[]
        char[] result = ByteBufferConversions.toCharArray(bb, converter);
        // THEN the result matches the original characters
        assertNotNull(result, "Result should not be null");
        assertArrayEquals("hello".toCharArray(), result, () -> "Expected char[] for \"hello\", got: " + asString(result));
    }

    @Test
    void toCharArray_fromByteArray_returnsExpectedChars() {
        // GIVEN a byte[] containing UTF‑8 bytes for "world"
        byte[] bytes = "world".getBytes();
        // WHEN converting to char[]
        char[] result = ByteBufferConversions.toCharArray(bytes, converter);
        // THEN the result matches the original characters
        assertNotNull(result);
        assertArrayEquals("world".toCharArray(), result);
    }

    @Test
    void toCharArray_fromCharBuffer_returnsSameChars() {
        // GIVEN a CharBuffer with the characters "buffer"
        CharBuffer cb = CharBuffer.wrap("buffer".toCharArray());
        // WHEN converting to char[]
        char[] result = ByteBufferConversions.toCharArray(cb, converter);
        // THEN the result is identical to the source buffer's content
        assertNotNull(result);
        assertArrayEquals("buffer".toCharArray(), result);
    }

    @Test
    void toCharArray_fromString_returnsExpectedChars() {
        // GIVEN a plain String (Converter knows how to handle String → CharBuffer)
        String source = "string";
        // WHEN converting to char[]
        char[] result = ByteBufferConversions.toCharArray(source, converter);
        // THEN the characters are correctly transferred
        assertNotNull(result);
        assertArrayEquals(source.toCharArray(), result);
    }

    @Test
    void toCharArray_fromNull_returnsNull() {
        // GIVEN a null source
        Object source = null;
        // WHEN converting to char[]
        char[] result = ByteBufferConversions.toCharArray(source, converter);
        // THEN the conversion result should be null (consistent with Converter's null handling)
        assertNull(result, "Conversion of null should produce null");
    }

    @Test
    void toCharArray_fromEmptyByteArray_returnsEmptyCharArray() {
        // GIVEN an empty byte array
        byte[] empty = new byte[0];
        // WHEN converting to char[]
        char[] result = ByteBufferConversions.toCharArray(empty, converter);
        // THEN we get an empty char array (not null)
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    void toCharArray_fromEmptyByteBuffer_returnsEmptyCharArray() {
        // GIVEN an empty ByteBuffer
        ByteBuffer empty = ByteBuffer.allocate(0);
        // WHEN converting to char[]
        char[] result = ByteBufferConversions.toCharArray(empty, converter);
        // THEN we get an empty char array
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("testCases")
    void toCharArray_variousInputs(Object from, Converter converter, String expectedJsonOrException, String description) {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        Converter usedConverter = (converter != null) ? converter : realConverter;
        // -----------------------------------------------------------------
        // Act & Assert
        // -----------------------------------------------------------------
        if (expectedJsonOrException.startsWith("java.")) {
            // Expected an exception
            Class<? extends Throwable> expectedType = (Class<? extends Throwable>) assertDoesNotThrow(() -> Class.forName(expectedJsonOrException));
            Throwable thrown = assertThrows(expectedType, () -> ByteBufferConversions.toCharArray(from, usedConverter), () -> "Expected exception " + expectedType.getName());
            // No further assertions – the type is enough.
        } else {
            // Expected a char[] result
            char[] result = ByteBufferConversions.toCharArray(from, usedConverter);
            char[] expected = parseExpected(expectedJsonOrException);
            if (expected == null) {
                assertNull(result, "Result should be null for input: " + description);
            } else {
                assertNotNull(result, "Result should not be null for input: " + description);
                assertArrayEquals(expected, result, () -> "Mismatch for " + description + ". Expected: " + asString(expected) + " Got: " + asString(result));
            }
        }
    }
}
