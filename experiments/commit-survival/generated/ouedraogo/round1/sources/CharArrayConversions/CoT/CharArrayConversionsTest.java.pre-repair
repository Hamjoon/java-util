package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Test suite for {@link CharArrayConversions}.
 *
 * The tests cover:
 *   – normal functionality for each conversion method,
 *   – edge cases such as null input, empty arrays, wrong input types,
 *   – correctness of charset‑based byte conversion,
 *   – immutability of returned copies.
 *
 * The library’s {@link Converter} class is used directly (no mocking) with a
 * default {@link ConverterOptions} instance.  The default charset of the
 * options is assumed to be UTF‑8 – this is the typical default in the Cedar
 * library; if the library defaults differ the assertions on byte values will
 * still be valid because they compare against the charset obtained from the
 * same {@code Converter} instance.
 */
public class CharArrayConversionsTest {

    private Converter converter;
    private Charset charset;

    @Before
    public void setUp() {
        // The library provides a public constructor Converter(ConverterOptions)
        // and a public method getOptions().getCharset().
        // We rely on the default options (no special configuration required).
        ConverterOptions options = new ConverterOptions();          // assume default ctor exists
        converter = new Converter(options);
        charset = converter.getOptions().getCharset();              // e.g., UTF‑8
        assertNotNull("Charset from ConverterOptions must not be null", charset);
    }

    /* --------------------------------------------------------------
     *  Basic “happy path” tests
     * -------------------------------------------------------------- */

    @Test
    public void testToString_basic() {
        char[] src = {'J','u','n','i','t'};
        String result = CharArrayConversions.toString(src, converter);
        assertEquals("JUnit", result);
    }

    @Test
    public void testToCharBuffer_basic() {
        char[] src = {'a','b','c'};
        CharBuffer buf = CharArrayConversions.toCharBuffer(src, converter);
        assertNotNull(buf);
        assertEquals(3, buf.remaining());
        assertArrayEquals(src, buf.array());
    }

    @Test
    public void testToStringBuffer_basic() {
        char[] src = {'x','y','z'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(src, converter);
        assertEquals("xyz", sb.toString());
    }

    @Test
    public void testToStringBuilder_basic() {
        char[] src = {'1','2','3'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(src, converter);
        assertEquals("123", sb.toString());
    }

    @Test
    public void testToByteBuffer_basic() {
        char[] src = {'A','B','C'};
        ByteBuffer bb = CharArrayConversions.toByteBuffer(src, converter);
        assertNotNull(bb);
        byte[] expected = new String(src).getBytes(charset);
        byte[] actual = new byte[bb.remaining()];
        bb.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArray_basic() {
        char[] src = {'h','e','l','l','o'};
        byte[] bytes = CharArrayConversions.toByteArray(src, converter);
        assertArrayEquals(new String(src).getBytes(charset), bytes);
    }

    @Test
    public void testToCharArray_basic() {
        char[] src = {'p','q','r'};
        char[] copy = CharArrayConversions.toCharArray(src, converter);
        assertArrayEquals(src, copy);
        // Verify that it is a *copy*, not the same reference
        src[0] = 'z';
        assertNotEquals(src[0], copy[0]);
    }

    /* --------------------------------------------------------------
     *  Edge‑case & exception tests
     * -------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToString_nullInput() {
        CharArrayConversions.toString(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_wrongType() {
        CharArrayConversions.toString("not a char array", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_nullInput() {
        CharArrayConversions.toCharBuffer(null, converter);
    }

    @Test
    public void testToCharBuffer_emptyArray() {
        CharBuffer buf = CharArrayConversions.toCharBuffer(new char[0], converter);
        assertNotNull(buf);
        assertEquals(0, buf.remaining());
    }

    @Test
    public void testToStringBuffer_emptyArray() {
        StringBuffer sb = CharArrayConversions.toStringBuffer(new char[0], converter);
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuilder_emptyArray() {
        StringBuilder sb = CharArrayConversions.toStringBuilder(new char[0], converter);
        assertEquals("", sb.toString());
    }

    @Test
    public void testToByteBuffer_emptyArray() {
        ByteBuffer bb = CharArrayConversions.toByteBuffer(new char[0], converter);
        assertNotNull(bb);
        assertEquals(0, bb.remaining());
    }

    @Test
    public void testToByteArray_emptyArray() {
        byte[] bytes = CharArrayConversions.toByteArray(new char[0], converter);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testToCharArray_emptyArray() {
        char[] copy = CharArrayConversions.toCharArray(new char[0], converter);
        assertEquals(0, copy.length);
    }

    @Test
    public void testLargeArrayConversion() {
        int size = 10_000;
        char[] large = new char[size];
        for (int i = 0; i < size; i++) {
            large[i] = (char) ('a' + (i % 26));
        }

        // String conversion
        String s = CharArrayConversions.toString(large, converter);
        assertEquals(size, s.length());
        assertEquals(new String(large), s);

        // CharBuffer conversion
        CharBuffer cb = CharArrayConversions.toCharBuffer(large, converter);
        assertEquals(size, cb.remaining());

        // ByteArray conversion (charset dependent)
        byte[] bytes = CharArrayConversions.toByteArray(large, converter);
        assertArrayEquals(new String(large).getBytes(charset), bytes);

        // CharArray copy
        char[] copy = CharArrayConversions.toCharArray(large, converter);
        assertArrayEquals(large, copy);
        // Ensure independence
        large[0] = 'Z';
        assertNotEquals(large[0], copy[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_nullInput() {
        CharArrayConversions.toByteBuffer(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArray_nullInput() {
        CharArrayConversions.toByteArray(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_nullInput() {
        CharArrayConversions.toCharArray(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_wrongType() {
        CharArrayConversions.toCharArray("string", converter);
    }
}
