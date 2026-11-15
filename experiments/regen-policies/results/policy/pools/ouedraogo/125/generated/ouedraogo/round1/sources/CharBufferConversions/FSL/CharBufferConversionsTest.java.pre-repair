package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link CharBufferConversions}.
 * These tests exercise every public static method in the class,
 * using real {@link Converter} and {@link CharBuffer} instances
 * (no mocks or subclassing of library types).
 */
public class CharBufferConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Use the default options – the charset defaults to UTF‑8.
        // The Converter constructor requires a ConverterOptions instance;
        // we can obtain a sane default via the public no‑arg constructor of
        // ConverterOptions (not shown here) – but the library provides a static
        // factory method in Converter for a default instance.  Since the API
        // only exposes the constructor, we create a minimal options object
        // that supplies UTF‑8.
        ConverterOptions options = new ConverterOptions()
                .withCharset(Charset.forName("UTF-8"));
        converter = new Converter(options);
    }

    @Test
    public void testToCharBuffer_ReadOnly() {
        CharBuffer original = CharBuffer.wrap("sample");
        CharBuffer result = CharBufferConversions.toCharBuffer(original, converter);

        // The result must be a read‑only view of the original buffer.
        assertTrue(result.isReadOnly());
        assertEquals(original.toString(), result.toString());

        // Changing the position of the original must not affect the result.
        original.position(2);
        assertEquals(0, result.position());
    }

    @Test
    public void testToCharBuffer_EmptyBuffer() {
        CharBuffer empty = CharBuffer.wrap("");
        CharBuffer result = CharBufferConversions.toCharBuffer(empty, converter);
        assertEquals(0, result.remaining());
        assertEquals("", result.toString());
    }

    @Test
    public void testToByteArray_SimpleString() {
        CharBuffer buffer = CharBuffer.wrap("ABC");
        byte[] bytes = CharBufferConversions.toByteArray(buffer, converter);
        // UTF‑8 encoding of "ABC"
        assertArrayEquals(new byte[]{65, 66, 67}, bytes);
    }

    @Test
    public void testToByteArray_Empty() {
        CharBuffer empty = CharBuffer.wrap("");
        byte[] bytes = CharBufferConversions.toByteArray(empty, converter);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testToByteBuffer_EncodingMatchesCharset() {
        CharBuffer buffer = CharBuffer.wrap("©"); // U+00A9 copyright sign
        ByteBuffer byteBuf = CharBufferConversions.toByteBuffer(buffer, converter);
        // UTF‑8 bytes for © are 0xC2 0xA9
        byte[] expected = new byte[]{(byte)0xC2, (byte)0xA9};
        byte[] actual = new byte[byteBuf.remaining()];
        byteBuf.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToString_FromCharBuffer() {
        CharBuffer buffer = CharBuffer.wrap("hello world");
        String result = CharBufferConversions.toString(buffer, converter);
        assertEquals("hello world", result);
    }

    @Test
    public void testToString_Empty() {
        CharBuffer empty = CharBuffer.wrap("");
        assertEquals("", CharBufferConversions.toString(empty, converter));
    }

    @Test
    public void testToCharArray_NonEmpty() {
        CharBuffer buffer = CharBuffer.wrap("xyz");
        char[] chars = CharBufferConversions.toCharArray(buffer, converter);
        assertArrayEquals(new char[]{'x', 'y', 'z'}, chars);
    }

    @Test
    public void testToCharArray_EmptyReturnsConstant() {
        CharBuffer empty = CharBuffer.wrap("");
        char[] chars = CharBufferConversions.toCharArray(empty, converter);
        // The implementation returns the shared EMPTY_CHAR_ARRAY constant.
        assertSame(EMPTY_CHAR_ARRAY, chars);
    }

    @Test
    public void testToStringBuffer_Basic() {
        CharBuffer buffer = CharBuffer.wrap("builder");
        StringBuffer sb = CharBufferConversions.toStringBuffer(buffer, converter);
        assertEquals("builder", sb.toString());
        // Mutating the StringBuffer must not affect the original CharBuffer.
        sb.append("X");
        assertEquals("builderX", sb.toString());
        assertEquals("builder", buffer.toString());
    }

    @Test
    public void testToStringBuilder_Basic() {
        CharBuffer buffer = CharBuffer.wrap("builder");
        StringBuilder sb = CharBufferConversions.toStringBuilder(buffer, converter);
        assertEquals("builder", sb.toString());
        sb.append("Y");
        assertEquals("builderY", sb.toString());
        // Original buffer stays unchanged.
        assertEquals("builder", buffer.toString());
    }

    @Test
    public void testRoundTrip_CharBufferToByteArrayToCharArray() {
        CharBuffer original = CharBuffer.wrap("roundtrip");
        byte[] bytes = CharBufferConversions.toByteArray(original, converter);
        // Decode back to CharBuffer using the same charset.
        CharBuffer decoded = converter.getOptions().getCharset().decode(ByteBuffer.wrap(bytes));
        char[] chars = CharBufferConversions.toCharArray(decoded, converter);
        assertArrayEquals(original.array(), chars);
    }

    @Test
    public void testMultipleHyphensInBuffer_DoNotAlterContent() {
        CharBuffer buffer = CharBuffer.wrap("---");
        // The conversion methods never strip hyphens – they only treat the
        // CharBuffer as raw characters.
        assertEquals("---", CharBufferConversions.toString(buffer, converter));
        assertArrayEquals(new char[]{'-','-','-'}, CharBufferConversions.toCharArray(buffer, converter));
    }
}
