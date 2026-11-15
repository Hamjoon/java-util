package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Calendar;
import java.util.Date;
import java.math.BigDecimal;
import java.math.BigInteger;
import com.cedarsoftware.util.convert.CommonValues;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.CharBuffer;
import java.time.Instant;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@ExtendWith(MockitoExtension.class)
class Converter_convert2long_Test {

    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// Integer
        Arguments.of(Integer.valueOf(12345), 12345L), // Double without fractional part
        Arguments.of(Double.valueOf(67890.0), 67890L), // Long (unchanged)
        Arguments.of(Long.valueOf(1234567890123L), 1234567890123L), // Numeric strings (trimmed)
        Arguments.of("98765", 98765L), Arguments.of("   54321   ", 54321L), // Booleans
        Arguments.of(Boolean.TRUE, 1L), Arguments.of(Boolean.FALSE, 0L), // BigDecimal (fraction truncated)
        Arguments.of(new BigDecimal("12345.67"), 12345L), // BigInteger
        Arguments.of(new BigInteger("9876543210"), 9876543210L), // Instant → epoch millis
        Arguments.of(Instant.parse("2023-01-01T00:00:00Z"), 1672531200000L), // Short
        Arguments.of(Short.valueOf((short) 123), 123L), // Byte
        Arguments.of(Byte.valueOf((byte) -45), -45L), // Double with fractional part (truncated)
        Arguments.of(123.99d, 123L), Arguments.of(123.45d, 123L), // AtomicInteger
        Arguments.of(new AtomicInteger(777), 777L), // AtomicLong
        Arguments.of(new AtomicLong(-999L), -999L), // AtomicBoolean true / false
        Arguments.of(new AtomicBoolean(true), 1L), Arguments.of(new AtomicBoolean(false), 0L), // String with leading plus sign and whitespace
        Arguments.of(" +12345 ", 12345L), // Negative numeric string
        Arguments.of("-9876", -9876L), // Zero string
        Arguments.of("0", 0L), // CharBuffer containing numeric string
        Arguments.of(CharBuffer.wrap("4567"), 4567L), // Boundary values
        Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE));
    }

    private static Stream<Arguments> provideInvalidConversions() {
        return Stream.of(// Non‑numeric string
        Arguments.of("not-a-number", NumberFormatException.class), // Empty string
        Arguments.of("", NumberFormatException.class), // Whitespace‑only string
        Arguments.of("   ", NumberFormatException.class), // Scientific notation (not an integer literal)
        Arguments.of("1e3", NumberFormatException.class), // Mixed sign characters
        Arguments.of("+-123", NumberFormatException.class), // Trailing alphabetic characters
        Arguments.of("123abc", NumberFormatException.class), // Char (single character) – not interpreted as code point
        Arguments.of(Character.valueOf('9'), UnsupportedOperationException.class), // CharBuffer with non‑numeric content
        Arguments.of(CharBuffer.wrap("abcXYZ"), NumberFormatException.class), // Date object – per JSON spec, unsupported
        Arguments.of(new Date(Instant.parse("2023-01-01T12:34:56Z").toEpochMilli()), UnsupportedOperationException.class), // LocalDate
        Arguments.of(java.time.LocalDate.parse("2023-01-01"), UnsupportedOperationException.class), // LocalDateTime
        Arguments.of(java.time.LocalDateTime.parse("2023-01-01T12:34:56"), UnsupportedOperationException.class), // ZonedDateTime
        Arguments.of(java.time.ZonedDateTime.parse("2023-01-01T12:34:56+01:00[Europe/Paris]"), UnsupportedOperationException.class), // Timestamp
        Arguments.of(java.sql.Timestamp.valueOf("2023-01-01 12:34:56.789"), UnsupportedOperationException.class), // UUID
        Arguments.of(java.util.UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), UnsupportedOperationException.class), // StringBuilder (unsupported source type)
        Arguments.of(new StringBuilder("123"), UnsupportedOperationException.class), // Plain Object
        Arguments.of(new Object(), UnsupportedOperationException.class), // List collection
        Arguments.of(java.util.List.of(1, 2, 3), UnsupportedOperationException.class), // Set collection
        Arguments.of(java.util.Set.of(10, 20), UnsupportedOperationException.class), // Primitive int array
        Arguments.of(new int[] { 5, 10, 15 }, UnsupportedOperationException.class), // Object array
        Arguments.of(new Object[] { "1", "2" }, UnsupportedOperationException.class), // BigDecimal overflow (beyond Long.MAX_VALUE)
        Arguments.of(new BigDecimal("9223372036854775808"), ArithmeticException.class), // BigInteger overflow (positive)
        Arguments.of(new BigInteger("9223372036854775808"), ArithmeticException.class), // BigInteger overflow (negative)
        Arguments.of(new BigInteger("-9223372036854775809"), ArithmeticException.class));
    }

    @Test
    void convert2long_NullInput_ReturnsZero() {
        long result = Converter.convert2long(null);
        assertEquals(CommonValues.LONG_ZERO, result, "convert2long(null) should return CommonValues.LONG_ZERO (0L)");
    }

    @Test
    void convert2long_StringNumeric_ReturnsParsedLong() {
        long result = Converter.convert2long("12345");
        assertEquals(12345L, result, "String numeric value should be parsed to long");
    }

    @Test
    void convert2long_Integer_ReturnsSameLongValue() {
        Integer source = 42;
        long result = Converter.convert2long(source);
        assertEquals(42L, result, "Integer should be converted to the same long value");
    }

    @Test
    void convert2long_Long_ReturnsSameValue() {
        Long source = 9_876_543_210L;
        long result = Converter.convert2long(source);
        assertEquals(9_876_543_210L, result, "Long should be returned unchanged");
    }

    @Test
    void convert2long_BigDecimal_ReturnsLongTruncated() {
        BigDecimal source = new BigDecimal("12345.678");
        long result = Converter.convert2long(source);
        assertEquals(12345L, result, "BigDecimal should be converted by truncating the fractional part");
    }

    @Test
    void convert2long_BigInteger_ReturnsSameLongValue() {
        BigInteger source = new BigInteger("9876543210");
        long result = Converter.convert2long(source);
        assertEquals(9_876_543_210L, result, "BigInteger should be converted to the same long value");
    }

    @Test
    void convert2long_Date_ReturnsEpochMillis() {
        // specific epoch millis
        Date source = new Date(1_600_000_000_000L);
        long result = Converter.convert2long(source);
        assertEquals(1_600_000_000_000L, result, "Date should be converted to its epoch millisecond value");
    }

    @Test
    void convert2long_Calendar_ReturnsEpochMillis() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(2_500_000_000L);
        long result = Converter.convert2long(cal);
        assertEquals(2_500_000_000L, result, "Calendar should be converted to its epoch millisecond value");
    }

    @Test
    void convert2long_UnsupportedType_ThrowsException() {
        // no conversion defined for Object -> long
        Object unsupported = new Object();
        assertThrows(RuntimeException.class, () -> Converter.convert2long(unsupported), "Conversion of an unsupported type should result in a RuntimeException");
    }

    /* -------------------------------------------------------------
     *  Valid conversions – numeric, strings, wrappers, temporals, etc.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] convert2long({0}) => {1}")
    @MethodSource("provideValidConversions")
    void convert2long_ValidConversions(Object source, long expected) {
        long result = Converter.convert2long(source);
        assertEquals(expected, result, () -> "Expected conversion of " + source + " to be " + expected);
    }

    /* -------------------------------------------------------------
     *  Invalid conversions – expect specific exception types
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] convert2long({0}) throws {1}")
    @MethodSource("provideInvalidConversions")
    void convert2long_InvalidConversions(Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convert2long(source), () -> "Expected " + expectedException.getSimpleName() + " for input: " + source);
    }
}
