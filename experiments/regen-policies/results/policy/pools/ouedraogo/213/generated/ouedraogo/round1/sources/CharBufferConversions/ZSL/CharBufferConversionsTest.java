package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.convert.MapConversions.VALUE;

/**
 * Unit tests for {@link CharBufferConversions}.
 * <p>
 * The tests cover every public static method in the class and verify
 * correct handling of normal, edge‑case and empty inputs.
 * Only APIs that are present in the supplied source or the listed
 * {@code Converter} API are used – no additional libraries or mocking
 * frameworks are required.
 */
public class CharBufferConversionsTest {

    private Converter converter;
    private Charset charset;

    @Before
    public void setUp() {
        // ConverterOptions is not shown, but it is part of the library.
        // It provides a Charset via getCharset().  The no‑arg constructor
        // creates an options instance with a default charset (UTF‑8 on the
        // reference implementation).  This is sufficient for the tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        charset = options.getCharset();
    }

    @Test
    public void testToCharBufferCreatesReadOnlyCopyAndPreservesState() {
        CharBuffer original = CharBuffer.allocate(10);
        original.put("abcde");
        original.flip();            // limit = 5, position = 0
        original.position(2);        // remaining = "cde"

        CharBuffer result = CharBufferConversions.toCharBuffer(original, converter);

        // The result must be a read‑only buffer
        assertTrue("Resulting CharBuffer should be read‑only", result.isReadOnly());

        // The content of the result must equal the remaining characters of the original
        assertEquals("cde", result.toString());

        // The original buffer's position must be unchanged
        assertEquals(2, original.position());
        assertEquals(5, original.limit());
    }

    @Test
    public void testToStringReturnsFullRemainingString() {
        CharBuffer buf = CharBuffer.wrap("hello world".toCharArray());
        buf.position(6); // remaining = "world"
        String result = CharBufferConversions.toString(buf, converter);
        assertEquals("world", result);
    }

    @Test
    public void testToStringBufferCreatesEquivalentStringBuffer() {
        CharBuffer buf = CharBuffer.wrap("buffer test".toCharArray());
        StringBuffer sb = CharBufferConversions.toStringBuffer(buf, converter);
        assertEquals("buffer test", sb.toString());
    }

    @Test
    public void testToStringBuilderCreatesEquivalentStringBuilder() {
        CharBuffer buf = CharBuffer.wrap("builder test".toCharArray());
        StringBuilder sb = CharBufferConversions.toStringBuilder(buf, converter);
        assertEquals("builder test", sb.toString());
    }

    @Test
    public void testToCharArrayReturnsRemainingCharacters() {
        CharBuffer buf = CharBuffer.wrap("charArray".toCharArray());
        buf.position(4); // remaining = "Array"
        char[] result = CharBufferConversions.toCharArray(buf, converter);
        assertArrayEquals(new char[] {'A','r','r','a','y'}, result);
        // original buffer position must stay unchanged
        assertEquals(4, buf.position());
    }

    @Test
    public void testToCharArrayWithEmptyBufferReturnsEmptyArray() {
        CharBuffer empty = CharBuffer.allocate(0);
        char[] result = CharBufferConversions.toCharArray(empty, converter);
        assertEquals(0, result.length);
    }

    @Test
    public void testToByteBufferEncodesUsingConverterCharset() {
        String text = "Encoding test ☺";
        CharBuffer buf = CharBuffer.wrap(text);
        ByteBuffer byteBuf = CharBufferConversions.toByteBuffer(buf, converter);

        // Decode the bytes using the same charset and compare
        String decoded = charset.decode(byteBuf).toString();
        assertEquals(text, decoded);
    }

    @Test
    public void testToByteArrayMatchesByteBufferContent() {
        String text = "Byte array verification";
        CharBuffer buf = CharBuffer.wrap(text);
        byte[] byteArray = CharBufferConversions.toByteArray(buf, converter);
        ByteBuffer byteBuffer = CharBufferConversions.toByteBuffer(buf, converter);

        // The ByteBuffer returned by toByteBuffer is positioned at 0 after encoding.
        byte[] bufferBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bufferBytes);

        assertArrayEquals(bufferBytes, byteArray);
    }

    @Test
    public void testToMapContainsSingleEntryWithCorrectKeyAndValue() {
        CharBuffer buf = CharBuffer.wrap("map value".toCharArray());
        Map<?, ?> map = CharBufferConversions.toMap(buf, converter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(VALUE));
        assertEquals("map value", map.get(VALUE));
    }
}
