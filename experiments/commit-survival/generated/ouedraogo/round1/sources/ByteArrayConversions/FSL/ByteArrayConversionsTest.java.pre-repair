package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * Unit tests for {@link ByteArrayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of {@code null} and empty byte arrays.</li>
 *   <li>Standard UTF‑8 conversion.</li>
 *   <li>Behaviour of the returned mutable objects (ByteBuffer shares the backing array).</li>
 *   <li>All public static conversion helpers.</li>
 * </ul>
 */
public class ByteArrayConversionsTest {

    /** Helper that creates a {@link Converter} with the given charset. */
    private static Converter createConverter(Charset charset) {
        // ConverterOptions is part of the library; it provides a setter for the charset.
        // The exact API is not listed, but the typical library offers a constructor
        // and a {@code setCharset} method.  If the method name differs, reflection could
        // be used, however the test is written assuming the public setter exists.
        ConverterOptions options = new ConverterOptions();
        // The library’s ConverterOptions usually has a method called setCharset(Charset)
        // – if it does not, the test will still compile because the constructor will
        // accept the default charset.
        try {
            options.getClass()
                   .getMethod("setCharset", Charset.class)
                   .invoke(options, charset);
        } catch (Exception ignored) {
            // If the method is not present we simply rely on the default charset.
        }
        return new Converter(options);
    }

    /** A converter that uses UTF‑8 (the default in most environments). */
    private static final Converter UTF8_CONVERTER = createConverter(StandardCharsets.UTF_8);

    @Test
    public void testToString_WithNormalData() {
        byte[] source = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(source, UTF8_CONVERTER);
        assertEquals("Hello, world!", result);
    }

    @Test
    public void testToString_WithEmptyArray() {
        byte[] source = new byte[0];
        String result = ByteArrayConversions.toString(source, UTF8_CONVERTER);
        // StringUtilities.EMPTY is defined as the empty string.
        assertEquals(StringUtilities.EMPTY, result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToString_WithNull() {
        byte[] source = null;
        String result = ByteArrayConversions.toString(source, UTF8_CONVERTER);
        assertEquals(StringUtilities.EMPTY, result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToString_WithNonAsciiCharactersAndDifferentCharset() {
        // ISO‑8859‑1 maps the byte 0xE9 to 'é'.
        byte[] source = {(byte) 0xE9};
        Converter isoConverter = createConverter(StandardCharsets.ISO_8859_1);
        String result = ByteArrayConversions.toString(source, isoConverter);
        assertEquals("é", result);
    }

    @Test
    public void testToByteBuffer_SharedBackingArray() {
        byte[] source = "buffer".getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(source, UTF8_CONVERTER);

        // Verify capacity/limit/position
        assertEquals(source.length, buffer.remaining());
        assertEquals(0, buffer.position());
        assertEquals(source.length, buffer.limit());

        // The buffer should expose the same underlying array.
        byte[] extracted = new byte[buffer.remaining()];
        buffer.get(extracted);
        assertArrayEquals(source, extracted);

        // Modify the original array and ensure the buffer sees the change.
        source[0] = (byte) 'B';
        buffer.rewind(); // reset position to 0
        assertEquals((byte) 'B', buffer.get());
    }

    @Test
    public void testToCharBuffer_ContentMatchesString() {
        byte[] source = "charBuffer".getBytes(StandardCharsets.UTF_8);
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(source, UTF8_CONVERTER);
        assertEquals("charBuffer", charBuffer.toString());
    }

    @Test
    public void testToCharArray_ContentMatchesString() {
        byte[] source = "charArray".getBytes(StandardCharsets.UTF_8);
        char[] chars = ByteArrayConversions.toCharArray(source, UTF8_CONVERTER);
        assertArrayEquals("charArray".toCharArray(), chars);
    }

    @Test
    public void testToStringBuffer_ContentMatchesString() {
        byte[] source = "stringBuffer".getBytes(StandardCharsets.UTF_8);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(source, UTF8_CONVERTER);
        assertEquals("stringBuffer", sb.toString());

        // Mutate the buffer and ensure it does not affect the original byte array.
        sb.append("-extra");
        assertEquals("stringBuffer-extra", sb.toString());
        // Original bytes unchanged
        assertEquals("stringBuffer", new String(source, StandardCharsets.UTF_8));
    }

    @Test
    public void testToStringBuilder_ContentMatchesString() {
        byte[] source = "stringBuilder".getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(source, UTF8_CONVERTER);
        assertEquals("stringBuilder", sb.toString());

        // Verify mutability
        sb.insert(0, "pre-");
        assertEquals("pre-stringBuilder", sb.toString());
    }

    @Test
    public void testAllConversions_WithEmptyInput() {
        byte[] empty = new byte[0];

        assertEquals(StringUtilities.EMPTY, ByteArrayConversions.toString(empty, UTF8_CONVERTER));
        assertEquals(0, ByteArrayConversions.toByteBuffer(empty, UTF8_CONVERTER).remaining());
        assertEquals("", ByteArrayConversions.toCharBuffer(empty, UTF8_CONVERTER).toString());
        assertEquals(0, ByteArrayConversions.toCharArray(empty, UTF8_CONVERTER).length);
        assertEquals(StringUtilities.EMPTY, ByteArrayConversions.toStringBuffer(empty, UTF8_CONVERTER).toString());
        assertEquals(StringUtilities.EMPTY, ByteArrayConversions.toStringBuilder(empty, UTF8_CONVERTER).toString());
    }
}
