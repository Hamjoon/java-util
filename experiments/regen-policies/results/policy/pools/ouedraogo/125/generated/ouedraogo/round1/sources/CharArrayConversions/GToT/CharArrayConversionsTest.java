package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Comprehensive JUnit‑4 test suite for {@code CharArrayConversions}.
 *
 * <p>All public static methods of {@code CharArrayConversions} are exercised
 * with:
 * <ul>
 *   <li>typical (happy‑path) inputs</li>
 *   <li>edge‑case inputs (empty arrays, large arrays, surrogate pairs)</li>
 *   <li>invalid inputs that should provoke {@code ClassCastException} or {@code NullPointerException}</li>
 * </ul>
 *
 * <p>The {@code Converter} instance used in the tests is created with a
 * {@code ConverterOptions} that supplies {@link StandardCharsets#UTF_8} – the
 * only option required by the class under test (its {@code toByteBuffer}
 * implementation uses {@code converter.getOptions().getCharset()}).</p>
 */
public class CharArrayConversionsTest {

    /** A {@code Converter} that uses UTF‑8 for all charset‑related operations. */
    private Converter utf8Converter;

    @Before
    public void setUp() {
        // The real library provides a way to create a ConverterOptions with a Charset.
        // The exact constructor/method names are not published here; we use the
        // most common pattern seen in the Cedar Software code base.
        ConverterOptions options = new ConverterOptions(StandardCharsets.UTF_8);
        utf8Converter = new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /*  List of public static methods in CharArrayConversions (extracted)   */
    /* ---------------------------------------------------------------------- */
    // static ByteBuffer toByteBuffer(Object from, Converter converter)
    // static String     toString(Object from, Converter converter)
    // static CharBuffer toCharBuffer(Object from, Converter converter)
    // static StringBuffer toStringBuffer(Object from, Converter converter)
    // static StringBuilder toStringBuilder(Object from, Converter converter)
    // static byte[]      toByteArray(Object from, Converter converter)
    // static char[]      toCharArray(Object from, Converter converter)

    /* ---------------------------------------------------------------------- */
    /*  Basic happy‑path tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString_HappyPath() {
        char[] source = {'J','u','n','i','t'};
        String result = CharArrayConversions.toString(source, utf8Converter);
        assertEquals("JUnit", result);
    }

    @Test
    public void testToCharBuffer_HappyPath() {
        char[] source = {'a','b','c'};
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, utf8Converter);
        assertEquals(3, buffer.length());
        assertEquals('a', buffer.get(0));
        assertEquals('b', buffer.get(1));
        assertEquals('c', buffer.get(2));
        // The CharBuffer should wrap the original array (no copy)
        assertSame(source, buffer.array());
    }

    @Test
    public void testToStringBuffer_HappyPath() {
        char[] source = {'x','y','z'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, utf8Converter);
        assertEquals("xyz", sb.toString());
    }

    @Test
    public void testToStringBuilder_HappyPath() {
        char[] source = {'1','2','3','4'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, utf8Converter);
        assertEquals("1234", sb.toString());
    }

    @Test
    public void testToByteArray_HappyPath() {
        char[] source = {'A', 'B', 'C'};
        byte[] bytes = CharArrayConversions.toByteArray(source, utf8Converter);
        // UTF‑8 encoding of "ABC" is 0x41 0x42 0x43
        byte[] expected = "ABC".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testToCharArray_HappyPath() {
        char[] source = {'p','q','r'};
        char[] copy = CharArrayConversions.toCharArray(source, utf8Converter);
        assertArrayEquals(source, copy);
        // Ensure a deep copy (different reference)
        assertNotSame(source, copy);
    }

    @Test
    public void testToByteBuffer_HappyPath() {
        char[] source = {'h','i'};
        ByteBuffer buffer = CharArrayConversions.toByteBuffer(source, utf8Converter);
        byte[] actual = new byte[buffer.remaining()];
        buffer.get(actual);
        assertArrayEquals("hi".getBytes(StandardCharsets.UTF_8), actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case tests                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyCharArray() {
        char[] empty = new char[0];

        assertEquals("", CharArrayConversions.toString(empty, utf8Converter));
        assertEquals(0, CharArrayConversions.toCharBuffer(empty, utf8Converter).length());
        assertEquals("", CharArrayConversions.toStringBuffer(empty, utf8Converter).toString());
        assertEquals("", CharArrayConversions.toStringBuilder(empty, utf8Converter).toString());
        assertArrayEquals(new byte[0], CharArrayConversions.toByteArray(empty, utf8Converter));
        assertArrayEquals(new char[0], CharArrayConversions.toCharArray(empty, utf8Converter));
        assertEquals(0, CharArrayConversions.toByteBuffer(empty, utf8Converter).remaining());
    }

    @Test
    public void testSurrogatePair() {
        // Unicode character U+1F600 (GRINNING FACE) -> surrogate pair in UTF‑16
        String smile = "\uD83D\uDE00";
        char[] chars = smile.toCharArray();

        // toString should preserve the surrogate pair
        assertEquals(smile, CharArrayConversions.toString(chars, utf8Converter));

        // toByteArray should encode the character in UTF‑8 (4 bytes: F0 9F 98 80)
        byte[] expected = smile.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, CharArrayConversions.toByteArray(chars, utf8Converter));
    }

    @Test
    public void testLargeCharArray() {
        int size = 10_000;
        char[] large = new char[size];
        for (int i = 0; i < size; i++) {
            large[i] = (char) ('a' + (i % 26));
        }

        // Verify that the conversion methods handle the large input without error
        assertEquals(new String(large), CharArrayConversions.toString(large, utf8Converter));
        assertEquals(size, CharArrayConversions.toCharBuffer(large, utf8Converter).remaining());
        assertEquals(size, CharArrayConversions.toByteArray(large, utf8Converter).length);
    }

    /* ---------------------------------------------------------------------- */
    /*  Exception / invalid‑input tests                                      */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        CharArrayConversions.toString(null, utf8Converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        CharArrayConversions.toString("not a char[]", utf8Converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharBuffer_NullInput() {
        CharArrayConversions.toCharBuffer(null, utf8Converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharBuffer_WrongType() {
        CharArrayConversions.toCharBuffer(12345, utf8Converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_NullInput() {
        CharArrayConversions.toStringBuffer(null, utf8Converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_WrongType() {
        CharArrayConversions.toStringBuffer(new Object(), utf8Converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_NullInput() {
        CharArrayConversions.toStringBuilder(null, utf8Converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_WrongType() {
        CharArrayConversions.toStringBuilder(Boolean.TRUE, utf8Converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArray_NullInput() {
        CharArrayConversions.toByteArray(null, utf8Converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteArray_WrongType() {
        CharArrayConversions.toByteArray(new int[]{1,2,3}, utf8Converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullInput() {
        CharArrayConversions.toByteBuffer(null, utf8Converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteBuffer_WrongType() {
        CharArrayConversions.toByteBuffer(new String("abc"), utf8Converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_NullInput() {
        CharArrayConversions.toCharArray(null, utf8Converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_WrongType() {
        CharArrayConversions.toCharArray(new byte[]{65,66}, utf8Converter);
    }
}
