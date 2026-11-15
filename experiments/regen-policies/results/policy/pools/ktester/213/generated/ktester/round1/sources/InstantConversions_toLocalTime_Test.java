package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLocalTime_Test {

    private static Converter converter;

    private static TimeZone originalTimeZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Force a known default time‑zone so that conversions that rely on it are deterministic
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // The Converter needs a ConverterOptions instance – the default constructor is sufficient
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default time‑zone
        TimeZone.setDefault(originalTimeZone);
        converter = null;
    }

    /**
     * Provides {@link Instant} test data.
     */
    private static Stream<Arguments> instantConversionProvider() {
        return Stream.of(Arguments.of(Instant.ofEpochSecond(0), LocalTime.of(0, 0, 0), "Convert epoch Instant (1970‑01‑01T00:00:00Z) to LocalTime"), Arguments.of(Instant.parse("2021-10-01T15:45:30Z"), LocalTime.of(15, 45, 30), "Convert specific Instant (2021‑10‑01T15:45:30Z) to LocalTime"));
    }

    /**
     * Provides {@link Date}, {@link Timestamp} and {@link Calendar} test data.
     */
    private static Stream<Arguments> dateAndCalendarConversionProvider() {
        // java.util.Date representing the epoch
        Date epochDate = new Date(0L);
        // java.sql.Timestamp with nanosecond part (the conversion should drop nanos)
        Timestamp ts = new Timestamp(Instant.parse("2021-10-01T15:45:30.123Z").toEpochMilli());
        // Calendar set to 2022‑03‑15T08:20:55Z in UTC
        Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.set(2022, Calendar.MARCH, 15, 8, 20, 55);
        utcCalendar.set(Calendar.MILLISECOND, 0);
        return Stream.of(Arguments.of(epochDate, LocalTime.of(0, 0, 0), "Convert java.util.Date representing epoch to LocalTime"), Arguments.of(ts, LocalTime.of(15, 45, 30), "Convert java.sql.Timestamp with nanoseconds to LocalTime (fractional part ignored)"), Arguments.of(utcCalendar, LocalTime.of(8, 20, 55), "Convert Calendar (UTC) set to 2022‑03‑15T08:20:55Z to LocalTime"));
    }

    @Test
    void toLocalTime_fromInstant() {
        Instant instant = Instant.parse("2023-01-01T12:34:56.000Z");
        LocalTime expected = LocalTime.of(12, 34, 56);
        LocalTime actual = InstantConversions.toLocalTime(instant, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 23, 45, 30, 0, ZoneId.of("UTC"));
        LocalTime expected = LocalTime.of(23, 45, 30);
        LocalTime actual = InstantConversions.toLocalTime(zdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2022, 12, 31, 08, 15, 0, 0, ZoneOffset.UTC);
        LocalTime expected = LocalTime.of(8, 15, 0);
        LocalTime actual = InstantConversions.toLocalTime(odt, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromDate() {
        // Date holds milliseconds since the epoch; use the same instant as above
        Date date = Date.from(Instant.parse("2023-01-01T12:34:56.000Z"));
        LocalTime expected = LocalTime.of(12, 34, 56);
        LocalTime actual = InstantConversions.toLocalTime(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalTime_fromLongMillis() {
        // Milliseconds since epoch for 2023‑01‑01T12:34:56Z
        long millis = Instant.parse("2023-01-01T12:34:56.000Z").toEpochMilli();
        LocalTime expected = LocalTime.of(12, 34, 56);
        LocalTime actual = InstantConversions.toLocalTime(millis, converter);
        assertEquals(expected, actual);
    }

    /**
     * Test conversion from {@link Instant} objects.
     *
     * Covers:
     *   - epoch instant (1970‑01‑01T00:00:00Z)
     *   - a specific instant with a non‑zero time component
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("instantConversionProvider")
    void toLocalTime_fromInstant_cases(Object from, LocalTime expected, String description) {
        LocalTime actual = InstantConversions.toLocalTime(from, converter);
        assertEquals(expected, actual, description);
    }

    /**
     * Test conversion from {@link Date}, {@link Timestamp} and {@link Calendar}
     * instances.
     *
     * Covers:
     *   - java.util.Date representing the epoch
     *   - java.sql.Timestamp with nanosecond precision (fractional seconds ignored)
     *   - java.util.Calendar set to a specific UTC date‑time
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("dateAndCalendarConversionProvider")
    void toLocalTime_fromDateAndCalendar_cases(Object from, LocalTime expected, String description) {
        LocalTime actual = InstantConversions.toLocalTime(from, converter);
        assertEquals(expected, actual, description);
    }
}
