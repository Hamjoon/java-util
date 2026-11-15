package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link ClassConversions}.
 *
 * This test class covers:
 * <ul>
 *   <li>Normal operation – converting a {@code Class} object to its fully‑qualified name.</li>
 *   <li>Edge cases such as {@code null} input, non‑{@code Class} objects, primitive and array classes.</li>
 *   <li>Verification that the {@code Converter} argument is ignored (it may be {@code null}).</li>
 * </ul>
 *
 * The tests are written using only the public API shown in the problem statement
 * and do not rely on any external mocking frameworks.
 */
public class ClassConversionsTest {

    /**
     * Basic happy‑path test: a regular class should return its fully‑qualified name.
     */
    @Test
    public void testToString_WithRegularClass() {
        String result = ClassConversions.toString(String.class, null);
        assertEquals("java.lang.String", result);
    }

    /**
     * Primitive type class – the name returned by {@code Class#getName()} is the keyword.
     */
    @Test
    public void testToString_WithPrimitiveClass() {
        String result = ClassConversions.toString(int.class, null);
        assertEquals("int", result);
    }

    /**
     * Array class – {@code Class#getName()} returns the internal JVM descriptor.
     */
    @Test
    public void testToString_WithArrayClass() {
        String result = ClassConversions.toString(String[].class, null);
        assertEquals("[Ljava.lang.String;", result);
    }

    /**
     * The {@code Converter} argument is not used by the method, therefore passing a real
     * instance (or {@code null}) must not affect the outcome.
     */
    @Test
    public void testToString_WithNonNullConverter() {
        // We can use the public API to obtain a dummy Converter instance.
        Converter dummy = new Converter(new ConverterOptions());
        String result = ClassConversions.toString(Double.class, dummy);
        assertEquals("java.lang.Double", result);
    }

    /**
     * Passing {@code null} as the {@code from} argument should result in a
     * {@link NullPointerException} because the method dereferences the casted object.
     */
    @Test(expected = NullPointerException.class)
    public void testToString_NullFrom() {
        ClassConversions.toString(null, null);
    }

    /**
     * Passing an object that is not a {@code Class} should cause a {@link ClassCastException}
     * when the method attempts to cast it.
     */
    @Test(expected = ClassCastException.class)
    public void testToString_NonClassObject() {
        Object notAClass = new Object();
        ClassConversions.toString(notAClass, null);
    }
}
