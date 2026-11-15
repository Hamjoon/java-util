package com.cedarsoftware.util.convert;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CharacterArrayConversions}.
 *
 * The class under test contains only static package‑private methods, therefore the test class
 * resides in the same package to gain access.  The {@code Converter} argument is not used by the
 * implementation, but a real instance is supplied to keep the contract realistic.
 */
public class CharacterArrayConversionsTest {

    // A minimal, real Converter instance – the concrete class is final and has a public constructor.
    // The ConverterOptions class is part of the public API and provides a no‑arg constructor.
    private static final Converter DUMMY_CONVERTER = new Converter(new ConverterOptions());

    /**
     * Helper that creates a Character[] from a var‑args list of chars.
     */
    private static Character[] chars(char... values) {
        Character[] result = new Character[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }

    @Test
    public void toString_WithTypicalCharacters_ReturnsConcatenatedString() {
        Character[] source = chars('J', 'u', 'n', 'i', 't');
        String result = CharacterArrayConversions.toString(source, DUMMY_CONVERTER);
        assertEquals("JUnit", result);
    }

    @Test
    public void toString_WithEmptyArray_ReturnsEmptyString() {
        Character[] source = new Character[0];
        String result = CharacterArrayConversions.toString(source, DUMMY_CONVERTER);
        assertEquals("", result);
    }

    @Test
    public void toString_WithNullElements_InsertsLiteralNull() {
        // The implementation uses StringBuilder.append(Object) for a null Character,
        // which results in the literal string "null".
        Character[] source = new Character[]{'a', null, 'c'};
        String result = CharacterArrayConversions.toString(source, DUMMY_CONVERTER);
        assertEquals("anullc", result);
    }

    @Test(expected = NullPointerException.class)
    public void toString_WithNullArray_ThrowsNullPointerException() {
        CharacterArrayConversions.toString(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void toString_WithWrongArrayType_ThrowsClassCastException() {
        // Passing a String[] forces the cast (Character[]) to fail.
        Object wrongType = new String[]{"x", "y"};
        CharacterArrayConversions.toString(wrongType, DUMMY_CONVERTER);
    }

    @Test
    public void toStringBuilder_ReturnsBuilderWithSameContentAsToString() {
        Character[] source = chars('t', 'e', 's', 't');
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(source, DUMMY_CONVERTER);
        assertNotNull(sb);
        assertEquals("test", sb.toString());

        // Mutate the builder to confirm it is a separate, mutable object.
        sb.append("ing");
        assertEquals("testing", sb.toString());
        // Original conversion must remain unchanged.
        assertEquals("test", CharacterArrayConversions.toString(source, DUMMY_CONVERTER));
    }

    @Test
    public void toStringBuilder_WithEmptyArray_ReturnsEmptyBuilder() {
        Character[] source = new Character[0];
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(source, DUMMY_CONVERTER);
        assertNotNull(sb);
        assertEquals(0, sb.length());
        assertEquals("", sb.toString());
    }

    @Test
    public void toStringBuilder_WithNullElement_InsertsLiteralNull() {
        Character[] source = new Character[]{null};
        StringBuilder sb = CharacterArrayConversions.toStringBuilder(source, DUMMY_CONVERTER);
        assertEquals("null", sb.toString());
    }

    @Test
    public void toStringBuffer_ReturnsBufferWithSameContentAsToString() {
        Character[] source = chars('b', 'u', 'f', 'f', 'e', 'r');
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(source, DUMMY_CONVERTER);
        assertNotNull(buffer);
        assertEquals("buffer", buffer.toString());

        // Verify mutability.
        buffer.append("Test");
        assertEquals("bufferTest", buffer.toString());
    }

    @Test
    public void toStringBuffer_WithEmptyArray_ReturnsEmptyBuffer() {
        Character[] source = new Character[0];
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(source, DUMMY_CONVERTER);
        assertNotNull(buffer);
        assertEquals(0, buffer.length());
        assertEquals("", buffer.toString());
    }

    @Test
    public void toStringBuffer_WithNullElement_InsertsLiteralNull() {
        Character[] source = new Character[]{null, 'x'};
        StringBuffer buffer = CharacterArrayConversions.toStringBuffer(source, DUMMY_CONVERTER);
        assertEquals("nullx", buffer.toString());
    }

    @Test(expected = NullPointerException.class)
    public void toStringBuilder_WithNullArray_ThrowsNullPointerException() {
        CharacterArrayConversions.toStringBuilder(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void toStringBuffer_WithNullArray_ThrowsNullPointerException() {
        CharacterArrayConversions.toStringBuffer(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void toStringBuilder_WithWrongArrayType_ThrowsClassCastException() {
        Object wrong = new Integer[]{1, 2};
        CharacterArrayConversions.toStringBuilder(wrong, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void toStringBuffer_WithWrongArrayType_ThrowsClassCastException() {
        Object wrong = new Double[]{1.0, 2.0};
        CharacterArrayConversions.toStringBuffer(wrong, DUMMY_CONVERTER);
    }
}
