package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toMap_Test {

    /**
     * The mock {@link Converter} that is passed to the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A reusable {@link Calendar} instance used by the tests.
     */
    private Calendar calendar;

    /**
     * Fixed epoch milliseconds for the test calendar (2023‑01‑15T12:34:56.789Z).
     */
    private static final long FIXED_EPOCH_MILLIS = 1673792096789L;

    @BeforeEach
    void setupBeforeEach() {
        // Build a GregorianCalendar with a known instant and a non‑UTC time‑zone to verify the zone entry.
        calendar = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        calendar.setTimeInMillis(FIXED_EPOCH_MILLIS);
        // The mockConverter is injected by MockitoExtension; no stubbing is required because
        // CalendarConversions.toMap does not interact with it.
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the calendar reference for GC friendliness.
        calendar = null;
    }

    /**
     * Helper that creates a {@link Calendar} with the given epoch millis and time‑zone.
     */
    private static Calendar calendarOf(long epochMillis, String zoneId) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone(zoneId));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Data provider for the valid conversion test.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// 2022‑01‑15 10:20:30.456 UTC
        Arguments.of("Standard UTC conversion", calendarOf(1642249230456L, "UTC"), "2022-01-15", "10:20:30.456000000", "UTC", 1642249230456L), // 2021‑01‑01 00:00:00.000 GMT (month index 0)
        Arguments.of("Zero‑based month handling (January)", calendarOf(1609459200000L, "GMT"), "2021-01-01", "00:00:00.000000000", "GMT", 1609459200000L), // 2023‑07‑04 15:45:12.789 America/New_York
        Arguments.of("Non‑UTC zone America/New_York", calendarOf(1688501112789L, "America/New_York"), "2023-07-04", "15:45:12.789000000", "America/New_York", 1688501112789L), // 2000‑02‑29 23:59:59.000 UTC (leap day)
        Arguments.of("Leap day with zero milliseconds", calendarOf(951868799000L, "UTC"), "2000-02-29", "23:59:59.000000000", "UTC", 951868799000L), // 2021‑12‑31 23:59:59.999 UTC (month index 11)
        Arguments.of("End of year, December, max millisecond", calendarOf(1640995199999L, "UTC"), "2021-12-31", "23:59:59.999000000", "UTC", 1640995199999L), // 1969‑12‑31 23:59:59.000 UTC (pre‑epoch)
        Arguments.of("Negative epoch (one second before 1970)", calendarOf(-1000L, "UTC"), "1969-12-31", "23:59:59.000000000", "UTC", -1000L), // 2022‑08‑15 08:15:30.005 GMT+05:30
        Arguments.of("Custom offset zone GMT+05:30", calendarOf(1660517730005L, "GMT+05:30"), "2022-08-15", "08:15:30.005000000", "GMT+05:30", 1660517730005L), // 2022‑03‑27 12:34:56.123 America/Los_Angeles
        Arguments.of("IANA zone America/Los_Angeles", calendarOf(1648350896123L, "America/Los_Angeles"), "2022-03-27", "12:34:56.123000000", "America/Los_Angeles", 1648350896123L), // 2022‑01‑01 00:00:00.007 GMT (single‑digit millisecond)
        Arguments.of("Single‑digit millisecond padding", calendarOf(1640995200007L, "GMT"), "2022-01-01", "00:00:00.007000000", "GMT", 1640995200007L));
    }

    /**
     * Helper to create a Calendar with arbitrary field values (may be invalid).
     */
    private static Calendar calendarWithFields(int year, int monthZeroBased, int day, int hour, int minute, int second, int millisecond, String zoneId) {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone(zoneId));
        // force DateTimeException on illegal values
        cal.setLenient(false);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, monthZeroBased);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, millisecond);
        // Trigger validation by calling getTimeInMillis()
        try {
            cal.getTimeInMillis();
        } catch (Exception ignored) {
            // Validation exception is expected for some test cases.
        }
        return cal;
    }

    /**
     * Data provider for invalid field scenarios.
     */
    private static Stream<Arguments> invalidFieldProvider() {
        return Stream.of(Arguments.of("Month value 13 (out of range)", // month 12 -> 13 after +1
        calendarWithFields(2022, 12, 15, 10, 20, 30, 456, "UTC")), Arguments.of("February 31st (invalid day)", // Feb 31
        calendarWithFields(2021, 1, 31, 0, 0, 0, 0, "GMT")), Arguments.of("Hour value 24 (invalid hour)", calendarWithFields(2022, 0, 15, 24, 0, 0, 0, "UTC")), Arguments.of("Minute value 60 (invalid minute)", calendarWithFields(2022, 0, 15, 10, 60, 0, 0, "UTC")), Arguments.of("Second value 60 (invalid second)", calendarWithFields(2022, 0, 15, 10, 20, 60, 0, "UTC")), Arguments.of("Millisecond value 1000 (invalid nanosecond range)", calendarWithFields(2022, 0, 15, 10, 20, 30, 1000, "UTC")), Arguments.of("Negative month -1 (after +1 becomes 0)", calendarWithFields(2022, -2, 15, 10, 20, 30, 456, "UTC")));
    }

    @Test
    void toMap_returnsMapWithCorrectDateTimeAndZone() {
        // GIVEN – a Calendar instance (prepared in @BeforeEach) and a mock Converter.
        // WHEN
        Map<String, Object> result = CalendarConversions.toMap(calendar, mockConverter);
        // THEN – verify map contains the expected entries.
        assertNotNull(result, "Resulting map must not be null");
        // Expected date string (year-month-day) – note that Calendar.MONTH is zero‑based.
        String expectedDate = java.time.LocalDate.of(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)).toString();
        // Expected time string (hour:minute:second.nanos)
        String expectedTime = java.time.LocalTime.of(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND), // convert ms → ns
        calendar.get(Calendar.MILLISECOND) * 1_000_000).toString();
        // Expected zone ID string.
        String expectedZone = calendar.getTimeZone().toZoneId().toString();
        // Expected epoch millis.
        long expectedEpoch = calendar.getTimeInMillis();
        assertEquals(expectedDate, result.get(MapConversions.DATE), "DATE entry mismatch");
        assertEquals(expectedTime, result.get(MapConversions.TIME), "TIME entry mismatch");
        assertEquals(expectedZone, result.get(MapConversions.ZONE), "ZONE entry mismatch");
        assertEquals(expectedEpoch, result.get(MapConversions.EPOCH_MILLIS), "EPOCH_MILLIS entry mismatch");
    }

    @Test
    void toMap_handlesUtcZoneCorrectly() {
        // GIVEN – a calendar set to UTC.
        Calendar utcCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(FIXED_EPOCH_MILLIS);
        // WHEN
        Map<String, Object> result = CalendarConversions.toMap(utcCal, mockConverter);
        // THEN
        assertEquals("UTC", result.get(MapConversions.ZONE), "Zone should be UTC");
        assertEquals(FIXED_EPOCH_MILLIS, result.get(MapConversions.EPOCH_MILLIS), "Epoch millis should match input");
    }

    /**
     * --------------------------------------------------------------------- *
     *  Parameterised tests for all *valid* conversion scenarios.
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toMap_validConversions(String description, Calendar inputCal, String expDate, String expTime, String expZone, long expEpoch) {
        // WHEN
        Map<String, Object> result = CalendarConversions.toMap(inputCal, mockConverter);
        // THEN
        assertAll("result map", () -> assertEquals(expDate, result.get(MapConversions.DATE), "DATE mismatch"), () -> assertEquals(expTime, result.get(MapConversions.TIME), "TIME mismatch"), () -> assertEquals(expZone, result.get(MapConversions.ZONE), "ZONE mismatch"), () -> assertEquals(expEpoch, result.get(MapConversions.EPOCH_MILLIS), "EPOCH_MILLIS mismatch"));
    }

    /**
     * --------------------------------------------------------------------- *
     *  Tests for invalid inputs that should result in exceptions.
     *  ---------------------------------------------------------------------
     */
    @Test
    void toMap_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toMap(null, mockConverter), "Passing null as 'from' must throw NPE");
    }

    @Test
    void toMap_nonCalendarFrom_throwsClassCastException() {
        Date notACalendar = new Date();
        assertThrows(ClassCastException.class, () -> CalendarConversions.toMap(notACalendar, mockConverter), "Passing a non‑Calendar object must throw CCE");
    }

    @Test
    void toMap_calendarWithNullTimeZone_throwsNullPointerException() {
        // Calendar.getInstance(null) yields a Calendar whose TimeZone defaults to the JVM default.
        // To force a null TimeZone we use reflection (allowed per requirements).
        Calendar cal = Calendar.getInstance();
        try {
            java.lang.reflect.Field tzField = Calendar.class.getDeclaredField("zone");
            tzField.setAccessible(true);
            tzField.set(cal, null);
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        assertThrows(NullPointerException.class, () -> CalendarConversions.toMap(cal, mockConverter), "Calendar with null TimeZone should cause NPE when calling toZoneId()");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidFieldProvider")
    void toMap_invalidCalendarFields_throwsDateTimeException(String description, Calendar badCal) {
        assertThrows(DateTimeException.class, () -> CalendarConversions.toMap(badCal, mockConverter), description);
    }
}
