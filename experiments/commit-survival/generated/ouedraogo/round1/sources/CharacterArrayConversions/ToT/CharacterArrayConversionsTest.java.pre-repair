package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterArrayConversionsTest {

    /** Helper to create a dummy Converter – the implementation is not used by the
     *  CharacterArrayConversions methods, but a non‑null instance is required by the
     *  method signatures. */
    private static Converter dummyConverter() {
        // ConverterOptions class is part of the library; we assume it has a public no‑arg ctor.
        return new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Tests for toString(Object, Converter)
     * -------------------------------------------------------------- */

    @Test
    public void testToStringTypicalCharacters() {
        Character[] input = new Character[] { 'J', 'u', 'n', 'i', 't' };
        String result = CharacterArrayConversions.toString(input, dummyConverter());
        assertEquals("JUnit", result);
    }

    @Test
    public void testToStringEmptyArray() {
        Character[] input = new Character[0];
        String result = CharacterArrayConversions.toString(input, dummyConverter());
        assertEquals("", result);
    }

    @Test
    public void testToStringArrayWithNullElements() {
        Character[] input = new Character[] { 'A', null, 'B' };
        // StringBuilder.append(null) appends the string "null"
        String result = CharacterArrayConversions.toString(input, dummyConverter());
        assertEquals("AnullB", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullArrayThrowsNPE() {
        CharacterArrayConversions.toString(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringWrongSourceTypeThrowsCCE() {
        // Passing a String[] instead of Character[] triggers the cast failure.
        Object wrong = new String[] { "x", "y" };
        CharacterArrayConversions.toString(wrong, dummyConverter());
    }

    @Test
    public void testToStringWithNullConverter() {
        Character[] input = new Character[] { 'N', 'U', 'L', 'L' };
        // The converter argument is ignored; passing null must not affect the result.
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("NULL", result);
    }

    /* --------------------------------------------------------------
     *  Tests for toStringBuilder(Object, Converter)
     * -------------------------------------------------------------- */

    @Test
    public void testToStringBuilderTypicalCharacters() {
        Character[] input = new Character[] { 'B', 'u', 'i', 'l', 'd' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter());
        assertEquals("Build", sb.toString());
    }

    @Test
    public void testToStringBuilderEmptyArray() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter());
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuilderArrayWithNullElements() {
        Character[] input = new Character[] { null, 'X', null };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter());
        // "nullXnull"
        assertEquals("nullXnull", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilderNullArrayThrowsNPE() {
        CharacterArrayConversions.toStringBuilder(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilderWrongSourceTypeThrowsCCE() {
        Object wrong = new Integer[] { 1, 2 };
        CharacterArrayConversions.toStringBuilder(wrong, dummyConverter());
    }

    @Test
    public void testToStringBuilderWithNullConverter() {
        Character[] input = new Character[] { 'Z' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertEquals("Z", sb.toString());
    }

    /* --------------------------------------------------------------
     *  Tests for toStringBuffer(Object, Converter)
     * -------------------------------------------------------------- */

    @Test
    public void testToStringBufferTypicalCharacters() {
        Character[] input = new Character[] { 'B', 'u', 'f', 'f', 'e', 'r' };
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, dummyConverter());
        assertEquals("Buffer", buf.toString());
    }

    @Test
    public void testToStringBufferEmptyArray() {
        Character[] input = new Character[0];
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, dummyConverter());
        assertEquals(0, buf.length());
        assertEquals("", buf.toString());
    }

    @Test
    public void testToStringBufferArrayWithNullElements() {
        Character[] input = new Character[] { 'X', null };
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, dummyConverter());
        // "Xnull"
        assertEquals("Xnull", buf.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBufferNullArrayThrowsNPE() {
        CharacterArrayConversions.toStringBuffer(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBufferWrongSourceTypeThrowsCCE() {
        Object wrong = new Double[] { 1.0 };
        CharacterArrayConversions.toStringBuffer(wrong, dummyConverter());
    }

    @Test
    public void testToStringBufferWithNullConverter() {
        Character[] input = new Character[] { 'A', 'B' };
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, null);
        assertEquals("AB", buf.toString());
    }
}
