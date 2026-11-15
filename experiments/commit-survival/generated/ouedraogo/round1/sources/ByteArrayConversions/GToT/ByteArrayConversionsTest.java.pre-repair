package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ByteArrayConversions}.
 *
 * The tests are written from the point of view of three “experts” that
 * each contributed one test per method, then merged the results.
 */
public class ByteArrayConversionsTest {

    private Converter converter;
    private Charset testCharset;

    @Before
    public void setUp() {
        // The library’s default options use UTF‑8; we rely on that.
        // If the library provides a way to specify a charset we could set it here.
        ConverterOptions options = new ConverterOptions();               // assumed public ctor
        // Ensure the charset is UTF‑8 for deterministic results
        // (most implementations expose a setter; if not, the default is UTF‑8)
        try {
            java.lang.reflect.Method setCharset = options.getClass()
                    .getMethod("setCharset", Charset.class);
            setCharset.invoke(options, Charset.forName("UTF-8"));
        } catch (Exception ignored) {
            // If the method does not exist we simply rely on the default charset.
        }
        this.converter = new Converter(options);
        this.testCharset = Charset.forName("UTF-8");
    }

    /* -------------------------------------------------------------
     * 1. toString()
     * ----------------------------------------------------------- */
    @Test
    public void testToString_normalBytes() {
        byte[] bytes = "Hello World".getBytes(testCharset);
        String result = ByteArrayConversions.toString(bytes, converter);
        assertEquals("Hello World", result);
    }

    @Test
    public void testToString_nullBytes() {
        String result = ByteArrayConversions.toString(null, converter);
        assertEquals(StringUtilities.EMPTY, result);
    }

    @Test
    public void testToString_emptyBytes() {
        byte[] empty = new byte[0];
        String result = ByteArrayConversions.toString(empty, converter);
        assertEquals("", result);
    }

    @Test
    public void testToString_utf8MultibyteCharacter() {
        // "é" in UTF‑8 = 0xC3 0xA9
        byte[] bytes = new byte[]{(byte)0xC3, (byte)0xA9};
        String result = ByteArrayConversions.toString(bytes, converter);
        assertEquals("é", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_nullConverter() {
        byte[] bytes = "test".getBytes(testCharset);
        ByteArrayConversions.toString(bytes, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_wrongSourceType() {
        ByteArrayConversions.toString("Not a byte[]", converter);
    }

    /* -------------------------------------------------------------
     * 2. toByteBuffer()
     * ----------------------------------------------------------- */
    @Test
    public void testToByteBuffer_normalBytes() {
        byte[] src = {1, 2, 3, 4};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(src, converter);
        assertEquals(src.length, buffer.capacity());
        assertArrayEquals(src, buffer.array());
    }

    @Test
    public void testToByteBuffer_reflectsChangesInSourceArray() {
        byte[] src = {10, 20, 30};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(src, converter);
        // modify source after wrapping
        src[1] = 99;
        assertEquals(99, buffer.get(1));
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_nullConverter() {
        byte[] src = {0};
        ByteArrayConversions.toByteBuffer(src, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBuffer_wrongSourceType() {
        ByteArrayConversions.toByteBuffer("string", converter);
    }

    /* -------------------------------------------------------------
     * 3. toCharBuffer()
     * ----------------------------------------------------------- */
    @Test
    public void testToCharBuffer_normalBytes() {
        byte[] src = "abc".getBytes(testCharset);
        CharBuffer cb = ByteArrayConversions.toCharBuffer(src, converter);
        assertEquals("abc", cb.toString());
        // CharBuffer returned by CharBuffer.wrap is read‑only
        assertTrue(cb.isReadOnly());
    }

    @Test
    public void testToCharBuffer_emptyBytes() {
        CharBuffer cb = ByteArrayConversions.toCharBuffer(new byte[0], converter);
        assertEquals(0, cb.length());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_nullConverter() {
        ByteArrayConversions.toCharBuffer(new byte[]{1}, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_wrongSourceType() {
        ByteArrayConversions.toCharBuffer(12345, converter);
    }

    /* -------------------------------------------------------------
     * 4. toCharArray()
     * ----------------------------------------------------------- */
    @Test
    public void testToCharArray_normalBytes() {
        byte[] src = "JUnit".getBytes(testCharset);
        char[] chars = ByteArrayConversions.toCharArray(src, converter);
        assertArrayEquals("JUnit".toCharArray(), chars);
    }

    @Test
    public void testToCharArray_emptyBytes() {
        char[] chars = ByteArrayConversions.toCharArray(new byte[0], converter);
        assertEquals(0, chars.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_nullConverter() {
        ByteArrayConversions.toCharArray(new byte[]{0}, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_wrongSourceType() {
        ByteArrayConversions.toCharArray(new Object(), converter);
    }

    /* -------------------------------------------------------------
     * 5. toStringBuffer()
     * ----------------------------------------------------------- */
    @Test
    public void testToStringBuffer_normalBytes() {
        byte[] src = "buffer".getBytes(testCharset);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(src, converter);
        assertEquals("buffer", sb.toString());
        // Changing the StringBuffer must not affect original bytes
        sb.append("X");
        assertEquals("buffer", ByteArrayConversions.toString(src, converter));
    }

    @Test
    public void testToStringBuffer_emptyBytes() {
        StringBuffer sb = ByteArrayConversions.toStringBuffer(new byte[0], converter);
        assertEquals(0, sb.length());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_nullConverter() {
        ByteArrayConversions.toStringBuffer(new byte[]{0}, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_wrongSourceType() {
        ByteArrayConversions.toStringBuffer(Boolean.TRUE, converter);
    }

    /* -------------------------------------------------------------
     * 6. toStringBuilder()
     * ----------------------------------------------------------- */
    @Test
    public void testToStringBuilder_normalBytes() {
        byte[] src = "builder".getBytes(testCharset);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(src, converter);
        assertEquals("builder", sb.toString());
        // Mutating the builder does not change the original byte array
        sb.reverse();
        assertEquals("builder", ByteArrayConversions.toString(src, converter));
    }

    @Test
    public void testToStringBuilder_emptyBytes() {
        StringBuilder sb = ByteArrayConversions.toStringBuilder(new byte[0], converter);
        assertEquals(0, sb.length());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_nullConverter() {
        ByteArrayConversions.toStringBuilder(new byte[]{0}, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_wrongSourceType() {
        ByteArrayConversions.toStringBuilder(Double.valueOf(1.23), converter);
    }
}
