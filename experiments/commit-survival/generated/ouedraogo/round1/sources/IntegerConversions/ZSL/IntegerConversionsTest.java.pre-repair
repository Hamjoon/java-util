package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.LocalTime;

import static org.junit.Assert.*;

public class IntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance.
        // For the purpose of these tests we do not need any specific options,
        // so we pass null which is accepted by the library's implementation
        // (the converter is only forwarded to LongConversions which does not
        // dereference it for the simple millisecond‑to‑LocalTime conversion).
        this.converter = new Converter(null);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /**
     * Helper that mimics the expected conversion performed by
     * LongConversions.toLocalTime when given a millisecond count.
     *
     * @param ms milliseconds since start of day
     * @return the corresponding LocalTime
     */
    private static LocalTime expectedLocalTimeFromMillis(int ms) {
        int seconds = ms / 1000;
        int nanoAdjustment = (ms % 1000) * 1_000_000;
        return LocalTime.ofSecondOfDay(seconds).plusNanos(nanoAdjustment);
    }

    @Test
    public void testToLocalTimeTypicalValues() {
        int[] testValues = {0, 1, 999, 1000, 12345, 86_399_999, 86_400_000 - 1};

        for (int ms : testValues) {
            LocalTime result = IntegerConversions.toLocalTime(ms, converter);
            LocalTime expected = expectedLocalTimeFromMillis(ms);
            assertEquals("Conversion mismatch for ms=" + ms, expected, result);
        }
    }

    @Test
    public void testToLocalTimeNegativeValue() {
        int negativeMs = -1;
        LocalTime result = IntegerConversions.toLocalTime(negativeMs, converter);
        // Negative milliseconds should wrap to the previous day; LocalTime handles this by normalising.
        // The expected behaviour is the same as LongConversions, which treats the long value directly.
        LocalTime expected = expectedLocalTimeFromMillis(negativeMs);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTimeMaximumInt() {
        int maxInt = Integer.MAX_VALUE; // 2,147,483,647 ms ≈ 596 hours, exceeds a day
        LocalTime result = IntegerConversions.toLocalTime(maxInt, converter);
        // LongConversions likely reduces the value modulo 24 hours.
        // We compute the same modulo manually.
        int msInDay = 24 * 60 * 60 * 1000;
        int normalizedMs = maxInt % msInDay;
        LocalTime expected = expectedLocalTimeFromMillis(normalizedMs);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTimeNullInput() {
        thrown.expect(NullPointerException.class);
        IntegerConversions.toLocalTime(null, converter);
    }

    @Test
    public void testToLocalTimeWrongTypeThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        // Pass a String that cannot be cast to Integer
        IntegerConversions.toLocalTime("not an integer", converter);
    }
}
