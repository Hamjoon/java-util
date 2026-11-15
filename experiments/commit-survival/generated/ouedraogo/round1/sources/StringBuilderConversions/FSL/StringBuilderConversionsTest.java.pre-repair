package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link StringBuilderConversions}.
 */
public class StringBuilderConversionsTest {

    /**
     * Helper class with a custom {@code toString()} implementation.
     */
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
     * Verify that {@code toString(Object, Converter)} returns the result of {@code Object.toString()}
     * regardless of the supplied {@link Converter} instance.
     */
    @Test
    public void testToStringReturnsObjectToString() {
        // Arrange
        Converter dummyConverter = new Converter(null); // Converter is not used by the method

        // Simple built‑in types
        assertEquals("123", StringBuilderConversions.toString(Integer.valueOf(123), dummyConverter));
        assertEquals("true", StringBuilderConversions.toString(Boolean.TRUE, dummyConverter));

        // StringBuilder (its toString() returns the contained characters)
        StringBuilder sb = new StringBuilder("builder");
        assertEquals("builder", StringBuilderConversions.toString(sb, dummyConverter));

        // Custom object with overridden toString()
        CustomObject custom = new CustomObject("xyz");
        assertEquals("Custom:xyz", StringBuilderConversions.toString(custom, dummyConverter));

        // Empty string
        assertEquals("", StringBuilderConversions.toString("", dummyConverter));
    }

    /**
     * Ensure that the method does not depend on the state of the {@link Converter}
     * (i.e., changing the converter does not change the output).
     */
    @Test
    public void testConverterParameterIsIgnored() {
        // Arrange two different converters (they could be configured differently)
        Converter converterA = new Converter(null);
        Converter converterB = new Converter(null);

        String input = "ignoreMe";

        // Act & Assert – result must be identical for both converters
        String resultA = StringBuilderConversions.toString(input, converterA);
        String resultB = StringBuilderConversions.toString(input, converterB);
        assertEquals("ignoreMe", resultA);
        assertEquals(resultA, resultB);
    }

    /**
     * Verify that a {@code null} source object causes a {@link NullPointerException},
     * because the implementation directly calls {@code from.toString()}.
     */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullSourceThrowsNPE() {
        // Arrange
        Converter converter = new Converter(null);

        // Act – should throw NPE
        StringBuilderConversions.toString(null, converter);
    }
}
