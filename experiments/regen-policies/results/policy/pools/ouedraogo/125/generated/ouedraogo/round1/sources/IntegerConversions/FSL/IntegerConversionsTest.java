package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.time.LocalTime;

import org.junit.Test;

/**
 * Unit tests for {@link IntegerConversions}.
 *
 * These tests verify:
 * <ul>
 *   <li>Correct conversion of an {@link Integer} representing milliseconds since midnight
 *       to a {@link LocalTime} instance.</li>
 *   <li>Behaviour when {@code null} is supplied as the source object.</li>
 *   <li>Behaviour when the source object is not an {@link Integer} (e.g., {@link Long}).</li>
 * </ul>
 *
 * The tests rely only on the public API of {@link Converter} and the
 * {@link LongConversions} delegate used by {@code IntegerConversions}.
 */
public class IntegerConversionsTest {

    /** Helper method to create a default {@link Converter} instance. */
    private static Converter createDefaultConverter() {
        // The library provides a public constructor that accepts a ConverterOptions instance.
        // ConverterOptions is assumed to have a public no‑argument constructor.
        return new Converter(new ConverterOptions());
    }

    /**
     * Verify that a valid {@link Integer} value representing milliseconds is correctly
     * transformed into a {@link LocalTime}. The expected value is calculated using
     * {@link LocalTime#ofNanoOfDay(long)} which interprets the argument as nanoseconds
     * from midnight.
     */
    @Test
    public void testToLocalTime_ValidInteger() {
        Converter converter = createDefaultConverter();

        // Test a set of representative millisecond values.
        int[] msValues = {0, 1, 999, 1_000, 12_345, 86_399_999}; // up to just before next day
        for (int ms : msValues) {
            Object source = Integer.valueOf(ms);
            LocalTime expected = LocalTime.ofNanoOfDay(((long) ms) * 1_000_000L);
            LocalTime actual = IntegerConversions.toLocalTime(source, converter);
            assertEquals("Conversion failed for ms = " + ms, expected, actual);
        }
    }

    /**
     * Verify that passing {@code null} as the source object results in a {@link NullPointerException}
     * because the method attempts to cast the argument to {@link Integer}.
     */
    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullSource() {
        Converter converter = createDefaultConverter();
        IntegerConversions.toLocalTime(null, converter);
    }

    /**
     * Verify that passing an object that is not an {@link Integer} (e.g., a {@link Long})
     * results in a {@link ClassCastException}. The method performs an unchecked cast.
     */
    @Test(expected = ClassCastException.class)
    public void testToLocalTime_NonIntegerSource() {
        Converter converter = createDefaultConverter();
        Object notAnInteger = Long.valueOf(1234L);
        IntegerConversions.toLocalTime(notAnInteger, converter);
    }

    /**
     * Verify that the {@code Converter} instance is indeed forwarded to the
     * {@link LongConversions#toLocalTime(long, Converter)} delegate.
     *
     * This test uses a {@link Converter} that has a custom conversion registered for
     * {@code Long → LocalTime}. The custom conversion simply returns a fixed sentinel
     * value, allowing us to confirm that the delegate was invoked with the supplied
     * {@code Converter}.
     */
    @Test
    public void testToLocalTime_ConverterIsUsedByDelegate() {
        // Create a Converter and register a custom conversion for Long → LocalTime.
        Converter converter = createDefaultConverter();
        final LocalTime sentinel = LocalTime.of(23, 59, 59);
        converter.addConversion(Long.class, LocalTime.class, (from, conv) -> sentinel);

        // The integer value does not matter because the custom conversion will ignore it.
        Integer source = Integer.valueOf(42);
        LocalTime result = IntegerConversions.toLocalTime(source, converter);

        // The result should be the sentinel value supplied by our custom conversion.
        assertEquals("Delegate conversion did not use the supplied Converter", sentinel, result);
    }
}
