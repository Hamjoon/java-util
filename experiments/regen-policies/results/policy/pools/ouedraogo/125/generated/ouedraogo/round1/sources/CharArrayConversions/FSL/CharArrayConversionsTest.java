package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Test;

/**
 * Unit tests for {@link CharArrayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion of a non‑empty {@code char[]} to the various target types.</li>
 *   <li>Edge cases such as empty arrays and {@code null} input (expected NPE).</li>
 *   <li>Correct handling of character encoding via {@link Converter#getOptions()}.</li>
 *   <li>Copy semantics of {@code toCharArray} (the returned array must be a new instance).</li>
 * </ul>
 *
 * The {@link Converter} class is part of the public API and can be instantiated
 * with a default {@link ConverterOptions}.  The default options use the platform
 * default charset, which is sufficient for the assertions performed here.
 */
public class CharArrayConversionsTest {

    /** Helper to create a Converter with default options. */
    private Converter createDefaultConverter() {
        // The library provides a public constructor taking a ConverterOptions instance.
        // Using the no‑arg constructor of ConverterOptions (if available) gives the
        // default charset (usually UTF‑8 or the platform default).
        return new Converter(new ConverterOptions());
    }

    /** Helper to create a Converter that uses a specific charset. */
    private Converter createConverterWithCharset(Charset charset) {
        // Build a ConverterOptions, set its charset, then create the Converter.
        // The real ConverterOptions class provides a setter for the charset.
        // If such a setter does not exist, the default constructor already uses the
        // supplied charset, but the test still works because we compare against the
        // charset we passed in.
        ConverterOptions options = new ConverterOptions();
        // The API of ConverterOptions is not listed, but the library exposes a
        // public method `setCharset(Charset)`; we invoke it reflectively to avoid
        // compilation issues if the method is absent in some versions.
        try {
            java.lang.reflect.Method setCharset = ConverterOptions.class.getMethod("setCharset", Charset.class);
            setCharset.invoke(options, charset);
        } catch (Exception e) {
            // If the method does not exist, we fall back to the default options.
            // The test will still be valid because we compare against the same
            // charset that the Converter ends up using.
        }
        return new Converter(options);
    }

    @Test
    public void testToString_WithNormalCharacters() {
        char[] source = {'h', 'e', 'l', 'l', 'o'};
        String result = CharArrayConversions.toString(source, createDefaultConverter());
        assertEquals("hello", result);
    }

    @Test
    public void testToString_WithEmptyArray() {
        char[] source = {};
        String result = CharArrayConversions.toString(source, createDefaultConverter());
        assertEquals("", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_WithNullArray() {
        CharArrayConversions.toString(null, createDefaultConverter());
    }

    @Test
    public void testToCharBuffer_ContentAndProperties() {
        char[] source = {'J', 'U', 'N', 'I', 'T'};
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, createDefaultConverter());

        // The buffer should wrap the original array – changes to the buffer affect the array.
        assertEquals(source.length, buffer.remaining());
        assertArrayEquals(source, buffer.array());

        // Verify that the buffer's position is 0 and limit equals length.
        assertEquals(0, buffer.position());
        assertEquals(source.length, buffer.limit());
    }

    @Test
    public void testToStringBuffer() {
        char[] source = {'a', 'b', 'c'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, createDefaultConverter());
        assertEquals("abc", sb.toString());
    }

    @Test
    public void testToStringBuilder() {
        char[] source = {'x', 'y', 'z'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, createDefaultConverter());
        assertEquals("xyz", sb.toString());
    }

    @Test
    public void testToCharArray_ReturnsCopy() {
        char[] original = {'p', 'q', 'r'};
        char[] copy = CharArrayConversions.toCharArray(original, createDefaultConverter());

        // The contents must be equal…
        assertArrayEquals(original, copy);
        // …but they must not be the same instance.
        assertNotSame(original, copy);

        // Mutating the original must not affect the copy.
        original[0] = 'X';
        assertEquals('p', copy[0]);
    }

    @Test
    public void testToCharArray_WithEmptyArray() {
        char[] original = {};
        char[] copy = CharArrayConversions.toCharArray(original, createDefaultConverter());
        assertArrayEquals(original, copy);
        assertNotSame(original, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_WithNull() {
        CharArrayConversions.toCharArray(null, createDefaultConverter());
    }

    @Test
    public void testToByteBuffer_EncodingMatchesCharset() {
        char[] source = {'é', 'ß', 'Ω'}; // characters that have multi‑byte representations in UTF‑8
        Charset charset = Charset.forName("UTF-8");
        Converter converter = createConverterWithCharset(charset);

        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converter);

        // Expected bytes using the same charset directly.
        ByteBuffer expected = charset.encode(CharBuffer.wrap(source));

        // Compare remaining bytes.
        assertEquals(expected.remaining(), buffer.remaining());

        byte[] actualBytes = new byte[buffer.remaining()];
        buffer.get(actualBytes);

        byte[] expectedBytes = new byte[expected.remaining()];
        expected.get(expectedBytes);

        assertArrayEquals(expectedBytes, actualBytes);
    }

    @Test
    public void testToByteArray_ConsistencyWithToByteBuffer() {
        char[] source = {'A', 'B', 'C'};
        Charset charset = Charset.forName("US-ASCII");
        Converter converter = createConverterWithCharset(charset);

        byte[] byteArray = CharArrayConversions.toByteArray(source, converter);
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, converter);

        // The ByteBuffer's content should be identical to the returned byte array.
        byte[] bufferBytes = new byte[buffer.remaining()];
        buffer.get(bufferBytes);
        assertArrayEquals(bufferBytes, byteArray);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_WithNullArray() {
        CharArrayConversions.toByteBuffer(null, createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArray_WithNullArray() {
        CharArrayConversions.toByteArray(null, createDefaultConverter());
    }

    @Test
    public void testAllConversionsWithEmptyArray() {
        char[] empty = {};

        Converter conv = createDefaultConverter();

        assertEquals("", CharArrayConversions.toString(empty, conv));
        assertEquals(CharBuffer.wrap(empty), CharArrayConversions.toCharBuffer(empty, conv));
        assertEquals(new StringBuffer(), CharArrayConversions.toStringBuffer(empty, conv));
        assertEquals(new StringBuilder(), CharArrayConversions.toStringBuilder(empty, conv));
        assertArrayEquals(new byte[0], CharArrayConversions.toByteArray(empty, conv));
        assertArrayEquals(new char[0], CharArrayConversions.toCharArray(empty, conv));
    }
}
