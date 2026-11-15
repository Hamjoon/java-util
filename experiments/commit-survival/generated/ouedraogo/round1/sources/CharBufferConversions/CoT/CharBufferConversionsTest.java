package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

public class CharBufferConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a default ConverterOptions that includes a Charset.
        // We rely on the public constructor of Converter.
        ConverterOptions options = new ConverterOptions(); // assumes a public no‑arg ctor
        // If the library requires a charset, the default constructor will set UTF‑8.
        this.converter = new Converter(options);
    }

    /* -------------------------------------------------
       1. Normal behaviour tests
       ------------------------------------------------- */

    @Test
    public void testToCharBufferReturnsReadOnlyBuffer() {
        CharBuffer original = CharBuffer.wrap("hello");
        CharBuffer result = CharBufferConversions.toCharBuffer(original, converter);
        assertEquals("hello", result.toString());
        assertTrue(result.isReadOnly());

        // original buffer must stay at position 0
        assertEquals(0, original.position());
    }

    @Test
    public void testToByteArrayMatchesEncoding() {
        CharBuffer src = CharBuffer.wrap("abc");
        Charset cs = converter.getOptions().getCharset();
        byte[] expected = "abc".getBytes(cs);
        byte[] actual = CharBufferConversions.toByteArray(src, converter);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteBufferEncodesCorrectly() {
        CharBuffer src = CharBuffer.wrap("µ"); // micro sign, non‑ASCII
        Charset cs = converter.getOptions().getCharset();
        byte[] expected = "µ".getBytes(cs);

        ByteBuffer bb = CharBufferConversions.toByteBuffer(src, converter);
        byte[] actual = new byte[bb.remaining()];
        bb.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToStringReturnsRemainingCharacters() {
        CharBuffer src = CharBuffer.wrap("xyz");
        src.position(1); // remaining = "yz"
        String result = CharBufferConversions.toString(src, converter);
        assertEquals("yz", result);
    }

    @Test
    public void testToCharArrayEmptyBuffer() {
        CharBuffer src = CharBuffer.allocate(0);
        char[] result = CharBufferConversions.toCharArray(src, converter);
        assertEquals(0, result.length);
    }

    @Test
    public void testToCharArrayCopiesRemainingChars() {
        CharBuffer src = CharBuffer.wrap("12345");
        src.position(2); // remaining = "345"
        char[] result = CharBufferConversions.toCharArray(src, converter);
        assertArrayEquals(new char[]{'3', '4', '5'}, result);
    }

    @Test
    public void testToStringBufferContent() {
        CharBuffer src = CharBuffer.wrap("buffer");
        StringBuffer sb = CharBufferConversions.toStringBuffer(src, converter);
        assertEquals("buffer", sb.toString());
    }

    @Test
    public void testToStringBuilderContent() {
        CharBuffer src = CharBuffer.wrap("builder");
        StringBuilder sb = CharBufferConversions.toStringBuilder(src, converter);
        assertEquals("builder", sb.toString());
    }

    /* -------------------------------------------------
       2. Edge‑case / exception tests
       ------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToCharBufferWithWrongTypeThrows() {
        Integer notABuffer = 42;
        CharBufferConversions.toCharBuffer(notABuffer, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBufferWithNullFromThrows() {
        CharBufferConversions.toCharBuffer(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArrayWithNullConverterThrows() {
        CharBuffer src = CharBuffer.wrap("test");
        CharBufferConversions.toByteArray(src, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBufferWithNullConverterThrows() {
        CharBuffer src = CharBuffer.wrap("test");
        CharBufferConversions.toByteBuffer(src, null);
    }

    @Test
    public void testReadOnlyBufferCannotBeWritten() {
        CharBuffer src = CharBuffer.wrap("immutable");
        CharBuffer ro = CharBufferConversions.toCharBuffer(src, converter);
        try {
            ro.put('x');
            fail("Expected ReadOnlyBufferException");
        } catch (ReadOnlyBufferException expected) {
            // success
        }
    }

    @Test
    public void testToStringHandlesSurrogatePairs() {
        // U+1F600 GRINNING FACE (surrogate pair)
        String emoji = new StringBuilder().appendCodePoint(0x1F600).toString();
        CharBuffer src = CharBuffer.wrap(emoji);
        String result = CharBufferConversions.toString(src, converter);
        assertEquals(emoji, result);
    }
}
