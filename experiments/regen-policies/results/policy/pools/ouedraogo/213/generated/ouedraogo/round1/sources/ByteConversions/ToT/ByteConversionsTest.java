package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ByteConversions}.
 * <p>
 * The class under test is package‑private and final, therefore the test class
 * resides in the same package to gain access to its static methods.
 * <p>
 * The test cases cover:
 * <ul>
 *   <li>Typical conversion of a positive {@code Byte} value to {@code Character}.</li>
 *   <li>Boundary values – {@code Byte.MAX_VALUE}, {@code Byte.MIN_VALUE}, and zero.</li>
 *   <li>Sign‑extension behaviour for negative byte values.</li>
 *   <li>Null source object (expect {@link NullPointerException}).</li>
 *   <li>Incorrect source type (expect {@link ClassCastException}).</li>
 *   <li>Passing a non‑null {@link Converter} instance (the argument is ignored).</li>
 * </ul>
 */
public class ByteConversionsTest {

    /**
     * Helper that creates a {@link Converter} instance.
     * The {@code Converter} class requires a {@code ConverterOptions} argument.
     * For the purpose of these tests the concrete behaviour of the converter is
     * irrelevant – the {@code toCharacter} method does not use it – therefore a
     * default‑constructed {@code ConverterOptions} (if available) or {@code null}
     * is sufficient.
     */
    private static Converter createConverter() {
        try {
            // Attempt to use a no‑arg constructor for ConverterOptions if it exists.
            Class<?> optsClass = Class.forName("com.cedarsoftware.util.convert.ConverterOptions");
            Object opts = optsClass.getDeclaredConstructor().newInstance();
            return new Converter((ConverterOptions) opts);
        } catch (Exception e) {
            // Fallback – the Converter constructor accepts null in the current library version.
            return new Converter(null);
        }
    }

    /** Typical conversion – positive byte value. */
    @Test
    public void testToCharacter_PositiveValue() {
        Byte source = 65;                 // ASCII 'A'
        Character result = ByteConversions.toCharacter(source, null);
        assertEquals("Positive byte 65 should convert to 'A'", Character.valueOf('A'), result);
    }

    /** Zero conversion. */
    @Test
    public void testToCharacter_Zero() {
        Byte source = 0;
        Character result = ByteConversions.toCharacter(source, null);
        assertEquals("Zero byte should convert to '\\u0000'", Character.valueOf('\u0000'), result);
    }

    /** Conversion of the maximum byte value (127). */
    @Test
    public void testToCharacter_MaxByte() {
        Byte source = Byte.MAX_VALUE;     // 127
        Character result = ByteConversions.toCharacter(source, null);
        assertEquals("Byte.MAX_VALUE should convert to character 127",
                     Character.valueOf((char) 127), result);
    }

    /** Conversion of the minimum byte value (-128) – checks sign‑extension handling. */
    @Test
    public void testToCharacter_MinByte() {
        Byte source = Byte.MIN_VALUE;     // -128
        Character result = ByteConversions.toCharacter(source, null);
        // (char) (-128) == 0xFF80 (since -128 as int is 0xFFFFFF80, cast to char yields 0xFF80)
        char expected = (char) -128;
        assertEquals("Byte.MIN_VALUE should convert to char 0xFF80", Character.valueOf(expected), result);
    }

    /** Conversion of a negative byte that is not the minimum – e.g., -1. */
    @Test
    public void testToCharacter_NegativeOne() {
        Byte source = -1;
        Character result = ByteConversions.toCharacter(source, null);
        // (char) -1 == 0xFFFF
        assertEquals("Byte -1 should convert to char 0xFFFF",
                     Character.valueOf('\uFFFF'), result);
    }

    /** Passing a non‑null {@link Converter} instance – the argument is ignored. */
    @Test
    public void testToCharacter_WithConverterInstance() {
        Byte source = 97;                 // 'a'
        Converter converter = createConverter();
        Character result = ByteConversions.toCharacter(source, converter);
        assertEquals("Converter argument should have no effect", Character.valueOf('a'), result);
    }

    /** Null source object – should throw {@link NullPointerException}. */
    @Test(expected = NullPointerException.class)
    public void testToCharacter_NullSource() {
        ByteConversions.toCharacter(null, null);
    }

    /** Source object of incompatible type – should throw {@link ClassCastException}. */
    @Test(expected = ClassCastException.class)
    public void testToCharacter_IncompatibleSourceType() {
        Integer wrongType = 42;
        ByteConversions.toCharacter(wrongType, null);
    }
}
