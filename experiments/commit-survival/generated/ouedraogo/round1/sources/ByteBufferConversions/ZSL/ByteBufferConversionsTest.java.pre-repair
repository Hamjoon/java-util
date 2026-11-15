package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;
import static org.junit.Assert.*;

public class ByteBufferConversionsTest {

    private Converter converter;
    private Charset charset;

    @Before
    public void setUp() {
        // The real ConverterOptions class is not shown, but it provides a getCharset() method.
        // We rely on the default behaviour (UTF-8) for the tests.
        converter = new Converter(new ConverterOptions());
        charset = converter.getOptions().getCharset();
    }

    @Test
    public void testToByteBufferCreatesReadOnlyBufferAndPreservesOriginalState() {
        byte[] data = "TestData".getBytes(charset);
        ByteBuffer original = ByteBuffer.wrap(data);
        original.position(2).limit(data.length - 2); // set a non‑default position/limit

        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);

        // Result must be read‑only
        assertTrue(result.isReadOnly());

        // Result must have the same remaining bytes as the original slice
        assertEquals(original.remaining(), result.remaining());

        // Original buffer's position and limit must be unchanged
        assertEquals(2, original.position());
        assertEquals(data.length - 2, original.limit());

        // The content of the read‑only buffer must match the original slice
        byte[] expected = Arrays.copyOfRange(data, 2, data.length - 2);
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArrayReturnsCorrectBytes() {
        byte[] data = "Hello ByteBuffer".getBytes(charset);
        ByteBuffer buffer = ByteBuffer.wrap(data);

        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        assertArrayEquals(data, result);
    }

    @Test
    public void testToByteArrayWhenNoRemainingReturnsEmptyArray() {
        byte[] data = "NoRemaining".getBytes(charset);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        // Move position to limit so there is nothing remaining
        buffer.position(buffer.limit());

        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        assertSame(EMPTY_BYTE_ARRAY, result);
    }

    @Test
    public void testToCharBufferDecodesUsingConverterCharset() {
        String original = "CharsetTest \u00A9";
        ByteBuffer buffer = ByteBuffer.wrap(original.getBytes(charset));

        CharBuffer charBuffer = ByteBufferConversions.toCharBuffer(buffer, converter);
        assertEquals(original, charBuffer.toString());
    }

    @Test
    public void testToStringProducesCorrectString() {
        String original = "StringConversion";
        ByteBuffer buffer = ByteBuffer.wrap(original.getBytes(charset));

        String result = ByteBufferConversions.toString(buffer, converter);
        assertEquals(original, result);
    }

    @Test
    public void testToCharArrayProducesCorrectCharArray() {
        String original = "CharArrayTest";
        ByteBuffer buffer = ByteBuffer.wrap(original.getBytes(charset));

        char[] chars = ByteBufferConversions.toCharArray(buffer, converter);
        assertArrayEquals(original.toCharArray(), chars);
    }

    @Test
    public void testToStringBufferCreatesEquivalentContent() {
        String original = "StringBufferContent";
        ByteBuffer buffer = ByteBuffer.wrap(original.getBytes(charset));

        StringBuffer sb = ByteBufferConversions.toStringBuffer(buffer, converter);
        assertEquals(original, sb.toString());
    }

    @Test
    public void testToStringBuilderCreatesEquivalentContent() {
        String original = "StringBuilderContent";
        ByteBuffer buffer = ByteBuffer.wrap(original.getBytes(charset));

        StringBuilder sb = ByteBufferConversions.toStringBuilder(buffer, converter);
        assertEquals(original, sb.toString());
    }
}
