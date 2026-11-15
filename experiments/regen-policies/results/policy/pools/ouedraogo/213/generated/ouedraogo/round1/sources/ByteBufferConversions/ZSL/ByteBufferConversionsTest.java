package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static com.cedarsoftware.util.convert.MapConversions.VALUE;
import static org.junit.Assert.*;

public class ByteBufferConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter class is part of the library; we use the default options.
        // The default charset (UTF‑8) will correctly decode simple ASCII bytes.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToByteBufferCreatesReadOnlySlice() {
        byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
        ByteBuffer original = ByteBuffer.wrap(data);
        original.position(1);          // point to 'e'
        original.limit(4);             // slice "ell"

        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);

        // The returned buffer must be read‑only and must represent the same slice.
        assertTrue("Resulting buffer should be read‑only", result.isReadOnly());

        // The slice should contain the bytes for "ell"
        byte[] expected = new byte[]{'e', 'l', 'l'};
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expected, actual);

        // Original buffer's position/limit must be unchanged.
        assertEquals(1, original.position());
        assertEquals(4, original.limit());
    }

    @Test
    public void testToByteBufferReadOnlyModificationThrows() {
        ByteBuffer original = ByteBuffer.wrap(new byte[]{0, 1, 2});
        ByteBuffer readOnly = ByteBufferConversions.toByteBuffer(original, converter);
        try {
            readOnly.put((byte) 9);
            fail("Expected ReadOnlyBufferException");
        } catch (ReadOnlyBufferException expected) {
            // expected
        }
    }

    @Test
    public void testToByteArrayNonEmptyBuffer() {
        byte[] data = {10, 20, 30, 40};
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(1);
        buffer.limit(3); // slice {20,30}
        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        assertArrayEquals(new byte[]{20, 30}, result);
    }

    @Test
    public void testToByteArrayEmptyBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testToCharBufferDecodesCorrectly() {
        // Use simple ASCII characters – decoding is independent of charset choice.
        byte[] bytes = "ABC".getBytes(StandardCharsets.US_ASCII);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        CharBuffer charBuf = ByteBufferConversions.toCharBuffer(buffer, converter);
        assertEquals("ABC", charBuf.toString());
    }

    @Test
    public void testToStringUsesCharBuffer() {
        byte[] bytes = "TestString".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        String result = ByteBufferConversions.toString(buffer, converter);
        assertEquals("TestString", result);
    }

    @Test
    public void testToCharArrayMatchesCharBuffer() {
        byte[] bytes = "xyz".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        char[] chars = ByteBufferConversions.toCharArray(buffer, converter);
        assertArrayEquals(new char[]{'x', 'y', 'z'}, chars);
    }

    @Test
    public void testToStringBufferAndBuilder() {
        byte[] bytes = "Builder".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        StringBuffer sb = ByteBufferConversions.toStringBuffer(buffer, converter);
        StringBuilder sbuilder = ByteBufferConversions.toStringBuilder(buffer, converter);

        assertEquals("Builder", sb.toString());
        assertEquals("Builder", sbuilder.toString());
    }

    @Test
    public void testToMapWithArrayBackedBuffer() {
        byte[] data = "Base64Test".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        // Move position to test slicing
        buffer.position(4); // points at '64T...'
        buffer.limit(data.length); // keep rest

        Map<?, ?> map = ByteBufferConversions.toMap(buffer, converter);
        assertNotNull(map);
        assertTrue(map.containsKey(VALUE));

        // Expected slice bytes
        byte[] expectedSlice = new byte[data.length - 4];
        System.arraycopy(data, 4, expectedSlice, 0, expectedSlice.length);
        String expectedEncoded = Base64.getEncoder().encodeToString(expectedSlice);
        assertEquals(expectedEncoded, map.get(VALUE));

        // Ensure original buffer's position is unchanged after conversion
        assertEquals(4, buffer.position());
    }

    @Test
    public void testToMapWithDirectBuffer() {
        byte[] data = "DirectBuffer".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
        buffer.put(data);
        buffer.flip(); // position 0, limit = data.length

        // Change position/limit to create a slice
        buffer.position(2);
        buffer.limit(data.length - 2); // slice "rectBuffer"

        Map<?, ?> map = ByteBufferConversions.toMap(buffer, converter);
        assertNotNull(map);
        assertTrue(map.containsKey(VALUE));

        // Expected slice bytes
        byte[] expectedSlice = new byte[data.length - 4];
        System.arraycopy(data, 2, expectedSlice, 0, expectedSlice.length);
        String expectedEncoded = Base64.getEncoder().encodeToString(expectedSlice);
        assertEquals(expectedEncoded, map.get(VALUE));

        // Verify original position restored
        assertEquals(2, buffer.position());
    }

    @Test
    public void testToMapWithEmptyBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        Map<?, ?> map = ByteBufferConversions.toMap(empty, converter);
        assertNotNull(map);
        assertTrue(map.containsKey(VALUE));
        assertEquals("", map.get(VALUE)); // Base64 of empty byte array is empty string
    }
}
