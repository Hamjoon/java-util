package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class ByteArrayConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a ConverterOptions class (not shown) that has a default charset.
        // We simply construct a Converter with default options.
        ConverterOptions options = new ConverterOptions(); // default charset is UTF‑8
        this.converter = new Converter(options);
    }

    @Test
    public void testToString_NullInputReturnsEmpty() {
        byte[] input = null;
        String result = ByteArrayConversions.toString(input, converter);
        assertEquals(StringUtilities.EMPTY, result);
    }

    @Test
    public void testToString_EmptyArrayReturnsEmptyString() {
        byte[] input = new byte[0];
        String result = ByteArrayConversions.toString(input, converter);
        assertEquals("", result);
    }

    @Test
    public void testToString_AsciiBytes() {
        String original = "Hello, JUnit!";
        byte[] input = original.getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(input, converter);
        assertEquals(original, result);
    }

    @Test
    public void testToString_UnicodeBytes() {
        String original = "Café ß漢字 🚀"; // includes accented, German ß, CJK, emoji
        byte[] input = original.getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(input, converter);
        assertEquals(original, result);
    }

    @Test
    public void testToByteBuffer_WrapsArray() {
        byte[] data = {0, 1, 2, 3, 4, 5};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(data, converter);
        assertNotNull(buffer);
        assertEquals(data.length, buffer.remaining());
        // Verify content
        for (byte expected : data) {
            assertEquals(expected, buffer.get());
        }
        // After reading all bytes the position should be at limit
        assertEquals(buffer.limit(), buffer.position());
    }

    @Test
    public void testToCharBuffer_WrapsString() {
        String text = "BufferTest";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(data, converter);
        assertNotNull(charBuffer);
        assertEquals(text.length(), charBuffer.remaining());
        // Compare each character
        for (int i = 0; i < text.length(); i++) {
            assertEquals(text.charAt(i), charBuffer.get());
        }
        // Position should now be at limit
        assertEquals(charBuffer.limit(), charBuffer.position());
    }

    @Test
    public void testToCharArray_MatchesStringCharacters() {
        String expected = "CharArray✓";
        byte[] data = expected.getBytes(StandardCharsets.UTF_8);
        char[] result = ByteArrayConversions.toCharArray(data, converter);
        assertArrayEquals(expected.toCharArray(), result);
    }

    @Test
    public void testToStringBuffer_ContentMatches() {
        String expected = "StringBufferContent";
        byte[] data = expected.getBytes(StandardCharsets.UTF_8);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(data, converter);
        assertNotNull(sb);
        assertEquals(expected, sb.toString());
        // Mutability test – append and verify
        sb.append("-appended");
        assertEquals(expected + "-appended", sb.toString());
    }

    @Test
    public void testToStringBuilder_ContentMatches() {
        String expected = "StringBuilderContent";
        byte[] data = expected.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(data, converter);
        assertNotNull(sb);
        assertEquals(expected, sb.toString());
        // Mutability test – insert and verify
        sb.insert(0, "prefix-");
        assertEquals("prefix-" + expected, sb.toString());
    }

    @Test
    public void testRoundTrip_ByteBufferToString() {
        String original = "RoundTripTest";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(bytes, converter);
        // Simulate a round‑trip conversion using the same utility methods
        byte[] extracted = new byte[buffer.remaining()];
        buffer.get(extracted);
        String roundTrip = ByteArrayConversions.toString(extracted, converter);
        assertEquals(original, roundTrip);
    }

    @Test
    public void testMultipleConversionsConsistency() {
        String original = "Consistency✓";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);

        // toString
        String s = ByteArrayConversions.toString(bytes, converter);
        assertEquals(original, s);

        // toCharArray
        char[] chars = ByteArrayConversions.toCharArray(bytes, converter);
        assertArrayEquals(original.toCharArray(), chars);

        // toCharBuffer
        CharBuffer cb = ByteArrayConversions.toCharBuffer(bytes, converter);
        assertEquals(original, cb.toString());

        // toStringBuffer
        StringBuffer sbuf = ByteArrayConversions.toStringBuffer(bytes, converter);
        assertEquals(original, sbuf.toString());

        // toStringBuilder
        StringBuilder sbld = ByteArrayConversions.toStringBuilder(bytes, converter);
        assertEquals(original, sbld.toString());
    }
}
