package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * JUnit 4 test suite for {@link AtomicIntegerConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage – converting a valid {@code AtomicInteger} and verifying the value is copied.</li>
 *   <li>Edge values – zero, negative numbers and {@code Integer.MAX_VALUE}.</li>
 *   <li>Error handling – {@code null} source and source of an incompatible type.</li>
 *   <li>Verification that the {@code Converter} argument is ignored.</li>
 * </ul>
 */
public class AtomicIntegerConversionsTest {

    /**
     * Helper that creates a dummy {@code Converter} instance.
     * The actual {@code Converter} implementation is not needed for these tests
     * because {@code AtomicIntegerConversions.toAtomicInteger} does not use it.
     */
    private static Converter dummyConverter() {
        // The constructor of Converter requires a ConverterOptions instance.
        // Passing {@code null} is acceptable for the purpose of these tests
        // because the method under test never dereferences the object.
        return new Converter(null);
    }

    @Test
    public void testToAtomicIntegerCopiesValueAndCreatesNewInstance() {
        AtomicInteger original = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, dummyConverter());

        // The value must be identical
        assertEquals("Converted AtomicInteger should have the same int value",
                original.intValue(), result.intValue());

        // The returned instance must be a new object, not the same reference
        assertNotSame("Converted AtomicInteger should be a new instance",
                original, result);
    }

    @Test
    public void testToAtomicIntegerWithEdgeValues() {
        // Zero
        AtomicInteger zero = new AtomicInteger(0);
        AtomicInteger zeroResult = AtomicIntegerConversions.toAtomicInteger(zero, dummyConverter());
        assertEquals(0, zeroResult.intValue());

        // Negative value
        AtomicInteger negative = new AtomicInteger(-12345);
        AtomicInteger negativeResult = AtomicIntegerConversions.toAtomicInteger(negative, dummyConverter());
        assertEquals(-12345, negativeResult.intValue());

        // Integer.MAX_VALUE
        AtomicInteger max = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger maxResult = AtomicIntegerConversions.toAtomicInteger(max, dummyConverter());
        assertEquals(Integer.MAX_VALUE, maxResult.intValue());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNullFromThrowsNPE() {
        // Passing null as the source should cause a NullPointerException
        // when the method attempts to call intValue() on the casted object.
        AtomicIntegerConversions.toAtomicInteger(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerInvalidTypeThrowsClassCastException() {
        // An object that is not an AtomicInteger should trigger a ClassCastException
        // during the explicit cast inside the method.
        Integer notAtomic = Integer.valueOf(10);
        AtomicIntegerConversions.toAtomicInteger(notAtomic, dummyConverter());
    }

    @Test
    public void testToAtomicIntegerIgnoresConverterParameter() {
        AtomicInteger source = new AtomicInteger(99);

        // Pass a null converter – this should be fine because the method never dereferences it.
        AtomicInteger resultWithNullConverter = AtomicIntegerConversions.toAtomicInteger(source, null);
        assertEquals(99, resultWithNullConverter.intValue());

        // Pass a "real" converter instance – the behaviour must be identical.
        Converter realConverter = dummyConverter();
        AtomicInteger resultWithRealConverter = AtomicIntegerConversions.toAtomicInteger(source, realConverter);
        assertEquals(99, resultWithRealConverter.intValue());

        // Ensure both results are equal and distinct from the source.
        assertEquals(resultWithNullConverter.intValue(), resultWithRealConverter.intValue());
        assertNotSame(source, resultWithNullConverter);
        assertNotSame(source, resultWithRealConverter);
    }
}
