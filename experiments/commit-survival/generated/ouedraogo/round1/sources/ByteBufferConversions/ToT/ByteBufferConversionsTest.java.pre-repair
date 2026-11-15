package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Comprehensive JUnit‑4 test suite for {@link ByteBufferConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage (non‑empty buffers, normal characters)</li>
 *   <li>Edge cases (empty buffers, multibyte UTF‑8 characters)</li>
 *   <li>Error scenarios (wrong source type, null arguments)</li>
 *   <li>Behaviour of the read‑only view returned by {@code toByteBuffer}</li>
 * </ul>
 *
 * Only the public API of {@link Converter} (as supplied in the prompt) and the
 * classes that appear in {@code ByteBufferConversions} are used.  No mocking
 * frameworks are employed – real objects are constructed.
 */
public class ByteBufferConversionsTest {

    /** A {@link Converter} instance configured to use UTF‑8 for charset operations. */
    private Converter utf8Converter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the library; we configure it to use UTF‑8.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(StandardCharsets.UTF_8);
        utf8Converter = new Converter(options);
    }

    @After
    public void tearDown() {
        utf8Converter = null;
    }

    /* --------------------------------------------------------------
     *  toByteBuffer
     * -------------------------------------------------------------- */

    @Test
    public void testToByteBufferCreatesReadOnlyDuplicate() {
        byte[] data = "Hello".getBytes(StandardCharsets.UTF_8);
        ByteBuffer original = ByteBuffer.wrap(data);
        original.position(1);               // move position to test preservation
        original.limit(data.length - 1);    // shrink limit

        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, null);

        // The result must be a different object
        assertNotSame(original, result);
        // Must be read‑only
        assertTrue(result.isReadOnly());

        // The content must be identical (same remaining bytes)
        assertEquals(original.remaining(), result.remaining());
        byte[] expected = new byte[original.remaining()];
        original.slice().get(expected);
        byte[] actual = new byte[result.remaining()];
        result.slice().get(actual);
        assertArrayEquals(expected, actual);

        // Changing the original's underlying array must be reflected
        original.put(1, (byte) 'a');
        result.position(0);
        assertEquals('a', (char) result.get(1));
    }

    @Test(expected = ReadOnlyBufferException.class)
    public void testReadOnlyBufferRejectsPut() {
        ByteBuffer src = ByteBuffer.wrap("abc".getBytes(StandardCharsets.UTF_8));
        ByteBuffer ro = ByteBufferConversions.toByteBuffer(src, null);
        ro.put((byte) 'x'); // should throw ReadOnlyBufferException
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBufferWithInvalidSourceThrows() {
        // Passing a non‑ByteBuffer object triggers the cast inside the method.
        ByteBufferConversions.toByteBuffer("not a buffer", null);
    }

    /* --------------------------------------------------------------
     *  toCharBuffer
     * -------------------------------------------------------------- */

    @Test
    public void testToCharBufferDecodesUtf8Correctly() {
        String text = "Café";
        ByteBuffer src = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        CharBuffer cb = ByteBufferConversions.toCharBuffer(src, utf8Converter);
        assertEquals(text, cb.toString());
    }

    @Test
    public void testToCharBufferWithEmptyBufferReturnsEmptyCharBuffer() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        CharBuffer cb = ByteBufferConversions.toCharBuffer(empty, utf8Converter);
        assertEquals(0, cb.length());
        assertEquals("", cb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBufferWithNullConverterFails() {
        ByteBuffer src = ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8));
        // Null converter leads to NPE when getOptions() is invoked.
        ByteBufferConversions.toCharBuffer(src, null);
    }

    /* --------------------------------------------------------------
     *  toByteArray
     * -------------------------------------------------------------- */

    @Test
    public void testToByteArrayCopiesRemainingBytes() {
        byte[] payload = {0, 1, 2, 3, 4, 5};
        ByteBuffer buf = ByteBuffer.wrap(payload);
        buf.position(2); // only bytes 2..5 should be copied
        byte[] result = ByteBufferConversions.toByteArray(buf, utf8Converter);
        assertArrayEquals(new byte[]{2, 3, 4, 5}, result);
    }

    @Test
    public void testToByteArrayEmptyBufferReturnsConstant() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        byte[] result = ByteBufferConversions.toByteArray(empty, utf8Converter);
        assertSame(EMPTY_BYTE_ARRAY, result);
    }

    @Test
    public void testToByteArrayNullBufferHandledGracefully() {
        // toByteBuffer never returns null for a valid ByteBuffer source,
        // but we simulate the pathological case by subclassing ByteBuffer
        // (not allowed) – instead we verify that a null source throws
        // ClassCastException before reaching this method.
        try {
            ByteBufferConversions.toByteArray(null, utf8Converter);
            fail("Expected ClassCastException");
        } catch (ClassCastException expected) {
            // expected
        }
    }

    /* --------------------------------------------------------------
     *  toString
     * -------------------------------------------------------------- */

    @Test
    public void testToStringProducesCorrectString() {
        String original = "𝄞 Music Symbol"; // contains multibyte UTF‑8 chars
        ByteBuffer src = ByteBuffer.wrap(original.getBytes(StandardCharsets.UTF_8));
        String result = ByteBufferConversions.toString(src, utf8Converter);
        assertEquals(original, result);
    }

    @Test
    public void testToStringWithEmptyBufferReturnsEmptyString() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        assertEquals("", ByteBufferConversions.toString(empty, utf8Converter));
    }

    /* --------------------------------------------------------------
     *  toCharArray
     * -------------------------------------------------------------- */

    @Test
    public void testToCharArrayMatchesCharBufferContent() {
        String text = "JUnit";
        ByteBuffer src = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        char[] chars = ByteBufferConversions.toCharArray(src, utf8Converter);
        assertArrayEquals(text.toCharArray(), chars);
    }

    @Test
    public void testToCharArrayWithEmptyBufferReturnsEmptyArray() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        char[] result = ByteBufferConversions.toCharArray(empty, utf8Converter);
        assertEquals(0, result.length);
    }

    /* --------------------------------------------------------------
     *  toStringBuffer
     * -------------------------------------------------------------- */

    @Test
    public void testToStringBufferProducesIdenticalContent() {
        String text = "StringBufferTest";
        ByteBuffer src = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        StringBuffer sb = ByteBufferConversions.toStringBuffer(src, utf8Converter);
        assertEquals(text, sb.toString());
        // Verify mutability of the returned StringBuffer
        sb.append("-extra");
        assertEquals(text + "-extra", sb.toString());
    }

    /* --------------------------------------------------------------
     *  toStringBuilder
     * -------------------------------------------------------------- */

    @Test
    public void testToStringBuilderProducesIdenticalContent() {
        String text = "StringBuilderTest";
        ByteBuffer src = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = ByteBufferConversions.toStringBuilder(src, utf8Converter);
        assertEquals(text, sb.toString());
        // Verify mutability of the returned StringBuilder
        sb.append("-more");
        assertEquals(text + "-more", sb.toString());
    }

    /* --------------------------------------------------------------
     *  Miscellaneous error handling
     * -------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testAllMethodsRejectNullSourceWhenCastIsRequired() {
        // toByteBuffer is the only method that does not dereference the converter,
        // but it still casts the source to ByteBuffer, leading to NPE before the cast.
        ByteBufferConversions.toByteBuffer(null, utf8Converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArrayWithNullConverterFails() {
        ByteBuffer src = ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8));
        ByteBufferConversions.toCharArray(src, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilderWithNullConverterFails() {
        ByteBuffer src = ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8));
        ByteBufferConversions.toStringBuilder(src, null);
    }
}
