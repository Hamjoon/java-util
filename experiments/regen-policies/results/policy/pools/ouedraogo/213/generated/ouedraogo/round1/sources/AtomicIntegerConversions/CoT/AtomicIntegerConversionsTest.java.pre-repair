package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link AtomicIntegerConversions}.
 * The class under test is package‑private, therefore the test class resides in the same package.
 */
public class AtomicIntegerConversionsTest {

    /**
     * Extracted public (exposed) methods of {@code AtomicIntegerConversions}.
     * <p>
     * The class only contains one static method:
     * <pre>
     * static AtomicInteger toAtomicInteger(Object from, Converter converter)
     * </pre>
     * The method is package‑private, but can be accessed from tests placed in the same package.
     */
    
    // ------------------------------------------------------------------------
    // Basic functionality tests
    // ------------------------------------------------------------------------

    /** 
     * Verify that a valid {@link AtomicInteger} instance is correctly converted
     * to a new {@link AtomicInteger} with the same int value.
     */
    @Test
    public void testToAtomicInteger_NormalCase() {
        AtomicInteger original = new AtomicInteger(42);
        // Converter is not used inside the method; passing null is acceptable.
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, null);
        assertNotNull("Result must not be null", result);
        assertEquals("Converted value must match original", original.get(), result.get());

        // Ensure the returned instance is a different object (deep copy semantics)
        assertNotSame("Result should be a new instance", original, result);

        // Changing the original must not affect the result
        original.set(100);
        assertEquals("Result must remain unchanged after original is mutated", 42, result.get());
    }

    // ------------------------------------------------------------------------
    // Edge‑case and exception handling tests
    // ------------------------------------------------------------------------

    /**
     * Passing {@code null} as the source object should cause a {@link NullPointerException}
     * because the method dereferences the casted value.
     */
    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullSource() {
        AtomicIntegerConversions.toAtomicInteger(null, null);
    }

    /**
     * Passing an object that is not an {@link AtomicInteger} must trigger a
     * {@link ClassCastException} at the cast operation.
     */
    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_InvalidSourceType() {
        String notAnAtomic = "I am not an AtomicInteger";
        AtomicIntegerConversions.toAtomicInteger(notAnAtomic, null);
    }

    /**
     * The {@code Converter} argument is not used by the implementation.
     * Supplying a non‑null instance (even a dummy one) must not affect the outcome.
     */
    @Test
    public void testToAtomicInteger_WithConverterInstance() {
        AtomicInteger source = new AtomicInteger(-7);
        // Create a minimal Converter instance; the concrete class requires a ConverterOptions.
        // Since ConverterOptions is not part of the supplied code, we use the no‑arg constructor
        // if available, otherwise we pass null (the Converter constructor accepts a ConverterOptions).
        Converter converter = new Converter(null);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, converter);
        assertEquals("Value must be transferred correctly even when a Converter is supplied",
                     source.get(), result.get());
    }

    /**
     * Verify that the method returns a fresh {@link AtomicInteger} even when the
     * source holds the minimum and maximum int values.
     */
    @Test
    public void testToAtomicInteger_BoundaryValues() {
        AtomicInteger min = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger max = new AtomicInteger(Integer.MAX_VALUE);

        AtomicInteger minResult = AtomicIntegerConversions.toAtomicInteger(min, null);
        AtomicInteger maxResult = AtomicIntegerConversions.toAtomicInteger(max, null);

        assertEquals(Integer.MIN_VALUE, minResult.get());
        assertEquals(Integer.MAX_VALUE, maxResult.get());

        // Ensure independence
        assertNotSame(min, minResult);
        assertNotSame(max, maxResult);
    }
}
