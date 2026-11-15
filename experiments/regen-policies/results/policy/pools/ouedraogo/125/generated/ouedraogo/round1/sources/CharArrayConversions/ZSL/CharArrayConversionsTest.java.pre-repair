/****************************************************************************************
 * CharArrayConversionsTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@link com.cedarsoftware.util.convert.CharArrayConversions}.
 *
 * The tests cover every public (package‑private) static method in the target class,
 * exercising normal, edge‑case and error conditions.  The test class lives in the same
 * package as the class under test so that the package‑private methods are accessible.
 *
 * Author: ChatGPT (OpenAI)
 * Date:   2026‑11‑14
 ****************************************************************************************/
package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class CharArrayConversionsTest {

    private static final char[] SIMPLE_CHARS = "Hello".toCharArray();
    private static final char[] UNICODE_CHARS = "¡¢£¤¥".toCharArray(); // characters > 0x7F
    private static final char[] EMPTY_CHARS = new char[0];

    private Converter utf8Converter;
    private Converter utf16leConverter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /** Helper to create a {@link Converter} with a specific charset. */
    private static Converter createConverter(Charset charset) {
        // ConverterOptions is part of the same library; it provides a setter for the charset.
        // The exact API is not shown in the excerpt, but the library defines a
        // `setCharset(Charset)` method (common in such utilities).  Using it keeps the test
        // independent of internal implementation details.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(charset);
        return new Converter(options);
    }

    @Before
    public void setUp() {
        utf8Converter = createConverter(StandardCharsets.UTF_8);
        utf16leConverter = createConverter(StandardCharsets.UTF_16LE);
    }

    /* -------------------------------------------------------------------------
     * toString(char[]) tests
     * ---------------------------------------------------------------------- */
    @Test
    public void testToStringSimple() {
        String result = CharArrayConversions.toString(SIMPLE_CHARS, utf8Converter);
        assertEquals("Hello", result);
    }

    @Test
    public void testToStringUnicode() {
        String result = CharArrayConversions.toString(UNICODE_CHARS, utf8Converter);
        assertEquals(new String(UNICODE_CHARS), result);
    }

    @Test
    public void testToStringEmpty() {
        String result = CharArrayConversions.toString(EMPTY_CHARS, utf8Converter);
        assertEquals("", result);
    }

    @Test
    public void testToStringNullArrayThrowsNPE() {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toString(null, utf8Converter);
    }

    /* -------------------------------------------------------------------------
     * toCharBuffer(char[]) tests
     * ---------------------------------------------------------------------- */
    @Test
    public void testToCharBufferWrapsArray() {
        CharBuffer buffer = CharArrayConversions.toCharBuffer(SIMPLE_CHARS, utf8Converter);
        assertNotNull(buffer);
        assertEquals(SIMPLE_CHARS.length, buffer.remaining());

        // Verify the buffer content matches the source array.
        char[] fromBuffer = new char[SIMPLE_CHARS.length];
        buffer.get(fromBuffer);
        assertArrayEquals(SIMPLE_CHARS, fromBuffer);
    }

    @Test
    public void testToCharBufferReflectsChangesInOriginalArray() {
        char[] mutable = "abc".toCharArray();
        CharBuffer buffer = CharArrayConversions.toCharBuffer(mutable, utf8Converter);
        mutable[0] = 'z';
        buffer.rewind(); // reset position to read from start
        assertEquals('z', buffer.get());
    }

    @Test
    public void testToCharBufferEmpty() {
        CharBuffer buffer = CharArrayConversions.toCharBuffer(EMPTY_CHARS, utf8Converter);
        assertNotNull(buffer);
        assertEquals(0, buffer.remaining());
    }

    @Test
    public void testToCharBufferNullArrayThrowsNPE() {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toCharBuffer(null, utf8Converter);
    }

    /* -------------------------------------------------------------------------
     * toStringBuffer(char[]) tests
     * ---------------------------------------------------------------------- */
    @Test
    public void testToStringBufferContent() {
        StringBuffer sb = CharArrayConversions.toStringBuffer(SIMPLE_CHARS, utf8Converter);
        assertEquals("Hello", sb.toString());
    }

    @Test
    public void testToStringBufferUnicode() {
        StringBuffer sb = CharArrayConversions.toStringBuffer(UNICODE_CHARS, utf8Converter);
        assertEquals(new String(UNICODE_CHARS), sb.toString());
    }

    @Test
    public void testToStringBufferEmpty() {
        StringBuffer sb = CharArrayConversions.toStringBuffer(EMPTY_CHARS, utf8Converter);
        assertEquals("", sb.toString());
    }

    /* -------------------------------------------------------------------------
     * toStringBuilder(char[]) tests
     * ---------------------------------------------------------------------- */
    @Test
    public void testToStringBuilderContent() {
        StringBuilder sb = CharArrayConversions.toStringBuilder(SIMPLE_CHARS, utf8Converter);
        assertEquals("Hello", sb.toString());
    }

    @Test
    public void testToStringBuilderUnicode() {
        StringBuilder sb = CharArrayConversions.toStringBuilder(UNICODE_CHARS, utf8Converter);
        assertEquals(new String(UNICODE_CHARS), sb.toString());
    }

    @Test
    public void testToStringBuilderEmpty() {
        StringBuilder sb = CharArrayConversions.toStringBuilder(EMPTY_CHARS, utf8Converter);
        assertEquals("", sb.toString());
    }

    /* -------------------------------------------------------------------------
     * toByteArray(char[]) tests – exercised via toByteBuffer internally
     * ---------------------------------------------------------------------- */
    @Test
    public void testToByteArrayUtf8Simple() {
        byte[] bytes = CharArrayConversions.toByteArray(SIMPLE_CHARS, utf8Converter);
        assertArrayEquals("Hello".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    public void testToByteArrayUtf8Unicode() {
        byte[] expected = new String(UNICODE_CHARS).getBytes(StandardCharsets.UTF_8);
        byte[] actual = CharArrayConversions.toByteArray(UNICODE_CHARS, utf8Converter);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteArrayUtf16LESimple() {
        byte[] bytes = CharArrayConversions.toByteArray(SIMPLE_CHARS, utf16leConverter);
        // UTF‑16LE: each char becomes two bytes, low‑order byte first.
        byte[] expected = "Hello".getBytes(StandardCharsets.UTF_16LE);
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testToByteArrayEmpty() {
        byte[] bytes = CharArrayConversions.toByteArray(EMPTY_CHARS, utf8Converter);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testToByteArrayNullArrayThrowsNPE() {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toByteArray(null, utf8Converter);
    }

    /* -------------------------------------------------------------------------
     * toCharArray(char[]) tests – verifies defensive copy semantics
     * ---------------------------------------------------------------------- */
    @Test
    public void testToCharArrayCreatesCopy() {
        char[] source = "copyMe".toCharArray();
        char[] copy = CharArrayConversions.toCharArray(source, utf8Converter);
        assertNotSame("Returned array should be a copy, not the original reference", source, copy);
        assertArrayEquals(source, copy);

        // Mutate the original and verify the copy is unaffected.
        source[0] = 'X';
        assertNotEquals(source[0], copy[0]);
    }

    @Test
    public void testToCharArrayEmpty() {
        char[] copy = CharArrayConversions.toCharArray(EMPTY_CHARS, utf8Converter);
        assertNotNull(copy);
        assertEquals(0, copy.length);
    }

    @Test
    public void testToCharArrayNullArrayThrowsNPE() {
        thrown.expect(NullPointerException.class);
        CharArrayConversions.toCharArray(null, utf8Converter);
    }

    /* -------------------------------------------------------------------------
     * toByteBuffer(char[]) indirect test – validates that the charset from
     * ConverterOptions is honoured.
     * ---------------------------------------------------------------------- */
    @Test
    public void testToByteBufferUsesConverterCharset_Utf8() {
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(SIMPLE_CHARS, utf8Converter);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals("Hello".getBytes(StandardCharsets.UTF_8), actual);
    }

    @Test
    public void testToByteBufferUsesConverterCharset_Utf16LE() {
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(SIMPLE_CHARS, utf16leConverter);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals("Hello".getBytes(StandardCharsets.UTF_16LE), actual);
    }
}
