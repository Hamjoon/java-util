package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toDate_Test {

    /**
     * Shared Converter instance used by all test methods.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions object; the default constructor
        // provides a sensible set of built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> numericSourceProvider() {
        return Stream.of(Arguments.of(1622548800000L, 1622548800000L, "Long"), Arguments.of(12345, 12345L, "Integer (auto‑boxed)"), Arguments.of(new BigInteger("9876543210"), 9876543210L, "BigInteger"));
    }

    private static Stream<Arguments> numericStringProvider() {
        return Stream.of(Arguments.of("1622548800000", 1622548800000L, "String containing a numeric value"), Arguments.of("   12345   ", 12345L, "String with surrounding whitespace"));
    }

    private static Stream<Arguments> floatingPointProvider() {
        return Stream.of(Arguments.of(12345.0d, 12345L, "Double value should be truncated"), Arguments.of(6789.9f, 6789L, "Float value should be truncated (floor)"));
    }

    @Test
    @DisplayName("toDate should convert a Long representing epoch milliseconds")
    void testToDateFromLong() {
        // 2021‑05‑03T00:00:00Z
        long epochMillis = 1_620_000_000_000L;
        Date result = DateConversions.toDate(epochMillis, converter);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(epochMillis, result.getTime(), "Date time should match the supplied epoch milliseconds");
    }

    @Test
    @DisplayName("toDate should return an equivalent Date when given a Date")
    void testToDateFromDate() {
        Date source = new Date(1_620_123_456_789L);
        Date result = DateConversions.toDate(source, converter);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(source.getTime(), result.getTime(), "Date time should be identical to the source Date's time");
    }

    @Test
    @DisplayName("toDate should convert a java.sql.Timestamp to a Date")
    void testToDateFromTimestamp() {
        long epochMillis = 1_620_987_654_321L;
        Timestamp ts = new Timestamp(epochMillis);
        Date result = DateConversions.toDate(ts, converter);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(epochMillis, result.getTime(), "Date time should match the timestamp's epoch milliseconds");
    }

    @Test
    @DisplayName("toDate should handle null input gracefully (returns epoch 0)")
    void testToDateFromNull() {
        Date result = DateConversions.toDate(null, converter);
        assertNotNull(result, "Resulting Date must not be null even for null input");
        assertEquals(0L, result.getTime(), "When the source is null, the conversion should yield epoch 0");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericSourceProvider")
    @DisplayName("Valid conversions – primitive and wrapper numbers")
    void testNumericConversions(Object from, long expectedEpoch, String description) {
        Date result = DateConversions.toDate(from, converter);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericStringProvider")
    @DisplayName("Valid conversions – String representations of numbers")
    void testStringNumberConversions(String from, long expectedEpoch, String description) {
        Date result = DateConversions.toDate(from, converter);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), description);
    }

    @Test
    @DisplayName("Valid conversions – java.util.Date")
    void testDateSubclassConversion() {
        Date source = new Date(1609459200000L);
        Date result = DateConversions.toDate(source, converter);
        assertEquals(source.getTime(), result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – java.sql.Timestamp (subclass of Date)")
    void testTimestampSubclassConversion() {
        Timestamp ts = new Timestamp(1609459200000L);
        Date result = DateConversions.toDate(ts, converter);
        assertEquals(ts.getTime(), result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – Instant")
    void testInstantConversion() {
        Instant instant = Instant.ofEpochMilli(1622548800000L);
        Date result = DateConversions.toDate(instant, converter);
        assertEquals(instant.toEpochMilli(), result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – LocalDateTime (system default zone)")
    void testLocalDateTimeConversion() {
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 1, 12, 30, 15);
        ZoneId zone = ZoneId.systemDefault();
        long expected = ldt.atZone(zone).toInstant().toEpochMilli();
        Date result = DateConversions.toDate(ldt, converter);
        assertEquals(expected, result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – LocalDate (start‑of‑day, system default zone)")
    void testLocalDateConversion() {
        LocalDate ld = LocalDate.of(2021, 6, 1);
        ZoneId zone = ZoneId.systemDefault();
        long expected = ld.atStartOfDay(zone).toInstant().toEpochMilli();
        Date result = DateConversions.toDate(ld, converter);
        assertEquals(expected, result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – ZonedDateTime (UTC)")
    void testZonedDateTimeConversion() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 6, 1, 12, 30, 15, 0, ZoneId.of("UTC"));
        long expected = zdt.toInstant().toEpochMilli();
        Date result = DateConversions.toDate(zdt, converter);
        assertEquals(expected, result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – OffsetDateTime (offset applied)")
    void testOffsetDateTimeConversion() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 6, 1, 12, 30, 15, 0, ZoneOffset.ofHours(-5));
        // 2021‑06‑01T17:30:15Z
        long expected = odt.toInstant().toEpochMilli();
        Date result = DateConversions.toDate(odt, converter);
        assertEquals(expected, result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – Calendar (UTC)")
    void testCalendarUtcConversion() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1622548800000L);
        Date result = DateConversions.toDate(cal, converter);
        assertEquals(1622548800000L, result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – Calendar (non‑UTC zone)")
    void testCalendarNonUtcConversion() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.setTimeInMillis(1622548800000L);
        Date result = DateConversions.toDate(cal, converter);
        assertEquals(1622548800000L, result.getTime());
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("floatingPointProvider")
    @DisplayName("Valid conversions – floating point numbers")
    void testFloatingPointConversions(Number from, long expectedEpoch, String description) {
        Date result = DateConversions.toDate(from, converter);
        assertEquals(expectedEpoch, result.getTime(), description);
    }

    @Test
    @DisplayName("Invalid conversions – floating‑point special values (NaN)")
    void testFloatingPointNaN() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(Double.NaN, converter));
    }

    @Test
    @DisplayName("Invalid conversions – floating‑point special values (POSITIVE_INFINITY)")
    void testFloatingPointPosInfinity() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(Double.POSITIVE_INFINITY, converter));
    }

    @Test
    @DisplayName("Invalid conversions – floating‑point special values (NEGATIVE_INFINITY)")
    void testFloatingPointNegInfinity() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(Float.NEGATIVE_INFINITY, converter));
    }

    @Test
    @DisplayName("Valid conversions – BigDecimal without fractional part")
    void testBigDecimalWhole() {
        BigDecimal bd = new BigDecimal("5555555555");
        Date result = DateConversions.toDate(bd, converter);
        assertEquals(5555555555L, result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – BigDecimal with fractional part (truncated)")
    void testBigDecimalFractional() {
        BigDecimal bd = new BigDecimal("12345.678");
        Date result = DateConversions.toDate(bd, converter);
        assertEquals(12345L, result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – AtomicLong")
    void testAtomicLongConversion() {
        AtomicLong al = new AtomicLong(9876543210L);
        Date result = DateConversions.toDate(al, converter);
        assertEquals(9876543210L, result.getTime());
    }

    @Test
    @DisplayName("Valid conversions – BigInteger")
    void testBigIntegerConversion() {
        BigInteger bi = new BigInteger("9876543210");
        Date result = DateConversions.toDate(bi, converter);
        assertEquals(9876543210L, result.getTime());
    }

    @Test
    @DisplayName("Boundary values – Long.MAX_VALUE")
    void testLongMaxValue() {
        Date result = DateConversions.toDate(Long.MAX_VALUE, converter);
        assertEquals(Long.MAX_VALUE, result.getTime());
    }

    @Test
    @DisplayName("Boundary values – Long.MIN_VALUE")
    void testLongMinValue() {
        Date result = DateConversions.toDate(Long.MIN_VALUE, converter);
        assertEquals(Long.MIN_VALUE, result.getTime());
    }

    @Test
    @DisplayName("Edge case – null source (should throw IllegalArgumentException)")
    void testNullSourceThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(null, converter));
    }

    @Test
    @DisplayName("Edge case – unsupported Boolean source")
    void testUnsupportedBoolean() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(Boolean.TRUE, converter));
    }

    @Test
    @DisplayName("Invalid conversion – non‑numeric string")
    void testNonNumericString() {
        assertThrows(NumberFormatException.class, () -> DateConversions.toDate("not-a-number", converter));
    }

    @Test
    @DisplayName("Invalid conversion – ISO‑8601 date string")
    void testIso8601String() {
        assertThrows(NumberFormatException.class, () -> DateConversions.toDate("2021-06-01T12:30:15Z", converter));
    }

    @Test
    @DisplayName("Invalid conversion – LocalTime (unsupported)")
    void testLocalTimeUnsupported() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(LocalTime.of(12, 30, 15), converter));
    }

    @Test
    @DisplayName("Invalid conversion – YearMonth (unsupported)")
    void testYearMonthUnsupported() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate(YearMonth.of(2021, 6), converter));
    }

    @Test
    @DisplayName("Invalid conversion – empty string")
    void testEmptyString() {
        assertThrows(NumberFormatException.class, () -> DateConversions.toDate("", converter));
    }

    @Test
    @DisplayName("Invalid conversion – whitespace‑only string")
    void testWhitespaceOnlyString() {
        assertThrows(NumberFormatException.class, () -> DateConversions.toDate("   \t\n  ", converter));
    }

    @Test
    @DisplayName("Invalid conversion – BigInteger overflow")
    void testBigIntegerOverflow() {
        // Long.MAX_VALUE + 1
        BigInteger tooBig = new BigInteger("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> DateConversions.toDate(tooBig, converter));
    }

    @Test
    @DisplayName("Invalid conversion – numeric string overflow (positive)")
    void testNumericStringOverflowPositive() {
        assertThrows(NumberFormatException.class, () -> DateConversions.toDate("9223372036854775808", converter));
    }

    @Test
    @DisplayName("Invalid conversion – numeric string overflow (negative)")
    void testNumericStringOverflowNegative() {
        assertThrows(NumberFormatException.class, () -> DateConversions.toDate("-9223372036854775809", converter));
    }

    @Test
    @DisplayName("Invalid conversion – BigDecimal overflow (positive)")
    void testBigDecimalOverflowPositive() {
        BigDecimal tooBig = new BigDecimal("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> DateConversions.toDate(tooBig, converter));
    }

    @Test
    @DisplayName("Invalid conversion – BigDecimal overflow (negative)")
    void testBigDecimalOverflowNegative() {
        BigDecimal tooSmall = new BigDecimal("-9223372036854775809");
        assertThrows(ArithmeticException.class, () -> DateConversions.toDate(tooSmall, converter));
    }

    @Test
    @DisplayName("Custom Converter – returns a specific long for a special string")
    void testCustomConverterReturnsLong() {
        // Mock a Converter that returns 9999L when asked to convert the string "special".
        Converter custom = mock(Converter.class);
        when(custom.convert(eq("special"), any())).thenReturn(9999L);
        Date result = DateConversions.toDate("special", custom);
        assertEquals(9999L, result.getTime());
    }

    @Test
    @DisplayName("Custom Converter – returns a numeric string which is then parsed")
    void testCustomConverterReturnsString() {
        Converter custom = mock(Converter.class);
        when(custom.convert(eq("special"), any())).thenReturn("55555");
        Date result = DateConversions.toDate("special", custom);
        assertEquals(55555L, result.getTime());
    }

    @Test
    @DisplayName("Custom Converter – throws IllegalStateException (should propagate)")
    void testCustomConverterThrowsException() {
        Converter custom = mock(Converter.class);
        when(custom.convert(eq("any"), any())).thenThrow(new IllegalStateException("boom"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> DateConversions.toDate("any", custom));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("Custom Converter – returns null (should result in IllegalArgumentException)")
    void testCustomConverterReturnsNull() {
        Converter custom = mock(Converter.class);
        when(custom.convert(eq("valid"), any())).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate("valid", custom));
    }

    @Test
    @DisplayName("Custom Converter – returns unsupported type (Boolean)")
    void testCustomConverterReturnsUnsupportedType() {
        Converter custom = mock(Converter.class);
        when(custom.convert(eq("valid"), any())).thenReturn(Boolean.TRUE);
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toDate("valid", custom));
    }
}
