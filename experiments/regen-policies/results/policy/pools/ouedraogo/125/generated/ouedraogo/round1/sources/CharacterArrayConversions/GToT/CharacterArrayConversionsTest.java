package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@code CharacterArrayConversions}.
 *
 * The class under test contains three package‑private static methods that
 * convert a {@code Character[]} array into a {@code String}, {@code StringBuilder},
 * or {@code StringBuffer}.  All methods ignore the supplied {@code Converter}
 * instance, so the tests pass {@code null} for that argument.
 *
 * The test cases cover:
 *   • Normal usage (typical non‑empty arrays)
 *   • Empty arrays
 *   • {@code null} as the source array
 *   • Arrays that contain {@code null} elements (which cause NPE because of unboxing)
 *   • Passing an object of the wrong type (causes {@code ClassCastException})
 *   • Basic sanity checks on the returned mutable objects (capacity, content)
 */
public class CharacterArrayConversionsTest {

    /* -------------------------------------------------
     *  Tests for toString(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToStringTypical() {
        Character[] input = new Character[] {'a', 'b', 'c'};
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("abc", result);
    }

    @Test
    public void testToStringEmpty() {
        Character[] input = new Character[0];
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullArray() {
        CharacterArrayConversions.toString(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringArrayWithNullElement() {
        Character[] input = new Character[] {'x', null, 'y'};
        CharacterArrayConversions.toString(input, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringWrongType() {
        Object notCharArray = new String[] {"a", "b"};
        CharacterArrayConversions.toString(notCharArray, null);
    }

    /* -------------------------------------------------
     *  Tests for toStringBuilder(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToStringBuilderTypical() {
        Character[] input = new Character[] {'J', 'U', 'N', 'I', 'T'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertEquals("JUNIT", sb.toString());
        // The constructor was given the exact length as the initial capacity.
        assertTrue(sb.capacity() >= input.length);
    }

    @Test
    public void testToStringBuilderEmpty() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertEquals("", sb.toString());
        assertTrue(sb.capacity() >= 0);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilderNullArray() {
        CharacterArrayConversions.toStringBuilder(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilderArrayWithNullElement() {
        Character[] input = new Character[] {null};
        CharacterArrayConversions.toStringBuilder(input, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilderWrongType() {
        Object notCharArray = new Integer[] {1, 2, 3};
        CharacterArrayConversions.toStringBuilder(notCharArray, null);
    }

    /* -------------------------------------------------
     *  Tests for toStringBuffer(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToStringBufferTypical() {
        Character[] input = new Character[] {'b', 'u', 'f', 'f', 'e', 'r'};
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, null);
        assertEquals("buffer", sb.toString());
        assertTrue(sb.capacity() >= input.length);
    }

    @Test
    public void testToStringBufferEmpty() {
        Character[] input = new Character[0];
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, null);
        assertEquals("", sb.toString());
        assertTrue(sb.capacity() >= 0);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBufferNullArray() {
        CharacterArrayConversions.toStringBuffer(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBufferArrayWithNullElement() {
        Character[] input = new Character[] {'a', null};
        CharacterArrayConversions.toStringBuffer(input, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBufferWrongType() {
        Object notCharArray = new Object();
        CharacterArrayConversions.toStringBuffer(notCharArray, null);
    }
}
