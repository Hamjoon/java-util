package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * JUnit 4 test suite for {@link ByteBufferConversions}.
 *
 * The tests are placed in the same package as the class under test
 * because all methods are package‑private static.
 *
 * The {@link Converter} instance is created with default {@link ConverterOptions}
 * (the library provides a no‑arg constructor). The default options use the
 * platform charset which is sufficient for the test data (ASCII characters only).
 */
public class ByteBufferConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library supplies a no‑arg constructor for ConverterOptions.
        // Using it creates a Converter with a default Charset (UTF‑8 on most platforms).
        ConverterOptions options = new ConverterOptions(); // assume public no‑arg ctor
        converter = new Converter(options);
    }

    @After
    public void tearDown() {
        converter = null;
    }

    /* --------------------------------------------------------------
     * Helper utilities
     * -------------------------------------------------------------- */

    /** Creates a ByteBuffer containing the supplied bytes and rewinds it. */
    private static ByteBuffer wrap(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data);
        bb.rewind(); // position = 0, limit = data.length
        return bb;
    }

    /** Returns a string representation of the given byte array using the
     *  converter's charset (UTF‑8 in our tests). */
    private String bytesToString(byte[] bytes) {
        Charset cs = converter.getOptions().getCharset();
        return new String(bytes, cs);
    }

    /* --------------------------------------------------------------
     * Public method signatures (extracted)
     *
     * static CharBuffer toCharBuffer(Object from, Converter converter)
     * static ByteBuffer toByteBuffer(Object from, Converter converter)
     * static byte[]     toByteArray(Object from, Converter converter)
     * static String     toString(Object from, Converter converter)
     * static char[]     toCharArray(Object from, Converter converter)
     * static StringBuffer toStringBuffer(Object from, Converter converter)
     * static StringBuilder toStringBuilder(Object from, Converter converter)
     * -------------------------------------------------------------- */

    /* --------------------------------------------------------------
     * Basic functional tests
     * -------------------------------------------------------------- */

    @Test
    public void testToByteBufferReturnsReadOnlyBufferAndPreservesPosition() {
        byte[] data = {1, 2, 3, 4, 5};
        ByteBuffer original = wrap(data);
        original.position(2);   // set a non‑zero position
        original.mark();        // mark at position 2

        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);

        // Result must be a read‑only view
        Assert.assertTrue("Result buffer should be read‑only", result.isReadOnly());

        // Original buffer's position and mark must be unchanged
        Assert.assertEquals("Original position must stay unchanged", 2, original.position());
        original.reset(); // should not throw because mark is still there
        Assert.assertEquals("Original position after reset", 2, original.position());

        // Result buffer should have the same remaining bytes as the original at the time of conversion
        Assert.assertEquals("Remaining bytes must match", original.remaining(), result.remaining());

        // Verify the content is identical
        byte[] expected = new byte[original.remaining()];
        original.slice().get(expected);
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        Assert.assertArrayEquals("Byte content must be identical", expected, actual);
    }

    @Test
    public void testToByteArrayEmptyBuffer() {
        ByteBuffer empty = wrap(new byte[0]);
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        Assert.assertNotNull("Result must not be null", result);
        Assert.assertEquals("Empty buffer should yield empty array", 0, result.length);
    }

    @Test
    public void testToByteArrayRemainingBytes() {
        byte[] data = {10, 20, 30, 40, 50};
        ByteBuffer buffer = wrap(data);
        buffer.position(1); // leave first byte un‑read
        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        byte[] expected = {20, 30, 40, 50};
        Assert.assertArrayEquals("Only remaining bytes should be returned", expected, result);
    }

    @Test
    public void testToStringEmpty() {
        ByteBuffer empty = wrap(new byte[0]);
        String result = ByteBufferConversions.toString(empty, converter);
        Assert.assertEquals("Empty buffer should produce empty string", "", result);
    }

    @Test
    public void testToStringContent() {
        String original = "Hello ByteBuffer!";
        byte[] bytes = original.getBytes(converter.getOptions().getCharset());
        ByteBuffer buffer = wrap(bytes);
        String result = ByteBufferConversions.toString(buffer, converter);
        Assert.assertEquals("String conversion must match original", original, result);
    }

    @Test
    public void testToCharArrayContent() {
        String original = "CharArrayTest";
        byte[] bytes = original.getBytes(converter.getOptions().getCharset());
        ByteBuffer buffer = wrap(bytes);
        char[] result = ByteBufferConversions.toCharArray(buffer, converter);
        char[] expected = original.toCharArray();
        Assert.assertArrayEquals("Char array must match original characters", expected, result);
    }

    @Test
    public void testToStringBufferContent() {
        String original = "StringBuffer Content";
        ByteBuffer buffer = wrap(original.getBytes(converter.getOptions().getCharset()));
        StringBuffer sb = ByteBufferConversions.toStringBuffer(buffer, converter);
        Assert.assertEquals("StringBuffer content must match original", original, sb.toString());
    }

    @Test
    public void testToStringBuilderContent() {
        String original = "StringBuilder Content";
        ByteBuffer buffer = wrap(original.getBytes(converter.getOptions().getCharset()));
        StringBuilder sb = ByteBufferConversions.toStringBuilder(buffer, converter);
        Assert.assertEquals("StringBuilder content must match original", original, sb.toString());
    }

    /* --------------------------------------------------------------
     * Edge‑case & exception tests
     * -------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToByteBufferWithNullArgumentThrowsNPE() {
        ByteBufferConversions.toByteBuffer(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBufferWithInvalidObjectThrowsClassCastException() {
        // Pass a String instead of ByteBuffer – the method casts to ByteBuffer.
        ByteBufferConversions.toByteBuffer("Not a ByteBuffer", converter);
    }

    @Test
    public void testToByteArrayWhenBufferHasNoRemainingReturnsEmptyArray() {
        byte[] data = {1, 2, 3};
        ByteBuffer buffer = wrap(data);
        buffer.position(data.length); // move position to limit => no remaining
        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        Assert.assertArrayEquals("When no remaining bytes, should return EMPTY_BYTE_ARRAY",
                new byte[0], result);
    }

    @Test
    public void testToCharBufferDoesNotAlterOriginalBufferState() {
        String text = "ImmutableTest";
        ByteBuffer original = wrap(text.getBytes(converter.getOptions().getCharset()));
        original.position(3);
        original.mark();

        CharBuffer charBuf = ByteBufferConversions.toCharBuffer(original, converter);

        // Original position and mark must stay the same
        Assert.assertEquals("Original position unchanged", 3, original.position());
        original.reset(); // must succeed
        Assert.assertEquals("Original position after reset unchanged", 3, original.position());

        // CharBuffer content must be the full string (decode starts at buffer's position 0)
        Assert.assertEquals("Decoded CharBuffer must equal the original text",
                text, charBuf.toString());
    }
}
