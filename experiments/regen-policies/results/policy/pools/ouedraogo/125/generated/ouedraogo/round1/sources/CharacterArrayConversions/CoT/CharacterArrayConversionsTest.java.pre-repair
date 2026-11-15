package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterArrayConversionsTest {

    /* ------------------- Basic functional tests ------------------- */

    @Test
    public void testToStringNormal() {
        Character[] src = new Character[] { 'J', 'a', 'v', 'a' };
        String result = CharacterArrayConversions.toString(src, null);
        assertEquals("Java", result);
    }

    @Test
    public void testToStringBuilderNormal() {
        Character[] src = new Character[] { 'T', 'e', 's', 't' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(src, null);
        assertEquals("Test", sb.toString());
    }

    @Test
    public void testToStringBufferNormal() {
        Character[] src = new Character[] { 'U', 'n', 'i', 't' };
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(src, null);
        assertEquals("Unit", sb.toString());
    }

    /* ------------------- Edge‑case / exception tests ------------------- */

    // Null source argument – expect NullPointerException
    @Test(expected = NullPointerException.class)
    public void testToString_NullSource() {
        CharacterArrayConversions.toString(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_NullSource() {
        CharacterArrayConversions.toStringBuilder(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_NullSource() {
        CharacterArrayConversions.toStringBuffer(null, null);
    }

    // Wrong source type – expect ClassCastException
    @Test(expected = ClassCastException.class)
    public void testToString_WrongSourceType() {
        String[] wrong = new String[] { "a", "b" };
        CharacterArrayConversions.toString(wrong, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_WrongSourceType() {
        Integer[] wrong = new Integer[] { 1, 2 };
        CharacterArrayConversions.toStringBuilder(wrong, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_WrongSourceType() {
        Object[] wrong = new Object[] { new Object() };
        CharacterArrayConversions.toStringBuffer(wrong, null);
    }

    // Empty array – should produce empty result
    @Test
    public void testToString_EmptyArray() {
        Character[] empty = new Character[0];
        assertEquals("", CharacterArrayConversions.toString(empty, null));
    }

    @Test
    public void testToStringBuilder_EmptyArray() {
        Character[] empty = new Character[0];
        assertEquals("", CharacterArrayConversions.toStringBuilder(empty, null).toString());
    }

    @Test
    public void testToStringBuffer_EmptyArray() {
        Character[] empty = new Character[0];
        assertEquals("", CharacterArrayConversions.toStringBuffer(empty, null).toString());
    }

    // Array containing null elements – "null" literal should appear
    @Test
    public void testToString_ArrayWithNulls() {
        Character[] src = new Character[] { 'A', null, 'B' };
        assertEquals("AnullB", CharacterArrayConversions.toString(src, null));
    }

    @Test
    public void testToStringBuilder_ArrayWithNulls() {
        Character[] src = new Character[] { null, 'X', null };
        assertEquals("nullXnull", CharacterArrayConversions.toStringBuilder(src, null).toString());
    }

    @Test
    public void testToStringBuffer_ArrayWithNulls() {
        Character[] src = new Character[] { null };
        assertEquals("null", CharacterArrayConversions.toStringBuffer(src, null).toString());
    }

    // Converter argument is null – method should still work
    @Test
    public void testToString_WithNullConverter() {
        Character[] src = new Character[] { 'N', 'u', 'l', 'l' };
        assertEquals("Null", CharacterArrayConversions.toString(src, null));
    }
}
