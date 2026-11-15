package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterArrayConversionsTest {

    /** Helper to create a dummy Converter – the implementation details are irrelevant for the
     *  methods under test because they never use the Converter instance. */
    private static Converter dummyConverter() {
        // ConverterOptions has a public no‑arg constructor in the library.
        return new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Tests for toString(Object, Converter)
     * -------------------------------------------------------------- */

    @Test
    public void toString_normalCharacters() {
        Character[] input = new Character[]{'J', 'u', 'n', 'i', 't'};
        String result = CharacterArrayConversions.toString(input, dummyConverter());
        assertEquals("JUnit", result);
    }

    @Test
    public void toString_emptyArray() {
        Character[] input = new Character[0];
        String result = CharacterArrayConversions.toString(input, dummyConverter());
        assertEquals("", result);
    }

    @Test
    public void toString_withNullElements() {
        Character[] input = new Character[]{'A', null, 'B'};
        // StringBuilder appends the literal "null" for a null reference.
        String result = CharacterArrayConversions.toString(input, dummyConverter());
        assertEquals("AnullB", result);
    }

    @Test
    public void toString_nullConverterIsAllowed() {
        Character[] input = new Character[]{'X', 'Y'};
        // The method never dereferences the converter, so passing null must not fail.
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("XY", result);
    }

    @Test(expected = NullPointerException.class)
    public void toString_nullInputArrayThrowsNPE() {
        // Casting null to Character[] yields null; accessing length triggers NPE.
        CharacterArrayConversions.toString(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toString_wrongInputTypeThrowsClassCastException() {
        // Passing an object that is not a Character[] should cause a ClassCastException
        // when the method attempts to cast.
        Object notAnArray = new int[]{1, 2, 3};
        CharacterArrayConversions.toString(notAnArray, dummyConverter());
    }

    /* --------------------------------------------------------------
     *  Tests for toStringBuilder(Object, Converter)
     * -------------------------------------------------------------- */

    @Test
    public void toStringBuilder_normalCharacters() {
        Character[] input = new Character[]{'T', 'e', 's', 't'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter());
        assertEquals("Test", sb.toString());
    }

    @Test
    public void toStringBuilder_emptyArray() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter());
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void toStringBuilder_withNullElements() {
        Character[] input = new Character[]{null, 'C'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter());
        // null element becomes the string "null"
        assertEquals("nullC", sb.toString());
    }

    @Test
    public void toStringBuilder_nullConverterIsAllowed() {
        Character[] input = new Character[]{'1', '2'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertEquals("12", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void toStringBuilder_nullInputArrayThrowsNPE() {
        CharacterArrayConversions.toStringBuilder(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toStringBuilder_wrongInputTypeThrowsClassCastException() {
        Object notACharArray = "not an array";
        CharacterArrayConversions.toStringBuilder(notACharArray, dummyConverter());
    }

    /* --------------------------------------------------------------
     *  Tests for toStringBuffer(Object, Converter)
     * -------------------------------------------------------------- */

    @Test
    public void toStringBuffer_normalCharacters() {
        Character[] input = new Character[]{'B', 'u', 'f', 'f', 'e', 'r'};
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, dummyConverter());
        assertEquals("Buffer", sb.toString());
    }

    @Test
    public void toStringBuffer_emptyArray() {
        Character[] input = new Character[0];
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, dummyConverter());
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void toStringBuffer_withNullElements() {
        Character[] input = new Character[]{'X', null, 'Y'};
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, dummyConverter());
        assertEquals("XnullY", sb.toString());
    }

    @Test
    public void toStringBuffer_nullConverterIsAllowed() {
        Character[] input = new Character[]{'A'};
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, null);
        assertEquals("A", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void toStringBuffer_nullInputArrayThrowsNPE() {
        CharacterArrayConversions.toStringBuffer(null, dummyConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toStringBuffer_wrongInputTypeThrowsClassCastException() {
        Object notACharArray = new Object();
        CharacterArrayConversions.toStringBuffer(notACharArray, dummyConverter());
    }
}
