package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * JUnit 4 test suite for {@link CharBufferConversions}.
 * The tests are placed in the same package to allow access to the
 * package‑private static methods of {@code CharBufferConversions}.
 */
public class CharBufferConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The real ConverterOptions class is not shown, but it is assumed to have
        // a public no‑arg constructor and to default to UTF‑8 charset.
        // If a different constructor is required, adjust accordingly.
        ConverterOptions options = new ConverterOptions();                 // default options (UTF‑8)
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods
     * --------------------------------------------------------------------- */

    private CharBuffer mutableCharBuffer(String content) {
        CharBuffer buf = CharBuffer.allocate(content.length());
        buf.put(content);
        buf.flip();               // prepare for reading (position = 0, limit = length)
        return buf;
    }

    private CharBuffer readOnlyCharBuffer(String content) {
        return mutableCharBuffer(content).asReadOnlyBuffer();
    }

    /* --------------------------------------------------------------------- *
     *  Basic functionality tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_ReturnsReadOnlyCopy() {
        CharBuffer original = mutableCharBuffer("hello");
        CharBuffer result = CharBufferConversions.toCharBuffer(original, converter);

        // The result must be a read‑only buffer
        assertTrue("Resulting CharBuffer should be read‑only", result.isReadOnly());

        // The content must be identical
        assertEquals("Buffers should have same remaining characters",
                     original.remaining(), result.remaining());
        assertEquals("Buffers should contain the same characters",
                     original.toString(), result.toString());

        // Original buffer's position must not be altered by the conversion
        assertEquals("Original buffer position should stay unchanged",
                     0, original.position());
    }

    @Test
    public void testToString_ReturnsCorrectString() {
        CharBuffer source = mutableCharBuffer("JUnit4");
        String result = CharBufferConversions.toString(source, converter);
        assertEquals("String conversion should match buffer content", "JUnit4", result);
    }

    @Test
    public void testToStringBuffer_ReturnsCorrectStringBuffer() {
        CharBuffer source = mutableCharBuffer("StringBuffer");
        StringBuffer sb = CharBufferConversions.toStringBuffer(source, converter);
        assertEquals("StringBuffer content must match source", "StringBuffer", sb.toString());
    }

    @Test
    public void testToStringBuilder_ReturnsCorrectStringBuilder() {
        CharBuffer source = mutableCharBuffer("StringBuilder");
        StringBuilder sb = CharBufferConversions.toStringBuilder(source, converter);
        assertEquals("StringBuilder content must match source", "StringBuilder", sb.toString());
    }

    @Test
    public void testToCharArray_NonEmptyBuffer() {
        CharBuffer source = mutableCharBuffer("abcde");
        char[] chars = CharBufferConversions.toCharArray(source, converter);
        assertArrayEquals("Char array should contain same characters",
                          new char[]{'a','b','c','d','e'}, chars);
    }

    @Test
    public void testToCharArray_EmptyBuffer() {
        CharBuffer empty = mutableCharBuffer("");
        char[] chars = CharBufferConversions.toCharArray(empty, converter);
        assertSame("Empty buffer should return the shared EMPTY_CHAR_ARRAY",
                   com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY, chars);
    }

    @Test
    public void testToByteBuffer_EncodingMatchesCharset() {
        CharBuffer source = mutableCharBuffer("µñ");   // characters with multibyte UTF‑8 representation
        ByteBuffer byteBuf = CharBufferConversions.toByteBuffer(source, converter);
        Charset cs = converter.getOptions().getCharset();
        ByteBuffer expected = cs.encode(source);
        assertEquals("Encoded ByteBuffer should be equal to charset.encode()", expected, byteBuf);
    }

    @Test
    public void testToByteArray_ProducesSameBytesAsByteBuffer() {
        CharBuffer source = mutableCharBuffer("test");
        byte[] bytes = CharBufferConversions.toByteArray(source, converter);
        ByteBuffer byteBuf = CharBufferConversions.toByteBuffer(source, converter);
        byte[] expected = new byte[byteBuf.remaining()];
        byteBuf.get(expected);
        assertArrayEquals("Byte array should match byte buffer contents", expected, bytes);
    }

    /* --------------------------------------------------------------------- *
     *  Edge‑case and exception handling tests
     * --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_NullInputThrowsNPE() {
        CharBufferConversions.toCharBuffer(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_WrongTypeThrowsCCE() {
        // Passing a String instead of a CharBuffer should cause a ClassCastException
        CharBufferConversions.toCharBuffer("not a CharBuffer", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullConverterThrowsNPE() {
        CharBuffer source = mutableCharBuffer("data");
        CharBufferConversions.toByteBuffer(source, null);
    }

    @Test
    public void testToCharArray_BufferWithPartialRemaining() {
        // Create a buffer, advance its position, then convert
        CharBuffer source = mutableCharBuffer("partial");
        source.position(2); // remaining should be "rtial"
        char[] chars = CharBufferConversions.toCharArray(source, converter);
        assertArrayEquals("Only remaining characters should be returned",
                          new char[]{'r','t','i','a','l'}, chars);
    }

    @Test
    public void testToString_EmptyBufferReturnsEmptyString() {
        CharBuffer empty = mutableCharBuffer("");
        String result = CharBufferConversions.toString(empty, converter);
        assertEquals("Empty CharBuffer should produce empty String", "", result);
    }

    @Test
    public void testToStringBuffer_ReadOnlySource() {
        CharBuffer source = readOnlyCharBuffer("readonly");
        StringBuffer sb = CharBufferConversions.toStringBuffer(source, converter);
        assertEquals("StringBuffer should correctly copy from a read‑only CharBuffer",
                     "readonly", sb.toString());
    }

    @Test
    public void testToStringBuilder_ReadOnlySource() {
        CharBuffer source = readOnlyCharBuffer("readonlySB");
        StringBuilder sb = CharBufferConversions.toStringBuilder(source, converter);
        assertEquals("StringBuilder should correctly copy from a read‑only CharBuffer",
                     "readonlySB", sb.toString());
    }
}
