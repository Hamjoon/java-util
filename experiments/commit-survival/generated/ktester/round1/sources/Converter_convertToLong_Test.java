package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.ConversionException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToLong_Test {

    static Stream<Arguments> numericSuccessProvider() {
        return Stream.of(// null → null (handled in a separate test)
        Arguments.of(123L, 123L), Arguments.of(Integer.valueOf(42), 42L), Arguments.of(Short.valueOf((short) 45), 45L), Arguments.of(Byte.valueOf((byte) 7), 7L), Arguments.of(new BigInteger("9876543210"), 9876543210L), // fractional part truncated
        Arguments.of(new BigDecimal("12345.67"), 12345L), // double truncation
        Arguments.of(Double.valueOf(123.99d), 123L), // float truncation
        Arguments.of(Float.valueOf(456.0f), 456L), Arguments.of(new AtomicLong(789L), 789L), Arguments.of(new AtomicInteger(321), 321L), Arguments.of(new AtomicBoolean(true), 1L), Arguments.of(new AtomicBoolean(false), 0L));
    }

    static Stream<Arguments> stringCharSuccessProvider() {
        return Stream.of(Arguments.of("123456", 123456L), Arguments.of("   42   ", 42L), // character digit
        Arguments.of('9', 9L), // non‑digit char will be handled by failure test
        Arguments.of('+', 0L));
    }

    static Stream<Arguments> dateTimeSuccessProvider() {
        // Helper calendar in UTC
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2021‑01‑01T00:00:00Z
        utcCal.setTimeInMillis(1609459200000L);
        // LocalDate interpreted as start‑of‑day UTC
        long localDateEpoch = LocalDate.of(2021, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        // LocalDateTime interpreted in system default zone – we compute expected using the same logic
        LocalDateTime ldt = LocalDateTime.of(2021, 1, 1, 12, 30, 15);
        long ldtEpoch = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        // ZonedDateTime (UTC)
        long zonedEpoch = ZonedDateTime.parse("2021-01-01T00:00:00Z").toInstant().toEpochMilli();
        // Calendar with non‑UTC zone but same instant
        Calendar nyCal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCal.setTimeInMillis(1609459200000L);
        // Instant
        Instant instant = Instant.ofEpochMilli(1609459200000L);
        return Stream.of(Arguments.of(new Date(1609459200000L), 1609459200000L), Arguments.of(new Timestamp(1609459200000L), 1609459200000L), Arguments.of(LocalDate.of(2021, 1, 1), localDateEpoch), Arguments.of(ldt, ldtEpoch), Arguments.of(ZonedDateTime.parse("2021-01-01T00:00:00Z"), zonedEpoch), Arguments.of(utcCal, 1609459200000L), Arguments.of(nyCal, 1609459200000L), Arguments.of(java.sql.Date.valueOf(LocalDate.of(2021, 1, 1)), 1609459200000L), Arguments.of(instant, 1609459200000L));
    }

    static Stream<Arguments> booleanSuccessProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, 1L), Arguments.of(Boolean.FALSE, 0L), Arguments.of(true, 1L), Arguments.of(false, 0L));
    }

    static Stream<Arguments> failureProvider() {
        return Stream.of(// Non‑numeric string
        Arguments.of("not a number", ConversionException.class), // Character not a digit
        Arguments.of('A', ConversionException.class), // Arbitrary object
        Arguments.of(new Object(), ConversionException.class), // BigDecimal overflow
        Arguments.of(new BigDecimal("1E40"), ConversionException.class), // BigInteger overflow
        Arguments.of(new BigInteger("9223372036854775808"), ConversionException.class), Arguments.of(new BigInteger("-9223372036854775809"), ConversionException.class), // Empty / whitespace strings
        Arguments.of("", ConversionException.class), Arguments.of("   ", ConversionException.class), // Malformed numeric strings
        Arguments.of("123abc", ConversionException.class), Arguments.of("true", ConversionException.class), Arguments.of("1e3", ConversionException.class), Arguments.of("0xFF", ConversionException.class), Arguments.of("--123", ConversionException.class), Arguments.of("+ 123", ConversionException.class), // Special floating‑point values
        Arguments.of(Double.NaN, ConversionException.class), Arguments.of(Double.POSITIVE_INFINITY, ConversionException.class), Arguments.of(Float.NEGATIVE_INFINITY, ConversionException.class), // Overflow floating‑point values
        Arguments.of(Double.MAX_VALUE, ConversionException.class), Arguments.of(Float.MAX_VALUE, ConversionException.class), // Unsupported temporal types
        Arguments.of(LocalTime.of(12, 34, 56), ConversionException.class), Arguments.of(OffsetDateTime.now(), ConversionException.class), Arguments.of(YearMonth.of(2021, 5), ConversionException.class), // Unsupported collection / wrapper types
        Arguments.of(new int[] { 1, 2, 3 }, ConversionException.class), Arguments.of(Optional.of(123), ConversionException.class), Arguments.of(new StringBuilder("12345"), ConversionException.class), Arguments.of(new AtomicReference<>(123L), ConversionException.class), Arguments.of(Duration.ofHours(5), ConversionException.class), Arguments.of(java.sql.Time.valueOf(LocalTime.of(12, 0)), ConversionException.class), Arguments.of(OptionalInt.of(42), ConversionException.class), // Enum constant
        Arguments.of(MyEnum.VALUE, ConversionException.class));
    }

    static Stream<Arguments> boundaryNumericProvider() {
        return Stream.of(Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE), Arguments.of(new BigDecimal("9223372036854775807"), 9223372036854775807L), Arguments.of(new BigDecimal("-9223372036854775808"), -9223372036854775808L));
    }

    static Stream<Arguments> stringEdgeProvider() {
        return Stream.of(Arguments.of("+123", 123L), Arguments.of("-456", -456L), Arguments.of("  789  ", 789L));
    }

    static Stream<Arguments> additionalAtomicProvider() {
        return Stream.of(Arguments.of(Short.valueOf((short) 12), 12L), Arguments.of(Byte.valueOf((byte) -5), -5L));
    }

    @Test
    void convertFromLong_ShouldReturnSameValue() {
        Long source = 123L;
        Long result = Converter.convertToLong(source);
        assertEquals(source, result);
    }

    @Test
    void convertFromInteger_ShouldReturnLongValue() {
        Integer source = 42;
        Long result = Converter.convertToLong(source);
        assertEquals(42L, result);
    }

    @Test
    void convertFromShort_ShouldReturnLongValue() {
        Short source = 7;
        Long result = Converter.convertToLong(source);
        assertEquals(7L, result);
    }

    @Test
    void convertFromByte_ShouldReturnLongValue() {
        Byte source = 5;
        Long result = Converter.convertToLong(source);
        assertEquals(5L, result);
    }

    @Test
    void convertFromBigInteger_ShouldReturnLongValue() {
        BigInteger source = new BigInteger("9876543210");
        Long result = Converter.convertToLong(source);
        assertEquals(9876543210L, result);
    }

    @Test
    void convertFromBigDecimal_ShouldReturnLongValue() {
        BigDecimal source = new BigDecimal("12345.67");
        Long result = Converter.convertToLong(source);
        // fractional part is truncated
        assertEquals(12345L, result);
    }

    @Test
    void convertFromStringNumeric_ShouldParseLong() {
        String source = "55555";
        Long result = Converter.convertToLong(source);
        assertEquals(55555L, result);
    }

    @Test
    void convertFromStringWithWhitespace_ShouldParseLong() {
        String source = "   777   ";
        Long result = Converter.convertToLong(source);
        assertEquals(777L, result);
    }

    @Test
    void convertFromDate_ShouldReturnEpochMillis() {
        // 2020‑09‑13T12:26:40Z
        Date source = new Date(1_600_000_000_000L);
        Long result = Converter.convertToLong(source);
        assertEquals(1_600_000_000_000L, result);
    }

    @Test
    void convertFromTimestamp_ShouldReturnEpochMillis() {
        Timestamp source = new Timestamp(1_600_000_000_123L);
        Long result = Converter.convertToLong(source);
        assertEquals(1_600_000_000_123L, result);
    }

    @Test
    void convertFromLocalDateTime_ShouldReturnEpochMilliAtSystemDefaultZone() {
        // LocalDateTime does not contain zone information, the converter uses the system default zone.
        LocalDateTime source = LocalDateTime.of(2020, 9, 13, 12, 26, 40);
        Long result = Converter.convertToLong(source);
        // Compute expected epoch millis using the same logic as the library (system default zone)
        long expected = source.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(expected, result);
    }

    @Test
    void convertFromNull_ShouldReturnNull() {
        assertNull(Converter.convertToLong(null));
    }

    @Test
    void convertFromNonConvertibleObject_ShouldThrowException() {
        // no conversion defined for plain Object -> Long
        Object source = new Object();
        assertThrows(RuntimeException.class, () -> Converter.convertToLong(source));
    }

    @Test
    void convertFromStringNonNumeric_ShouldThrowException() {
        String source = "not-a-number";
        assertThrows(RuntimeException.class, () -> Converter.convertToLong(source));
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {1}")
    @MethodSource("numericSuccessProvider")
    void convertNumericTypes_ShouldReturnExpectedLong(Object source, Long expected) {
        Long result = Converter.convertToLong(source);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convert \"{0}\" → {1}")
    @MethodSource("stringCharSuccessProvider")
    void convertStringAndChar_ShouldParseLong(Object source, Long expected) {
        Long result = Converter.convertToLong(source);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {1}")
    @MethodSource("dateTimeSuccessProvider")
    void convertDateTimeTypes_ShouldReturnEpochMillis(Object source, Long expected) {
        Long result = Converter.convertToLong(source);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {1}")
    @MethodSource("booleanSuccessProvider")
    void convertBoolean_ShouldMapToLong(Object source, Long expected) {
        Long result = Converter.convertToLong(source);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convert {0} throws {1}")
    @MethodSource("failureProvider")
    void convertInvalidInputs_ShouldThrowException(Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToLong(source));
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {1}")
    @MethodSource("boundaryNumericProvider")
    void convertBoundaryValues_ShouldHandleCorrectly(Object source, Long expected) {
        Long result = Converter.convertToLong(source);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convert \"{0}\" → {1}")
    @MethodSource("stringEdgeProvider")
    void convertStringEdgeCases_ShouldParseCorrectly(String source, Long expected) {
        Long result = Converter.convertToLong(source);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {1}")
    @MethodSource("additionalAtomicProvider")
    void convertAdditionalAtomicTypes_ShouldUnwrap(Object source, Long expected) {
        Long result = Converter.convertToLong(source);
        assertEquals(expected, result);
    }
}
