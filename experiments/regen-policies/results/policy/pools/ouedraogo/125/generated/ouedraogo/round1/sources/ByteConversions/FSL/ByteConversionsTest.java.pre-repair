package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link ByteConversions}.
 * <p>
 * The class only contains a single static method {@code toCharacter(Object, Converter)}.
 * The method casts the {@code from} argument to {@link Byte} and returns the character
 * whose Unicode code point is the signed byte value (i.e. the byte is widened to an int
 * and then narrowed to {@code char}).  The {@code Converter} argument is not used,
 * therefore the tests pass {@code null} for it.
 */
public class ByteConversionsTest {

    /** Helper to invoke the method under test without a real Converter. */
    private static char toChar(Object from) {
        return ByteConversions.toCharacter(from, null);
    }

    @Test
    public void testPositiveByteValues() {
        assertEquals('A', toChar(Byte.valueOf((byte) 65));          // 'A' == 65
        assertEquals('0', toChar(Byte.valueOf((byte) 48));          // '0' == 48
        assertEquals('\u007F', toChar(Byte.valueOf((byte) 127));   // max positive byte
    }

    @Test
    public void testZeroByte() {
        assertEquals('\u0000', toChar(Byte.valueOf((byte) 0)));
    }

    @Test
    public void testNegativeByteValues() {
        // -1 -> 0xFFFF (65535)
        assertEquals('\uFFFF', toChar(Byte.valueOf((byte) -1)));
        // -128 -> 0xFF80 (65408)
        assertEquals('\uFF80', toChar(Byte.valueOf((byte) -128)));
        // -64 -> 0xFFC0 (65472)
        assertEquals('\uFFC0', toChar(Byte.valueOf((byte) -64)));
    }

    @Test
    public void testByteBoundaryValues() {
        // Byte.MAX_VALUE = 127 -> '\u007F'
        assertEquals('\u007F', toChar(Byte.valueOf(Byte.MAX_VALUE)));
        // Byte.MIN_VALUE = -128 -> '\uFF80'
        assertEquals('\uFF80', toChar(Byte.valueOf(Byte.MIN_VALUE)));
    }

    @Test(expected = NullPointerException.class)
    public void testNullFromThrowsNPE() {
        // Casting null to Byte yields null; calling byteValue() triggers NPE.
        toChar(null);
    }

    @Test(expected = ClassCastException.class)
    public void testInvalidFromTypeThrowsClassCastException() {
        // Passing an object that is not a Byte should cause a ClassCastException.
        toChar(Integer.valueOf(10));
    }

    @Test
    public void testConverterParameterIsIgnored() {
        // Even when a non‑null Converter is supplied, the result must be identical.
        Converter dummy = new Converter(null); // options are irrelevant for this test
        Character result = ByteConversions.toCharacter(Byte.valueOf((byte) 42), dummy);
        assertEquals((char) 42, (char) result.charValue());
    }

    @Test
    public void testMultipleCallsAreStateless() {
        // Ensure repeated invocations do not interfere with each other.
        assertEquals('x', toChar(Byte.valueOf((byte) 120)));
        assertEquals('y', toChar(Byte.valueOf((byte) 121)));
        assertEquals('z', toChar(Byte.valueOf((byte) 122)));
    }
}
