package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Unit tests for {@link StringBufferConversions}.
 *
 * The class under test only contains a single static method {@code toString(Object, Converter)}.
 * The method simply delegates to {@code Object.toString()} and does not use the {@link Converter}
 * argument.  The tests therefore verify:
 *
 * <ul>
 *   <li>Correct string conversion for typical objects.</li>
 *   <li>Behavior when the source object is {@code null} (expected {@link NullPointerException}).</li>
 *   <li>That the supplied {@link Converter} instance is ignored.</li>
 *   <li>That the utility class cannot be instantiated via normal means (private constructor).</li>
 * </ul>
 */
public class StringBufferConversionsTest {

    /** Helper class with a custom {@code toString()} implementation. */
    private static final class CustomObject {
        private final String value;

        CustomObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Custom:" + value;
        }
    }

    /**
     * Verify that a plain {@link String} is returned unchanged.
     */
    @Test
    public void testToStringWithString() {
        String input = "hello world";
        Converter converter = new Converter(new ConverterOptions());

        String result = StringBufferConversions.toString(input, converter);
        assertEquals("The toString method should return the original string.", input, result);
    }

    /**
     * Verify that an object with an overridden {@code toString()} yields the overridden value.
     */
    @Test
    public void testToStringWithCustomObject() {
        CustomObject obj = new CustomObject("test");
        Converter converter = new Converter(new ConverterOptions());

        String result = StringBufferConversions.toString(obj, converter);
        assertEquals("Custom:test", result);
    }

    /**
     * Verify that the {@link Converter} argument does not influence the result.
     * Two different Converter instances are passed and the same output is expected.
     */
    @Test
    public void testConverterArgumentIsIgnored() {
        String input = "ignored";

        Converter converterA = new Converter(new ConverterOptions());
        Converter converterB = new Converter(new ConverterOptions());

        String resultA = StringBufferConversions.toString(input, converterA);
        String resultB = StringBufferConversions.toString(input, converterB);

        assertEquals("Result should be identical regardless of Converter instance.", resultA, resultB);
        assertEquals("ignored", resultA);
    }

    /**
     * The method does not guard against {@code null} inputs; a {@link NullPointerException}
     * is expected when {@code from} is {@code null}.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullObject() {
        Converter converter = new Converter(new ConverterOptions());
        // This call must throw NPE because the implementation calls from.toString()
        StringBufferConversions.toString(null, converter);
    }

    /**
     * Ensure that the utility class cannot be instantiated via its private constructor.
     * Attempting to invoke the constructor without making it accessible should result
     * in an {@link IllegalAccessException}.
     */
    @Test
    public void testPrivateConstructorIsInaccessible() {
        try {
            Constructor<StringBufferConversions> ctor = StringBufferConversions.class.getDeclaredConstructor();
            // Do NOT set accessible; the constructor should be inaccessible.
            ctor.newInstance();
            fail("Expected IllegalAccessException when invoking a private constructor without setAccessible(true).");
        } catch (NoSuchMethodException e) {
            fail("Private no‑arg constructor should exist: " + e);
        } catch (InvocationTargetException e) {
            // The underlying exception should be IllegalAccessException wrapped in InvocationTargetException
            Throwable cause = e.getCause();
            assertTrue("Expected IllegalAccessException wrapped in InvocationTargetException.",
                    cause instanceof IllegalAccessException);
        } catch (InstantiationException | IllegalAccessException e) {
            // These are also acceptable outcomes depending on JVM behavior.
            assertTrue("Expected IllegalAccessException or InstantiationException.", 
                    e instanceof IllegalAccessException || e instanceof InstantiationException);
        }
    }

    /**
     * As a secondary check, verify that the private constructor *can* be invoked when
     * accessibility is explicitly overridden. This ensures the constructor exists
     * and does not perform unexpected logic.
     */
    @Test
    public void testPrivateConstructorCanBeInvokedViaReflectionWhenAccessible() throws Exception {
        Constructor<StringBufferConversions> ctor = StringBufferConversions.class.getDeclaredConstructor();
        assertTrue("Constructor should be private.", (ctor.getModifiers() & java.lang.reflect.Modifier.PRIVATE) != 0);
        ctor.setAccessible(true);
        Object instance = ctor.newInstance();
        assertNotNull("Instance created via reflection should not be null.", instance);
        assertTrue("Instance should be of type StringBufferConversions.", instance instanceof StringBufferConversions);
    }
}
