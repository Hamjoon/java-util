package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CharArrayConversions}.
 *
 * The tests are written from the perspective of three independent experts.
 * Each expert contributes a single test case for each public static method.
 * The suite therefore covers:
 *
 * <ul>
 *   <li>normal usage (non‑empty character arrays)</li>
 *   <li>edge cases (empty arrays, null arguments)</li>
 *   <li>error scenarios (wrong source type, missing charset)</li>
 *   <li>behavioural contracts (copy vs. reference, charset encoding)</li>
 * </ul>
 *
 * Only the classes, constructors and methods that appear in the source
 * of {@code CharArrayConversions} or in the public API of {@code Converter}
 * are used – no mocking frameworks or external libraries are required.
 */
public class CharArrayConversionsTest {

    /** A reusable {@link Converter} whose {@link ConverterOptions} supplies the
     *  default platform charset.  The concrete {@code ConverterOptions}
     *  class is not part of the public API shown, but the Cedar library
     *  provides a no‑arg constructor that initialises the charset to
     *  {@code Charset.defaultCharset()}.  This is sufficient for the
     *  encoding‑related tests. */
    private Converter defaultConverter;

    @Before
    public void setUp() {
        // The library offers a public constructor {@code new Converter(ConverterOptions)}.
        //  Using the no‑arg constructor of {@code ConverterOptions} yields a default charset.
        //  (If the library version used does not have a no‑arg ctor, the test will fail
        //   at compile time – the test suite is written against the documented API.)
        defaultConverter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Expert 1 – tests for the String‑related conversions
     * -------------------------------------------------------------- */

    @Test
    public void testToString_WithTypicalData() {
        char[] source = new char[]{'J', 'u', 'n', 'i', 't'};
        String expected = "JUnit";

        String result = CharArrayConversions.toString(source, defaultConverter);
        assertEquals(expected, result);
    }

    @Test
    public void testToString_WithEmptyArray() {
        char[] source = new char[0];
        String result = CharArrayConversions.toString(source, defaultConverter);
        assertEquals("", result);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WithWrongSourceType() {
        // The method casts the argument to {@code char[]}; any other type must fail.
        CharArrayConversions.toString("not a char array", defaultConverter);
    }

    /* --------------------------------------------------------------
     *  Expert 2 – tests for the Buffer and Builder conversions
     * -------------------------------------------------------------- */

    @Test
    public void testToCharBuffer_Typical() {
        char[] source = new char[]{'A', 'B', 'C'};
        CharBuffer buffer = CharArrayConversions.toCharBuffer(source, defaultConverter);
        assertNotNull(buffer);
        assertEquals(3, buffer.remaining());
        assertEquals('A', buffer.get());
        assertEquals('B', buffer.get());
        assertEquals('C', buffer.get());
    }

    @Test
    public void testToCharBuffer_EmptyArray() {
        CharBuffer buffer = CharArrayConversions.toCharBuffer(new char[0], defaultConverter);
        assertNotNull(buffer);
        assertEquals(0, buffer.remaining());
    }

    @Test
    public void testToStringBuffer_Typical() {
        char[] source = new char[]{'x', 'y', 'z'};
        StringBuffer sb = CharArrayConversions.toStringBuffer(source, defaultConverter);
        assertEquals("xyz", sb.toString());
    }

    @Test
    public void testToStringBuilder_Typical() {
        char[] source = new char[]{'1', '2', '3'};
        StringBuilder sb = CharArrayConversions.toStringBuilder(source, defaultConverter);
        assertEquals("123", sb.toString());
    }

    @Test
    public void testToByteBuffer_WithDefaultCharset() {
        char[] source = new char[]{'h', 'e', 'l', 'l', 'o'};
        ByteBuffer byteBuffer = CharArrayConversions.toByteBuffer(source, defaultConverter);
        assertNotNull(byteBuffer);
        // Encode the same chars with the charset from the converter options
        Charset cs = defaultConverter.getOptions().getCharset();
        byte[] expected = cs.encode(CharBuffer.wrap(source)).array();

        // The buffer may be a slice; compare the remaining bytes.
        byte[] actual = new byte[byteBuffer.remaining()];
        byteBuffer.get(actual);
        assertArrayEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteBuffer_NullConverterOptions() {
        // Build a Converter with a null options reference to provoke NPE.
        Converter broken = new Converter(null);
        CharArrayConversions.toByteBuffer(new char[]{'x'}, broken);
    }

    @Test
    public void testToByteArray_Typical() {
        char[] source = new char[]{'J', 'a', 'v', 'a'};
        byte[] bytes = CharArrayConversions.toByteArray(source, defaultConverter);
        assertNotNull(bytes);
        Charset cs = defaultConverter.getOptions().getCharset();
        byte[] expected = cs.encode(CharBuffer.wrap(source)).array();
        assertArrayEquals(expected, bytes);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteArray_NullConverter() {
        // Passing a null converter triggers a NPE when the method attempts
        // to dereference it for the charset.
        CharArrayConversions.toByteArray(new char[]{'a'}, null);
    }

    /* --------------------------------------------------------------
     *  Expert 3 – tests for the array‑copy conversion
     * -------------------------------------------------------------- */

    @Test
    public void testToCharArray_CopiesContent() {
        char[] original = new char[]{'c', 'o', 'p', 'y'};
        char[] copy = CharArrayConversions.toCharArray(original, defaultConverter);
        assertArrayEquals(original, copy);
        // Must be a different instance (defensive copy)
        assertNotSame(original, copy);
    }

    @Test
    public void testToCharArray_EmptyArray() {
        char[] empty = new char[0];
        char[] result = CharArrayConversions.toCharArray(empty, defaultConverter);
        assertNotNull(result);
        assertEquals(0, result.length);
        assertNotSame(empty, result);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharArray_WrongSourceType() {
        // Force a ClassCastException by passing an incompatible object.
        CharArrayConversions.toCharArray("string", defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharArray_NullSource() {
        // Null source leads to a NPE when the method attempts the cast.
        CharArrayConversions.toCharArray(null, defaultConverter);
    }
}
