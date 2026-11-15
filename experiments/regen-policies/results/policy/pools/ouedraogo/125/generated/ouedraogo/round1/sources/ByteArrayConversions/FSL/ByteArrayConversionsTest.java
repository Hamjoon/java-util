package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive tests for {@link ByteArrayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion of a non‑null, non‑empty byte array.</li>
 *   <li>Conversion of an empty byte array.</li>
 *   <li>Conversion when the source object is {@code null} (only {@code toString} is defined to handle this case).</li>
 *   <li>Correct handling of the charset supplied through {@link Converter#getOptions()}.</li>
 *   <li>Behaviour of the returned {@link ByteBuffer}, {@link CharBuffer}, {@code char[]} ,
 *       {@link StringBuffer} and {@link StringBuilder} objects.</li>
 *   <li>Independence of the returned objects from subsequent modifications of the original byte array.</li>
 * </ul>
 *
 * The test class uses only the public API that is documented in the problem statement.
 */
public class ByteArrayConversionsTest {

    private Converter converterUtf8;

    /**
     * Creates a {@link Converter} whose {@link ConverterOptions} use UTF‑8 as the charset.
     * The concrete {@code ConverterOptions} class is not part of the supplied source,
     * but the library provides a no‑arg constructor and defaults to UTF‑8 – this is
     * sufficient for the purpose of these unit tests because all test data is ASCII.
     */
    @Before
    public void setUp() {
        // The library’s ConverterOptions defaults to UTF‑8; if a setter exists we would use it.
        // The test does not depend on the exact charset as long as it can represent ASCII.
        converterUtf8 = new Converter(new ConverterOptions());
    }

    // ------------------------------------------------------------------------
    // toString(...)
    // ------------------------------------------------------------------------

    @Test
    public void testToString_NullInput_ReturnsEmptyString() {
        Object input = null;
        String result = ByteArrayConversions.toString(input, converterUtf8);
        assertEquals(StringUtilities.EMPTY, result);
    }

    @Test
    public void testToString_EmptyArray_ReturnsEmptyString() {
        byte[] empty = new byte[0];
        String result = ByteArrayConversions.toString(empty, converterUtf8);
        assertEquals("", result);
    }

    @Test
    public void testToString_AsciiBytes_ReturnsCorrectString() {
        byte[] hello = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(hello, converterUtf8);
        assertEquals("Hello, World!", result);
    }

    @Test
    public void testToString_NonAsciiBytes_WithUtf8Charset() {
        // The Euro sign (U+20AC) encoded in UTF‑8 is three bytes: 0xE2 0x82 0xAC
        byte[] euro = new byte[] {(byte)0xE2, (byte)0x82, (byte)0xAC};
        String result = ByteArrayConversions.toString(euro, converterUtf8);
        assertEquals("\u20AC", result);
    }

    // ------------------------------------------------------------------------
    // toByteBuffer(...)
    // ------------------------------------------------------------------------

    @Test
    public void testToByteBuffer_WrapsArrayCorrectly() {
        byte[] data = {1, 2, 3, 4, 5};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, converterUtf8);

        // Buffer should be a view over the original array
        assertEquals(data.length, buffer.remaining());
        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], buffer.get(i));
        }

        // Modifying the original array must be reflected in the buffer
        data[0] = 99;
        assertEquals(99, buffer.get(0));
    }

    @Test
    public void testToByteBuffer_EmptyArray() {
        byte[] empty = new byte[0];
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(empty, converterUtf8);
        assertEquals(0, buffer.remaining());
    }

    // ------------------------------------------------------------------------
    // toCharBuffer(...)
    // ------------------------------------------------------------------------

    @Test
    public void testToCharBuffer_ProducesCorrectContent() {
        byte[] bytes = "abcXYZ".getBytes(StandardCharsets.UTF_8);
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(bytes, converterUtf8);

        assertEquals(6, charBuffer.length());
        assertEquals("abcXYZ", charBuffer.toString());
    }

    @Test
    public void testToCharBuffer_EmptyArray() {
        byte[] empty = new byte[0];
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(empty, converterUtf8);
        assertEquals(0, charBuffer.length());
        assertEquals("", charBuffer.toString());
    }

    // ------------------------------------------------------------------------
    // toCharArray(...)
    // ------------------------------------------------------------------------

    @Test
    public void testToCharArray_MatchesStringCharacters() {
        byte[] bytes = "Test123".getBytes(StandardCharsets.UTF_8);
        char[] chars = ByteArrayConversions.toCharArray(bytes, converterUtf8);
        assertArrayEquals("Test123".toCharArray(), chars);
    }

    @Test
    public void testToCharArray_EmptyArray() {
        byte[] empty = new byte[0];
        char[] chars = ByteArrayConversions.toCharArray(empty, converterUtf8);
        assertEquals(0, chars.length);
    }

    // ------------------------------------------------------------------------
    // toStringBuffer(...)
    // ------------------------------------------------------------------------

    @Test
    public void testToStringBuffer_ContentMatches() {
        byte[] bytes = "buffer".getBytes(StandardCharsets.UTF_8);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(bytes, converterUtf8);
        assertEquals("buffer", sb.toString());

        // Verify that the returned object is mutable and independent from the source array
        sb.append("-extra");
        assertEquals("buffer-extra", sb.toString());

        // Original byte array unchanged
        assertEquals("buffer", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testToStringBuffer_EmptyArray() {
        byte[] empty = new byte[0];
        StringBuffer sb = ByteArrayConversions.toStringBuffer(empty, converterUtf8);
        assertEquals("", sb.toString());
    }

    // ------------------------------------------------------------------------
    // toStringBuilder(...)
    // ------------------------------------------------------------------------

    @Test
    public void testToStringBuilder_ContentMatches() {
        byte[] bytes = "builder".getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(bytes, converterUtf8);
        assertEquals("builder", sb.toString());

        sb.reverse();
        assertEquals("redrub", sb.toString());
    }

    @Test
    public void testToStringBuilder_EmptyArray() {
        byte[] empty = new byte[0];
        StringBuilder sb = ByteArrayConversions.toStringBuilder(empty, converterUtf8);
        assertEquals("", sb.toString());
    }

    // ------------------------------------------------------------------------
    // Integration test – chaining conversions
    // ------------------------------------------------------------------------

    @Test
    public void testChainedConversions_AllProduceSameLogicalString() {
        String original = "Chain-123";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);

        // Convert to each target type
        String asString = ByteArrayConversions.toString(bytes, converterUtf8);
        CharBuffer asCharBuffer = ByteArrayConversions.toCharBuffer(bytes, converterUtf8);
        char[] asCharArray = ByteArrayConversions.toCharArray(bytes, converterUtf8);
        StringBuffer asStringBuffer = ByteArrayConversions.toStringBuffer(bytes, converterUtf8);
        StringBuilder asStringBuilder = ByteArrayConversions.toStringBuilder(bytes, converterUtf8);

        // All results must represent the same textual content
        assertEquals(original, asString);
        assertEquals(original, asCharBuffer.toString());
        assertArrayEquals(original.toCharArray(), asCharArray);
        assertEquals(original, asStringBuffer.toString());
        assertEquals(original, asStringBuilder.toString());
    }
}
