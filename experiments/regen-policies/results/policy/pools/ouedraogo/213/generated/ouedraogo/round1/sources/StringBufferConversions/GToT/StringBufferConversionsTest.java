package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringBufferConversions}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>Normal usage with typical objects (String, Integer, custom objects).</li>
 *   <li>Edge cases such as {@code null} arguments, objects whose {@code toString()}
 *       returns {@code null}, and objects whose {@code toString()} throws an exception.</li>
 *   <li>Verification that the {@code Converter} argument is ignored (both non‑null and {@code null}
 *       values are accepted without affecting the result).</li>
 * </ul>
 *
 * The class under test is package‑private, so the test resides in the same package.
 */
public class StringBufferConversionsTest {

    /** Helper class that overrides {@code toString()} with a predictable value. */
    private static final class CustomObject {
        @Override
        public String toString() {
            return "CustomObjectString";
        }
    }

    /** Helper class whose {@code toString()} deliberately returns {@code null}. */
    private static final class NullStringObject {
        @Override
        public String toString() {
            return null;
        }
    }

    /** Helper class whose {@code toString()} throws a runtime exception. */
    private static final class ThrowingToStringObject {
        @Override
        public String toString() {
            throw new RuntimeException("boom");
        }
    }

    /**
     * Creates a minimal {@link Converter} instance required for the method signature.
     * The actual conversion logic is irrelevant because {@code StringBufferConversions.toString}
     * does not use the {@code Converter} argument.
     */
    private static Converter createConverter() {
        // ConverterOptions has a public no‑arg constructor in the library.
        return new Converter(new ConverterOptions());
    }

    // -----------------------------------------------------------------------
    // Basic functionality tests
    // -----------------------------------------------------------------------

    @Test
    public void testToString_WithString_ReturnsSameString() {
        String input = "Hello, World!";
        String result = StringBufferConversions.toString(input, createConverter());
        assertEquals("The method should return the original string unchanged.", input, result);
    }

    @Test
    public void testToString_WithInteger_ReturnsIntegerString() {
        Integer input = 42;
        String result = StringBufferConversions.toString(input, createConverter());
        assertEquals("Integer.toString() should be used.", "42", result);
    }

    @Test
    public void testToString_WithCustomObject_ReturnsCustomString() {
        CustomObject input = new CustomObject();
        String result = StringBufferConversions.toString(input, createConverter());
        assertEquals("Custom object's toString() value should be returned.", "CustomObjectString", result);
    }

    // -----------------------------------------------------------------------
    // Edge‑case and exception handling tests
    // -----------------------------------------------------------------------

    @Test
    public void testToString_WithNullObject_ThrowsNullPointerException() {
        try {
            StringBufferConversions.toString(null, createConverter());
            fail("Expected NullPointerException when 'from' argument is null.");
        } catch (NullPointerException expected) {
            // Expected outcome
        }
    }

    @Test
    public void testToString_WithNullConverter_DoesNotThrow() {
        String input = "NoConverter";
        // The method does not use the converter, so passing null should be safe.
        String result = StringBufferConversions.toString(input, null);
        assertEquals("Result should be identical to input even when converter is null.", input, result);
    }

    @Test
    public void testToString_ObjectToStringReturnsNull_ReturnsNull() {
        NullStringObject input = new NullStringObject();
        String result = StringBufferConversions.toString(input, createConverter());
        assertNull("If the object's toString() returns null, the method should return null.", result);
    }

    @Test
    public void testToString_ObjectToStringThrowsException_PropagatesException() {
        ThrowingToStringObject input = new ThrowingToStringObject();
        try {
            StringBufferConversions.toString(input, createConverter());
            fail("Expected RuntimeException to propagate from object's toString().");
        } catch (RuntimeException e) {
            assertEquals("Exception message should be preserved.", "boom", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Additional sanity checks
    // -----------------------------------------------------------------------

    @Test
    public void testToString_WithStringBuilder_ReturnsBuilderString() {
        StringBuilder sb = new StringBuilder("BuilderContent");
        String result = StringBufferConversions.toString(sb, createConverter());
        assertEquals("StringBuilder's toString() should be used.", "BuilderContent", result);
    }

    @Test
    public void testToString_RepeatedCalls_ReturnConsistentResults() {
        CustomObject obj = new CustomObject();
        String first = StringBufferConversions.toString(obj, createConverter());
        String second = StringBufferConversions.toString(obj, createConverter());
        assertEquals("Repeated calls should yield the same result.", first, second);
    }
}
