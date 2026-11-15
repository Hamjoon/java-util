package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toCalendar_Test {

    private Converter converter;

    private ConverterOptions options;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test with a known time‑zone.
        options = new ConverterOptions();
        // Default to UTC; individual tests may override this.
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        converter = new Converter(options);
    }

    private static Stream<Arguments> normalTimesProvider() {
        return Stream.of(Arguments.of(LocalTime.of(13, 45, 30, 123_000_000), 13, 45, 30, 123), Arguments.of(LocalTime.of(0, 0, 0, 0), 0, 0, 0, 0));
    }

    private static Stream<Arguments> boundaryTimesProvider() {
        return Stream.of(// 23:59:59.999999999 -> 999 ms (truncation)
        Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), 23, 59, 59, 999), // 12:30:15.001234567 -> 1 ms (truncation)
        Arguments.of(LocalTime.of(12, 30, 15, 1_234_567), 12, 30, 15, 1));
    }

    @Test
    void testToCalendarBasicFields() {
        // GIVEN
        // 13:45:30.000
        LocalTime time = LocalTime.of(13, 45, 30, 0);
        // WHEN
        Calendar cal = LocalTimeConversions.toCalendar(time, converter);
        // THEN
        assertNotNull(cal, "Calendar should not be null");
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Calendar should use the UTC time‑zone from ConverterOptions");
        assertEquals(13, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(45, cal.get(Calendar.MINUTE));
        assertEquals(30, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    void testToCalendarWithNanosAndCustomTimeZone() {
        // GIVEN
        // Use a time zone with a non‑UTC offset to ensure the zone is respected.
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        options.setTimeZone(tz);
        // Re‑create the converter so it picks up the new time‑zone.
        converter = new Converter(options);
        // 12:34:56.123456789 -> expected milliseconds = 123 (nanoseconds truncated)
        LocalTime time = LocalTime.of(12, 34, 56, 123_456_789);
        // WHEN
        Calendar cal = LocalTimeConversions.toCalendar(time, converter);
        // THEN
        assertNotNull(cal, "Calendar should not be null");
        assertEquals(tz, cal.getTimeZone(), "Calendar should use the time‑zone supplied via ConverterOptions");
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(34, cal.get(Calendar.MINUTE));
        assertEquals(56, cal.get(Calendar.SECOND));
        assertEquals(123, cal.get(Calendar.MILLISECOND), "Milliseconds should be derived from nanoseconds by integer division (123456789 / 1_000_000)");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for normal and boundary times
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] Normal time: {0}")
    @MethodSource("normalTimesProvider")
    void testNormalTimes(LocalTime input, int expHour, int expMinute, int expSecond, int expMilli) {
        Calendar cal = LocalTimeConversions.toCalendar(input, converter);
        assertAll(() -> assertNotNull(cal, "Calendar must not be null"), () -> assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Calendar must use UTC time‑zone"), () -> assertEquals(expHour, cal.get(Calendar.HOUR_OF_DAY), "Hour mismatch"), () -> assertEquals(expMinute, cal.get(Calendar.MINUTE), "Minute mismatch"), () -> assertEquals(expSecond, cal.get(Calendar.SECOND), "Second mismatch"), () -> assertEquals(expMilli, cal.get(Calendar.MILLISECOND), "Millisecond mismatch"));
    }

    @ParameterizedTest(name = "[{index}] Boundary time: {0}")
    @MethodSource("boundaryTimesProvider")
    void testBoundaryTimes(LocalTime input, int expHour, int expMinute, int expSecond, int expMilli) {
        Calendar cal = LocalTimeConversions.toCalendar(input, converter);
        assertAll(() -> assertNotNull(cal, "Calendar must not be null"), () -> assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Calendar must use UTC time‑zone"), () -> assertEquals(expHour, cal.get(Calendar.HOUR_OF_DAY), "Hour mismatch"), () -> assertEquals(expMinute, cal.get(Calendar.MINUTE), "Minute mismatch"), () -> assertEquals(expSecond, cal.get(Calendar.SECOND), "Second mismatch"), () -> assertEquals(expMilli, cal.get(Calendar.MILLISECOND), "Millisecond mismatch"));
    }

    /* --------------------------------------------------------------
     *  Time‑zone difference test (non‑UTC)
     * -------------------------------------------------------------- */
    @Test
    void testTimeZoneDifference() {
        // GIVEN a converter configured for GMT+02:00
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        options.setTimeZone(tz);
        converter = new Converter(options);
        LocalTime time = LocalTime.of(10, 20, 30, 0);
        // WHEN
        Calendar cal = LocalTimeConversions.toCalendar(time, converter);
        // THEN
        assertAll(() -> assertNotNull(cal, "Calendar must not be null"), () -> assertEquals(tz, cal.getTimeZone(), "Calendar must use the supplied GMT+02:00 zone"), () -> assertEquals(10, cal.get(Calendar.HOUR_OF_DAY), "Hour should be unchanged"), () -> assertEquals(20, cal.get(Calendar.MINUTE), "Minute should be unchanged"), () -> assertEquals(30, cal.get(Calendar.SECOND), "Second should be unchanged"), () -> assertEquals(0, cal.get(Calendar.MILLISECOND), "Millisecond should be unchanged"));
    }

    /* --------------------------------------------------------------
     *  Error handling tests
     * -------------------------------------------------------------- */
    @Test
    void testFromWrongTypeThrowsClassCastException() {
        Object notALocalTime = "not a LocalTime";
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toCalendar(notALocalTime, converter), "Passing a non‑LocalTime object should cause ClassCastException");
    }

    @Test
    void testFromNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toCalendar(null, converter), "Null 'from' argument should result in NPE");
    }

    @Test
    void testConverterNullThrowsNullPointerException() {
        LocalTime time = LocalTime.of(5, 5, 5, 0);
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toCalendar(time, null), "Null 'converter' argument should result in NPE");
    }

    /* --------------------------------------------------------------
     *  Null options / null time‑zone scenarios using Mockito
     * -------------------------------------------------------------- */
    @Test
    void testConverterWithNullOptionsThrowsNullPointerException() {
        // Mock a Converter that returns null for getOptions()
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        LocalTime time = LocalTime.of(1, 2, 3, 0);
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toCalendar(time, mockConverter), "Converter with null options should cause NPE when accessing getTimeZone()");
    }

    @Test
    void testConverterWithNullTimeZoneThrowsNullPointerException() {
        // Mock ConverterOptions to return null TimeZone
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockOptions.getTimeZone()).thenReturn(null);
        // Mock Converter to return the above options
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        LocalTime time = LocalTime.of(1, 2, 3, 0);
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toCalendar(time, mockConverter), "Converter with null TimeZone should cause NPE inside Calendar.getInstance(null)");
    }
}
