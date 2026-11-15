package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToBigInteger_Test {

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Direct numeric wrapper types
        Arguments.of("Integer value -42", Integer.valueOf(-42), BigInteger.valueOf(-42)), Arguments.of("Long value 9876543210L", Long.valueOf(9876543210L), BigInteger.valueOf(9876543210L)), Arguments.of("Short value 7", Short.valueOf((short) 7), BigInteger.valueOf(7)), Arguments.of("Byte value 3", Byte.valueOf((byte) 3), BigInteger.valueOf(3)), Arguments.of("Float value -12.34f → -12", Float.valueOf(-12.34f), BigInteger.valueOf(-12)), Arguments.of("Double value 1234.56 → 1234", Double.valueOf(1234.56), BigInteger.valueOf(1234)), // Boolean mapping
        Arguments.of("Boolean TRUE → 1", Boolean.TRUE, BigInteger.ONE), Arguments.of("Boolean FALSE → 0", Boolean.FALSE, BigInteger.ZERO), // Character numeric
        Arguments.of("Character '9' → 9", Character.valueOf('9'), BigInteger.valueOf(9)), // String based conversions
        Arguments.of("Numeric String \"12345678901234567890\"", "12345678901234567890", new BigInteger("12345678901234567890")), Arguments.of("String with whitespace and sign \"   -42   \"", "   -42   ", BigInteger.valueOf(-42)), Arguments.of("String with explicit plus sign \"+12345\"", "+12345", BigInteger.valueOf(12345)), Arguments.of("String with leading zeros \"00000123\"", "00000123", BigInteger.valueOf(123)), // CharSequence implementations
        Arguments.of("StringBuilder \"9876543210\"", new StringBuilder("9876543210"), BigInteger.valueOf(9876543210L)), Arguments.of("CharBuffer \"9876543210\"", CharBuffer.wrap("9876543210"), BigInteger.valueOf(9876543210L)), Arguments.of("CharBuffer with whitespace \"   42   \"", CharBuffer.wrap("   42   "), BigInteger.valueOf(42)), // Buffer types
        Arguments.of("ByteBuffer big‑endian {0x01,0x00,0x00,0x00} → 16777216", ByteBuffer.wrap(new byte[] { 0x01, 0x00, 0x00, 0x00 }), BigInteger.valueOf(16777216L)), // BigDecimal / scientific notation
        Arguments.of("BigDecimal \"12345.6789\" → 12345", new BigDecimal("12345.6789"), new BigInteger("12345")), Arguments.of("BigDecimal \"1.23E3\" → 1230", new BigDecimal("1.23E3"), BigInteger.valueOf(1230)), // Date / Time
        Arguments.of("java.util.Date epoch millis 1609459200000L", new Date(1609459200000L), BigInteger.valueOf(1609459200000L)), Arguments.of("java.sql.Timestamp 1609459200123L", new Timestamp(1609459200123L), BigInteger.valueOf(1609459200123L)), Arguments.of("java.sql.Date 1609459200000L", new java.sql.Date(1609459200000L), BigInteger.valueOf(1609459200000L)), Arguments.of("Calendar UTC epoch 1609459200000L", utcCalendar(1609459200000L), BigInteger.valueOf(1609459200000L)), Arguments.of("Instant epoch seconds 123456789L → 123456789000", Instant.ofEpochSecond(123456789L), BigInteger.valueOf(123456789000L)), Arguments.of("Instant with nanos 1s + 500_000_000ns → 1500", Instant.ofEpochSecond(1, 500_000_000), BigInteger.valueOf(1500L)), Arguments.of("LocalDate 1970‑01‑02 → 1", LocalDate.of(1970, 1, 2), BigInteger.ONE), Arguments.of("LocalDate 1969‑12‑31 → -1", LocalDate.of(1969, 12, 31), BigInteger.valueOf(-1)), Arguments.of("LocalDateTime 1970‑01‑01T00:00:01 → 1000", LocalDateTime.of(1970, 1, 1, 0, 0, 1), BigInteger.valueOf(1000L)), Arguments.of("LocalDateTime 1969‑12‑31T23:59:59 → -1000", LocalDateTime.of(1969, 12, 31, 23, 59, 59), BigInteger.valueOf(-1000L)), Arguments.of("ZonedDateTime UTC epoch → 0", ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), BigInteger.ZERO), Arguments.of("ZonedDateTime +02:00 offset → 7200000", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2)), BigInteger.valueOf(7_200_000L)), // UUID
        Arguments.of("UUID least‑significant bit 1 → 1", UUID.fromString("00000000-0000-0000-0000-000000000001"), BigInteger.ONE), // Atomic types
        Arguments.of("AtomicInteger 12345 → 12345", new AtomicInteger(12345), BigInteger.valueOf(12345)), Arguments.of("AtomicLong -9876543210L → -9876543210", new AtomicLong(-9876543210L), BigInteger.valueOf(-9876543210L)), Arguments.of("AtomicBoolean true → 1", new AtomicBoolean(true), BigInteger.ONE), Arguments.of("AtomicBoolean false → 0", new AtomicBoolean(false), BigInteger.ZERO), // Empty string should be treated as 0 according to API doc
        Arguments.of("Empty string \"\" → 0", "", BigInteger.ZERO));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Non‑numeric strings
        Arguments.of("Non‑numeric string \"not-a-number\"", "not-a-number", NumberFormatException.class), Arguments.of("String containing only plus sign \"+\"", "+", NumberFormatException.class), Arguments.of("String containing only minus sign \"-\"", "-", NumberFormatException.class), Arguments.of("String with trailing characters \"123abc\"", "123abc", NumberFormatException.class), // Empty CharBuffer / ByteBuffer
        Arguments.of("Empty CharBuffer", CharBuffer.wrap(""), NumberFormatException.class), Arguments.of("Empty ByteBuffer", ByteBuffer.wrap(new byte[] {}), IllegalArgumentException.class), // Non‑numeric CharBuffer
        Arguments.of("CharBuffer with non‑numeric chars \"12a34\"", CharBuffer.wrap("12a34"), NumberFormatException.class), // Non‑numeric character
        Arguments.of("Character 'A' (non‑numeric) → NumberFormatException", Character.valueOf('A'), NumberFormatException.class), // NaN / infinities
        Arguments.of("Double.NaN", Double.NaN, IllegalArgumentException.class), Arguments.of("Double.POSITIVE_INFINITY", Double.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of("Double.NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY, IllegalArgumentException.class), Arguments.of("Float.POSITIVE_INFINITY", Float.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of("Float.NEGATIVE_INFINITY", Float.NEGATIVE_INFINITY, IllegalArgumentException.class), // Unsupported object types
        Arguments.of("Plain Object", new Object(), IllegalArgumentException.class), Arguments.of("int[] primitive array", new int[] { 1, 2, 3 }, IllegalArgumentException.class), Arguments.of("Array of BigInteger objects", new BigInteger[] { BigInteger.ONE }, IllegalArgumentException.class), Arguments.of("java.time.Duration", Duration.ofHours(5), IllegalArgumentException.class), Arguments.of("java.util.Optional", Optional.of(123), IllegalArgumentException.class), Arguments.of("Collection List<Integer>", List.of(1, 2, 3), IllegalArgumentException.class));
    }

    /**
     * Creates a {@link Calendar} instance set to UTC with the given epoch millis.
     */
    private static Calendar utcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertNull(Converter.convertToBigInteger(null), "Converting a null source should return null");
    }

    @Test
    void shouldReturnSameInstanceWhenSourceIsBigInteger() {
        BigInteger source = new BigInteger("12345678901234567890");
        BigInteger result = Converter.convertToBigInteger(source);
        assertSame(source, result, "When the source is already a BigInteger the same instance should be returned");
    }

    @Test
    void shouldConvertLongToBigInteger() {
        Long source = 42L;
        BigInteger expected = BigInteger.valueOf(42L);
        assertEquals(expected, Converter.convertToBigInteger(source));
    }

    @Test
    void shouldConvertIntegerToBigInteger() {
        Integer source = 7;
        BigInteger expected = BigInteger.valueOf(7L);
        assertEquals(expected, Converter.convertToBigInteger(source));
    }

    @Test
    void shouldConvertStringNumericToBigInteger() {
        String source = "9876543210";
        BigInteger expected = new BigInteger("9876543210");
        assertEquals(expected, Converter.convertToBigInteger(source));
    }

    @Test
    void shouldThrowNumberFormatExceptionForNonNumericString() {
        String source = "not-a-number";
        assertThrows(NumberFormatException.class, () -> Converter.convertToBigInteger(source), "Converting a non‑numeric String should throw NumberFormatException");
    }

    @Test
    void shouldConvertBigDecimalToBigInteger() {
        BigDecimal source = new BigDecimal("12345.678");
        // The standard conversion truncates the fractional part (like BigDecimal#toBigInteger)
        BigInteger expected = source.toBigInteger();
        assertEquals(expected, Converter.convertToBigInteger(source));
    }

    @Test
    void shouldConvertDoubleToBigInteger() {
        Double source = 3.14159;
        // Converter follows the same rule as BigDecimal conversion – truncates toward zero
        BigInteger expected = BigDecimal.valueOf(source).toBigInteger();
        assertEquals(expected, Converter.convertToBigInteger(source));
    }

    @Test
    void shouldConvertByteArrayToBigIntegerWhenArrayRepresentsNumber() {
        // The Converter can interpret a byte[] as a number (big‑endian)
        // 256 in decimal
        byte[] source = new byte[] { 0x01, 0x00 };
        BigInteger expected = new BigInteger(1, source);
        assertEquals(expected, Converter.convertToBigInteger(source));
    }

    @Test
    void shouldThrowUnsupportedOperationExceptionForUnsupportedType() {
        // For a type that has no conversion (e.g., Boolean) the Converter throws an exception.
        Boolean source = Boolean.TRUE;
        assertThrows(UnsupportedOperationException.class, () -> Converter.convertToBigInteger(source), "Converting an unsupported type should throw UnsupportedOperationException");
    }

    /**
     * All successful conversion cases.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void shouldConvertValidSources(String description, Object source, BigInteger expected) {
        assertEquals(expected, Converter.convertToBigInteger(source), description);
    }

    /**
     * All cases that must result in an exception.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    void shouldThrowForInvalidSources(String description, Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToBigInteger(source), description);
    }
}
