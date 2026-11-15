package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Unit tests for {@link ByteBufferConversions}. The tests verify the
 * behaviour of each conversion method using real {@link ByteBuffer}
 * instances and a real {@link Converter} (with default options).
 *
 * The class under test is package‑private, therefore the test class is
 * placed in the same package to gain access.
 */
public class ByteBufferConversionsTest {

    /**
     * Helper that creates a {@link Converter} with the default options.
     * The default {@link ConverterOptions} uses UTF‑8 as its charset,
     * which is sufficient for all test data.
     */
    private static Converter createDefaultConverter() {
        // The library provides a public constructor that accepts ConverterOptions.
        // Using the no‑arg constructor of ConverterOptions gives the default charset.
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testToByteBuffer_isReadOnlyAndPreservesPosition() {
        Converter converter = createDefaultConverter();

        // Original buffer with known content and a non‑zero position
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        ByteBuffer original = ByteBuffer.wrap(data);
        original.position(1); // set position to the second byte
        original.limit(3);   // limit to the third byte

        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);

        // The returned buffer must be read‑only
        assertTrue("Resulting ByteBuffer should be read‑only", result.isReadOnly());

        // The original buffer's position and limit must remain unchanged
        assertEquals("Original position should stay unchanged", 1, original.position());
        assertEquals("Original limit should stay unchanged", 3, original.limit());

        // The read‑only copy should have the same position/limit as the original at the time of copying
        assertEquals("Copy should have same position as original", original.position(), result.position());
        assertEquals("Copy should have same limit as original", original.limit(), result.limit());

        // Verify the content is the same (bytes 1..2 are 'b' and 'c')
        byte[] expected = new byte[] { 'b', 'c' };
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals("Content of the read‑only buffer must match", expected, actual);
    }

    @Test
    public void testToByteArray_emptyBufferReturnsEmptyArray() {
        Converter converter = createDefaultConverter();

        ByteBuffer empty = ByteBuffer.allocate(0);
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);

        assertNotNull("Result should never be null", result);
        assertEquals("Empty buffer must produce an empty byte array", 0, result.length);
        assertArrayEquals("Empty byte array expected", new byte[0], result);
    }

    @Test
    public void testToByteArray_nonEmptyBuffer() {
        Converter converter = createDefaultConverter();

        String text = "test";
        ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);

        assertArrayEquals("Byte array must represent the original string", text.getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void testToCharBuffer_decodesUsingConverterCharset() {
        Converter converter = createDefaultConverter();

        String original = "charBuffer";
        ByteBuffer buffer = ByteBuffer.wrap(original.getBytes(StandardCharsets.UTF_8));
        CharBuffer charBuffer = ByteBufferConversions.toCharBuffer(buffer, converter);

        assertNotNull("CharBuffer should not be null", charBuffer);
        assertEquals("Decoded CharBuffer content must match original", original, charBuffer.toString());
    }

    @Test
    public void testToString_returnsDecodedString() {
        Converter converter = createDefaultConverter();

        String expected = "hello world";
        ByteBuffer buffer = ByteBuffer.wrap(expected.getBytes(StandardCharsets.UTF_8));
        String result = ByteBufferConversions.toString(buffer, converter);

        assertEquals("String conversion must produce the original text", expected, result);
    }

    @Test
    public void testToCharArray_returnsCorrectCharacters() {
        Converter converter = createDefaultConverter();

        String source = "charArray";
        ByteBuffer buffer = ByteBuffer.wrap(source.getBytes(StandardCharsets.UTF_8));
        char[] chars = ByteBufferConversions.toCharArray(buffer, converter);

        assertArrayEquals("Char array must equal source characters", source.toCharArray(), chars);
    }

    @Test
    public void testToStringBuffer_andToStringBuilder() {
        Converter converter = createDefaultConverter();

        String text = "builderTest";
        ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

        StringBuffer sb = ByteBufferConversions.toStringBuffer(buffer, converter);
        assertNotNull("StringBuffer should not be null", sb);
        assertEquals("StringBuffer content must match", text, sb.toString());

        // Need a fresh buffer because the previous call may have consumed it
        buffer.rewind();

        StringBuilder sbuilder = ByteBufferConversions.toStringBuilder(buffer, converter);
        assertNotNull("StringBuilder should not be null", sbuilder);
        assertEquals("StringBuilder content must match", text, sbuilder.toString());
    }
}
