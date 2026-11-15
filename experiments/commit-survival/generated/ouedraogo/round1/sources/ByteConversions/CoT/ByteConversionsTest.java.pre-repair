package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ByteConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion of {@link Byte} values to {@link Character}.</li>
 *   <li>Edge‑case byte values (0, positive, negative, MIN_VALUE).</li>
 *   <li>Behaviour when the {@code from} argument is {@code null}.</li>
 *   <li>Behaviour when the {@code from} argument is of an incompatible type.</li>
 *   <li>Verification that the {@code Converter} argument is not used (can be {@code null}).</li>
 * </ul>
 */
@RunWith(JUnit4.class)
public class ByteConversionsTest {

    /** Helper method to obtain a {@link Converter} instance.
     *  The {@code Converter} class is part of the library under test; its
     *  implementation details are irrelevant for {@code ByteConversions},
     *  therefore we can safely pass {@code null} for its {@code ConverterOptions}
     *  argument (the constructor accepts a {@code ConverterOptions} instance,
     *  which may be {@code null}). */
    private static Converter createConverter() {
        return new Converter(null);
    }

    // -----------------------------------------------------------------------
    // Basic functional tests
    // -----------------------------------------------------------------------

    @Test
    public void toCharacter_positiveByte_returnsCorrespondingChar() {
        Byte input = Byte.valueOf((byte) 65); // ASCII 'A'
        Character result = ByteConversions.toCharacter(input, createConverter());
        assertEquals(Character.valueOf('A'), result);
    }

    @Test
    public void toCharacter_zeroByte_returnsNullChar() {
        Byte input = Byte.valueOf((byte) 0);
        Character result = ByteConversions.toCharacter(input, createConverter());
        assertEquals(Character.valueOf('\u0000'), result);
    }

    @Test
    public void toCharacter_negativeByte_returnsUnsignedCharValue() {
        // -1 as a signed byte is 0xFF, which becomes 0x00FF as a char (255)
        Byte input = Byte.valueOf((byte) -1);
        Character result = ByteConversions.toCharacter(input, createConverter());
        assertEquals(Character.valueOf('\u00FF'), result);
    }

    @Test
    public void toCharacter_minByte_returnsCorrectChar() {
        // Byte.MIN_VALUE = -128 -> 0x80, char value 0xFF80
        Byte input = Byte.valueOf(Byte.MIN_VALUE);
        Character result = ByteConversions.toCharacter(input, createConverter());
        assertEquals(Character.valueOf('\uFF80'), result);
    }

    @Test
    public void toCharacter_converterCanBeNull() {
        Byte input = Byte.valueOf((byte) 10);
        // The method does not use the converter, so passing null must not affect the result.
        Character result = ByteConversions.toCharacter(input, null);
        assertEquals(Character.valueOf('\n'), result);
    }

    // -----------------------------------------------------------------------
    // Edge‑case and exception tests
    // -----------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void toCharacter_nullFrom_throwsNullPointerException() {
        // The method casts the argument to Byte and then dereferences it,
        // which results in a NullPointerException for a null argument.
        ByteConversions.toCharacter(null, createConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toCharacter_incompatibleType_throwsClassCastException() {
        // Passing an object that is not a Byte should cause a ClassCastException
        // when the method attempts to cast it to Byte.
        Integer notAByte = Integer.valueOf(42);
        ByteConversions.toCharacter(notAByte, createConverter());
    }
}
