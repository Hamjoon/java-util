package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link CharacterArrayConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversion of a non‑empty {@code Character[]} to {@code String},
 *       {@code StringBuilder} and {@code StringBuffer}.</li>
 *   <li>Conversion of an empty array (should yield an empty result).</li>
 *   <li>Behaviour when the input array contains {@code null} elements
 *       (expected {@link NullPointerException} because of unboxing).</li>
 *   <li>Behaviour when the supplied {@code Object} is not a {@code Character[]} 
 *       (expected {@link ClassCastException}).</li>
 *   <li>Passing a {@code null} {@link Converter} (the converter is unused, so it must not affect the result).</li>
 * </ul>
 */
public class CharacterArrayConversionsTest {

    /** Helper to obtain a {@link Converter} instance – the implementation does not use it. */
    private static Converter newConverter() {
        // The public API allows {@code null} options.
        return new Converter(null);
    }

    /* -------------------------------------------------
     *  Tests for toString(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToString_WithTypicalCharacters() {
        Character[] input = new Character[] { 'a', 'b', 'c', '1', '@' };
        String result = CharacterArrayConversions.toString(input, newConverter());
        assertEquals("abc1@", result);
    }

    @Test
    public void testToString_WithEmptyArray() {
        Character[] input = new Character[0];
        String result = CharacterArrayConversions.toString(input, newConverter());
        assertEquals("", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_WithNullElementThrowsNPE() {
        Character[] input = new Character[] { 'x', null, 'y' };
        // The method should attempt to unbox the null Character and throw NPE.
        CharacterArrayConversions.toString(input, newConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WithWrongArrayTypeThrowsClassCastException() {
        // Pass a String[] instead of Character[]
        Object notCharacterArray = new String[] { "a", "b" };
        CharacterArrayConversions.toString(notCharacterArray, newConverter());
    }

    @Test
    public void testToString_WithNullConverterDoesNotAffectResult() {
        Character[] input = new Character[] { 'J', 'U', 'N', 'I', 'T' };
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("JUNIT", result);
    }

    /* -------------------------------------------------
     *  Tests for toStringBuilder(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToStringBuilder_WithTypicalCharacters() {
        Character[] input = new Character[] { 'h', 'e', 'l', 'l', 'o' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, newConverter());
        assertNotNull(sb);
        assertEquals("hello", sb.toString());
        assertEquals(5, sb.length());
    }

    @Test
    public void testToStringBuilder_WithEmptyArray() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, newConverter());
        assertNotNull(sb);
        assertEquals("", sb.toString());
        assertEquals(0, sb.length());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_WithNullElementThrowsNPE() {
        Character[] input = new Character[] { null };
        CharacterArrayConversions.toStringBuilder(input, newConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_WithWrongArrayTypeThrowsClassCastException() {
        Object notCharacterArray = new Integer[] { 1, 2, 3 };
        CharacterArrayConversions.toStringBuilder(notCharacterArray, newConverter());
    }

    @Test
    public void testToStringBuilder_WithNullConverterDoesNotAffectResult() {
        Character[] input = new Character[] { 'J', 'A', 'V', 'A' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertEquals("JAVA", sb.toString());
    }

    /* -------------------------------------------------
     *  Tests for toStringBuffer(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToStringBuffer_WithTypicalCharacters() {
        Character[] input = new Character[] { 'b', 'u', 'f', 'f', 'e', 'r' };
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(input, newConverter());
        assertNotNull(buffer);
        assertEquals("buffer", buffer.toString());
        assertEquals(6, buffer.length());
    }

    @Test
    public void testToStringBuffer_WithEmptyArray() {
        Character[] input = new Character[0];
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(input, newConverter());
        assertNotNull(buffer);
        assertEquals("", buffer.toString());
        assertEquals(0, buffer.length());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_WithNullElementThrowsNPE() {
        Character[] input = new Character[] { 'a', null };
        CharacterArrayConversions.toStringBuffer(input, newConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_WithWrongArrayTypeThrowsClassCastException() {
        Object notCharacterArray = new Double[] { 1.0, 2.0 };
        CharacterArrayConversions.toStringBuffer(notCharacterArray, newConverter());
    }

    @Test
    public void testToStringBuffer_WithNullConverterDoesNotAffectResult() {
        Character[] input = new Character[] { 'T', 'e', 's', 't' };
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(input, null);
        assertEquals("Test", buffer.toString());
    }
}
