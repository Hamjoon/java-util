package com.cedarsoftware.util.convert;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
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
class CalendarConversions_toAtomicLong_Test {

    private Converter converter;

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC‑5/‑4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    // Re‑usable time‑zones for the tests
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // The Converter is required by the method signature even though it is not used internally.
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
     * Creates a {@link GregorianCalendar} with the supplied fields.
     * The month argument follows the usual 1‑based convention (January = 1).
     */
    private static Calendar createGregorian(int year, int month, int day, int hour, int minute, int second, TimeZone tz) {
        Calendar cal = new GregorianCalendar(tz);
        // clear all fields first
        cal.clear();
        cal.set(year, month - 1, day, hour, minute, second);
        return cal;
    }

    /**
     * Provides a stream of successful conversion scenarios.
     * Each argument consists of:
     *   1. a textual description,
     *   2. the Calendar instance to convert,
     *   3. the expected epoch‑millisecond value.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// 2023‑01‑01T00:00:00Z (UTC) → 1672531200000L
        Arguments.of("GregorianCalendar 2023‑01‑01 UTC", createGregorian(2023, 1, 1, 0, 0, 0, UTC), 1672531200000L), // Calendar created with UTC now – tolerance of ±1000 ms
        Arguments.of("Current UTC time (tolerance test)", // will be adjusted inside the test
        Calendar.getInstance(UTC), // placeholder, handled specially (see test below)
        -1L), // Long.MAX_VALUE milliseconds
        Arguments.of("Calendar with Long.MAX_VALUE milliseconds", new GregorianCalendar() {

            {
                setTimeInMillis(Long.MAX_VALUE);
            }
        }, Long.MAX_VALUE), // Epoch start (1970‑01‑01T00:00:00Z)
        Arguments.of("Epoch start UTC", createGregorian(1970, 1, 1, 0, 0, 0, UTC), 0L), // Leap day 2000‑02‑29 12:34:56 America/New_York → 951825296000L
        Arguments.of("Leap day 2000‑02‑29 America/New_York", createGregorian(2000, 2, 29, 12, 34, 56, NEW_YORK), 951825296000L), // Asia/Tokyo calendar set to epoch zero
        Arguments.of("Asia/Tokyo calendar with epoch zero", new GregorianCalendar(TOKYO) {

            {
                setTimeInMillis(0L);
            }
        }, 0L), // Negative epoch millisecond (-1)
        Arguments.of("Calendar with -1 millisecond", new GregorianCalendar() {

            {
                setTimeInMillis(-1L);
            }
        }, -1L), // Long.MIN_VALUE milliseconds
        Arguments.of("Calendar with Long.MIN_VALUE milliseconds", new GregorianCalendar() {

            {
                setTimeInMillis(Long.MIN_VALUE);
            }
        }, Long.MIN_VALUE), // Lenient rollover: Feb 30 2024 becomes Mar 1 2024 (default zone)
        Arguments.of("Lenient rollover of invalid date (Feb 30 2024)", new GregorianCalendar() {

            {
                clear();
                set(Calendar.YEAR, 2024);
                // 1 (zero‑based)
                set(Calendar.MONTH, Calendar.FEBRUARY);
                // invalid, will roll over
                set(Calendar.DAY_OF_MONTH, 30);
                set(Calendar.HOUR_OF_DAY, 0);
                set(Calendar.MINUTE, 0);
                set(Calendar.SECOND, 0);
            }
        }, // Expected value is whatever the calendar computes after rollover
        // (we compute it lazily in the test body)
        -// placeholder, will be replaced
        1L));
    }

    /**
     * Provides inputs that should cause the method to throw an exception.
     * Each argument consists of:
     *   1. description,
     *   2. the object supplied as the {@code from} argument,
     *   3. the expected exception class.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null argument
        Arguments.of("Null 'from' argument", null, NullPointerException.class), // Wrong type (String)
        Arguments.of("Non‑Calendar object (String)", "2023-01-01", ClassCastException.class), // Strict (non‑lenient) calendar with invalid date → IllegalArgumentException
        Arguments.of("Non‑lenient calendar with invalid date", new GregorianCalendar() {

            {
                setLenient(false);
                clear();
                set(Calendar.YEAR, 2024);
                set(Calendar.MONTH, Calendar.FEBRUARY);
                set(Calendar.DAY_OF_MONTH, 30);
            }
        }, IllegalArgumentException.class), // Calendar subclass that throws RuntimeException from getTime()
        Arguments.of("Calendar subclass throwing RuntimeException from getTime()", new GregorianCalendar() {

            @Override
            public java.util.Date getTime() {
                throw new RuntimeException("forced exception");
            }
        }, RuntimeException.class), // Calendar subclass that returns null from getTime()
        Arguments.of("Calendar subclass returning null from getTime()", new GregorianCalendar() {

            @Override
            public java.util.Date getTime() {
                return null;
            }
        }, NullPointerException.class));
    }

    @Test
    void testCalendarToAtomicLong_Tokyo() {
        Calendar cal = createGregorian(2023, 6, 15, 12, 30, 45, TOKYO);
        cal.set(Calendar.MILLISECOND, 123);
        AtomicLong result = CalendarConversions.toAtomicLong(cal, converter);
        assertEquals(cal.getTimeInMillis(), result.get(), "AtomicLong value should match Calendar epoch milliseconds");
    }

    @Test
    void testCalendarToAtomicLong_Paris() {
        Calendar cal = createGregorian(1969, 12, 31, 23, 59, 59, TimeZone.getTimeZone("Europe/Paris"));
        cal.set(Calendar.MILLISECOND, 999);
        AtomicLong result = CalendarConversions.toAtomicLong(cal, converter);
        assertEquals(cal.getTimeInMillis(), result.get(), "AtomicLong value should match Calendar epoch milliseconds");
    }

    @Test
    void testCalendarToAtomicLong_NewYork() {
        Calendar cal = createGregorian(1970, 1, 1, 0, 0, 1, NEW_YORK);
        cal.set(Calendar.MILLISECOND, 0);
        AtomicLong result = CalendarConversions.toAtomicLong(cal, converter);
        assertEquals(cal.getTimeInMillis(), result.get(), "AtomicLong value should match Calendar epoch milliseconds");
    }

    @Test
    void testCalendarToAtomicLong_EdgeCases() {
        // Leap year, end of month, zero milliseconds
        Calendar cal = createGregorian(2000, 2, 29, 23, 59, 59, UTC);
        cal.set(Calendar.MILLISECOND, 0);
        AtomicLong result = CalendarConversions.toAtomicLong(cal, converter);
        assertEquals(cal.getTimeInMillis(), result.get(), "AtomicLong should correctly handle leap‑year dates");
        // Very early epoch (before 1970)
        Calendar early = createGregorian(1960, 7, 15, 6, 0, 0, UTC);
        early.set(Calendar.MILLISECOND, 500);
        AtomicLong earlyResult = CalendarConversions.toAtomicLong(early, converter);
        assertEquals(early.getTimeInMillis(), earlyResult.get(), "AtomicLong should correctly handle dates before the Unix epoch");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void testValidConversions(String description, Calendar calendar, long expectedMillis) {
        AtomicLong result = CalendarConversions.toAtomicLong(calendar, converter);
        assertEquals(expectedMillis, result.get(), () -> "AtomicLong value mismatch for case: " + description);
    }

    /**
     * Special handling for the two placeholder cases in {@link #validConversionProvider()}:
     * <ul>
     *   <li>Current UTC time – we compare with a tolerance of ±1000 ms.</li>
     *   <li>Lenient rollover – we compute the expected value from the calendar itself.</li>
     * </ul>
     */
    @Test
    void testValidConversions_ToleranceAndRollover() {
        // ----- case: current UTC time (tolerance) -----
        Calendar utcNow = Calendar.getInstance(UTC);
        long now = System.currentTimeMillis();
        utcNow.setTimeInMillis(now);
        AtomicLong resultNow = CalendarConversions.toAtomicLong(utcNow, converter);
        assertTrue(Math.abs(resultNow.get() - now) <= 1000, "AtomicLong value should be within ±1000 ms of System.currentTimeMillis()");
        // ----- case: lenient rollover (Feb 30 2024) -----
        Calendar lenient = new GregorianCalendar() {

            {
                clear();
                set(Calendar.YEAR, 2024);
                set(Calendar.MONTH, Calendar.FEBRUARY);
                // invalid, rolls over to Mar 1
                set(Calendar.DAY_OF_MONTH, 30);
                set(Calendar.HOUR_OF_DAY, 0);
                set(Calendar.MINUTE, 0);
                set(Calendar.SECOND, 0);
            }
        };
        // Calendar already performed rollover
        long expectedRollover = lenient.getTimeInMillis();
        AtomicLong resultRollover = CalendarConversions.toAtomicLong(lenient, converter);
        assertEquals(expectedRollover, resultRollover.get(), "AtomicLong should reflect Calendar's lenient rollover result");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionProvider")
    void testExceptionScenarios(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CalendarConversions.toAtomicLong(input, converter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
