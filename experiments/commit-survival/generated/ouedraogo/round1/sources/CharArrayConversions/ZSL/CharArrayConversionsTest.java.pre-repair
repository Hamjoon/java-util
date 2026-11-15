package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Unit tests for {@link CharArrayConversions}.
 *
 * The tests are placed in the same package as the class under test so that the
 * package‑private static methods are directly accessible.
 */
public class CharArrayConversionsTest {

    private Converter converter;
    private Charset charset;

    @Before
    public void setUp() {
        // The real ConverterOptions class is not shown, but it is expected to have a
        // no‑arg constructor and a setCharset/getCharset pair.  Using the default
        // options (which normally default to UTF‑8) is sufficient for our tests.
        ConverterOptions options = new ConverterOptions();
        charset = Charset.forName("UTF-8");
        // If the options class allows setting a charset explicitly, do it.
        try {
            options.getClass().getMethod("setCharset", Charset.class).invoke(options, charset);
        } catch (Exception ignored) {
            // If the method does not exist, rely on the default charset.
        }
        converter = new Converter(options);
    }

    @Test
    public void testToString_WithTypicalCharacters() {
        char[] source = {'J', 'a', 'v', 'a', ' ', 'T', 'e', 's', 't'};
        String result = CharArrayConversions.toString(source, converter);
        assertEquals("Java Test", result);
    }

    @Test
    public void testToString_WithEmptyArray() {
        char[] source = new char[0];
        String result = CharArrayConversions.toString(source, converter);
        assertEquals("", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_WithNull() {
        CharArrayConversions.toString(null, converter);
    }

    @Test
    public void testToCharBuffer_ReturnsWrappedBuffer() {
        char[] source = {'a', 'b', 'c'};
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, converter);
        assertNotNull(buffer);
        assertEquals(3, buffer.remaining());
        assertEquals('a', buffer.get());
        assertEquals('b', buffer.get());
        assertEquals('c', buffer.get());
    }

    @Test
    public void testToCharBuffer_EmptyArray() {
        CharBuffer buffer = CharArrayConversions.toCharBuffer(new char[0], converter);
        assertNotNull(buffer);
        assertEquals(0, buffer.remaining());
    }

    @Test
    public void testToStringBuffer_ContentMatchesSource() {
        char[] source = {'X', 'Y', 'Z'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, converter);
        assertEquals("XYZ", sb.toString());
    }

    @Test
    public void testToStringBuilder_ContentMatchesSource() {
        char[] source = {'1', '2', '3'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertEquals("123", sb.toString());
    }

    @Test
    public void testToByteArray_EncodingMatchesCharset() {
        char[] source = {'h', 'e', 'l', 'l', 'o'};
        byte[] expected = new String(source).getBytes(charset);
        byte[] actual = CharArrayConversions.toByteArray(source, converter);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArray_EmptyArray() {
        byte[] actual = CharArrayConversions.toByteArray(new char[0], converter);
        assertEquals(0, actual.length);
    }

    @Test
    public void testToByteBuffer_ConsistencyWithByteArray() {
        char[] source = {'A', 'B', 'C', 'ø'};
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converter);
        byte[] fromBuffer = new byte[buffer.remaining()];
        buffer.get(fromBuffer);

        byte[] fromMethod = CharArrayConversions.toByteArray(source, converter);
        assertArrayEquals(fromMethod, fromBuffer);
    }

    @Test
    public void testToCharArray_ReturnsCopyNotReference() {
        char[] original = {'p', 'q', 'r'};
        char[] copy = CharArrayConversions.toCharArray(original, converter);
        assertArrayEquals(original, copy);
        // Mutate original and verify copy does not change
        original[0] = 'z';
        assertNotEquals(original[0], copy[0]);
    }

    @Test
    public void testToCharArray_EmptyArray() {
        char[] empty = new char[0];
        char[] result = CharArrayConversions.toCharArray(empty, converter);
        assertArrayEquals(empty, result);
        assertNotSame(empty, result); // should be a different instance
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_NullInput() {
        CharArrayConversions.toCharArray(null, converter);
    }

    @Test
    public void testToByteBuffer_ContentMatchesDirectEncoding() {
        char[] source = {'J', 'U', 'N', 'I', 'T'};
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converter);
        byte[] expected = new String(source).getBytes(charset);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals(expected, actual);
    }
}
