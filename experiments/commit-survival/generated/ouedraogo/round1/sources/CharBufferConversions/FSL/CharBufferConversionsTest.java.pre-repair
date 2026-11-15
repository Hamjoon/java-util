package com.cedarsoftware.util.convert;

import org.junit.Test;

import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

public class CharBufferConversionsTest {

    /** Helper to create a Converter with a known charset (UTF‑8). */
    private static Converter createUtf8Converter() {
        // ConverterOptions is part of the library; it has a default constructor
        // and a setter for the charset.  Use the public API that is available.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(Charset.forName("UTF-8"));
        return new Converter(options);
    }

    @Test
    public void testToCharBufferReturnsReadOnlyBufferAndDoesNotAffectOriginal() {
        Converter converter = createUtf8Converter();

        CharBuffer original = CharBuffer.wrap(new char[]{'a', 'b', 'c', 'd'});
        original.position(1);          // set a non‑zero position
        original.limit(3);             // limit to a subset

        CharBuffer result = CharBufferConversions.toCharBuffer(original, converter);

        // result must be read‑only
        assertTrue(result.isReadOnly());

        // result must contain the same characters as the original buffer's *remaining* view
        assertEquals(2, result.remaining());
        assertEquals('b', result.get());
        assertEquals('c', result.get());

        // original buffer's position and limit must be unchanged
        assertEquals(1, original.position());
        assertEquals(3, original.limit());
    }

    @Test
    public void testToCharBufferWithEmptyBuffer() {
        Converter converter = createUtf8Converter();

        CharBuffer empty = CharBuffer.allocate(0);
        CharBuffer result = CharBufferConversions.toCharBuffer(empty, converter);

        assertTrue(result.isReadOnly());
        assertEquals(0, result.remaining());
    }

    @Test
    public void testToStringProducesCorrectString() {
        Converter converter = createUtf8Converter();

        CharBuffer buf = CharBuffer.wrap("Hello World".toCharArray());
        String str = CharBufferConversions.toString(buf, converter);

        assertEquals("Hello World", str);
    }

    @Test
    public void testToCharArrayCopiesRemainingCharacters() {
        Converter converter = createUtf8Converter();

        CharBuffer buf = CharBuffer.wrap("abcdef".toCharArray());
        buf.position(2); // remaining should be "cdef"
        char[] chars = CharBufferConversions.toCharArray(buf, converter);

        assertArrayEquals(new char[]{'c', 'd', 'e', 'f'}, chars);
    }

    @Test
    public void testToCharArrayEmptyBufferReturnsSharedEmptyArray() {
        Converter converter = createUtf8Converter();

        CharBuffer empty = CharBuffer.allocate(0);
        char[] chars = CharBufferConversions.toCharArray(empty, converter);

        // The implementation returns EMPTY_CHAR_ARRAY – we can only verify it is empty.
        assertEquals(0, chars.length);
    }

    @Test
    public void testToStringBufferCreatesCorrectBuffer() {
        Converter converter = createUtf8Converter();

        CharBuffer buf = CharBuffer.wrap("buffer".toCharArray());
        StringBuffer sb = CharBufferConversions.toStringBuffer(buf, converter);

        assertEquals("buffer", sb.toString());
    }

    @Test
    public void testToStringBuilderCreatesCorrectBuilder() {
        Converter converter = createUtf8Converter();

        CharBuffer buf = CharBuffer.wrap("builder".toCharArray());
        StringBuilder sb = CharBufferConversions.toStringBuilder(buf, converter);

        assertEquals("builder", sb.toString());
    }

    @Test
    public void testToByteBufferEncodingMatchesCharset() {
        Converter converter = createUtf8Converter();

        CharBuffer buf = CharBuffer.wrap("π".toCharArray()); // non‑ASCII character
        ByteBuffer byteBuf = CharBufferConversions.toByteBuffer(buf, converter);

        // UTF‑8 encoding of π is three bytes: 0xCF 0x80
        byte[] expected = new byte[]{(byte)0xCF, (byte)0x80};
        byte[] actual = new byte[byteBuf.remaining()];
        byteBuf.get(actual);

        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArrayUsesByteBufferConversions() {
        Converter converter = createUtf8Converter();

        CharBuffer buf = CharBuffer.wrap("test".toCharArray());
        byte[] bytes = CharBufferConversions.toByteArray(buf, converter);

        // "test" in UTF‑8 is the same as its ASCII bytes
        byte[] expected = "test".getBytes(Charset.forName("UTF-8"));
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testToByteArrayEmptyCharBufferReturnsEmptyArray() {
        Converter converter = createUtf8Converter();

        CharBuffer empty = CharBuffer.allocate(0);
        byte[] bytes = CharBufferConversions.toByteArray(empty, converter);

        assertEquals(0, bytes.length);
    }

    @Test
    public void testAllConversionsPreserveOriginalContentEvenWhenBufferHasMark() {
        Converter converter = createUtf8Converter();

        CharBuffer buf = CharBuffer.wrap("markTest".toCharArray());
        buf.position(3);
        buf.mark(); // mark at position 3

        CharBuffer result = CharBufferConversions.toCharBuffer(buf, converter);

        // The result must start from the original position (3) and include remaining chars
        assertEquals("kTest".length(), result.remaining());
        assertEquals('k', result.get());
        assertEquals('T', result.get());

        // Original buffer's position and mark must stay untouched
        assertEquals(3, buf.position());
        buf.reset(); // should succeed because mark is still present
        assertEquals(3, buf.position());
    }

    @Test
    public void testToCharArrayDoesNotModifyOriginalBufferPosition() {
        Converter converter = createUtf8Converter();

        CharBuffer buf = CharBuffer.wrap("position".toCharArray());
        buf.position(2);
        CharBufferConversions.toCharArray(buf, converter);

        // After conversion, original buffer's position must remain unchanged
        assertEquals(2, buf.position());
    }
}
