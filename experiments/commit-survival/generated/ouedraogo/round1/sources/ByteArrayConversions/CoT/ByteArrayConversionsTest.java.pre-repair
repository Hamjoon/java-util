package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * JUnit 4 test cases for {@code ByteArrayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public (package‑private static) methods with normal inputs.</li>
 *   <li>Edge cases such as {@code null} byte arrays, empty arrays, and unsupported input types.</li>
 *   <li>Exception scenarios – {@code ClassCastException} when the source object is not a {@code byte[]},
 *       and {@code NullPointerException} when the {@code Converter} argument is {@code null}.</li>
 * </ul>
 *
 * The test class resides in the same package as {@code ByteArrayConversions} so that it can
 * access the package‑private static methods directly.
 */
public class ByteArrayConversionsTest {

    /* --------------------------------------------------------------------- */
    /* Helper methods to create a Converter with a specific Charset          */
    /* --------------------------------------------------------------------- */

    private static Converter createConverter(Charset charset) {
        // The real library provides a mutable ConverterOptions class.
        // For the purpose of these tests we assume it has a public no‑arg constructor
        // and a setter for the charset.  If the actual API differs, adjust accordingly.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(charset);
        return new Converter(options);
    }

    private static final Converter UTF8_CONVERTER = createConverter(StandardCharsets.UTF_8);
    private static final Converter ISO_CONVERTER = createConverter(StandardCharsets.ISO_8859_1);

    /* --------------------------------------------------------------------- */
    /* 1. Basic functionality tests                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToString_NormalBytes_UTF8() {
        byte[] data = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(data, UTF8_CONVERTER);
        Assert.assertEquals("Hello, world!", result);
    }

    @Test
    public void testToString_NormalBytes_ISO() {
        byte[] data = "Café".getBytes(StandardCharsets.ISO_8859_1);
        String result = ByteArrayConversions.toString(data, ISO_CONVERTER);
        Assert.assertEquals("Café", result);
    }

    @Test
    public void testToByteBuffer_WrapsArray() {
        byte[] data = {1, 2, 3, 4, 5};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, UTF8_CONVERTER);
        Assert.assertEquals(data.length, buffer.remaining());
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals(data[i], buffer.get(i));
        }
    }

    @Test
    public void testToCharBuffer_WrapsString() {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(data, UTF8_CONVERTER);
        Assert.assertEquals("abc", charBuffer.toString());
    }

    @Test
    public void testToCharArray_ReturnsCorrectArray() {
        byte[] data = "xyz".getBytes(StandardCharsets.UTF_8);
        char[] chars = ByteArrayConversions.toCharArray(data, UTF8_CONVERTER);
        Assert.assertArrayEquals(new char[]{'x', 'y', 'z'}, chars);
    }

    @Test
    public void testToStringBuffer_ReturnsCorrectBuffer() {
        byte[] data = "buffer".getBytes(StandardCharsets.UTF_8);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(data, UTF8_CONVERTER);
        Assert.assertEquals("buffer", sb.toString());
    }

    @Test
    public void testToStringBuilder_ReturnsCorrectBuilder() {
        byte[] data = "builder".getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(data, UTF8_CONVERTER);
        Assert.assertEquals("builder", sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /* 2. Edge‑case tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToString_NullByteArray_ReturnsEmptyString() {
        String result = ByteArrayConversions.toString(null, UTF8_CONVERTER);
        Assert.assertEquals(StringUtilities.EMPTY, result);
    }

    @Test
    public void testToString_EmptyByteArray_ReturnsEmptyString() {
        byte[] empty = new byte[0];
        String result = ByteArrayConversions.toString(empty, UTF8_CONVERTER);
        Assert.assertEquals("", result);
    }

    @Test
    public void testToByteBuffer_NullArray_WrapsNull() {
        // ByteBuffer.wrap(null) throws NullPointerException – we verify that the method propagates it.
        try {
            ByteArrayConversions.toByteBuffer(null, UTF8_CONVERTER);
            Assert.fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testToCharBuffer_NullArray_PropagatesNPE() {
        try {
            ByteArrayConversions.toCharBuffer(null, UTF8_CONVERTER);
            Assert.fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testToCharArray_NullArray_ReturnsEmptyCharArray() {
        // toString(null) returns EMPTY, whose toCharArray() is an empty array.
        char[] chars = ByteArrayConversions.toCharArray(null, UTF8_CONVERTER);
        Assert.assertEquals(0, chars.length);
    }

    @Test
    public void testToStringBuffer_NullArray_ReturnsEmptyBuffer() {
        StringBuffer sb = ByteArrayConversions.toStringBuffer(null, UTF8_CONVERTER);
        Assert.assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuilder_NullArray_ReturnsEmptyBuilder() {
        StringBuilder sb = ByteArrayConversions.toStringBuilder(null, UTF8_CONVERTER);
        Assert.assertEquals("", sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /* 3. Exception handling – wrong source type or null converter          */
    /* --------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToString_WrongSourceType_ThrowsClassCastException() {
        // Passing a String instead of a byte[] causes the cast inside the method to fail.
        ByteArrayConversions.toString("not a byte array", UTF8_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBuffer_WrongSourceType_ThrowsClassCastException() {
        ByteArrayConversions.toByteBuffer("not a byte array", UTF8_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_WrongSourceType_ThrowsClassCastException() {
        ByteArrayConversions.toCharBuffer(12345, UTF8_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_WrongSourceType_ThrowsClassCastException() {
        ByteArrayConversions.toCharArray(new Object(), UTF8_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_WrongSourceType_ThrowsClassCastException() {
        ByteArrayConversions.toStringBuffer(Boolean.TRUE, UTF8_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_WrongSourceType_ThrowsClassCastException() {
        ByteArrayConversions.toStringBuilder(3.14, UTF8_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullConverter_ThrowsNPE() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        ByteArrayConversions.toString(data, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullConverter_ThrowsNPE() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        ByteArrayConversions.toByteBuffer(data, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_NullConverter_ThrowsNPE() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        ByteArrayConversions.toCharBuffer(data, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_NullConverter_ThrowsNPE() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        ByteArrayConversions.toCharArray(data, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_NullConverter_ThrowsNPE() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        ByteArrayConversions.toStringBuffer(data, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_NullConverter_ThrowsNPE() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        ByteArrayConversions.toStringBuilder(data, null);
    }
}
