package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toString_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Default converter options are sufficient for all conversions performed here.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Calendar} with the supplied fields.
     *
     * @param year   the year
     * @param month  the month (1‑based, as used in the test data)
     * @param day    the day of month
     * @param hour   hour of day (0‑23)
     * @param minute minute
     * @param second second
     * @param millis millisecond part
     * @param tz     the time‑zone for the calendar
     * @return a fully initialised Calendar instance
     */
    private static Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar months are zero‑based
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    private static Stream<Arguments> zoneHandlingWithoutFractionalSeconds() {
        return Stream.of(// ZoneId contains '/', abbreviation lookup is skipped.
        Arguments.of("Named zone with '/' and no fraction", "2023-03-25T12:34:56-04:00[America/New_York]", "2023-03-25T12:34:56-04:00[America/New_York]"), // Abbreviation 'UTC' should be replaced with full zone name.
        Arguments.of("Abbreviation 'UTC' is expanded to full zone name", "2023-03-25T12:34:56Z[UTC]", "2023-03-25T12:34:56+00:00[UTC]"), // Simple offset – formatter without brackets.
        Arguments.of("Pure ZoneOffset without '/' and no fraction", "2023-03-25T12:34:56-05:00", "2023-03-25T12:34:56-05:00"), // Abbreviation not in map – stays as is, brackets are kept.
        Arguments.of("Named zone with abbreviation not in map", "2023-03-25T12:34:56+02:00[GMT+02:00]", "2023-03-25T12:34:56+02:00[GMT+02:00]"));
    }

    private static Stream<Arguments> fractionalWithZoneOffset() {
        return Stream.of(Arguments.of("Offset with nanosecond fraction", "2023-03-25T12:34:56.123456789-04:00", "2023-03-25T12:34:56.123456789-04:00"), Arguments.of("Zero fraction should be suppressed", "2023-03-25T12:34:56.0-05:00", "2023-03-25T12:34:56-05:00"));
    }

    private static Stream<Arguments> fractionalWithNamedZone() {
        return Stream.of(Arguments.of("Named zone with three‑digit fraction", "2023-03-25T12:34:56.250-04:00[America/New_York]", "2023-03-25T12:34:56.250-04:00[America/New_York]"), Arguments.of("One‑nanosecond fraction (nine‑digit output)", "2023-03-25T12:34:56.000000001+02:00[Europe/Paris]", "2023-03-25T12:34:56.000000001+02:00[Europe/Paris]"));
    }

    private static Stream<Arguments> abbreviationMappingWithFraction() {
        return Stream.of(Arguments.of("EST abbreviation expands to America/New_York, fraction retained", "2023-03-25T12:34:56.75EST", "2023-03-25T12:34:56.75-05:00[America/New_York]"), Arguments.of("PST abbreviation expands to America/Los_Angeles, fraction retained", "2023-03-25T12:34:56.5PST", "2023-03-25T12:34:56.5-08:00[America/Los_Angeles]"));
    }

    private static Stream<Arguments> objectInputs() {
        return Stream.of(Arguments.of("ZonedDateTime with nanoseconds and named zone", ZonedDateTime.of(2023, 3, 25, 12, 34, 56, 987_654_321, ZoneId.of("America/New_York")), "2023-03-25T12:34:56.987654321-04:00[America/New_York]"), Arguments.of("OffsetDateTime without fraction", OffsetDateTime.of(2023, 3, 25, 12, 34, 56, 0, ZoneOffset.of("+02:00")), "2023-03-25T12:34:56+02:00"), Arguments.of("OffsetDateTime with fraction", OffsetDateTime.of(2023, 3, 25, 12, 34, 56, 250_000_000, ZoneOffset.of("+02:00")), "2023-03-25T12:34:56.25+02:00"));
    }

    /**
     * Verify that a Calendar in UTC (a ZoneOffset) is formatted without the
     * zone‑name block (the “[VV]” part) and that no fractional seconds are emitted
     * when the nanosecond component is zero.
     */
    @Test
    void toString_WithZoneOffset_NoFraction() {
        Calendar utcCal = createCalendar(2023, 1, 2, 3, 4, 5, 0, TimeZone.getTimeZone("UTC"));
        String result = CalendarConversions.toString(utcCal, converter);
        // Expected format: 2023-01-02T03:04:05+00:00
        assertEquals("2023-01-02T03:04:05+00:00", result);
    }

    /**
     * Verify that a Calendar in a named zone (America/New_York) is formatted
     * with the zone‑name block and that fractional seconds are included when
     * the Calendar carries nanosecond precision.
     */
    @Test
    void toString_WithNamedZone_WithFraction() {
        // Calendar stores milliseconds only; to get nanoseconds we need to
        // add them via the underlying ZonedDateTime after conversion.
        Calendar nyCal = createCalendar(2023, 1, 2, 3, 4, 5, 123, TimeZone.getTimeZone("America/New_York"));
        // Convert the Calendar to a ZonedDateTime first so we can inject extra nanos.
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(nyCal, converter);
        // Add 456,789 nanoseconds (total 123ms + 456,789ns = 123,456,789ns)
        zdt = zdt.withNano(123_456_789);
        // Use the overloaded toString that accepts a ZonedDateTime directly via the
        // Converter – we achieve this by passing the ZonedDateTime as the source object.
        String result = CalendarConversions.toString(zdt, converter);
        // Expected pattern: 2023-01-02T03:04:05.123456789-05:00[America/New_York]
        // The offset for 2023‑01‑02 in New York is -05:00 (standard time).
        assertEquals("2023-01-02T03:04:05.123456789-05:00[America/New_York]", result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zoneHandlingWithoutFractionalSeconds")
    void toString_ZoneHandlingWithoutFraction(String description, Object from, String expected) {
        String actual = CalendarConversions.toString(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("fractionalWithZoneOffset")
    void toString_FractionalWithZoneOffset(String description, Object from, String expected) {
        String actual = CalendarConversions.toString(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("fractionalWithNamedZone")
    void toString_FractionalWithNamedZone(String description, Object from, String expected) {
        String actual = CalendarConversions.toString(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("abbreviationMappingWithFraction")
    void toString_AbbreviationMappingWithFraction(String description, Object from, String expected) {
        String actual = CalendarConversions.toString(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("objectInputs")
    void toString_ObjectInputs(String description, Object from, String expected) {
        String actual = CalendarConversions.toString(from, converter);
        assertEquals(expected, actual, description);
    }

    @Test
    void toString_NullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toString(null, converter), "Passing null as 'from' should throw NPE");
    }

    @Test
    void toString_MalformedString_throwsDateTimeParseException() {
        assertThrows(DateTimeParseException.class, () -> CalendarConversions.toString("not-a-date-time", converter), "Malformed ISO‑8601 string should cause DateTimeParseException");
    }

    @Test
    void toString_NullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toString("2023-03-25T12:34:56Z", null), "Null converter argument should cause NPE");
    }

    @Test
    void toString_UnsupportedSourceType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toString(12345, converter), "Unsupported source type (Integer) should cause IllegalArgumentException");
    }

    @Test
    void toString_LocalDateTimeWithoutZone_throwsDateTimeException() {
        assertThrows(DateTimeException.class, () -> CalendarConversions.toString(LocalDateTime.of(2023, 3, 25, 12, 34, 56), converter), "LocalDateTime lacking zone/offset should cause DateTimeException");
    }
}
