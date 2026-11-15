package com.cedarsoftware.util.convert;

import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;
import static org.junit.Assert.*;

public class ByteBufferConversionsTest {

    // Helper to create a Converter with the default charset (UTF‑8)
    private static Converter createDefaultConverter() {
        // ConverterOptions is assumed to have a public no‑arg constructor.
        // The default options use UTF‑8 which is sufficient for our tests.
        ConverterOptions options = new ConverterOptions();
        return new Converter(options);
    }

    @Test
    public void testToByteBufferReturnsReadOnlyCopy() {
        Converter converter = createDefaultConverter();

        // original mutable buffer
        ByteBuffer original = ByteBuffer.allocate(10);
        original.put(new byte[]{0,1,2,3,4,5,6,7,8,9});
        original.flip(); // limit = 10, position = 0

        // call the method under test
        ByteBuffer readOnly = ByteBufferConversions.toByteBuffer(original, converter);

        // the returned buffer must be a different instance
        assertNotSame("Returned buffer should be a new instance", original, readOnly);

        // it must be read‑only
        assertTrue("Returned buffer must be read‑only", readOnly.isReadOnly());

        // changing the original position must not affect the copy
        original.position(5);
        assertEquals("Copy should retain original position (0)", 0, readOnly.position());

        // the contents must be identical
        assertEquals("Remaining bytes should be equal",
                original.remaining(), readOnly.remaining());
        for (int i = 0; i < original.remaining(); i++) {
            assertEquals(original.get(i), readOnly.get(i));
        }
    }

    @Test
    public void testToByteArrayWithData() {
        Converter converter = createDefaultConverter();

        byte[] source = {10, 20, 30, 40, 50};
        ByteBuffer buffer = ByteBuffer.wrap(source);

        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        assertArrayEquals("Byte array conversion should preserve all bytes", source, result);
    }

    @Test
    public void testToByteArrayEmptyBufferReturnsEmptyConstant() {
        Converter converter = createDefaultConverter();

        ByteBuffer empty = ByteBuffer.allocate(0);
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        assertSame("Empty buffer should return the shared EMPTY_BYTE_ARRAY constant",
                EMPTY_BYTE_ARRAY, result);
    }

    @Test
    public void testToCharBufferDecodesUtf8Correctly() {
        Converter converter = createDefaultConverter();

        String original = "Café Δ";
        ByteBuffer buffer = ByteBuffer.wrap(original.getBytes(StandardCharsets.UTF_8));

        CharBuffer charBuffer = ByteBufferConversions.toCharBuffer(buffer, converter);
        assertNotNull("CharBuffer must not be null", charBuffer);
        assertEquals("Decoded CharBuffer should contain the original string",
                original, charBuffer.toString());
    }

    @Test
    public void testToStringEmptyBuffer() {
        Converter converter = createDefaultConverter();

        ByteBuffer empty = ByteBuffer.allocate(0);
        String result = ByteBufferConversions.toString(empty, converter);
        assertEquals("Empty buffer should produce an empty string", "", result);
    }

    @Test
    public void testToStringNonEmptyBuffer() {
        Converter converter = createDefaultConverter();

        String text = "Hello, World!";
        ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

        String result = ByteBufferConversions.toString(buffer, converter);
        assertEquals("String conversion should match original text", text, result);
    }

    @Test
    public void testToCharArrayMatchesStringContent() {
        Converter converter = createDefaultConverter();

        String sample = "JUnit";
        ByteBuffer buffer = ByteBuffer.wrap(sample.getBytes(StandardCharsets.UTF_8));

        char[] chars = ByteBufferConversions.toCharArray(buffer, converter);
        assertArrayEquals("Char array must equal the characters of the original string",
                sample.toCharArray(), chars);
    }

    @Test
    public void testToStringBufferContainsSameData() {
        Converter converter = createDefaultConverter();

        String data = "StringBufferTest";
        ByteBuffer buffer = ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8));

        StringBuffer sb = ByteBufferConversions.toStringBuffer(buffer, converter);
        assertEquals("StringBuffer should contain the same text as the original", data, sb.toString());
    }

    @Test
    public void testToStringBuilderContainsSameData() {
        Converter converter = createDefaultConverter();

        String data = "StringBuilderTest";
        ByteBuffer buffer = ByteBuffer.wrap(data.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = ByteBufferConversions.toStringBuilder(buffer, converter);
        assertEquals("StringBuilder should contain the same text as the original", data, sb.toString());
    }

    @Test
    public void testMultipleConversionsShareSameUnderlyingBytes() {
        Converter converter = createDefaultConverter();

        String content = "shared";
        ByteBuffer original = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));

        // toByteArray
        byte[] bytes = ByteBufferConversions.toByteArray(original, converter);
        assertArrayEquals(content.getBytes(StandardCharsets.UTF_8), bytes);

        // toString
        String asString = ByteBufferConversions.toString(original, converter);
        assertEquals(content, asString);

        // toCharArray
        char[] chars = ByteBufferConversions.toCharArray(original, converter);
        assertArrayEquals(content.toCharArray(), chars);
    }

    @Test
    public void testToByteBufferWithBufferHavingNoRemaining() {
        Converter converter = createDefaultConverter();

        // Create a buffer with capacity 5, put 5 bytes, then set position to limit
        ByteBuffer full = ByteBuffer.allocate(5);
        full.put(new byte[]{1,2,3,4,5});
        full.flip(); // limit = 5, position = 0
        full.position(5); // now no remaining

        ByteBuffer result = ByteBufferConversions.toByteBuffer(full, converter);
        assertNotNull("Resulting buffer should not be null", result);
        assertFalse("Resulting buffer should report no remaining bytes", result.hasRemaining());
        assertEquals("Resulting buffer should be read‑only", true, result.isReadOnly());
    }
}
