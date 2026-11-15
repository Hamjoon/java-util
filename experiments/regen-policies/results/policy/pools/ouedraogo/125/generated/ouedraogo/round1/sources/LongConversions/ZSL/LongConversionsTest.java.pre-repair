package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.time.LocalTime;
import java.time.DateTimeException;

/**
 * Unit tests for {@link LongConversions}.
 *
 * The class under test is package‑private with a package‑private static method,
 * therefore the test class lives in the same package.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversions (midnight, one second, maximum valid value)</li>
 *   <li>Boundary checks (negative value, value larger than a day)</li>
 *   <li>Invalid input types (null, non‑Long objects)</li>
 *   <li>Verification that the supplied {@link Converter} argument is not used</li>
 * </ul>
 */
public class LongConversionsTest {

    /**
     * Helper that calls the package‑private method via reflection.
     * This avoids making the test class depend on the method being
     * public while still allowing us to pass any {@link Converter}
     * instance (including {@code null}).
     */
    private static LocalTime invokeToLocalTime(Object from, Converter converter) {
        try {
            java.lang.reflect.Method m = LongConversions.class.getDeclaredMethod("toLocalTime", Object.class, Converter.class);
            m.setAccessible(true);
            //noinspection unchecked
            return (LocalTime) m.invoke(null, from, converter);
        } catch (Exception e) {
            // Wrap any reflection exception as a RuntimeException for simplicity.
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMidnightConversion() {
        LocalTime result = invokeToLocalTime(0L, null);
        Assert.assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    public void testOneSecondConversion() {
        // 1,000 milliseconds = 1 second
        LocalTime result = invokeToLocalTime(1_000L, null);
        Assert.assertEquals(LocalTime.of(0, 0, 1), result);
    }

    @Test
    public void testMaximumValidValueConversion() {
        // 86,399,999 milliseconds = 23:59:59.999
        long maxMillis = 86_399_999L;
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000); // 999 ms = 999,000,000 ns
        LocalTime result = invokeToLocalTime(maxMillis, null);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testNegativeValueThrowsException() {
        long negative = -1L;
        try {
            invokeToLocalTime(negative, null);
            Assert.fail("Expected IllegalArgumentException for negative input");
        } catch (RuntimeException re) {
            // The reflection wrapper throws RuntimeException; unwrap the cause.
            Throwable cause = re.getCause();
            Assert.assertTrue("Root cause should be IllegalArgumentException",
                    cause instanceof IllegalArgumentException);
            IllegalArgumentException iae = (IllegalArgumentException) cause;
            Assert.assertTrue("Message should contain the offending value",
                    iae.getMessage().contains("[" + negative + "]"));
            // The cause of the IllegalArgumentException should be a DateTimeException
            Assert.assertTrue("Cause should be DateTimeException",
                    iae.getCause() instanceof DateTimeException);
        }
    }

    @Test
    public void testTooLargeValueThrowsException() {
        long tooLarge = 86_400_000L; // exactly 24 hours, out of range
        try {
            invokeToLocalTime(tooLarge, null);
            Assert.fail("Expected IllegalArgumentException for out‑of‑range input");
        } catch (RuntimeException re) {
            Throwable cause = re.getCause();
            Assert.assertTrue(cause instanceof IllegalArgumentException);
            IllegalArgumentException iae = (IllegalArgumentException) cause;
            Assert.assertTrue(iae.getMessage().contains("[" + tooLarge + "]"));
            Assert.assertTrue(iae.getCause() instanceof DateTimeException);
        }
    }

    @Test
    public void testNullInputThrowsNullPointerException() {
        try {
            invokeToLocalTime(null, null);
            Assert.fail("Expected NullPointerException when 'from' is null");
        } catch (RuntimeException re) {
            Throwable cause = re.getCause();
            Assert.assertTrue(cause instanceof NullPointerException);
        }
    }

    @Test
    public void testNonLongInputThrowsClassCastException() {
        try {
            invokeToLocalTime("12345", null);
            Assert.fail("Expected ClassCastException when 'from' is not a Long");
        } catch (RuntimeException re) {
            Throwable cause = re.getCause();
            Assert.assertTrue(cause instanceof ClassCastException);
        }
    }

    @Test
    public void testConverterParameterIsIgnored() {
        // The method does not use the Converter argument. Pass a dummy instance.
        Converter dummy = new Converter(new ConverterOptions()); // assuming a default options ctor
        LocalTime result = invokeToLocalTime(2_000L, dummy);
        Assert.assertEquals(LocalTime.of(0, 0, 2), result);
    }
}
