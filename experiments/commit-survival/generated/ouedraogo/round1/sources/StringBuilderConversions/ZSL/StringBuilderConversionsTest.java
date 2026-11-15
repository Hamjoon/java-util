package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringBuilderConversionsTest {

    /**
     * Verify that toString returns the result of the {@code toString()} method
     * of the supplied object, even when that object is a {@link StringBuilder}.
     */
    @Test
    public void testToString_WithStringBuilder_ReturnsCorrectString() {
        StringBuilder sb = new StringBuilder("Hello, World!");
        // Pass null for the Converter because the method does not use it.
        String result = StringBuilderConversions.toString(sb, null);
        assertEquals("Hello, World!", result);
    }

    /**
     * Verify that toString works with a user‑defined class that overrides {@code toString()}.
     */
    @Test
    public void testToString_WithCustomObject_ReturnsCustomString() {
        class Custom {
            @Override
            public String toString() {
                return "CustomString";
            }
        }

        Custom custom = new Custom();
        String result = StringBuilderConversions.toString(custom, null);
        assertEquals("CustomString", result);
    }

    /**
     * Verify that passing {@code null} as the source object causes a {@link NullPointerException}.
     * The method does not guard against a null source; it simply dereferences it.
     */
    @Test(expected = NullPointerException.class)
    public void testToString_WithNullSource_ThrowsNullPointerException() {
        StringBuilderConversions.toString(null, null);
    }

    /**
     * Verify that the {@code Converter} argument is ignored.
     * The method should produce the same output regardless of the Converter instance supplied.
     *
     * Because we cannot reliably construct a real {@code Converter} without its
     * {@code ConverterOptions} dependency (which is not part of the provided API),
     * we simply pass {@code null} and assert that the result matches the expected value.
     * This test documents the intended behaviour of the method.
     */
    @Test
    public void testToString_IgnoresConverterArgument() {
        StringBuilder sb = new StringBuilder("IgnoredConverter");
        // The method should not touch the converter argument, so null is fine.
        String resultWithNull = StringBuilderConversions.toString(sb, null);
        assertEquals("IgnoredConverter", resultWithNull);
    }
}
