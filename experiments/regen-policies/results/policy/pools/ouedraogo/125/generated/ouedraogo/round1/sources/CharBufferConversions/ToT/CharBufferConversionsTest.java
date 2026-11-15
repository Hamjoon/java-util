package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CharBufferConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal usage (typical strings)</li>
 *   <li>edge cases (empty buffers, buffers with no remaining characters)</li>
 *   <li>error scenarios (wrong input type, null values)</li>
 * </ul>
 *
 * No mocking is used – all objects are created through their public constructors or factory
 * methods as required by the task description.
 */
public class CharBufferConversionsTest {

    private Converter converter;
    private Charset charset = StandardCharsets.UTF_8;

    @Before
    public void setUp() {
        // ConverterOptions is part of the same library; it provides a Charset for encoding.
        // The default constructor creates an options instance with a mutable charset.
        ConverterOptions options = new ConverterOptions();
        options.setCharset(charset);
        converter = new Converter(options);
    }

    /* -------------------------------------------------------------
     *  toCharBuffer()
     * ------------------------------------------------------------- */

    @Test
    public void toCharBuffer_returnsReadOnlyBufferWithSameContent() {
        CharBuffer original = CharBuffer.wrap("hello world");
        // change position/limit to verify that the returned buffer is independent
        original.position(2);
        original.limit(7); // contains "llo w"

        CharBuffer result = CharBufferConversions.toCharBuffer(original, converter);

        // result must be read‑only
        assertTrue("Resulting CharBuffer should be read‑only", result.isReadOnly());

        // content must be the whole original sequence (not the sliced view)
        assertEquals("hello world", result.toString());

        // original position/limit changes must NOT affect the new buffer's position
        original.position(0);
        original.limit(original.capacity());
        assertEquals("Resulting buffer's position should stay at 0", 0, result.position());
    }

    @Test(expected = ClassCastException.class)
    public void toCharBuffer_throwsWhenInputIsNotCharBuffer() {
        // Passing a String (or any non‑CharBuffer) should cause a ClassCastException
        CharBufferConversions.toCharBuffer("not a CharBuffer", converter);
    }

    @Test(expected = NullPointerException.class)
    public void toCharBuffer_throwsWhenInputIsNull() {
        CharBufferConversions.toCharBuffer(null, converter);
    }

    /* -------------------------------------------------------------
     *  toByteArray()
     * ------------------------------------------------------------- */

    @Test
    public void toByteArray_encodesCharactersUsingConfiguredCharset() {
        CharBuffer source = CharBuffer.wrap("Café"); // contains a non‑ASCII character
        byte[] expected = "Café".getBytes(charset);

        byte[] result = CharBufferConversions.toByteArray(source, converter);

        assertArrayEquals("Byte array must match UTF‑8 encoding of the source string", expected, result);
    }

    @Test
    public void toByteArray_emptyCharBuffer_returnsEmptyArray() {
        CharBuffer empty = CharBuffer.wrap("");
        byte[] result = CharBufferConversions.toByteArray(empty, converter);
        assertEquals("Byte array length for empty buffer must be zero", 0, result.length);
    }

    /* -------------------------------------------------------------
     *  toByteBuffer()
     * ------------------------------------------------------------- */

    @Test
    public void toByteBuffer_encodesAndPreservesByteOrder() {
        CharBuffer source = CharBuffer.wrap("AB");
        ByteBuffer result = CharBufferConversions.toByteBuffer(source, converter);

        // The buffer should contain the UTF‑8 bytes for "AB"
        byte[] expected = "AB".getBytes(charset);
        byte[] actual = new byte[result.remaining()];
        result.get(actual);
        assertArrayEquals("ByteBuffer must contain the UTF‑8 representation of the source", expected, actual);
    }

    @Test(expected = ClassCastException.class)
    public void toByteBuffer_throwsWhenInputIsNotCharBuffer() {
        CharBufferConversions.toByteBuffer(12345, converter);
    }

    /* -------------------------------------------------------------
     *  toString()
     * ------------------------------------------------------------- */

    @Test
    public void toString_returnsExactStringRepresentation() {
        CharBuffer source = CharBuffer.wrap("JUnit Test");
        String result = CharBufferConversions.toString(source, converter);
        assertEquals("String conversion must return the original characters", "JUnit Test", result);
    }

    @Test
    public void toString_emptyCharBuffer_returnsEmptyString() {
        CharBuffer empty = CharBuffer.wrap("");
        assertEquals("Empty CharBuffer should produce an empty String", "", CharBufferConversions.toString(empty, converter));
    }

    /* -------------------------------------------------------------
     *  toCharArray()
     * ------------------------------------------------------------- */

    @Test
    public void toCharArray_copiesRemainingCharacters() {
        CharBuffer source = CharBuffer.wrap("abcdef");
        source.position(2); // remaining = "cdef"
        char[] result = CharBufferConversions.toCharArray(source, converter);
        assertArrayEquals(new char[]{'c', 'd', 'e', 'f'}, result);
    }

    @Test
    public void toCharArray_whenNoRemaining_returnsSharedEmptyArray() {
        CharBuffer empty = CharBuffer.wrap("");
        char[] result = CharBufferConversions.toCharArray(empty, converter);
        // The implementation returns the constant EMPTY_CHAR_ARRAY reference.
        assertSame("Empty buffer must return the shared EMPTY_CHAR_ARRAY constant", EMPTY_CHAR_ARRAY, result);
    }

    @Test
    public void toCharArray_doesNotExposeInternalBuffer() {
        CharBuffer source = CharBuffer.wrap("xyz");
        char[] result = CharBufferConversions.toCharArray(source, converter);
        // Mutate the returned array – the original buffer must stay unchanged.
        result[0] = 'a';
        assertEquals("Original CharBuffer content must remain unchanged", 'x', source.get(0));
    }

    /* -------------------------------------------------------------
     *  toStringBuffer()
     * ------------------------------------------------------------- */

    @Test
    public void toStringBuffer_containsSameCharacters() {
        CharBuffer source = CharBuffer.wrap("StringBuffer");
        StringBuffer sb = CharBufferConversions.toStringBuffer(source, converter);
        assertEquals("StringBuffer content must match source", "StringBuffer", sb.toString());
    }

    /* -------------------------------------------------------------
     *  toStringBuilder()
     * ------------------------------------------------------------- */

    @Test
    public void toStringBuilder_containsSameCharacters() {
        CharBuffer source = CharBuffer.wrap("StringBuilder");
        StringBuilder sb = CharBufferConversions.toStringBuilder(source, converter);
        assertEquals("StringBuilder content must match source", "StringBuilder", sb.toString());
    }

    /* -------------------------------------------------------------
     *  Additional error handling – null converter
     * ------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void methods_throwWhenConverterIsNull() {
        CharBuffer source = CharBuffer.wrap("test");
        // All static methods dereference the converter; passing null must NPE.
        CharBufferConversions.toString(source, null);
    }
}
