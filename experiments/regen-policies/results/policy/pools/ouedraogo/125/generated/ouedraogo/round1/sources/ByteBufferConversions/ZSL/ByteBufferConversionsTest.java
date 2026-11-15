package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ByteBufferConversions}.
 *
 * The tests are placed in the same package as {@code ByteBufferConversions}
 * so that the package‑private static methods are directly accessible.
 *
 * The {@link Converter} class is part of the public API of the library.
 * For the purpose of these tests we rely on the default behaviour of
 * {@code ConverterOptions#getCharset()}, which is UTF‑8 in the reference
 * implementation.  All byte‑to‑char conversions therefore use UTF‑8.
 */
public class ByteBufferConversionsTest {

    private Converter converter;
    private Charset charset;

    @Before
    public void setUp() {
        // The library provides a public constructor that accepts ConverterOptions.
        // We use the default options; the default charset is UTF‑8.
        converter = new Converter(new ConverterOptions());
        charset = converter.getOptions().getCharset();
        assertNotNull("Converter must provide a non‑null charset", charset);
    }

    /**
     * Verify that {@code toByteBuffer} returns a **read‑only** view of the original buffer,
     * that the view has the same remaining bytes and that modifications to the original
     * buffer are reflected in the view (because it is a view, not a copy).
     */
    @Test
    public void testToByteBufferReadOnlyView() {
        byte[] data = { 1, 2, 3, 4, 5 };
        ByteBuffer original = ByteBuffer.allocate(data.length);
        original.put(data);
        original.flip(); // prepare for reading (position = 0, limit = data.length)

        // Move the original position to test that the view respects the current position
        original.position(2);

        ByteBuffer view = ByteBufferConversions.toByteBuffer(original, converter);

        // The view must be a different object but share the same underlying data
        assertNotSame("View must be a different ByteBuffer instance", original, view);
        assertTrue("View must be read‑only", view.isReadOnly());

        // The view should have the same remaining bytes as the original at the time of conversion
        assertEquals("Remaining bytes must match", original.remaining(), view.remaining());

        // Content check – the bytes we read from the view must equal the tail of the original data
        byte[] expected = Arrays.copyOfRange(data, 2, data.length);
        byte[] actual = new byte[view.remaining()];
        view.get(actual);
        assertArrayEquals("View must contain the same remaining bytes as original", expected, actual);
    }

    /**
     * {@code toByteArray} should return a fresh byte array containing the remaining bytes
     * of the supplied buffer.  The original buffer's position should be advanced to its limit.
     */
    @Test
    public void testToByteArrayNonEmpty() {
        byte[] payload = "JUnit".getBytes(charset);
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        // Simulate a partially consumed buffer (position = 2)
        buffer.position(2);

        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);

        // Expected array contains only the remaining bytes ("nit")
        byte[] expected = Arrays.copyOfRange(payload, 2, payload.length);
        assertArrayEquals("toByteArray must return only the remaining bytes", expected, result);

        // The original buffer should now be exhausted (position == limit)
        assertFalse("Original buffer should have no remaining bytes after conversion", buffer.hasRemaining());
    }

    /**
     * When the supplied buffer has no remaining bytes, {@code toByteArray}
     * must return the constant {@code EMPTY_BYTE_ARRAY}.
     */
    @Test
    public void testToByteArrayEmpty() {
        ByteBuffer empty = ByteBuffer.allocate(0); // zero‑capacity buffer
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        assertSame("Empty buffer should return EMPTY_BYTE_ARRAY constant",
                com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY, result);
    }

    /**
     * Verify that {@code toCharBuffer} correctly decodes the bytes using the
     * charset provided by the converter's options.
     */
    @Test
    public void testToCharBufferDecoding() {
        String original = "Hello, 世界"; // contains ASCII and non‑ASCII characters
        byte[] bytes = original.getBytes(charset);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        CharBuffer charBuffer = ByteBufferConversions.toCharBuffer(buffer, converter);
        assertNotNull("CharBuffer must not be null", charBuffer);
        assertEquals("Decoded CharBuffer length must match original string length",
                original.length(), charBuffer.length());
        assertEquals("Decoded content must match original string", original, charBuffer.toString());
    }

    /**
     * {@code toString} is a thin wrapper around {@code toCharBuffer}.
     */
    @Test
    public void testToString() {
        String sample = "ByteBufferConversionsTest";
        ByteBuffer buffer = ByteBuffer.wrap(sample.getBytes(charset));

        String result = ByteBufferConversions.toString(buffer, converter);
        assertEquals("toString must produce the same string as the original", sample, result);
    }

    /**
     * {@code toCharArray} should produce a char array identical to the one
     * obtained from the original string.
     */
    @Test
    public void testToCharArray() {
        String text = "CharArray✓";
        ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(charset));

        char[] chars = ByteBufferConversions.toCharArray(buffer, converter);
        assertArrayEquals("Char array must match original string's chars",
                text.toCharArray(), chars);
    }

    /**
     * {@code toStringBuffer} must produce a {@link StringBuffer} whose content
     * equals the original string.
     */
    @Test
    public void testToStringBuffer() {
        String source = "StringBufferContent";
        ByteBuffer buffer = ByteBuffer.wrap(source.getBytes(charset));

        StringBuffer sb = ByteBufferConversions.toStringBuffer(buffer, converter);
        assertNotNull("StringBuffer must not be null", sb);
        assertEquals("StringBuffer content must match source string", source, sb.toString());
    }

    /**
     * {@code toStringBuilder} must produce a {@link StringBuilder} whose content
     * equals the original string.
     */
    @Test
    public void testToStringBuilder() {
        String source = "StringBuilderContent";
        ByteBuffer buffer = ByteBuffer.wrap(source.getBytes(charset));

        StringBuilder sb = ByteBufferConversions.toStringBuilder(buffer, converter);
        assertNotNull("StringBuilder must not be null", sb);
        assertEquals("StringBuilder content must match source string", source, sb.toString());
    }

    /**
     * Ensure that a buffer whose position is not zero is handled correctly by all
     * conversion methods.  The methods must operate only on the *remaining* bytes.
     */
    @Test
    public void testPartialBufferConversions() {
        String full = "PartialBufferTest";
        byte[] bytes = full.getBytes(charset);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        // Consume first 7 bytes ("Partial")
        buffer.position(7);

        // Expected remaining string is "BufferTest"
        String expected = full.substring(7);

        // toString
        assertEquals(expected, ByteBufferConversions.toString(buffer.asReadOnlyBuffer(), converter));

        // toCharArray
        assertArrayEquals(expected.toCharArray(),
                ByteBufferConversions.toCharArray(buffer.asReadOnlyBuffer(), converter));

        // toStringBuffer
        assertEquals(expected,
                ByteBufferConversions.toStringBuffer(buffer.asReadOnlyBuffer(), converter).toString());

        // toStringBuilder
        assertEquals(expected,
                ByteBufferConversions.toStringBuilder(buffer.asReadOnlyBuffer(), converter).toString());

        // toByteArray
        assertArrayEquals(expected.getBytes(charset),
                ByteBufferConversions.toByteArray(buffer.asReadOnlyBuffer(), converter));
    }
}
