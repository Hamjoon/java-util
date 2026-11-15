package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2float_Test {

    private static Stream<Arguments> numericProvider() {
        return Stream.of(Arguments.of("Integer 42", 42, 42.0f), Arguments.of("Long 1234567890123L", 1234567890123L, 1.2345679E12f), Arguments.of("Double 3.1415926535", 3.1415926535, 3.1415927f), Arguments.of("BigDecimal 12345.6789", new BigDecimal("12345.6789"), 12345.679f), Arguments.of("Short value (as int literal) 7", (short) 7, 7.0f), Arguments.of("Byte value (as int literal) -5", (byte) -5, -5.0f), Arguments.of("Float 2.5f", 2.5f, 2.5f));
    }

    private static Stream<Arguments> stringProvider() {
        return Stream.of(// valid numeric strings
        Arguments.of("0", "0", 0.0f), Arguments.of(" -12.34 ", " -12.34 ", -12.34f), Arguments.of("1e3", "1e3", 1000.0f), // special float literals
        Arguments.of("NaN", "NaN", Float.NaN), Arguments.of("Infinity", "Infinity", Float.POSITIVE_INFINITY), Arguments.of("-Infinity", "-Infinity", Float.NEGATIVE_INFINITY), // error cases
        Arguments.of("non‑numeric \"abc\"", "abc", NumberFormatException.class), Arguments.of("empty string", "", NumberFormatException.class), Arguments.of("whitespace only", "   ", NumberFormatException.class), Arguments.of("literal \"null\"", "null", NumberFormatException.class));
    }

    private static Stream<Arguments> characterProvider() {
        return Stream.of(// Unicode 65
        Arguments.of('A', 65.0f), // Null character
        Arguments.of('\0', 0.0f));
    }

    private static Stream<Arguments> bufferProvider() {
        return Stream.of(Arguments.of("CharBuffer with numeric string", CharBuffer.wrap("123.45"), 123.45f), Arguments.of("ByteBuffer with numeric bytes (\"123\")", ByteBuffer.wrap(new byte[] { 49, 50, 51 }), NumberFormatException.class), Arguments.of("CharBuffer with non‑numeric content", CharBuffer.wrap("abc"), NumberFormatException.class), Arguments.of("ByteBuffer with non‑numeric content (\"abc\")", ByteBuffer.wrap(new byte[] { 97, 98, 99 }), NumberFormatException.class));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(Arguments.of("Float.MAX_VALUE (as double literal)", 3.4028235e+38, 3.4028235E38f), Arguments.of("Float.MIN_VALUE (subnormal)", 1.4e-45, 1.4E-45f), Arguments.of("Double.MAX_VALUE exceeds float range", 1.7976931348623157e+308, Float.POSITIVE_INFINITY));
    }

    private static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of("java.util.Date instance", new Date(), IllegalArgumentException.class), Arguments.of("java.util.UUID instance", UUID.randomUUID(), IllegalArgumentException.class), Arguments.of("int array", new int[] { 1, 2, 3 }, IllegalArgumentException.class), Arguments.of("generic Object", new Object(), IllegalArgumentException.class));
    }

    /**
     * {@code null} should be converted to {@code 0.0f}.
     */
    @Test
    void convertNullReturnsZero() {
        float result = Converter.convert2float(null);
        assertEquals(0.0f, result, "Null input must yield 0.0f");
    }

    /**
     * Integer values should be converted to the same numeric value as a {@code float}.
     */
    @Test
    void convertInteger() {
        Integer source = 42;
        float expected = 42.0f;
        float actual = Converter.convert2float(source);
        assertEquals(expected, actual, "Integer conversion to float failed");
    }

    /**
     * String representations of numbers should be parsed to a {@code float}.
     */
    @Test
    void convertString() {
        String source = "3.14159";
        float expected = 3.14159f;
        float actual = Converter.convert2float(source);
        assertEquals(expected, actual, 0.00001f, "String conversion to float failed");
    }

    /**
     * Double values should be converted preserving the numeric value.
     */
    @Test
    void convertDouble() {
        Double source = 2.71828;
        float expected = 2.71828f;
        float actual = Converter.convert2float(source);
        assertEquals(expected, actual, 0.00001f, "Double conversion to float failed");
    }

    /**
     * {@link BigDecimal} values should be converted to {@code float} correctly.
     */
    @Test
    void convertBigDecimal() {
        BigDecimal source = new BigDecimal("7.1");
        float expected = 7.1f;
        float actual = Converter.convert2float(source);
        assertEquals(expected, actual, 0.00001f, "BigDecimal conversion to float failed");
    }

    /**
     * Boolean {@code true} should be converted to {@code 1.0f} and {@code false} to {@code 0.0f}.
     */
    @Test
    void convertBoolean() {
        assertEquals(1.0f, Converter.convert2float(Boolean.TRUE), "Boolean true should become 1.0f");
        assertEquals(0.0f, Converter.convert2float(Boolean.FALSE), "Boolean false should become 0.0f");
    }

    /**
     * Character numeric values should be converted to their Unicode code point as a float.
     */
    @Test
    void convertCharacter() {
        // Unicode 65
        char source = 'A';
        float expected = 65.0f;
        float actual = Converter.convert2float(source);
        assertEquals(expected, actual, "Character conversion to float failed");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericProvider")
    void convertNumericTypes(String description, Object input, float expected) {
        float actual = Converter.convert2float(input);
        assertEquals(expected, actual, 0.000001f, () -> "Numeric conversion failed for: " + description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: \"{0}\"")
    @MethodSource("stringProvider")
    void convertStringRepresentations(String source, Object input, Object expected) {
        if (expected instanceof Class) {
            // Expected exception
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convert2float(input), "Expected exception for input: \"" + source + "\"");
        } else if (expected instanceof Float && Float.isNaN((Float) expected)) {
            float actual = Converter.convert2float(input);
            assertTrue(Float.isNaN(actual), "Expected NaN for input: \"" + source + "\"");
        } else if (expected instanceof Float && Float.isInfinite((Float) expected)) {
            float actual = Converter.convert2float(input);
            assertEquals(expected, actual, "Expected " + expected + " for input: \"" + source + "\"");
        } else {
            float actual = Converter.convert2float(input);
            assertEquals((Float) expected, actual, 0.000001f, "String conversion failed for input: \"" + source + "\"");
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bufferProvider")
    void convertBufferTypes(String description, Object input, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convert2float(input), "Expected exception for buffer case: " + description);
        } else {
            float actual = Converter.convert2float(input);
            assertEquals((Float) expected, actual, 0.000001f, "Buffer conversion failed for: " + description);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeCaseProvider")
    void convertEdgeCases(String description, Object input, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convert2float(input), "Expected exception for edge case: " + description);
        } else if (expected instanceof Float && Float.isInfinite((Float) expected)) {
            float actual = Converter.convert2float(input);
            assertEquals(expected, actual, "Edge case conversion mismatch for: " + description);
        } else {
            float actual = Converter.convert2float(input);
            assertEquals((Float) expected, actual, 0.000001f, "Edge case conversion mismatch for: " + description);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedProvider")
    void convertUnsupportedTypes(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convert2float(input), "Unsupported conversion should throw for: " + description);
    }
}
