package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;

/**
 * JUnit‑4 test suite for {@link ByteBufferConversions}.
 *
 * The tests cover:
 *   1. Normal (happy‑path) usage of each public static method.
 *   2. Edge‑case scenarios such as empty buffers, read‑only behaviour,
 *      and illegal argument handling.
 *
 * The test class resides in the same package as {@code ByteBufferConversions}
 * so that the package‑private static methods are directly accessible.
 */
public class ByteBufferConversionsTest {

    private Converter converter;
    private Charset charset;

    @Before
    public void setUp() {
        // The real library supplies a default ConverterOptions that uses UTF‑8.
        // Creating a Converter with default options is sufficient for the
        // decoding operations performed in ByteBufferConversions.
        converter = new Converter(new ConverterOptions());
        charset = converter.getOptions().getCharset(); // expected UTF‑8
    }

    /* --------------------------------------------------------------------- */
    /*  1. toByteBuffer                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteBufferReturnsReadOnlyCopy() {
        byte[] data = {0x01, 0x02, 0x03, 0x04};
        ByteBuffer original = ByteBuffer.wrap(data);
        original.position(1);   // change position to ensure the returned buffer is independent
        original.limit(3);      // limit to two bytes (positions 1 and 2)

        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);

        // Result must be a read‑only buffer
        assertTrue("Resulting buffer should be read‑only", result.isReadOnly());

        // Result must have the same remaining bytes as the original slice (2 bytes)
        assertEquals(2, result.remaining());

        // The original buffer's position/limit must be unchanged after the call
        assertEquals(1, original.position());
        assertEquals(3, original.limit());

        // Verify content
        byte[] expected = Arrays.copyOfRange(data, 1, 3);
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBufferWithNonByteBufferThrows() {
        // Passing an object that is not a ByteBuffer should trigger a ClassCastException
        ByteBufferConversions.toByteBuffer("not a buffer", converter);
    }

    /* --------------------------------------------------------------------- */
    /*  2. toByteArray                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteArrayCopiesAllRemainingBytes() {
        byte[] data = {10, 20, 30, 40, 50};
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(2); // leave first two bytes untouched

        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        byte[] expected = {30, 40, 50};

        assertArrayEquals(expected, result);
    }

    @Test
    public void testToByteArrayEmptyBufferReturnsEmptyConstant() {
        ByteBuffer empty = ByteBuffer.allocate(0); // zero‑capacity buffer
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        assertSame("Empty buffers must return the shared EMPTY_BYTE_ARRAY constant",
                EMPTY_BYTE_ARRAY, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteArrayWithInvalidSourceThrows() {
        ByteBufferConversions.toByteArray(12345, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  3. toCharBuffer                                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharBufferDecodesUsingConverterCharset() {
        // UTF‑8 encoding of "ABC"
        byte[] utf8 = "ABC".getBytes(charset);
        ByteBuffer buffer = ByteBuffer.wrap(utf8);

        CharBuffer charBuf = ByteBufferConversions.toCharBuffer(buffer, converter);
        assertEquals("ABC", charBuf.toString());
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBufferWithNonByteBufferThrows() {
        ByteBufferConversions.toCharBuffer(new Object(), converter);
    }

    /* --------------------------------------------------------------------- */
    /*  4. toString                                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringProducesCorrectString() {
        String original = "Hello 世界"; // contains ASCII and non‑ASCII characters
        ByteBuffer buffer = ByteBuffer.wrap(original.getBytes(charset));

        String result = ByteBufferConversions.toString(buffer, converter);
        assertEquals(original, result);
    }

    /* --------------------------------------------------------------------- */
    /*  5. toCharArray                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharArrayMatchesStringCharArray() {
        String text = "JUnit4";
        ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(charset));

        char[] result = ByteBufferConversions.toCharArray(buffer, converter);
        assertArrayEquals(text.toCharArray(), result);
    }

    /* --------------------------------------------------------------------- */
    /*  6. toStringBuffer                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBufferContent() {
        String payload = "StringBufferTest";
        ByteBuffer buffer = ByteBuffer.wrap(payload.getBytes(charset));

        StringBuffer sb = ByteBufferConversions.toStringBuffer(buffer, converter);
        assertEquals(payload, sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  7. toStringBuilder                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilderContent() {
        String payload = "StringBuilderTest";
        ByteBuffer buffer = ByteBuffer.wrap(payload.getBytes(charset));

        StringBuilder sb = ByteBufferConversions.toStringBuilder(buffer, converter);
        assertEquals(payload, sb.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  8. Verify that original buffer position is not altered by any method   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMethodsDoNotModifyOriginalBufferPosition() {
        byte[] data = "unchanged".getBytes(charset);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.position(3);
        buffer.limit(7); // slice "anged"

        // Call each conversion method
        ByteBufferConversions.toByteBuffer(buffer, converter);
        ByteBufferConversions.toByteArray(buffer, converter);
        ByteBufferConversions.toCharBuffer(buffer, converter);
        ByteBufferConversions.toString(buffer, converter);
        ByteBufferConversions.toCharArray(buffer, converter);
        ByteBufferConversions.toStringBuffer(buffer, converter);
        ByteBufferConversions.toStringBuilder(buffer, converter);

        // Verify original position/limit unchanged
        assertEquals(3, buffer.position());
        assertEquals(7, buffer.limit());
    }
}
