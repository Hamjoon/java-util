package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link ClassConversions}.
 *
 * The class under test contains only one public static method:
 *
 * <pre>
 * static String toString(Object from, Converter converter)
 * </pre>
 *
 * The method simply casts the {@code from} argument to {@code Class<?>} and returns
 * the fully‑qualified class name via {@link Class#getName()}.
 *
 * The test suite covers:
 * <ul>
 *   <li>Typical usage – passing a regular {@code Class} instance.</li>
 *   <li>Edge cases – {@code null} arguments, wrong type for {@code from}, primitive class objects.</li>
 *   <li>Exception handling – verifies that the method throws the expected {@link ClassCastException}
 *       when {@code from} is not a {@code Class}.</li>
 * </ul>
 *
 * No mocking is required because the method does not interact with the {@link Converter}
 * instance; a real {@link Converter} is constructed with default options.
 */
public class ClassConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Construct a Converter with default (empty) options – the actual
        // implementation details are irrelevant for ClassConversions.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Test the normal behaviour: a valid Class object is converted to its name.
     */
    @Test
    public void testToString_WithValidClass() {
        String result = ClassConversions.toString(String.class, converter);
        assertEquals("java.lang.String", result);
    }

    /**
     * Test that primitive class objects are handled correctly.
     * The name of a primitive class is its keyword (e.g., "int").
     */
    @Test
    public void testToString_WithPrimitiveClass() {
        String result = ClassConversions.toString(int.class, converter);
        assertEquals("int", result);
    }

    /**
     * Verify that passing {@code null} as the {@code from} argument
     * results in a {@link NullPointerException}. The method attempts
     * to cast the argument before any null‑check, therefore the NPE
     * originates from the cast operation.
     */
    @Test(expected = NullPointerException.class)
    public void testToString_NullFromArgument() {
        ClassConversions.toString(null, converter);
    }

    /**
     * Verify that passing {@code null} as the {@code converter} argument
     * does not affect the method because the converter is not used.
     * The method should still return the class name.
     */
    @Test
    public void testToString_NullConverterArgument() {
        String result = ClassConversions.toString(Double.class, null);
        assertEquals("java.lang.Double", result);
    }

    /**
     * Verify that providing an object that is not a {@code Class}
     * results in a {@link ClassCastException}. This is the contract
     * implied by the internal cast in the implementation.
     */
    @Test(expected = ClassCastException.class)
    public void testToString_FromNotAClass() {
        ClassConversions.toString("not a class", converter);
    }

    /**
     * Verify that an array class (e.g., String[]) is correctly converted
     * to its canonical name.
     */
    @Test
    public void testToString_WithArrayClass() {
        String result = ClassConversions.toString(String[].class, converter);
        assertEquals("[Ljava.lang.String;", result);
    }

    /**
     * Verify that an inner class is correctly handled.
     */
    @Test
    public void testToString_WithInnerClass() {
        class LocalInner {}
        String result = ClassConversions.toString(LocalInner.class, converter);
        // The name includes the enclosing class name separated by '$'
        assertTrue(result.contains("$"));
        assertTrue(result.endsWith("LocalInner"));
    }

    /**
     * Verify that a Class object representing a generic type (e.g., java.util.List)
     * returns the raw type name, as generic information is erased at runtime.
     */
    @Test
    public void testToString_WithGenericClass() {
        String result = ClassConversions.toString(java.util.List.class, converter);
        assertEquals("java.util.List", result);
    }
}
