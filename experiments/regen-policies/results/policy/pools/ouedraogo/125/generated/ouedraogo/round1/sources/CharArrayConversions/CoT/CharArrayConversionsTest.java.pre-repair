package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * JUnit 4 test cases for {@link CharArrayConversions}.
 *
 * The tests are placed in the same package as {@code CharArrayConversions}
 * so that package‑private static methods can be accessed directly.
 */
public class CharArrayConversionsTest {

    private Converter converter;
    private Charset charset;

    @Before
    public void setUp() {
        // The library provides a Converter that requires ConverterOptions.
        // We rely on the default options (which use UTF‑8) for our tests.
        // If the library requires explicit charset configuration, the default
        // constructor will still supply a usable Charset.
        converter = new Converter(new ConverterOptions());
        charset = converter.getOptions().getCharset(); // expected to be UTF‑8
    }

    /* ----------------------------------------------------------------------
     *  List of package‑private static methods (the "public" API for the tests)
     * ----------------------------------------------------------------------
     *
     * static ByteBuffer toByteBuffer(Object from, Converter converter)
     * static String toString(Object from, Converter converter)
     * static CharBuffer toCharBuffer(Object from, Converter converter)
     * static StringBuffer toStringBuffer(Object from, Converter converter)
     * static StringBuilder toStringBuilder(Object from, Converter converter)
     * static byte[] toByteArray(Object from, Converter converter)
     * static char[] toCharArray(Object from, Converter converter)
     */

    // ---------------------------- Basic functionality ----------------------------

    @Test
    public void testToString_basic() {
        char[] source = {'h', 'e', 'l', 'l', 'o'};
        String result = CharArrayConversions.toString(source, converter);
        assertEquals("hello", result);
    }

    @Test
    public void testToCharBuffer_basic() {
        char[] source = {'J', 'U', 'N', 'I', 'T'};
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, converter);
        assertNotNull(buffer);
        assertEquals(source.length, buffer.remaining());
        assertArrayEquals(source, buffer.array());
    }

    @Test
    public void testToStringBuffer_basic() {
        char[] source = {'a', 'b', 'c'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, converter);
        assertNotNull(sb);
        assertEquals("abc", sb.toString());
    }

    @Test
    public void testToStringBuilder_basic() {
        char[] source = {'x', 'y', 'z'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertNotNull(sb);
        assertEquals("xyz", sb.toString());
    }

    @Test
    public void testToByteBuffer_basic() {
        char[] source = {'A', 'B', 'C'};
        ByteBuffer byteBuffer = CharArrayConversions.toByteBuffer(source, converter);
        assertNotNull(byteBuffer);
        byte[] expected = new String(source).getBytes(charset);
        byte[] actual = new byte[byteBuffer.remaining()];
        byteBuffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArray_basic() {
        char[] source = {'1', '2', '3'};
        byte[] bytes = CharArrayConversions.toByteArray(source, converter);
        assertNotNull(bytes);
        assertArrayEquals(new String(source).getBytes(charset), bytes);
    }

    @Test
    public void testToCharArray_copy() {
        char[] source = {'p', 'q', 'r'};
        char[] copy = CharArrayConversions.toCharArray(source, converter);
        assertNotNull(copy);
        assertArrayEquals(source, copy);
        // Verify that the returned array is a *different* instance
        assertNotSame(source, copy);
        // Changing the original must not affect the copy
        source[0] = 'z';
        assertEquals('p', copy[0]);
    }

    // ---------------------------- Edge Cases ----------------------------

    @Test
    public void testEmptyArrayConversions() {
        char[] empty = new char[0];

        assertEquals("", CharArrayConversions.toString(empty, converter));
        assertEquals(0, CharArrayConversions.toCharBuffer(empty, converter).remaining());
        assertEquals("", CharArrayConversions.toStringBuffer(empty, converter).toString());
        assertEquals("", CharArrayConversions.toStringBuilder(empty, converter).toString());
        assertEquals(0, CharArrayConversions.toByteBuffer(empty, converter).remaining());
        assertEquals(0, CharArrayConversions.toByteArray(empty, converter).length);
        assertArrayEquals(empty, CharArrayConversions.toCharArray(empty, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToString_nullInput() {
        CharArrayConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_nullInput() {
        CharArrayConversions.toCharBuffer(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_nullInput() {
        CharArrayConversions.toStringBuffer(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_nullInput() {
        CharArrayConversions.toStringBuilder(null, converter);
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
    public void testToString_wrongType() {
        // Passing an object that is NOT a char[] should trigger a ClassCastException
        CharArrayConversions.toString("not a char array", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_wrongType() {
        CharArrayConversions.toCharBuffer(12345, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_wrongType() {
        CharArrayConversions.toStringBuffer(new Object(), converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_wrongType() {
        CharArrayConversions.toStringBuilder(Boolean.TRUE, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBuffer_wrongType() {
        CharArrayConversions.toByteBuffer(new int[]{1, 2, 3}, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteArray_wrongType() {
        CharArrayConversions.toByteArray(new double[]{0.1, 0.2}, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_wrongType() {
        CharArrayConversions.toCharArray(new String("abc"), converter);
    }

    // ---------------------------- Charset specific edge case ----------------------------

    @Test
    public void testToByteBuffer_nonAsciiCharacters() {
        // Use characters that require more than one byte in UTF‑8 (e.g., Euro sign, Chinese)
        char[] source = {'\u20AC', '\u4E2D', '\u6587'}; // €中文
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converter);
        byte[] expected = new String(source).getBytes(charset);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals("UTF‑8 encoding of non‑ASCII chars must match", expected, actual);
    }

    @Test
    public void testToByteArray_nonAsciiCharacters() {
        char[] source = {'\u00A9', '\u2603'}; // ©☃
        byte[] bytes = CharArrayConversions.toByteArray(source, converter);
        assertArrayEquals(new String(source).getBytes(charset), bytes);
    }
}
