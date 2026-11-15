package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link ByteConversions}.
 *
 * The {@code ByteConversions.toCharacter} method is deliberately tiny, but it has a few
 * edge‑cases that are easy to miss:
 *
 * <ul>
 *   <li>Normal positive values map directly to the same Unicode code point.</li>
 *   <li>Zero maps to {@code '\\u0000'}.</li>
 *   <li>Negative values are sign‑extended when widened to {@code int} before being narrowed to {@code char},
 *       resulting in the high‑order 16 bits being filled with {@code 0xFF}.  This behaviour is verified
 *       for {@code -1} and {@code Byte.MIN_VALUE}.</li>
 *   <li>Passing {@code null} for the source object should raise {@link NullPointerException}.</li>
 *   <li>Passing an object of a type other than {@link Byte} should raise {@link ClassCastException}.</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, but a non‑null instance is supplied
 * to keep the contract intact.
 */
public class ByteConversionsTest {

    // Helper to create a minimal, usable Converter instance.
    private static Converter createConverter() {
        // ConverterOptions has a public no‑arg constructor according to the public API.
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testPositiveByteToChar() {
        Converter converter = createConverter();

        // 65 -> 'A'
        Byte b = Byte.valueOf((byte) 65);
        Character result = ByteConversions.toCharacter(b, converter);
        assertEquals(Character.valueOf('A'), result);

        // 127 (Byte.MAX_VALUE) -> '\u007F'
        b = Byte.valueOf(Byte.MAX_VALUE);
        result = ByteConversions.toCharacter(b, converter);
        assertEquals(Character.valueOf((char) 127), result);
    }

    @Test
    public void testZeroByteToChar() {
        Converter converter = createConverter();

        Byte b = Byte.valueOf((byte) 0);
        Character result = ByteConversions.toCharacter(b, converter);
        assertEquals(Character.valueOf('\u0000'), result);
    }

    @Test
    public void testNegativeByteToChar() {
        Converter converter = createConverter();

        // -1 should become 0xFFFF (65535)
        Byte b = Byte.valueOf((byte) -1);
        Character result = ByteConversions.toCharacter(b, converter);
        assertEquals(Character.valueOf('\uFFFF'), result);

        // Byte.MIN_VALUE (-128) -> 0xFF80 (65408)
        b = Byte.valueOf(Byte.MIN_VALUE);
        result = ByteConversions.toCharacter(b, converter);
        assertEquals(Character.valueOf('\uFF80'), result);
    }

    @Test(expected = NullPointerException.class)
    public void testNullSourceThrowsNPE() {
        Converter converter = createConverter();

        // The method attempts to call byteValue() on a null reference.
        ByteConversions.toCharacter(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testWrongSourceTypeThrowsClassCastException() {
        Converter converter = createConverter();

        // Passing an Integer should cause a cast to Byte to fail.
        Integer notAByte = Integer.valueOf(42);
        ByteConversions.toCharacter(notAByte, converter);
    }

    @Test
    public void testConverterArgumentIsIrrelevant() {
        // Provide a null converter – the implementation never touches it.
        Byte b = Byte.valueOf((byte) 50);
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf((char) 50), result);
    }
}
