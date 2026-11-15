package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@code ByteArrayConversions}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Normal behaviour for all conversion methods.</li>
 *   <li>Edge cases such as {@code null} input, empty arrays and
 *       non‑byte inputs that provoke {@code ClassCastException}.</li>
 *   <li>Exception handling – verification that the expected
 *       {@code NullPointerException} or {@code ClassCastException}
 *       is thrown when illegal arguments are supplied.</li>
 *   <li>Immutability / copy semantics where applicable (e.g. the
 *       {@code char[]} returned by {@code toCharArray}).</li>
 * </ul>
 * </p>
 *
 * The test class resides in the same package as {@code ByteArrayConversions}
 * so it can access the package‑private static methods.
 */
public class ByteArrayConversionsTest {

    /** A reusable {@code Converter} configured to use UTF‑8. */
    private Converter converter;

    @Before
    public void setUp() {
        // The real library provides a mutable ConverterOptions class.
        // We create it, set the charset to UTF‑8 and pass it to the Converter.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(StandardCharsets.UTF_8);
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- */
    /*                         toString(...) tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToString_NullInput() {
        Object from = null;
        String result = ByteArrayConversions.toString(from, converter);
        // StringUtilities.EMPTY is defined as the empty string ("").
        assertEquals("", result);
    }

    @Test
    public void testToString_EmptyArray() {
        byte[] from = new byte[0];
        String result = ByteArrayConversions.toString(from, converter);
        assertEquals("", result);
    }

    @Test
    public void testToString_NormalArray() {
        byte[] from = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(from, converter);
        assertEquals("Hello, world!", result);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongInputType() {
        // Passing a String instead of a byte[] forces a ClassCastException.
        ByteArrayConversions.toString("not a byte array", converter);
    }

    /* --------------------------------------------------------------------- */
    /*                      toByteBuffer(...) tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteBuffer_NullInput() {
        try {
            ByteArrayConversions.toByteBuffer(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testToByteBuffer_EmptyArray() {
        byte[] from = new byte[0];
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(from, converter);
        assertNotNull(buffer);
        assertEquals(0, buffer.remaining());
    }

    @Test
    public void testToByteBuffer_NormalArray() {
        byte[] from = {1, 2, 3, 4, 5};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(from, converter);
        assertNotNull(buffer);
        assertEquals(from.length, buffer.remaining());

        // Verify that the buffer contains the same bytes in the same order.
        for (byte b : from) {
            assertEquals(b, buffer.get());
        }
        // After reading all bytes the buffer should be exhausted.
        assertEquals(0, buffer.remaining());
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBuffer_WrongInputType() {
        ByteArrayConversions.toByteBuffer("bad type", converter);
    }

    /* --------------------------------------------------------------------- */
    /*                      toCharBuffer(...) tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_NullInput() {
        CharBuffer cb = ByteArrayConversions.toCharBuffer(null, converter);
        assertEquals(0, cb.length());
        assertEquals("", cb.toString());
    }

    @Test
    public void testToCharBuffer_EmptyArray() {
        CharBuffer cb = ByteArrayConversions.toCharBuffer(new byte[0], converter);
        assertEquals(0, cb.length());
        assertEquals("", cb.toString());
    }

    @Test
    public void testToCharBuffer_NormalArray() {
        String original = "ABCxyz";
        CharBuffer cb = ByteArrayConversions.toCharBuffer(original.getBytes(StandardCharsets.UTF_8), converter);
        assertEquals(original.length(), cb.length());
        assertEquals(original, cb.toString());
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_WrongInputType() {
        ByteArrayConversions.toCharBuffer(12345, converter);
    }

    /* --------------------------------------------------------------------- */
    /*                      toCharArray(...) tests                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharArray_NullInput() {
        char[] chars = ByteArrayConversions.toCharArray(null, converter);
        assertEquals(0, chars.length);
    }

    @Test
    public void testToCharArray_EmptyArray() {
        char[] chars = ByteArrayConversions.toCharArray(new byte[0], converter);
        assertEquals(0, chars.length);
    }

    @Test
    public void testToCharArray_NormalArray() {
        String text = "Sample text";
        char[] chars = ByteArrayConversions.toCharArray(text.getBytes(StandardCharsets.UTF_8), converter);
        assertArrayEquals(text.toCharArray(), chars);
    }

    @Test
    public void testToCharArray_Independence() {
        // Ensure the returned char[] is a copy – changes to it do not affect the source bytes.
        byte[] source = "immutable".getBytes(StandardCharsets.UTF_8);
        char[] chars = ByteArrayConversions.toCharArray(source, converter);
        chars[0] = 'X';
        assertEquals('i', (char) source[0]); // source remains unchanged
        assertEquals('X', chars[0]);        // but the returned array reflects the modification
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_WrongInputType() {
        ByteArrayConversions.toCharArray(new Object(), converter);
    }

    /* --------------------------------------------------------------------- */
    /*                    toStringBuffer(...) tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBuffer_NullInput() {
        StringBuffer sb = ByteArrayConversions.toStringBuffer(null, converter);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuffer_EmptyArray() {
        StringBuffer sb = ByteArrayConversions.toStringBuffer(new byte[0], converter);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuffer_NormalArray() {
        String text = "Buffer content";
        StringBuffer sb = ByteArrayConversions.toStringBuffer(text.getBytes(StandardCharsets.UTF_8), converter);
        assertEquals(text, sb.toString());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_WrongInputType() {
        ByteArrayConversions.toStringBuffer(Boolean.TRUE, converter);
    }

    /* --------------------------------------------------------------------- */
    /*                    toStringBuilder(...) tests                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilder_NullInput() {
        StringBuilder sb = ByteArrayConversions.toStringBuilder(null, converter);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuilder_EmptyArray() {
        StringBuilder sb = ByteArrayConversions.toStringBuilder(new byte[0], converter);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuilder_NormalArray() {
        String text = "Builder test";
        StringBuilder sb = ByteArrayConversions.toStringBuilder(text.getBytes(StandardCharsets.UTF_8), converter);
        assertEquals(text, sb.toString());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_WrongInputType() {
        ByteArrayConversions.toStringBuilder(3.14, converter);
    }
}
