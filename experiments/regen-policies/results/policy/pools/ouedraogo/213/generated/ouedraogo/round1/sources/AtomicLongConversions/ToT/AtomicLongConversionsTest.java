package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link AtomicLongConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage with positive, zero, negative and {@code Long.MAX_VALUE} values.</li>
 *   <li>Ensuring the returned {@code AtomicLong} is a new instance (no shared mutable state).</li>
 *   <li>Behaviour when the source {@code AtomicLong} is modified after conversion.</li>
 *   <li>Null input handling (expected {@link NullPointerException}).</li>
 *   <li>Invalid source type handling (expected {@link ClassCastException}).</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore a {@code null}
 * reference is sufficient for the tests.
 */
public class AtomicLongConversionsTest {

    /** Helper constant – a dummy {@code Converter} instance (null is acceptable). */
    private static final Converter DUMMY_CONVERTER = null;

    @Test
    public void testToAtomicLongCopiesPositiveValue() {
        AtomicLong source = new AtomicLong(123L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, DUMMY_CONVERTER);

        // Verify a new instance is created
        Assert.assertNotSame("Result should be a new instance", source, result);
        // Verify the value is correctly copied
        Assert.assertEquals("Value should be copied unchanged", source.get(), result.get());
    }

    @Test
    public void testToAtomicLongCopiesNegativeValue() {
        AtomicLong source = new AtomicLong(-456L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, DUMMY_CONVERTER);
        Assert.assertNotSame(source, result);
        Assert.assertEquals(source.get(), result.get());
    }

    @Test
    public void testToAtomicLongCopiesZero() {
        AtomicLong source = new AtomicLong(0L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, DUMMY_CONVERTER);
        Assert.assertNotSame(source, result);
        Assert.assertEquals(0L, result.get());
    }

    @Test
    public void testToAtomicLongCopiesLongMaxValue() {
        AtomicLong source = new AtomicLong(Long.MAX_VALUE);
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, DUMMY_CONVERTER);
        Assert.assertNotSame(source, result);
        Assert.assertEquals(Long.MAX_VALUE, result.get());
    }

    @Test
    public void testResultIsIndependentFromSourceAfterConversion() {
        AtomicLong source = new AtomicLong(99L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(source, DUMMY_CONVERTER);

        // Change the source after conversion
        source.set(100L);

        // Ensure the result stays unchanged
        Assert.assertEquals("Result should retain original value after source mutation",
                99L, result.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongWithNullInputThrowsNPE() {
        // The implementation casts the argument to AtomicLong and then calls get(),
        // which will throw NPE when the argument itself is null.
        AtomicLongConversions.toAtomicLong(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongWithInvalidTypeThrowsClassCastException() {
        // Pass an object that is not an AtomicLong – the cast should fail.
        AtomicLongConversions.toAtomicLong("not an AtomicLong", DUMMY_CONVERTER);
    }
}
