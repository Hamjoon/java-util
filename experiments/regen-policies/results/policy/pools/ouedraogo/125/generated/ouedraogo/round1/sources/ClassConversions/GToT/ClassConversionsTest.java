package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@code ClassConversions}.
 *
 * This suite covers:
 * <ul>
 *   <li>Typical usage – converting a {@code Class} object to its fully‑qualified name.</li>
 *   <li>Edge cases such as primitive types, array types, and inner classes.</li>
 *   <li>Exception scenarios – {@code null} arguments and arguments of the wrong type.</li>
 *   <li>Behaviour when the {@code Converter} argument is {@code null} (the method does not use it).</li>
 * </ul>
 */
public class ClassConversionsTest {

    /**
     * Basic happy‑path test: a regular reference type.
     */
    @Test
    public void testToStringWithRegularClass() {
        String result = ClassConversions.toString(String.class, null);
        assertEquals("java.lang.String", result);
    }

    /**
     * Verify that primitive class objects are handled correctly.
     */
    @Test
    public void testToStringWithPrimitiveClass() {
        String result = ClassConversions.toString(int.class, null);
        assertEquals("int", result);
    }

    /**
     * Verify that array class objects return the binary name as defined by {@link Class#getName()}.
     */
    @Test
    public void testToStringWithArrayClass() {
        String result = ClassConversions.toString(String[].class, null);
        assertEquals("[Ljava.lang.String;", result);
    }

    /**
     * Verify that an inner (member) class is converted to its binary name.
     */
    @Test
    public void testToStringWithInnerClass() {
        class InnerClass {}
        String result = ClassConversions.toString(InnerClass.class, null);
        // The binary name includes the enclosing class name separated by '$'
        assertTrue(result.contains("$InnerClass"));
    }

    /**
     * The method does not use the {@code Converter} argument, but it must accept {@code null}
     * without throwing an exception.
     */
    @Test
    public void testToStringWithNullConverter() {
        String result = ClassConversions.toString(Double.class, null);
        assertEquals("java.lang.Double", result);
    }

    /**
     * Passing {@code null} as the {@code from} argument should result in a {@code NullPointerException}
     * because the method attempts to cast and invoke {@code getName()} on it.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFrom() {
        ClassConversions.toString(null, null);
    }

    /**
     * Passing an object that is not an instance of {@code Class} should cause a {@code ClassCastException}
     * due to the explicit cast inside the method.
     */
    @Test(expected = ClassCastException.class)
    public void testToStringWithInvalidFromType() {
        ClassConversions.toString("I am not a Class object", null);
    }
}
