package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.LocalTime;

/**
 * Unit tests for {@link LongConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Successful conversion of valid millisecond values to {@link LocalTime}.</li>
 *   <li>Boundary conditions (zero, last valid millisecond of the day).</li>
 *   <li>Invalid inputs (negative values, values larger than the maximum allowed for a day).</li>
 *   <li>Incorrect input type (non‑Long) – ensures the cast behaves as expected.</li>
 * </ul>
 *
 * The {@link Converter} instance is required by the signature but is not used by the
 * implementation, therefore a minimal instance created with {@code null} options is sufficient.
 */
public class LongConversionsTest {

    // Helper to create a dummy Converter – the implementation under test does not use it.
    private static Converter dummyConverter() {
        return new Converter(null);
    }

    @Test
    public void testMidnightConversion() {
        Long millis = 0L;
        LocalTime result = LongConversions.toLocalTime(millis, dummyConverter());
        Assert.assertEquals("Midnight should be converted to LocalTime.MIDNIGHT", LocalTime.MIDNIGHT, result);
    }

    @Test
    public void testOneHourConversion() {
        // 1 hour = 60 minutes * 60 seconds * 1000 ms = 3_600_000 ms
        Long millis = 3_600_000L;
        LocalTime expected = LocalTime.of(1, 0, 0, 0);
        LocalTime result = LongConversions.toLocalTime(millis, dummyConverter());
        Assert.assertEquals("1 hour in milliseconds should convert to 01:00", expected, result);
    }

    @Test
    public void testLastValidMillisecondOfDay() {
        // 86,399,999 ms = 23:59:59.999
        Long millis = 86_399_999L;
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000);
        LocalTime result = LongConversions.toLocalTime(millis, dummyConverter());
        Assert.assertEquals("The last valid millisecond of the day should map to 23:59:59.999",
                expected, result);
    }

    @Test
    public void testNegativeMillisThrowsIllegalArgumentException() {
        Long millis = -1L;
        try {
            LongConversions.toLocalTime(millis, dummyConverter());
            Assert.fail("Expected IllegalArgumentException for negative millisecond value");
        } catch (IllegalArgumentException ex) {
            // Verify that the message contains the offending value and the expected range hint
            Assert.assertTrue("Exception message should contain the input value",
                    ex.getMessage().contains("[-1]"));
            Assert.assertTrue("Exception message should mention the valid range",
                    ex.getMessage().contains(">= 0 && <= 86399999"));
        }
    }

    @Test
    public void testTooLargeMillisThrowsIllegalArgumentException() {
        // 86,400,000 ms would represent exactly 24:00:00.000 which is out of range for LocalTime
        Long millis = 86_400_000L;
        try {
            LongConversions.toLocalTime(millis, dummyConverter());
            Assert.fail("Expected IllegalArgumentException for millisecond value exceeding the day");
        } catch (IllegalArgumentException ex) {
            // Check that the offending value appears in the message
            Assert.assertTrue("Exception message should contain the input value",
                    ex.getMessage().contains("[86400000]"));
            Assert.assertTrue("Exception message should mention the valid range",
                    ex.getMessage().contains(">= 0 && <= 86399999"));
        }
    }

    @Test
    public void testNonLongInputCausesClassCastException() {
        // The method casts the input to Long without checking; passing a String should cause a ClassCastException
        String notALong = "12345";
        try {
            LongConversions.toLocalTime(notALong, dummyConverter());
            Assert.fail("Expected ClassCastException when input is not a Long");
        } catch (ClassCastException ex) {
            // Expected path – no further assertions needed
        }
    }
}
