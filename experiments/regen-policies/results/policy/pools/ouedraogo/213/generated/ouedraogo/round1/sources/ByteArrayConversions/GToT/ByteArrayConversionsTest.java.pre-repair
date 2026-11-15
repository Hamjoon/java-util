package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * JUnit‑4 test suite for {@link ByteArrayConversions}.
 *
 * The tests are written in the same package as the class under test so that
 * package‑private static methods are directly accessible.
 *
 * The test cases cover:
 * <ul>
 *   <li>Normal (happy‑path) usage for each conversion method.</li>
 *   <li>Edge cases such as {@code null} and empty byte arrays.</li>
 *   <li>Exception scenarios (e.g., {@code NullPointerException} from {@code ByteBuffer.wrap}).</li>
 *   <li>Verification that the supplied {@link Converter} charset is honoured.</li>
 * </ul>
 */
public class ByteArrayConversionsTest {

    /** A {@link Converter} instance that uses UTF‑8 as its charset. */
    private Converter utf8Converter;

    @Before
    public void setUp() {
        // The concrete implementation of ConverterOptions is not shown, but
        // the library provides a no‑arg constructor that defaults to UTF‑8.
        // If a different constructor is required, adjust accordingly.
        ConverterOptions options = new ConverterOptions();               // default UTF‑8
        utf8Converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- *
     *  toString(Object, Converter)                                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testToString_NullByteArray_ReturnsEmptyString() {
        byte[] input = null;
        String result = ByteArrayConversions.toString(input, utf8Converter);
        assertEquals(StringUtilities.EMPTY, result);
    }

    @Test
    public void testToString_EmptyByteArray_ReturnsEmptyString() {
        byte[] input = new byte[0];
        String result = ByteArrayConversions.toString(input, utf8Converter);
        assertEquals("", result);
    }

    @Test
    public void testToString_AsciiBytes_ReturnsCorrectString() {
        String original = "Hello, World!";
        byte[] input = original.getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(input, utf8Converter);
        assertEquals(original, result);
    }

    @Test
    public void testToString_UnicodeBytes_CharsetIsRespected() {
        String original = "こんにちは"; // Japanese "Hello"
        // Encode with UTF‑8 (the charset used by the converter)
        byte[] input = original.getBytes(StandardCharsets.UTF_8);
        String result = ByteArrayConversions.toString(input, utf8Converter);
        assertEquals(original, result);
    }

    /* --------------------------------------------------------------------- *
     *  toByteBuffer(Object, Converter)                                      *
     * --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullByteArray_ThrowsNPE() {
        byte[] input = null;
        ByteArrayConversions.toByteBuffer(input, utf8Converter);
    }

    @Test
    public void testToByteBuffer_ValidBytes_ReturnsBufferWithSameContent() {
        byte[] input = {0, 1, 2, 3, 4, 5};
        ByteBuffer buffer = ByteArrayConversions.toByteBuffer(input, utf8Converter);
        assertEquals(input.length, buffer.remaining());
        for (byte b : input) {
            assertEquals(b, buffer.get());
        }
    }

    /* --------------------------------------------------------------------- *
     *  toCharBuffer(Object, Converter)                                      *
     * --------------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_ValidBytes_ReturnsCorrectCharBuffer() {
        String original = "ABCxyz123";
        byte[] input = original.getBytes(StandardCharsets.UTF_8);
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(input, utf8Converter);
        assertEquals(original, charBuffer.toString());
    }

    @Test
    public void testToCharBuffer_EmptyArray_ReturnsEmptyCharBuffer() {
        byte[] input = new byte[0];
        CharBuffer charBuffer = ByteArrayConversions.toCharBuffer(input, utf8Converter);
        assertEquals(0, charBuffer.length());
    }

    /* --------------------------------------------------------------------- *
     *  toCharArray(Object, Converter)                                       *
     * --------------------------------------------------------------------- */

    @Test
    public void testToCharArray_ValidBytes_ReturnsCorrectArray() {
        String original = "TestChars";
        byte[] input = original.getBytes(StandardCharsets.UTF_8);
        char[] result = ByteArrayConversions.toCharArray(input, utf8Converter);
        assertArrayEquals(original.toCharArray(), result);
    }

    @Test
    public void testToCharArray_EmptyArray_ReturnsEmptyArray() {
        byte[] input = new byte[0];
        char[] result = ByteArrayConversions.toCharArray(input, utf8Converter);
        assertEquals(0, result.length);
    }

    /* --------------------------------------------------------------------- *
     *  toStringBuffer(Object, Converter)                                    *
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringBuffer_ValidBytes_ReturnsCorrectStringBuffer() {
        String original = "StringBufferContent";
        byte[] input = original.getBytes(StandardCharsets.UTF_8);
        StringBuffer sb = ByteArrayConversions.toStringBuffer(input, utf8Converter);
        assertEquals(original, sb.toString());
    }

    @Test
    public void testToStringBuffer_EmptyArray_ReturnsEmptyStringBuffer() {
        byte[] input = new byte[0];
        StringBuffer sb = ByteArrayConversions.toStringBuffer(input, utf8Converter);
        assertEquals(0, sb.length());
    }

    /* --------------------------------------------------------------------- *
     *  toStringBuilder(Object, Converter)                                   *
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilder_ValidBytes_ReturnsCorrectStringBuilder() {
        String original = "BuilderContent";
        byte[] input = original.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = ByteArrayConversions.toStringBuilder(input, utf8Converter);
        assertEquals(original, sb.toString());
    }

    @Test
    public void testToStringBuilder_EmptyArray_ReturnsEmptyStringBuilder() {
        byte[] input = new byte[0];
        StringBuilder sb = ByteArrayConversions.toStringBuilder(input, utf8Converter);
        assertEquals(0, sb.length());
    }

    /* --------------------------------------------------------------------- *
     *  Additional edge‑case: very large array (performance‑related)        *
     * --------------------------------------------------------------------- */

    @Test
    public void testToString_LargeByteArray() {
        // Build a 1 MiB array of repeating pattern "AB"
        int size = 1024 * 1024;
        byte[] input = new byte[size];
        for (int i = 0; i < size; i++) {
            input[i] = (byte) ((i % 2 == 0) ? 'A' : 'B');
        }
        String result = ByteArrayConversions.toString(input, utf8Converter);
        assertEquals(size, result.length());
        // Verify first and last characters
        assertEquals('A', result.charAt(0));
        assertEquals((size % 2 == 0) ? 'A' : 'B', result.charAt(size - 1));
    }
}
