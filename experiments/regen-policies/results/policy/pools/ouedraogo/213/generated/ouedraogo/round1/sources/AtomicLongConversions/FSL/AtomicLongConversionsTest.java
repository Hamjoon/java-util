package com.cedarsoftware.util.convert;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

public class AtomicLongConversionsTest {

    // Helper method to create a dummy Converter (the implementation is irrelevant for the tests)
    private static Converter dummyConverter() {
        // The Converter constructor expects a ConverterOptions instance; passing null is acceptable for our tests
        return new Converter(null);
    }

    @Test
    public void testToAtomicLongCreatesNewInstanceWithSameValue() {
        AtomicLong original = new AtomicLong(12345L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, dummyConverter());

        // The value must be identical
        assertEquals(original.get(), result.get());
        // The returned instance must be a different object (new instance)
        assertNotSame("Result should be a new AtomicLong, not the original reference", original, result);
    }

    @Test
    public void testToAtomicLongIndependenceAfterOriginalChanges() {
        AtomicLong original = new AtomicLong(0L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, dummyConverter());

        // Change the original after conversion
        original.set(999L);

        // The result must retain the value captured at conversion time
        assertEquals("Result should retain the original value at conversion time", 0L, result.get());
        // The original now has a different value
        assertEquals("Original should reflect the new value after mutation", 999L, original.get());
    }

    @Test
    public void testToAtomicLongHandlesZeroAndNegativeValues() {
        AtomicLong zero = new AtomicLong(0L);
        AtomicLong negative = new AtomicLong(-42L);
        AtomicLong max = new AtomicLong(Long.MAX_VALUE);
        AtomicLong min = new AtomicLong(Long.MIN_VALUE);

        assertEquals(0L, AtomicLongConversions.toAtomicLong(zero, dummyConverter()).get());
        assertEquals(-42L, AtomicLongConversions.toAtomicLong(negative, dummyConverter()).get());
        assertEquals(Long.MAX_VALUE, AtomicLongConversions.toAtomicLong(max, dummyConverter()).get());
        assertEquals(Long.MIN_VALUE, AtomicLongConversions.toAtomicLong(min, dummyConverter()).get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullInputThrowsNPE() {
        // Passing null should cause a NullPointerException when the method tries to dereference it
        AtomicLongConversions.toAtomicLong(null, dummyConverter());
    }

    @Test
    public void testToAtomicLongInvalidTypeThrowsClassCastException() {
        Object notAnAtomicLong = "I am not an AtomicLong";
        try {
            AtomicLongConversions.toAtomicLong(notAnAtomicLong, dummyConverter());
            fail("Expected ClassCastException when passing an object that is not an AtomicLong");
        } catch (ClassCastException e) {
            // Expected exception – test passes
        }
    }
}
