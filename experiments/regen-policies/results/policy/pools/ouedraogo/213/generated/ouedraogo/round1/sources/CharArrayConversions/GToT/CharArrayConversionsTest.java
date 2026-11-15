package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * JUnit 4 test suite for {@link CharArrayConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Normal usage of each conversion method.</li>
 *   <li>Edge‑cases such as empty arrays, null input, and arrays with
 *       special characters (including surrogate pairs).</li>
 *   <li>Exception scenarios – the class does not throw its own
 *       exceptions, but it will throw {@link NullPointerException}
 *       when a null reference is supplied where a {@code char[]} is
 *       expected.</li>
 * </ul>
 * The {@link Converter} required by the static methods is built with
 * default {@link ConverterOptions} that use {@link Charset#defaultCharset()}.
 */
public class CharArrayConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Use default options – the charset is the platform default.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* -------------------------------------------------------------
     *  toCharBuffer
     * ------------------------------------------------------------- */
    @Test
    public void testToCharBuffer_Normal() {
        char[] source = {'a', 'b', 'c'};
        CharBuffer cb = CharArrayConversions.toCharBuffer(source, converter);
        assertNotNull(cb);
        assertEquals(3, cb.remaining());
        assertArrayEquals(source, cb.array());
    }

    @Test
    public void testToCharBuffer_EmptyArray() {
        char[] source = new char[0];
        CharBuffer cb = CharArrayConversions.toCharBuffer(source, converter);
        assertNotNull(cb);
        assertEquals(0, cb.remaining());
        assertArrayEquals(source, cb.array());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_NullInput() {
        CharArrayConversions.toCharBuffer(null, converter);
    }

    /* -------------------------------------------------------------
     *  toString
     * ------------------------------------------------------------- */
    @Test
    public void testToString_Normal() {
        char[] source = {'J', 'u', 'n', 'i', 't'};
        String result = CharArrayConversions.toString(source, converter);
        assertEquals("JUnit", result);
    }

    @Test
    public void testToString_EmptyArray() {
        char[] source = new char[0];
        String result = CharArrayConversions.toString(source, converter);
        assertEquals("", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        CharArrayConversions.toString(null, converter);
    }

    /* -------------------------------------------------------------
     *  toStringBuffer
     * ------------------------------------------------------------- */
    @Test
    public void testToStringBuffer_Normal() {
        char[] source = {'A', 'B', 'C'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, converter);
        assertEquals(new StringBuffer("ABC").toString(), sb.toString());
    }

    @Test
    public void testToStringBuffer_EmptyArray() {
        char[] source = new char[0];
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, converter);
        assertEquals(0, sb.length());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_NullInput() {
        CharArrayConversions.toStringBuffer(null, converter);
    }

    /* -------------------------------------------------------------
     *  toStringBuilder
     * ------------------------------------------------------------- */
    @Test
    public void testToStringBuilder_Normal() {
        char[] source = {'x', 'y', 'z'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertEquals(new StringBuilder("xyz").toString(), sb.toString());
    }

    @Test
    public void testToStringBuilder_EmptyArray() {
        char[] source = new char[0];
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, converter);
        assertEquals(0, sb.length());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_NullInput() {
        CharArrayConversions.toStringBuilder(null, converter);
    }

    /* -------------------------------------------------------------
     *  toByteBuffer
     * ------------------------------------------------------------- */
    @Test
    public void testToByteBuffer_Normal() {
        // Use characters that map to more than one byte in UTF‑8 to verify encoding.
        char[] source = {'€', '𐍈'}; // Euro sign (3 bytes), Gothic letter (4 bytes)
        ByteBuffer bb = CharArrayConversions.toByteBuffer(source, converter);
        assertNotNull(bb);
        // Encode manually for comparison
        byte[] expected = converter.getOptions().getCharset().encode(CharBuffer.wrap(source)).array();
        // ByteBuffer may have a backing array larger than remaining, compare only the used part.
        byte[] actual = new byte[bb.remaining()];
        bb.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToByteBuffer_EmptyArray() {
        char[] source = new char[0];
        ByteBuffer bb = CharArrayConversions.toByteBuffer(source, converter);
        assertNotNull(bb);
        assertEquals(0, bb.remaining());
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullInput() {
        CharArrayConversions.toByteBuffer(null, converter);
    }

    /* -------------------------------------------------------------
     *  toByteArray
     * ------------------------------------------------------------- */
    @Test
    public void testToByteArray_Normal() {
        char[] source = {'A', 'B', 'C'};
        byte[] result = CharArrayConversions.toByteArray(source, converter);
        byte[] expected = converter.getOptions().getCharset().encode(CharBuffer.wrap(source)).array();
        // Trim possible trailing zeroes from the backing array
        int len = result.length;
        assertEquals(expected.length, len);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testToByteArray_EmptyArray() {
        char[] source = new char[0];
        byte[] result = CharArrayConversions.toByteArray(source, converter);
        assertEquals(0, result.length);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArray_NullInput() {
        CharArrayConversions.toByteArray(null, converter);
    }

    /* -------------------------------------------------------------
     *  toCharArray
     * ------------------------------------------------------------- */
    @Test
    public void testToCharArray_Normal() {
        char[] source = {'h', 'e', 'l', 'l', 'o'};
        char[] copy = CharArrayConversions.toCharArray(source, converter);
        assertArrayEquals(source, copy);
        // Ensure a defensive copy – modifying the result must not affect the original
        copy[0] = 'H';
        assertEquals('h', source[0]);
        assertEquals('H', copy[0]);
    }

    @Test
    public void testToCharArray_EmptyArray() {
        char[] source = new char[0];
        char[] copy = CharArrayConversions.toCharArray(source, converter);
        assertArrayEquals(source, copy);
        assertNotSame(source, copy);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_NullInput() {
        CharArrayConversions.toCharArray(null, converter);
    }

    /* -------------------------------------------------------------
     *  Additional edge cases – surrogate pairs & non‑BMP characters
     * ------------------------------------------------------------- */
    @Test
    public void testSurrogatePair_ConversionConsistency() {
        // A character outside the BMP (U+1F600 GRINNING FACE) represented by a surrogate pair.
        String emoji = new String(Character.toChars(0x1F600));
        char[] source = emoji.toCharArray(); // length == 2
        // toCharBuffer → toByteBuffer → toByteArray should round‑trip correctly.
        CharBuffer cb = CharArrayConversions.toCharBuffer(source, converter);
        assertEquals(2, cb.remaining());

        ByteBuffer bb = CharArrayConversions.toByteBuffer(source, converter);
        byte[] bytes = CharArrayConversions.toByteArray(source, converter);
        assertEquals(bb.remaining(), bytes.length);

        // Decode back to characters and compare
        CharBuffer decoded = converter.getOptions().getCharset().decode(ByteBuffer.wrap(bytes));
        assertArrayEquals(source, decoded.array());
    }

    @Test
    public void testLargeArray_PerformanceAndCorrectness() {
        // Build a large char array (10,000 elements) with a repeating pattern.
        char[] source = new char[10_000];
        for (int i = 0; i < source.length; i++) {
            source[i] = (char) ('a' + (i % 26));
        }
        // Verify that all conversion methods handle the size without error.
        assertEquals(source.length, CharArrayConversions.toCharArray(source, converter).length);
        assertEquals(source.length, CharArrayConversions.toString(source, converter).length());
        assertEquals(source.length, CharArrayConversions.toStringBuilder(source, converter).length());
        assertEquals(source.length, CharArrayConversions.toStringBuffer(source, converter).length());
        assertEquals(source.length, CharArrayConversions.toCharBuffer(source, converter).remaining());

        byte[] bytes = CharArrayConversions.toByteArray(source, converter);
        // The byte length depends on the charset; for ASCII‑compatible charset each char → 1 byte.
        // Verify that converting back yields the original characters.
        CharBuffer roundTrip = converter.getOptions().getCharset().decode(ByteBuffer.wrap(bytes));
        assertArrayEquals(source, roundTrip.array());
    }
}
