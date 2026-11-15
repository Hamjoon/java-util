package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterArrayConversionsTest {

    /** 
     * The conversion methods do not use the {@link Converter} instance, 
     * so we can safely create one with {@code null} options.
     */
    private final Converter dummyConverter = new Converter(null);

    /* --------------------------------------------------------------------- *
     *  toString() tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToString_WithTypicalCharacters() {
        Character[] input = new Character[] {'H', 'e', 'l', 'l', 'o'};
        String result = CharacterArrayConversions.toString(input, dummyConverter);
        assertEquals("Hello", result);
    }

    @Test
    public void testToString_WithEmptyArray() {
        Character[] input = new Character[0];
        String result = CharacterArrayConversions.toString(input, dummyConverter);
        assertEquals("", result);
    }

    @Test
    public void testToString_WithNullElements() {
        Character[] input = new Character[] {null, 'A', null};
        String result = CharacterArrayConversions.toString(input, dummyConverter);
        // StringBuilder.append(Object) converts a null reference to the literal "null"
        assertEquals("nullAnull", result);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WithInvalidSourceType() {
        // The method unconditionally casts to Character[]; any other type causes a ClassCastException.
        Object notCharArray = new int[] {1, 2, 3};
        CharacterArrayConversions.toString(notCharArray, dummyConverter);
    }

    /* --------------------------------------------------------------------- *
     *  toStringBuilder() tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringBuilder_BasicContent() {
        Character[] input = new Character[] {'J', 'U', 'N', 'I', 'T'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);
        assertEquals("JUNIT", sb.toString());
    }

    @Test
    public void testToStringBuilder_WithEmptyArray() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuilder_ModifyResultDoesNotAffectSource() {
        Character[] input = new Character[] {'A', 'B', 'C'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);
        assertEquals("ABC", sb.toString());

        // mutate the returned builder
        sb.append("D");
        assertEquals("ABCD", sb.toString());

        // the original array should still produce the original string
        String again = CharacterArrayConversions.toString(input, dummyConverter);
        assertEquals("ABC", again);
    }

    @Test
    public void testToStringBuilder_WithNullElements() {
        Character[] input = new Character[] {null, 'X'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);
        assertEquals("nullX", sb.toString());
    }

    /* --------------------------------------------------------------------- *
     *  toStringBuffer() tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringBuffer_BasicContent() {
        Character[] input = new Character[] {'B', 'u', 'f', 'f', 'e', 'r'};
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, dummyConverter);
        assertEquals("Buffer", buf.toString());
    }

    @Test
    public void testToStringBuffer_WithEmptyArray() {
        Character[] input = new Character[0];
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, dummyConverter);
        assertEquals(0, buf.length());
        assertEquals("", buf.toString());
    }

    @Test
    public void testToStringBuffer_WithNullElements() {
        Character[] input = new Character[] {null, 'Z'};
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, dummyConverter);
        // null becomes the literal "null"
        assertEquals("nullZ", buf.toString());
    }

    @Test
    public void testToStringBuffer_ModifyResultDoesNotAffectSource() {
        Character[] input = new Character[] {'1', '2'};
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, dummyConverter);
        assertEquals("12", buf.toString());

        buf.append("3");
        assertEquals("123", buf.toString());

        // original array still yields the original value
        String again = CharacterArrayConversions.toString(input, dummyConverter);
        assertEquals("12", again);
    }
}
