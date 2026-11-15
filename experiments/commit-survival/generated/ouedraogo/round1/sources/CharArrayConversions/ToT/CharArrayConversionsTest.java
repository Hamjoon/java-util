package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

public class CharArrayConversionsTest {

    private Converter converterUtf8;
    private Converter converterIso;

    @Before
    public void setUp() {
        // Default UTF-8 charset
        ConverterOptions utf8Options = new ConverterOptions(Charset.forName("UTF-8"));
        converterUtf8 = new Converter(utf8Options);

        // ISO-8859-1 charset for alternative testing
        ConverterOptions isoOptions = new ConverterOptions(Charset.forName("ISO-8859-1"));
        converterIso = new Converter(isoOptions);
    }

    // ---------- toCharBuffer ----------
    @Test
    public void testToCharBuffer_Typical() {
        char[] source = {'a', 'b', 'c'};
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, converterUtf8);
        assertNotNull(buffer);
        assertEquals(3, buffer.remaining());
        assertEquals('a', buffer.get());
        assertEquals('b', buffer.get());
        assertEquals('c', buffer.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_WrongSourceType() {
        // Passing a String instead of char[] should cause a ClassCastException
        CharArrayConversions.toCharBuffer("not a char array", converterUtf8);
    }

    // ---------- toString ----------
    @Test
    public void testToString_Typical() {
        char[] source = {'J', 'U', 'N', 'I', 'T'};
        String result = CharArrayConversions.toString(source, converterUtf8);
        assertEquals("JUNIT", result);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongSourceType() {
        CharArrayConversions.toString(12345, converterUtf8);
    }

    // ---------- toStringBuffer ----------
    @Test
    public void testToStringBuffer_Typical() {
        char[] source = {'x', 'y', 'z'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, converterUtf8);
        assertNotNull(sb);
        assertEquals("xyz", sb.toString());
    }

    // ---------- toStringBuilder ----------
    @Test
    public void testToStringBuilder_Typical() {
        char[] source = {'1', '2', '3'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converterUtf8);
        assertNotNull(sb);
        assertEquals("123", sb.toString());
    }

    // ---------- toByteArray ----------
    @Test
    public void testToByteArray_UTF8() {
        char[] source = {'h', 'e', 'l', 'l', 'o'};
        byte[] bytes = CharArrayConversions.toByteArray(source, converterUtf8);
        // UTF-8 encoding of "hello"
        byte[] expected = "hello".getBytes(Charset.forName("UTF-8"));
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testToByteArray_ISO8859_1() {
        char[] source = {'h', 'e', 'l', 'l', 'o'};
        byte[] bytes = CharArrayConversions.toByteArray(source, converterIso);
        byte[] expected = "hello".getBytes(Charset.forName("ISO-8859-1"));
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testToByteArray_WithNonAsciiCharacters_UTF8() {
        char[] source = {'é', 'ß', 'Ω'}; // characters outside ISO-8859-1 range
        byte[] bytes = CharArrayConversions.toByteArray(source, converterUtf8);
        byte[] expected = new String(source).getBytes(Charset.forName("UTF-8"));
        assertArrayEquals(expected, bytes);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteArray_WrongSourceType() {
        CharArrayConversions.toByteArray("string", converterUtf8);
    }

    // ---------- toCharArray ----------
    @Test
    public void testToCharArray_Typical() {
        char[] source = {'a', 'b', 'c'};
        char[] copy = CharArrayConversions.toCharArray(source, converterUtf8);
        assertNotSame(source, copy); // must be a new array
        assertArrayEquals(source, copy);
    }

    @Test
    public void testToCharArray_EmptyArray() {
        char[] source = {};
        char[] copy = CharArrayConversions.toCharArray(source, converterUtf8);
        assertNotNull(copy);
        assertEquals(0, copy.length);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_WrongSourceType() {
        CharArrayConversions.toCharArray(42, converterUtf8);
    }

    // ---------- toByteBuffer ----------
    @Test
    public void testToByteBuffer_UTF8() {
        char[] source = {'J', 'a', 'v', 'a'};
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converterUtf8);
        assertNotNull(buffer);
        byte[] expected = "Java".getBytes(Charset.forName("UTF-8"));
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteBuffer_ISO8859_1() {
        char[] source = {'J', 'a', 'v', 'a'};
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converterIso);
        byte[] expected = "Java".getBytes(Charset.forName("ISO-8859-1"));
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBuffer_WrongSourceType() {
        CharArrayConversions.toByteBuffer(new Object(), converterUtf8);
    }
}
