package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class LocalDateTimeConversions_toLocalTime_Test {

    /**
     * Converter instance used for all tests.
     */
    private Converter converter;

    /**
     * Converter used for the majority of the tests (real implementation).
     */
    private Converter realConverter;

    /**
     * Original system zone – restored after the whole test run.
     */
    private static ZoneId originalZone;

    @BeforeAll
    static void beforeAll() {
        // Preserve the original zone and force UTC for deterministic results.
        originalZone = ZoneId.systemDefault();
        System.setProperty("user.timezone", "UTC");
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter works out‑of‑the‑box for the
        // standard JDK date‑time types.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // The real Converter works out‑of‑the‑box for the standard JDK date‑time types.
        ConverterOptions options = new ConverterOptions();
        realConverter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // The Converter does not hold external resources, but we clear the reference
        // to aid garbage collection.
        converter = null;
        realConverter = null;
    }

    @AfterAll
    static void afterAll() {
        // Restore the original zone.
        System.setProperty("user.timezone", originalZone.getId());
        TimeZone.setDefault(TimeZone.getTimeZone(originalZone));
    }

    private static Stream<Arguments> successCases() {
        // Calendar in UTC – 10:20:30.123
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.clear();
        utcCal.set(2023, Calendar.MAY, 17, 10, 20, 30);
        utcCal.set(Calendar.MILLISECOND, 123);
        LocalTime utcCalExpected = LocalTime.of(10, 20, 30, 123_000_000);
        // Calendar in America/New_York – 07:15:30.250
        Calendar nyCal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCal.clear();
        nyCal.set(2023, Calendar.MAY, 17, 7, 15, 30);
        nyCal.set(Calendar.MILLISECOND, 250);
        LocalTime nyCalExpected = LocalTime.of(7, 15, 30, 250_000_000);
        return Stream.of(// LocalDateTime
        arguments(LocalDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000), LocalTime.of(14, 30, 45, 123_000_000)), // ZonedDateTime with offset -5
        arguments(ZonedDateTime.of(2023, 5, 17, 9, 15, 0, 0, ZoneOffset.ofHours(-5)), LocalTime.of(9, 15)), // OffsetDateTime
        arguments(OffsetDateTime.of(2023, 5, 17, 22, 0, 0, 0, ZoneOffset.ofHours(2)), LocalTime.of(22, 0)), // java.sql.Timestamp
        arguments(Timestamp.valueOf("2023-05-17 23:59:59.999"), LocalTime.of(23, 59, 59, 999_000_000)), // java.util.Date (instant based)
        arguments(Date.from(Instant.parse("2023-05-17T12:00:00Z")), LocalTime.of(12, 0)), // Calendar UTC
        arguments(utcCal, utcCalExpected), // ISO_LOCAL_DATE_TIME string
        arguments("2023-05-17T08:45:30", LocalTime.of(8, 45, 30)), // Long epoch millis
        arguments(123_456_789L, LocalTime.ofInstant(Instant.ofEpochMilli(123_456_789L), ZoneOffset.UTC).toLocalTime()), // BigDecimal epoch seconds
        arguments(new BigDecimal("1622548800"), LocalTime.ofInstant(Instant.ofEpochSecond(1622548800L), ZoneOffset.UTC).toLocalTime()), // BigInteger epoch millis
        arguments(new BigInteger("1622548800000"), LocalTime.ofInstant(Instant.ofEpochMilli(1622548800000L), ZoneOffset.UTC).toLocalTime()), // java.sql.Date (midnight)
        arguments(java.sql.Date.valueOf("2023-05-17"), LocalTime.of(0, 0)), // LocalDate (midnight)
        arguments(LocalDate.of(2023, 5, 17), LocalTime.of(0, 0)), // Instant UTC
        arguments(Instant.parse("2023-05-17T14:30:45.123Z"), LocalTime.of(14, 30, 45, 123_000_000)), // String with offset
        arguments("2023-05-17T08:45:30+02:00", LocalTime.of(8, 45, 30)), // ISO_LOCAL_TIME string
        arguments("08:45:30", LocalTime.of(8, 45, 30)), // BigDecimal with fractional seconds
        arguments(new BigDecimal("1622548800.5"), LocalTime.ofInstant(Instant.ofEpochSecond(1622548800L, 500_000_000L), ZoneOffset.UTC).toLocalTime()), // BigInteger epoch seconds
        arguments(new BigInteger("1622548800"), LocalTime.ofInstant(Instant.ofEpochSecond(1622548800L), ZoneOffset.UTC).toLocalTime()), // Calendar New York
        arguments(nyCal, nyCalExpected), // OffsetTime
        arguments(OffsetTime.of(13, 20, 15, 0, ZoneOffset.ofHours(-3)), LocalTime.of(13, 20, 15)), // Long (different value) – same logic as earlier
        arguments(987_654_321L, LocalTime.ofInstant(Instant.ofEpochMilli(987_654_321L), ZoneOffset.UTC).toLocalTime()), // LocalDateTime with a real (non‑null) converter – should still work
        arguments(// source
        LocalDateTime.of(2023, 5, 17, 0, 0, 0, 0), // expected
        LocalTime.of(0, 0)));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null source -> NullPointerException (as per toZonedDateTime contract)
        arguments(null, null, NullPointerException.class), // Unsupported object type
        arguments(new Object(), null, ClassCastException.class), // ZoneId unsupported
        arguments(ZoneId.of("UTC"), null, ClassCastException.class), // Duration unsupported
        arguments(Duration.ofHours(5), null, ClassCastException.class), // Double unsupported
        arguments(Double.valueOf(12345.67), null, ClassCastException.class), // Array unsupported
        arguments(new Object[] { 1, 2, 3 }, null, ClassCastException.class), // Empty string
        arguments("", null, DateTimeParseException.class), // Invalid month in string
        arguments("2023-13-01T00:00:00", null, DateTimeParseException.class), // Long.MAX_VALUE epoch millis out of range
        arguments(Long.MAX_VALUE, null, DateTimeException.class), // BigInteger(Long.MAX_VALUE) epoch millis out of range
        arguments(new BigInteger(Long.toString(Long.MAX_VALUE)), null, DateTimeException.class), // Integer unsupported
        arguments(Integer.valueOf(987654321), null, ClassCastException.class), // Malformed date‑time string
        arguments("not-a-date-time", null, DateTimeParseException.class));
    }

    @Test
    void shouldConvertFromLocalDateTime() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2023, Month.MAY, 10, 9, 15, 30);
        LocalTime expected = LocalTime.of(9, 15, 30);
        // WHEN
        LocalTime actual = LocalDateTimeConversions.toLocalTime(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertFromInstant() {
        // GIVEN
        // Instant representing 2023‑05‑10T14:30:15Z
        Instant source = Instant.parse("2023-05-10T14:30:15Z");
        // The conversion uses the system default zone; to keep the test deterministic,
        // we force the default zone to UTC for the duration of this test.
        ZoneId originalZone = ZoneId.systemDefault();
        try {
            System.setProperty("user.timezone", "UTC");
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            LocalTime expected = LocalTime.of(14, 30, 15);
            // WHEN
            LocalTime actual = LocalDateTimeConversions.toLocalTime(source, converter);
            // THEN
            assertEquals(expected, actual);
        } finally {
            // Restore original zone
            System.setProperty("user.timezone", originalZone.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(originalZone));
        }
    }

    @Test
    void shouldConvertFromDate() {
        // GIVEN
        // java.util.Date representing 2023‑08‑15T12:34:56 in the system default zone.
        // We create it via an Instant to avoid locale‑dependent arithmetic.
        Instant instant = Instant.parse("2023-08-15T12:34:56Z");
        Date source = Date.from(instant);
        // Expected time is 12:34:56 (UTC). As with the Instant test we enforce UTC.
        ZoneId originalZone = ZoneId.systemDefault();
        try {
            System.setProperty("user.timezone", "UTC");
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            LocalTime expected = LocalTime.of(12, 34, 56);
            // WHEN
            LocalTime actual = LocalDateTimeConversions.toLocalTime(source, converter);
            // THEN
            assertEquals(expected, actual);
        } finally {
            System.setProperty("user.timezone", originalZone.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(originalZone));
        }
    }

    @Test
    void shouldConvertFromString() {
        // GIVEN
        // The Converter can parse an ISO‑8601 date‑time string.
        String source = "2023-12-01T07:45:20";
        LocalTime expected = LocalTime.of(7, 45, 20);
        // WHEN
        LocalTime actual = LocalDateTimeConversions.toLocalTime(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalTime actual = LocalDateTimeConversions.toLocalTime(source, converter);
        // THEN
        assertNull(actual);
    }

    /* -------------------------------------------------------------
     *  Successful conversion scenarios (parameterised)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successCases")
    void shouldConvertToLocalTime(Object source, LocalTime expected) {
        LocalTime actual = LocalDateTimeConversions.toLocalTime(source, null);
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  Conversion scenarios that involve a non‑null Converter
     * ------------------------------------------------------------- */
    @Test
    void shouldConvertWithRealConverter() {
        // Using a real Converter does not change the result.
        LocalDateTime source = LocalDateTime.of(2023, 5, 17, 0, 0);
        LocalTime expected = LocalTime.of(0, 0);
        LocalTime actual = LocalDateTimeConversions.toLocalTime(source, realConverter);
        assertEquals(expected, actual);
    }

    @Test
    void shouldPropagateExceptionFromThrowingConverter() {
        // A mock Converter that throws RuntimeException on any call.
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("Mock conversion failure");
        });
        LocalDateTime source = LocalDateTime.of(2023, 5, 17, 12, 0);
        assertThrows(RuntimeException.class, () -> LocalDateTimeConversions.toLocalTime(source, throwingConverter));
    }

    /* -------------------------------------------------------------
     *  Exception / error handling scenarios (parameterised)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionCases")
    void shouldThrowExpectedException(Object source, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateTimeConversions.toLocalTime(source, converter));
    }
}
