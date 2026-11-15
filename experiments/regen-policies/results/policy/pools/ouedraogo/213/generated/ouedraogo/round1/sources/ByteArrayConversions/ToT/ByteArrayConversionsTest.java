package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.StringUtilities;
import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@code ByteArrayConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Typical usage – converting a non‑null {@code byte[]} containing UTF‑8 text.</li>
 *   <li>Edge cases – {@code null} input, empty arrays and wrong input types.</li>
 *   <li>Behaviour of every conversion method (String, ByteBuffer, CharBuffer,
 *       char[], StringBuffer, StringBuilder).</li>
 *   <li>Verification that the underlying {@link Converter} options (charset) are honoured.</li>
 * </ul>
 *
 * <p>No mocking frameworks are used; real {@link Converter} and
 * {@link ConverterOptions} instances are created as they are part of the public API.
 */
public class ByteArrayConversionsTest {

    /** A Converter configured with UTF‑8 – the default charset used by the library. */
    private Converter converter;

    /** Sample text used in most tests. */
    private static final String SAMPLE_TEXT = "JUnit‑4 Test ☀️";

    /** UTF‑8 byte representation of {@link #SAMPLE_TEXT}. */
    private static final byte[] SAMPLE_BYTES = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);

    @Before
    public void setUp() {
        // ConverterOptions exists in the library; the default constructor sets UTF‑8 as charset.
        // If a different API is required, adjust accordingly – the tests only rely on the fact
        // that the charset matches UTF‑8.
        ConverterOptions options = new ConverterOptions(); // default UTF‑8 charset
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- *
     *                      toString() tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToString_WithNonNullByteArray_ReturnsCorrectString() {
        Object input = SAMPLE_BYTES;
        String result = ByteArrayConversions.toString(input, converter);
        assertEquals("String conversion should respect UTF‑8 bytes", SAMPLE_TEXT, result);
    }

    @Test
    public void testToString_WithNullByteArray_ReturnsEmptyString() {
        Object input = null;
        String result = ByteArrayConversions.toString(input, converter);
        assertSame("Null byte array must return StringUtilities.EMPTY", StringUtilities.EMPTY, result);
    }

    @Test
    public void testToString_WithWrongObjectType_ThrowsClassCastException() {
        Object input = "I am not a byte array";
        try {
            ByteArrayConversions.toString(input, converter);
            fail("Expected ClassCastException for non‑byte[] input");
        } catch (ClassCastException expected) {
            // expected path
        }
    }

    /* --------------------------------------------------------------------- *
     *                      toByteBuffer() tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToByteBuffer_WithNonNullByteArray_ReturnsWrappedBuffer() {
        Object input = SAMPLE_BYTES;
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(input, converter);
        assertNotNull("ByteBuffer must not be null", buffer);
        assertEquals("Buffer capacity must match input length", SAMPLE_BYTES.length, buffer.capacity());

        // Verify the content – the buffer's position is 0, limit == capacity after wrap().
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals("ByteBuffer contents must equal the original byte array", SAMPLE_BYTES, actual);
    }

    @Test
    public void testToByteBuffer_WithNullByteArray_ThrowsNullPointerException() {
        Object input = null;
        try {
            ByteArrayConversions.toByteBuffer(input, converter);
            fail("Expected NullPointerException when wrapping a null byte[]");
        } catch (NullPointerException expected) {
            // expected path
        }
    }

    @Test
    public void testToByteBuffer_WithWrongObjectType_ThrowsClassCastException() {
        Object input = 12345; // not a byte[]
        try {
            ByteArrayConversions.toByteBuffer(input, converter);
            fail("Expected ClassCastException for non‑byte[] input");
        } catch (ClassCastException expected) {
            // expected path
        }
    }

    /* --------------------------------------------------------------------- *
     *                      toCharBuffer() tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_WithNonNullByteArray_ReturnsCorrectCharBuffer() {
        Object input = SAMPLE_BYTES;
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(input, converter);
        assertNotNull("CharBuffer must not be null", charBuffer);
        assertEquals("CharBuffer length must equal original string length",
                SAMPLE_TEXT.length(), charBuffer.length());
        assertEquals("CharBuffer content must match original string", SAMPLE_TEXT, charBuffer.toString());
    }

    @Test
    public void testToCharBuffer_WithNullByteArray_ReturnsEmptyCharBuffer() {
        Object input = null;
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(input, converter);
        assertNotNull("CharBuffer must not be null even for null input", charBuffer);
        assertEquals("Empty CharBuffer expected", 0, charBuffer.length());
    }

    @Test
    public void testToCharBuffer_WithWrongObjectType_ThrowsClassCastException() {
        Object input = new Object();
        try {
            ByteArrayConversions.toCharBuffer(input, converter);
            fail("Expected ClassCastException for non‑byte[] input");
        } catch (ClassCastException expected) {
            // expected path
        }
    }

    /* --------------------------------------------------------------------- *
     *                      toCharArray() tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToCharArray_WithNonNullByteArray_ReturnsCorrectArray() {
        Object input = SAMPLE_BYTES;
        char[] chars = ByteArrayConversions.toCharArray(input, converter);
        assertArrayEquals("char[] must equal the characters of the original string",
                SAMPLE_TEXT.toCharArray(), chars);
    }

    @Test
    public void testToCharArray_WithNullByteArray_ReturnsEmptyArray() {
        Object input = null;
        char[] chars = ByteArrayConversions.toCharArray(input, converter);
        assertEquals("Empty char[] expected for null input", 0, chars.length);
    }

    @Test
    public void testToCharArray_WithWrongObjectType_ThrowsClassCastException() {
        Object input = Boolean.TRUE;
        try {
            ByteArrayConversions.toCharArray(input, converter);
            fail("Expected ClassCastException for non‑byte[] input");
        } catch (ClassCastException expected) {
            // expected path
        }
    }

    /* --------------------------------------------------------------------- *
     *                      toStringBuffer() tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringBuffer_WithNonNullByteArray_ReturnsCorrectStringBuffer() {
        Object input = SAMPLE_BYTES;
        StringBuffer sb = ByteArrayConversions.toStringBuffer(input, converter);
        assertNotNull("StringBuffer must not be null", sb);
        assertEquals("StringBuffer content must match original string", SAMPLE_TEXT, sb.toString());
    }

    @Test
    public void testToStringBuffer_WithNullByteArray_ReturnsEmptyStringBuffer() {
        Object input = null;
        StringBuffer sb = ByteArrayConversions.toStringBuffer(input, converter);
        assertNotNull("StringBuffer must not be null for null input", sb);
        assertEquals("Empty StringBuffer expected", 0, sb.length());
    }

    @Test
    public void testToStringBuffer_WithWrongObjectType_ThrowsClassCastException() {
        Object input = new int[]{1, 2, 3};
        try {
            ByteArrayConversions.toStringBuffer(input, converter);
            fail("Expected ClassCastException for non‑byte[] input");
        } catch (ClassCastException expected) {
            // expected path
        }
    }

    /* --------------------------------------------------------------------- *
     *                      toStringBuilder() tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilder_WithNonNullByteArray_ReturnsCorrectStringBuilder() {
        Object input = SAMPLE_BYTES;
        StringBuilder sb = ByteArrayConversions.toStringBuilder(input, converter);
        assertNotNull("StringBuilder must not be null", sb);
        assertEquals("StringBuilder content must match original string", SAMPLE_TEXT, sb.toString());
    }

    @Test
    public void testToStringBuilder_WithNullByteArray_ReturnsEmptyStringBuilder() {
        Object input = null;
        StringBuilder sb = ByteArrayConversions.toStringBuilder(input, converter);
        assertNotNull("StringBuilder must not be null for null input", sb);
        assertEquals("Empty StringBuilder expected", 0, sb.length());
    }

    @Test
    public void testToStringBuilder_WithWrongObjectType_ThrowsClassCastException() {
        Object input = new java.util.Date();
        try {
            ByteArrayConversions.toStringBuilder(input, converter);
            fail("Expected ClassCastException for non‑byte[] input");
        } catch (ClassCastException expected) {
            // expected path
        }
    }

    /* --------------------------------------------------------------------- *
     *                      Additional sanity checks
     * --------------------------------------------------------------------- */

    @Test
    public void testAllConversionsProduceConsistentResults() {
        Object input = SAMPLE_BYTES;

        String asString = ByteArrayConversions.toString(input, converter);
        CharBuffer asCharBuffer = ByteArrayConversions.toCharBuffer(input, converter);
        char[] asCharArray = ByteArrayConversions.toCharArray(input, converter);
        StringBuffer asStringBuffer = ByteArrayConversions.toStringBuffer(input, converter);
        StringBuilder asStringBuilder = ByteArrayConversions.toStringBuilder(input, converter);
        ByteBuffer asByteBuffer = ByteArrayConversions.toByteBuffer(input, converter);

        // All textual representations must be equal to the original string
        assertEquals(asString, asCharBuffer.toString());
        assertArrayEquals(asString.toCharArray(), asCharArray);
        assertEquals(asString, asStringBuffer.toString());
        assertEquals(asString, asStringBuilder.toString());

        // The byte buffer must contain the same bytes as the original array
        byte[] fromBuffer = new byte[asByteBuffer.remaining()];
        asByteBuffer.get(fromBuffer);
        assertArrayEquals("ByteBuffer must contain the same bytes as the source array",
                SAMPLE_BYTES, fromBuffer);
    }
}
