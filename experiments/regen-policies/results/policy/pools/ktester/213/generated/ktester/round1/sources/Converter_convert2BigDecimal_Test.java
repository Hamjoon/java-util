package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2BigDecimal_Test {

    static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// ----- Numeric Types -----
        Arguments.of(123, new BigDecimal("123")), Arguments.of(1234567890123456789L, new BigDecimal("1234567890123456789")), Arguments.of(123.45d, new BigDecimal("123.45")), // scientific‑notation double
        Arguments.of(123400000000.0d, new BigDecimal("123400000000.0")), // float
        Arguments.of(123400000000.0f, new BigDecimal("123400000000.0")), Arguments.of((short) 32767, new BigDecimal("32767")), Arguments.of((byte) 127, new BigDecimal("127")), Arguments.of(new BigInteger("123456789012345678901234567890"), new BigDecimal("123456789012345678901234567890")), Arguments.of(new BigDecimal("9876543210.12345"), // unchanged BigDecimal
        new BigDecimal("9876543210.12345")), // ----- String Conversions -----
        Arguments.of("42", new BigDecimal("42")), Arguments.of("   3.1415   ", new BigDecimal("3.1415")), Arguments.of("-0.001", new BigDecimal("-0.001")), // scientific notation
        Arguments.of("1e+30", new BigDecimal("1E+30")), // ----- Character and Buffer Types -----
        // single‑character string
        Arguments.of("9", new BigDecimal("9")), // Character
        Arguments.of('7', new BigDecimal("7")), // CharBuffer
        Arguments.of(CharBuffer.wrap("56"), new BigDecimal("56")));
    }

    static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// ----- Empty / malformed strings -----
        Arguments.of("", NumberFormatException.class), Arguments.of("not a number", NumberFormatException.class), // ----- ByteBuffer (unsupported) -----
        Arguments.of(ByteBuffer.wrap(new byte[] { 48, 49, 50 }), NumberFormatException.class), // ----- Invalid / Unsupported Types -----
        Arguments.of(Boolean.TRUE, NumberFormatException.class), Arguments.of(Date.from(Instant.parse("2023-01-01T12:00:00Z")), NumberFormatException.class), Arguments.of(Arrays.asList(1, 2, 3), NumberFormatException.class), // ----- Special Floating Point Values -----
        Arguments.of(Double.NaN, IllegalArgumentException.class), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Double.NEGATIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Float.NaN, IllegalArgumentException.class), Arguments.of(Float.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Float.NEGATIVE_INFINITY, IllegalArgumentException.class), // ----- Unsupported Byte Array -----
        Arguments.of(new byte[] { 49, 50, 51 }, NumberFormatException.class), // ----- CharBuffer edge cases -----
        // empty CharBuffer
        Arguments.of(CharBuffer.allocate(0), NumberFormatException.class), Arguments.of(CharBuffer.wrap("abc"), NumberFormatException.class), // ----- Unsupported Atomic Types -----
        Arguments.of(new AtomicInteger(42), NumberFormatException.class), Arguments.of(new AtomicLong(123456789L), NumberFormatException.class), Arguments.of(new AtomicBoolean(true), NumberFormatException.class));
    }

    @Test
    @DisplayName("convert2BigDecimal returns ZERO for null input")
    void testNullInputReturnsZero() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null), "Null should be converted to BigDecimal.ZERO");
    }

    @Test
    @DisplayName("convert2BigDecimal returns the same instance for a BigDecimal")
    void testBigDecimalInputIsReturnedUnchanged() {
        BigDecimal source = new BigDecimal("12345.6789");
        BigDecimal result = Converter.convert2BigDecimal(source);
        assertSame(source, result, "A BigDecimal input should be returned unchanged");
    }

    @Test
    @DisplayName("convert2BigDecimal correctly converts various Number types")
    void testNumberConversions() {
        assertEquals(new BigDecimal("42"), Converter.convert2BigDecimal(42), "Integer should be converted correctly");
        assertEquals(new BigDecimal("42"), Converter.convert2BigDecimal(42L), "Long should be converted correctly");
        assertEquals(new BigDecimal("42.5"), Converter.convert2BigDecimal(42.5f), "Float should be converted correctly");
        assertEquals(new BigDecimal("42.5"), Converter.convert2BigDecimal(42.5d), "Double should be converted correctly");
        assertEquals(new BigDecimal("7"), Converter.convert2BigDecimal((short) 7), "Short should be converted correctly");
        assertEquals(new BigDecimal("9"), Converter.convert2BigDecimal((byte) 9), "Byte should be converted correctly");
    }

    @Test
    @DisplayName("convert2BigDecimal converts numeric String values")
    void testStringConversion() {
        assertEquals(new BigDecimal("12345.6789"), Converter.convert2BigDecimal("12345.6789"), "String containing a valid decimal should be converted");
        assertEquals(new BigDecimal("-9876.54321"), Converter.convert2BigDecimal("-9876.54321"), "Negative numeric String should be converted");
    }

    @Test
    @DisplayName("convert2BigDecimal throws IllegalArgumentException for non‑numeric input")
    void testInvalidConversionThrows() {
        // not a type that Converter knows how to turn into BigDecimal
        Object nonNumeric = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2BigDecimal(nonNumeric), "Converting a non‑numeric, non‑String object should throw an exception");
    }

    @Test
    @DisplayName("convert2BigDecimal handles scientific notation strings")
    void testScientificNotationString() {
        assertEquals(new BigDecimal("1.23E3"), Converter.convert2BigDecimal("1.23E3"), "String in scientific notation should be converted correctly");
    }

    @Test
    @DisplayName("convert2BigDecimal handles whitespace‑padded strings")
    void testWhitespacePaddedString() {
        assertEquals(new BigDecimal("42"), Converter.convert2BigDecimal("   42   "), "String with surrounding whitespace should be trimmed and converted");
    }

    @ParameterizedTest(name = "[{index}] convert2BigDecimal({0}) → {1}")
    @MethodSource("successfulConversionProvider")
    void testSuccessfulConversions(Object input, BigDecimal expected) {
        assertEquals(expected, Converter.convert2BigDecimal(input));
    }

    @ParameterizedTest(name = "[{index}] convert2BigDecimal({0}) throws {1}")
    @MethodSource("exceptionConversionProvider")
    void testExceptionConversions(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convert2BigDecimal(input));
    }
}
