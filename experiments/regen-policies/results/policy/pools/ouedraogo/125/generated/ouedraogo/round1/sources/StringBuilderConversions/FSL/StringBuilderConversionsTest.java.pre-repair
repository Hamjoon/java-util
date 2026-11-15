package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.Test;

/**
 * Unit tests for {@link StringBuilderConversions}.
 */
public class StringBuilderConversionsTest {

    /**
     * Verify that {@code toString(Object, Converter)} returns the result of {@code Object.toString()}.
     */
    @Test
    public void testToStringReturnsObjectToString() {
        // StringBuilder implements toString()
        StringBuilder sb = new StringBuilder("builder");
        String result = StringBuilderConversions.toString(sb, null);
        assertEquals("builder", result);

        // Integer overrides toString()
        Integer number = 42;
        result = StringBuilderConversions.toString(number, null);
        assertEquals("42", result);

        // Custom class with its own toString()
        class Custom {
            @Override
            public String toString() {
                return "custom-value";
            }
        }
        Custom custom = new Custom();
        result = StringBuilderConversions.toString(custom, null);
        assertEquals("custom-value", result);
    }

    /**
     * The method does not use the {@code Converter} argument.
     * Passing a non‑null instance must not affect the outcome.
     */
    @Test
    public void testToStringIgnoresConverterArgument() {
        // The Converter class is final and has no public no‑arg constructor.
        // We therefore pass {@code null} which is allowed because the method never dereferences it.
        String input = "ignore-converter";
        String result = StringBuilderConversions.toString(input, null);
        assertEquals("ignore-converter", result);
    }

    /**
     * Passing {@code null} as the {@code from} argument should throw a {@link NullPointerException},
     * because the implementation dereferences it to call {@code toString()}.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFromThrowsException() {
        StringBuilderConversions.toString(null, null);
    }

    /**
     * The utility class has a private constructor to prevent instantiation.
     * This test uses reflection to ensure the constructor exists, is private,
     * and can be invoked without throwing an unexpected exception (coverage purpose).
     */
    @Test
    public void testPrivateConstructorCoverage() throws Exception {
        Constructor<StringBuilderConversions> ctor = StringBuilderConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", Modifier.isPrivate(ctor.getModifiers()));
        ctor.setAccessible(true);
        // Instantiating the class should succeed (even though it's not intended for use)
        StringBuilderConversions instance = ctor.newInstance();
        assertNotNull("Instance created via reflection should not be null", instance);
    }
}
