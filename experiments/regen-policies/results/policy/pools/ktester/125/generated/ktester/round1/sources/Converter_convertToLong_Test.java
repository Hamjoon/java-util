package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToLong_Test {

    private static Stream<Arguments> successfulConversions() {
        return Stream.of(// Null (already covered, but kept for completeness)
        Arguments.of(null, null, "Null input should return null"), // Primitive / wrapper numbers
        Arguments.of(1234567890123L, 1234567890123L, "Long instance unchanged"), Arguments.of(42, 42L, "Integer to Long"), Arguments.of(7.89, 7L, "Double truncated"), Arguments.of(1234.0f, 1234L, "Float truncated"), Arguments.of(new BigDecimal("12345.0"), 12345L, "BigDecimal to Long (exact)"), Arguments.of(new BigDecimal("1.9999999999999999999"), 1L, "BigDecimal with fractional part truncated"), // Strings
        Arguments.of("9876543210", 9876543210L, "Decimal string parsed"), Arguments.of("0x1F", 31L, "Hexadecimal string parsed"), Arguments.of("  42  ", 42L, "String with surrounding whitespace trimmed"), Arguments.of(" +42 ", 42L, "String with leading plus sign and whitespace"), Arguments.of("-0xFF", -255L, "Negative hexadecimal string parsed"), Arguments.of("2023-08-15T12:34:56", 1692106496000L, "ISO‑8601 LocalDateTime string"), Arguments.of("2023-08-15", 1692057600000L, "ISO‑8601 LocalDate string (start of day UTC)"), Arguments.of("2023-08-15T12:34:56Z", 1692106496000L, "ISO‑8601 ZonedDateTime string with Z suffix"), // Booleans
        Arguments.of(Boolean.TRUE, 1L, "Boolean true → 1"), Arguments.of(Boolean.FALSE, 0L, "Boolean false → 0"), // Atomic types
        Arguments.of(new AtomicInteger(1234567890), 1234567890L, "AtomicInteger unwrapped"), Arguments.of(new AtomicLong(9876543210L), 9876543210L, "AtomicLong unwrapped"), Arguments.of(new AtomicBoolean(true), 1L, "AtomicBoolean true → 1"), Arguments.of(new AtomicBoolean(false), 0L, "AtomicBoolean false → 0"), // Date/Time objects
        Arguments.of(1622547800000L, 1622547800000L, "Long epoch millis unchanged"), Arguments.of(new Date(1622547800000L), 1622547800000L, "java.util.Date → epoch millis"), Arguments.of(new Timestamp(1622547800000L), 1622547800000L, "java.sql.Timestamp → epoch millis"), Arguments.of(calendarOf(1622547800000L), 1622547800000L, "java.util.Calendar → epoch millis"), Arguments.of(Instant.ofEpochMilli(1622547800000L), 1622547800000L, "Instant → epoch millis"), Arguments.of(LocalDateTime.of(2023, 8, 15, 12, 34, 56), 1692106496000L, "LocalDateTime → epoch millis (UTC)"), Arguments.of(ZonedDateTime.of(LocalDateTime.of(2023, 8, 15, 12, 34, 56), ZoneOffset.UTC), 1692106496000L, "ZonedDateTime UTC → epoch millis"), Arguments.of(java.sql.Date.valueOf("2023-08-15"), 1692057600000L, "java.sql.Date → start‑of‑day UTC epoch millis"), // Primitive wrappers
        Arguments.of(Byte.valueOf((byte) 127), 127L, "Byte value widened"), Arguments.of(Short.valueOf((short) -32768), -32768L, "Short value widened"), Arguments.of(Character.valueOf('A'), 65L, "Character converted to Unicode code point"), // Buffers
        Arguments.of(CharBuffer.wrap("123"), 123L, "CharBuffer containing numeric string"), Arguments.of(ByteBuffer.wrap(new byte[] { 0, 0, 0, 42 }), 42L, "ByteBuffer interpreted as big‑endian int"), // Boundary values
        Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE, "Long.MAX_VALUE unchanged"), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE, "Long.MIN_VALUE unchanged"));
    }

    private static Calendar calendarOf(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    private static Stream<Arguments> exceptionConversions() {
        return Stream.of(// Unsupported numeric string formats
        Arguments.of("0b1010", NumberFormatException.class, "Binary prefix not supported"), Arguments.of("", NumberFormatException.class, "Empty string"), Arguments.of("123abc", NumberFormatException.class, "String with non‑numeric characters"), Arguments.of("0xGHI", NumberFormatException.class, "Invalid hexadecimal characters"), Arguments.of("   ", NumberFormatException.class, "String with only whitespace"), Arguments.of("+", NumberFormatException.class, "String with only a plus sign"), Arguments.of("12.34", NumberFormatException.class, "Decimal string (dot) not accepted for Long"), // Overflow / arithmetic errors
        Arguments.of(new BigInteger("9223372036854775808"), ArithmeticException.class, "BigInteger larger than Long.MAX_VALUE should overflow"), // Unsupported object types
        Arguments.of(new Object(), com.cedarsoftware.util.convert.ConversionException.class, "Arbitrary object is not convertible to Long"), Arguments.of(Arrays.asList(1, 2, 3), com.cedarsoftware.util.convert.ConversionException.class, "Collection types are not supported for conversion to Long"));
    }

    @Test
    void convert_NullInput_ReturnsNull() {
        // given
        Object source = null;
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertNull(result, "Converting null should return null");
    }

    @Test
    void convert_LongInput_ReturnsSameValue() {
        // given
        Long source = 42L;
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(source, result);
    }

    @Test
    void convert_IntegerInput_ReturnsLongValue() {
        // given
        Integer source = 7;
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(7L, result);
    }

    @Test
    void convert_StringNumeric_ReturnsParsedLong() {
        // given
        String source = "123456";
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(123456L, result);
    }

    @Test
    void convert_StringNonNumeric_ReturnsNull() {
        // given
        String source = "not-a-number";
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertNull(result, "Non‑numeric strings should convert to null");
    }

    @Test
    void convert_BigDecimal_ReturnsLongTruncated() {
        // given
        BigDecimal source = new BigDecimal("99.99");
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(99L, result);
    }

    @Test
    void convert_BigInteger_ReturnsLongValue() {
        // given
        BigInteger source = new BigInteger("1234567890123");
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(1234567890123L, result);
    }

    @Test
    void convert_Date_ReturnsEpochMillis() {
        // given
        // a fixed timestamp
        Date source = new Date(1_600_000_000_000L);
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(source.getTime(), result);
    }

    @Test
    void convert_Calendar_ReturnsTimeInMillis() {
        // given
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1_610_000_000_000L);
        long expected = cal.getTimeInMillis();
        // when
        Long result = Converter.convertToLong(cal);
        // then
        assertEquals(expected, result);
    }

    @Test
    void convert_Instant_ReturnsEpochMillis() {
        // given
        Instant instant = Instant.ofEpochMilli(1_620_000_000_000L);
        // when
        Long result = Converter.convertToLong(instant);
        // then
        assertEquals(instant.toEpochMilli(), result);
    }

    @Test
    void convert_AtomicLong_ReturnsContainedValue() {
        // given
        AtomicLong source = new AtomicLong(555L);
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(555L, result);
    }

    @Test
    void convert_AtomicInteger_ReturnsContainedValue() {
        // given
        AtomicInteger source = new AtomicInteger(42);
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(42L, result);
    }

    @Test
    void convert_AtomicBoolean_TrueReturnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(1L, result);
    }

    @Test
    void convert_AtomicBoolean_FalseReturnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(0L, result);
    }

    @Test
    void convert_BooleanTrue_ReturnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(1L, result);
    }

    @Test
    void convert_BooleanFalse_ReturnsZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(0L, result);
    }

    @Test
    void convert_Byte_ReturnsLongValue() {
        // given
        Byte source = (byte) 9;
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(9L, result);
    }

    @Test
    void convert_Short_ReturnsLongValue() {
        // given
        Short source = (short) 123;
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(123L, result);
    }

    @Test
    void convert_Float_ReturnsLongTruncated() {
        // given
        Float source = 3.14f;
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(3L, result);
    }

    @Test
    void convert_Double_ReturnsLongTruncated() {
        // given
        Double source = 6.99;
        // when
        Long result = Converter.convertToLong(source);
        // then
        assertEquals(6L, result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversions")
    void convert_Successful(Object input, Long expected, String description) {
        // when
        Long result = Converter.convertToLong(input);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionConversions")
    void convert_ThrowsException(Object input, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> Converter.convertToLong(input), description);
    }
}
