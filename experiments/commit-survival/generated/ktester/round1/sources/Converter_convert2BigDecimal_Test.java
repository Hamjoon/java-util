package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2BigDecimal_Test {

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Primitive / wrapper types
        Arguments.of("Integer → BigDecimal", 123, new BigDecimal("123")), Arguments.of("Long → BigDecimal", 9_223_372_036_854_775_807L, new BigDecimal("9223372036854775807")), Arguments.of("Short → BigDecimal", (short) 12, new BigDecimal("12")), Arguments.of("Byte (negative) → BigDecimal", (byte) -8, new BigDecimal("-8")), Arguments.of("Float → BigDecimal", 3.14f, new BigDecimal(Float.toString(3.14f))), Arguments.of("Double → BigDecimal", 45.67, new BigDecimal(Double.toString(45.67))), // Numeric strings
        Arguments.of("Numeric String \"89.01\"", "89.01", new BigDecimal("89.01")), Arguments.of("String with leading plus \"+100.5\"", "+100.5", new BigDecimal("100.5")), Arguments.of("Scientific notation \"1.23e4\"", "1.23e4", new BigDecimal("12300")), Arguments.of("Leading zeros \"0000123.4500\"", "0000123.4500", new BigDecimal("0000123.4500")), // Negative numbers
        Arguments.of("Negative Integer -99", -99, new BigDecimal("-99")), Arguments.of("Negative Double -0.001", -0.001, new BigDecimal("-0.001")), Arguments.of("Numeric String with minus \"-45.67\"", "-45.67", new BigDecimal("-45.67")), // BigDecimal and BigInteger
        Arguments.of("Existing BigDecimal \"987.654\"", new BigDecimal("987.654"), new BigDecimal("987.654")), Arguments.of("BigInteger → BigDecimal", new BigInteger("12345678901234567890"), new BigDecimal("12345678901234567890")), Arguments.of("Very large BigInteger", new BigInteger("999999999999999999999999999999"), new BigDecimal("999999999999999999999999999999")), Arguments.of("BigDecimal with many fractional digits", new BigDecimal("0.12345678901234567890"), new BigDecimal("0.12345678901234567890")), // Atomic numeric types
        Arguments.of("AtomicInteger 42", new AtomicInteger(42), new BigDecimal("42")), Arguments.of("AtomicLong 1234567890123", new AtomicLong(1_234_567_890_123L), new BigDecimal("1234567890123")));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Unsupported primitive / wrapper types
        Arguments.of("Boolean true", Boolean.TRUE), Arguments.of("Character 'A'", 'A'), // Date / time types
        Arguments.of("java.util.Date instance", new Date()), Arguments.of("java.time.LocalDate instance", java.time.LocalDate.now()), Arguments.of("java.time.LocalDateTime instance", java.time.LocalDateTime.now()), Arguments.of("java.time.ZonedDateTime instance", java.time.ZonedDateTime.now()), // Enum
        Arguments.of("Enum constant (DayOfWeek.MONDAY)", java.time.DayOfWeek.MONDAY), // Collections / arrays
        Arguments.of("List of integers", List.of(1, 2, 3)), Arguments.of("Map<String,Integer>", Map.of("a", 1, "b", 2)), Arguments.of("int[] array", new int[] { 1, 2, 3 }), // Atomic types that are not numeric
        Arguments.of("AtomicBoolean true", new AtomicBoolean(true)), // Non‑numeric strings
        Arguments.of("Empty string (already covered by separate test, kept for completeness)", ""), Arguments.of("Whitespace only string \"   \"", "   "), Arguments.of("Non‑numeric string \"not-a-number\"", "not-a-number"), Arguments.of("String with letters \"12ab34\"", "12ab34"), Arguments.of("Multiple sign characters \"++10\"", "++10"), Arguments.of("Grouping commas \"1,000\"", "1,000"), Arguments.of("Underscore separator \"1_000\"", "1_000"), Arguments.of("String with surrounding whitespace \" 123 \"", " 123 "), Arguments.of("String \"NaN\"", "NaN"), Arguments.of("String \"Infinity\"", "Infinity"), Arguments.of("String \"-Infinity\"", "-Infinity"));
    }

    @Test
    void convertNullReturnsZero() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(null), "Null input must be converted to BigDecimal.ZERO");
    }

    @Test
    void convertInteger() {
        Integer value = 42;
        assertEquals(BigDecimal.valueOf(42), Converter.convert2BigDecimal(value));
    }

    @Test
    void convertLong() {
        // Long.MAX_VALUE
        Long value = 9_223_372_036_854_775_807L;
        assertEquals(BigDecimal.valueOf(value), Converter.convert2BigDecimal(value));
    }

    @Test
    void convertFloat() {
        Float value = 3.14f;
        assertEquals(BigDecimal.valueOf(value), Converter.convert2BigDecimal(value));
    }

    @Test
    void convertDouble() {
        Double value = 2.718281828459045;
        assertEquals(BigDecimal.valueOf(value), Converter.convert2BigDecimal(value));
    }

    @Test
    void convertBigDecimal() {
        BigDecimal value = new BigDecimal("123456789.987654321");
        assertSame(value, Converter.convert2BigDecimal(value), "When the source is already a BigDecimal the same instance should be returned");
    }

    @Test
    void convertBigInteger() {
        BigInteger value = new BigInteger("12345678901234567890");
        assertEquals(new BigDecimal(value), Converter.convert2BigDecimal(value));
    }

    @Test
    void convertNumericString() {
        String value = "456.789";
        assertEquals(new BigDecimal(value), Converter.convert2BigDecimal(value));
    }

    @Test
    void convertNonNumericStringThrows() {
        String value = "not-a-number";
        // The underlying converter throws a RuntimeException (ConversionException) for unsupported conversions.
        assertThrows(RuntimeException.class, () -> Converter.convert2BigDecimal(value));
    }

    @Test
    void convertEmptyStringReturnsZero() {
        assertEquals(BigDecimal.ZERO, Converter.convert2BigDecimal(""), "Empty string should be treated as zero per API contract");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void convertValidValues(String description, Object input, BigDecimal expected) {
        BigDecimal result = Converter.convert2BigDecimal(input);
        assertEquals(0, expected.compareTo(result), () -> description + " – expected " + expected + " but got " + result);
    }

    /**
     * Special case where the source is already a {@link BigDecimal}.
     * The contract states the same instance should be returned.
     */
    @Test
    void convertBigDecimalReturnsSameInstance() {
        BigDecimal src = new BigDecimal("987.654");
        BigDecimal result = Converter.convert2BigDecimal(src);
        assertSame(src, result, "When the source is a BigDecimal the same instance must be returned");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    void convertInvalidValues(String description, Object input) {
        assertThrows(RuntimeException.class, () -> Converter.convert2BigDecimal(input), () -> description + " – expected a RuntimeException");
    }
}
