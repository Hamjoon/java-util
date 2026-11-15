package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link AtomicIntegerConversions}.
 *
 * The tests cover:
 *   • Normal conversion (value copy)
 *   • Independence of the returned {@link AtomicInteger}
 *   • Extreme integer values (MAX_VALUE, MIN_VALUE)
 *   • Null source argument (NPE)
 *   • Wrong source type (ClassCastException)
 *
 * The {@code Converter} argument is not used by the implementation, therefore {@code null}
 * is passed in all test invocations.
 */
public class AtomicIntegerConversionsTest {

    /** Basic happy‑path test – value is copied correctly. */
    @Test
    public void testToAtomicIntegerCopiesValue() {
        AtomicInteger source = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, null);
        assertEquals("The converted value must match the source value", 42, result.intValue());
    }

    /** The returned instance must be a new object, not the original one. */
    @Test
    public void testReturnedAtomicIntegerIsIndependent() {
        AtomicInteger source = new AtomicInteger(10);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, null);
        assertNotSame("Result must be a different instance", source, result);
        assertEquals(10, result.intValue());

        // Changing the source after conversion must not affect the result.
        source.set(20);
        assertEquals("Result must retain the original value after source mutation",
                     10, result.intValue());
    }

    /** Conversion of the maximum int value. */
    @Test
    public void testToAtomicIntegerWithMaxValue() {
        AtomicInteger source = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, null);
        assertEquals(Integer.MAX_VALUE, result.intValue());
    }

    /** Conversion of the minimum int value. */
    @Test
    public void testToAtomicIntegerWithMinValue() {
        AtomicInteger source = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, null);
        assertEquals(Integer.MIN_VALUE, result.intValue());
    }

    /** Passing {@code null} as the source must throw NPE. */
    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNullFromThrowsNPE() {
        AtomicIntegerConversions.toAtomicInteger(null, null);
    }

    /** Passing an object that is not an AtomicInteger must throw ClassCastException. */
    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerInvalidTypeThrowsClassCastException() {
        AtomicIntegerConversions.toAtomicInteger("I am not atomic", null);
    }
}
