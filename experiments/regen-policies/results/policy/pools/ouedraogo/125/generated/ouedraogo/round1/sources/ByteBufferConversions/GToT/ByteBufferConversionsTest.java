package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ByteBufferConversions}.
 *
 * The tests cover:
 *   • normal operation of every public static method,
 *   • edge cases such as empty buffers,
 *   • and error handling (e.g., passing a non‑ByteBuffer argument).
 *
 * The test class uses only the APIs that are visible in the supplied source
 * and the public {@code Converter} API.
 */
public class ByteBufferConversionsTest {

    private Converter converter;

    /** Build a Converter that uses UTF‑8 – the charset required for the decode step. */
    @Before
    public void setUp() {
        // ConverterOptions is not shown, but the library provides a default constructor
        // and a setter for the charset.  If the real API differs, adjust accordingly.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(StandardCharsets.UTF_8);
        converter = new Converter(options);
    }

    /** Helper – create a **read‑only** ByteBuffer from a String using UTF‑8 encoding. */
    private static ByteBuffer byteBufferFromString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        ByteBuffer mutable = ByteBuffer.wrap(bytes);
        // The class under test works with a *read‑only* view, so we emulate that here.
        return mutable.asReadOnlyBuffer();
    }

    /* -------------------------------------------------------------
       1. toByteBuffer – normal case
       ------------------------------------------------------------- */
    @Test
    public void testToByteBuffer_returnsReadOnlyCopyWithSameContent() {
        ByteBuffer src = ByteBuffer.allocate(10);
        src.put(new byte[]{1, 2, 3, 4, 5});
        src.flip(); // prepare for reading

        ByteBuffer result = ByteBufferConversions.toByteBuffer(src, converter);

        assertNotSame("Result should be a different instance", src, result);
        assertTrue("Result must be read‑only", result.isReadOnly());
        assertEquals("Remaining bytes must be equal", src.remaining(), result.remaining());

        // Verify the actual bytes
        byte[] expected = new byte[src.remaining()];
        src.get(expected);
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBuffer_withNonByteBuffer_throwsException() {
        ByteBufferConversions.toByteBuffer("not a buffer", converter);
    }

    /* -------------------------------------------------------------
       2. toCharBuffer – normal case
       ------------------------------------------------------------- */
    @Test
    public void testToCharBuffer_decodesUtf8Correctly() {
        String original = "Hello, 世界";
        ByteBuffer buffer = byteBufferFromString(original);

        CharBuffer chars = ByteBufferConversions.toCharBuffer(buffer, converter);
        assertEquals(original, chars.toString());
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_withNonByteBuffer_throwsException() {
        ByteBufferConversions.toCharBuffer(12345, converter);
    }

    /* -------------------------------------------------------------
       3. toByteArray – normal case
       ------------------------------------------------------------- */
    @Test
    public void testToByteArray_returnsExactBytes() {
        String text = "ByteArrayTest";
        ByteBuffer buffer = byteBufferFromString(text);

        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        assertArrayEquals(text.getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void testToByteArray_emptyBuffer_returnsEmptyArray() {
        ByteBuffer empty = ByteBuffer.allocate(0).asReadOnlyBuffer();
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        assertArrayEquals(com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteArray_withNonByteBuffer_throwsException() {
        ByteBufferConversions.toByteArray(new Object(), converter);
    }

    /* -------------------------------------------------------------
       4. toString – normal case
       ------------------------------------------------------------- */
    @Test
    public void testToString_returnsDecodedString() {
        String source = "JUnit4 Test";
        ByteBuffer buffer = byteBufferFromString(source);
        String result = ByteBufferConversions.toString(buffer, converter);
        assertEquals(source, result);
    }

    @Test
    public void testToString_emptyBuffer_returnsEmptyString() {
        ByteBuffer empty = ByteBuffer.allocate(0).asReadOnlyBuffer();
        assertEquals("", ByteBufferConversions.toString(empty, converter));
    }

    @Test(expected = ClassCastException.class)
    public void testToString_withNonByteBuffer_throwsException() {
        ByteBufferConversions.toString(Boolean.TRUE, converter);
    }

    /* -------------------------------------------------------------
       5. toCharArray – normal case
       ------------------------------------------------------------- */
    @Test
    public void testToCharArray_matchesCharSequence() {
        String source = "CharArray✓";
        ByteBuffer buffer = byteBufferFromString(source);
        char[] chars = ByteBufferConversions.toCharArray(buffer, converter);
        assertArrayEquals(source.toCharArray(), chars);
    }

    @Test
    public void testToCharArray_emptyBuffer_returnsEmptyArray() {
        ByteBuffer empty = ByteBuffer.allocate(0).asReadOnlyBuffer();
        char[] result = ByteBufferConversions.toCharArray(empty, converter);
        assertEquals(0, result.length);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_withNonByteBuffer_throwsException() {
        ByteBufferConversions.toCharArray(new int[]{1, 2, 3}, converter);
    }

    /* -------------------------------------------------------------
       6. toStringBuffer – normal case
       ------------------------------------------------------------- */
    @Test
    public void testToStringBuffer_containsCorrectData() {
        String source = "StringBuffer test";
        ByteBuffer buffer = byteBufferFromString(source);
        StringBuffer sb = ByteBufferConversions.toStringBuffer(buffer, converter);
        assertEquals(source, sb.toString());
    }

    @Test
    public void testToStringBuffer_emptyBuffer_isEmpty() {
        ByteBuffer empty = ByteBuffer.allocate(0).asReadOnlyBuffer();
        StringBuffer sb = ByteBufferConversions.toStringBuffer(empty, converter);
        assertEquals(0, sb.length());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_withNonByteBuffer_throwsException() {
        ByteBufferConversions.toStringBuffer(3.14, converter);
    }

    /* -------------------------------------------------------------
       7. toStringBuilder – normal case
       ------------------------------------------------------------- */
    @Test
    public void testToStringBuilder_containsCorrectData() {
        String source = "StringBuilder test";
        ByteBuffer buffer = byteBufferFromString(source);
        StringBuilder sb = ByteBufferConversions.toStringBuilder(buffer, converter);
        assertEquals(source, sb.toString());
    }

    @Test
    public void testToStringBuilder_emptyBuffer_isEmpty() {
        ByteBuffer empty = ByteBuffer.allocate(0).asReadOnlyBuffer();
        StringBuilder sb = ByteBufferConversions.toStringBuilder(empty, converter);
        assertEquals(0, sb.length());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_withNonByteBuffer_throwsException() {
        ByteBufferConversions.toStringBuilder(new Object(), converter);
    }
}
---  
