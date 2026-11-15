/****************************************************************************************
 * CharBufferConversionsTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@code com.cedarsoftware.util.convert.CharBufferConversions}.
 *
 * The tests cover:
 *   • toCharBuffer – read‑only behavior and content preservation.
 *   • toByteArray – correct UTF‑8 byte representation via {@code ByteBufferConversions}.
 *   • toByteBuffer – proper charset encoding.
 *   • toString – round‑trip string conversion.
 *   • toCharArray – handling of non‑empty and empty buffers.
 *   • toStringBuffer – content equality with original characters.
 *   • toStringBuilder – content equality with original characters.
 *
 * The test class uses only the public API of {@code Converter} and standard JDK classes.
 *
 * Author: OpenAI ChatGPT
 * Date:   2026‑11‑13
 ****************************************************************************************/

package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test class for {@link CharBufferConversions}.
 */
public class CharBufferConversionsTest {

    /** A {@code Converter} instance with UTF‑8 charset – the default for the library. */
    private Converter converter;

    /** Sample text used throughout the tests. */
    private static final String SAMPLE_TEXT = "Cedar©™ – Testing 1, 2, 3!";

    /** CharBuffer wrapping the sample text. */
    private CharBuffer sampleBuffer;

    @Before
    public void setUp() {
        // Build ConverterOptions with UTF‑8 charset.
        // The actual library provides a mutable setter; we rely on that contract.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(StandardCharsets.UTF_8);
        converter = new Converter(options);

        // Create a writable CharBuffer that contains the sample text.
        sampleBuffer = CharBuffer.wrap(SAMPLE_TEXT.toCharArray());
    }

    /** Helper to create a fresh read‑only CharBuffer via the class under test. */
    private CharBuffer getReadOnlyBuffer() {
        return CharBufferConversions.toCharBuffer(sampleBuffer, converter);
    }

    /* --------------------------------------------------------------------- */
    /*                       toCharBuffer() Tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharBufferReturnsReadOnlyBufferWithSameContent() {
        CharBuffer ro = getReadOnlyBuffer();

        // Verify that the buffer is indeed read‑only.
        assertTrue("Buffer should be read‑only", ro.isReadOnly());

        // Verify the content matches the original characters.
        assertEquals("Buffer length must match source", sampleBuffer.remaining(), ro.remaining());
        assertEquals("String representation must be identical",
                sampleBuffer.toString(),
                ro.toString());

        // Verify that attempts to modify the buffer throw ReadOnlyBufferException.
        try {
            ro.put('X');
            fail("Expected ReadOnlyBufferException when writing to read‑only buffer");
        } catch (ReadOnlyBufferException expected) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /*                       toByteBuffer() Tests                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteBufferEncodesUsingConfiguredCharset() {
        ByteBuffer byteBuf = CharBufferConversions.toByteBuffer(sampleBuffer, converter);
        Charset charset = converter.getOptions().getCharset();

        // Decode the bytes back to a string using the same charset and compare.
        String decoded = charset.decode(byteBuf).toString();
        assertEquals("Decoded string must equal original", SAMPLE_TEXT, decoded);
    }

    /* --------------------------------------------------------------------- */
    /*                       toByteArray() Tests                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteArrayProducesCorrectUtf8Bytes() {
        byte[] bytes = CharBufferConversions.toByteArray(sampleBuffer, converter);
        byte[] expected = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals("Byte array must equal UTF‑8 encoding of the sample text", expected, bytes);
    }

    /* --------------------------------------------------------------------- */
    /*                       toString() Tests                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringReturnsExactString() {
        String result = CharBufferConversions.toString(sampleBuffer, converter);
        assertEquals("String conversion must be exact", SAMPLE_TEXT, result);
    }

    /* --------------------------------------------------------------------- */
    /*                       toCharArray() Tests                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharArrayCopiesAllRemainingChars() {
        char[] chars = CharBufferConversions.toCharArray(sampleBuffer, converter);
        assertArrayEquals("Char array must contain the same characters as the source",
                SAMPLE_TEXT.toCharArray(),
                chars);
    }

    @Test
    public void testToCharArrayOnEmptyBufferReturnsEmptyArray() {
        CharBuffer empty = CharBuffer.allocate(0);
        char[] result = CharBufferConversions.toCharArray(empty, converter);
        assertNotNull("Result must not be null", result);
        assertEquals("Empty buffer should yield empty char array", 0, result.length);
    }

    /* --------------------------------------------------------------------- */
    /*                       toStringBuffer() Tests                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBufferContentMatchesSource() {
        StringBuffer sb = CharBufferConversions.toStringBuffer(sampleBuffer, converter);
        assertEquals("StringBuffer content must equal original string",
                SAMPLE_TEXT,
                sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /*                       toStringBuilder() Tests                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilderContentMatchesSource() {
        StringBuilder sb = CharBufferConversions.toStringBuilder(sampleBuffer, converter);
        assertEquals("StringBuilder content must equal original string",
                SAMPLE_TEXT,
                sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /*                       Additional Edge Cases                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharBufferDoesNotAlterOriginalBufferPosition() {
        // Advance the original buffer's position deliberately.
        sampleBuffer.position(5);
        int originalPosition = sampleBuffer.position();

        CharBuffer ro = CharBufferConversions.toCharBuffer(sampleBuffer, converter);
        // The read‑only view should start at the same position as the original.
        assertEquals("Read‑only buffer must reflect original position", originalPosition, ro.position());

        // Confirm that after using the read‑only buffer, the original position is unchanged.
        ro.toString(); // consume the view
        assertEquals("Original buffer position must remain unchanged after conversion",
                originalPosition,
                sampleBuffer.position());
    }

    @Test
    public void testToByteBufferDoesNotModifyOriginalCharBufferPosition() {
        sampleBuffer.position(3);
        int beforePos = sampleBuffer.position();

        ByteBuffer bb = CharBufferConversions.toByteBuffer(sampleBuffer, converter);
        // The original buffer's position should stay the same.
        assertEquals("Original CharBuffer position must stay unchanged after toByteBuffer",
                beforePos,
                sampleBuffer.position());

        // Ensure the returned ByteBuffer is ready for reading (position = 0, limit = capacity).
        assertEquals("ByteBuffer position should be zero after encoding", 0, bb.position());
        assertTrue("ByteBuffer should have remaining bytes", bb.remaining() > 0);
    }
}
