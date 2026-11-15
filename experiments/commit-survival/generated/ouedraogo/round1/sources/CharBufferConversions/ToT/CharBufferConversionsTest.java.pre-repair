package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
import static org.junit.Assert.*;

public class CharBufferConversionsTest {

    private Converter converter;
    private Charset testCharset = StandardCharsets.UTF_8;

    @Before
    public void setUp() {
        // The real ConverterOptions class is part of the library.
        // It provides a way to set the charset used for encoding.
        // We assume it has a no‑arg constructor and a setter for the charset.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(testCharset);
        converter = new Converter(options);
    }

    /** -----------------------------------------------------------------------
     *  toCharBuffer
     * ----------------------------------------------------------------------- */
    @Test
    public void testToCharBufferReturnsReadOnlyCopy() {
        CharBuffer original = CharBuffer.wrap("sample text");
        // change position to prove the returned buffer is independent
        original.position(3);

        CharBuffer result = CharBufferConversions.toCharBuffer(original, converter);

        assertNotNull(result);
        assertTrue(result.isReadOnly());

        // The content must be the same as the original buffer's *remaining* characters
        CharBuffer expected = CharBuffer.wrap("sample text");
        expected.position(3); // because original had position 3
        assertEquals(expected.remaining(), result.remaining());
        char[] expectedChars = new char[expected.remaining()];
        expected.get(expectedChars);
        char[] resultChars = new char[result.remaining()];
        result.get(resultChars);
        assertArrayEquals(expectedChars, resultChars);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBufferWithInvalidObjectThrows() {
        Object notABuffer = "I am not a CharBuffer";
        CharBufferConversions.toCharBuffer(notABuffer, converter);
    }

    /** -----------------------------------------------------------------------
     *  toString
     * ----------------------------------------------------------------------- */
    @Test
    public void testToStringProducesCorrectString() {
        CharBuffer buf = CharBuffer.wrap("hello world");
        String result = CharBufferConversions.toString(buf, converter);
        assertEquals("hello world", result);
    }

    /** -----------------------------------------------------------------------
     *  toCharArray
     * ----------------------------------------------------------------------- */
    @Test
    public void testToCharArrayWithContent() {
        CharBuffer buf = CharBuffer.wrap("abcde");
        char[] result = CharBufferConversions.toCharArray(buf, converter);
        assertArrayEquals(new char[]{'a', 'b', 'c', 'd', 'e'}, result);
    }

    @Test
    public void testToCharArrayEmptyBufferReturnsConstant() {
        CharBuffer empty = CharBuffer.allocate(0);
        char[] result = CharBufferConversions.toCharArray(empty, converter);
        // The method returns the shared EMPTY_CHAR_ARRAY constant
        assertSame(EMPTY_CHAR_ARRAY, result);
    }

    /** -----------------------------------------------------------------------
     *  toStringBuffer
     * ----------------------------------------------------------------------- */
    @Test
    public void testToStringBufferCreatesCorrectBuffer() {
        CharBuffer buf = CharBuffer.wrap("buffer content");
        StringBuffer sb = CharBufferConversions.toStringBuffer(buf, converter);
        assertEquals("buffer content", sb.toString());
    }

    /** -----------------------------------------------------------------------
     *  toStringBuilder
     * ----------------------------------------------------------------------- */
    @Test
    public void testToStringBuilderCreatesCorrectBuilder() {
        CharBuffer buf = CharBuffer.wrap("builder content");
        StringBuilder sb = CharBufferConversions.toStringBuilder(buf, converter);
        assertEquals("builder content", sb.toString());
    }

    /** -----------------------------------------------------------------------
     *  toByteBuffer
     * ----------------------------------------------------------------------- */
    @Test
    public void testToByteBufferEncodesUsingCharset() {
        CharBuffer buf = CharBuffer.wrap("µñ"); // characters that have multi‑byte UTF‑8 representation
        ByteBuffer byteBuf = CharBufferConversions.toByteBuffer(buf, converter);
        byte[] expected = "µñ".getBytes(testCharset);
        byte[] actual = new byte[byteBuf.remaining()];
        byteBuf.get(actual);
        assertArrayEquals(expected, actual);
    }

    /** -----------------------------------------------------------------------
     *  toByteArray
     * ----------------------------------------------------------------------- */
    @Test
    public void testToByteArrayMatchesByteBuffer() {
        CharBuffer buf = CharBuffer.wrap("Testing 123!");
        byte[] fromMethod = CharBufferConversions.toByteArray(buf, converter);
        byte[] expected = "Testing 123!".getBytes(testCharset);
        assertArrayEquals(expected, fromMethod);
    }

    /** -----------------------------------------------------------------------
     *  Edge Cases
     * ----------------------------------------------------------------------- */
    @Test
    public void testAllConversionsWithLargeContent() {
        // Build a large string (> 10,000 characters) to stress the conversions
        char[] largeChars = new char[15000];
        Arrays.fill(largeChars, 'x');
        String largeString = new String(largeChars);
        CharBuffer buf = CharBuffer.wrap(largeString);

        // toCharArray
        char[] arr = CharBufferConversions.toCharArray(buf, converter);
        assertEquals(largeChars.length, arr.length);
        assertArrayEquals(largeChars, arr);

        // toString
        assertEquals(largeString, CharBufferConversions.toString(buf, converter));

        // toByteArray
        byte[] bytes = CharBufferConversions.toByteArray(buf, converter);
        assertArrayEquals(largeString.getBytes(testCharset), bytes);
    }

    @Test
    public void testToCharArrayDoesNotModifyOriginalPosition() {
        CharBuffer buf = CharBuffer.wrap("position");
        int originalPos = buf.position();
        CharBufferConversions.toCharArray(buf, converter);
        assertEquals("Method must not change buffer position", originalPos, buf.position());
    }
}
