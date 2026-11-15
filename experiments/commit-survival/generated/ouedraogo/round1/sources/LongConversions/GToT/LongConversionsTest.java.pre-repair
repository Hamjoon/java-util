package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.LocalTime;

/**
 * Tests for {@link LongConversions}.
 *
 * The test class resides in the same package as {@code LongConversions}
 * so it can access the package‑private static method.
 */
public class LongConversionsTest {

    // The Converter argument is not used by LongConversions.toLocalTime,
    // but we provide a non‑null instance to keep the API contract.
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // Converter requires a ConverterOptions instance; we can use the default constructor.
        dummyConverter = new Converter(new ConverterOptions());
    }

    /** Helper that invokes the package‑private method via reflection. */
    private LocalTime invokeToLocalTime(Object from) {
        return LongConversions.toLocalTime(from, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  Typical / nominal conversions                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testZeroMillis() {
        LocalTime result = invokeToLocalTime(0L);
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    public void testTypicalValue() {
        // 1,234,567 ms = 20 minutes, 34 seconds and 567 milliseconds
        long millis = 1_234_567L;
        LocalTime expected = LocalTime.of(0, 20, 34, 567_000_000);
        LocalTime result = invokeToLocalTime(millis);
        assertEquals(expected, result);
    }

    @Test
    public void testMaximumValidMillis() {
        // 86,399,999 ms = 23:59:59.999
        long maxValid = 86_399_999L;
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000);
        LocalTime result = invokeToLocalTime(maxValid);
        assertEquals(expected, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge cases – values outside the accepted range                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNegativeMillisThrows() {
        long negative = -1L;
        try {
            invokeToLocalTime(negative);
            fail("Expected IllegalArgumentException for negative millis");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399999"));
            assertTrue(e.getMessage().contains(String.valueOf(negative)));
        }
    }

    @Test
    public void testMillisBeyond24HoursThrows() {
        long beyond = 86_400_000L; // exactly 24 hours
        try {
            invokeToLocalTime(beyond);
            fail("Expected IllegalArgumentException for millis >= 86_400_000");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399999"));
            assertTrue(e.getMessage().contains(String.valueOf(beyond)));
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Additional sanity checks (optional)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLargePositiveValueThrows() {
        // A value that would overflow the nanosecond range if accepted.
        long huge = Long.MAX_VALUE;
        try {
            invokeToLocalTime(huge);
            fail("Expected IllegalArgumentException for excessively large millis");
        } catch (IllegalArgumentException e) {
            // The exact message is not crucial; we only verify that the exception type is correct.
        }
    }
}
