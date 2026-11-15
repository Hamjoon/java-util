package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link ByteConversions}.
 *
 * The {@code toCharacter} method converts a {@link Byte} to a {@link Character}
 * by casting the byte's numeric value to a char. The method does not use the
 * {@link Converter} argument, but the API requires it, so both {@code null}
 * and a real {@code Converter} instance are exercised.
 */
public class ByteConversionsTest {

    /**
     * Helper that creates a {@link Converter} instance. The constructor requires a
     * {@code ConverterOptions} object, but the implementation of {@code toCharacter}
     * never uses the converter, so we can safely pass {@code null}.
     *
     * @return a Converter instance (may be {@code null} if the caller prefers)
     */
    private Converter createConverter() {
        // Passing null is acceptable because the method under test does not interact with the Converter.
        return new Converter(null);
    }

    /** Positive byte value should map to the same Unicode code point. */
    @Test
    public void testPositiveByteConversion() {
        Byte input = 65; // ASCII 'A'
        Character result = ByteConversions.toCharacter(input, createConverter());
        assertEquals('A', (char) result);
    }

    /** Zero byte value should become the null character. */
    @Test
    public void testZeroByteConversion() {
        Byte input = 0;
        Character result = ByteConversions.toCharacter(input, null); // also test with null converter
        assertEquals('\0', (char) result);
    }

    /** Maximum byte value (127) should be preserved. */
    @Test
    public void testMaxByteConversion() {
        Byte input = Byte.MAX_VALUE; // 127
        Character result = ByteConversions.toCharacter(input, createConverter());
        assertEquals((char) 127, (char) result);
    }

    /** Minimum byte value (-128) should be sign‑extended then truncated to char (0xFF80). */
    @Test
    public void testMinByteConversion() {
        Byte input = Byte.MIN_VALUE; // -128
        Character result = ByteConversions.toCharacter(input, createConverter());
        assertEquals((char) 0xFF80, (char) result);
    }

    /** Negative byte values are sign‑extended; -1 becomes char 0xFFFF (65535). */
    @Test
    public void testNegativeByteConversion() {
        Byte input = -1;
        Character result = ByteConversions.toCharacter(input, createConverter());
        assertEquals((char) 0xFFFF, (char) result);
    }

    /** Passing {@code null} as the source object should throw NullPointerException. */
    @Test(expected = NullPointerException.class)
    public void testNullFromThrowsNullPointerException() {
        ByteConversions.toCharacter(null, createConverter());
    }

    /** Passing an object that is not a Byte should result in ClassCastException. */
    @Test(expected = ClassCastException.class)
    public void testInvalidSourceTypeThrowsClassCastException() {
        Integer notAByte = Integer.valueOf(42);
        ByteConversions.toCharacter(notAByte, createConverter());
    }

    /** Ensure that a non‑null Converter that is never used does not affect the outcome. */
    @Test
    public void testNonNullConverterIgnored() {
        Byte input = 100;
        Converter converter = createConverter(); // non‑null
        Character result = ByteConversions.toCharacter(input, converter);
        assertEquals((char) 100, (char) result);
    }
}
