package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.MockedStatic;

class CalendarConversions_toMonthDay_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – they contain a ZoneId (system default) which
        // is sufficient for the conversion logic exercised here.
        converter = new Converter(new DefaultConverterOptions());
        // Default converter – system default zone (UTC is overridden per test case)
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a {@link Calendar} with the supplied fields.
     * The month argument follows the usual 1‑based convention (January = 1) and is
     * internally converted to the 0‑based value expected by {@link Calendar}.
     */
    private Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Helper that creates a {@link Calendar} representing the given {@link Instant}
     *  in the supplied {@link TimeZone}.
     */
    private static Calendar calendarFromInstant(Instant instant, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.setTime(Date.from(instant));
        return cal;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Standard UTC conversion – same day
        Arguments.of("Standard UTC conversion", calendarFromInstant(Instant.parse("2023-03-15T10:00:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("UTC"), "--03-15"), // UTC → America/New_York (offset -05:00) – still same calendar day
        Arguments.of("UTC to New York same day", calendarFromInstant(Instant.parse("2023-03-15T10:00:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("America/New_York"), "--03-15"), // Shift back one day (Los Angeles, -08:00)
        Arguments.of("Shift to previous day (Los Angeles)", calendarFromInstant(Instant.parse("2023-03-15T01:00:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("America/Los_Angeles"), "--03-14"), // Leap‑year 2020‑02‑29 stays unchanged in UTC
        Arguments.of("Leap‑year Feb 29 UTC", calendarFromInstant(Instant.parse("2020-02-29T12:00:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("UTC"), "--02-29"), // Year‑end rollover – Paris (+01:00) moves to next year, but MonthDay reflects new date
        Arguments.of("Year‑end rollover to Jan 1 (Paris)", calendarFromInstant(Instant.parse("2023-12-31T23:30:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("Europe/Paris"), "--01-01"), // Control case – same instant, UTC zone
        Arguments.of("Year‑end control UTC", calendarFromInstant(Instant.parse("2023-12-31T23:30:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("UTC"), "--12-31"), // Extreme positive offset (+14:00) moves to next day
        Arguments.of("Extreme positive offset (Kiritimati)", calendarFromInstant(Instant.parse("2023-01-01T00:15:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("Pacific/Kiritimati"), "--01-02"), // Baseline UTC for same instant
        Arguments.of("Baseline UTC for extreme offset test", calendarFromInstant(Instant.parse("2023-01-01T00:15:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("UTC"), "--01-01"), // Non‑leap year Feb 28, Los Angeles offset does not cross midnight
        Arguments.of("Non‑leap Feb 28 stays same (Los Angeles)", calendarFromInstant(Instant.parse("2022-02-28T23:00:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("America/Los_Angeles"), "--02-28"), // Leap‑day stays same when offset does not cross midnight
        Arguments.of("Leap‑day stays same (Los Angeles, no rollover)", calendarFromInstant(Instant.parse("2020-02-29T23:00:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("America/Los_Angeles"), "--02-29"), // Leap‑day rolls back to Feb 28
        Arguments.of("Leap‑day rolls back to Feb 28 (Los Angeles)", calendarFromInstant(Instant.parse("2020-02-29T01:00:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("America/Los_Angeles"), "--02-28"), // Epoch start UTC
        Arguments.of("Epoch start UTC", calendarFromInstant(Instant.parse("1970-01-01T00:00:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("UTC"), "--01-01"), // Epoch start shifted to previous day (Honolulu -10:00)
        Arguments.of("Epoch start shifted to previous day (Honolulu)", calendarFromInstant(Instant.parse("1970-01-01T00:00:00Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("Pacific/Honolulu"), "--12-31"), // 32‑bit overflow edge (Jan 19 2038) UTC
        Arguments.of("32‑bit overflow edge UTC", calendarFromInstant(Instant.parse("2038-01-19T03:14:07Z"), TimeZone.getTimeZone("UTC")), ZoneId.of("UTC"), "--01-19"));
    }

    private static Stream<Arguments> exceptionProvider() {
        // Mock converters for special null‑option scenarios
        Converter nullOptionsConverter = mock(Converter.class);
        when(nullOptionsConverter.getOptions()).thenReturn(null);
        Converter nullZoneIdConverter = mock(Converter.class);
        DefaultConverterOptions optsWithNullZone = mock(DefaultConverterOptions.class);
        when(optsWithNullZone.getZoneId()).thenReturn(null);
        when(nullZoneIdConverter.getOptions()).thenReturn(optsWithNullZone);
        return Stream.of(// Null Calendar
        Arguments.of("Null Calendar argument", null, new Converter(new DefaultConverterOptions()), NullPointerException.class), // Wrong type (String)
        Arguments.of("Non‑Calendar source object", "not a calendar", new Converter(new DefaultConverterOptions()), ClassCastException.class), // Null converter
        Arguments.of("Null converter argument", calendarFromInstant(Instant.parse("2023-03-15T10:00:00Z"), TimeZone.getTimeZone("UTC")), null, NullPointerException.class), // Converter with null Options
        Arguments.of("Converter with null Options", calendarFromInstant(Instant.parse("2023-03-15T10:00:00Z"), TimeZone.getTimeZone("UTC")), nullOptionsConverter, NullPointerException.class), // Converter with Options that have null ZoneId
        Arguments.of("Converter with Options.zoneId == null", calendarFromInstant(Instant.parse("2023-03-15T10:00:00Z"), TimeZone.getTimeZone("UTC")), nullZoneIdConverter, NullPointerException.class), // Calendar date beyond Instant range (year 10000)
        Arguments.of("Date beyond Instant range (year 10000)", // GregorianCalendar can hold year 10000, but toInstant() will fail
        createFarFutureCalendar(), new Converter(new DefaultConverterOptions()), DateTimeException.class));
    }

    /**
     * Creates a {@link Calendar} set to the year 10000‑01‑01T00:00:00Z.
     *  The method deliberately avoids calling {@code toInstant()} so that the
     *  exception is triggered inside {@code CalendarConversions.toMonthDay}.
     */
    private static Calendar createFarFutureCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(10000, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void testToMonthDay_WithVariousTimeZones() {
        // Tokyo (UTC+9)
        Calendar tokyoCal = createCalendar(1888, 1, 2, 3, 45, 20, 222, TimeZone.getTimeZone("Asia/Tokyo"));
        assertEquals(MonthDay.of(1, 2), CalendarConversions.toMonthDay(tokyoCal, converter));
        // Paris (UTC+1/+2 depending on DST)
        Calendar parisCal = createCalendar(1969, 12, 31, 14, 25, 35, 444, TimeZone.getTimeZone("Europe/Paris"));
        assertEquals(MonthDay.of(12, 31), CalendarConversions.toMonthDay(parisCal, converter));
        // New York (UTC‑5/‑4)
        Calendar nyCal = createCalendar(1970, 1, 1, 8, 50, 40, 666, TimeZone.getTimeZone("America/New_York"));
        assertEquals(MonthDay.of(1, 1), CalendarConversions.toMonthDay(nyCal, converter));
        // Another example – June 15
        Calendar nyCal2 = createCalendar(2023, 6, 15, 17, 10, 5, 999, TimeZone.getTimeZone("America/New_York"));
        assertEquals(MonthDay.of(6, 15), CalendarConversions.toMonthDay(nyCal2, converter));
    }

    @Test
    void testToMonthDay_NullCalendarThrowsNPE() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toMonthDay(null, converter));
    }

    /* --------------------------------------------------------------
     *  Successful conversions – parameterized tests
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void testToMonthDay_ValidConversions(String description, Calendar calendar, ZoneId zoneId, String expectedMonthDayString) {
        // configure converter with the required zone
        converter = new Converter(new DefaultConverterOptions(zoneId));
        MonthDay result = CalendarConversions.toMonthDay(calendar, converter);
        assertEquals(expectedMonthDayString, result.toString(), () -> "Failed: " + description);
    }

    /* --------------------------------------------------------------
     *  Exception scenarios – parameterized tests
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void testToMonthDay_Exceptions(String description, Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CalendarConversions.toMonthDay(from, conv), () -> "Expected exception for: " + description);
    }
}
