package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link StringBuilderConversions}.
 *
 * The class under test provides a single static {@code toString} method that simply
 * delegates to {@code Object.toString()} and does not use the {@link Converter} argument.
 * The tests cover normal operation, edge‑cases (null input, custom {@code toString}
 * implementations, and the fact that the {@code Converter} argument is ignored.
 */
public class StringBuilderConversionsTest {

    /** Simple sanity check – the method should return the same value as {@code Object.toString()}. */
    @Test
    public void testToStringWithPlainObject() {
        Object obj = new Object();
        String expected = obj.toString();
        String actual = StringBuilderConversions.toString(obj, null);
        assertEquals("Result should match Object.toString()", expected, actual);
    }

    /** When the source is already a {@link String}, {@code toString} should return that instance. */
    @Test
    public void testToStringWithString() {
        String source = "sample text";
        String result = StringBuilderConversions.toString(source, null);
        // The implementation returns the exact same String instance for java.lang.String
        assertSame("String instance should be returned unchanged", source, result);
    }

    /** Verify that a custom {@code toString} implementation is honoured. */
    @Test
    public void testToStringWithCustomObject() {
        class Custom {
            @Override
            public String toString() {
                return "custom-value";
            }
        }
        Custom custom = new Custom();
        String result = StringBuilderConversions.toString(custom, null);
        assertEquals("custom-value", result);
    }

    /** The method must propagate a {@code NullPointerException} when {@code from} is {@code null}. */
    @Test(expected = NullPointerException.class)
    public void testToStringWithNullFromThrowsNPE() {
        StringBuilderConversions.toString(null, null);
    }

    /** The {@link Converter} argument is not used – passing a real (or null) instance must not affect the outcome. */
    @Test
    public void testToStringIgnoresConverterArgument() {
        class Dummy {
            @Override
            public String toString() {
                return "dummy";
            }
        }
        Dummy dummy = new Dummy();

        // Pass a null Converter – should work.
        String resultWithNull = StringBuilderConversions.toString(dummy, null);
        assertEquals("dummy", resultWithNull);

        // Pass a Converter instance (constructed with default options if possible).
        // Since the implementation does not use the converter, we can safely pass null.
        // If a real Converter were needed, it would be created via new Converter(options).
        // Here we simply reuse null to demonstrate the point.
        String resultWithConverter = StringBuilderConversions.toString(dummy, null);
        assertEquals("dummy", resultWithConverter);
    }

    /** If an object's {@code toString} returns {@code null}, the method should return {@code null}. */
    @Test
    public void testToStringWhenObjectToStringReturnsNull() {
        class ReturnsNull {
            @Override
            public String toString() {
                return null;
            }
        }
        ReturnsNull obj = new ReturnsNull();
        String result = StringBuilderConversions.toString(obj, null);
        assertNull("Result should be null when object's toString returns null", result);
    }
}
