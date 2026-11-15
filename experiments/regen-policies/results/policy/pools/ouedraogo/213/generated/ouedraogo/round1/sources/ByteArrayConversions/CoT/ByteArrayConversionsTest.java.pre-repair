package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * JUnit 4 test cases for {@link ByteArrayConversions}.
 *
 * The tests are placed in the same package as the class under test so that they can
 * access the package‑private static methods.
 */
public class ByteArrayConversionsTest {

    /** Helper to obtain a default {@link Converter} instance. */
    private static Converter defaultConverter() {
        // The library provides a ConverterOptions class (not shown) that supplies a Charset.
        // Using the no‑arg constructor of ConverterOptions gives the default options (UTF‑8).
        return new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /*  List of public (actually package‑private static) methods in the class   */
    /* ---------------------------------------------------------------------- */
    // static String toString(Object from, Converter converter)
    // static ByteBuffer toByteBuffer(Object from, Converter converter)
    // static CharBuffer toCharBuffer(Object from, Converter converter)
    // static char[] toCharArray(Object from, Converter converter)
    // static StringBuffer toStringBuffer(Object from, Converter converter)
    // static StringBuilder toStringBuilder(Object from, Converter converter)

    /* ---------------------------------------------------------------------- */
    /*  Basic functionality tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString_WithValidBytes() {
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(data, defaultConverter());
        Assert.assertEquals("Hello World", result);
    }

    @Test
    public void testToString_WithEmptyArray() {
        byte[] data = new byte[0];
        String result = ByteArrayConversions.toString(data, defaultConverter());
        Assert.assertEquals("", result);
    }

    @Test
    public void testToString_WithNullArray() {
        String result = ByteArrayConversions.toString(null, defaultConverter());
        // StringUtilities.EMPTY is defined as the empty string ("")
        Assert.assertEquals("", result);
    }

    @Test
    public void testToByteBuffer_WithValidBytes() {
        byte[] data = {1, 2, 3, 4, 5};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, defaultConverter());

        Assert.assertNotNull(buffer);
        Assert.assertEquals(data.length, buffer.remaining());

        // Verify the content
        for (byte b : data) {
            Assert.assertEquals(b, buffer.get());
        }
        // After reading all bytes the buffer should be exhausted
        Assert.assertEquals(0, buffer.remaining());
    }

    @Test
    public void testToCharBuffer_WithValidBytes() {
        byte[] data = "ABC".getBytes(StandardCharsets.UTF_8);
        CharBuffer charBuf = ByteArrayConversions.toCharBuffer(data, defaultConverter());

        Assert.assertNotNull(charBuf);
        Assert.assertEquals("ABC", charBuf.toString());
    }

    @Test
    public void testToCharArray_WithValidBytes() {
        byte[] data = "xyz".getBytes(StandardCharsets.UTF_8);
        char[] chars = ByteArrayConversions.toCharArray(data, defaultConverter());

        Assert.assertArrayEquals(new char[] {'x', 'y', 'z'}, chars);
    }

    @Test
    public void testToStringBuffer_WithValidBytes() {
        byte[] data = "buffer".getBytes(StandardCharsets.UTF_8);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(data, defaultConverter());

        Assert.assertNotNull(sb);
        Assert.assertEquals("buffer", sb.toString());
    }

    @Test
    public void testToStringBuilder_WithValidBytes() {
        byte[] data = "builder".getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(data, defaultConverter());

        Assert.assertNotNull(sb);
        Assert.assertEquals("builder", sb.toString());
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case and exception handling tests                               */
    /* ---------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToString_WithWrongObjectType() {
        // Passing a String instead of a byte[] should cause a ClassCastException
        ByteArrayConversions.toString("not a byte array", defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBuffer_WithWrongObjectType() {
        ByteArrayConversions.toByteBuffer("not a byte array", defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_WithNullArray() {
        // ByteBuffer.wrap(null) throws NullPointerException
        ByteArrayConversions.toByteBuffer(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_WithWrongObjectType() {
        ByteArrayConversions.toCharBuffer(12345, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_WithWrongObjectType() {
        ByteArrayConversions.toCharArray(new Object(), defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_WithWrongObjectType() {
        ByteArrayConversions.toStringBuffer(new Object(), defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_WithWrongObjectType() {
        ByteArrayConversions.toStringBuilder(new Object(), defaultConverter());
    }

    @Test
    public void testToString_UsesConverterCharset() {
        // Verify that the charset supplied by the Converter is honoured.
        // We'll construct a string containing a non‑ASCII character.
        String original = "café"; // contains 'é' (0xE9 in UTF‑8)
        byte[] utf8Bytes = original.getBytes(StandardCharsets.UTF_8);
        byte[] isoBytes = original.getBytes(Charset.forName("ISO-8859-1"));

        // Converter with UTF‑8 charset (default)
        Converter utf8Converter = defaultConverter();
        String resultUtf8 = ByteArrayConversions.toString(utf8Bytes, utf8Converter);
        Assert.assertEquals(original, resultUtf8);

        // Converter with ISO‑8859‑1 charset
        ConverterOptions isoOptions = new ConverterOptions();
        isoOptions.setCharset(Charset.forName("ISO-8859-1"));
        Converter isoConverter = new Converter(isoOptions);
        String resultIso = ByteArrayConversions.toString(isoBytes, isoConverter);
        Assert.assertEquals(original, resultIso);
    }
}
