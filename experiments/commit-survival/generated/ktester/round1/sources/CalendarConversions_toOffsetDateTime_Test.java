package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toOffsetDateTime_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    /**
     * Provides a stream of arguments representing the various calendar
     * configurations described in the JSON test cases.
     *
     * Each element is an {@code Object[]} with three entries:
     *   0 – a Calendar instance (or null) to be passed as the 'from' argument,
     *   1 – the expected result (OffsetDateTime or an exception class),
     *   2 – a short description used for the test name.
     */
    private static Stream<Arguments> calendarProvider() {
        List<Arguments> args = new ArrayList<>();
        // 1. Standard UTC conversion
        {
            Calendar cal = GregorianCalendar.from(ZonedDateTime.of(2023, 1, 15, 10, 30, 45, 123_000_000, ZoneOffset.UTC));
            OffsetDateTime expected = OffsetDateTime.of(2023, 1, 15, 10, 30, 45, 123_000_000, ZoneOffset.UTC);
            args.add(Arguments.of(cal, expected, "UTC standard conversion"));
        }
        // 2. America/New_York (EDT, UTC‑4) in summer
        {
            ZoneId zone = ZoneId.of("America/New_York");
            ZonedDateTime zdt = ZonedDateTime.of(2023, 6, 1, 8, 15, 0, 0, // 2023‑06‑01 08:15 EDT (UTC‑4)
            zone);
            Calendar cal = GregorianCalendar.from(zdt);
            OffsetDateTime expected = OffsetDateTime.of(2023, 6, 1, 8, 15, 0, 0, ZoneOffset.ofHours(-4));
            args.add(Arguments.of(cal, expected, "NY summer (EDT) conversion"));
        }
        // 3. Asia/Tokyo (JST, UTC+9) near year boundary
        {
            ZoneId zone = ZoneId.of("Asia/Tokyo");
            ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 999_000_000, zone);
            Calendar cal = GregorianCalendar.from(zdt);
            OffsetDateTime expected = OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 999_000_000, ZoneOffset.ofHours(9));
            args.add(Arguments.of(cal, expected, "Tokyo year‑end conversion"));
        }
        // 4. Epoch start UTC
        {
            Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(0L);
            OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
            args.add(Arguments.of(cal, expected, "Epoch start UTC"));
        }
        // 5. Null calendar – expect NullPointerException
        args.add(Arguments.of(null, NullPointerException.class, "Null calendar should throw NPE"));
        // 6. DST transition in Europe/Paris (spring forward)
        {
            ZoneId zone = ZoneId.of("Europe/Paris");
            // 2023‑03‑26 02:30 does not exist; Calendar normalises to 03:30 CEST (+02)
            ZonedDateTime zdt = ZonedDateTime.of(2023, 3, 26, 2, 30, 0, 0, zone);
            Calendar cal = GregorianCalendar.from(zdt);
            OffsetDateTime expected = OffsetDateTime.of(2023, 3, 26, 3, 30, 0, 0, ZoneOffset.ofHours(2));
            args.add(Arguments.of(cal, expected, "Paris spring forward normalisation"));
        }
        // 7. Leap day UTC
        {
            Calendar cal = GregorianCalendar.from(ZonedDateTime.of(2000, 2, 29, 12, 0, 0, 0, ZoneOffset.UTC));
            OffsetDateTime expected = OffsetDateTime.of(2000, 2, 29, 12, 0, 0, 0, ZoneOffset.UTC);
            args.add(Arguments.of(cal, expected, "Leap year UTC"));
        }
        // 8. America/New_York just before DST fall‑back (standard time)
        {
            ZoneId zone = ZoneId.of("America/New_York");
            // 2023‑11‑05 01:30 before the fallback is still EST (UTC‑5)
            ZonedDateTime zdt = ZonedDateTime.of(2023, 11, 5, 1, 30, 0, 0, zone).// forces the -05 offset
            withEarlierOffsetAtOverlap();
            Calendar cal = GregorianCalendar.from(zdt);
            OffsetDateTime expected = OffsetDateTime.of(2023, 11, 5, 1, 30, 0, 0, ZoneOffset.ofHours(-5));
            args.add(Arguments.of(cal, expected, "NY pre‑fallback EST"));
        }
        // 9. America/New_York just after DST fall‑back (daylight time)
        {
            ZoneId zone = ZoneId.of("America/New_York");
            // Same local time but the later offset (-04)
            ZonedDateTime zdt = ZonedDateTime.of(2023, 11, 5, 1, 30, 0, 0, zone).// forces the -04 offset
            withLaterOffsetAtOverlap();
            Calendar cal = GregorianCalendar.from(zdt);
            OffsetDateTime expected = OffsetDateTime.of(2023, 11, 5, 1, 30, 0, 0, ZoneOffset.ofHours(-4));
            args.add(Arguments.of(cal, expected, "NY post‑fallback EDT"));
        }
        // 10. Extreme negative offset (GMT‑12) before epoch
        {
            TimeZone tz = TimeZone.getTimeZone("GMT-12");
            Calendar cal = new GregorianCalendar(tz);
            // 1969‑12‑31T23:59:59.999‑12:00
            cal.setTimeInMillis(-1);
            OffsetDateTime expected = OffsetDateTime.of(1969, 12, 31, 23, 59, 59, 999_000_000, ZoneOffset.ofHours(-12));
            args.add(Arguments.of(cal, expected, "Extreme negative offset before epoch"));
        }
        // 11. Invalid date with lenient=false – expect IllegalArgumentException
        {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setLenient(false);
            // Setting an impossible date (Feb 30) will throw when we call getTime()
            cal.set(2023, Calendar.FEBRUARY, 30);
            args.add(Arguments.of(cal, IllegalArgumentException.class, "Invalid date with lenient=false should throw IAE"));
        }
        // 12. Custom offset +05:45 (Asia/Kathmandu)
        {
            // Build a custom TimeZone with raw offset +5h45m
            TimeZone tz = TimeZone.getTimeZone("GMT+05:45");
            Calendar cal = new GregorianCalendar(tz);
            cal.set(2023, Calendar.AUGUST, 15, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            OffsetDateTime expected = OffsetDateTime.of(2023, 8, 15, 0, 0, 0, 0, ZoneOffset.ofHoursMinutes(5, 45));
            args.add(Arguments.of(cal, expected, "Custom offset +05:45"));
        }
        // 13. Non‑Calendar object – expect ClassCastException
        args.add(Arguments.of("I am not a calendar", ClassCastException.class, "Non‑Calendar argument should throw CCE"));
        return args.stream();
    }

    @Test
    void toOffsetDateTime_WithUtcCalendar_ReturnsCorrectOffsetDateTime() {
        // GIVEN
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        GregorianCalendar utcCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(instant.toEpochMilli());
        // WHEN
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(utcCal, mockConverter);
        // THEN
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
        assertEquals(expected, result, "OffsetDateTime should match the instant with UTC offset");
    }

    @Test
    void toOffsetDateTime_WithPositiveOffsetCalendar_ReturnsCorrectOffsetDateTime() {
        // GIVEN
        // Choose a zone with a +02:00 offset (e.g., Europe/Berlin in winter)
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        // UTC instant
        Instant instant = Instant.parse("2023-06-15T08:00:00Z");
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(instant.toEpochMilli());
        // WHEN
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, mockConverter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(2);
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, expectedOffset);
        assertEquals(expected, result, "OffsetDateTime should reflect the calendar's +02:00 offset");
    }

    @Test
    void toOffsetDateTime_WithNegativeOffsetCalendar_ReturnsCorrectOffsetDateTime() {
        // GIVEN
        // Choose a zone with a -05:00 offset (e.g., America/New_York standard time)
        TimeZone tz = TimeZone.getTimeZone("GMT-05:00");
        // UTC instant
        Instant instant = Instant.parse("2023-12-01T15:30:00Z");
        GregorianCalendar cal = new GregorianCalendar(tz);
        cal.setTimeInMillis(instant.toEpochMilli());
        // WHEN
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(cal, mockConverter);
        // THEN
        ZoneOffset expectedOffset = ZoneOffset.ofHours(-5);
        OffsetDateTime expected = OffsetDateTime.ofInstant(instant, expectedOffset);
        assertEquals(expected, result, "OffsetDateTime should reflect the calendar's -05:00 offset");
    }

    @Test
    void toOffsetDateTime_NullCalendar_ThrowsClassCastException() {
        // GIVEN
        Object notACalendar = new Object();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toOffsetDateTime(notACalendar, mockConverter), "Passing a non‑Calendar object should result in a ClassCastException");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("calendarProvider")
    void toOffsetDateTime_VariousScenarios(Object from, Object expectedOrException, String description) {
        // WHEN / THEN
        if (expectedOrException instanceof Class) {
            // Expect an exception
            Class<?> exClass = (Class<?>) expectedOrException;
            assertThrows(exClass, () -> CalendarConversions.toOffsetDateTime(from, mockConverter), description);
        } else {
            // Normal conversion – expect an OffsetDateTime
            OffsetDateTime result = CalendarConversions.toOffsetDateTime(from, mockConverter);
            OffsetDateTime expected = (OffsetDateTime) expectedOrException;
            assertEquals(expected, result, description);
        }
    }
}
