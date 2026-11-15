package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

/**
 * Unit tests for {@link CharArrayConversions}.
 *
 * The tests are placed in the same package so that the package‑private
 * static methods of {@code CharArrayConversions} are accessible.
 *
 * The {@code Converter} instance is created with a default {@code ConverterOptions}
 * object; the default charset (normally UTF‑8) is used for the encoding
 * related tests.
 */
public class CharArrayConversionsTest {

    /** Helper that creates a {@code Converter} with default options. */
    private static Converter createDefaultConverter() {
        // {@code ConverterOptions} has a public no‑arg constructor in the library.
        return new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------------- */
    /* toString                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringSimple() {
        Converter conv = createDefaultConverter();
        char[] input = {'h', 'e', 'l', 'l', 'o'};
        String result = CharArrayConversions.toString(input, conv);
        assertEquals("hello", result);
    }

    @Test
    public void testToStringEmptyArray() {
        Converter conv = createDefaultConverter();
        char[] input = {};
        String result = CharArrayConversions.toString(input, conv);
        assertEquals("", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullInput() {
        Converter conv = createDefaultConverter();
        CharArrayConversions.toString(null, conv);
    }

    /* --------------------------------------------------------------------- */
    /* toCharBuffer                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharBufferWrapsArray() {
        Converter conv = createDefaultConverter();
        char[] input = {'a', 'b', 'c'};
        CharBuffer buffer = CharArrayConversions.toCharBuffer(input, conv);

        // The buffer should contain the same characters in the same order.
        assertEquals(3, buffer.remaining());
        assertEquals('a', buffer.get());

        // The buffer is backed by the original array – a change in the array
        // must be visible through the buffer.
        input[1] = 'z';
        buffer.position(1);
        assertEquals('z', buffer.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBufferNullInput() {
        Converter conv = createDefaultConverter();
        CharArrayConversions.toCharBuffer(null, conv);
    }

    /* --------------------------------------------------------------------- */
    /* toStringBuffer / toStringBuilder                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringBufferAndBuilderContent() {
        Converter conv = createDefaultConverter();
        char[] input = {'J', 'U', 'N', 'I', 'T'};

        StringBuffer sb = CharArrayConversions.toStringBuffer(input, conv);
        StringBuilder sbld = CharArrayConversions.toStringBuilder(input, conv);

        assertEquals("JUNIT", sb.toString());
        assertEquals("JUNIT", sbld.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBufferNullInput() {
        Converter conv = createDefaultConverter();
        CharArrayConversions.toStringBuffer(null, conv);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilderNullInput() {
        Converter conv = createDefaultConverter();
        CharArrayConversions.toStringBuilder(null, conv);
    }

    /* --------------------------------------------------------------------- */
    /* toByteBuffer / toByteArray                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteBufferMatchesStringEncoding() {
        Converter conv = createDefaultConverter();
        char[] input = {'A', 'B', 'C'};
        ByteBuffer byteBuf = CharArrayConversions.toByteBuffer(input, conv);

        // The default charset is used by the library (normally UTF‑8).
        byte[] expected = "ABC".getBytes(conv.getOptions().getCharset());

        byte[] actual = new byte[byteBuf.remaining()];
        byteBuf.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArrayIsConsistentWithToByteBuffer() {
        Converter conv = createDefaultConverter();
        char[] input = {'x', 'y', 'z', 'é'}; // include a non‑ASCII character

        byte[] fromArray = CharArrayConversions.toByteArray(input, conv);
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(input, conv);
        byte[] fromBuffer = new byte[buffer.remaining()];
        buffer.get(fromBuffer);

        assertArrayEquals(fromBuffer, fromArray);
    }

    @Test
    public void testToByteBufferEmptyArray() {
        Converter conv = createDefaultConverter();
        char[] input = {};
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(input, conv);
        assertEquals(0, buffer.remaining());
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBufferNullInput() {
        Converter conv = createDefaultConverter();
        CharArrayConversions.toByteBuffer(null, conv);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArrayNullInput() {
        Converter conv = createDefaultConverter();
        CharArrayConversions.toByteArray(null, conv);
    }

    /* --------------------------------------------------------------------- */
    /* toCharArray                                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharArrayReturnsCopy() {
        Converter conv = createDefaultConverter();
        char[] original = {'m', 'a', 'p'};
        char[] copy = CharArrayConversions.toCharArray(original, conv);

        // Content must be identical.
        assertArrayEquals(original, copy);
        // Must be a different instance.
        assertNotSame(original, copy);

        // Modifying the original must not affect the copy.
        original[0] = 'z';
        assertEquals('m', copy[0]);
    }

    @Test
    public void testToCharArrayEmpty() {
        Converter conv = createDefaultConverter();
        char[] empty = {};
        char[] result = CharArrayConversions.toCharArray(empty, conv);
        assertArrayEquals(empty, result);
        assertNotSame(empty, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArrayNullInput() {
        Converter conv = createDefaultConverter();
        CharArrayConversions.toCharArray(null, conv);
    }
}
