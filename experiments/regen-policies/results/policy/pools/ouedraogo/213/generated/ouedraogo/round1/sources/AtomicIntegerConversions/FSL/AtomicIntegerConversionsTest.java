package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Test;

/**
 * Unit tests for {@link AtomicIntegerConversions}.
 *
 * These tests cover:
 * <ul>
 *   <li>Successful conversion of a valid {@link AtomicInteger}.</li>
 *   <li>Independence of the returned instance from the source instance.</li>
 *   <li>Behaviour when {@code null} is supplied as the source object.</li>
 *   <li>Behaviour when an object of an incompatible type is supplied.</li>
 *   <li>Verification that the utility class cannot be instantiated.</li>
 * </ul>
 */
public class AtomicIntegerConversionsTest {

    // Helper to obtain a minimal Converter instance – the implementation under test does not use it.
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    @Test
    public void testToAtomicIntegerCopiesValue() {
        AtomicInteger source = new AtomicInteger(123);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, DUMMY_CONVERTER);

        // The result must contain the same numeric value.
        assertEquals("Converted value should match source value", source.get(), result.get());

        // The result must be a distinct object (a copy, not the original reference).
        assertNotSame("Result should be a new AtomicInteger instance", source, result);
    }

    @Test
    public void testResultIsIndependentFromSource() {
        AtomicInteger source = new AtomicInteger(5);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, DUMMY_CONVERTER);

        // Change the source after conversion.
        source.set(99);

        // The previously created result must retain the original value.
        assertEquals("Result should remain unchanged after source is mutated", 5, result.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerWithNullSourceThrowsNPE() {
        // The implementation casts the argument to AtomicInteger and then dereferences it,
        // which results in a NullPointerException for a null argument.
        AtomicIntegerConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerWithInvalidTypeThrowsClassCastException() {
        // Passing an object that is not an AtomicInteger should cause a ClassCastException.
        AtomicIntegerConversions.toAtomicInteger("not an atomic integer", DUMMY_CONVERTER);
    }

    @Test
    public void testUtilityClassHasPrivateConstructor() throws Exception {
        Constructor<AtomicIntegerConversions> ctor = AtomicIntegerConversions.class.getDeclaredConstructor();
        assertTrue("Constructor must be private", Modifier.isPrivate(ctor.getModifiers()));

        // Ensure that attempting to instantiate the class via reflection fails with an IllegalAccessException
        // unless we make it accessible. This confirms the class is intended to be non‑instantiable.
        try {
            ctor.setAccessible(true);
            ctor.newInstance();
            fail("Instantiating utility class should not be allowed");
        } catch (Exception e) {
            // The exact exception type may vary (InvocationTargetException wrapping IllegalAccessException),
            // but any exception indicates that the class is not meant to be instantiated.
            assertTrue(e.getCause() instanceof IllegalAccessException ||
                       e instanceof IllegalAccessException);
        }
    }
}
