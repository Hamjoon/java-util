package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.LocalTime;
import java.time.DateTimeException;

/**
 * Unit tests for {@link LongConversions}.
 *
 * The test class lives in the same package as {@code LongConversions}
 * so it can access the package‑private static method.
 */
public class LongConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor (as per the supplied API)
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------
     *  Basic functional tests
     * ------------------------------------------------- */

    @Test
    public void testToLocalTimeZeroMillis() {
        LocalTime result = LongConversions.toLocalTime(0L, converter);
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    public void testToLocalTimeOneSecond() {
        LocalTime expected = LocalTime.ofSecondOfDay(1); // 00:00:01
        LocalTime result = LongConversions.toLocalTime(1_000L, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTimeMidday() {
        long middayMillis = 12L * 60 * 60 * 1_000; // 12 hours in ms
        LocalTime expected = LocalTime.of(12, 0);
        LocalTime result = LongConversions.toLocalTime(middayMillis, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTimeMaximumValid() {
        long maxValidMillis = 86_399_999L; // 23:59:59.999
        // Expected nanoseconds = maxValidMillis * 1_000_000
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000);
        LocalTime result = LongConversions.toLocalTime(maxValidMillis, converter);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------
     *  Edge‑case / exception tests
     * ------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeNegativeMillis() {
        LongConversions.toLocalTime(-1L, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeBeyondDayMillis() {
        // 24 hours = 86_400_000 ms (one millisecond too many)
        LongConversions.toLocalTime(86_400_000L, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTimeNullFrom() {
        LongConversions.toLocalTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTimeWrongTypeFrom() {
        // Passing an Integer instead of Long
        LongConversions.toLocalTime(Integer.valueOf(123), converter);
    }

    @Test
    public void testToLocalTimeNullConverter() {
        // The converter argument is not used; passing null must not affect conversion.
        LocalTime expected = LocalTime.ofSecondOfDay(2); // 2 seconds
        LocalTime result = LongConversions.toLocalTime(2_000L, null);
        assertEquals(expected, result);
    }
}
