package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.LocalTime;

/**
 * Comprehensive JUnit‑4 test suite for {@link IntegerConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Typical conversions from {@code Integer} milliseconds to {@code LocalTime}</li>
 *   <li>Edge cases – zero, maximum {@code int} value, and negative values</li>
 *   <li>Error scenarios – {@code null} arguments and wrong source type</li>
 * </ul>
 *
 * <p>Only the public API of {@link Converter} (as supplied in the prompt) and the
 * classes that appear in the source file are used. No mocking frameworks are employed.
 */
public class IntegerConversionsTest {

    /** Helper to create a default {@link Converter} instance. */
    private static Converter defaultConverter() {
        // The library provides a public constructor that takes a ConverterOptions instance.
        // An empty options object is sufficient for the default conversion behaviour.
        return new Converter(new ConverterOptions());
    }

    /** Convert milliseconds expressed as an {@code Integer} to {@code LocalTime} using the class under test. */
    private static LocalTime convert(Integer ms, Converter converter) {
        return IntegerConversions.toLocalTime(ms, converter);
    }

    /* --------------------------------------------------------------
     *  Typical / happy‑path conversions
     * -------------------------------------------------------------- */

    @Test
    public void testToLocalTime_ZeroMilliseconds() {
        Converter cv = defaultConverter();
        LocalTime expected = LocalTime.of(0, 0, 0, 0);
        Assert.assertEquals(expected, convert(0, cv));
    }

    @Test
    public void testToLocalTime_OneHour() {
        // 1 hour = 60 min * 60 sec * 1000 ms = 3_600_000 ms
        Converter cv = defaultConverter();
        LocalTime expected = LocalTime.of(1, 0, 0, 0);
        Assert.assertEquals(expected, convert(3_600_000, cv));
    }

    @Test
    public void testToLocalTime_ComplexTime() {
        // 12:34:56.789  →  ((12*60 + 34)*60 + 56) * 1000 + 789 ms
        int ms = (int) (((12L * 60 + 34) * 60 + 56) * 1000 + 789);
        Converter cv = defaultConverter();
        LocalTime expected = LocalTime.of(12, 34, 56, 789_000_000);
        Assert.assertEquals(expected, convert(ms, cv));
    }

    @Test
    public void testToLocalTime_MaxIntValue() {
        // The maximum int value is 2_147_483_647 ms.
        // It corresponds to 596 hours, 7 minutes, 23 seconds and 647 ms.
        // LocalTime will wrap around after 24 hours, so we compute the expected
        // time‑of‑day using modulo arithmetic.
        int ms = Integer.MAX_VALUE;
        long totalNanos = ms * 1_000_000L; // convert to nanoseconds for LocalTime
        long nanosPerDay = 24L * 60 * 60 * 1_000_000_000L;
        long nanosOfDay = totalNanos % nanosPerDay;

        int hour = (int) (nanosOfDay / (60L * 60 * 1_000_000_000L));
        nanosOfDay %= 60L * 60 * 1_000_000_000L;
        int minute = (int) (nanosOfDay / (60L * 1_000_000_000L));
        nanosOfDay %= 60L * 1_000_000_000L;
        int second = (int) (nanosOfDay / 1_000_000_000L);
        int nano = (int) (nanosOfDay % 1_000_000_000L);

        LocalTime expected = LocalTime.of(hour, minute, second, nano);
        Converter cv = defaultConverter();

        Assert.assertEquals(expected, convert(ms, cv));
    }

    /* --------------------------------------------------------------
     *  Error / exceptional scenarios
     * -------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullFromArgument() {
        Converter cv = defaultConverter();
        IntegerConversions.toLocalTime(null, cv);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullConverterArgument() {
        // The implementation forwards the converter to LongConversions; a null
        // reference will cause a NPE inside that call.
        IntegerConversions.toLocalTime(0, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTime_WrongSourceType() {
        // Passing a Long (or any non‑Integer) should trigger the cast exception
        // inside IntegerConversions.toLocalTime.
        Converter cv = defaultConverter();
        IntegerConversions.toLocalTime(Long.valueOf(123L), cv);
    }

    @Test
    public void testToLocalTime_NegativeMilliseconds() {
        // The library is expected to reject negative millisecond values.
        // The exact exception type depends on LongConversions implementation;
        // we assert that *some* RuntimeException is thrown.
        Converter cv = defaultConverter();
        try {
            convert(-1, cv);
            Assert.fail("Expected an exception for negative milliseconds");
        } catch (RuntimeException ex) {
            // Accepted – the implementation should not silently produce a LocalTime.
            Assert.assertTrue("Exception should indicate illegal argument",
                    ex instanceof IllegalArgumentException ||
                    ex.getMessage().toLowerCase().contains("negative"));
        }
    }
}
