package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toDouble_Test {

    @Mock
    private Converter // Converter is not used by the method, a mock is sufficient
    mockConverter;

    private Calendar calendar;

    /**
     * Calendar that throws a RuntimeException from {@code getTime()}.
     */
    private static class CustomCalendarThrowingGetTime extends GregorianCalendar {

        @Override
        public Date getTime() {
            throw new RuntimeException("boom");
        }
    }

    /**
     * Calendar that returns {@code null} from {@code getTime()}.
     */
    private static class CustomCalendarReturningNull extends GregorianCalendar {

        @Override
        public Date getTime() {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use UTC to avoid time‑zone side effects when converting epoch millis
        calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the calendar reference for safety
        calendar = null;
    }

    @Test
    void toDouble_returnsCorrectSecondsForPositiveEpoch() {
        // given
        // 2009‑02‑13T23:31:30.890Z
        long epochMillis = 1_234_567_890L;
        calendar.setTimeInMillis(epochMillis);
        double expected = epochMillis / 1000.0;
        // when
        double actual = CalendarConversions.toDouble(calendar, mockConverter);
        // then
        assertEquals(expected, actual, 1e-12, "toDouble should convert epoch milliseconds to seconds as a double");
    }

    @Test
    void toDouble_returnsCorrectSecondsForZeroEpoch() {
        // given
        // 1970‑01‑01T00:00:00Z
        long epochMillis = 0L;
        calendar.setTimeInMillis(epochMillis);
        double expected = 0.0;
        // when
        double actual = CalendarConversions.toDouble(calendar, mockConverter);
        // then
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    void toDouble_returnsCorrectSecondsForNegativeEpoch() {
        // given
        // 1969‑12‑31T23:59:59.000Z
        long epochMillis = -1_000L;
        calendar.setTimeInMillis(epochMillis);
        double expected = epochMillis / 1000.0;
        // when
        double actual = CalendarConversions.toDouble(calendar, mockConverter);
        // then
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    void toDouble_throwsNullPointerExceptionWhenFromIsNull() {
        // given
        Object nullCalendar = null;
        // when / then
        assertThrows(NullPointerException.class, () -> CalendarConversions.toDouble(nullCalendar, mockConverter));
    }

    /* --------------------------------------------------------------
     *  Fractional seconds – milliseconds retained
     * -------------------------------------------------------------- */
    @Test
    void toDouble_preservesFractionalMilliseconds() {
        // 2023‑03‑15T12:34:56.789Z  => epoch millis = 1678887296789
        calendar.setTimeInMillis(1_678_887_296_789L);
        // 1678887296.789
        double expected = 1_678_887_296_789L / 1000.0;
        double actual = CalendarConversions.toDouble(calendar, mockConverter);
        assertEquals(expected, actual, 1e-12);
    }

    /* --------------------------------------------------------------
     *  Different time‑zone – same instant
     * -------------------------------------------------------------- */
    @Test
    void toDouble_ignoresCalendarTimeZone() {
        // Same instant as 2023‑03‑15T12:34:56Z but created with New York zone
        Calendar nyCal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        // epoch millis for 2023‑03‑15T12:34:56Z
        nyCal.setTimeInMillis(1_678_887_296_000L);
        double actual = CalendarConversions.toDouble(nyCal, mockConverter);
        assertEquals(1_678_887_296.0, actual, 1e-12);
    }

    /* --------------------------------------------------------------
     *  Far‑future and far‑past dates
     * -------------------------------------------------------------- */
    @Test
    void toDouble_handlesFarFutureDate() {
        // 3000‑01‑01T00:00:00Z
        calendar.setTimeInMillis(32_503_636_800_000L);
        double actual = CalendarConversions.toDouble(calendar, mockConverter);
        assertEquals(32_503_636_800.0, actual, 1e-6);
    }

    @Test
    void toDouble_handlesFarPastDate() {
        // 0001‑01‑01T00:00:00Z (proleptic Gregorian)
        calendar.setTimeInMillis(-62_135_596_800_000L);
        double actual = CalendarConversions.toDouble(calendar, mockConverter);
        assertEquals(-62_135_596_800.0, actual, 1e-6);
    }

    /* --------------------------------------------------------------
     *  Calendar subclasses (BuddhistCalendar)
     * -------------------------------------------------------------- */
    @Test
    void toDouble_worksWithBuddhistCalendarSubclass() {
        BuddhistCalendar buddhistCal = new BuddhistCalendar(TimeZone.getTimeZone("UTC"));
        // 2023‑03‑15T12:34:56Z
        buddhistCal.setTimeInMillis(1_678_887_296_000L);
        double actual = CalendarConversions.toDouble(buddhistCal, mockConverter);
        assertEquals(1_678_887_296.0, actual, 1e-12);
    }

    @Test
    void toDouble_throwsClassCastExceptionWhenFromIsNotCalendar_String() {
        assertThrows(ClassCastException.class, () -> CalendarConversions.toDouble("not a calendar", mockConverter));
    }

    @Test
    void toDouble_throwsClassCastExceptionWhenFromIsNotCalendar_Date() {
        Date date = new Date();
        assertThrows(ClassCastException.class, () -> CalendarConversions.toDouble(date, mockConverter));
    }

    /* --------------------------------------------------------------
     *  Non‑null `converter` argument (unused)
     * -------------------------------------------------------------- */
    @Test
    void toDouble_ignoresNonNullConverter() {
        // 2022‑01‑01T00:00:00Z
        calendar.setTimeInMillis(1_640_995_200_000L);
        double actual = CalendarConversions.toDouble(calendar, mockConverter);
        assertEquals(1_640_995_200.0, actual, 1e-12);
    }

    /* --------------------------------------------------------------
     *  Hostile converter (methods throw) – should not affect outcome
     * -------------------------------------------------------------- */
    @Test
    void toDouble_ignoresConverterThatThrowsOnMethodCalls() {
        // Configure the mock to throw on any method call (just in case)
        org.mockito.Mockito.doThrow(new RuntimeException("converter boom")).when(mockConverter).toString(org.mockito.ArgumentMatchers.any());
        // 2022‑01‑01T00:00:00Z
        calendar.setTimeInMillis(1_640_995_200_000L);
        double actual = CalendarConversions.toDouble(calendar, mockConverter);
        assertEquals(1_640_995_200.0, actual, 1e-12);
    }

    /* --------------------------------------------------------------
     *  Calendar subclass where getTime() misbehaves
     * -------------------------------------------------------------- */
    @Test
    void toDouble_propagatesRuntimeExceptionFromGetTime() {
        Calendar throwingCal = new CustomCalendarThrowingGetTime();
        // value irrelevant – getTime() throws first
        throwingCal.setTimeInMillis(0L);
        assertThrows(RuntimeException.class, () -> CalendarConversions.toDouble(throwingCal, mockConverter));
    }

    @Test
    void toDouble_throwsNullPointerExceptionWhenGetTimeReturnsNull() {
        Calendar nullCal = new CustomCalendarReturningNull();
        nullCal.setTimeInMillis(0L);
        assertThrows(NullPointerException.class, () -> CalendarConversions.toDouble(nullCal, mockConverter));
    }
}
