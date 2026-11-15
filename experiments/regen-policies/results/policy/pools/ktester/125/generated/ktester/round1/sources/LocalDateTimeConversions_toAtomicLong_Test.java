package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toAtomicLong_Test {

    private Converter converter;

    // -----------------------------------------------------------------
    private static class CustomObject {
        // No fields – just a distinct type for the test.
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter can be created with default options – it contains all built‑in conversions
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Primitive wrappers
        arguments(12345, 12345L), arguments(9876543210L, 9876543210L), // Numeric strings (including whitespace)
        arguments("777777", 777777L), arguments("  777777  ", 777777L), // ISO‑8601 date‑time strings
        arguments("2023-08-15T12:34:56", 1692105296000L), arguments("2023-08-15T12:34:56Z", 1692105296000L), arguments("2023-08-15T12:34:56+02:00", 1692098096000L), arguments("2023-08-15T12:34:56[Europe/Paris]", 1692098096000L), arguments("2023-08-15T12:34:56-04:00[America/New_York]", 1692113296000L), // ISO‑8601 date only / time only strings
        arguments("2023-08-15", 1692057600000L), arguments("12:34:56", 45296000L), // Direct epoch‑millisecond long
        arguments(1625097600000L, 1625097600000L), // Floating point numbers (truncation)
        arguments(123.456, 123L), arguments(123.456f, 123L), // BigDecimal / BigInteger
        arguments(new BigDecimal("123.456"), 123L), arguments(new BigDecimal("1234567890.987"), 1234567890L), arguments(new BigInteger("1234567890123456789"), 1234567890123456789L), // Date / Timestamp / Instant
        arguments(new Date(1692105296000L), 1692105296000L), arguments(new Timestamp(1692105296000L), 1692105296000L), arguments(Instant.ofEpochMilli(1692105296000L), 1692105296000L), // java.time types
        arguments(LocalDate.of(2023, 8, 15), 1692057600000L), arguments(LocalTime.of(12, 34, 56), 45296000L), arguments(LocalDateTime.of(2023, 8, 15, 12, 34, 56), 1692105296000L), arguments(OffsetDateTime.of(2023, 8, 15, 12, 34, 56, 0, ZoneOffset.ofHours(2)), 1692098096000L), arguments(ZonedDateTime.of(LocalDateTime.of(2023, 8, 15, 12, 34, 56), ZoneId.of("Europe/Paris")), 1692098096000L), // Calendar set to UTC
        arguments(utcCalendar(1692105296000L), 1692105296000L), // Boolean values
        arguments(Boolean.TRUE, 1L), arguments(Boolean.FALSE, 0L));
    }

    private static Calendar utcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    private static Stream<Arguments> customConverterProvider() {
        // Mock that returns a fixed long value (5555) for the given source object
        Converter customMock = mock(Converter.class);
        when(customMock.convert(any(), eq(Long.class))).thenReturn(5555L);
        // Mock that simply delegates to the real converter (used for DummyConverter case)
        Converter dummyMock = mock(Converter.class);
        // Let the real converter handle the conversion – we don’t need to stub anything.
        return Stream.of(arguments(new CustomObject(), customMock, 5555L), // DummyConverter – the source is an Integer, conversion should ignore the converter
        arguments(42, dummyMock, 42L));
    }

    private static Stream<Arguments> exceptionProvider() {
        // Mock that throws IllegalStateException
        Converter throwingMock = mock(Converter.class);
        when(throwingMock.convert(any(), eq(Long.class))).thenThrow(new IllegalStateException("conversion failed"));
        // Mock that returns null (causing NPE later)
        Converter nullReturningMock = mock(Converter.class);
        when(nullReturningMock.convert(any(), eq(Long.class))).thenReturn(null);
        return Stream.of(// Null source – library’s toLong throws NPE
        arguments(null, NullPointerException.class), // Non‑numeric string
        arguments("notANumber", NumberFormatException.class), // Empty string
        arguments("", NumberFormatException.class), // Numeric string out of long range
        arguments("9223372036854775808", NumberFormatException.class), // Invalid ISO‑8601 date strings
        arguments("2023-02-30", DateTimeParseException.class), arguments("2023-08-15T25:00:00", DateTimeParseException.class), // Unsupported source type
        arguments(new Object(), ClassCastException.class), // BigInteger overflow
        arguments(new BigInteger("9223372036854775808"), ArithmeticException.class), // BigDecimal overflow
        arguments(new BigDecimal("9223372036854775808"), ArithmeticException.class), // Converter that throws an exception
        arguments("123", IllegalStateException.class, throwingMock), // Converter that returns null
        arguments(new CustomObject(), NullPointerException.class, nullReturningMock));
    }

    // Helper overload for the two‑argument exception provider (no custom converter)
    private static Stream<Arguments> exceptionProvider() {
        return Stream.concat(// Cases that use the real converter
        Stream.of(arguments(null, NullPointerException.class), arguments("notANumber", NumberFormatException.class), arguments("", NumberFormatException.class), arguments("9223372036854775808", NumberFormatException.class), arguments("2023-02-30", DateTimeParseException.class), arguments("2023-08-15T25:00:00", DateTimeParseException.class), arguments(new Object(), ClassCastException.class), arguments(new BigInteger("9223372036854775808"), ArithmeticException.class), arguments(new BigDecimal("9223372036854775808"), ArithmeticException.class)), // Cases that require a custom mock converter
        Stream.of(arguments("123", IllegalStateException.class, mockThrowingConverter()), arguments(new CustomObject(), NullPointerException.class, mockNullReturningConverter())));
    }

    // -----------------------------------------------------------------
    private static Converter mockThrowingConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Long.class))).thenThrow(new IllegalStateException("conversion failed"));
        return mock;
    }

    private static Converter mockNullReturningConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Long.class))).thenReturn(null);
        return mock;
    }

    @Test
    void toAtomicLongFromLong() {
        Long source = 123L;
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    void toAtomicLongFromString() {
        String source = "456";
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(456L, result.get());
    }

    @Test
    void toAtomicLongFromLocalDateTime() {
        LocalDateTime source = LocalDateTime.of(2023, Month.MARCH, 15, 10, 30, 45);
        // The conversion should treat the LocalDateTime as epoch seconds (default behavior of the library)
        long expected = source.toInstant(ZoneOffset.UTC).toEpochMilli();
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicLongFromDate() {
        // a fixed epoch milli
        Date source = new Date(1_600_000_000_000L);
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(1_600_000_000_000L, result.get());
    }

    @Test
    void toAtomicLongFromNull() {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(null, converter);
        assertNotNull(result);
        // Converter returns 0 for numeric primitives when source is null
        assertEquals(0L, result.get());
    }

    @Test
    void toAtomicLongFromInstant() {
        Instant source = Instant.now();
        long expected = source.toEpochMilli();
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicLongFromLocalTime() {
        // LocalTime is converted to the number of nanos of the day; the library treats that as a long value
        LocalTime source = LocalTime.of(12, 34, 56, 123_456_789);
        long expected = source.toNanoOfDay();
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (no custom converter)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("successfulConversionProvider")
    void testToAtomicLongSuccess(Object source, long expected) {
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    /* --------------------------------------------------------------
     *  Successful conversion with a custom Converter mock
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: custom converter – {2}")
    @MethodSource("customConverterProvider")
    void testToAtomicLongWithCustomConverter(Object source, Converter mockConverter, long expected) {
        // Replace the real converter with the mock for this invocation
        AtomicLong result = LocalDateTimeConversions.toAtomicLong(source, mockConverter);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    /* --------------------------------------------------------------
     *  Exception test cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: expects {1}")
    @MethodSource("exceptionProvider")
    void testToAtomicLongExceptions(Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateTimeConversions.toAtomicLong(source, converter));
    }
}
