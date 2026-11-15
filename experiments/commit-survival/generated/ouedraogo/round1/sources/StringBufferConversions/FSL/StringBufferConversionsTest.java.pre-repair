package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

/**
 * Unit tests for {@link StringBufferConversions}.
 */
public class StringBufferConversionsTest {

    /**
     * Simple object whose {@code toString()} returns a known value.
     */
    private static final class SimpleObject {
        @Override
        public String toString() {
            return "simple";
        }
    }

    /**
     * Object with a {@code toString()} that deliberately returns {@code null}
     * (although this is a bad practice, the method under test should simply
     * return whatever the delegate returns).
     */
    private static final class NullToStringObject {
        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * Verify that a regular {@link String} is returned unchanged.
     */
    @Test
    public void testToString_WithString() {
        String input = "hello world";
        // converter argument is unused – we can safely pass null
        String result = StringBufferConversions.toString(input, null);
        assertEquals("The method should return the original string", input, result);
    }

    /**
     * Verify that an {@link Integer}'s {@code toString()} representation is returned.
     */
    @Test
    public void testToString_WithInteger() {
        Integer input = 12345;
        String result = StringBufferConversions.toString(input, null);
        assertEquals("Integer toString() should be used", "12345", result);
    }

    /**
     * Verify that a custom object's {@code toString()} is respected.
     */
    @Test
    public void testToString_WithCustomObject() {
        SimpleObject input = new SimpleObject();
        String result = StringBufferConversions.toString(input, null);
        assertEquals("Custom object's toString() should be returned", "simple", result);
    }

    /**
     * Verify that a {@code null} result from {@code toString()} is propagated.
     */
    @Test
    public void testToString_WithObjectReturningNull() {
        NullToStringObject input = new NullToStringObject();
        String result = StringBufferConversions.toString(input, null);
        assertNull("Method should return null when object's toString() returns null", result);
    }

    /**
     * The method does not guard against a {@code null} source object; a
     * {@link NullPointerException} is expected.
     */
    @Test(expected = NullPointerException.class)
    public void testToString_NullSourceThrowsNPE() {
        StringBufferConversions.toString(null, null);
    }

    /**
     * The class has a private constructor. Using reflection we can verify that
     * it exists and is indeed private, and that it can be invoked when accessibility
     * is overridden (useful for code‑coverage tools).
     */
    @Test
    public void testPrivateConstructor() throws Exception {
        Constructor<StringBufferConversions> ctor = StringBufferConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private", java.lang.reflect.Modifier.isPrivate(ctor.getModifiers()));

        // Make it accessible and instantiate – should succeed without side‑effects
        ctor.setAccessible(true);
        StringBufferConversions instance = ctor.newInstance();
        assertNotNull("Instance created via reflection should not be null", instance);
    }

    /**
     * Demonstrate that the {@code Converter} argument is truly ignored.
     * We create a real {@link Converter} instance (with default options) and
     * pass it; the result must be identical to passing {@code null}.
     */
    @Test
    public void testToString_IgnoresConverterArgument() {
        String input = "ignore-me";

        // Create a Converter with default options – the class is final, so we cannot
        // subclass or mock it. The constructor requires a ConverterOptions instance.
        // Assuming a no‑arg constructor exists for ConverterOptions (common in the library).
        ConverterOptions options = new ConverterOptions();
        Converter converter = new Converter(options);

        String resultWithNull = StringBufferConversions.toString(input, null);
        String resultWithConverter = StringBufferConversions.toString(input, converter);

        assertEquals("Result should be identical regardless of the Converter argument", resultWithNull, resultWithConverter);
    }
}
