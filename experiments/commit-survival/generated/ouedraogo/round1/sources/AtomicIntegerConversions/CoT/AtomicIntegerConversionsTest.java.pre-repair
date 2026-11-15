package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.AtomicIntegerConversions;
import org.junit.Test;
import org.junit.Assert;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JUnit‑4 test suite for {@link AtomicIntegerConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal operation of each conversion method.</li>
 *   <li>Verification that a new {@link AtomicInteger} instance is created.</li>
 *   <li>Forwarding of the underlying {@code int}/`long` value to {@link LongConversions}.</li>
 *   <li>Edge‑case handling: {@code null} arguments and wrong source types.</li>
 * </ul>
 *
 * The {@link Converter} argument is not used by the current implementation of
 * {@code AtomicIntegerConversions}, therefore {@code null} is passed where a
 * {@code Converter} is required.  This keeps the tests independent from the
 * internal details of {@code Converter} and {@code ConverterOptions}.
 */
public class AtomicIntegerConversionsTest {

    /* ---------------------------------------------------------------------- */
    /*  Tests for toAtomicInteger(Object, Converter)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicIntegerCreatesNewInstance() {
        AtomicInteger original = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, null);

        // Must be a different object
        Assert.assertNotSame("Result should be a new instance", original, result);
        // But must hold the same int value
        Assert.assertEquals("Result should contain the same int value", original.get(), result.get());
    }

    @Test
    public void testToAtomicIntegerCopiesValue() {
        int[] testValues = {0, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 123456};

        for (int v : testValues) {
            AtomicInteger src = new AtomicInteger(v);
            AtomicInteger dst = AtomicIntegerConversions.toAtomicInteger(src, null);
            Assert.assertEquals("Value should be copied correctly for " + v, v, dst.get());
        }
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNullInputThrowsNPE() {
        AtomicIntegerConversions.toAtomicInteger(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerInvalidTypeThrowsClassCastException() {
        // Pass a String instead of an AtomicInteger – the cast inside the method should fail
        AtomicIntegerConversions.toAtomicInteger("not an atomic integer", null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for toLocalTime(Object, Converter)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLocalTimeForwardsValue() {
        int[] testValues = {0, 1, 60, 12345, -5000};

        for (int v : testValues) {
            AtomicInteger src = new AtomicInteger(v);
            LocalTime actual = AtomicIntegerConversions.toLocalTime(src, null);
            LocalTime expected = LongConversions.toLocalTime((long) v, null);
            Assert.assertEquals(
                    "LocalTime conversion should match LongConversions for value " + v,
                    expected,
                    actual);
        }
    }

    @Test
    public void testToLocalTimeWithZero() {
        AtomicInteger src = new AtomicInteger(0);
        LocalTime actual = AtomicIntegerConversions.toLocalTime(src, null);
        LocalTime expected = LongConversions.toLocalTime(0L, null);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTimeNegativeValue() {
        AtomicInteger src = new AtomicInteger(-1);
        LocalTime actual = AtomicIntegerConversions.toLocalTime(src, null);
        LocalTime expected = LongConversions.toLocalTime(-1L, null);
        Assert.assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTimeNullInputThrowsNPE() {
        AtomicIntegerConversions.toLocalTime(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTimeInvalidTypeThrowsClassCastException() {
        // Pass a type that is not AtomicInteger – the internal cast will throw
        AtomicIntegerConversions.toLocalTime(12345, null);
    }
}
