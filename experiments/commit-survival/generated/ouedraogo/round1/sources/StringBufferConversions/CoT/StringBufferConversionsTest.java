package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test suite for {@link StringBufferConversions}.
 *
 * The class under test contains a single static method:
 * <pre>
 * static String toString(Object from, Converter converter)
 * </pre>
 *
 * The method simply returns {@code from.toString()}.  The {@code converter}
 * argument is ignored, but it must be supplied because it is part of the
 * public API.  The test cases therefore focus on:
 * <ul>
 *   <li>Normal operation with a regular object.</li>
 *   <li>Operation with an object that overrides {@code toString()}.</li>
 *   <li>Behaviour when {@code from} is {@code null} (expected NPE).</li>
 *   <li>Behaviour when {@code converter} is {@code null} (should still work).</li>
 * </ul>
 *
 * The tests use only the public API of {@link Converter} as supplied in the
 * problem statement – a real instance is created via its constructor.
 */
public class StringBufferConversionsTest {

    // ----------------------------------------------------------------------
    // Helper objects used by several test cases
    // ----------------------------------------------------------------------
    /** Simple POJO with default {@code toString()} implementation. */
    private static class SimplePojo {
        // No fields – default Object.toString() will be used.
    }

    /** POJO that overrides {@code toString()}. */
    private static class OverriddenToString {
        private final String value;

        OverriddenToString(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Overridden:" + value;
        }
    }

    /** Minimal {@code Converter} instance – the implementation details are irrelevant
     *  for the tests because {@code StringBufferConversions.toString} does not use it. */
    private static Converter createDummyConverter() {
        // The constructor requires a ConverterOptions object; we can pass null
        // because the test does not invoke any method on the Converter.
        return new Converter(null);
    }

    // ----------------------------------------------------------------------
    // 1. Basic functionality tests
    // ----------------------------------------------------------------------

    /**
     * Verify that {@code toString} returns the result of {@code Object.toString()}
     * for a plain object that does not override the method.
     */
    @Test
    public void testToString_WithSimplePojo_ReturnsObjectToString() {
        SimplePojo pojo = new SimplePojo();
        Converter converter = createDummyConverter();

        String result = StringBufferConversions.toString(pojo, converter);

        // The default Object.toString() format is "<className>@<hashcode>"
        assertNotNull("Result must not be null", result);
        assertTrue("Result should contain the class name",
                result.startsWith(SimplePojo.class.getName() + "@"));
    }

    /**
     * Verify that {@code toString} returns the overridden string when the
     * source object provides its own {@code toString()} implementation.
     */
    @Test
    public void testToString_WithOverriddenToString_ReturnsCustomString() {
        OverriddenToString obj = new OverriddenToString("testValue");
        Converter converter = createDummyConverter();

        String result = StringBufferConversions.toString(obj, converter);

        assertEquals("Overridden:testValue", result);
    }

    /**
     * Verify that passing a {@code null} converter does not affect the outcome.
     * The method should still delegate to {@code from.toString()}.
     */
    @Test
    public void testToString_WithNullConverter_UsesObjectToString() {
        OverriddenToString obj = new OverriddenToString("nullConverter");
        // Converter is intentionally null
        String result = StringBufferConversions.toString(obj, null);

        assertEquals("Overridden:nullConverter", result);
    }

    // ----------------------------------------------------------------------
    // 2. Edge‑case / exception tests
    // ----------------------------------------------------------------------

    /**
     * The method does not perform a null‑check on the {@code from} argument,
     * therefore a {@code NullPointerException} is expected when it is null.
     */
    @Test(expected = NullPointerException.class)
    public void testToString_WithNullFrom_ThrowsNullPointerException() {
        Converter converter = createDummyConverter();

        // This call must throw NPE because 'from' is null and we attempt
        // to invoke 'null.toString()'.
        StringBufferConversions.toString(null, converter);
    }

    /**
     * Although the {@code converter} argument is ignored, passing a {@code null}
     * reference should not cause any exception (already covered by a normal test,
     * but we keep a dedicated test for clarity).
     */
    @Test
    public void testToString_WithNullFromAndNullConverter_ThrowsNPE() {
        try {
            StringBufferConversions.toString(null, null);
            fail("Expected NullPointerException because 'from' is null");
        } catch (NullPointerException expected) {
            // Expected path
        }
    }
}
