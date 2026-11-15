package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.GregorianCalendar;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class CalendarConversions_toInstant_Test {

    /**
     * Converter instance required by the method signature (not used internally).
     */
    private static Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5/‑4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not affect CalendarConversions.toInstant, but a non‑null instance is required.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Calendar} with the supplied fields.
     * The month argument follows the usual 1‑based convention (January = 1).
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar months are zero‑based, hence the “- 1”.
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Convenience overload for UTC calendars.
     */
    private static Calendar utcCalendar(int year, int month, int day, int hour, int minute, int second, int millis) {
        return createCalendar(TimeZone.getTimeZone("UTC"), year, month, day, hour, minute, second, millis);
    }

    /**
     * Valid Calendar conversions (including subclass and ignored Converter).
     */
    static Stream<Arguments> validCalendarProvider() {
        return Stream.of(// Standard GregorianCalendar in UTC
        arguments(utcCalendar(2023, 1, 1, 0, 0, 0, 0), null, Instant.parse("2023-01-01T00:00:00Z"), "Standard GregorianCalendar in UTC should be converted to the exact same instant."), // Calendar with +02:00 offset – local time 2023‑01‑01T00:00:00 should become 2022‑12‑31T22:00:00Z
        arguments(createCalendar(TimeZone.getTimeZone("UTC+02:00"), 2023, 1, 1, 0, 0, 0, 0), null, Instant.parse("2022-12-31T22:00:00Z"), "Calendar with a +02:00 offset should produce an instant 2 hours earlier in UTC."), // Pre‑epoch date with millisecond precision
        arguments(utcCalendar(1960, 6, 15, 12, 30, 45, 123), null, Instant.parse("1960-06-15T12:30:45.123Z"), "Calendar representing a date before the Unix epoch should be converted correctly."), // Subclass of Calendar (GregorianCalendar is already a subclass, but we also test JapaneseImperialCalendar)
        arguments(new java.util.JapaneseImperialCalendar(TimeZone.getTimeZone("UTC")), null, Instant.parse("1970-01-01T00:00:00Z"), "A non‑Gregorian Calendar subclass should also be handled."), // Converter that throws on any method call – should be ignored
        arguments(utcCalendar(1970, 1, 1, 0, 0, 0, 0), Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be used");
        }), Instant.EPOCH, "Converter is ignored; even a throwing mock does not affect the result."));
    }

    /**
     * Time‑zone edge cases (UTC‑12 and UTC+14).
     */
    static Stream<Arguments> timeZoneEdgeProvider() {
        return Stream.of(// UTC‑12:00 offset – local midnight becomes 12:00 UTC same day
        arguments(createCalendar(TimeZone.getTimeZone("UTC-12:00"), 2023, 1, 1, 0, 0, 0, 0), Instant.parse("2023-01-01T12:00:00Z"), "Maximum negative offset should shift the instant forward by 12 hours."), // UTC+14:00 offset – local midnight becomes previous day 10:00 UTC
        arguments(createCalendar(TimeZone.getTimeZone("UTC+14:00"), 2023, 1, 1, 0, 0, 0, 0), Instant.parse("2022-12-31T10:00:00Z"), "Maximum positive offset should shift the instant backward by 14 hours."));
    }

    /**
     * Very large epoch milliseconds – far future and far past dates.
     */
    static Stream<Arguments> extremeDateProvider() {
        return Stream.of(// Year 3000‑01‑01T00:00:00Z
        arguments(utcCalendar(3000, 1, 1, 0, 0, 0, 0), Instant.parse("3000-01-01T00:00:00Z"), "Future date far beyond typical 32‑bit epoch range."), // Year 0001‑01‑01T00:00:00Z (before epoch)
        arguments(utcCalendar(1, 1, 1, 0, 0, 0, 0), Instant.parse("0001-01-01T00:00:00Z"), "Ancient date before 1970 with large negative epoch milliseconds."));
    }

    /**
     * Out‑of‑range Calendar dates – expect an exception from Calendar.toInstant().
     */
    static Stream<Arguments> outOfRangeProvider() {
        return Stream.of(// Far future beyond Instant.MAX (year 292278995)
        arguments(createCalendar(TimeZone.getTimeZone("UTC"), 292278995, 1, 1, 0, 0, 0, 0)), // Far past beyond Instant.MIN (year -292278994)
        arguments(createCalendar(TimeZone.getTimeZone("UTC"), -292278994, 1, 1, 0, 0, 0, 0)));
    }

    @Test
    void testToInstant_Tokyo() {
        Calendar cal = createCalendar(2023, 6, 15, 12, 30, 45, 123, TOKYO);
        // direct JDK conversion – serves as the oracle
        Instant expected = cal.toInstant();
        Instant actual = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, actual, "Instant produced from Tokyo calendar should match");
    }

    @Test
    void testToInstant_Paris() {
        Calendar cal = createCalendar(1969, 12, 31, 23, 59, 59, 999, PARIS);
        Instant expected = cal.toInstant();
        Instant actual = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, actual, "Instant produced from Paris calendar should match");
    }

    @Test
    void testToInstant_NewYork() {
        Calendar cal = createCalendar(1970, 1, 1, 0, 0, 0, 0, NEW_YORK);
        Instant expected = cal.toInstant();
        Instant actual = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, actual, "Instant produced from New York calendar should match");
    }

    @Test
    void testToInstant_EpochBoundary() {
        // Calendar set to the epoch instant (1970‑01‑01T00:00:00Z) in UTC.
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Instant expected = Instant.EPOCH;
        Instant actual = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, actual, "Instant at epoch should be Instant.EPOCH");
    }

    @Test
    void testToInstant_NullArgument() {
        // The method casts the argument to Calendar; a null should cause a NullPointerException.
        assertThrows(NullPointerException.class, () -> CalendarConversions.toInstant(null, converter), "Passing null should result in NullPointerException");
    }

    @Test
    void testToInstant_WrongTypeArgument() {
        // Passing an object that is not a Calendar should cause a ClassCastException.
        Object notACalendar = "I am not a calendar";
        assertThrows(ClassCastException.class, () -> CalendarConversions.toInstant(notACalendar, converter), "Passing a non‑Calendar object should cause ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validCalendarProvider")
    void testValidCalendarConversions(Calendar cal, Converter conv, Instant expected, String description) {
        // If a mock Converter is supplied, ensure it does not get invoked.
        if (conv != null && Mockito.mockingDetails(conv).isMock()) {
            // No stubbing needed – any unexpected call will throw per the mock definition.
        }
        Instant actual = CalendarConversions.toInstant(cal, conv);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("timeZoneEdgeProvider")
    void testTimeZoneEdgeCases(Calendar cal, Instant expected, String description) {
        Instant actual = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, actual, description);
    }

    /**
     * Leap‑second simulation – Java cannot represent 23:59:60, so we use the next representable instant.
     */
    @Test
    void testLeapSecondSimulation() {
        // Represented as 2017‑01‑01T00:00:00Z (the next millisecond after the leap second)
        Calendar cal = utcCalendar(2017, 1, 1, 0, 0, 0, 0);
        Instant expected = Instant.parse("2017-01-01T00:00:00Z");
        Instant actual = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, actual, "Leap‑second is represented by the next instant.");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("extremeDateProvider")
    void testVeryLargeEpochMilliseconds(Calendar cal, Instant expected, String description) {
        Instant actual = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, actual, description);
    }

    /**
     * Millisecond precision test.
     */
    @Test
    void testMillisecondPrecision() {
        Calendar cal = utcCalendar(2023, 3, 15, 12, 34, 56, 789);
        Instant expected = Instant.parse("2023-03-15T12:34:56.789Z");
        Instant actual = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, actual, "Sub‑second (millisecond) precision must be preserved.");
    }

    /**
     * Non‑Gregorian Calendar subclass (JapaneseImperialCalendar).
     */
    @Test
    void testNonGregorianCalendarSubclass() {
        Calendar cal = new java.util.JapaneseImperialCalendar(TimeZone.getTimeZone("UTC"));
        // Set to 2023‑04‑01T00:00:00Z
        cal.clear();
        cal.set(2023, Calendar.APRIL, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Instant expected = Instant.parse("2023-04-01T00:00:00Z");
        Instant actual = CalendarConversions.toInstant(cal, converter);
        assertEquals(expected, actual, "Non‑Gregorian Calendar subclass should be converted correctly.");
    }

    @ParameterizedTest(name = "[{index}] Out‑of‑range calendar should throw")
    @MethodSource("outOfRangeProvider")
    void testOutOfRangeCalendar(Calendar cal) {
        // Calendar.toInstant() throws either DateTimeException or IllegalArgumentException
        assertThrows(RuntimeException.class, () -> CalendarConversions.toInstant(cal, converter), "Conversion of an out‑of‑range calendar should result in an exception.");
    }
}
