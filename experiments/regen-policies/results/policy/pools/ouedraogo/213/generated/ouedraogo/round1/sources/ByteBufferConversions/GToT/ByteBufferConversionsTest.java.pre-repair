package com.cedarsoftware.util.convert;

import org.junit.*;
import static org.junit.Assert.*;

import java.nio.*;
import java.util.Base64;
import java.util.Map;

/**
 * Comprehensive JUnit‑4 test suite for {@code ByteBufferConversions}.
 *
 * The tests are written in the same package as the class under test so that the
 * package‑private static methods can be accessed directly.
 *
 * The suite covers:
 * <ul>
 *   <li>basic happy‑path functionality of each conversion method</li>
 *   <li>edge‑case handling such as empty buffers, non‑zero positions and
 *       read‑only buffers</li>
 *   <li>exception scenarios (e.g. {@code null} arguments)</li>
 *   <li>behaviour of {@code toMap} for both array‑backed and direct buffers,
 *       including position restoration</li>
 * </ul>
 */
public class ByteBufferConversionsTest {

    /** A shared {@code Converter} instance used by all tests. */
    private static Converter converter;

    @BeforeClass
    public static void initConverter() {
        // {@code ConverterOptions} has a no‑arg constructor in the library.
        // It defaults to UTF‑8 charset which is sufficient for our tests.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------------- */
    /*  toByteBuffer                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteBufferReturnsReadOnlyView() {
        byte[] data = {1, 2, 3, 4};
        ByteBuffer mutable = ByteBuffer.wrap(data);
        ByteBuffer result = ByteBufferConversions.toByteBuffer(mutable, converter);

        assertTrue("Result must be a read‑only buffer", result.isReadOnly());
        // The underlying data must be the same (no copy)
        assertEquals("Capacity must be unchanged", mutable.capacity(), result.capacity());
        // Changing the original buffer must be visible through the read‑only view
        mutable.put(0, (byte) 9);
        assertEquals("View must reflect changes made to the original buffer", (byte) 9, result.get(0));
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBufferWithNull() {
        ByteBufferConversions.toByteBuffer(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  toCharBuffer                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharBufferDecodesUtf8Correctly() {
        String original = "Hello, 世界";
        ByteBuffer bytes = ByteBuffer.wrap(original.getBytes(converter.getOptions().getCharset()));
        CharBuffer chars = ByteBufferConversions.toCharBuffer(bytes, converter);
        assertEquals("Decoded characters must match original string", original, chars.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBufferWithNull() {
        ByteBufferConversions.toCharBuffer(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  toByteArray                                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteArrayCopiesRemainingBytes() {
        byte[] source = {10, 20, 30, 40, 50};
        ByteBuffer buffer = ByteBuffer.wrap(source);
        buffer.position(1);
        buffer.limit(4); // remaining = {20,30,40}
        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        assertArrayEquals("Result must contain only the remaining slice", new byte[]{20, 30, 40}, result);
    }

    @Test
    public void testToByteArrayEmptyBufferReturnsEmptyArray() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        assertEquals("Empty buffer must return empty array", 0, result.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArrayWithNull() {
        ByteBufferConversions.toByteArray(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  toString                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringProducesSameString() {
        String text = "JUnit‑4 Test";
        ByteBuffer buf = ByteBuffer.wrap(text.getBytes(converter.getOptions().getCharset()));
        String result = ByteBufferConversions.toString(buf, converter);
        assertEquals(text, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringWithNull() {
        ByteBufferConversions.toString(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  toCharArray                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharArrayMatchesOriginal() {
        String text = "CharArray";
        ByteBuffer buf = ByteBuffer.wrap(text.getBytes(converter.getOptions().getCharset()));
        char[] chars = ByteBufferConversions.toCharArray(buf, converter);
        assertArrayEquals("Char array must equal original characters",
                text.toCharArray(), chars);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArrayWithNull() {
        ByteBufferConversions.toCharArray(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  toStringBuffer                                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBufferContents() {
        String text = "StringBuffer";
        ByteBuffer buf = ByteBuffer.wrap(text.getBytes(converter.getOptions().getCharset()));
        StringBuffer sb = ByteBufferConversions.toStringBuffer(buf, converter);
        assertEquals(text, sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBufferWithNull() {
        ByteBufferConversions.toStringBuffer(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  toStringBuilder                                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilderContents() {
        String text = "StringBuilder";
        ByteBuffer buf = ByteBuffer.wrap(text.getBytes(converter.getOptions().getCharset()));
        StringBuilder sb = ByteBufferConversions.toStringBuilder(buf, converter);
        assertEquals(text, sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilderWithNull() {
        ByteBufferConversions.toStringBuilder(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  toMap                                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToMapArrayBackedBufferEncodesCorrectly() {
        byte[] data = "Base64Test".getBytes(converter.getOptions().getCharset());
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(2); // slice starts at index 2
        buffer.limit(8);    // length = 6 bytes (indices 2‑7)

        Map<?, ?> result = ByteBufferConversions.toMap(buffer, converter);
        String expectedBase64 = Base64.getEncoder().encodeToString(
                java.util.Arrays.copyOfRange(data, 2, 8));

        assertEquals("Map must contain exactly one entry", 1, result.size());
        assertEquals("Encoded value must match Base64 of the slice",
                expectedBase64, result.get(VALUE));
        // Original position must be unchanged
        assertEquals("Original position must be restored after toMap",
                2, buffer.position());
    }

    @Test
    public void testToMapDirectBufferEncodesCorrectlyAndRestoresPosition() {
        byte[] data = "DirectBuffer".getBytes(converter.getOptions().getCharset());
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length);
        direct.put(data);
        direct.flip();                 // position = 0, limit = data.length
        direct.position(3);           // create a slice starting at index 3
        direct.limit(9);              // length = 6 bytes (3‑8)

        int originalPos = direct.position();
        Map<?, ?> result = ByteBufferConversions.toMap(direct, converter);
        String expectedBase64 = Base64.getEncoder().encodeToString(
                java.util.Arrays.copyOfRange(data, 3, 9));

        assertEquals(expectedBase64, result.get(VALUE));
        assertEquals("Position must be restored after encoding", originalPos, direct.position());
    }

    @Test
    public void testToMapEmptyBufferProducesEmptyBase64() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        Map<?, ?> result = ByteBufferConversions.toMap(empty, converter);
        assertEquals("", result.get(VALUE));
    }

    @Test(expected = NullPointerException.class)
    public void testToMapWithNull() {
        ByteBufferConversions.toMap(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  Utility: ensure that the static constant VALUE is imported correctly */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMapKeyConstant() {
        // The constant is defined in MapConversions as "value".  Verify that the
        // test uses the same key that the implementation puts into the map.
        assertEquals("value", VALUE);
    }
}
