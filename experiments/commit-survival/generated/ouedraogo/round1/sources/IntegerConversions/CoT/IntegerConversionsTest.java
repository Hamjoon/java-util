package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.LocalTime;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@code IntegerConversions}.
 *
 * The class under test is package‑private, therefore the test class resides in the same
 * package so that it can access the static {@code toLocalTime} method.
 *
 * The tests cover:
 *   1. Normal conversion of a valid {@link Integer} millisecond value.
 *   2. Edge cases such as {@code null}, non‑{@code Integer} input, negative values and
 *      values that exceed a single day.
 *   3. Verification that the proper exceptions are thrown for illegal arguments.
 */
public class IntegerConversionsTest {

    private Converter converter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // The real Converter implementation only needs a ConverterOptions instance.
        // Using the default constructor of ConverterOptions (assumed to exist) is sufficient
        // for these tests because IntegerConversions does not inspect the options.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that mirrors the expected behaviour of {@code LongConversions.toLocalTime}.
     * The real implementation converts a millisecond count since midnight to a {@link LocalTime}.
     * This method is used only to compute the expected value for assertions.
     */
    private static LocalTime expectedLocalTimeFromMillis(long ms) {
        // Normalise the millisecond value to the range [0, 86_400_000) (number of ms in a day)
        long msInDay = 86_400_000L;
        long normalized = ((ms % msInDay) + msInDay) % msInDay; // handles negative values
        int hour = (int) (normalized / 3_600_000L);
        int minute = (int) ((normalized % 3_600_000L) / 60_000L);
        int second = (int) ((normalized % 60_000L) / 1_000L);
        int nano = (int) ((normalized % 1_000L) * 1_000_000);
        return LocalTime.of(hour, minute, second, nano);
    }

    /* --------------------------------------------------------------------- */
    /* 1. Basic functional test                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void toLocalTime_givenZeroMilliseconds_returnsMidnight() {
        Integer input = 0;
        LocalTime result = IntegerConversions.toLocalTime(input, converter);
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    public void toLocalTime_givenOneHourInMilliseconds_returnsCorrectTime() {
        Integer input = 3_600_000; // 1 hour = 3,600,000 ms
        LocalTime result = IntegerConversions.toLocalTime(input, converter);
        assertEquals(LocalTime.of(1, 0), result);
    }

    @Test
    public void toLocalTime_givenArbitraryMilliseconds_returnsExpectedLocalTime() {
        Integer input = 12_345_678; // arbitrary value
        LocalTime expected = expectedLocalTimeFromMillis(input.longValue());
        LocalTime actual = IntegerConversions.toLocalTime(input, converter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- */
    /* 2. Edge‑case / exception tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void toLocalTime_givenNullInput_throwsNullPointerException() {
        thrown.expect(NullPointerException.class);
        IntegerConversions.toLocalTime(null, converter);
    }

    @Test
    public void toLocalTime_givenNonIntegerInput_throwsClassCastException() {
        thrown.expect(ClassCastException.class);
        // Passing a String where an Integer is expected forces the cast inside the method to fail
        IntegerConversions.toLocalTime("not-an-integer", converter);
    }

    @Test
    public void toLocalTime_givenNegativeMilliseconds_returnsWrappedLocalTime() {
        Integer input = -1_000; // -1 second
        // Expected behaviour: the negative value is wrapped to the previous day’s time
        LocalTime expected = expectedLocalTimeFromMillis(input.longValue());
        LocalTime actual = IntegerConversions.toLocalTime(input, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void toLocalTime_givenValueLargerThanOneDay_wrapsCorrectly() {
        // 2 days + 2 hours + 30 minutes = 2*86_400_000 + 2*3_600_000 + 30*60_000 = 194,580,000 ms
        Integer input = 194_580_000;
        LocalTime expected = expectedLocalTimeFromMillis(input.longValue());
        LocalTime actual = IntegerConversions.toLocalTime(input, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void toLocalTime_givenIntegerMaxValue_wrapsCorrectly() {
        Integer input = Integer.MAX_VALUE; // 2,147,483,647 ms (~24.85 days)
        LocalTime expected = expectedLocalTimeFromMillis(input.longValue());
        LocalTime actual = IntegerConversions.toLocalTime(input, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void toLocalTime_givenIntegerMinValue_wrapsCorrectly() {
        Integer input = Integer.MIN_VALUE; // -2,147,483,648 ms
        LocalTime expected = expectedLocalTimeFromMillis(input.longValue());
        LocalTime actual = IntegerConversions.toLocalTime(input, converter);
        assertEquals(expected, actual);
    }
}
