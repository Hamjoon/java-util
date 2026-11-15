package com.cedarsoftware.util.convert;

import java.time.YearMonth;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toYearMonth_Test {

    private Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5/‑4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a concrete options instance – the library provides a default one.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that builds a {@link Calendar} with the supplied fields.
     */
    private Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar months are zero‑based
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Helper that builds a {@link Calendar} from an {@link Instant} in the given time‑zone.
     */
    private static Calendar calendarFromInstant(Instant instant, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.setTime(Date.from(instant));
        return cal;
    }

    private static Stream<Arguments> normalAndBoundaryProvider() {
        return Stream.of(// Normal conversion – UTC
        Arguments.of(calendarFromInstant(Instant.parse("2023-05-15T12:34:56Z"), UTC), ZoneId.of("UTC"), YearMonth.of(2023, 5), "Standard conversion where the Calendar instant is interpreted in UTC"), // Normal conversion – offset moves date back a day
        Arguments.of(calendarFromInstant(Instant.parse("2023-01-01T01:00:00Z"), UTC), ZoneId.of("America/Los_Angeles"), YearMonth.of(2022, 12), "Instant 2023‑01‑01T01:00:00Z interpreted in -08:00 zone becomes 2022‑12‑31"), // Boundary – epoch start
        Arguments.of(calendarFromInstant(Instant.parse("1970-01-01T00:00:00Z"), UTC), ZoneId.of("UTC"), YearMonth.of(1970, 1), "Epoch start instant interpreted in UTC"), // Leap day
        Arguments.of(calendarFromInstant(Instant.parse("2020-02-29T12:00:00Z"), UTC), ZoneId.of("UTC"), YearMonth.of(2020, 2), "Leap‑year day should be February 2020"), // UTC → Asia/Tokyo crosses month boundary forward
        Arguments.of(calendarFromInstant(Instant.parse("2023-01-31T23:00:00Z"), UTC), ZoneId.of("Asia/Tokyo"), YearMonth.of(2023, 2), "Applying +09:00 offset moves date to 2023‑02‑01"), // UTC → America/Los_Angeles crosses month boundary backward
        Arguments.of(calendarFromInstant(Instant.parse("2023-03-01T01:00:00Z"), UTC), ZoneId.of("America/Los_Angeles"), YearMonth.of(2023, 2), "Applying -08:00 offset moves date back to 2023‑02‑28"), // End‑of‑year instant stays within December
        Arguments.of(calendarFromInstant(Instant.parse("2023-12-31T23:59:59Z"), UTC), ZoneId.of("UTC"), YearMonth.of(2023, 12), "End‑of‑year instant stays in December 2023"), // Start‑of‑year instant
        Arguments.of(calendarFromInstant(Instant.parse("2024-01-01T00:00:00Z"), UTC), ZoneId.of("UTC"), YearMonth.of(2024, 1), "Start‑of‑year instant should produce January 2024"), // 2038‑01‑19T03:14:07Z – classic 32‑bit overflow point
        Arguments.of(calendarFromInstant(Instant.parse("2038-01-19T03:14:07Z"), UTC), ZoneId.of("UTC"), YearMonth.of(2038, 1), "Conversion at the 2038 problem boundary"));
    }

    private static Stream<Arguments> errorHandlingProvider() {
        // Helper calendar for tests that need a valid Calendar instance
        Calendar validCal = Calendar.getInstance(UTC);
        validCal.clear();
        validCal.setTime(Date.from(Instant.parse("2023-05-15T12:34:56Z")));
        // Mock converter that returns a null ZoneId
        Converter converterNullZone = mock(Converter.class, RETURNS_DEEP_STUBS);
        when(converterNullZone.getOptions().getZoneId()).thenReturn(null);
        // Mock converter whose getOptions() returns null
        Converter converterNullOptions = mock(Converter.class);
        when(converterNullOptions.getOptions()).thenReturn(null);
        // Mock converter that returns a proper ZoneId (used for the null‑from case)
        Converter converterWithUtc = mock(Converter.class, RETURNS_DEEP_STUBS);
        when(converterWithUtc.getOptions().getZoneId()).thenReturn(ZoneId.of("UTC"));
        // Calendar far beyond Instant.MAX – using GregorianCalendar with a huge year
        Calendar farFuture = new GregorianCalendar();
        farFuture.clear();
        farFuture.set(1_000_000_000, Calendar.JANUARY, 1, 0, 0, 0);
        farFuture.setTimeZone(UTC);
        return Stream.of(// Null 'from' argument – NPE
        Arguments.of(null, ZoneId.of("UTC"), converterWithUtc, NullPointerException.class, "Null 'from' argument leads to NPE"), // Non‑Calendar object – ClassCastException
        Arguments.of("2023-05-01", ZoneId.of("UTC"), converterWithUtc, ClassCastException.class, "Passing a non‑Calendar object should cause a ClassCastException"), // Null converter – NPE
        Arguments.of(validCal, ZoneId.of("UTC"), null, NullPointerException.class, "Null converter triggers NPE when the method accesses converter.getOptions()"), // Converter with null ZoneId – NPE
        Arguments.of(validCal, null, converterNullZone, NullPointerException.class, "A null ZoneId in the converter options causes NPE at atZone(null)"), // Calendar beyond Instant range – DateTimeException
        Arguments.of(farFuture, ZoneId.of("UTC"), converterWithUtc, java.time.DateTimeException.class, "Calendar date exceeds the range supported by java.time.Instant"), // Converter whose getOptions() returns null – NPE
        Arguments.of(validCal, ZoneId.of("UTC"), converterNullOptions, NullPointerException.class, "Converter's getOptions() returns null, leading to NPE"));
    }

    @Test
    void testCalendarToYearMonth() {
        // Directly invoke the package‑private static method under test.
        assertEquals(YearMonth.of(1888, 1), CalendarConversions.toYearMonth(createCalendar(1888, 1, 2, 12, 30, 45, 123, TOKYO), converter));
        assertEquals(YearMonth.of(1969, 12), CalendarConversions.toYearMonth(createCalendar(1969, 12, 31, 23, 59, 59, 999, PARIS), converter));
        assertEquals(YearMonth.of(1970, 1), CalendarConversions.toYearMonth(createCalendar(1970, 1, 1, 0, 0, 1, 1, NEW_YORK), converter));
        assertEquals(YearMonth.of(2023, 6), CalendarConversions.toYearMonth(createCalendar(2023, 6, 15, 15, 30, 0, 500, TOKYO), converter));
    }

    @Test
    void testCalendarToYearMonth_NullInput() {
        // The method should return null when the source Calendar is null.
        assertNull(CalendarConversions.toYearMonth(null, converter));
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("normalAndBoundaryProvider")
    void testToYearMonth_Normal(Calendar calendar, ZoneId zoneId, YearMonth expected, String description) {
        // Arrange – mock a Converter that returns the desired ZoneId
        converter = mock(Converter.class, RETURNS_DEEP_STUBS);
        when(converter.getOptions().getZoneId()).thenReturn(zoneId);
        // Act
        YearMonth result = CalendarConversions.toYearMonth(calendar, converter);
        // Assert
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("errorHandlingProvider")
    void testToYearMonth_Errors(Object from, ZoneId zoneId, Converter customConverter, Class<? extends Throwable> expectedException, String description) {
        // Act & Assert
        Throwable thrown = assertThrows(expectedException, () -> CalendarConversions.toYearMonth(from, customConverter), description);
        // optional: verify exception message contains something useful
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testCalendarToYearMonth_ExistingSamples() {
        // Directly invoke the package‑private static method under test.
        assertEquals(YearMonth.of(1888, 1), CalendarConversions.toYearMonth(createCalendar(1888, 1, 2, 12, 30, 45, 123, TOKYO), converter));
        assertEquals(YearMonth.of(1969, 12), CalendarConversions.toYearMonth(createCalendar(1969, 12, 31, 23, 59, 59, 999, PARIS), converter));
        assertEquals(YearMonth.of(1970, 1), CalendarConversions.toYearMonth(createCalendar(1970, 1, 1, 0, 0, 1, 1, NEW_YORK), converter));
        assertEquals(YearMonth.of(2023, 6), CalendarConversions.toYearMonth(createCalendar(2023, 6, 15, 15, 30, 0, 500, TOKYO), converter));
    }

    @Test
    void testCalendarToYearMonth_NullInput_Existing() {
        // The original test expected a null return; the specification says an NPE is thrown.
        // Keeping the test for backward compatibility – it will now assert that an NPE is thrown.
        assertThrows(NullPointerException.class, () -> CalendarConversions.toYearMonth(null, converter));
    }
}
