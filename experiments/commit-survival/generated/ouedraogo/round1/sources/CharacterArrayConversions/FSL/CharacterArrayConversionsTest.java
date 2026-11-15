package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link CharacterArrayConversions}.
 * The class is package‑private, therefore the test class lives in the same package.
 * All public behaviour (return values, exceptions) is verified.
 */
public class CharacterArrayConversionsTest {

    // ----------------------------------------------------------------------
    // Helper method – creates a Character[] from a var‑args of chars (including nulls)
    // ----------------------------------------------------------------------
    private static Character[] chars(Object... values) {
        Character[] result = new Character[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (Character) values[i];
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // toString(..) tests
    // ----------------------------------------------------------------------
    @Test
    public void toStringShouldReturnEmptyStringForEmptyArray() {
        Character[] empty = new Character[0];
        String result = CharacterArrayConversions.toString(empty, null);
        assertEquals("", result);
    }

    @Test
    public void toStringShouldConcatenateAllCharacters() {
        Character[] data = chars('H', 'e', 'l', 'l', 'o');
        String result = CharacterArrayConversions.toString(data, null);
        assertEquals("Hello", result);
    }

    @Test
    public void toStringShouldHandleNullElementsInsideArray() {
        Character[] data = chars('A', null, 'B');
        String result = CharacterArrayConversions.toString(data, null);
        // StringBuilder.append(Object) uses String.valueOf(Object) → "null"
        assertEquals("AnullB", result);
    }

    @Test(expected = NullPointerException.class)
    public void toStringShouldThrowNPEWhenArrayIsNull() {
        CharacterArrayConversions.toString(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void toStringShouldThrowClassCastWhenSourceIsNotCharacterArray() {
        // Passing a String[] forces a ClassCastException at the cast site.
        CharacterArrayConversions.toString(new String[]{"a", "b"}, null);
    }

    // ----------------------------------------------------------------------
    // toStringBuilder(..) tests
    // ----------------------------------------------------------------------
    @Test
    public void toStringBuilderShouldReturnBuilderWithCorrectContent() {
        Character[] data = chars('J', 'u', 'n', 'i', 't');
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(data, null);
        assertNotNull(sb);
        assertEquals("Junit", sb.toString());
    }

    @Test
    public void toStringBuilderShouldBeEmptyForEmptyArray() {
        Character[] empty = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(empty, null);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void toStringBuilderShouldIncludeLiteralNullWhenArrayContainsNull() {
        Character[] data = chars('X', null, 'Y');
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(data, null);
        assertEquals("XnullY", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void toStringBuilderShouldThrowNPEWhenArrayIsNull() {
        CharacterArrayConversions.toStringBuilder(null, null);
    }

    // ----------------------------------------------------------------------
    // toStringBuffer(..) tests
    // ----------------------------------------------------------------------
    @Test
    public void toStringBufferShouldReturnBufferWithCorrectContent() {
        Character[] data = chars('B', 'u', 'f', 'f', 'e', 'r');
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(data, null);
        assertNotNull(buffer);
        assertEquals("Buffer", buffer.toString());
    }

    @Test
    public void toStringBufferShouldBeEmptyForEmptyArray() {
        Character[] empty = new Character[0];
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(empty, null);
        assertEquals(0, buffer.length());
        assertEquals("", buffer.toString());
    }

    @Test
    public void toStringBufferShouldHandleNullElements() {
        Character[] data = chars(null, 'Z');
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(data, null);
        // null element becomes the string "null"
        assertEquals("nullZ", buffer.toString());
    }

    @Test(expected = NullPointerException.class)
    public void toStringBufferShouldThrowNPEWhenArrayIsNull() {
        CharacterArrayConversions.toStringBuffer(null, null);
    }

    // ----------------------------------------------------------------------
    // Additional sanity check – the unused Converter parameter does not affect outcome
    // ----------------------------------------------------------------------
    @Test
    public void conversionMethodsIgnoreConverterArgument() {
        // Create a dummy Converter – the implementation is irrelevant for these methods.
        Converter dummy = new Converter(new ConverterOptions());

        Character[] data = chars('a', 'b', 'c');
        assertEquals("abc", CharacterArrayConversions.toString(data, dummy));
        assertEquals("abc", CharacterArrayConversions.toStringBuilder(data, dummy).toString());
        assertEquals("abc", CharacterArrayConversions.toStringBuffer(data, dummy).toString());
    }
}
