package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

public class CharacterArrayConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // Converter constructor requires ConverterOptions; passing null is allowed for these tests
        dummyConverter = new Converter(null);
    }

    /* -------------------------------------------------------------
       1. toString() – basic functionality
       ------------------------------------------------------------- */
    @Test
    public void testToString_Basic() {
        Character[] input = new Character[]{'J', 'u', 'n', 'i', 't'};
        String result = CharacterArrayConversions.toString(input, dummyConverter);
        assertEquals("JUnit", result);
    }

    @Test
    public void testToString_EmptyArray() {
        Character[] input = new Character[0];
        String result = CharacterArrayConversions.toString(input, dummyConverter);
        assertEquals("", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullArray() {
        CharacterArrayConversions.toString(null, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_ArrayWithNullElement() {
        Character[] input = new Character[]{'a', null, 'b'};
        CharacterArrayConversions.toString(input, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongSourceType() {
        Object notCharArray = new String[]{"a", "b"};
        CharacterArrayConversions.toString(notCharArray, dummyConverter);
    }

    @Test
    public void testToString_NullConverterIgnored() {
        Character[] input = new Character[]{'x', 'y'};
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("xy", result);
    }

    /* -------------------------------------------------------------
       2. toStringBuilder() – basic functionality
       ------------------------------------------------------------- */
    @Test
    public void testToStringBuilder_Basic() {
        Character[] input = new Character[]{'A', 'B', 'C'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);
        assertNotNull(sb);
        assertEquals("ABC", sb.toString());
        assertEquals(3, sb.length());
    }

    @Test
    public void testToStringBuilder_EmptyArray() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);
        assertEquals("", sb.toString());
        assertEquals(0, sb.length());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_NullArray() {
        CharacterArrayConversions.toStringBuilder(null, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuilder_ArrayWithNullElement() {
        Character[] input = new Character[]{'a', null};
        CharacterArrayConversions.toStringBuilder(input, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuilder_WrongSourceType() {
        Object notCharArray = new Integer[]{1, 2};
        CharacterArrayConversions.toStringBuilder(notCharArray, dummyConverter);
    }

    @Test
    public void testToStringBuilder_NullConverterIgnored() {
        Character[] input = new Character[]{'z'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertEquals("z", sb.toString());
    }

    /* -------------------------------------------------------------
       3. toStringBuffer() – basic functionality
       ------------------------------------------------------------- */
    @Test
    public void testToStringBuffer_Basic() {
        Character[] input = new Character[]{'1', '2', '3'};
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, dummyConverter);
        assertNotNull(sb);
        assertEquals("123", sb.toString());
        assertEquals(3, sb.length());
    }

    @Test
    public void testToStringBuffer_EmptyArray() {
        Character[] input = new Character[0];
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, dummyConverter);
        assertEquals("", sb.toString());
        assertEquals(0, sb.length());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_NullArray() {
        CharacterArrayConversions.toStringBuffer(null, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringBuffer_ArrayWithNullElement() {
        Character[] input = new Character[]{null, 'b'};
        CharacterArrayConversions.toStringBuffer(input, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToStringBuffer_WrongSourceType() {
        Object notCharArray = new Double[]{1.0, 2.0};
        CharacterArrayConversions.toStringBuffer(notCharArray, dummyConverter);
    }

    @Test
    public void testToStringBuffer_NullConverterIgnored() {
        Character[] input = new Character[]{'p', 'q'};
        StringBuffer sb = CharacterArrayConversions.toStringBuffer(input, null);
        assertEquals("pq", sb.toString());
    }
}
