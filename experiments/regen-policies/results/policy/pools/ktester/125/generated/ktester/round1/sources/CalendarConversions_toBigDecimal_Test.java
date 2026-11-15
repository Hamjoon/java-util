package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toBigDecimal_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /* --------------------------------------------------------------
     *  Parameterised normal conversion tests
     * -------------------------------------------------------------- */
    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// 2023‑01‑01T00:00:00Z
        Arguments.of(createUtcCalendar(2023, Calendar.JANUARY, 1, 0, 0, 0, 0), new BigDecimal(1672531200000L).divide(BigDecimalConversions.GRAND)), // 1970‑01‑01T00:00:00.123Z (epoch millis = 123)
        Arguments.of(createUtcCalendar(1970, Calendar.JANUARY, 1, 0, 0, 0, 123), new BigDecimal(123L).divide(BigDecimalConversions.GRAND)));
    }

    /* --------------------------------------------------------------
     *  Helper method to create a UTC Calendar with precise millis
     * -------------------------------------------------------------- */
    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second, int millis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        // Ensure the internal time value is recomputed
        cal.getTimeInMillis();
        return cal;
    }

    @Test
    void toBigDecimal_convertsCalendarEpochMillisToBigDecimal() {
        // GIVEN – a Calendar set to a known instant (2020‑01‑01T00:00:00Z)
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2020‑01‑01 00:00:00 UTC
        long expectedEpochMillis = 1_577_836_800_000L;
        calendar.setTimeInMillis(expectedEpochMillis);
        // Expected result uses the same logic as the implementation
        BigDecimal expected = new BigDecimal(expectedEpochMillis).divide(BigDecimalConversions.GRAND);
        // WHEN
        BigDecimal actual = CalendarConversions.toBigDecimal(calendar, mockConverter);
        // THEN
        assertEquals(expected, actual, "BigDecimal conversion of Calendar epoch millis is incorrect");
    }

    @Test
    void toBigDecimal_nonCalendarThrowsClassCastException() {
        // GIVEN – an object that is NOT a Calendar
        Object notACalendar = "I am not a calendar";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toBigDecimal(notACalendar, mockConverter), "Expected ClassCastException when source object is not a Calendar");
    }

    @ParameterizedTest(name = "{index}: {0} → expected {1}")
    @MethodSource("normalConversionProvider")
    void toBigDecimal_normalConversion(Calendar calendar, BigDecimal expected) {
        // WHEN
        BigDecimal actual = CalendarConversions.toBigDecimal(calendar, mockConverter);
        // THEN
        assertEquals(expected, actual, "BigDecimal conversion of Calendar epoch millis is incorrect");
    }

    /* --------------------------------------------------------------
     *  Null input handling
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_nullInputThrowsNPE() {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CalendarConversions.toBigDecimal(null, null), "Method should throw NPE when the source object is null");
    }

    @Test
    void toBigDecimal_dateInstanceThrowsClassCastException() {
        // GIVEN – a java.util.Date (different type)
        Date date = new Date();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CalendarConversions.toBigDecimal(date, mockConverter), "Expected ClassCastException when source object is a Date");
    }

    /* --------------------------------------------------------------
     *  Epoch boundary values
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_epochZero() {
        Calendar cal = createUtcCalendar(1970, Calendar.JANUARY, 1, 0, 0, 0, 0);
        BigDecimal expected = BigDecimal.ZERO.divide(BigDecimalConversions.GRAND);
        BigDecimal actual = CalendarConversions.toBigDecimal(cal, mockConverter);
        assertEquals(expected, actual, "Epoch 0 should produce zero BigDecimal after division");
    }

    @Test
    void toBigDecimal_oneMillisecondBeforeEpoch() {
        // 1969‑12‑31T23:59:59.999Z => -1 ms
        Calendar cal = createUtcCalendar(1969, Calendar.DECEMBER, 31, 23, 59, 59, 999);
        // setTimeInMillis with -1 to ensure the negative value
        cal.setTimeInMillis(-1L);
        BigDecimal expected = new BigDecimal(-1L).divide(BigDecimalConversions.GRAND);
        BigDecimal actual = CalendarConversions.toBigDecimal(cal, mockConverter);
        assertEquals(expected, actual, "Negative epoch millis should be handled correctly");
    }

    /* --------------------------------------------------------------
     *  Timezone offset handling
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_timezoneNewYork() {
        // 2023‑01‑01 12:00 in America/New_York (UTC‑5) => 2023‑01‑01T17:00Z
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Calendar.getInstance(tz);
        cal.set(2023, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // Force the calendar to compute the time value
        // should be 1672578000000L
        long epochMillis = cal.getTimeInMillis();
        BigDecimal expected = new BigDecimal(epochMillis).divide(BigDecimalConversions.GRAND);
        BigDecimal actual = CalendarConversions.toBigDecimal(cal, mockConverter);
        assertEquals(expected, actual, "Timezone offset should be correctly normalised to UTC");
    }

    @Test
    void toBigDecimal_timezoneTokyo() {
        // 2023‑07‑15 23:45:30.250 in Asia/Tokyo (UTC+9)
        TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
        Calendar cal = Calendar.getInstance(tz);
        cal.set(2023, Calendar.JULY, 15, 23, 45, 30);
        cal.set(Calendar.MILLISECOND, 250);
        // expected 1689453930250L
        long epochMillis = cal.getTimeInMillis();
        BigDecimal expected = new BigDecimal(epochMillis).divide(BigDecimalConversions.GRAND);
        BigDecimal actual = CalendarConversions.toBigDecimal(cal, mockConverter);
        assertEquals(expected, actual, "Positive timezone offset should be handled correctly");
    }

    /* --------------------------------------------------------------
     *  Converter argument ignored
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_converterIgnoredEvenIfFaulty() {
        // Calendar set to a known instant
        Calendar cal = createUtcCalendar(2022, Calendar.MAY, 10, 8, 15, 0, 0);
        // Mock converter that throws on any method call
        Converter faultyConverter = mock(Converter.class);
        // any method
        doThrow(new RuntimeException("Should not be called")).when(faultyConverter).toString();
        BigDecimal expected = new BigDecimal(cal.getTimeInMillis()).divide(BigDecimalConversions.GRAND);
        BigDecimal actual = CalendarConversions.toBigDecimal(cal, faultyConverter);
        assertEquals(expected, actual, "Method must ignore the converter argument");
    }

    /* --------------------------------------------------------------
     *  Subclass handling (GregorianCalendar)
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_gregorianCalendarLeapDay() {
        GregorianCalendar greg = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        greg.set(2000, Calendar.FEBRUARY, 29, 0, 0, 0);
        greg.set(Calendar.MILLISECOND, 0);
        // 951782400000L
        long epochMillis = greg.getTimeInMillis();
        BigDecimal expected = new BigDecimal(epochMillis).divide(BigDecimalConversions.GRAND);
        BigDecimal actual = CalendarConversions.toBigDecimal(greg, mockConverter);
        assertEquals(expected, actual, "GregorianCalendar (leap day) should be handled like Calendar");
    }

    /* --------------------------------------------------------------
     *  Large future date
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_largeFutureDate() {
        Calendar cal = createUtcCalendar(3000, Calendar.JANUARY, 1, 0, 0, 0, 0);
        // 32503680000000L
        long epochMillis = cal.getTimeInMillis();
        BigDecimal expected = new BigDecimal(epochMillis).divide(BigDecimalConversions.GRAND);
        BigDecimal actual = CalendarConversions.toBigDecimal(cal, mockConverter);
        assertEquals(expected, actual, "Very large epoch millis should be processed without overflow");
    }

    /* --------------------------------------------------------------
     *  Calendar#getTime() returns null (mocked)
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_calendarGetTimeReturnsNull() {
        Calendar mockCalendar = mock(Calendar.class);
        when(mockCalendar.getTime()).thenReturn(null);
        // The cast succeeds, but the subsequent call on null should throw NPE
        assertThrows(NullPointerException.class, () -> CalendarConversions.toBigDecimal(mockCalendar, mockConverter), "When Calendar#getTime() returns null a NPE should propagate");
    }
}
