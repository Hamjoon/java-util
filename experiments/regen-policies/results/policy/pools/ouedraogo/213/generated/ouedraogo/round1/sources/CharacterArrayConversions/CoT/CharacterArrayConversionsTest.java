package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterArrayConversionsTest {

    /** -----------------------------------------------------------------------
     *  List of public (package‑private) methods in {@code CharacterArrayConversions}
     *  -----------------------------------------------------------------------
     *
     *  1. static String toString(Object from, Converter converter)
     *  2. static StringBuilder toStringBuilder(Object from, Converter converter)
     *  3. static StringBuffer toStringBuffer(Object from, Converter converter)
     *
     *  All three methods expect {@code from} to be a {@code Character[]} and simply
     *  concatenate the characters into the respective mutable/immutable string type.
     *  The {@code converter} argument is not used inside the methods.
     * ----------------------------------------------------------------------- */

    /* -------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------- */

    @Test
    public void testToString_basic() {
        Character[] input = new Character[] { 'J', 'a', 'v', 'a' };
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("Java", result);
    }

    @Test
    public void testToStringBuilder_basic() {
        Character[] input = new Character[] { 'J', 'u', 'n', 'i', 't' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertEquals("Junit", sb.toString());
    }

    @Test
    public void testToStringBuffer_basic() {
        Character[] input = new Character[] { 'T', 'e', 's', 't' };
        StringBuffer sbuf = CharacterArrayConversions.toStringBuffer(input, null);
        assertEquals("Test", sbuf.toString());
    }

    /* -------------------------------------------------
     *  Edge case tests
     * ------------------------------------------------- */

    @Test
    public void testToString_emptyArray() {
        Character[] input = new Character[0];
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("", result);
    }

    @Test
    public void testToStringBuilder_emptyArray() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertEquals("", sb.toString());
        assertEquals(0, sb.length());
    }

    @Test
    public void testToStringBuffer_emptyArray() {
        Character[] input = new Character[0];
        StringBuffer sbuf = CharacterArrayConversions.toStringBuffer(input, null);
        assertEquals("", sbuf.toString());
        assertEquals(0, sbuf.length());
    }

    @Test
    public void testToString_arrayWithNullElements() {
        Character[] input = new Character[] { 'a', null, 'b', null };
        // StringBuilder.append(null) appends the string "null"
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("anullbnull", result);
    }

    @Test
    public void testToStringBuilder_arrayWithNullElements() {
        Character[] input = new Character[] { null, 'x', null };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertEquals("nullxnull", sb.toString());
    }

    @Test
    public void testToStringBuffer_arrayWithNullElements() {
        Character[] input = new Character[] { 'y', null };
        StringBuffer sbuf = CharacterArrayConversions.toStringBuffer(input, null);
        assertEquals("ynull", sbuf.toString());
    }

    @Test
    public void testToString_largeArray() {
        int size = 10_000;
        Character[] input = new Character[size];
        StringBuilder expected = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            char ch = (char) ('a' + (i % 26));
            input[i] = ch;
            expected.append(ch);
        }
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals(expected.toString(), result);
    }

    /* -------------------------------------------------
     *  Exception handling tests
     * ------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToString_invalidInput_notCharacterArray() {
        // Pass a String[] which cannot be cast to Character[]
        Object wrong = new String[] { "oops" };
        CharacterArrayConversions.toString(wrong, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_invalidInput_notCharacterArray() {
        Object wrong = new Integer[] { 1, 2, 3 };
        CharacterArrayConversions.toStringBuilder(wrong, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_invalidInput_notCharacterArray() {
        Object wrong = new Object();
        CharacterArrayConversions.toStringBuffer(wrong, null);
    }

    /* -------------------------------------------------
     *  Mutability / independence tests
     * ------------------------------------------------- */

    @Test
    public void testToStringBuilder_mutability() {
        Character[] input = new Character[] { 'm', 'u', 't', 'a', 'b', 'l', 'e' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        sb.append("-modified");
        // original array must remain unchanged
        assertEquals("mutable-modified", sb.toString());
        assertEquals('m', input[0].charValue());
    }

    @Test
    public void testToStringBuffer_mutability() {
        Character[] input = new Character[] { 'b', 'u', 'f', 'f', 'e', 'r' };
        StringBuffer sbuf = CharacterArrayConversions.toStringBuffer(input, null);
        sbuf.append("-changed");
        assertEquals("buffer-changed", sbuf.toString());
        assertEquals('b', input[0].charValue());
    }
}
