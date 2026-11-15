package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link StringBufferConversions}.
 *
 * The class under test contains a single static method:
 * <pre>
 *   static String toString(Object from, Converter converter)
 * </pre>
 * which simply delegates to {@code from.toString()}.  The tests verify
 * correct behavior for a variety of input objects, handling of {@code null},
 * and the fact that the {@code Converter} argument is ignored.
 */
public class StringBufferConversionsTest {

    /** A regular {@link Converter} instance – the implementation is irrelevant for the tests. */
    private final Converter converter = new Converter(null);

    // ----------------------------------------------------------------------
    // Simple positive cases
    // ----------------------------------------------------------------------

    @Test
    public void toString_WithInteger_ReturnsIntegerString() {
        Integer value = 42;
        String result = StringBufferConversions.toString(value, converter);
        assertEquals("42", result);
    }

    @Test
    public void toString_WithString_ReturnsSameString() {
        String value = "Hello, World!";
        String result = StringBufferConversions.toString(value, converter);
        assertEquals(value, result);
    }

    @Test
    public void toString_WithStringBuilder_ReturnsBuilderContents() {
        StringBuilder sb = new StringBuilder("builder");
        String result = StringBufferConversions.toString(sb, converter);
        assertEquals("builder", result);
    }

    @Test
    public void toString_WithStringBuffer_ReturnsBufferContents() {
        StringBuffer sbuf = new StringBuffer("buffer");
        String result = StringBufferConversions.toString(sbuf, converter);
        assertEquals("buffer", result);
    }

    @Test
    public void toString_WithCustomObject_ReturnsCustomToString() {
        Dummy dummy = new Dummy();
        String result = StringBufferConversions.toString(dummy, converter);
        assertEquals("DummyObject", result);
    }

    @Test
    public void toString_WithObjectReturningNullToString_ReturnsNull() {
        NullToString nts = new NullToString();
        String result = StringBufferConversions.toString(nts, converter);
        assertNull(result);
    }

    // ----------------------------------------------------------------------
    // Null handling
    // ----------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void toString_WithNullObject_ThrowsNPE() {
        // The method calls from.toString() without a null‑check, so NPE is expected.
        StringBufferConversions.toString(null, converter);
    }

    // ----------------------------------------------------------------------
    // Converter argument is ignored – we verify that passing null works.
    // ----------------------------------------------------------------------

    @Test
    public void toString_WithNullConverter_IgnoresConverter() {
        String value = "NoConverterNeeded";
        // Pass a null converter; method should still work because it never uses it.
        String result = StringBufferConversions.toString(value, null);
        assertEquals(value, result);
    }

    // ----------------------------------------------------------------------
    // Helper classes used in the tests
    // ----------------------------------------------------------------------

    /** Simple class with a deterministic {@code toString()} implementation. */
    private static class Dummy {
        @Override
        public String toString() {
            return "DummyObject";
        }
    }

    /** Class whose {@code toString()} deliberately returns {@code null}. */
    private static class NullToString {
        @Override
        public String toString() {
            return null;
        }
    }
}
