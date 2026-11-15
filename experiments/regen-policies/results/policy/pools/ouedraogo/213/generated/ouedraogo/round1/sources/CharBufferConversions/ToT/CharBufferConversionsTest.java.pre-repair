package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
import static com.cedarsoftware.util.convert.MapConversions.VALUE;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CharBufferConversions}.
 *
 * <p>The tests are written as if three independent experts had each
 * contributed one test case per method.  After discussion the final
 * suite consolidates all scenarios – normal usage, edge cases and
 * expected failure modes – into a single, maintainable class.</p>
 */
public class CharBufferConversionsTest {

    private Converter converter;
    private Charset charset;

    @Before
    public void setUp() {
        // The real library provides a default‑constructor for ConverterOptions
        // that uses the platform default charset.  We rely on that behaviour
        // because the conversion methods only need a Charset instance.
        converter = new Converter(new ConverterOptions());
        charset = converter.getOptions().getCharset();
    }

    /* -------------------------------------------------------------
     *  toCharBuffer
     * ------------------------------------------------------------- */
    @Test
    public void testToCharBuffer_ReturnsReadOnlyBuffer() {
        CharBuffer original = CharBuffer.wrap("test");
        CharBuffer result = CharBufferConversions.toCharBuffer(original, converter);

        // Verify that the returned buffer is a read‑only view of the original.
        assertTrue("Result should be read‑only", result.isReadOnly());

        // The content must be identical.
        assertEquals("Content must match", original.toString(), result.toString());

        // Modifying the returned buffer must throw ReadOnlyBufferException.
        try {
            result.put('X');
            fail("Expected ReadOnlyBufferException when writing to read‑only buffer");
        } catch (java.nio.ReadOnlyBufferException expected) {
            // expected
        }

        // Ensure original buffer's position/limit are unchanged.
        assertEquals(0, original.position());
        assertEquals(original.limit(), original.capacity());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_NullInput() {
        CharBufferConversions.toCharBuffer(null, converter);
    }

    /* -------------------------------------------------------------
     *  toCharArray
     * ------------------------------------------------------------- */
    @Test
    public void testToCharArray_NonEmptyBuffer() {
        CharBuffer buf = CharBuffer.wrap("hello");
        char[] result = CharBufferConversions.toCharArray(buf, converter);
        assertArrayEquals(new char[]{'h','e','l','l','o'}, result);
    }

    @Test
    public void testToCharArray_EmptyBuffer() {
        CharBuffer empty = CharBuffer.wrap("");
        char[] result = CharBufferConversions.toCharArray(empty, converter);
        // Should return the shared constant EMPTY_CHAR_ARRAY
        assertSame("Empty buffer must return EMPTY_CHAR_ARRAY", EMPTY_CHAR_ARRAY, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_NullInput() {
        CharBufferConversions.toCharArray(null, converter);
    }

    /* -------------------------------------------------------------
     *  toString
     * ------------------------------------------------------------- */
    @Test
    public void testToString() {
        CharBuffer buf = CharBuffer.wrap("JUnit");
        String result = CharBufferConversions.toString(buf, converter);
        assertEquals("JUnit", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        CharBufferConversions.toString(null, converter);
    }

    /* -------------------------------------------------------------
     *  toStringBuffer
     * ------------------------------------------------------------- */
    @Test
    public void testToStringBuffer() {
        CharBuffer buf = CharBuffer.wrap("StringBuffer");
        StringBuffer sb = CharBufferConversions.toStringBuffer(buf, converter);
        assertEquals("StringBuffer", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_NullInput() {
        CharBufferConversions.toStringBuffer(null, converter);
    }

    /* -------------------------------------------------------------
     *  toStringBuilder
     * ------------------------------------------------------------- */
    @Test
    public void testToStringBuilder() {
        CharBuffer buf = CharBuffer.wrap("StringBuilder");
        StringBuilder sb = CharBufferConversions.toStringBuilder(buf, converter);
        assertEquals("StringBuilder", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_NullInput() {
        CharBufferConversions.toStringBuilder(null, converter);
    }

    /* -------------------------------------------------------------
     *  toByteBuffer & toByteArray (indirectly via toByteArray)
     * ------------------------------------------------------------- */
    @Test
    public void testToByteArray_MatchesCharsetEncoding() {
        CharBuffer buf = CharBuffer.wrap("bytes");
        byte[] expected = buf.toString().getBytes(charset);
        byte[] actual = CharBufferConversions.toByteArray(buf, converter);
        assertArrayEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArray_NullInput() {
        CharBufferConversions.toByteArray(null, converter);
    }

    /* -------------------------------------------------------------
     *  toMap
     * ------------------------------------------------------------- */
    @Test
    public void testToMap_ContainsValueKey() {
        CharBuffer buf = CharBuffer.wrap("mapValue");
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) CharBufferConversions.toMap(buf, converter);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(VALUE));
        assertEquals("mapValue", map.get(VALUE));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        CharBufferConversions.toMap(null, converter);
    }

    /* -------------------------------------------------------------
     *  Defensive test: ensure that passing a non‑CharBuffer object
     *  fails with a ClassCastException (the implementation casts
     *  directly).  This is an error scenario the original code does
     *  not guard against, and the test documents the current
     *  behaviour.
     * ------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_WrongType() {
        CharBufferConversions.toCharBuffer("not a CharBuffer", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_WrongType() {
        CharBufferConversions.toCharArray("not a CharBuffer", converter);
    }
}
