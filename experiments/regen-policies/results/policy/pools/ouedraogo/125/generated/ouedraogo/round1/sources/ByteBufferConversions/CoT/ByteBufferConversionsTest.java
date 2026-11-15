package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;

/**
 * Unit tests for {@link ByteBufferConversions}.
 *
 * The tests are written in the same package as {@code ByteBufferConversions}
 * so that the package‑private static methods are directly accessible.
 */
public class ByteBufferConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The real library provides a ConverterOptions class that supplies a Charset.
        // Using the default constructor gives us the standard UTF‑8 charset.
        converter = new Converter(new ConverterOptions());
    }

    /* ---------------------------------------------------------------------- */
    /*  1. list of public (actually package‑private) methods in ByteBufferConversions */
    /* ---------------------------------------------------------------------- */
    // static CharBuffer toCharBuffer(Object from, Converter converter)
    // static ByteBuffer toByteBuffer(Object from, Converter converter)
    // static byte[]    toByteArray(Object from, Converter converter)
    // static String    toString(Object from, Converter converter)
    // static char[]    toCharArray(Object from, Converter converter)
    // static StringBuffer toStringBuffer(Object from, Converter converter)
    // static StringBuilder toStringBuilder(Object from, Converter converter)

    /* ---------------------------------------------------------------------- */
    /*  2. Basic functionality tests                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByteBufferReturnsReadOnlyCopy() {
        byte[] data = {1, 2, 3, 4};
        ByteBuffer original = ByteBuffer.wrap(data); // writable buffer
        int originalPos = original.position();

        ByteBuffer result = ByteBufferConversions.toByteBuffer(original, converter);

        // result must be a read‑only view
        assertTrue("Resulting buffer should be read‑only", result.isReadOnly());

        // original buffer's position must be unchanged
        assertEquals("Original buffer position must stay the same", originalPos, original.position());

        // content must be identical
        byte[] extracted = new byte[result.remaining()];
        result.get(extracted);
        assertArrayEquals("Content of the read‑only buffer must match original", data, extracted);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBufferWithInvalidTypeThrowsException() {
        String notABuffer = "I am not a ByteBuffer";
        ByteBufferConversions.toByteBuffer(notABuffer, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBufferWithNullThrowsNPE() {
        ByteBufferConversions.toByteBuffer(null, converter);
    }

    @Test
    public void testToByteArrayWithData() {
        byte[] data = {10, 20, 30, 40, 50};
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        assertArrayEquals("Byte array conversion must preserve data", data, result);
    }

    @Test
    public void testToByteArrayEmptyBufferReturnsEmptyArray() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        byte[] result = ByteBufferConversions.toByteArray(empty, converter);
        assertNotNull("Result must never be null", result);
        assertEquals("Empty buffer should yield empty array", 0, result.length);
        // also verify it is the library constant EMPTY_BYTE_ARRAY (reference equality)
        assertSame("Should return the shared EMPTY_BYTE_ARRAY constant", EMPTY_BYTE_ARRAY, result);
    }

    @Test
    public void testToStringDecodesUtf8Correctly() {
        String text = "Hello, ByteBuffer!";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(text);
        String result = ByteBufferConversions.toString(buffer, converter);
        assertEquals("String conversion must decode using the converter's charset", text, result);
    }

    @Test
    public void testToStringBuffer() {
        String text = "StringBuffer test";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(text);
        StringBuffer sb = ByteBufferConversions.toStringBuffer(buffer, converter);
        assertEquals("StringBuffer content must match original text", text, sb.toString());
    }

    @Test
    public void testToStringBuilder() {
        String text = "StringBuilder test";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(text);
        StringBuilder sb = ByteBufferConversions.toStringBuilder(buffer, converter);
        assertEquals("StringBuilder content must match original text", text, sb.toString());
    }

    @Test
    public void testToCharArray() {
        String text = "charArray";
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(text);
        char[] result = ByteBufferConversions.toCharArray(buffer, converter);
        assertArrayEquals("Char array must contain the same characters as the source string",
                text.toCharArray(), result);
    }

    /* ---------------------------------------------------------------------- */
    /*  3. Edge‑case / exception tests                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByteArrayWhenBufferHasNoRemaining() {
        // Create a buffer, advance its position to the limit so hasRemaining() == false
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.position(5); // now no remaining bytes
        byte[] result = ByteBufferConversions.toByteArray(buffer, converter);
        assertSame("When no bytes remain, should return EMPTY_BYTE_ARRAY", EMPTY_BYTE_ARRAY, result);
    }

    @Test
    public void testToCharBufferWithNonByteBufferInput() {
        try {
            ByteBufferConversions.toCharBuffer("not a buffer", converter);
            fail("Expected ClassCastException when non‑ByteBuffer is supplied");
        } catch (ClassCastException e) {
            // expected
        }
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBufferWithNullInput() {
        ByteBufferConversions.toCharBuffer(null, converter);
    }

    @Test
    public void testToStringWhenBufferIsEmpty() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        String result = ByteBufferConversions.toString(empty, converter);
        assertEquals("Empty buffer should produce empty string", "", result);
    }

    @Test
    public void testToStringBuilderWhenBufferIsEmpty() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        StringBuilder sb = ByteBufferConversions.toStringBuilder(empty, converter);
        assertEquals("StringBuilder from empty buffer must be empty", "", sb.toString());
    }

    @Test
    public void testToStringBufferWhenBufferIsEmpty() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        StringBuffer sb = ByteBufferConversions.toStringBuffer(empty, converter);
        assertEquals("StringBuffer from empty buffer must be empty", "", sb.toString());
    }
}
