package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterArrayConversionsTest {

    /* ---------- Helper utilities ---------- */

    /** Simple helper to invoke the conversion methods without needing a real Converter. */
    private static final Converter DUMMY_CONVERTER = null;   // the methods ignore this argument

    private static Character[] toCharacterArray(String s) {
        if (s == null) return null;
        Character[] result = new Character[s.length()];
        for (int i = 0; i < s.length(); i++) {
            result[i] = s.charAt(i);
        }
        return result;
    }

    /* ---------- Tests for toString(Object, Converter) ---------- */

    @Test
    public void testToStringTypical() {
        Character[] input = new Character[] { 'J', 'u', 'n', 'i', 't' };
        String expected = "Junit";
        String actual = CharacterArrayConversions.toString(input, DUMMY_CONVERTER);
        assertEquals(expected, actual);
    }

    @Test
    public void testToStringEmptyArray() {
        Character[] input = new Character[0];
        String actual = CharacterArrayConversions.toString(input, DUMMY_CONVERTER);
        assertEquals("", actual);
    }

    @Test
    public void testToStringArrayWithNullElements() {
        Character[] input = new Character[] { 'A', null, 'B' };
        // null element is appended via StringBuilder.append(Object) => "null"
        String expected = "AnullB";
        String actual = CharacterArrayConversions.toString(input, DUMMY_CONVERTER);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullArrayThrowsNPE() {
        CharacterArrayConversions.toString(null, DUMMY_CONVERTER);
    }

    @Test
    public void testToStringLargeArray() {
        int size = 10_000;
        Character[] input = new Character[size];
        StringBuilder expected = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            char ch = (char) ('a' + (i % 26));
            input[i] = ch;
            expected.append(ch);
        }
        String actual = CharacterArrayConversions.toString(input, DUMMY_CONVERTER);
        assertEquals(expected.toString(), actual);
    }

    /* ---------- Tests for toStringBuilder(Object, Converter) ---------- */

    @Test
    public void testToStringBuilderTypical() {
        Character[] input = new Character[] { 'J', 'a', 'v', 'a' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, DUMMY_CONVERTER);
        assertEquals("Java", sb.toString());
    }

    @Test
    public void testToStringBuilderEmptyArray() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, DUMMY_CONVERTER);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuilderArrayWithNullElements() {
        Character[] input = new Character[] { null, 'X', null };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, DUMMY_CONVERTER);
        // Each null becomes the string "null"
        assertEquals("nullXnull", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilderNullArrayThrowsNPE() {
        CharacterArrayConversions.toStringBuilder(null, DUMMY_CONVERTER);
    }

    @Test
    public void testToStringBuilderIndependence() {
        Character[] input = new Character[] { 'T', 'e', 's', 't' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, DUMMY_CONVERTER);
        sb.append("X");
        // Original array must remain unchanged; method returns a new builder each call
        assertEquals("TestX", sb.toString());
        StringBuilder sb2 = CharacterArrayConversions.toStringBuilder(input, DUMMY_CONVERTER);
        assertEquals("Test", sb2.toString());
    }

    /* ---------- Tests for toStringBuffer(Object, Converter) ---------- */

    @Test
    public void testToStringBufferTypical() {
        Character[] input = new Character[] { 'B', 'u', 'f', 'f', 'e', 'r' };
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(input, DUMMY_CONVERTER);
        assertEquals("Buffer", buffer.toString());
    }

    @Test
    public void testToStringBufferEmptyArray() {
        Character[] input = new Character[0];
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(input, DUMMY_CONVERTER);
        assertEquals(0, buffer.length());
        assertEquals("", buffer.toString());
    }

    @Test
    public void testToStringBufferArrayWithNullElements() {
        Character[] input = new Character[] { 'Y', null };
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(input, DUMMY_CONVERTER);
        // null becomes "null"
        assertEquals("Ynull", buffer.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBufferNullArrayThrowsNPE() {
        CharacterArrayConversions.toStringBuffer(null, DUMMY_CONVERTER);
    }

    @Test
    public void testToStringBufferThreadSafetyHint() {
        // StringBuffer is synchronized; we simply verify that the content matches expectations.
        Character[] input = new Character[] { 'S', 'y', 'n', 'c' };
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(input, DUMMY_CONVERTER);
        assertEquals("Sync", buffer.toString());
    }
}
