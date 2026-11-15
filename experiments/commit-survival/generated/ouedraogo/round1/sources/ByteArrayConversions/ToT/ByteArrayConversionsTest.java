package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * JUnit 4 test suite for {@link ByteArrayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage with non‑null, non‑empty byte arrays.</li>
 *   <li>Edge cases such as {@code null} and empty byte arrays.</li>
 *   <li>Correct handling of character set encoding.</li>
 *   <li>Return types and content of the various conversion methods.</li>
 * </ul>
 *
 * No external mocking frameworks are used – real {@link Converter} and
 * {@link ConverterOptions} objects are created via their public constructors.
 */
public class ByteArrayConversionsTest {

    private Converter converterUtf8;
    private Converter converterIso;

    @Before
    public void setUp() {
        // The library provides a public constructor Converter(ConverterOptions).
        // ConverterOptions has a public no‑arg constructor (assumed) and a
        // setter for the charset.  Because the exact API of ConverterOptions
        // is not listed, we use reflection to set the charset field safely.
        ConverterOptions utf8Options = new ConverterOptions();
        setCharset(utf8Options, StandardCharsets.UTF_8);
        converterUtf8 = new Converter(utf8Options);

        ConverterOptions isoOptions = new ConverterOptions();
        setCharset(isoOptions, StandardCharsets.ISO_8859_1);
        converterIso = new Converter(isoOptions);
    }

    /**
     * Helper that uses reflection to set the private {@code charset} field of
     * {@link ConverterOptions}.  This is permissible because the field is part
     * of the public contract of the library (the tests must be able to control
     * the charset used by {@link ByteArrayConversions#toString}).
     */
    private void setCharset(ConverterOptions options, Charset charset) {
        try {
            java.lang.reflect.Field f = ConverterOptions.class.getDeclaredField("charset");
            f.setAccessible(true);
            f.set(options, charset);
        } catch (Exception e) {
            throw new RuntimeException("Unable to set charset on ConverterOptions", e);
        }
    }

    /* --------------------------------------------------------------------- */
    /* toString                                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToString_NullBytes_ReturnsEmptyString() {
        byte[] input = null;
        String result = ByteArrayConversions.toString(input, converterUtf8);
        assertSame("When input byte[] is null, the EMPTY constant must be returned",
                StringUtilities.EMPTY, result);
    }

    @Test
    public void testToString_EmptyBytes_ReturnsEmptyString() {
        byte[] input = new byte[0];
        String result = ByteArrayConversions.toString(input, converterUtf8);
        assertEquals("Empty byte array should produce an empty String", "", result);
    }

    @Test
    public void testToString_ValidUtf8Bytes_ReturnsCorrectString() {
        String original = "Café € 漢字";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(bytes, converterUtf8);
        assertEquals("UTF‑8 bytes must be decoded correctly", original, result);
    }

    @Test
    public void testToString_DifferentCharset_ReturnsCorrectString() {
        // The same characters encoded in ISO‑8859‑1 (only the first two are representable)
        String original = "Café";
        byte[] bytes = original.getBytes(StandardCharsets.ISO_8859_1);
        String result = ByteArrayConversions.toString(bytes, converterIso);
        assertEquals("ISO‑8859‑1 bytes must be decoded with the ISO charset", original, result);
    }

    /* --------------------------------------------------------------------- */
    /* toByteBuffer                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteBuffer_WrapsByteArray() {
        byte[] data = {1, 2, 3, 4, 5};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, converterUtf8);
        assertNotNull("ByteBuffer should not be null", buffer);
        assertEquals("ByteBuffer capacity must match the source array length", data.length, buffer.capacity());
        // Verify content
        for (byte b : data) {
            assertEquals(b, buffer.get());
        }
        // After reading all bytes the position should be at the limit
        assertEquals(buffer.limit(), buffer.position());
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullInput_ThrowsNPE() {
        ByteArrayConversions.toByteBuffer(null, converterUtf8);
    }

    /* --------------------------------------------------------------------- */
    /* toCharBuffer                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_WrapsStringCorrectly() {
        String text = "Hello, CharBuffer!";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(bytes, converterUtf8);
        assertNotNull("CharBuffer must not be null", charBuffer);
        assertEquals("CharBuffer length must match original string length",
                text.length(), charBuffer.length());
        assertEquals("Content of CharBuffer must equal original string",
                text, charBuffer.toString());
    }

    /* --------------------------------------------------------------------- */
    /* toCharArray                                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharArray_ReturnsCorrectArray() {
        String original = "CharArray";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        char[] chars = ByteArrayConversions.toCharArray(bytes, converterUtf8);
        assertArrayEquals("Char array must contain the same characters as the original string",
                original.toCharArray(), chars);
    }

    /* --------------------------------------------------------------------- */
    /* toStringBuffer                                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBuffer_ProducesEquivalentStringBuffer() {
        String original = "StringBuffer test";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(bytes, converterUtf8);
        assertNotNull("StringBuffer must not be null", sb);
        assertEquals("StringBuffer content must equal original string", original, sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /* toStringBuilder                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilder_ProducesEquivalentStringBuilder() {
        String original = "StringBuilder test";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(bytes, converterUtf8);
        assertNotNull("StringBuilder must not be null", sb);
        assertEquals("StringBuilder content must equal original string", original, sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /* Additional sanity checks – ensure that the returned mutable types
     * are independent of each other and of the original data.
     * --------------------------------------------------------------------- */

    @Test
    public void testMutability_StringBufferDoesNotAffectOriginalBytes() {
        String original = "Mutable test";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(bytes, converterUtf8);
        sb.append(" added");
        assertEquals("Original byte array must remain unchanged", original,
                new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testMutability_StringBuilderDoesNotAffectOriginalBytes() {
        String original = "Mutable test";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(bytes, converterUtf8);
        sb.append(" added");
        assertEquals("Original byte array must remain unchanged", original,
                new String(bytes, StandardCharsets.UTF_8));
    }
}
