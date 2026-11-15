package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.LocalTime;
import java.time.DateTimeException;

/**
 * Comprehensive JUnit‑4 test suite for {@link LongConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical successful conversions from milliseconds to {@link LocalTime}.</li>
 *   <li>Boundary values (0 ms and the maximum valid millisecond value).</li>
 *   <li>Invalid inputs: negative values, values larger than a day, and non‑Long objects.</li>
 *   <li>Verification that the thrown {@link IllegalArgumentException} contains the original
 *       {@link DateTimeException} as its cause.</li>
 * </ul>
 *
 * The {@code Converter} argument of {@code LongConversions.toLocalTime} is not used by the
 * implementation, therefore {@code null} is passed safely.
 */
public class LongConversionsTest {

    /** Helper to invoke the package‑private conversion method. */
    private static LocalTime convert(Object from) {
        // The method does not use the Converter, so we pass null.
        return LongConversions.toLocalTime(from, null);
    }

    /** Typical conversion – 0 ms should map to midnight. */
    @Test
    public void testZeroMilliseconds() {
        LocalTime result = convert(Long.valueOf(0L));
        Assert.assertEquals(LocalTime.MIDNIGHT, result);
    }

    /** Typical conversion – 1 second (1000 ms) should map to 00:00:01. */
    @Test
    public void testOneSecond() {
        LocalTime expected = LocalTime.of(0, 0, 1, 0);
        LocalTime result   = convert(Long.valueOf(1_000L));
        Assert.assertEquals(expected, result);
    }

    /** Boundary conversion – the greatest millisecond value that fits in a day
     *  (86 399 999 ms = 23:59:59.999). */
    @Test
    public void testMaximumValidMillisecond() {
        long maxMillis = 86_399_999L; // 24 h – 1 ms
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000);
        LocalTime result   = convert(Long.valueOf(maxMillis));
        Assert.assertEquals(expected, result);
    }

    /** Negative millisecond value must result in IllegalArgumentException with a
     *  helpful message and the original DateTimeException as the cause. */
    @Test
    public void testNegativeMilliseconds() {
        long negative = -1L;
        try {
            convert(Long.valueOf(negative));
            Assert.fail("Expected IllegalArgumentException for negative milliseconds");
        } catch (IllegalArgumentException ex) {
            // Verify message contains the offending value
            Assert.assertTrue("Message should contain the input value",
                    ex.getMessage().contains("[" + negative + "]"));
            // Verify the cause is a DateTimeException thrown by LocalTime.ofNanoOfDay
            Assert.assertNotNull("Cause must be present", ex.getCause());
            Assert.assertTrue("Cause should be DateTimeException",
                    ex.getCause() instanceof DateTimeException);
        }
    }

    /** Millisecond value larger than the number of milliseconds in a day must also
     *  raise IllegalArgumentException. */
    @Test
    public void testTooLargeMilliseconds() {
        long tooLarge = 86_400_000L; // exactly 24 h, which is out of range for LocalTime
        try {
            convert(Long.valueOf(tooLarge));
            Assert.fail("Expected IllegalArgumentException for too‑large milliseconds");
        } catch (IllegalArgumentException ex) {
            Assert.assertTrue("Message should contain the input value",
                    ex.getMessage().contains("[" + tooLarge + "]"));
            Assert.assertNotNull("Cause must be present", ex.getCause());
            Assert.assertTrue("Cause should be DateTimeException",
                    ex.getCause() instanceof DateTimeException);
        }
    }

    /** Passing an object that is not a {@link Long} results in a ClassCastException
     *  before any conversion attempt. */
    @Test(expected = ClassCastException.class)
    public void testNonLongInput() {
        // Integer cannot be cast to Long inside the method.
        convert(Integer.valueOf(123));
    }

    /** Verify that the method works when the Long instance is provided via the
     *  primitive‑boxing conversion (i.e., as a plain {@code long} argument). */
    @Test
    public void testPrimitiveLongAutoboxing() {
        long millis = 12_345L;
        LocalTime expected = LocalTime.ofNanoOfDay(millis * 1_000_000L);
        LocalTime result   = convert(millis); // autoboxes to Long
        Assert.assertEquals(expected, result);
    }

    /** Re‑run a valid conversion several times to ensure no hidden state is kept
     *  inside the static method. */
    @Test
    public void testRepeatedValidConversions() {
        for (int i = 0; i < 10; i++) {
            long millis = i * 10_000L; // 0, 10 000, 20 000, …
            LocalTime expected = LocalTime.ofNanoOfDay(millis * 1_000_000L);
            LocalTime result   = convert(Long.valueOf(millis));
            Assert.assertEquals("Iteration " + i + " failed", expected, result);
        }
    }
}
