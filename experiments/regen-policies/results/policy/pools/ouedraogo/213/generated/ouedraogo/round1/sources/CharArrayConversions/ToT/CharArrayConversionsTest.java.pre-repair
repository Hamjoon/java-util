package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CharArrayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical use cases – normal, non‑empty char arrays.</li>
 *   <li>Edge cases – empty arrays and Unicode surrogate pairs.</li>
 *   <li>Error scenarios – passing {@code null} where a {@code char[]} is required.</li>
 *   <li>Behaviour of the returned mutable containers (StringBuffer, StringBuilder, CharBuffer).</li>
 *   <li>Correct handling of the {@link Converter} charset (UTF‑8 by default).</li>
 * </ul>
 *
 * No mocking frameworks are used; real {@link Converter} and {@link ConverterOptions}
 * objects are instantiated as provided by the library.
 */
public class CharArrayConversionsTest
{
    private Converter converter;               // uses default options (UTF‑8 charset)
    private ConverterOptions defaultOptions;   // the concrete class is supplied by the library

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp()
    {
        // The library supplies a public no‑arg constructor for ConverterOptions.
        // If the real library requires a different construction, adjust accordingly.
        defaultOptions = new ConverterOptions();          // default charset = UTF‑8
        converter = new Converter(defaultOptions);
    }

    /* ----------------------------------------------------------------------
     * toString(char[])
     * ---------------------------------------------------------------------- */
    @Test
    public void testToStringTypical()
    {
        char[] source = {'J', 'u', 'n', 'i', 't'};
        String result = CharArrayConversions.toString(source, converter);
        assertEquals("JUnit", result);
    }

    @Test
    public void testToStringEmpty()
    {
        char[] source = new char[0];
        String result = CharArrayConversions.toString(source, converter);
        assertEquals("", result);
    }

    @Test
    public void testToStringNull()
    {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toString(null, converter);
    }

    /* ----------------------------------------------------------------------
     * toCharBuffer(char[])
     * ---------------------------------------------------------------------- */
    @Test
    public void testToCharBufferTypical()
    {
        char[] source = {'a', 'b', 'c'};
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, converter);
        assertEquals(3, buffer.remaining());
        assertArrayEquals(source, buffer.array());

        // Verify that the CharBuffer is a *view* of the original array.
        source[0] = 'z';
        assertEquals('z', buffer.get(0));
    }

    @Test
    public void testToCharBufferEmpty()
    {
        CharBuffer buffer = CharArrayConversions.toCharBuffer(new char[0], converter);
        assertEquals(0, buffer.remaining());
    }

    @Test
    public void testToCharBufferNull()
    {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toCharBuffer(null, converter);
    }

    /* ----------------------------------------------------------------------
     * toStringBuffer(char[])
     * ---------------------------------------------------------------------- */
    @Test
    public void testToStringBufferTypical()
    {
        char[] source = {'x', 'y', 'z'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, converter);
        assertEquals("xyz", sb.toString());

        // Mutability: changes to the buffer must not affect the original array.
        sb.append('!');
        assertEquals("xyz!", sb.toString());
        assertArrayEquals(new char[]{'x', 'y', 'z'}, source);
    }

    @Test
    public void testToStringBufferEmpty()
    {
        StringBuffer sb = CharArrayConversions.toStringBuffer(new char[0], converter);
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBufferNull()
    {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toStringBuffer(null, converter);
    }

    /* ----------------------------------------------------------------------
     * toStringBuilder(char[])
     * ---------------------------------------------------------------------- */
    @Test
    public void testToStringBuilderTypical()
    {
        char[] source = {'1', '2', '3'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertEquals("123", sb.toString());

        sb.append('4');
        assertEquals("1234", sb.toString());
        assertArrayEquals(new char[]{'1', '2', '3'}, source);
    }

    @Test
    public void testToStringBuilderEmpty()
    {
        StringBuilder sb = CharArrayConversions.toStringBuilder(new char[0], converter);
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuilderNull()
    {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toStringBuilder(null, converter);
    }

    /* ----------------------------------------------------------------------
     * toByteArray(char[])
     * ---------------------------------------------------------------------- */
    @Test
    public void testToByteArrayTypical()
    {
        char[] source = {'A', 'B', 'C'};
        byte[] bytes = CharArrayConversions.toByteArray(source, converter);
        // UTF‑8 encoding of "ABC"
        assertArrayEquals(new byte[]{65, 66, 67}, bytes);
    }

    @Test
    public void testToByteArrayWithSurrogatePair()
    {
        // Unicode U+1F600 (GRINNING FACE) -> surrogate pair \uD83D\uDE00
        char[] source = {'\uD83D', '\uDE00'};
        byte[] bytes = CharArrayConversions.toByteArray(source, converter);
        // UTF‑8 bytes for U+1F600: F0 9F 98 80
        assertArrayEquals(new byte[]{(byte)0xF0, (byte)0x9F, (byte)0x98, (byte)0x80}, bytes);
    }

    @Test
    public void testToByteArrayEmpty()
    {
        byte[] bytes = CharArrayConversions.toByteArray(new char[0], converter);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testToByteArrayNull()
    {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toByteArray(null, converter);
    }

    /* ----------------------------------------------------------------------
     * toByteBuffer(char[])
     * ---------------------------------------------------------------------- */
    @Test
    public void testToByteBufferTypical()
    {
        char[] source = {'h', 'i'};
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converter);
        assertEquals(2, buffer.remaining());

        byte[] expected = "hi".getBytes(Charset.forName("UTF-8"));
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteBufferSurrogatePair()
    {
        char[] source = {'\uD83D', '\uDE03'}; // U+1F603 (SMILING FACE WITH OPEN MOUTH)
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converter);
        byte[] expected = new byte[]{(byte)0xF0, (byte)0x9F, (byte)0x98, (byte)0x83};
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteBufferEmpty()
    {
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(new char[0], converter);
        assertEquals(0, buffer.remaining());
    }

    @Test
    public void testToByteBufferNull()
    {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toByteBuffer(null, converter);
    }

    /* ----------------------------------------------------------------------
     * toCharArray(char[])
     * ---------------------------------------------------------------------- */
    @Test
    public void testToCharArrayCopy()
    {
        char[] source = {'x', 'y', 'z'};
        char[] copy = CharArrayConversions.toCharArray(source, converter);
        assertArrayEquals(source, copy);

        // Verify deep copy – modifications to source must not affect the result.
        source[0] = 'a';
        assertEquals('x', copy[0]);
    }

    @Test
    public void testToCharArrayEmpty()
    {
        char[] copy = CharArrayConversions.toCharArray(new char[0], converter);
        assertEquals(0, copy.length);
    }

    @Test
    public void testToCharArrayNull()
    {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toCharArray(null, converter);
    }
}
