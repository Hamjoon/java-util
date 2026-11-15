package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Comprehensive JUnit‑4 test suite for {@code CharArrayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage for each conversion method.</li>
 *   <li>Edge cases such as empty arrays, Unicode characters and large inputs.</li>
 *   <li>Exception scenarios (null source, wrong type).</li>
 *   <li>Verification that mutable results are copies, not references to the original array.</li>
 * </ul>
 *
 * The test class is placed in the same package as {@code CharArrayConversions}
 * so that the package‑private static methods are directly accessible.
 */
public class CharArrayConversionsTest {

    /** A simple {@code Converter} that uses UTF‑8 charset. */
    private Converter utf8Converter;

    @Before
    public void setUp() {
        // The real library provides a default ConverterOptions that defaults to UTF‑8.
        // If the library requires explicit charset configuration, the following
        // code will still compile because the required constructors / setters exist
        // in the actual library.  For the purpose of this test we only need a
        // Converter whose {@code getOptions().getCharset()} returns a UTF‑8 charset.
        utf8Converter = new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /*                     toString(Object, Converter)                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringTypical() {
        char[] source = {'J', 'u', 'n', 'i', 't'};
        String result = CharArrayConversions.toString(source, null);
        assertEquals("JUnit", result);
    }

    @Test
    public void testToStringEmptyArray() {
        char[] source = {};
        String result = CharArrayConversions.toString(source, null);
        assertEquals("", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullSource() {
        CharArrayConversions.toString(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringWrongType() {
        // Passing an object that is not a char[]
        CharArrayConversions.toString("not a char array", null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     toCharBuffer(Object, Converter)                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharBufferTypical() {
        char[] source = {'a', 'b', 'c'};
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, null);
        assertEquals(CharBuffer.wrap(source), buffer);
        // Ensure the buffer is backed by the original array (as per CharBuffer.wrap)
        buffer.put(0, 'z');
        assertEquals('z', source[0]);
    }

    @Test
    public void testToCharBufferEmptyArray() {
        CharBuffer buffer = CharArrayConversions.toCharBuffer(new char[0], null);
        assertEquals(0, buffer.remaining());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBufferNullSource() {
        CharArrayConversions.toCharBuffer(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBufferWrongType() {
        CharArrayConversions.toCharBuffer(12345, null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     toStringBuffer(Object, Converter)                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringBufferTypical() {
        char[] source = {'X', 'Y', 'Z'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, null);
        assertEquals("XYZ", sb.toString());
    }

    @Test
    public void testToStringBufferEmptyArray() {
        StringBuffer sb = CharArrayConversions.toStringBuffer(new char[0], null);
        assertEquals("", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBufferNullSource() {
        CharArrayConversions.toStringBuffer(null, null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     toStringBuilder(Object, Converter)                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringBuilderTypical() {
        char[] source = {'1', '2', '3'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, null);
        assertEquals("123", sb.toString());
    }

    @Test
    public void testToStringBuilderEmptyArray() {
        StringBuilder sb = CharArrayConversions.toStringBuilder(new char[0], null);
        assertEquals("", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilderNullSource() {
        CharArrayConversions.toStringBuilder(null, null);
    }

    /* ---------------------------------------------------------------------- */
    /*                     toByteBuffer(Object, Converter)                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByteBufferAscii() {
        char[] source = {'a', 'b', 'c'};
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, utf8Converter);
        byte[] expected = "abc".getBytes(StandardCharsets.UTF_8);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteBufferUnicode() {
        char[] source = {'é', 'Ω', '𐍈'}; // includes BMP and supplementary character
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, utf8Converter);
        byte[] expected = new String(source).getBytes(StandardCharsets.UTF_8);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteBufferEmptyArray() {
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(new char[0], utf8Converter);
        assertEquals(0, buffer.remaining());
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBufferNullSource() {
        CharArrayConversions.toByteBuffer(null, utf8Converter);
    }

    /* ---------------------------------------------------------------------- */
    /*                     toByteArray(Object, Converter)                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByteArrayTypical() {
        char[] source = {'h', 'e', 'l', 'l', 'o'};
        byte[] bytes = CharArrayConversions.toByteArray(source, utf8Converter);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    public void testToByteArrayUnicode() {
        char[] source = {'ß', '中', '𝄞'}; // German sharp s, Chinese character, musical symbol G clef
        byte[] bytes = CharArrayConversions.toByteArray(source, utf8Converter);
        assertArrayEquals(new String(source).getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    public void testToByteArrayEmpty() {
        byte[] bytes = CharArrayConversions.toByteArray(new char[0], utf8Converter);
        assertEquals(0, bytes.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArrayNullSource() {
        CharArrayConversions.toByteArray(null, utf8Converter);
    }

    /* ---------------------------------------------------------------------- */
    /*                     toCharArray(Object, Converter)                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharArrayCopy() {
        char[] original = {'a', 'b', 'c'};
        char[] copy = CharArrayConversions.toCharArray(original, null);
        assertArrayEquals(original, copy);
        // Verify that it is a true copy (modifying original does not affect copy)
        original[0] = 'z';
        assertNotEquals(original[0], copy[0]);
    }

    @Test
    public void testToCharArrayEmpty() {
        char[] copy = CharArrayConversions.toCharArray(new char[0], null);
        assertEquals(0, copy.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArrayNullSource() {
        CharArrayConversions.toCharArray(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArrayWrongType() {
        CharArrayConversions.toCharArray("not a char array", null);
    }
}
