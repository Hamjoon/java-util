package com.cedarsoftware.util.convert;

import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.GregorianCalendar;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toLong_Test {

    /**
     * Converter instance – required by the method signature but not used internally.
     */
    private Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5/‑4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    /**
     * Common time‑zones used in the test data.
     */
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final TimeZone GMT_PLUS_5 = TimeZone.getTimeZone("GMT+05:00");

    private static final TimeZone DEFAULT_TZ = TimeZone.getDefault();

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is required for the method signature; the implementation does not use it.
        converter = new Converter(new DefaultConverterOptions());
        // The concrete Converter implementation is not relevant for the method under test.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Creates a {@link Calendar} with the supplied fields.
     *
     * @param year   the year
     * @param month  the month (1‑based, i.e. January = 1)
     * @param day    the day of month
     * @param hour   hour of day (0‑23)
     * @param minute minute of hour
     * @param second second of minute
     * @param millis millisecond within the second
     * @param tz     the time‑zone for the calendar
     * @return a fully‑initialised Calendar instance
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
     * Creates a {@link Calendar} with the supplied fields.
     *
     * @param year   the year (e.g. 2023)
     * @param month  the month (0‑based as required by {@link Calendar#set})
     * @param day    the day of month
     * @param hour   hour of day (0‑23)
     * @param minute minute of hour
     * @param second second of minute
     * @param millis millisecond within the second
     * @param tz     the time‑zone for the calendar
     * @return a fully‑initialised {@link Calendar}
     */
    private static Calendar create(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Normal conversion cases – default system zone is used.
     */
    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// 2023‑05‑15 12:30:45 (month is 4 because Calendar months are 0‑based)
        Arguments.of(new GregorianCalendar(2023, Calendar.MAY, 15, 12, 30, 45), 1684158645000L, "Standard GregorianCalendar, system default zone"), // Unix epoch
        Arguments.of(new GregorianCalendar(1970, Calendar.JANUARY, 1, 0, 0, 0), 0L, "Epoch start should yield 0"), // Far‑future year 3000
        Arguments.of(new GregorianCalendar(3000, Calendar.JANUARY, 1, 0, 0, 0), 32503680000000L, "Year 3000 conversion"));
    }

    /**
     * Time‑zone handling cases.
     */
    private static Stream<Arguments> timezoneProvider() {
        return Stream.of(// 2023‑01‑01 00:00:00 UTC
        Arguments.of(create(2023, Calendar.JANUARY, 1, 0, 0, 0, 0, UTC), 1672531200000L, "UTC midnight 2023‑01‑01"), // Same wall‑clock time in GMT+05:00 (i.e. 5 hours ahead of UTC)
        Arguments.of(create(2023, Calendar.JANUARY, 1, 0, 0, 0, 0, GMT_PLUS_5), 1672513200000L, "GMT+05:00 midnight 2023‑01‑01 corresponds to 2022‑12‑31 19:00 UTC"));
    }

    /**
     * Negative epoch (pre‑Unix‑epoch) cases.
     */
    private static Stream<Arguments> negativeEpochProvider() {
        return Stream.of(Arguments.of(create(1969, Calendar.DECEMBER, 31, 23, 59, 59, 0, UTC), -1000L, "One second before epoch"));
    }

    /**
     * Millisecond‑precision cases.
     */
    private static Stream<Arguments> milliPrecisionProvider() {
        return Stream.of(Arguments.of(create(1970, Calendar.JANUARY, 1, 0, 0, 0, 123, UTC), 123L, "Epoch start with 123 ms"));
    }

    /**
     * Non‑null converter handling – the converter is ignored.
     */
    private static Stream<Arguments> nonNullConverterProvider() {
        return Stream.of(Arguments.of(new GregorianCalendar(2022, Calendar.JUNE, 10, 15, 45, 30), 1654877130000L, "Converter argument should be ignored"));
    }

    @Test
    void testCalendarToLong_Tokyo() {
        Calendar cal = createCalendar(2023, 6, 15, 12, 30, 45, 123, TOKYO);
        // direct value from the Calendar
        long expected = cal.getTimeInMillis();
        Long actual = CalendarConversions.toLong(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void testCalendarToLong_Paris() {
        Calendar cal = createCalendar(1970, 1, 1, 0, 0, 0, 0, PARIS);
        long expected = cal.getTimeInMillis();
        Long actual = CalendarConversions.toLong(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void testCalendarToLong_NewYork() {
        Calendar cal = createCalendar(1969, 12, 31, 23, 59, 59, 999, NEW_YORK);
        long expected = cal.getTimeInMillis();
        Long actual = CalendarConversions.toLong(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void testCalendarToLong_NullInput() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLong(null, converter));
    }

    @Test
    void testCalendarToLong_NonCalendarInput() {
        String notACalendar = "I am not a Calendar";
        assertThrows(ClassCastException.class, () -> CalendarConversions.toLong(notACalendar, converter));
    }

    @Test
    void testCalendarToLong_ConverterIsIgnored() {
        Calendar cal = createCalendar(2000, 2, 29, 23, 59, 59, 999, TimeZone.getTimeZone("UTC"));
        long expected = cal.getTimeInMillis();
        // Pass a null converter – the method does not use it, so it should still work.
        Long actual = CalendarConversions.toLong(cal, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalConversionProvider")
    void testNormalConversion(Calendar from, long expected, String description) {
        Long actual = CalendarConversions.toLong(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("timezoneProvider")
    void testTimezoneHandling(Calendar from, long expected, String description) {
        Long actual = CalendarConversions.toLong(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("negativeEpochProvider")
    void testNegativeEpoch(Calendar from, long expected, String description) {
        Long actual = CalendarConversions.toLong(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("milliPrecisionProvider")
    void testMillisecondPrecision(Calendar from, long expected, String description) {
        Long actual = CalendarConversions.toLong(from, converter);
        assertEquals(expected, actual, description);
    }

    @Test
    void testNullInput() {
        assertThrows(NullPointerException.class, () -> CalendarConversions.toLong(null, converter), "Null Calendar should throw NullPointerException");
    }

    @Test
    void testInvalidType() {
        String notACalendar = "2023-01-01";
        assertThrows(ClassCastException.class, () -> CalendarConversions.toLong(notACalendar, converter), "Non‑Calendar input should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonNullConverterProvider")
    void testConverterIsIgnored(Calendar from, long expected, String description) {
        // Pass a dummy (non‑null) Converter instance.
        Converter dummy = new Converter(new DefaultConverterOptions());
        Long actual = CalendarConversions.toLong(from, dummy);
        assertEquals(expected, actual, description);
    }
}
