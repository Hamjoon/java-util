package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link AtomicIntegerConversions}.
 *
 * The class under test is package‑private with a private constructor and a single static method.
 * The tests cover normal operation, error handling, independence of the returned instance,
 * and the private constructor visibility.
 */
public class AtomicIntegerConversionsTest {

    /**
     * Verify that a normal {@link AtomicInteger} is converted correctly.
     * The returned instance must contain the same int value but must be a distinct object.
     */
    @Test
    public void testToAtomicIntegerCopiesValueAndIsNewInstance() {
        AtomicInteger original = new AtomicInteger(42);
        // The method does not use the converter, so we can safely pass null.
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, null);

        // Value must be the same
        Assert.assertEquals("Converted value should match original", original.intValue(), result.intValue());

        // Must be a different instance
        Assert.assertNotSame("Converted AtomicInteger should be a new instance", original, result);
    }

    /**
     * Ensure that the conversion result is independent from the source.
     * Changing the source after conversion must not affect the returned object.
     */
    @Test
    public void testOriginalAndResultIndependence() {
        AtomicInteger source = new AtomicInteger(100);
        AtomicInteger copy = AtomicIntegerConversions.toAtomicInteger(source, null);

        // Mutate the source
        source.set(200);

        // The copy must retain the original value
        Assert.assertEquals("Copy must retain the value captured at conversion time", 100, copy.intValue());
        // The source now holds the new value
        Assert.assertEquals("Source should reflect the new value after mutation", 200, source.intValue());
    }

    /**
     * Passing {@code null} as the source should result in a {@link NullPointerException}
     * because the method attempts to cast the argument to {@link AtomicInteger}.
     */
    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerWithNullFromThrowsNPE() {
        AtomicIntegerConversions.toAtomicInteger(null, null);
    }

    /**
     * Passing an object that is not an {@link AtomicInteger} must cause a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerWithInvalidTypeThrowsClassCastException() {
        Integer notAtomic = Integer.valueOf(5);
        AtomicIntegerConversions.toAtomicInteger(notAtomic, null);
    }

    /**
     * The {@code Converter} argument is ignored by the implementation.
     * This test simply verifies that a non‑null converter does not interfere with the conversion.
     */
    @Test
    public void testToAtomicIntegerIgnoresConverterArgument() {
        AtomicInteger source = new AtomicInteger(-7);
        // Use a real Converter instance (options can be default‑constructed if needed)
        Converter converter = new Converter(new ConverterOptions());

        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, converter);
        Assert.assertEquals("Result should have the same value as source", source.intValue(), result.intValue());
    }

    /**
     * The class has a private no‑arg constructor. Using reflection we verify that:
     * <ul>
     *   <li>the constructor is indeed private</li>
     *   <li>it can be made accessible and invoked without throwing an exception</li>
     * </ul>
     * This provides coverage for the private constructor branch.
     */
    @Test
    public void testPrivateConstructorIsAccessibleViaReflection() throws Exception {
        Constructor<AtomicIntegerConversions> ctor = AtomicIntegerConversions.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor should be private", (ctor.getModifiers() & java.lang.reflect.Modifier.PRIVATE) != 0);

        // Make it accessible and instantiate
        ctor.setAccessible(true);
        AtomicIntegerConversions instance = ctor.newInstance();

        // The instance is not used; the mere fact that we could instantiate it is enough.
        Assert.assertNotNull("Instance created via reflection should not be null", instance);
    }
}
