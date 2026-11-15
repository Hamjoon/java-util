package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ByteConversions}.
 *
 * The tests cover:
 *   • Normal conversions for typical, zero, minimum and maximum byte values.
 *   • Behaviour when the {@code from} argument is {@code null}.
 *   • Behaviour when the {@code from} argument is not a {@link Byte}.
 *   • The fact that the {@code Converter} argument is ignored (including {@code null}).
 */
public class ByteConversionsTest {

    // ----------------------------------------------------------------------
    // Helper – a dummy Converter instance (the method does not use it)
    // ----------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = new Converter(new ConverterOptions());

    // ----------------------------------------------------------------------
    // Basic functional tests
    // ----------------------------------------------------------------------
    @Test
    public void testPositiveByte() {
        Byte b = 65;               // ASCII 'A'
        Character result = ByteConversions.toCharacter(b, DUMMY_CONVERTER);
        assertEquals(Character.valueOf('A'), result);
    }

    @Test
    public void testZeroByte() {
        Byte b = 0;
        Character result = ByteConversions.toCharacter(b, DUMMY_CONVERTER);
        assertEquals(Character.valueOf('\u0000'), result);
    }

    @Test
    public void testNegativeByte() {
        Byte b = -1;               // 0xFF → char value 65535
        Character result = ByteConversions.toCharacter(b, DUMMY_CONVERTER);
        assertEquals(Character.valueOf('\uFFFF'), result);
    }

    @Test
    public void testByteMinValue() {
        Byte b = Byte.MIN_VALUE;   // -128 → char value 65408 (0xFF80)
        Character result = ByteConversions.toCharacter(b, DUMMY_CONVERTER);
        assertEquals(Character.valueOf('\uFF80'), result);
    }

    @Test
    public void testByteMaxValue() {
        Byte b = Byte.MAX_VALUE;   // 127 → char value 127 (0x007F)
        Character result = ByteConversions.toCharacter(b, DUMMY_CONVERTER);
        assertEquals(Character.valueOf('\u007F'), result);
    }

    // ----------------------------------------------------------------------
    // Edge‑case / exception tests
    // ----------------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testFromIsNull() {
        // Casting null to Byte succeeds (produces null) but the subsequent
        // call to byteValue() throws NullPointerException.
        ByteConversions.toCharacter(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testFromIsNotByte() {
        // Passing an Integer should fail at the cast "(Byte) from".
        Integer notAByte = 42;
        ByteConversions.toCharacter(notAByte, DUMMY_CONVERTER);
    }

    @Test
    public void testConverterIsNull() {
        // The method never dereferences the Converter argument,
        // so a null value must be accepted without error.
        Byte b = 10;
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf('\n'), result);
    }
}
