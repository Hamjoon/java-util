package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@code CharacterArrayConversions}.
 *
 * The test class is placed in the same package as {@code CharacterArrayConversions}
 * so it can access the package‑private static methods.
 */
public class CharacterArrayConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter instance is not used by the methods under test,
        // but we provide a real object to satisfy the method signature.
        dummyConverter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------
     *  Tests for toString(Object, Converter)
     * ------------------------------------------------- */

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

    @Test
    public void testToString_NullArray() {
        try {
            CharacterArrayConversions.toString(null, dummyConverter);
            fail("Expected NullPointerException when source array is null");
        } catch (NullPointerException expected) {
            // expected – chars.length is accessed on a null reference
        }
    }

    @Test
    public void testToString_ArrayWithNullElements() {
        Character[] input = new Character[]{'a', null, 'c'};
        String result = CharacterArrayConversions.toString(input, dummyConverter);
        // StringBuilder.append(null) appends the literal "null"
        assertEquals("anullc", result);
    }

    @Test
    public void testToString_WrongSourceType() {
        Object notACharArray = new int[]{1, 2, 3};
        try {
            CharacterArrayConversions.toString(notACharArray, dummyConverter);
            fail("Expected ClassCastException when source is not a Character[]");
        } catch (ClassCastException expected) {
            // expected
        }
    }

    @Test
    public void testToString_NullConverter() {
        Character[] input = new Character[]{'x', 'y'};
        // The method does not use the Converter, so passing null is legal.
        String result = CharacterArrayConversions.toString(input, null);
        assertEquals("xy", result);
    }

    /* -------------------------------------------------
     *  Tests for toStringBuilder(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToStringBuilder_Basic() {
        Character[] input = new Character[]{'J', 'a', 'v', 'a'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);
        assertEquals("Java", sb.toString());
    }

    @Test
    public void testToStringBuilder_EmptyArray() {
        Character[] input = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void testToStringBuilder_NullArray() {
        try {
            CharacterArrayConversions.toStringBuilder(null, dummyConverter);
            fail("Expected NullPointerException for null source array");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testToStringBuilder_ArrayWithNullElements() {
        Character[] input = new Character[]{null, 'b'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, dummyConverter);
        assertEquals("nullb", sb.toString());
    }

    @Test
    public void testToStringBuilder_WrongSourceType() {
        Object notACharArray = "not an array";
        try {
            CharacterArrayConversions.toStringBuilder(notACharArray, dummyConverter);
            fail("Expected ClassCastException for wrong source type");
        } catch (ClassCastException expected) {
            // expected
        }
    }

    @Test
    public void testToStringBuilder_NullConverter() {
        Character[] input = new Character[]{'q'};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(input, null);
        assertEquals("q", sb.toString());
    }

    /* -------------------------------------------------
     *  Tests for toStringBuffer(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToStringBuffer_Basic() {
        Character[] input = new Character[]{'T', 'e', 's', 't'};
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, dummyConverter);
        assertEquals("Test", buf.toString());
    }

    @Test
    public void testToStringBuffer_EmptyArray() {
        Character[] input = new Character[0];
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, dummyConverter);
        assertEquals(0, buf.length());
        assertEquals("", buf.toString());
    }

    @Test
    public void testToStringBuffer_NullArray() {
        try {
            CharacterArrayConversions.toStringBuffer(null, dummyConverter);
            fail("Expected NullPointerException for null source array");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testToStringBuffer_ArrayWithNullElements() {
        Character[] input = new Character[]{'x', null};
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, dummyConverter);
        assertEquals("xnull", buf.toString());
    }

    @Test
    public void testToStringBuffer_WrongSourceType() {
        Object notACharArray = new Object();
        try {
            CharacterArrayConversions.toStringBuffer(notACharArray, dummyConverter);
            fail("Expected ClassCastException for wrong source type");
        } catch (ClassCastException expected) {
            // expected
        }
    }

    @Test
    public void testToStringBuffer_NullConverter() {
        Character[] input = new Character[]{'z'};
        StringBuffer buf = CharacterArrayConversions.toStringBuffer(input, null);
        assertEquals("z", buf.toString());
    }
}
