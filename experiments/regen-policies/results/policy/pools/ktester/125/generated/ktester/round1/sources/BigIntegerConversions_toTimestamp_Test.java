package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toTimestamp_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – they provide the full set of built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test suites.
        converter = null;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// java.util.Date
        Arguments.of(new Date(1633072800000L), new Timestamp(1633072800000L), "Date → Timestamp (epoch millis 1633072800000)"), // java.sql.Timestamp (already a Timestamp)
        Arguments.of(Timestamp.valueOf("2022-01-15 12:34:56.789"), Timestamp.valueOf("2022-01-15 12:34:56.789"), "Timestamp → Timestamp (preserve nanoseconds)"), // java.time.Instant
        Arguments.of(Instant.parse("2023-03-10T08:15:30.123Z"), Timestamp.from(Instant.parse("2023-03-10T08:15:30.123Z")), "Instant → Timestamp"), // java.time.LocalDateTime (system default zone)
        Arguments.of(LocalDateTime.of(2021, 12, 31, 23, 59, 59, 999_000_000), Timestamp.from(LocalDateTime.of(2021, 12, 31, 23, 59, 59, 999_000_000).atZone(ZoneId.systemDefault()).toInstant()), "LocalDateTime → Timestamp (system default zone)"), // java.time.ZonedDateTime (explicit UTC)
        Arguments.of(ZonedDateTime.of(2020, 2, 29, 12, 0, 0, 0, ZoneId.of("UTC")), Timestamp.from(Instant.parse("2020-02-29T12:00:00Z")), "ZonedDateTime UTC → Timestamp"), // java.time.OffsetDateTime (offset -5 hours)
        Arguments.of(OffsetDateTime.of(2019, 5, 20, 6, 30, 0, 0, ZoneOffset.ofHours(-5)), Timestamp.from(Instant.parse("2019-05-20T11:30:00Z")), "OffsetDateTime -05:00 → Timestamp (normalized to UTC)"), // java.util.Calendar with GMT+2
        Arguments.of(calendarWith(2022, Calendar.JUNE, 15, 10, 20, 30, 250, TimeZone.getTimeZone("GMT+2")), Timestamp.from(Instant.parse("2022-06-15T08:20:30.250Z")), "Calendar GMT+2 → Timestamp (converted to UTC)"), // java.math.BigInteger (epoch millis)
        Arguments.of(BigInteger.valueOf(1672531199000L), Timestamp.from(Instant.ofEpochMilli(1672531199000L)), "BigInteger epoch millis → Timestamp"), // java.lang.Long (epoch seconds)
        Arguments.of(Long.valueOf(1609459200L), Timestamp.from(Instant.ofEpochSecond(1609459200L)), "Long epoch seconds → Timestamp"), // String ISO‑8601 instant
        Arguments.of("2024-07-04T14:45:00Z", Timestamp.from(Instant.parse("2024-07-04T14:45:00Z")), "String ISO‑8601 instant → Timestamp"), // java.time.LocalDate (system default zone, start of day)
        Arguments.of(LocalDate.of(2022, 5, 15), Timestamp.from(LocalDate.of(2022, 5, 15).atStartOfDay(ZoneId.systemDefault()).toInstant()), "LocalDate → Timestamp (start of day, system zone)"), // java.sql.Date
        Arguments.of(java.sql.Date.valueOf("2021-08-20"), Timestamp.from(Instant.parse("2021-08-20T00:00:00Z")), "java.sql.Date → Timestamp (midnight UTC)"), // BigInteger zero
        Arguments.of(BigInteger.ZERO, Timestamp.from(Instant.ofEpochMilli(0L)), "BigInteger zero → Timestamp (Unix epoch)"), // BigInteger negative
        Arguments.of(new BigInteger("-1000"), Timestamp.from(Instant.ofEpochMilli(-1000L)), "BigInteger negative → Timestamp (pre‑1970)"), // Long zero seconds
        Arguments.of(Long.valueOf(0L), Timestamp.from(Instant.ofEpochSecond(0L)), "Long zero seconds → Timestamp (Unix epoch)"), // Long negative seconds
        Arguments.of(Long.valueOf(-1L), Timestamp.from(Instant.ofEpochSecond(-1L)), "Long negative seconds → Timestamp (one second before epoch)"), // String date‑only ISO‑8601
        Arguments.of("2023-04-01", Timestamp.from(LocalDate.parse("2023-04-01").atStartOfDay(ZoneOffset.UTC).toInstant()), "String date‑only → Timestamp (midnight UTC)"), // OffsetDateTime with nanoseconds
        Arguments.of(OffsetDateTime.of(2022, 10, 5, 14, 30, 15, 123_456_789, ZoneOffset.ofHours(2)), Timestamp.from(Instant.parse("2022-10-05T12:30:15.123456789Z")), "OffsetDateTime with nanos → Timestamp (preserve nanos)"));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(// Generic Object
        Arguments.of(new Object(), IllegalArgumentException.class, "Object (no conversion path)"), // UUID (unsupported)
        Arguments.of(UUID.randomUUID(), IllegalArgumentException.class, "UUID (unsupported type)"), // LocalTime (no date/zone)
        Arguments.of(LocalTime.of(14, 30, 15, 123_456_789), IllegalArgumentException.class, "LocalTime (cannot be converted to an instant)"), // Duration (represents amount, not instant)
        Arguments.of(Duration.ofHours(5).plusMinutes(30), IllegalArgumentException.class, "Duration (not an instant)"), // BigDecimal (unsupported numeric type)
        Arguments.of(new java.math.BigDecimal("1672531199000"), IllegalArgumentException.class, "BigDecimal (unsupported numeric type)"), // Integer (unsupported numeric type)
        Arguments.of(Integer.valueOf(1620000), IllegalArgumentException.class, "Integer (unsupported numeric type)"), // String with invalid format
        Arguments.of("not-a-date", DateTimeParseException.class, "Invalid date string"), // String time‑only format
        Arguments.of("13:45:00", DateTimeParseException.class, "Time‑only string (no date)"), // Calendar with null TimeZone
        Arguments.of(calendarWithNullTimeZone(), NullPointerException.class, "Calendar with null TimeZone"));
    }

    /**
     * Creates a {@link Calendar} pre‑populated with the supplied date‑time fields and time‑zone.
     */
    private static Calendar calendarWith(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Returns a {@link Calendar} whose time‑zone has been explicitly set to {@code null}.
     */
    private static Calendar calendarWithNullTimeZone() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2022, Calendar.JANUARY, 1, 0, 0, 0);
        // deliberately set a null time‑zone (will cause NPE in conversion)
        cal.setTimeZone(null);
        return cal;
    }

    /**
     * Verify that a {@link Date} is correctly converted to a {@link Timestamp}.
     */
    @Test
    void toTimestamp_fromDate_returnsEquivalentTimestamp() {
        // GIVEN
        // current moment
        Date source = new Date();
        Timestamp expected = new Timestamp(source.getTime());
        // WHEN
        Timestamp actual = BigIntegerConversions.toTimestamp(source, converter);
        // THEN
        assertEquals(expected, actual, "Timestamp from Date should match expected value");
    }

    /**
     * Verify that a {@link Long} representing epoch milliseconds is correctly converted.
     */
    @Test
    void toTimestamp_fromLong_returnsEquivalentTimestamp() {
        // GIVEN
        long epochMillis = System.currentTimeMillis();
        Long source = Long.valueOf(epochMillis);
        Timestamp expected = new Timestamp(epochMillis);
        // WHEN
        Timestamp actual = BigIntegerConversions.toTimestamp(source, converter);
        // THEN
        assertEquals(expected, actual, "Timestamp from Long epoch millis should match expected value");
    }

    /**
     * Verify that a {@link BigInteger} representing epoch milliseconds is correctly converted.
     */
    @Test
    void toTimestamp_fromBigInteger_returnsEquivalentTimestamp() {
        // GIVEN
        long epochMillis = System.currentTimeMillis();
        BigInteger source = BigInteger.valueOf(epochMillis);
        Timestamp expected = new Timestamp(epochMillis);
        // WHEN
        Timestamp actual = BigIntegerConversions.toTimestamp(source, converter);
        // THEN
        assertEquals(expected, actual, "Timestamp from BigInteger epoch millis should match expected value");
    }

    /**
     * Verify that a {@link Instant} is correctly converted.
     */
    @Test
    void toTimestamp_fromInstant_returnsEquivalentTimestamp() {
        // GIVEN
        Instant now = Instant.now();
        Timestamp expected = Timestamp.from(now);
        // WHEN
        Timestamp actual = BigIntegerConversions.toTimestamp(now, converter);
        // THEN
        assertEquals(expected, actual, "Timestamp from Instant should match expected value");
    }

    /**
     * Verify that a {@code null} source yields {@code null} (the Converter returns null for object types).
     */
    @Test
    void toTimestamp_fromNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Timestamp actual = BigIntegerConversions.toTimestamp(source, converter);
        // THEN
        assertNull(actual, "Converting null should result in null Timestamp");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulConversionProvider")
    void toTimestamp_successfulConversions(Object source, Timestamp expected, String description) {
        // WHEN
        Timestamp actual = BigIntegerConversions.toTimestamp(source, converter);
        // THEN
        assertEquals(expected, actual, "Conversion mismatch: " + description);
    }

    @Test
    void toTimestamp_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toTimestamp(null, converter), "Converting a null source should throw NullPointerException");
    }

    @Test
    void toTimestamp_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toTimestamp(Instant.now(), null), "Passing a null Converter should throw NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("unsupportedSourceProvider")
    void toTimestamp_unsupportedSource_throwsException(Object source, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigIntegerConversions.toTimestamp(source, converter), "Unsupported source should raise exception: " + description);
    }

    @Test
    void toTimestamp_zonedDateTimeDstGap_throwsDateTimeException() {
        // The local time 2021‑03‑14T02:30 does not exist in America/New_York
        assertThrows(DateTimeException.class, () -> {
            ZonedDateTime zdt = ZonedDateTime.of(2021, 3, 14, 2, 30, 0, 0, ZoneId.of("America/New_York"));
            // The above line itself throws, but we keep the call to the method for completeness
            BigIntegerConversions.toTimestamp(zdt, converter);
        }, "ZonedDateTime in DST gap should throw DateTimeException");
    }

    @Test
    void toTimestamp_zonedDateTimeInvalidZone_throwsDateTimeException() {
        assertThrows(DateTimeException.class, () -> {
            ZonedDateTime.of(2022, 5, 10, 12, 0, 0, 0, ZoneId.of("Invalid/Zone"));
        }, "Creating ZonedDateTime with invalid zone should throw DateTimeException");
    }
}
