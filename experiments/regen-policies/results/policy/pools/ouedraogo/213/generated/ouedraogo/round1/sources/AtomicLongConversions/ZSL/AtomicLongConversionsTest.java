package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link AtomicLongConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion of a valid {@link AtomicLong} instance.</li>
 *   <li>Boundary values (Long.MAX_VALUE and Long.MIN_VALUE).</li>
 *   <li>Verification that a new {@link AtomicLong} instance is created (no reference sharing).</li>
 *   <li>Behaviour when {@code null} is supplied as the source object.</li>
 *   <li>Behaviour when an object of an incompatible type is supplied.</li>
 *   <li>That the {@link Converter} argument is not used by the conversion logic.</li>
 * </ul>
 *
 * The test class resides in the same package as {@code AtomicLongConversions} so it can
 * access the package‑private static method directly.
 */
public class AtomicLongConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter instance is not used by AtomicLongConversions, but we need a non‑null instance.
        // ConverterOptions is part of the public API and can be instantiated with its default constructor.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Verify that a normal {@link AtomicLong} is converted to a new {@link AtomicLong}
     * with the same numeric value.
     */
    @Test
    public void testToAtomicLongCreatesNewInstanceWithSameValue() {
        AtomicLong original = new AtomicLong(12345L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);

        assertNotNull("Result must not be null", result);
        assertEquals("Converted value must match original", original.get(), result.get());
        assertNotSame("Result must be a different instance", original, result);
    }

    /**
     * Verify conversion of the extreme long values.
     */
    @Test
    public void testToAtomicLongWithBoundaryValues() {
        AtomicLong max = new AtomicLong(Long.MAX_VALUE);
        AtomicLong min = new AtomicLong(Long.MIN_VALUE);

        AtomicLong maxResult = AtomicLongConversions.toAtomicLong(max, converter);
        AtomicLong minResult = AtomicLongConversions.toAtomicLong(min, converter);

        assertEquals(Long.MAX_VALUE, maxResult.get());
        assertEquals(Long.MIN_VALUE, minResult.get());
        assertNotSame(max, maxResult);
        assertNotSame(min, minResult);
    }

    /**
     * Ensure that modifications to the original {@link AtomicLong} after conversion
     * do not affect the converted instance (and vice‑versa).
     */
    @Test
    public void testToAtomicLongIndependenceAfterConversion() {
        AtomicLong original = new AtomicLong(500L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, converter);

        // Change original
        original.set(999L);
        assertEquals("Result must retain original value after source is mutated", 500L, result.get());

        // Change result
        result.set(777L);
        assertEquals("Original must retain its value after result is mutated", 999L, original.get());
    }

    /**
     * Passing {@code null} as the source object should result in a {@link NullPointerException}
     * because the method dereferences the casted value.
     */
    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullInputThrowsNPE() {
        AtomicLongConversions.toAtomicLong(null, converter);
    }

    /**
     * Passing an object of an incompatible type (e.g., {@link String}) should cause a
     * {@link ClassCastException} during the cast to {@link AtomicLong}.
     */
    @Test(expected = ClassCastException.class)
    public void testToAtomicLongInvalidTypeThrowsClassCastException() {
        String notAnAtomicLong = "I am not an AtomicLong";
        AtomicLongConversions.toAtomicLong(notAnAtomicLong, converter);
    }

    /**
     * The {@link Converter} argument is not consulted by the conversion logic.
     * This test simply ensures that providing a different (but still valid) Converter
     * does not change the outcome.
     */
    @Test
    public void testToAtomicLongIgnoresConverterParameter() {
        AtomicLong source = new AtomicLong(42L);

        // Use a different Converter instance (different options) to prove it has no effect.
        Converter anotherConverter = new Converter(new ConverterOptions());

        AtomicLong result = AtomicLongConversions.toAtomicLong(source, anotherConverter);
        assertEquals(42L, result.get());
    }
}
