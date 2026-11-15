package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Unit tests for {@link CharArrayConversions}.
 *
 * The tests are written against the package‑private class, therefore the test class
 * resides in the same package as the class under test.
 */
public class CharArrayConversionsTest {

    /** Helper to create a Converter with default options (UTF‑8 charset). */
    private static Converter createDefaultConverter() {
        // ConverterOptions is assumed to have a public no‑arg constructor that
        // defaults to UTF‑8 charset. This matches the behaviour of the library.
        ConverterOptions options = new ConverterOptions();
        return new Converter(options);
    }

    @Test
    public void testToString_WithNormalCharacters() {
        char[] source = new char[] { 'J', 'a', 'v', 'a' };
        Converter converter = createDefaultConverter();

        String result = CharArrayConversions.toString(source, converter);
        Assert.assertEquals("Java", result);
    }

    @Test
    public void testToString_WithEmptyArray() {
        char[] source = new char[0];
        Converter converter = createDefaultConverter();

        String result = CharArrayConversions.toString(source, converter);
        Assert.assertEquals("", result);
    }

    @Test
    public void testToCharBuffer_ContentAndPosition() {
        char[] source = new char[] { 't', 'e', 's', 't' };
        Converter converter = createDefaultConverter();

        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, converter);
        // The buffer should be backed by the original array (wrap) – its remaining should equal length.
        Assert.assertEquals(source.length, buffer.remaining());

        // Verify each character.
        for (int i = 0; i < source.length; i++) {
            Assert.assertEquals(source[i], buffer.get(i));
        }
    }

    @Test
    public void testToCharBuffer_WithEmptyArray() {
        char[] source = new char[0];
        Converter converter = createDefaultConverter();

        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, converter);
        Assert.assertEquals(0, buffer.remaining());
    }

    @Test
    public void testToStringBuffer() {
        char[] source = new char[] { 'A', 'B', 'C' };
        Converter converter = createDefaultConverter();

        StringBuffer sb = CharArrayConversions.toStringBuffer(source, converter);
        Assert.assertEquals("ABC", sb.toString());
    }

    @Test
    public void testToStringBuilder() {
        char[] source = new char[] { 'x', 'y', 'z' };
        Converter converter = createDefaultConverter();

        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        Assert.assertEquals("xyz", sb.toString());
    }

    @Test
    public void testToByteBuffer_EncodingMatchesCharset() {
        char[] source = new char[] { 'é', 'ñ', '漢' }; // characters that produce multi‑byte sequences in UTF‑8
        Converter converter = createDefaultConverter();

        ByteBuffer byteBuffer = CharArrayConversions.toByteBuffer(source, converter);
        Charset charset = converter.getOptions().getCharset();

        // Expected bytes using the same charset
        byte[] expected = new String(source).getBytes(charset);
        byte[] actual = new byte[byteBuffer.remaining()];
        byteBuffer.get(actual);

        Assert.assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteBuffer_WithEmptyArray() {
        char[] source = new char[0];
        Converter converter = createDefaultConverter();

        ByteBuffer byteBuffer = CharArrayConversions.toByteBuffer(source, converter);
        Assert.assertEquals(0, byteBuffer.remaining());
    }

    @Test
    public void testToByteArray_MatchesByteBuffer() {
        char[] source = new char[] { 'J', 'U', 'N', 'I', 'T' };
        Converter converter = createDefaultConverter();

        byte[] byteArray = CharArrayConversions.toByteArray(source, converter);
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converter);
        byte[] bufferBytes = new byte[buffer.remaining()];
        buffer.get(bufferBytes);

        Assert.assertArrayEquals(bufferBytes, byteArray);
    }

    @Test
    public void testToByteArray_WithEmptyArray() {
        char[] source = new char[0];
        Converter converter = createDefaultConverter();

        byte[] byteArray = CharArrayConversions.toByteArray(source, converter);
        Assert.assertEquals(0, byteArray.length);
    }

    @Test
    public void testToCharArray_ReturnsCopyNotReference() {
        char[] original = new char[] { 'c', 'o', 'p', 'y' };
        Converter converter = createDefaultConverter();

        char[] copy = CharArrayConversions.toCharArray(original, converter);
        // Verify content equality
        Assert.assertArrayEquals(original, copy);
        // Verify that it is a different object
        Assert.assertNotSame(original, copy);

        // Mutate the original and ensure the copy does not change
        original[0] = 'X';
        Assert.assertEquals('c', copy[0]);
    }

    @Test
    public void testToCharArray_WithEmptyArray() {
        char[] original = new char[0];
        Converter converter = createDefaultConverter();

        char[] copy = CharArrayConversions.toCharArray(original, converter);
        Assert.assertArrayEquals(original, copy);
        Assert.assertNotSame(original, copy);
    }

    @Test
    public void testAllConversionsTogether_ConsistencyCheck() {
        char[] source = new char[] { 'C', 'h', 'a', 'r', 's' };
        Converter converter = createDefaultConverter();

        // String
        String asString = CharArrayConversions.toString(source, converter);
        Assert.assertEquals("Chars", asString);

        // CharBuffer
        CharBuffer charBuffer = CharArrayConversions.toCharBuffer(source, converter);
        Assert.assertEquals(asString, charBuffer.toString());

        // StringBuffer
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, converter);
        Assert.assertEquals(asString, sb.toString());

        // StringBuilder
        StringBuilder sbuilder = CharArrayConversions.toStringBuilder(source, converter);
        Assert.assertEquals(asString, sbuilder.toString());

        // ByteBuffer / byte[]
        ByteBuffer byteBuf = CharArrayConversions.toByteBuffer(source, converter);
        byte[] bytesFromMethod = CharArrayConversions.toByteArray(source, converter);
        byte[] bytesDirect = new byte[byteBuf.remaining()];
        byteBuf.get(bytesDirect);
        Assert.assertArrayEquals(bytesDirect, bytesFromMethod);
        // Verify that the bytes decode back to the original string using the same charset
        Charset cs = converter.getOptions().getCharset();
        String decoded = new String(bytesFromMethod, cs);
        Assert.assertEquals(asString, decoded);
    }
}
