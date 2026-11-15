package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link StringBufferConversions}.
 *
 * The class under test contains a single static conversion method:
 * <pre>
 * static String toString(Object from, Converter converter)
 * </pre>
 *
 * The method simply returns {@code from.toString()} and does not use the {@code Converter}
 * argument. The test suite therefore verifies:
 * <ul>
 *   <li>Normal operation with various object types.</li>
 *   <li>Behaviour when the {@code Converter} argument is {@code null}.</li>
 *   <li>Proper handling of a {@code null} source object (expected {@link NullPointerException}).</li>
 * </ul>
 */
public class StringBufferConversionsTest {

    /**
     * Helper method to create a simple {@link Converter} instance.
     * The {@code Converter} class requires a {@code ConverterOptions} argument,
     * but the {@code toString} method does not use the converter at all,
     * so we can safely pass {@code null}.
     */
    private static Converter createConverter() {
        return new Converter(null);
    }

    /** --------------------------------------------------------------------- *
     *  BASIC FUNCTIONALITY TESTS
     *  --------------------------------------------------------------------- */

    @Test
    public void testToString_WithString_ReturnsSameString() {
        String source = "Hello, World!";
        String result = StringBufferConversions.toString(source, createConverter());
        assertEquals("The method should return the original string's toString()", source, result);
    }

    @Test
    public void testToString_WithCustomObject_UsesCustomToString() {
        Object source = new Object() {
            @Override
            public String toString() {
                return "CustomStringRepresentation";
            }
        };
        String result = StringBufferConversions.toString(source, createConverter());
        assertEquals("The method should return the custom toString() value", "CustomStringRepresentation", result);
    }

    @Test
    public void testToString_WithStringBuilder_ReturnsBuilderString() {
        StringBuilder sb = new StringBuilder();
        sb.append("builder");
        sb.append("Test");
        String result = StringBufferConversions.toString(sb, createConverter());
        assertEquals("StringBuilder's toString() should be returned", sb.toString(), result);
    }

    @Test
    public void testToString_WithNullConverter_IgnoresConverter() {
        String source = "ignoreConverter";
        // Pass a null converter – the method should still work because it never touches the argument.
        String result = StringBufferConversions.toString(source, null);
        assertEquals("Method should work even when converter is null", source, result);
    }

    /** --------------------------------------------------------------------- *
     *  EDGE CASES & EXCEPTION HANDLING
     *  --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToString_NullSource_ThrowsNullPointerException() {
        // The implementation calls from.toString() directly, so a null source must cause NPE.
        StringBufferConversions.toString(null, createConverter());
    }

    @Test
    public void testToString_WithArrayObject_ReturnsArrayToString() {
        // An array's default toString() returns a type+hash string; we just verify that the method
        // forwards that exact value.
        Object[] array = new Object[] { "a", "b", "c" };
        String expected = array.toString(); // default Object.toString()
        String actual = StringBufferConversions.toString(array, createConverter());
        assertEquals("Array's toString() should be returned unchanged", expected, actual);
    }

    @Test
    public void testToString_WithPrimitiveWrapper_ReturnsWrapperToString() {
        Integer number = 42;
        String result = StringBufferConversions.toString(number, createConverter());
        assertEquals("Wrapper's toString() should be returned", "42", result);
    }

    @Test
    public void testToString_WithLargeString_ReturnsFullString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10_000; i++) {
            sb.append('x');
        }
        String largeString = sb.toString();
        String result = StringBufferConversions.toString(largeString, createConverter());
        assertEquals("Large strings should be returned unchanged", largeString, result);
    }
}
