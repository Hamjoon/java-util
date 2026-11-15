package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringBuilderConversionsTest {

    /**
     * List of public methods in {@link StringBuilderConversions}
     *
     * <pre>
     * static String toString(Object from, Converter converter)
     * </pre>
     */

    /**
     * Basic functionality test – the method should return the result of {@code from.toString()}.
     */
    @Test
    public void testToStringReturnsObjectToString() {
        String source = "Hello, World!";
        // The converter argument is ignored – we can safely pass null.
        String result = StringBuilderConversions.toString(source, null);
        assertEquals("Hello, World!", result);
    }

    /**
     * Verify that the {@code Converter} argument is truly ignored.
     */
    @Test
    public void testToStringIgnoresConverter() {
        String source = "ignore-me";

        // Build a real Converter instance (the concrete implementation is not used by the method).
        ConverterOptions options = new ConverterOptions(); // assumes a public no‑arg ctor
        Converter converter = new Converter(options);

        String result = StringBuilderConversions.toString(source, converter);
        assertEquals("ignore-me", result);
    }

    /**
     * Test with a non‑String object – e.g., an {@link Integer}.
     */
    @Test
    public void testToStringWithInteger() {
        Integer source = 42;
        String result = StringBuilderConversions.toString(source, null);
        assertEquals("42", result);
    }

    /**
     * Test with a {@link StringBuilder} instance.
     */
    @Test
    public void testToStringWithStringBuilder() {
        StringBuilder source = new StringBuilder("builder");
        String result = StringBuilderConversions.toString(source, null);
        assertEquals("builder", result);
    }

    /**
     * Test with a custom object that overrides {@code toString()} to return {@code null}.
     * The method should propagate the {@code null} result.
     */
    @Test
    public void testToStringCustomObjectReturnsNull() {
        class NullToString {
            @Override
            public String toString() {
                return null;
            }
        }

        NullToString source = new NullToString();
        String result = StringBuilderConversions.toString(source, null);
        assertNull(result);
    }

    /**
     * Edge‑case: passing {@code null} as the source object should raise a {@link NullPointerException}
     * because the implementation calls {@code from.toString()} without a null check.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullSourceThrowsNPE() {
        StringBuilderConversions.toString(null, null);
    }

    /**
     * Edge‑case: passing {@code null} as the {@code Converter} argument should **not** throw an exception.
     * The method does not dereference the converter.
     */
    @Test
    public void testToStringWithNullConverterDoesNotThrow() {
        String source = "no-converter";
        // No exception expected – just verify the return value.
        String result = StringBuilderConversions.toString(source, null);
        assertEquals("no-converter", result);
    }
}
