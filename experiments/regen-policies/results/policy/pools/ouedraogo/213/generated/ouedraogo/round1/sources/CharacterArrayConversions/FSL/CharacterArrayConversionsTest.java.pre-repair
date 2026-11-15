package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests for {@link CharacterArrayConversions}.
 */
public class CharacterArrayConversionsTest {

    // Helper to create a Converter instance – the methods under test do not use it.
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    @Test
    public void testToString_NormalArray() {
        Character[] input = new Character[] {'a', 'b', 'c', '1', '2', '3'};
        String result = CharacterArrayConversions.toString(input, DUMMY_CONVERTER);
        assertEquals("abc123", result);
    }

    @Test
    public void testToString_EmptyArray() {
        Character[] input = new Character[0];
        String result = CharacterArrayConversions.toString(input, DUMMY_CONVERTER);
        assertEquals("", result);
    }

    @Test
    public void testToString_ArrayWithNullElement() {
        Character[] input = new Character[] {'x', null, 'y'};
        try {
            CharacterArrayConversions.toString(input, DUMMY_CONVERTER);
            fail("Expected NullPointerException when array contains null Character");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testToString_WrongTypeThrowsClassCastException() {
        Object notCharArray = new String[] {"a", "b"};
        try {
            CharacterArrayConversions.toString(notCharArray, DUMMY_CONVERTER);
            fail("Expected ClassCastException for non‑Character[] argument");
        } catch (ClassCastException e) {
            // expected
        }
    }

    @Test
    public void testToStringBuilder_NormalArray() {
        Character[] input = new Character[] {'J', 'u', 'n', 'i', 't'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, DUMMY_CONVERTER);
        assertNotNull(sb);
        assertEquals("JUnit", sb.toString());
        // Verify that the returned builder is mutable and reflects further appends
        sb.append("-test");
        assertEquals("JUnit-test", sb.toString());
    }

    @Test
    public void testToStringBuilder_EmptyArray() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, DUMMY_CONVERTER);
        assertNotNull(sb);
        assertEquals(0, sb.length());
    }

    @Test
    public void testToStringBuilder_ArrayWithNullElement() {
        Character[] input = new Character[] {null};
        try {
            CharacterArrayConversions.toStringBuilder(input, DUMMY_CONVERTER);
            fail("Expected NullPointerException when array contains null Character");
        } catch (NullPointerException e) {
            // expected
        }
    }

    @Test
    public void testToStringBuffer_NormalArray() {
        Character[] input = new Character[] {'B', 'u', 'f', 'f', 'e', 'r'};
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, DUMMY_CONVERTER);
        assertNotNull(buf);
        assertEquals("Buffer", buf.toString());
        // Verify mutability
        buf.append("-ok");
        assertEquals("Buffer-ok", buf.toString());
    }

    @Test
    public void testToStringBuffer_EmptyArray() {
        Character[] input = new Character[0];
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, DUMMY_CONVERTER);
        assertNotNull(buf);
        assertEquals(0, buf.length());
    }

    @Test
    public void testToStringBuffer_ArrayWithNullElement() {
        Character[] input = new Character[] {'A', null};
        try {
            CharacterArrayConversions.toStringBuffer(input, DUMMY_CONVERTER);
            fail("Expected NullPointerException when array contains null Character");
        } catch (NullPointerException e) {
            // expected
        }
    }
}
