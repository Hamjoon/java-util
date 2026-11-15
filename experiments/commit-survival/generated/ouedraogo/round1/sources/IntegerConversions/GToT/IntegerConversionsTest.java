package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.time.DateTimeException;
import java.time.LocalTime;

/**
 * Tests for {@link IntegerConversions}.
 *
 * The tests cover:
 *   • normal conversion from an {@code Integer} representing milliseconds since midnight,
 *   • null input,
 *   • wrong input type,
 *   • negative milliseconds,
 *   • milliseconds that exceed a 24‑hour day.
 *
 * No mocking frameworks are used – real {@link Converter} and {@link ConverterOptions}
 * objects are instantiated as required by the library API.
 */
public class IntegerConversionsTest {

    private Converter converter;   // a real Converter instance (its internal behaviour is not needed)

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * Helper that converts a millisecond count to the expected LocalTime.
     */
    private static LocalTime expectedLocalTimeFromMillis(int ms) {
        long nanos = ((long) ms) * 1_000_000L;
        // LocalTime.ofNanoOfDay throws DateTimeException for out‑of‑range values,
        // which is exactly what we want to verify in the edge‑case tests.
        return LocalTime.ofNanoOfDay(nanos);
    }

    // -----------------------------------------------------------------------
    // 1. Normal (happy‑path) conversion
    // -----------------------------------------------------------------------
    @Test
    public void testToLocalTime_normalValue() {
        int millis = 12_345;                     // 12.345 seconds after midnight
        LocalTime expected = expectedLocalTimeFromMillis(millis);

        LocalTime result = IntegerConversions.toLocalTime(millis, converter);

        assertEquals("LocalTime should match the expected value for normal milliseconds",
                     expected, result);
    }

    // -----------------------------------------------------------------------
    // 2. Edge case – null input
    // -----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToLocalTime_nullInput() {
        IntegerConversions.toLocalTime(null, converter);
    }

    // -----------------------------------------------------------------------
    // 3. Edge case – wrong type (e.g., Long instead of Integer)
    // -----------------------------------------------------------------------
    @Test(expected = ClassCastException.class)
    public void testToLocalTime_wrongType() {
        Long notAnInteger = 5_000L;
        IntegerConversions.toLocalTime(notAnInteger, converter);
    }

    // -----------------------------------------------------------------------
    // 4. Edge case – negative milliseconds
    // -----------------------------------------------------------------------
    @Test(expected = DateTimeException.class)
    public void testToLocalTime_negativeMillis() {
        int negative = -1;
        IntegerConversions.toLocalTime(negative, converter);
    }

    // -----------------------------------------------------------------------
    // 5. Edge case – milliseconds greater than or equal to 24 h
    // -----------------------------------------------------------------------
    @Test(expected = DateTimeException.class)
    public void testToLocalTime_millisBeyondOneDay() {
        // 24 hours = 86_400_000 ms; using exactly that value should overflow LocalTime.
        int beyondDay = 86_400_000;
        IntegerConversions.toLocalTime(beyondDay, converter);
    }
}
