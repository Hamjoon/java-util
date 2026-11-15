package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class Converter_convert2AtomicLong_Test {

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Integer → AtomicLong
        arguments(Integer.valueOf(42), 42L), // Long → AtomicLong
        arguments(Long.valueOf(1234567890123L), 1234567890123L), // Short → AtomicLong
        arguments(Short.valueOf((short) 5), 5L), // Byte → AtomicLong (negative value)
        arguments(Byte.valueOf((byte) -7), -7L), // BigInteger within long range → AtomicLong
        arguments(new BigInteger("1234567890123"), 1234567890123L), // BigDecimal → AtomicLong (fractional part truncated)
        arguments(new BigDecimal("12345.678"), 12345L), // Numeric String → AtomicLong
        arguments("9876", 9876L), // String with surrounding whitespace → AtomicLong
        arguments("  55  ", 55L), // Existing AtomicLong → new AtomicLong with same value
        arguments(new AtomicLong(777L), 777L), // AtomicInteger → AtomicLong
        arguments(new AtomicInteger(321), 321L), // AtomicBoolean true → 1
        arguments(new AtomicBoolean(true), 1L), // AtomicBoolean false → 0
        arguments(new AtomicBoolean(false), 0L), // Boolean true → 1
        arguments(Boolean.TRUE, 1L), // Boolean false → 0
        arguments(Boolean.FALSE, 0L), // java.util.Date → epoch millis
        arguments(new Date(1_609_459_200_000L), 1_609_459_200_000L));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// Non‑numeric String
        arguments("notANumber"), // Empty String
        arguments(""), // Whitespace‑only String
        arguments("   \t\n"), // String numeric value greater than Long.MAX_VALUE
        arguments("9223372036854775808"), // BigInteger out of long range
        arguments(new BigInteger("9223372036854775808")), // BigDecimal below Long.MIN_VALUE
        arguments(new BigDecimal("-9223372036854775809")), // Double.NaN
        arguments(Double.NaN), // Float.POSITIVE_INFINITY
        arguments(Float.POSITIVE_INFINITY), // Non‑numeric char
        arguments('a'), // CharBuffer with non‑numeric characters
        arguments(CharBuffer.wrap("12x34")), // ByteBuffer that does not represent a parsable numeric string
        arguments(ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03 })), // Unsupported type: UUID
        arguments(UUID.randomUUID()), // Unsupported type: generic Object
        arguments(new Object()));
    }

    @Test
    @DisplayName("convert2AtomicLong returns AtomicLong(0) when input is null")
    void testConvertNull() {
        // given
        Object input = null;
        // when
        AtomicLong result = Converter.convert2AtomicLong(input);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(0L, result.get(), "AtomicLong value should be 0 for null input");
    }

    @Test
    @DisplayName("convert2AtomicLong correctly converts supported types")
    void testConvertSupportedTypes() {
        // given a variety of source objects that are known to be convertible to long
        Object[] sources = { // Long
        123L, // Integer
        456, // Double (truncates)
        78.9d, // String
        "321", // Date (milliseconds since epoch)
        new java.util.Date(1_600_000_000_000L), // Instant
        java.time.Instant.ofEpochMilli(1_600_000_000_000L), // LocalDateTime (converted via epoch)
        java.time.LocalDateTime.of(2020, 9, 13, 12, 0), java.time.ZonedDateTime.of(2020, 9, 13, 12, 0, 0, 0, // ZonedDateTime
        java.time.ZoneId.of("UTC")) };
        long[] expected = { 123L, 456L, // double to long truncates the fractional part
        78L, 321L, 1_600_000_000_000L, 1_600_000_000_000L, // LocalDateTime converted to epoch millis
        1_600_000_000_000L, // ZonedDateTime converted to epoch millis
        1_600_000_000_000L };
        for (int i = 0; i < sources.length; i++) {
            // when
            AtomicLong result = Converter.convert2AtomicLong(sources[i]);
            // then
            assertNotNull(result, "Result should not be null for source index " + i);
            assertEquals(expected[i], result.get(), "Incorrect conversion for source index " + i + " (type: " + sources[i].getClass().getSimpleName() + ")");
        }
    }

    @ParameterizedTest(name = "[{index}] convert2AtomicLong({0}) → {1}")
    @MethodSource("successfulConversionProvider")
    @DisplayName("convert2AtomicLong – successful conversions")
    void testConvert2AtomicLongSuccess(Object input, long expected) {
        AtomicLong result = Converter.convert2AtomicLong(input);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), () -> "Expected AtomicLong value " + expected + " but got " + result.get() + " for input type " + (input == null ? "null" : input.getClass().getSimpleName()));
    }

    @ParameterizedTest(name = "[{index}] convert2AtomicLong({0}) throws IllegalArgumentException")
    @MethodSource("exceptionConversionProvider")
    @DisplayName("convert2AtomicLong – expected IllegalArgumentException")
    void testConvert2AtomicLongThrows(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(input), () -> "Expected IllegalArgumentException for input: " + (input == null ? "null" : input.getClass().getSimpleName()));
    }
}
