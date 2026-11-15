package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Unit tests for {@link CharArrayConversions}.
 * The tests are placed in the same package to allow access to package‑private static methods.
 */
public class CharArrayConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter class requires a ConverterOptions instance.
        // We rely on the default constructor of ConverterOptions (assumed to exist)
        // and the default charset (UTF‑8) for deterministic results.
        ConverterOptions options = new ConverterOptions(); // default options
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- */
    /*  List of methods under test (package‑private static)                  */
    /* --------------------------------------------------------------------- */
    // static ByteBuffer toByteBuffer(Object from, Converter converter)
    // static String toString(Object from, Converter converter)
    // static CharBuffer toCharBuffer(Object from, Converter converter)
    // static StringBuffer toStringBuffer(Object from, Converter converter)
    // static StringBuilder toStringBuilder(Object from, Converter converter)
    // static byte[] toByteArray(Object from, Converter converter)
    // static char[] toCharArray(Object from, Converter converter)

    /* --------------------------------------------------------------------- */
    /*  Basic functionality tests                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_basic() {
        char[] data = {'a', 'b', 'c'};
        CharBuffer buf = CharArrayConversions.toCharBuffer(data, converter);
        assertNotNull(buf);
        assertEquals(3, buf.remaining());
        assertEquals('a', buf.get());
        assertEquals('b', buf.get());
        assertEquals('c', buf.get());
    }

    @Test
    public void testToString_basic() {
        char[] data = {'h', 'e', 'l', 'l', 'o'};
        String result = CharArrayConversions.toString(data, converter);
        assertEquals("hello", result);
    }

    @Test
    public void testToStringBuffer_basic() {
        char[] data = {'J', 'U', 'N', 'I', 'T'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(data, converter);
        assertNotNull(sb);
        assertEquals("JUNIT", sb.toString());
    }

    @Test
    public void testToStringBuilder_basic() {
        char[] data = {'t', 'e', 's', 't'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(data, converter);
        assertNotNull(sb);
        assertEquals("test", sb.toString());
    }

    @Test
    public void testToByteArray_basic() {
        char[] data = {'A', 'B', 'C'};
        byte[] bytes = CharArrayConversions.toByteArray(data, converter);
        // Encode using the same charset the Converter uses (default UTF‑8)
        byte[] expected = new String(data).getBytes(converter.getOptions().getCharset());
        assertArrayEquals(expected, bytes);
    }

    @Test
    public void testToByteBuffer_basic() {
        char[] data = {'x', 'y', 'z'};
        ByteBuffer bb = CharArrayConversions.toByteBuffer(data, converter);
        assertNotNull(bb);
        byte[] actual = new byte[bb.remaining()];
        bb.get(actual);
        byte[] expected = new String(data).getBytes(converter.getOptions().getCharset());
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testToCharArray_basic() {
        char[] original = {'p', 'q', 'r'};
        char[] copy = CharArrayConversions.toCharArray(original, converter);
        assertArrayEquals(original, copy);
        // Ensure it is a copy, not the same reference
        assertNotSame(original, copy);
    }

    /* --------------------------------------------------------------------- */
    /*  Edge case / exception tests                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEmptyCharArray() {
        char[] empty = new char[0];

        assertEquals("", CharArrayConversions.toString(empty, converter));
        assertEquals(0, CharArrayConversions.toCharBuffer(empty, converter).remaining());
        assertEquals(0, CharArrayConversions.toByteArray(empty, converter).length);
        assertEquals(0, CharArrayConversions.toByteBuffer(empty, converter).remaining());
        assertEquals("", CharArrayConversions.toStringBuffer(empty, converter).toString());
        assertEquals("", CharArrayConversions.toStringBuilder(empty, converter).toString());
        assertEquals(0, CharArrayConversions.toCharArray(empty, converter).length);
    }

    @Test
    public void testNullInput_ShouldThrowNullPointerException() {
        // All conversion methods dereference the Object argument immediately,
        // so a null argument results in NPE (or ClassCastException) depending on the cast.
        try {
            CharArrayConversions.toString(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException | ClassCastException e) {
            // acceptable – the contract does not define behaviour for null
        }

        try {
            CharArrayConversions.toCharBuffer(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException | ClassCastException e) {
        }

        try {
            CharArrayConversions.toStringBuffer(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException | ClassCastException e) {
        }

        try {
            CharArrayConversions.toStringBuilder(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException | ClassCastException e) {
        }

        try {
            CharArrayConversions.toByteArray(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException | ClassCastException e) {
        }

        try {
            CharArrayConversions.toByteBuffer(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException | ClassCastException e) {
        }

        try {
            CharArrayConversions.toCharArray(null, converter);
            fail("Expected NullPointerException");
        } catch (NullPointerException | ClassCastException e) {
        }
    }

    @Test(expected = ClassCastException.class)
    public void testNonCharArray_toString_throwsClassCastException() {
        CharArrayConversions.toString("not a char array", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNonCharArray_toCharBuffer_throwsClassCastException() {
        CharArrayConversions.toCharBuffer(12345, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNonCharArray_toStringBuffer_throwsClassCastException() {
        CharArrayConversions.toStringBuffer(new int[]{1, 2, 3}, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNonCharArray_toStringBuilder_throwsClassCastException() {
        CharArrayConversions.toStringBuilder(new Object(), converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNonCharArray_toByteArray_throwsClassCastException() {
        CharArrayConversions.toByteArray(new boolean[]{true, false}, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNonCharArray_toByteBuffer_throwsClassCastException() {
        CharArrayConversions.toByteBuffer(new double[]{1.0, 2.0}, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNonCharArray_toCharArray_throwsClassCastException() {
        CharArrayConversions.toCharArray(new java.util.Date(), converter);
    }

    @Test
    public void testLargeCharArray() {
        // Build a relatively large array (> 10,000 chars) to ensure buffers handle it.
        int size = 15000;
        char[] large = new char[size];
        for (int i = 0; i < size; i++) {
            large[i] = (char) ('a' + (i % 26));
        }

        // Verify that all conversion methods succeed and produce consistent results.
        String asString = CharArrayConversions.toString(large, converter);
        assertEquals(size, asString.length());

        CharBuffer cb = CharArrayConversions.toCharBuffer(large, converter);
        assertEquals(size, cb.remaining());

        byte[] bytes = CharArrayConversions.toByteArray(large, converter);
        assertEquals(asString.getBytes(converter.getOptions().getCharset()).length, bytes.length);

        ByteBuffer bb = CharArrayConversions.toByteBuffer(large, converter);
        assertEquals(bytes.length, bb.remaining());

        char[] copy = CharArrayConversions.toCharArray(large, converter);
        assertArrayEquals(large, copy);
        assertNotSame(large, copy);
    }

    /* --------------------------------------------------------------------- */
    /*  Charset handling verification (uses the charset supplied by options) */
    /* --------------------------------------------------------------------- */

    @Test
    public void testByteArrayMatchesCharsetEncoding() {
        // Use a charset that can represent a broader range of Unicode characters.
        // Assuming the ConverterOptions can be configured, we set UTF‑16BE.
        Charset utf16 = Charset.forName("UTF-16BE");
        ConverterOptions opts = new ConverterOptions();
        opts.setCharset(utf16);               // hypothetical setter
        Converter utf16Converter = new Converter(opts);

        char[] data = {'\u00A9', '\u20AC', '\uD83D', '\uDE00'}; // ©, €, 😀 (surrogate pair)
        byte[] bytes = CharArrayConversions.toByteArray(data, utf16Converter);
        byte[] expected = new String(data).getBytes(utf16);
        assertArrayEquals(expected, bytes);
    }
}
