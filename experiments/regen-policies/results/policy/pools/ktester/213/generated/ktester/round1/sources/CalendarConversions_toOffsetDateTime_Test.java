package com.cedarsoftware.util.convert;

import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toOffsetDateTime_Test {

    private Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1 / UTC+2 (DST)
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5 / UTC‑4 (DST)
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is required by the static method signature, even though the
        // implementation does not use it for the Calendar → OffsetDateTime conversion.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Calendar} with the supplied fields.
     * <p>
     * The month argument follows the conventional 1‑based numbering used by the
     * existing test suite (January = 1). Internally we convert it to the 0‑based
     * value expected by {@link Calendar#set(int, int, int, int, int, int)}.
     */
    private Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = new GregorianCalendar(tz);
        cal.clear();
        // Calendar months are 0‑based internally
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Provides a stream of arguments for the normal conversion tests.
     *
     * Each argument consists of:
     * <ul>
     *   <li>a prepared {@link Calendar}</li>
     *   <lithe expected {@link OffsetDateTime} as a string (ISO‑8601)</li>
     *   <li>a description (used only for readability in the test name)</li>
     *   <li>a {@link Converter} instance (may be {@code null} or a mock)</li>
     * </ul>
     */
    private static Stream<Arguments> conversionProvider() {
        return Stream.of(// ----- standard conversion tests -----
        Arguments.of(createCalendar(2023, 1, 1, 12, 34, 56, 0, TimeZone.getTimeZone("UTC")), "2023-01-01T12:34:56+00:00", "UTC calendar – offset +00:00", null), Arguments.of(createCalendar(2023, 1, 1, 12, 34, 56, 0, PARIS), "2023-01-01T12:34:56+01:00", "Paris winter (standard offset +01:00)", null), Arguments.of(createCalendar(2023, 7, 15, 9, 15, 30, 0, NEW_YORK), "2023-07-15T09:15:30-04:00", "New York summer (DST –04:00)", null), Arguments.of(createCalendar(2023, 1, 15, 9, 15, 30, 0, NEW_YORK), "2023-01-15T09:15:30-05:00", "New York winter (standard –05:00)", null), // ----- edge case tests -----
        Arguments.of(createCalendar(2023, 8, 1, 12, 0, 0, 0, // +05:30
        TimeZone.getTimeZone("Asia/Kolkata")), "2023-08-01T12:00:00+05:30", "Half‑hour offset (India)", null), Arguments.of(createCalendar(2023, 2, 1, 0, 0, 0, 0, // +12:45
        TimeZone.getTimeZone("Pacific/Chatham")), "2023-02-01T00:00:00+12:45", "Quarter‑hour offset (Chatham Islands)", null), Arguments.of(createCalendar(1970, 1, 1, 0, 0, 0, 0, TimeZone.getTimeZone("UTC")), "1970-01-01T00:00:00Z", "Unix epoch start", null), Arguments.of(createCalendar(1960, 1, 1, 0, 0, 0, 0, TimeZone.getTimeZone("UTC")), "1960-01-01T00:00:00Z", "Pre‑epoch date (negative epoch millis)", null), Arguments.of(createCalendar(2023, 12, 31, 23, 59, 59, 0, TimeZone.getTimeZone("Europe/London")), "2023-12-31T23:59:59Z", "London end‑of‑year with mock Converter", Mockito.mock(Converter.class)), Arguments.of(// SimpleTimeZone with a fixed +02:00 offset and no DST rules
        createCalendarWithSimpleTimeZone(2023, 5, 10, 15, 30, 0, 0, 2 * 60 * 60 * 1000, "Custom+02:00"), "2023-05-10T15:30:00+02:00", "Custom SimpleTimeZone (+02:00)", null));
    }

    /**
     * Creates a {@link GregorianCalendar} that uses a manually constructed {@link SimpleTimeZone}.
     *
     * @param year   year
     * @param month  1‑based month
     * @param day    day of month
     * @param hour   hour of day
     * @param minute minute
     * @param second second
     * @param millis millisecond
     * @param rawOffsetMillis raw offset in milliseconds (e.g. 2*60*60*1000 for +02:00)
     * @param id     identifier for the SimpleTimeZone
     * @return a {@link Calendar} configured with the given SimpleTimeZone
     */
    private static Calendar createCalendarWithSimpleTimeZone(int year, int month, int day, int hour, int minute, int second, int millis, int rawOffsetMillis, String id) {
        SimpleTimeZone stz = new SimpleTimeZone(rawOffsetMillis, id);
        // No DST rules are defined – the offset is constant.
        Calendar cal = new GregorianCalendar(stz);
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null Calendar argument → NullPointerException when cast is attempted
        Arguments.of(null, NullPointerException.class, "null Calendar should cause NPE"), // Wrong type (java.util.Date) → ClassCastException on the cast to Calendar
        Arguments.of(new java.util.Date(), ClassCastException.class, "non‑Calendar object should cause ClassCastException"), // TimeZone offset +19:00 (outside ±18:00) → DateTimeException from ZoneOffset
        Arguments.of(createCalendar(2023, 1, 1, 0, 0, 0, 0, new SimpleTimeZone(19 * 60 * 60 * 1000, "TooBig+19")), java.time.DateTimeException.class, "offset +19:00 exceeds allowed range"), // Calendar with a null TimeZone → NullPointerException when getOffset is called
        Arguments.of(calendarWithNullTimeZone(2023, 1, 1, 0, 0, 0, 0), NullPointerException.class, "Calendar whose TimeZone is null should cause NPE"));
    }

    /**
     * Returns a {@link Calendar} whose {@link TimeZone} has been explicitly set to {@code null}.
     */
    private static Calendar calendarWithNullTimeZone(int year, int month, int day, int hour, int minute, int second, int millis) {
        Calendar cal = createCalendar(year, month, day, hour, minute, second, millis, TimeZone.getTimeZone("UTC"));
        cal.setTimeZone(null);
        return cal;
    }

    @Test
    void testToOffsetDateTime_Tokyo() {
        Calendar cal = createCalendar(2023, 3, 25, 14, 30, 15, 123, TOKYO);
        OffsetDateTime actual = CalendarConversions.toOffsetDateTime(cal, converter);
        OffsetDateTime expected = OffsetDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
        assertEquals(expected, actual, "OffsetDateTime conversion for Tokyo timezone should match expected value");
    }

    @Test
    void testToOffsetDateTime_Paris() {
        Calendar cal = createCalendar(2021, 10, 31, 23, 45, 0, 0, PARIS);
        OffsetDateTime actual = CalendarConversions.toOffsetDateTime(cal, converter);
        OffsetDateTime expected = OffsetDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
        assertEquals(expected, actual, "OffsetDateTime conversion for Paris timezone (including possible DST) should match expected value");
    }

    @Test
    void testToOffsetDateTime_NewYork_DaylightSavingTransition() {
        // 2021‑03‑14 02:30 does not exist in New York (DST spring forward);
        // Calendar will adjust to 03:30. Verify that the method respects the adjusted instant.
        Calendar cal = createCalendar(2021, 3, 14, 2, 30, 0, 0, NEW_YORK);
        OffsetDateTime actual = CalendarConversions.toOffsetDateTime(cal, converter);
        OffsetDateTime expected = OffsetDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
        assertEquals(expected, actual, "OffsetDateTime conversion should handle DST transition correctly");
    }

    @Test
    void testToOffsetDateTime_EpochStart() {
        Calendar cal = createCalendar(1970, 1, 1, 0, 0, 0, 0, TimeZone.getTimeZone("UTC"));
        OffsetDateTime actual = CalendarConversions.toOffsetDateTime(cal, converter);
        OffsetDateTime expected = OffsetDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
        assertEquals(expected, actual, "Conversion of the epoch start should produce the correct OffsetDateTime");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("conversionProvider")
    void testToOffsetDateTime_Conversions(Calendar cal, String expectedIso, String description, Converter conv) {
        // Use the supplied converter (may be null or a mock)
        OffsetDateTime actual = CalendarConversions.toOffsetDateTime(cal, conv);
        OffsetDateTime expected = OffsetDateTime.parse(expectedIso);
        assertEquals(expected, actual, () -> "Conversion failed for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionProvider")
    void testToOffsetDateTime_Exceptions(Object from, Class<? extends Throwable> expectedException, String description) {
        // The method signature requires a Converter; we can safely pass the regular one.
        assertThrows(expectedException, () -> CalendarConversions.toOffsetDateTime(from, converter), () -> "Expected exception for case: " + description);
    }
}
