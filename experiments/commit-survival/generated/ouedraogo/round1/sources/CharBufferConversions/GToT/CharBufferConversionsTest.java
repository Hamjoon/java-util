package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
import static org.junit.Assert.*;

public class CharBufferConversionsTest {

    private Converter converter;
    private Charset testCharset = StandardCharsets.UTF_8;   // deterministic charset

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor; we set the charset.
        // The real library exposes a setter; if not, the default is UTF‑8 which is fine.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(testCharset);
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------
     * 1. toCharBuffer
     * -------------------------------------------------------------- */
    @Test
    public void testToCharBufferBasic() {
        CharBuffer src = CharBuffer.wrap("hello world");
        CharBuffer result = CharBufferConversions.toCharBuffer(src, converter);

        // The result must be a read‑only view
        assertTrue(result.isReadOnly());

        // Content must be identical
        assertEquals(src.toString(), result.toString());

        // Changing the original buffer should NOT affect the read‑only view
        src.position(0);
        src.put('H');               // modify source
        assertNotEquals(src.toString(), result.toString());
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBufferWithWrongType() {
        // Passing a String (not a CharBuffer) forces the cast to fail
        CharBufferConversions.toCharBuffer("not a buffer", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBufferWithNull() {
        CharBufferConversions.toCharBuffer(null, converter);
    }

    /* --------------------------------------------------------------
     * 2. toByteArray
     * -------------------------------------------------------------- */
    @Test
    public void testToByteArrayBasic() {
        CharBuffer src = CharBuffer.wrap("ABC");
        byte[] bytes = CharBufferConversions.toByteArray(src, converter);
        byte[] expected = "ABC".getBytes(testCharset);
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testToByteArrayEmptyBuffer() {
        CharBuffer src = CharBuffer.allocate(0); // empty, read‑only not required
        byte[] bytes = CharBufferConversions.toByteArray(src, converter);
        assertEquals(0, bytes.length);
    }

    /* --------------------------------------------------------------
     * 3. toByteBuffer
     * -------------------------------------------------------------- */
    @Test
    public void testToByteBufferBasic() {
        CharBuffer src = CharBuffer.wrap("xyz");
        ByteBuffer bb = CharBufferConversions.toByteBuffer(src, converter);
        byte[] expected = "xyz".getBytes(testCharset);
        byte[] actual = new byte[bb.remaining()];
        bb.get(actual);
        assertArrayEquals(expected, actual);
        // Position should be at the start after conversion (encode returns a buffer ready for reading)
        assertEquals(0, bb.position());
    }

    @Test
    public void testToByteBufferEmpty() {
        CharBuffer src = CharBuffer.allocate(0);
        ByteBuffer bb = CharBufferConversions.toByteBuffer(src, converter);
        assertEquals(0, bb.remaining());
    }

    /* --------------------------------------------------------------
     * 4. toString
     * -------------------------------------------------------------- */
    @Test
    public void testToStringBasic() {
        CharBuffer src = CharBuffer.wrap("sample text");
        String s = CharBufferConversions.toString(src, converter);
        assertEquals("sample text", s);
    }

    @Test
    public void testToStringEmpty() {
        CharBuffer src = CharBuffer.allocate(0);
        assertEquals("", CharBufferConversions.toString(src, converter));
    }

    /* --------------------------------------------------------------
     * 5. toCharArray
     * -------------------------------------------------------------- */
    @Test
    public void testToCharArrayBasic() {
        CharBuffer src = CharBuffer.wrap("charArray");
        char[] arr = CharBufferConversions.toCharArray(src, converter);
        assertArrayEquals("charArray".toCharArray(), arr);
    }

    @Test
    public void testToCharArrayEmpty() {
        CharBuffer src = CharBuffer.allocate(0);
        char[] arr = CharBufferConversions.toCharArray(src, converter);
        // Must be the exact constant defined in ArrayUtilities
        assertSame(EMPTY_CHAR_ARRAY, arr);
    }

    @Test
    public void testToCharArraySingleChar() {
        CharBuffer src = CharBuffer.wrap("Z");
        char[] arr = CharBufferConversions.toCharArray(src, converter);
        assertEquals(1, arr.length);
        assertEquals('Z', arr[0]);
    }

    /* --------------------------------------------------------------
     * 6. toStringBuffer
     * -------------------------------------------------------------- */
    @Test
    public void testToStringBufferBasic() {
        CharBuffer src = CharBuffer.wrap("buffer");
        StringBuffer sb = CharBufferConversions.toStringBuffer(src, converter);
        assertEquals("buffer", sb.toString());
    }

    @Test
    public void testToStringBufferEmpty() {
        CharBuffer src = CharBuffer.allocate(0);
        StringBuffer sb = CharBufferConversions.toStringBuffer(src, converter);
        assertEquals(0, sb.length());
    }

    /* --------------------------------------------------------------
     * 7. toStringBuilder
     * -------------------------------------------------------------- */
    @Test
    public void testToStringBuilderBasic() {
        CharBuffer src = CharBuffer.wrap("builder");
        StringBuilder sb = CharBufferConversions.toStringBuilder(src, converter);
        assertEquals("builder", sb.toString());
    }

    @Test
    public void testToStringBuilderEmpty() {
        CharBuffer src = CharBuffer.allocate(0);
        StringBuilder sb = CharBufferConversions.toStringBuilder(src, converter);
        assertEquals(0, sb.length());
    }
}
