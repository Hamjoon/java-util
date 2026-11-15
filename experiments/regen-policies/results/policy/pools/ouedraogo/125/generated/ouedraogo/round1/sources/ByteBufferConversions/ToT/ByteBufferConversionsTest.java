package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Comprehensive JUnit‑4 test suite for {@link ByteBufferConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversions (ByteBuffer → CharBuffer → String / char[] / StringBuffer / StringBuilder)</li>
 *   <li>Edge cases – empty buffers, zero‑remaining buffers, read‑only contract</li>
 *   <li>Behaviour of the helper methods when the underlying {@link Converter} supplies a known Charset (UTF‑8)</li>
 * </ul>
 *
 * NOTE: The library’s {@link ConverterOptions} class is not shown in the prompt,
 * but the public API of {@link Converter} indicates that a default‑constructed
 * {@code ConverterOptions} provides a {@link Charset}.  The tests rely on this
 * default being UTF‑8 – which is the usual default for Cedar‑Software’s
 * {@code Converter}.  If the default charset differs, replace
 * {@code StandardCharsets.UTF_8} with the appropriate charset.
 */
public class ByteBufferConversionsTest {

    private static final Charset TEST_CHARSET = StandardCharsets.UTF_8;
    private Converter converter;

    @Before
    public void setUp() {
        // Create a Converter with default options; the default charset is assumed to be UTF‑8.
        // If the library requires explicit options, adjust accordingly.
        ConverterOptions options = new ConverterOptions();
        // The real ConverterOptions class provides a way to set the charset.
        // If a setter exists, uncomment the following line:
        // options.setCharset(TEST_CHARSET);
        converter = new Converter(options);
    }

    /**
     * Helper that creates a read‑write {@link ByteBuffer} containing the UTF‑8
     * bytes of the supplied string.
     */
    private ByteBuffer bufferFromString(String s) {
        byte[] bytes = s.getBytes(TEST_CHARSET);
        return ByteBuffer.wrap(bytes); // position = 0, limit = bytes.length
    }

    /* --------------------------------------------------------------
     *  toByteBuffer()
     * ------------------------------------------------------------ */

    @Test
    public void testToByteBufferCreatesReadOnlyCopy() {
        ByteBuffer original = bufferFromString("readonly");
        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);

        // The result must be a different instance (asReadOnlyBuffer returns a new buffer)
        assertNotSame("Result must not be the same instance as the original", original, result);

        // The result must be read‑only
        try {
            result.put((byte) 0);
            fail("ReadOnlyBufferException expected when writing to the read‑only buffer");
        } catch (ReadOnlyBufferException expected) {
            // expected
        }

        // The content must be identical
        assertEquals("Remaining bytes must be equal", original.remaining(), result.remaining());

        byte[] originalBytes = new byte[original.remaining()];
        original.get(originalBytes);
        byte[] resultBytes = new byte[result.remaining()];
        result.get(resultBytes);
        assertArrayEquals("Byte content must be unchanged", originalBytes, resultBytes);
    }

    @Test
    public void testToByteBufferDoesNotAlterOriginalPosition() {
        ByteBuffer original = bufferFromString("position");
        original.position(2); // move position forward
        int originalPos = original.position();
        int originalLim = original.limit();

        ByteBuffer copy = ByteBufferConversions.toByteBuffer(original, converter);

        // Original buffer’s position & limit must stay unchanged
        assertEquals("Original position must remain unchanged", originalPos, original.position());
        assertEquals("Original limit must remain unchanged", originalLim, original.limit());

        // The copy must start at the same position as the original (asReadOnlyBuffer mirrors it)
        assertEquals("Copy position must match original", originalPos, copy.position());
        assertEquals("Copy limit must match original", originalLim, copy.limit());
    }

    /* --------------------------------------------------------------
     *  toCharBuffer()
     * ------------------------------------------------------------ */

    @Test
    public void testToCharBufferDecodesCorrectly() {
        String src = "Hello, 世界";
        ByteBuffer srcBuf = bufferFromString(src);
        CharBuffer cb = ByteBufferConversions.toCharBuffer(srcBuf, converter);

        assertNotNull("CharBuffer should not be null", cb);
        assertEquals("Decoded characters must match original string", src, cb.toString());
    }

    @Test
    public void testToCharBufferWithEmptyByteBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        CharBuffer cb = ByteBufferConversions.toCharBuffer(empty, converter);
        assertEquals("Empty buffer should decode to empty CharBuffer", 0, cb.length());
    }

    /* --------------------------------------------------------------
     *  toByteArray()
     * ------------------------------------------------------------ */

    @Test
    public void testToByteArrayTypical() {
        String text = "ByteArrayTest";
        ByteBuffer buf = bufferFromString(text);
        byte[] result = ByteBufferConversions.toByteArray(buf, converter);
        assertArrayEquals("Byte array must equal original UTF‑8 bytes",
                text.getBytes(TEST_CHARSET), result);
    }

    @Test
    public void testToByteArrayEmptyBufferReturnsConstant() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        assertSame("Empty buffer should return the shared EMPTY_BYTE_ARRAY constant",
                EMPTY_BYTE_ARRAY, result);
    }

    @Test
    public void testToByteArrayZeroRemaining() {
        // Create a buffer with capacity > 0 but position == limit → no remaining bytes
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.position(10); // now remaining() == 0
        byte[] result = ByteBufferConversions.toByteArray(buf, converter);
        assertSame("Zero‑remaining buffer should return EMPTY_BYTE_ARRAY", EMPTY_BYTE_ARRAY, result);
    }

    /* --------------------------------------------------------------
     *  toString()
     * ------------------------------------------------------------ */

    @Test
    public void testToStringMatchesOriginal() {
        String src = "StringConversion";
        ByteBuffer buf = bufferFromString(src);
        String result = ByteBufferConversions.toString(buf, converter);
        assertEquals("Resulting string must match the original", src, result);
    }

    @Test
    public void testToStringEmptyBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        String result = ByteBufferConversions.toString(empty, converter);
        assertEquals("Empty buffer should produce empty string", "", result);
    }

    /* --------------------------------------------------------------
     *  toCharArray()
     * ------------------------------------------------------------ */

    @Test
    public void testToCharArrayTypical() {
        String src = "CharArray✓";
        ByteBuffer buf = bufferFromString(src);
        char[] chars = ByteBufferConversions.toCharArray(buf, converter);
        assertArrayEquals("Char array must contain the same characters as source string",
                src.toCharArray(), chars);
    }

    @Test
    public void testToCharArrayEmptyBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        char[] chars = ByteBufferConversions.toCharArray(empty, converter);
        assertEquals("Empty buffer should yield empty char array", 0, chars.length);
    }

    /* --------------------------------------------------------------
     *  toStringBuffer()
     * ------------------------------------------------------------ */

    @Test
    public void testToStringBufferTypical() {
        String src = "StringBufferTest";
        ByteBuffer buf = bufferFromString(src);
        StringBuffer sb = ByteBufferConversions.toStringBuffer(buf, converter);
        assertEquals("StringBuffer content must equal source string", src, sb.toString());
    }

    @Test
    public void testToStringBufferEmpty() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        StringBuffer sb = ByteBufferConversions.toStringBuffer(empty, converter);
        assertEquals("Empty buffer should produce empty StringBuffer", "", sb.toString());
    }

    /* --------------------------------------------------------------
     *  toStringBuilder()
     * ------------------------------------------------------------ */

    @Test
    public void testToStringBuilderTypical() {
        String src = "StringBuilder✓";
        ByteBuffer buf = bufferFromString(src);
        StringBuilder sb = ByteBufferConversions.toStringBuilder(buf, converter);
        assertEquals("StringBuilder content must equal source string", src, sb.toString());
    }

    @Test
    public void testToStringBuilderEmpty() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        StringBuilder sb = ByteBufferConversions.toStringBuilder(empty, converter);
        assertEquals("Empty buffer should produce empty StringBuilder", "", sb.toString());
    }
}
