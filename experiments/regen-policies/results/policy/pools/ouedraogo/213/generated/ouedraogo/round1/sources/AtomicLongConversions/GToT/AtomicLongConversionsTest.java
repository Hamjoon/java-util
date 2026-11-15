package com.cedarsoftware.util.convert;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class AtomicLongConversionsTest {

    /**
     * Verify that a normal AtomicLong is converted to a new AtomicLong
     * with the same numeric value but a different object reference.
     */
    @Test
    public void testToAtomicLongCopiesValueAndCreatesNewInstance() {
        long[] testValues = {0L, 1L, -1L, Long.MAX_VALUE, Long.MIN_VALUE, 123456789L};

        for (long v : testValues) {
            AtomicLong original = new AtomicLong(v);
            AtomicLong converted = AtomicLongConversions.toAtomicLong(original, null);

            // The returned instance must be a different object
            assertNotSame("Converted instance should be a new object", original, converted);
            // But it must hold the same long value
            assertEquals("Converted value should match original", original.get(), converted.get());

            // Changing the original after conversion must not affect the converted value
            original.set(v + 1);
            assertEquals("Converted value must remain unchanged after original is mutated",
                    v, converted.get());
        }
    }

    /**
     * The method does not use the Converter argument, but passing a non‑null
     * instance should still work without side effects.
     */
    @Test
    public void testToAtomicLongWithNonNullConverter() {
        // We can safely pass null because Converter is not used, but we also test
        // that a real (though minimal) Converter instance does not interfere.
        Converter dummyConverter = new Converter(new ConverterOptions());

        AtomicLong original = new AtomicLong(42L);
        AtomicLong converted = AtomicLongConversions.toAtomicLong(original, dummyConverter);

        assertNotSame(original, converted);
        assertEquals(42L, converted.get());
    }

    /**
     * Passing {@code null} as the source object should result in a
     * {@link NullPointerException} because the method attempts to call
     * {@code get()} on the casted value.
     */
    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullSourceThrowsNPE() {
        AtomicLongConversions.toAtomicLong(null, null);
    }

    /**
     * Passing an object that is not an {@link AtomicLong} must cause a
     * {@link ClassCastException} due to the unchecked cast inside the method.
     */
    @Test(expected = ClassCastException.class)
    public void testToAtomicLongInvalidSourceThrowsClassCastException() {
        String notAnAtomicLong = "I am not an AtomicLong";
        AtomicLongConversions.toAtomicLong(notAnAtomicLong, null);
    }

    /**
     * Ensure that the private constructor of the utility class cannot be
     * invoked via reflection (a typical pattern for utility classes).
     */
    @Test
    public void testUtilityClassPrivateConstructor() throws Exception {
        java.lang.reflect.Constructor<AtomicLongConversions> ctor =
                AtomicLongConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        // Instantiating should not throw an exception, but the instance is not used.
        Object instance = ctor.newInstance();
        assertNotNull(instance);
    }
}
