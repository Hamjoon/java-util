package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link ClassConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal usage with various {@code Class} objects (regular, primitive, array, inner).</li>
 *   <li>Behaviour when {@code null} is supplied as the {@code from} argument.</li>
 *   <li>Behaviour when an object that is not a {@code Class} is supplied.</li>
 *   <li>Verification that the {@code Converter} argument is ignored (null is acceptable).</li>
 * </ul>
 */
public class ClassConversionsTest {

    /** Simple inner class used for the inner‑class test case. */
    static class DummyInner { }

    /**
     * Verify that a regular class returns its fully‑qualified name.
     */
    @Test
    public void testToString_WithRegularClass() {
        String result = ClassConversions.toString(String.class, null);
        assertEquals("java.lang.String", result);
    }

    /**
     * Verify that a primitive class returns the primitive name (e.g. {@code int}).
     */
    @Test
    public void testToString_WithPrimitiveClass() {
        String result = ClassConversions.toString(int.class, null);
        assertEquals("int", result);
    }

    /**
     * Verify that an array class returns the JVM internal name returned by {@code Class#getName()}.
     */
    @Test
    public void testToString_WithArrayClass() {
        String result = ClassConversions.toString(String[].class, null);
        // The name for a one‑dimensional String array is "[Ljava.lang.String;"
        assertEquals("[Ljava.lang.String;", result);
    }

    /**
     * Verify that an inner (static nested) class returns its fully‑qualified name,
     * which includes the enclosing class name separated by a dollar sign.
     */
    @Test
    public void testToString_WithInnerClass() {
        String result = ClassConversions.toString(DummyInner.class, null);
        assertEquals("com.cedarsoftware.util.convert.ClassConversionsTest$DummyInner", result);
    }

    /**
     * The method does not use the {@code Converter} argument, therefore passing {@code null}
     * must not affect the result.
     */
    @Test
    public void testToString_IgnoresConverterArgument() {
        String expected = Boolean.class.getName();
        String result = ClassConversions.toString(Boolean.class, null);
        assertEquals(expected, result);
    }

    /**
     * Passing {@code null} as the {@code from} argument should result in a {@link NullPointerException}
     * because the method attempts to call {@code getName()} on the casted reference.
     */
    @Test(expected = NullPointerException.class)
    public void testToString_NullFromArgument() {
        ClassConversions.toString(null, null);
    }

    /**
     * Passing an object that is not a {@code Class} should cause a {@link ClassCastException}
     * when the method attempts to cast it to {@code Class<?>}.
     */
    @Test(expected = ClassCastException.class)
    public void testToString_InvalidFromArgument() {
        Object notAClass = new Object();
        ClassConversions.toString(notAClass, null);
    }
}
