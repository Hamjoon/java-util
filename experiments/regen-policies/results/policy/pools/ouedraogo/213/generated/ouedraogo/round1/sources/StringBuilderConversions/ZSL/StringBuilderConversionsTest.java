package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Unit tests for {@link StringBuilderConversions}.
 * <p>
 * The class under test is a small utility with a private constructor and a single static method.
 * The tests verify:
 * <ul>
 *   <li>Correct delegation of {@code toString} to the supplied object's {@code toString()} method.</li>
 *   <li>Behaviour when {@code null} is supplied (expected {@link NullPointerException}).</li>
 *   <li>The {@code Converter} argument is ignored.</li>
 *   <li>The private constructor cannot be invoked without reflection, and can be instantiated via reflection.</li>
 * </ul>
 */
public class StringBuilderConversionsTest {

    /**
     * Helper method to obtain a usable {@link Converter} instance.
     * The concrete {@code ConverterOptions} class is part of the library; we assume it has a public no‑arg constructor.
     */
    private static Converter createConverter() {
        // Using the public constructor that accepts a ConverterOptions instance.
        // An empty options object is sufficient because the conversion logic in StringBuilderConversions does not use it.
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testToString_WithStringBuilder_ReturnsContents() {
        StringBuilder sb = new StringBuilder("hello world");
        Converter converter = createConverter();

        String result = StringBuilderConversions.toString(sb, converter);

        Assert.assertEquals("The toString result should match the StringBuilder contents", "hello world", result);
    }

    @Test
    public void testToString_WithInteger_ReturnsNumberString() {
        Integer number = 42;
        Converter converter = createConverter();

        String result = StringBuilderConversions.toString(number, converter);

        Assert.assertEquals("Integer toString should be its decimal representation", "42", result);
    }

    @Test
    public void testToString_WithCustomObject_UsesCustomToString() {
        class Custom {
            @Override
            public String toString() {
                return "custom-output";
            }
        }

        Custom custom = new Custom();
        Converter converter = createConverter();

        String result = StringBuilderConversions.toString(custom, converter);

        Assert.assertEquals("Custom object's toString should be used", "custom-output", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_WithNull_ThrowsNullPointerException() {
        Converter converter = createConverter();

        // This call must throw NullPointerException because the implementation invokes from.toString()
        StringBuilderConversions.toString(null, converter);
    }

    @Test
    public void testToString_IgnoresConverterArgument() {
        // The converter is not used by the method, but we can verify that passing a different instance
        // does not affect the outcome.
        StringBuilder sb = new StringBuilder("ignore-me");
        Converter converter1 = createConverter();
        Converter converter2 = new Converter(new ConverterOptions()); // a distinct instance

        String result1 = StringBuilderConversions.toString(sb, converter1);
        String result2 = StringBuilderConversions.toString(sb, converter2);

        Assert.assertEquals("Result should be identical regardless of which Converter is supplied", result1, result2);
    }

    @Test
    public void testPrivateConstructor_IsNotAccessible() throws NoSuchMethodException {
        Constructor<StringBuilderConversions> ctor = StringBuilderConversions.class.getDeclaredConstructor();
        Assert.assertTrue("Constructor should be private", (ctor.getModifiers() & java.lang.reflect.Modifier.PRIVATE) != 0);
    }

    @Test
    public void testPrivateConstructor_ReflectionCanInstantiate() throws Exception {
        Constructor<StringBuilderConversions> ctor = StringBuilderConversions.class.getDeclaredConstructor();
        // By default the constructor is not accessible; attempting to invoke it should throw an IllegalAccessException.
        try {
            ctor.newInstance();
            Assert.fail("Expected IllegalAccessException when invoking private constructor without setAccessible(true)");
        } catch (IllegalAccessException e) {
            // Expected path
        }

        // Make the constructor accessible and instantiate the class.
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();

        Assert.assertNotNull("Instance created via reflection should not be null", instance);
        Assert.assertEquals("Instance should be of type StringBuilderConversions",
                StringBuilderConversions.class, instance.getClass());
    }

    @Test
    public void testPrivateConstructor_OnlyOneInstancePossibleViaReflection() throws Exception {
        Constructor<StringBuilderConversions> ctor = StringBuilderConversions.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        StringBuilderConversions instance1 = ctor.newInstance();
        StringBuilderConversions instance2 = ctor.newInstance();

        // The class has no state; the two instances should be distinct objects but behave identically.
        Assert.assertNotSame("Two reflection‑created instances should be distinct objects", instance1, instance2);
    }
}
