package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toString_Test {

    private Converter converter;

    @Mock
    Converter // Mockito mock used in many test cases
    mockConverter;

    /**
     * Calendar subclass that throws an {@link IllegalArgumentException} from {@code getTime()}.
     */
    private static class IllegalArgumentCalendar extends GregorianCalendar {

        IllegalArgumentCalendar(TimeZone tz) {
            super(tz);
        }

        @Override
        public Date getTime() {
            throw new IllegalArgumentException("getTime failure");
        }
    }

    /**
     * Calendar subclass that throws an {@link IllegalStateException} from {@code getTime()}.
     */
    private static class IllegalStateCalendar extends GregorianCalendar {

        IllegalStateCalendar(TimeZone tz) {
            super(tz);
        }

        @Override
        public Date getTime() {
            throw new IllegalStateException("unexpected state");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter works out‑of‑the‑box for Date → String
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * Creates a {@link GregorianCalendar} in the given {@link TimeZone} and sets it to the
     * supplied date‑time fields. Milliseconds are set explicitly.
     */
    private static Calendar calendar(TimeZone tz, int year, int month, int day, int hour, int minute, int second, int milli) {
        Calendar cal = new GregorianCalendar(tz);
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, milli);
        return cal;
    }

    @Test
    void toString_convertsCalendarToString_usingConverter() {
        // GIVEN: a Calendar set to a known instant in UTC
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2021, Calendar.MARCH, 15, 12, 30, 45);
        // ensure deterministic output
        cal.set(Calendar.MILLISECOND, 0);
        // WHEN: delegating to CalendarConversions.toString(...)
        String actual = CalendarConversions.toString(cal, converter);
        // THEN: the result must be identical to converting the underlying Date directly
        String expected = converter.convert(cal.getTime(), String.class);
        assertEquals(expected, actual);
    }

    @Test
    void toString_nullCalendar_throwsNullPointerException() {
        // GIVEN: a null Calendar reference
        Calendar cal = null;
        // WHEN / THEN: a NullPointerException is expected because the method casts without a null‑check
        assertThrows(NullPointerException.class, () -> CalendarConversions.toString(cal, converter));
    }

    @Test
    void toString_standardConversion_isoInstant() {
        // GIVEN: a UTC calendar representing 2021‑01‑01T12:00:00Z
        Calendar cal = calendar(TimeZone.getTimeZone("UTC"), 2021, Calendar.JANUARY, 1, 12, 0, 0, 0);
        // Mock converter to return the instant string
        when(mockConverter.convert(any(Date.class), eq(String.class))).thenAnswer(invocation -> {
            Date d = invocation.getArgument(0);
            // ISO‑8601 instant
            return d.toInstant().toString();
        });
        // WHEN
        String result = CalendarConversions.toString(cal, mockConverter);
        // THEN
        assertEquals("2021-01-01T12:00:00Z", result);
        verify(mockConverter).convert(any(Date.class), eq(String.class));
    }

    @Test
    void toString_timezoneHandling_newYorkToUtc() {
        // GIVEN: a calendar in America/New_York (UTC‑4) set to 2021‑06‑15 08:30:00 EDT
        Calendar cal = calendar(TimeZone.getTimeZone("America/New_York"), 2021, Calendar.JUNE, 15, 8, 30, 0, 0);
        // Mock converter to format as ISO instant (UTC)
        when(mockConverter.convert(any(Date.class), eq(String.class))).thenAnswer(invocation -> {
            Date d = invocation.getArgument(0);
            return d.toInstant().toString();
        });
        // WHEN
        String result = CalendarConversions.toString(cal, mockConverter);
        // THEN: 08:30 EDT == 12:30 UTC
        assertEquals("2021-06-15T12:30:00Z", result);
    }

    @Test
    void toString_invalidType_throwsClassCastException() {
        // GIVEN: an object that is NOT a Calendar
        String notACalendar = "I am not a calendar";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toString(notACalendar, mockConverter));
    }

    @Test
    void toString_nullConverter_throwsNullPointerException() {
        // GIVEN: a valid Calendar but a null Converter
        Calendar cal = calendar(TimeZone.getTimeZone("UTC"), 2021, Calendar.JANUARY, 1, 0, 0, 0, 0);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toString(cal, null));
    }

    @Test
    void toString_epochStart_returnsZeroString() {
        // GIVEN: UTC calendar set to epoch (1970‑01‑01T00:00:00.000Z)
        Calendar cal = calendar(TimeZone.getTimeZone("UTC"), 1970, Calendar.JANUARY, 1, 0, 0, 0, 0);
        // Mock converter to return the epoch milliseconds as a string
        when(mockConverter.convert(any(Date.class), eq(String.class))).thenAnswer(invocation -> {
            Date d = invocation.getArgument(0);
            return Long.toString(d.getTime());
        });
        // WHEN
        String result = CalendarConversions.toString(cal, mockConverter);
        // THEN
        assertEquals("0", result);
    }

    @Test
    void toString_timezoneOffsetCrossingDayBoundary_tokyo() {
        // GIVEN: Calendar in Asia/Tokyo (UTC+9) set to 2022‑03‑01 00:00:00 local time
        Calendar cal = calendar(TimeZone.getTimeZone("Asia/Tokyo"), 2022, Calendar.MARCH, 1, 0, 0, 0, 0);
        // Mock converter to format as ISO instant
        when(mockConverter.convert(any(Date.class), eq(String.class))).thenAnswer(invocation -> {
            Date d = invocation.getArgument(0);
            return d.toInstant().toString();
        });
        // WHEN
        String result = CalendarConversions.toString(cal, mockConverter);
        // THEN: 00:00 JST == 15:00 previous day UTC
        assertEquals("2022-02-28T15:00:00Z", result);
    }

    @Test
    void toString_millisecondPrecision_preservesFraction() {
        // GIVEN: UTC calendar with 123 ms fraction
        Calendar cal = calendar(TimeZone.getTimeZone("UTC"), 2021, Calendar.JANUARY, 1, 12, 0, 0, 123);
        // Mock converter to use Instant#toString (includes fraction)
        when(mockConverter.convert(any(Date.class), eq(String.class))).thenAnswer(invocation -> {
            Date d = invocation.getArgument(0);
            return d.toInstant().toString();
        });
        // WHEN
        String result = CalendarConversions.toString(cal, mockConverter);
        // THEN
        assertEquals("2021-01-01T12:00:00.123Z", result);
    }

    @Test
    void toString_converterThrowsRuntimeException_propagates() {
        // GIVEN: a valid Calendar
        Calendar cal = calendar(TimeZone.getTimeZone("UTC"), 2022, Calendar.JANUARY, 1, 0, 0, 0, 0);
        // Mock converter to throw RuntimeException
        when(mockConverter.convert(any(Date.class), eq(String.class))).thenThrow(new RuntimeException("converter failure"));
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> CalendarConversions.toString(cal, mockConverter));
        assertEquals("converter failure", ex.getMessage());
    }

    @Test
    void toString_converterReturnsNull_propagatesNull() {
        // GIVEN: a valid Calendar
        Calendar cal = calendar(TimeZone.getTimeZone("UTC"), 2021, Calendar.MAY, 5, 10, 20, 30, 0);
        // Mock converter to return null
        when(mockConverter.convert(any(Date.class), eq(String.class))).thenReturn(null);
        // WHEN
        String result = CalendarConversions.toString(cal, mockConverter);
        // THEN
        assertNull(result);
    }

    @Test
    void toString_calendarGetTimeThrowsIllegalArgumentException_propagates() {
        // GIVEN
        Calendar faulty = new IllegalArgumentCalendar(TimeZone.getTimeZone("UTC"));
        // Mock converter – its behavior is irrelevant because getTime() fails first
        when(mockConverter.convert(any(Date.class), eq(String.class))).thenReturn("unused");
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toString(faulty, mockConverter));
    }

    @Test
    void toString_calendarGetTimeThrowsIllegalStateException_propagates() {
        // GIVEN
        Calendar faulty = new IllegalStateCalendar(TimeZone.getTimeZone("UTC"));
        // Mock converter – its behavior is irrelevant because getTime() fails first
        when(mockConverter.convert(any(Date.class), eq(String.class))).thenReturn("unused");
        // WHEN / THEN
        assertThrows(IllegalStateException.class, () -> CalendarConversions.toString(faulty, mockConverter));
    }
}
