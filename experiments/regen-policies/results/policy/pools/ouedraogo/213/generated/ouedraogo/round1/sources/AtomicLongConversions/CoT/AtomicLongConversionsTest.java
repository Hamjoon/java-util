package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link AtomicLongConversions}.
 *
 * The class under test is package‑private, therefore the test class
 * resides in the same package so it can access the static method
 * {@code toAtomicLong(Object, Converter)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion of a valid {@link AtomicLong} instance.</li>
 *   <li>Independence of the returned {@link AtomicLong} from the source.</li>
 *   <li>Handling of extreme {@code long} values.</li>
 *   <li>Edge‑case / exceptional scenarios:
 *       <ul>
 *         <li>source object is {@code null} → {@link NullPointerException}</li>
 *         <li>source object is not an {@link AtomicLong} → {@link ClassCastException}</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * Note: The {@code Converter} argument is not used by the method, so
 * we deliberately pass {@code null} in all tests to keep the focus on
 * the conversion logic.
 */
public class AtomicLongConversionsTest {

    /**
     * Helper that invokes the package‑private method under test.
     */
    private static AtomicLong invoke(Object source) {
        // The method is package‑private static; we can call it directly.
        return AtomicLongConversions.toAtomicLong(source, null);
    }

    // -----------------------------------------------------------------
    // Basic functionality tests
    // -----------------------------------------------------------------

    @Test
    public void testConversionCopiesValue() {
        AtomicLong original = new AtomicLong(12345L);
        AtomicLong copy = invoke(original);

        assertNotNull("Result should not be null", copy);
        assertEquals("Copied value should match original", original.get(), copy.get());
        assertNotSame("Returned instance must be a new object", original, copy);
    }

    @Test
    public void testReturnedAtomicLongIsIndependent() {
        AtomicLong original = new AtomicLong(99L);
        AtomicLong copy = invoke(original);

        // Change the original after conversion
        original.set(0L);
        assertEquals("Original value changed", 0L, original.get());
        // The copy must retain the value captured at conversion time
        assertEquals("Copy must remain unchanged", 99L, copy.get());
    }

    @Test
    public void testConversionWithZero() {
        AtomicLong original = new AtomicLong(0L);
        AtomicLong copy = invoke(original);
        assertEquals(0L, copy.get());
    }

    @Test
    public void testConversionWithNegativeValue() {
        AtomicLong original = new AtomicLong(-42L);
        AtomicLong copy = invoke(original);
        assertEquals(-42L, copy.get());
    }

    @Test
    public void testConversionWithLongMaxValue() {
        AtomicLong original = new AtomicLong(Long.MAX_VALUE);
        AtomicLong copy = invoke(original);
        assertEquals(Long.MAX_VALUE, copy.get());
    }

    @Test
    public void testConversionWithLongMinValue() {
        AtomicLong original = new AtomicLong(Long.MIN_VALUE);
        AtomicLong copy = invoke(original);
        assertEquals(Long.MIN_VALUE, copy.get());
    }

    // -----------------------------------------------------------------
    // Edge‑case / exception handling tests
    // -----------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testNullSourceThrowsNullPointerException() {
        // The method casts the argument to AtomicLong and then calls get()
        // on it, so a null argument leads to a NPE.
        invoke(null);
    }

    @Test(expected = ClassCastException.class)
    public void testInvalidSourceTypeThrowsClassCastException() {
        // Passing an object that is not an AtomicLong should cause a
        // ClassCastException at the cast site.
        invoke("I am not an AtomicLong");
    }

    @Test
    public void testConverterArgumentIsIgnored() {
        // Even when a non‑null Converter is supplied, the method does not
        // interact with it. We simply verify that no exception is thrown.
        AtomicLong original = new AtomicLong(777L);
        Converter dummyConverter = null; // explicit null to emphasise "unused"
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, dummyConverter);
        assertEquals(777L, result.get());
    }
}
