package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit 4 tests for {@code StringBuilderConversions}.
 */
public class StringBuilderConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the library; passing null is sufficient for these tests.
        dummyConverter = new Converter(null);
    }

    /* -------------------------------------------------
     * BASIC FUNCTIONALITY TESTS
     * ------------------------------------------------- */

    @Test
    public void testToStringWithString() {
        String input = "Hello World";
        String result = StringBuilderConversions.toString(input, dummyConverter);
        assertEquals("The method should return the original string", input, result);
    }

    @Test
    public void testToStringWithInteger() {
        Integer input = 42;
        String result = StringBuilderConversions.toString(input, dummyConverter);
        assertEquals("Integer.toString() should be returned", "42", result);
    }

    private static class CustomObj {
        private final String value;

        CustomObj(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    @Test
    public void testToStringWithCustomObject() {
        CustomObj input = new CustomObj("custom-value");
        String result = StringBuilderConversions.toString(input, dummyConverter);
        assertEquals("Custom object's toString() should be returned", "custom-value", result);
    }

    @Test
    public void testToStringWithNullConverter() {
        String input = "no-converter";
        String result = StringBuilderConversions.toString(input, null);
        assertEquals("Method must work even when converter is null", input, result);
    }

    /* -------------------------------------------------
     * EDGE CASE & EXCEPTION TESTS
     * ------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFromThrowsNPE() {
        // Expect a NullPointerException because `from` is null.
        StringBuilderConversions.toString(null, dummyConverter);
    }

    private static class NullToStringObj {
        @Override
        public String toString() {
            return null; // deliberately return null
        }
    }

    @Test
    public void testToStringWhenObjectReturnsNull() {
        NullToStringObj input = new NullToStringObj();
        String result = StringBuilderConversions.toString(input, dummyConverter);
        assertNull("If the object's toString() returns null, the method should return null", result);
    }

    @Test
    public void testToStringWithArray() {
        int[] array = {1, 2, 3};
        String result = StringBuilderConversions.toString(array, dummyConverter);
        // Default Object.toString() for int[] looks like "[I@<hash>"
        assertTrue("Result should start with [I@ for int[]", result.startsWith("[I@"));
    }

    @Test
    public void testToStringWithStringBuilder() {
        StringBuilder sb = new StringBuilder("builder-content");
        String result = StringBuilderConversions.toString(sb, dummyConverter);
        assertEquals("StringBuilder's toString() should be returned", "builder-content", result);
    }
}
