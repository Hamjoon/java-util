package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.Assert.*;

public class ByteBufferConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The real ConverterOptions class is part of the library.
        // It provides a way to set the charset used for decoding.
        // We assume a public no‑arg constructor and a setter for charset.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(StandardCharsets.UTF_8);
        converter = new Converter(options);
    }

    @Test
    public void testToByteBufferReadOnlyAndPositionPreserved() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        ByteBuffer original = ByteBuffer.wrap(data);
        original.position(1).limit(4); // slice = {2,3,4}

        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);

        // Result must be a read‑only buffer
        assertTrue("Resulting buffer should be read‑only", result.isReadOnly());

        // Position and limit must be identical to the original at the moment of conversion
        assertEquals("Position should be preserved", original.position(), result.position());
        assertEquals("Limit should be preserved", original.limit(), result.limit());

        // Changing the original's position after conversion must NOT affect the result's position
        original.position(0);
        assertEquals("Resulting buffer position must stay unchanged after original is moved",
                     1, result.position());

        // Trying to write through the result must throw ReadOnlyBufferException
        try {
            result.put((byte) 9);
            fail("Expected ReadOnlyBufferException when writing to a read‑only buffer");
        } catch (java.nio.ReadOnlyBufferException expected) {
            // expected
        }
    }

    @Test
    public void testToByteArrayWithRemainingBytes() {
        byte[] src = new byte[]{10, 20, 30, 40, 50};
        ByteBuffer buffer = ByteBuffer.wrap(src);
        buffer.position(2); // remaining = {30,40,50}

        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        byte[] expected = new byte[]{30, 40, 50};

        assertArrayEquals("Byte array should contain only the remaining bytes", expected, result);
    }

    @Test
    public void testToByteArrayWhenNoRemainingReturnsEmptyArray() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
        buffer.position(buffer.limit()); // no remaining bytes

        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        assertArrayEquals("When no bytes remain, an empty array should be returned", new byte[0], result);
    }

    @Test
    public void testToStringDecodesUtf8Correctly() {
        String original = "Café 🚀";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        String result = ByteBufferConversions.toString(buffer, converter);
        assertEquals("Decoded string must match original", original, result);
    }

    @Test
    public void testToStringWithEmptyBufferReturnsEmptyString() {
        ByteBuffer buffer = ByteBuffer.allocate(0);
        String result = ByteBufferConversions.toString(buffer, converter);
        assertEquals("Empty buffer should produce empty string", "", result);
    }

    @Test
    public void testToCharArrayMatchesStringCharacters() {
        String text = "Hello, 世界";
        ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

        char[] chars = ByteBufferConversions.toCharArray(buffer, converter);
        assertArrayEquals("Char array must match the characters of the original string",
                text.toCharArray(), chars);
    }

    @Test
    public void testToStringBufferContent() {
        String payload = "buffer‑test";
        ByteBuffer buffer = ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8));

        StringBuffer sb = ByteBufferConversions.toStringBuffer(buffer, converter);
        assertEquals("StringBuffer should contain the decoded characters", payload, sb.toString());
    }

    @Test
    public void testToStringBuilderContent() {
        String payload = "builder‑test";
        ByteBuffer buffer = ByteBuffer.wrap(payload.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = ByteBufferConversions.toStringBuilder(buffer, converter);
        assertEquals("StringBuilder should contain the decoded characters", payload, sb.toString());
    }

    @Test
    public void testToMapArrayBackedBuffer() {
        byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(2).limit(8); // slice = {2,3,4,5,6,7}

        Map<?, ?> map = ByteBufferConversions.toMap(buffer, converter);
        assertNotNull("Map result must not be null", map);
        assertTrue("Map must contain the VALUE key", map.containsKey(MapConversions.VALUE));

        // Expected Base64 of the slice
        byte[] slice = new byte[6];
        System.arraycopy(data, 2, slice, 0, 6);
        String expectedBase64 = Base64.getEncoder().encodeToString(slice);
        assertEquals("Encoded value must match Base64 of the sliced bytes",
                expectedBase64, map.get(MapConversions.VALUE));

        // Position must be restored after conversion
        assertEquals("Original buffer position must be restored", 2, buffer.position());
    }

    @Test
    public void testToMapDirectBuffer() {
        // Direct (non‑array‑backed) buffer
        ByteBuffer buffer = ByteBuffer.allocateDirect(12);
        for (int i = 0; i < 12; i++) {
            buffer.put((byte) i);
        }
        buffer.flip();               // limit = 12, position = 0
        buffer.position(4).limit(9); // slice = bytes 4..8 (5 bytes)

        Map<?, ?> map = ByteBufferConversions.toMap(buffer, converter);
        assertNotNull("Map result must not be null", map);
        assertTrue("Map must contain the VALUE key", map.containsKey(MapConversions.VALUE));

        // Build expected slice manually
        byte[] expectedSlice = new byte[5];
        for (int i = 0; i < 5; i++) {
            expectedSlice[i] = (byte) (i + 4);
        }
        String expectedBase64 = Base64.getEncoder().encodeToString(expectedSlice);
        assertEquals("Encoded value must match Base64 of the sliced bytes",
                expectedBase64, map.get(MapConversions.VALUE));

        // Position must be restored to the start of the slice (4)
        assertEquals("Original buffer position must be restored after toMap", 4, buffer.position());
    }

    @Test
    public void testToCharBufferDecodesCorrectly() {
        String text = "Δοκιμή"; // Greek word
        ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));

        CharBuffer charBuffer = ByteBufferConversions.toCharBuffer(buffer, converter);
        assertNotNull("CharBuffer must not be null", charBuffer);
        assertEquals("Decoded CharBuffer should have the same content as original string",
                text, charBuffer.toString());
    }
}
