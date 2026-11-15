package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link ByteArrayConversions}.
 *
 * The test class lives in the same package as the class under test,
 * therefore it can access the package‑private static methods.
 */
public class ByteArrayConversionsTest {

    private Converter converter;               // a Converter that uses UTF‑8 charset
    private static final String EMPTY = StringUtilities.EMPTY; // usually ""

    @Before
    public void setUp() {
        // ConverterOptions is not part of the supplied API, but the library
        // provides a default constructor that uses UTF‑8.  Hence we can create
        // a Converter with default options.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Tests for toString(...)
     * -------------------------------------------------------------- */
    @Test
    public void testToString_NormalBytes() {
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(data, converter);
        assertEquals("Hello World", result);
    }

    @Test
    public void testToString_EmptyByteArray() {
        byte[] data = new byte[0];
        String result = ByteArrayConversions.toString(data, converter);
        assertEquals("", result);
    }

    @Test
    public void testToString_NullByteArray() {
        // The method receives a null reference; it should return EMPTY.
        String result = ByteArrayConversions.toString(null, converter);
        assertEquals(EMPTY, result);
    }

    @Test
    public void testToString_MultiByteCharacters() {
        String original = "Café 漢字";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(data, converter);
        assertEquals(original, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullConverter() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        // Passing a null Converter triggers NPE when getOptions() is called.
        ByteArrayConversions.toString(data, null);
    }

    /* --------------------------------------------------------------
     *  Tests for toByteBuffer(...)
     * -------------------------------------------------------------- */
    @Test
    public void testToByteBuffer_NormalBytes() {
        byte[] data = {1, 2, 3, 4};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, converter);
        assertArrayEquals(data, buffer.array());
    }

    @Test
    public void testToByteBuffer_EmptyArray() {
        byte[] data = new byte[0];
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, converter);
        assertEquals(0, buffer.remaining());
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullArray() {
        ByteArrayConversions.toByteBuffer(null, converter);
    }

    /* --------------------------------------------------------------
     *  Tests for toCharBuffer(...)
     * -------------------------------------------------------------- */
    @Test
    public void testToCharBuffer_NormalString() {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        CharBuffer cb = ByteArrayConversions.toCharBuffer(data, converter);
        assertEquals("abc", cb.toString());
    }

    @Test
    public void testToCharBuffer_EmptyArray() {
        CharBuffer cb = ByteArrayConversions.toCharBuffer(new byte[0], converter);
        assertEquals(0, cb.length());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_NullArray() {
        ByteArrayConversions.toCharBuffer(null, converter);
    }

    /* --------------------------------------------------------------
     *  Tests for toCharArray(...)
     * -------------------------------------------------------------- */
    @Test
    public void testToCharArray_NormalString() {
        byte[] data = "xyz".getBytes(StandardCharsets.UTF_8);
        char[] chars = ByteArrayConversions.toCharArray(data, converter);
        assertArrayEquals(new char[] {'x', 'y', 'z'}, chars);
    }

    @Test
    public void testToCharArray_EmptyArray() {
        char[] chars = ByteArrayConversions.toCharArray(new byte[0], converter);
        assertEquals(0, chars.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_NullArray() {
        ByteArrayConversions.toCharArray(null, converter);
    }

    /* --------------------------------------------------------------
     *  Tests for toStringBuffer(...)
     * -------------------------------------------------------------- */
    @Test
    public void testToStringBuffer_NormalString() {
        byte[] data = "buffer".getBytes(StandardCharsets.UTF_8);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(data, converter);
        assertEquals("buffer", sb.toString());
    }

    @Test
    public void testToStringBuffer_EmptyArray() {
        StringBuffer sb = ByteArrayConversions.toStringBuffer(new byte[0], converter);
        assertEquals("", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_NullArray() {
        ByteArrayConversions.toStringBuffer(null, converter);
    }

    /* --------------------------------------------------------------
     *  Tests for toStringBuilder(...)
     * -------------------------------------------------------------- */
    @Test
    public void testToStringBuilder_NormalString() {
        byte[] data = "builder".getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(data, converter);
        assertEquals("builder", sb.toString());
    }

    @Test
    public void testToStringBuilder_EmptyArray() {
        StringBuilder sb = ByteArrayConversions.toStringBuilder(new byte[0], converter);
        assertEquals("", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_NullArray() {
        ByteArrayConversions.toStringBuilder(null, converter);
    }
}
