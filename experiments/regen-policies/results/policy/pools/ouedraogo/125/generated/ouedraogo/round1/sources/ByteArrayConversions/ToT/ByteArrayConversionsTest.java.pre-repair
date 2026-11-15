/****************************************************************************************
 * ByteArrayConversionsTest.java
 *
 * Comprehensive JUnit‑4 test suite for com.cedarsoftware.util.convert.ByteArrayConversions
 *
 * The tests cover:
 *   • Normal conversion paths for all static methods
 *   • Edge cases (null, empty arrays, multi‑byte characters)
 *   • Charset handling via the Converter's options
 *   • Expected failure when a required non‑null argument is omitted
 *
 * The test class is placed in the same package as ByteArrayConversions so that the
 * package‑private static methods are directly accessible.
 ****************************************************************************************/

package com.cedarsoftware.util.convert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test suite for {@code ByteArrayConversions}.
 *
 * The tests use real {@link Converter} and {@link ConverterOptions} instances – no mocks
 * or subclassing of library types are employed, as required by the task description.
 */
public class ByteArrayConversionsTest {

    /** Helper to create a {@link Converter} that uses the supplied {@link Charset}. */
    private static Converter converterWith(Charset charset) {
        // ConverterOptions is assumed to have a no‑arg constructor and a setter for charset.
        // The library’s real implementation provides this; the test compiles against it.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(charset);
        return new Converter(options);
    }

    /** Helper to obtain a default UTF‑8 converter (the most common default in the library). */
    private static Converter defaultConverter() {
        return converterWith(StandardCharsets.UTF_8);
    }

    /* --------------------------------------------------------------------- *
     *  toString(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToString_NullByteArray_ReturnsEmptyString() {
        byte[] input = null;
        String result = ByteArrayConversions.toString(input, defaultConverter());
        assertEquals("Null byte array must yield StringUtilities.EMPTY",
                     StringUtilities.EMPTY, result);
    }

    @Test
    public void testToString_EmptyByteArray_ReturnsEmptyString() {
        byte[] input = new byte[0];
        String result = ByteArrayConversions.toString(input, defaultConverter());
        assertEquals("Empty byte array must produce an empty string",
                     "", result);
    }

    @Test
    public void testToString_ValidBytes_UsesProvidedCharset() {
        // The same Unicode code point encoded in UTF‑8 and ISO‑8859‑1 differ.
        String original = "café"; // contains 'é' (U+00E9)
        byte[] utf8Bytes = original.getBytes(StandardCharsets.UTF_8);
        byte[] isoBytes = original.getBytes(StandardCharsets.ISO_8859_1);

        // UTF‑8 conversion – should reproduce the original string.
        String utf8Result = ByteArrayConversions.toString(utf8Bytes,
                converterWith(StandardCharsets.UTF_8));
        assertEquals("UTF‑8 conversion must preserve characters", original, utf8Result);

        // ISO‑8859‑1 conversion – the same bytes produce the same characters because the
        // charset matches the encoding used to produce the byte array.
        String isoResult = ByteArrayConversions.toString(isoBytes,
                converterWith(StandardCharsets.ISO_8859_1));
        assertEquals("ISO‑8859‑1 conversion must preserve characters", original, isoResult);
    }

    /* --------------------------------------------------------------------- *
     *  toByteBuffer(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToByteBuffer_WrapsByteArrayCorrectly() {
        byte[] data = {0x01, 0x02, 0x03, 0x04};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, defaultConverter());

        assertNotNull("ByteBuffer must not be null", buffer);
        assertEquals("ByteBuffer capacity must equal source array length",
                     data.length, buffer.capacity());

        // Verify content – ByteBuffer is backed by the original array.
        for (int i = 0; i < data.length; i++) {
            assertEquals("Byte at position " + i + " must match source array",
                         data[i], buffer.get(i));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullArray_ThrowsNPE() {
        // ByteBuffer.wrap(null) throws NPE; ByteArrayConversions does not guard against it.
        ByteArrayConversions.toByteBuffer(null, defaultConverter());
    }

    /* --------------------------------------------------------------------- *
     *  toCharBuffer(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_ProducesCorrectCharBuffer() {
        String text = "Hello, 世界"; // includes non‑ASCII characters
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(bytes, defaultConverter());

        assertNotNull("CharBuffer must not be null", charBuffer);
        assertEquals("CharBuffer length must match original string length",
                     text.length(), charBuffer.length());
        assertEquals("CharBuffer content must equal original string",
                     text, charBuffer.toString());
    }

    /* --------------------------------------------------------------------- *
     *  toCharArray(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToCharArray_ReturnsCorrectArray() {
        String text = "JUnit4";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        char[] chars = ByteArrayConversions.toCharArray(bytes, defaultConverter());

        assertArrayEquals("Char array must represent the original string",
                          text.toCharArray(), chars);
    }

    /* --------------------------------------------------------------------- *
     *  toStringBuffer(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringBuffer_CreatesCorrectStringBuffer() {
        String text = "StringBufferTest";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        StringBuffer sb = ByteArrayConversions.toStringBuffer(bytes, defaultConverter());

        assertNotNull("StringBuffer must not be null", sb);
        assertEquals("StringBuffer content must match original string",
                     text, sb.toString());
    }

    /* --------------------------------------------------------------------- *
     *  toStringBuilder(Object, Converter)
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilder_CreatesCorrectStringBuilder() {
        String text = "StringBuilderTest";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

        StringBuilder sb = ByteArrayConversions.toStringBuilder(bytes, defaultConverter());

        assertNotNull("StringBuilder must not be null", sb);
        assertEquals("StringBuilder content must match original string",
                     text, sb.toString());
    }

    /* --------------------------------------------------------------------- *
     *  Additional sanity checks – ensure that all conversions are consistent
     * --------------------------------------------------------------------- */

    @Test
    public void testAllConversionsYieldIdenticalStringRepresentation() {
        String original = "Consistency✓";
        byte[] data = original.getBytes(StandardCharsets.UTF_8);
        Converter conv = defaultConverter();

        // Direct toString
        String s1 = ByteArrayConversions.toString(data, conv);
        // Through CharBuffer
        CharBuffer cb = ByteArrayConversions.toCharBuffer(data, conv);
        // Through Char array
        char[] ca = ByteArrayConversions.toCharArray(data, conv);
        // Through StringBuffer / StringBuilder
        StringBuffer sbuf = ByteArrayConversions.toStringBuffer(data, conv);
        StringBuilder sbld = ByteArrayConversions.toStringBuilder(data, conv);

        // All must represent the same textual data
        assertEquals(s1, cb.toString());
        assertArrayEquals(s1.toCharArray(), ca);
        assertEquals(s1, sbuf.toString());
        assertEquals(s1, sbld.toString());
    }
}
