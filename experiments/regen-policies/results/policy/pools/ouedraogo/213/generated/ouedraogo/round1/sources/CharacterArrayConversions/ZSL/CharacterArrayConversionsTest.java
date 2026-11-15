package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link CharacterArrayConversions}.
 *
 * The class under test contains only static package‑private methods, therefore
 * the test class lives in the same package so it can access them directly.
 *
 * The {@code Converter} argument is not used by the implementation, so a
 * {@code null} reference is sufficient for all tests.
 */
public class CharacterArrayConversionsTest {

    /* --------------------------------------------------------------------- */
    /*  Tests for toString(Object, Converter)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void toString_normalArray_returnsCorrectString() {
        Character[] input = new Character[] { 'J', 'u', 'n', 'i', 't' };
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("JUnit", result);
    }

    @Test
    public void toString_emptyArray_returnsEmptyString() {
        Character[] input = new Character[0];
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("", result);
    }

    @Test(expected = NullPointerException.class)
    public void toString_nullArray_throwsNullPointerException() {
        CharacterArrayConversions.toString(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void toString_wrongObjectType_throwsClassCastException() {
        // Passing a String[] instead of Character[] forces the cast to fail.
        Object wrong = new String[] { "a", "b" };
        CharacterArrayConversions.toString(wrong, null);
    }

    @Test(expected = NullPointerException.class)
    public void toString_arrayWithNullElement_throwsNullPointerException() {
        Character[] input = new Character[] { 'A', null, 'Z' };
        CharacterArrayConversions.toString(input, null);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for toStringBuilder(Object, Converter)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void toStringBuilder_normalArray_returnsCorrectBuilder() {
        Character[] input = new Character[] { 'T', 'e', 's', 't' };
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertNotNull(sb);
        assertEquals(4, sb.length());
        assertEquals("Test", sb.toString());
    }

    @Test
    public void toStringBuilder_emptyArray_returnsEmptyBuilder() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertNotNull(sb);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void toStringBuilder_nullArray_throwsNullPointerException() {
        CharacterArrayConversions.toStringBuilder(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void toStringBuilder_wrongObjectType_throwsClassCastException() {
        Object wrong = new Integer[] { 1, 2 };
        CharacterArrayConversions.toStringBuilder(wrong, null);
    }

    @Test(expected = NullPointerException.class)
    public void toStringBuilder_arrayWithNullElement_throwsNullPointerException() {
        Character[] input = new Character[] { 'X', null };
        CharacterArrayConversions.toStringBuilder(input, null);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for toStringBuffer(Object, Converter)                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void toStringBuffer_normalArray_returnsCorrectBuffer() {
        Character[] input = new Character[] { 'B', 'u', 'f', 'f', 'e', 'r' };
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, null);
        assertNotNull(sb);
        assertEquals(6, sb.length());
        assertEquals("Buffer", sb.toString());
    }

    @Test
    public void toStringBuffer_emptyArray_returnsEmptyBuffer() {
        Character[] input = new Character[0];
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, null);
        assertNotNull(sb);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void toStringBuffer_nullArray_throwsNullPointerException() {
        CharacterArrayConversions.toStringBuffer(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void toStringBuffer_wrongObjectType_throwsClassCastException() {
        Object wrong = new Double[] { 1.0 };
        CharacterArrayConversions.toStringBuffer(wrong, null);
    }

    @Test(expected = NullPointerException.class)
    public void toStringBuffer_arrayWithNullElement_throwsNullPointerException() {
        Character[] input = new Character[] { null };
        CharacterArrayConversions.toStringBuffer(input, null);
    }

    /* --------------------------------------------------------------------- */
    /*  Additional sanity check: large array processing                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void toString_largeArray_performsEfficiently() {
        int size = 10_000;
        Character[] input = new Character[size];
        StringBuilder expected = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            char ch = (char) ('a' + (i % 26));
            input[i] = ch;
            expected.append(ch);
        }

        // Verify all three conversion methods produce the same result.
        assertEquals(expected.toString(),
                CharacterArrayConversions.toString(input, null));

        assertEquals(expected.toString(),
                CharacterArrayConversions.toStringBuilder(input, null).toString());

        assertEquals(expected.toString(),
                CharacterArrayConversions.toStringBuffer(input, null).toString());
    }
}
