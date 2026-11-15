package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.Assert.*;

public class ByteBufferConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is assumed to have a public no‑arg constructor.
        // Its default charset is UTF‑8 which matches the tests.
        converter = new Converter(new ConverterOptions());
    }

    /* -----------------------------------------------------------------
       Basic (happy‑path) tests
       ----------------------------------------------------------------- */

    @Test
    public void testToCharBuffer() {
        ByteBuffer src = ByteBuffer.wrap("abc".getBytes(StandardCharsets.UTF_8));
        CharBuffer cb = ByteBufferConversions.toCharBuffer(src, converter);
        assertEquals("abc", cb.toString());
    }

    @Test
    public void testToByteBuffer() {
        ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3});
        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, converter);
        assertTrue(result.isReadOnly());
        // read‑only view shares the same backing array
        assertArrayEquals(new byte[]{1, 2, 3}, result.array());
    }

    @Test
    public void testToByteArray() {
        ByteBuffer src = ByteBuffer.wrap(new byte[]{10, 20, 30});
        byte[] arr = ByteBufferConversions.toByteArray(src, converter);
        assertArrayEquals(new byte[]{10, 20, 30}, arr);
    }

    @Test
    public void testToString() {
        ByteBuffer src = ByteBuffer.wrap("hello".getBytes(StandardCharsets.UTF_8));
        String s = ByteBufferConversions.toString(src, converter);
        assertEquals("hello", s);
    }

    @Test
    public void testToCharArray() {
        ByteBuffer src = ByteBuffer.wrap("xyz".getBytes(StandardCharsets.UTF_8));
        char[] chars = ByteBufferConversions.toCharArray(src, converter);
        assertArrayEquals(new char[]{'x', 'y', 'z'}, chars);
    }

    @Test
    public void testToStringBuffer() {
        ByteBuffer src = ByteBuffer.wrap("buffer".getBytes(StandardCharsets.UTF_8));
        StringBuffer sb = ByteBufferConversions.toStringBuffer(src, converter);
        assertEquals("buffer", sb.toString());
    }

    @Test
    public void testToStringBuilder() {
        ByteBuffer src = ByteBuffer.wrap("builder".getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = ByteBufferConversions.toStringBuilder(src, converter);
        assertEquals("builder", sb.toString());
    }

    @Test
    public void testToMap_ArrayBacked() {
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        ByteBuffer src = ByteBuffer.wrap(data);
        Map<?, ?> map = ByteBufferConversions.toMap(src, converter);
        String expected = Base64.getEncoder().encodeToString(data);
        assertEquals(expected, map.get("value"));
    }

    /* -----------------------------------------------------------------
       Edge‑case / exception tests
       ----------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_NullInput() {
        ByteBufferConversions.toCharBuffer(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullInput() {
        ByteBufferConversions.toByteBuffer(null, converter);
    }

    @Test
    public void testToByteArray_EmptyBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        assertSame(ByteBufferConversions.EMPTY_BYTE_ARRAY, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArray_NullInput() {
        ByteBufferConversions.toByteArray(null, converter);
    }

    @Test
    public void testToMap_ArrayBacked_WithPosition() {
        byte[] data = "123456".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.position(2);                     // remaining slice = "3456"
        Map<?, ?> map = ByteBufferConversions.toMap(buf, converter);
        String expected = Base64.getEncoder()
                .encodeToString("3456".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, map.get("value"));
        // ensure original position unchanged
        assertEquals(2, buf.position());
    }

    @Test
    public void testToMap_DirectBuffer_PositionRestored() {
        byte[] data = "direct".getBytes(StandardCharsets.UTF_8);
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length);
        direct.put(data);
        direct.flip();          // position=0, limit=length
        direct.position(3);      // remaining = "ect"
        int originalPos = direct.position();

        Map<?, ?> map = ByteBufferConversions.toMap(direct, converter);
        String expected = Base64.getEncoder()
                .encodeToString("ect".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, map.get("value"));
        // verify that the method restored the original position
        assertEquals(originalPos, direct.position());
    }

    @Test
    public void testToMap_EmptyBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        Map<?, ?> map = ByteBufferConversions.toMap(empty, converter);
        // Base64 encoding of an empty byte array is the empty string
        assertEquals("", map.get("value"));
    }
}
