package com.cedarsoftware.util.convert;

import java.time.LocalTime;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LongConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical successful conversions (midnight, noon, arbitrary time).</li>
 *   <li>Edge‑case conversions (minimum value 0, maximum value 86 399 999 ms).</li>
 *   <li>Error handling for values outside the allowed range (negative and too‑large).</li>
 *   <li>Behaviour when a {@code null} {@link Converter} is supplied (the argument is ignored).</li>
 * </ul>
 */
public class LongConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the public API; we assume it has a public no‑arg ctor.
        dummyConverter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that directly invokes the package‑private method under test.
     */
    private static LocalTime invokeToLocalTime(Object from, Converter converter) {
        return LongConversions.toLocalTime(from, converter);
    }

    @Test
    public void testToLocalTimeMidnight() {
        Long millis = 0L;
        LocalTime result = invokeToLocalTime(millis, dummyConverter);
        assertEquals("Midnight should be converted to LocalTime.MIDNIGHT", LocalTime.MIDNIGHT, result);
    }

    @Test
    public void testToLocalTimeNoon() {
        Long millis = 12L * 60 * 60 * 1000; // 12:00:00.000
        LocalTime result = invokeToLocalTime(millis, dummyConverter);
        assertEquals("Noon should be converted to LocalTime.NOON", LocalTime.NOON, result);
    }

    @Test
    public void testToLocalTimeArbitraryTime() {
        // 01:23:45.678 -> 1 hour + 23 minutes + 45 seconds + 678 ms
        long millis = (1 * 60 * 60 + 23 * 60 + 45) * 1000L + 678;
        LocalTime expected = LocalTime.of(1, 23, 45, 678_000_000);
        LocalTime result = invokeToLocalTime(millis, dummyConverter);
        assertEquals("Arbitrary time conversion failed", expected, result);
    }

    @Test
    public void testToLocalTimeMaximumValue() {
        // 86 399 999 ms = 23:59:59.999
        Long millis = 86_399_999L;
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000);
        LocalTime result = invokeToLocalTime(millis, dummyConverter);
        assertEquals("Maximum allowed millis should map to 23:59:59.999", expected, result);
    }

    @Test
    public void testToLocalTimeNegativeValueThrows() {
        Long millis = -1L;
        try {
            invokeToLocalTime(millis, dummyConverter);
            fail("Expected IllegalArgumentException for negative millis");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention the allowed range",
                    e.getMessage().contains("must be >= 0 && <= 86399999"));
            assertEquals(millis, e.getCause() == null ? null : ((Number) e.getCause()).longValue());
        }
    }

    @Test
    public void testToLocalTimeTooLargeValueThrows() {
        Long millis = 86_400_000L; // exactly 24 hours – outside the valid range
        try {
            invokeToLocalTime(millis, dummyConverter);
            fail("Expected IllegalArgumentException for millis > 86_399_999");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention the allowed range",
                    e.getMessage().contains("must be >= 0 && <= 86399999"));
        }
    }

    @Test
    public void testToLocalTimeNullConverterIsIgnored() {
        Long millis = 1_000L; // 00:00:01.000
        LocalTime expected = LocalTime.of(0, 0, 1, 0);
        // Pass null for the Converter argument; method should still succeed because it never uses it.
        LocalTime result = invokeToLocalTime(millis, null);
        assertEquals("Conversion should succeed even when Converter is null", expected, result);
    }
}
