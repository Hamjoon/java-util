package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ClassConversions}.
 *
 * The tests cover:
 *   1. Normal behaviour with regular classes.
 *   2. Behaviour with primitive and array class objects.
 *   3. Edge‑case handling when {@code from} or {@code converter} is {@code null}.
 *   4. Exception scenarios when {@code from} is not a {@code Class} instance.
 */
public class ClassConversionsTest {

    /* --------------------------------------------------------------------
     *  List of public methods in ClassConversions (extracted from source)
     * --------------------------------------------------------------------
     *
     * static String toString(Object from, Converter converter)
     *
     * --------------------------------------------------------------------
     */

    /**
     * Basic test – a regular class (String) is passed as {@code from}.
     * The method should return the fully‑qualified class name.
     */
    @Test
    public void testToStringWithRegularClass() {
        String result = ClassConversions.toString(String.class, null);
        assertEquals("java.lang.String", result);
    }

    /**
     * Test with a primitive class (int).  Primitive class objects are also
     * instances of {@code Class}, and {@code getName()} returns the primitive name.
     */
    @Test
    public void testToStringWithPrimitiveClass() {
        String result = ClassConversions.toString(int.class, null);
        assertEquals("int", result);
    }

    /**
     * Test with an array class (String[]).  The name returned follows the JVM
     * internal array notation (e.g. "[Ljava.lang.String;").
     */
    @Test
    public void testToStringWithArrayClass() {
        String result = ClassConversions.toString(String[].class, null);
        assertEquals("[Ljava.lang.String;", result);
    }

    /**
     * Edge case – {@code converter} is {@code null}.  The method does not use
     * the converter, so the call should succeed and return the class name.
     */
    @Test
    public void testToStringWithNullConverter() {
        String result = ClassConversions.toString(Double.class, null);
        assertEquals("java.lang.Double", result);
    }

    /**
     * Edge case – {@code from} is {@code null}.  The cast succeeds (producing
     * {@code null}), but calling {@code getName()} on a {@code null} reference
     * throws a {@link NullPointerException}.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFrom() {
        ClassConversions.toString(null, null);
    }

    /**
     * Edge case – {@code from} is not a {@code Class} instance (e.g. a String).
     * The explicit cast to {@code Class<?>} will throw a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testToStringWithNonClassObject() {
        ClassConversions.toString("I am not a Class object", null);
    }

    /**
     * Additional sanity check – using a real {@link Converter} instance.
     * The method does not depend on the converter, so any instance (including
     * one built with {@code null} options) is acceptable.
     */
    @Test
    public void testToStringWithRealConverterInstance() {
        // Assuming ConverterOptions is a public class in the same package.
        // Passing null is allowed by the constructor signature.
        Converter converter = new Converter(null);
        String result = ClassConversions.toString(Boolean.class, converter);
        assertEquals("java.lang.Boolean", result);
    }
}
