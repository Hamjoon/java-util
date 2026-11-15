package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Unit tests for {@link CharBufferConversions}.
 *
 * The tests exercise every static method in the class, covering normal
 * usage as well as edge‑cases such as empty buffers.  The test class lives
 * in the same package as {@code CharBufferConversions} so that the package‑private
 * static methods are directly accessible.
 */
public class CharBufferConversionsTest {

    private Converter converter;
    private Charset charset;

    @Before
    public void setUp() {
        // The library provides a public Converter that internally creates a
        // ConverterOptions with a default Charset (UTF‑8).  We rely on the default
        // behaviour – the test only needs a non‑null Converter whose options
        // expose a Charset.
        converter = new Converter(new ConverterOptions());
        charset = converter.getOptions().getCharset();
        assertNotNull("Converter must provide a non‑null Charset", charset);
    }

    @Test
    public void testToCharBufferCreatesReadOnlyBuffer() {
        CharBuffer original = CharBuffer.wrap("Hello World");
        CharBuffer result = CharBufferConversions.toCharBuffer(original, converter);

        // The result must be a read‑only view of the original buffer.
        assertTrue("Resulting CharBuffer should be read‑only", result.isReadOnly());
        assertEquals("Capacity must be preserved", original.capacity(), result.capacity());

        // Positions should be independent – the original buffer's position must stay at 0.
        assertEquals("Original buffer position should stay unchanged", 0, original.position());

        // Content must be identical.
        assertEquals("Content must match", original.toString(), result.toString());
    }

    @Test
    public void testToString() {
        CharBuffer cb = CharBuffer.wrap("JUnit Test");
        String s = CharBufferConversions.toString(cb, converter);
        assertEquals("String conversion must match original content", "JUnit Test", s);
    }

    @Test
    public void testToCharArrayNonEmpty() {
        CharBuffer cb = CharBuffer.wrap("abcde");
        char[] chars = CharBufferConversions.toCharArray(cb, converter);
        assertArrayEquals("Char array must contain the same characters",
                new char[] {'a','b','c','d','e'}, chars);
    }

    @Test
    public void testToCharArrayEmpty() {
        CharBuffer empty = CharBuffer.wrap("");
        char[] chars = CharBufferConversions.toCharArray(empty, converter);
        assertEquals("Empty buffer should yield zero‑length array", 0, chars.length);
    }

    @Test
    public void testToStringBuffer() {
        CharBuffer cb = CharBuffer.wrap("StringBuffer Content");
        StringBuffer sb = CharBufferConversions.toStringBuffer(cb, converter);
        assertEquals("StringBuffer must contain the same characters",
                "StringBuffer Content", sb.toString());
    }

    @Test
    public void testToStringBuilder() {
        CharBuffer cb = CharBuffer.wrap("StringBuilder Content");
        StringBuilder sb = CharBufferConversions.toStringBuilder(cb, converter);
        assertEquals("StringBuilder must contain the same characters",
                "StringBuilder Content", sb.toString());
    }

    @Test
    public void testToByteBuffer() {
        String original = "ByteBuffer Test ☃";
        CharBuffer cb = CharBuffer.wrap(original);
        ByteBuffer bb = CharBufferConversions.toByteBuffer(cb, converter);

        // The ByteBuffer should contain the UTF‑8 (or whatever charset the
        // ConverterOptions uses) encoding of the original string.
        byte[] expected = original.getBytes(charset);
        byte[] actual = new byte[bb.remaining()];
        bb.get(actual);
        assertArrayEquals("Encoded bytes must match the charset encoding", expected, actual);
    }

    @Test
    public void testToByteArray() {
        String original = "ByteArray Test ñ";
        CharBuffer cb = CharBuffer.wrap(original);
        byte[] bytes = CharBufferConversions.toByteArray(cb, converter);

        byte[] expected = original.getBytes(charset);
        assertArrayEquals("Byte array must match the charset encoding of the original string",
                expected, bytes);
    }

    @Test
    public void testToByteArrayEmpty() {
        CharBuffer empty = CharBuffer.wrap("");
        byte[] bytes = CharBufferConversions.toByteArray(empty, converter);
        assertEquals("Empty CharBuffer must result in zero‑length byte array", 0, bytes.length);
    }
}
