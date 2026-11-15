package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link CharArrayConversions}.
 */
public class CharArrayConversionsTest {

    private Converter converter;
    private Charset charset;

    @Before
    public void setUp() {
        // The real library provides a default ConverterOptions that contains a Charset.
        // For the purpose of these tests we rely on the default behaviour (UTF‑8).
        ConverterOptions options = new ConverterOptions(); // assume default ctor exists
        converter = new Converter(options);
        charset = options.getCharset(); // should be non‑null (UTF‑8 in the library)
    }

    /* ---------------------------------------------------------------------- */
    /* toString                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringSimple() {
        char[] source = {'h', 'e', 'l', 'l', 'o'};
        String result = CharArrayConversions.toString(source, converter);
        assertEquals("hello", result);
    }

    @Test
    public void testToStringEmpty() {
        char[] source = {};
        String result = CharArrayConversions.toString(source, converter);
        assertEquals("", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNull() {
        CharArrayConversions.toString(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* toCharBuffer                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharBufferSharesArray() {
        char[] source = {'a', 'b', 'c'};
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, converter);
        assertEquals(3, buffer.remaining());
        assertEquals('a', buffer.get(0));
        assertEquals('b', buffer.get(1));
        assertEquals('c', buffer.get(2));

        // modify the buffer and verify the original array changes (shared backing array)
        buffer.put(1, 'z');
        assertEquals('z', source[1]);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBufferNull() {
        CharArrayConversions.toCharBuffer(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* toStringBuffer                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringBufferContent() {
        char[] source = {'J', 'U', 'N', 'I', 'T'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, converter);
        assertEquals("JUNIT", sb.toString());
    }

    @Test
    public void testToStringBufferEmpty() {
        char[] source = {};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, converter);
        assertEquals("", sb.toString());
    }

    /* ---------------------------------------------------------------------- */
    /* toStringBuilder                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringBuilderContent() {
        char[] source = {'B', 'u', 'i', 'l', 'd', 'e', 'r'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertEquals("Builder", sb.toString());
    }

    @Test
    public void testToStringBuilderEmpty() {
        char[] source = {};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertEquals("", sb.toString());
    }

    /* ---------------------------------------------------------------------- */
    /* toByteArray                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByteArrayMatchesCharsetEncoding() {
        char[] source = {'é', 'ß', 'Ω'}; // characters that produce multi‑byte sequences in UTF‑8
        byte[] expected = new String(source).getBytes(charset);
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArrayEmpty() {
        char[] source = {};
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        assertEquals(0, actual.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArrayNull() {
        CharArrayConversions.toByteArray(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* toByteBuffer                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByteBufferContent() {
        char[] source = {'A', 'B', 'C'};
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converter);
        byte[] expected = new String(source).getBytes(charset);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteBufferEmpty() {
        char[] source = {};
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converter);
        assertEquals(0, buffer.remaining());
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBufferNull() {
        CharArrayConversions.toByteBuffer(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* toCharArray                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharArrayCopiesContent() {
        char[] source = {'x', 'y', 'z'};
        char[] copy = CharArrayConversions.toCharArray(source, converter);
        assertArrayEquals(source, copy);

        // modify the copy and ensure the original is unchanged
        copy[0] = 'a';
        assertEquals('x', source[0]);
    }

    @Test
    public void testToCharArrayEmpty() {
        char[] source = {};
        char[] copy = CharArrayConversions.toCharArray(source, converter);
        assertArrayEquals(source, copy);
        assertNotSame(source, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArrayNull() {
        CharArrayConversions.toCharArray(null, converter);
    }
}
