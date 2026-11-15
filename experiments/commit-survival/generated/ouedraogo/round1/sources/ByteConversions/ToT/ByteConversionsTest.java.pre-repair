package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ByteConversions}.
 *
 * The class under test contains a single static method:
 *
 * <pre>
 * static Character toCharacter(Object from, Converter converter)
 * </pre>
 *
 * The method:
 * <ul>
 *   <li>expects {@code from} to be a {@link Byte}</li>
 *   <li>ignores the {@code converter} argument</li>
 *   <li>converts the byte value to a {@code char} by casting the primitive {@code byte}
 *       to {@code char}</li>
 * </ul>
 *
 * The test suite covers:
 * <ol>
 *   <li>Typical conversions (positive values, ASCII characters)</li>
 *   <li>Boundary values (min/max byte, negative values)</li>
 *   <li>Invalid inputs (wrong type, {@code null} values)</li>
 *   <li>Behaviour when {@code converter} is {@code null}</li>
 * </ol>
 *
 * No mocking frameworks are used – a real {@link Converter} instance is created with a
 * {@code null} {@link ConverterOptions} because the method never touches the converter.
 */
public class ByteConversionsTest {

    /** Helper to obtain a dummy Converter – the implementation does not matter for these tests. */
    private static Converter dummyConverter() {
        // The Converter constructor requires a ConverterOptions; passing null is acceptable
        // because the method under test never uses the Converter instance.
        return new Converter(null);
    }

    /** 1. Typical conversion: a positive byte that maps to an ASCII character. */
    @Test
    public void testTypicalPositiveByte() {
        Byte b = (byte) 65; // ASCII 'A'
        Character result = ByteConversions.toCharacter(b, dummyConverter());
        assertEquals("Conversion of 65 should yield 'A'", Character.valueOf('A'), result);
    }

    /** 2. Edge case: the maximum byte value (127). */
    @Test
    public void testMaxByteValue() {
        Byte b = Byte.MAX_VALUE; // 127
        Character result = ByteConversions.toCharacter(b, dummyConverter());
        assertEquals("Max byte 127 should map to char 127", Character.valueOf((char) 127), result);
    }

    /** 3. Edge case: the minimum byte value (-128). */
    @Test
    public void testMinByteValue() {
        Byte b = Byte.MIN_VALUE; // -128
        // Casting -128 (0x80) to char yields 0xFF80 (65536 - 128 = 65408)
        char expected = (char) b.byteValue();
        Character result = ByteConversions.toCharacter(b, dummyConverter());
        assertEquals("Min byte -128 should map to char 0xFF80", Character.valueOf(expected), result);
    }

    /** 4. Edge case: a negative byte that is not the minimum (e.g., -1). */
    @Test
    public void testNegativeByte() {
        Byte b = (byte) -1;
        // -1 as a byte becomes 0xFF, which as a char is 0xFFFF (65535)
        char expected = (char) b.byteValue();
        Character result = ByteConversions.toCharacter(b, dummyConverter());
        assertEquals("Byte -1 should map to char 0xFFFF", Character.valueOf(expected), result);
    }

    /** 5. Null {@code from} argument – should throw NullPointerException when dereferenced. */
    @Test(expected = NullPointerException.class)
    public void testNullFromArgument() {
        ByteConversions.toCharacter(null, dummyConverter());
    }

    /** 6. Wrong type for {@code from} – should throw ClassCastException. */
    @Test(expected = ClassCastException.class)
    public void testInvalidFromType() {
        Integer notAByte = Integer.valueOf(42);
        ByteConversions.toCharacter(notAByte, dummyConverter());
    }

    /** 7. {@code converter} argument is {@code null} – method should still work. */
    @Test
    public void testNullConverterArgument() {
        Byte b = (byte) 50;
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals("Conversion should succeed even when converter is null", Character.valueOf((char)50), result);
    }

    /** 8. Verify that the method truly ignores the Converter instance (different instances give same result). */
    @Test
    public void testConverterIrrelevance() {
        Byte b = (byte) 100;
        Converter c1 = new Converter(null);
        Converter c2 = new Converter(null);
        Character r1 = ByteConversions.toCharacter(b, c1);
        Character r2 = ByteConversions.toCharacter(b, c2);
        assertEquals("Result must be identical regardless of Converter instance", r1, r2);
    }
}
