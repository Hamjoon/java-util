package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ByteConversions}.
 *
 * The class under test contains a single static method:
 * <pre>
 *   static Character toCharacter(Object from, Converter converter)
 * </pre>
 *
 * The method simply casts the supplied {@code from} object to {@code Byte}
 * and then converts the primitive {@code byte} value to a {@code char}.
 * The {@code Converter} argument is not used, but it is part of the
 * public API and therefore must be supplied (it may be {@code null}).
 *
 * The tests cover:
 * <ul>
 *   <li>Typical conversions (e.g., 65 → 'A')</li>
 *   <li>Boundary values (Byte.MIN_VALUE, Byte.MAX_VALUE, zero)</li>
 *   <li>Negative values and the sign‑extension behaviour when casting to {@code char}</li>
 *   <li>Invalid input types (expect {@code ClassCastException})</li>
 *   <li>Null input (expect {@code NullPointerException})</li>
 *   <li>Ensuring the {@code Converter} argument does not affect the result</li>
 * </ul>
 */
public class ByteConversionsTest {

    /**
     * Helper that invokes the method under test with a {@code null} converter.
     */
    private static Character convert(Object from) {
        return ByteConversions.toCharacter(from, null);
    }

    /**
     * Typical conversion: the ASCII value for 'A' is 65.
     */
    @Test
    public void testTypicalConversion() {
        Byte input = 65; // ASCII 'A'
        Character result = convert(input);
        assertEquals(Character.valueOf('A'), result);
    }

    /**
     * Zero value should become the null character '\u0000'.
     */
    @Test
    public void testZeroConversion() {
        Byte input = 0;
        Character result = convert(input);
        assertEquals(Character.valueOf('\u0000'), result);
    }

    /**
     * Conversion of the maximum byte value (127). 127 maps directly to the
     * Unicode code point U+007F (DELETE control character).
     */
    @Test
    public void testMaxByteConversion() {
        Byte input = Byte.MAX_VALUE; // 127
        Character result = convert(input);
        assertEquals(Character.valueOf((char) 127), result);
    }

    /**
     * Conversion of the minimum byte value (-128).  Because the byte is
     * signed, the value is sign‑extended to an {@code int} (-128) before
     * being cast to {@code char}, resulting in the code point U+FF80.
     */
    @Test
    public void testMinByteConversion() {
        Byte input = Byte.MIN_VALUE; // -128
        Character result = convert(input);
        assertEquals(Character.valueOf((char) -128), result); // (char)-128 == 0xFF80
    }

    /**
     * Negative values other than Byte.MIN_VALUE also demonstrate sign‑extension.
     * Example: -1 becomes 0xFFFF (character with code point 65535).
     */
    @Test
    public void testNegativeByteConversion() {
        Byte input = -1;
        Character result = convert(input);
        assertEquals(Character.valueOf((char) -1), result); // (char)-1 == 0xFFFF
    }

    /**
     * Passing a non‑Byte object should cause a {@link ClassCastException}
     * because the method unconditionally casts the argument to {@code Byte}.
     */
    @Test(expected = ClassCastException.class)
    public void testInvalidInputType() {
        Integer notAByte = 42;
        // The cast inside the method will fail.
        ByteConversions.toCharacter(notAByte, null);
    }

    /**
     * Passing {@code null} as the source object triggers a {@link NullPointerException}
     * when the method attempts to cast {@code null} to {@code Byte}.
     */
    @Test(expected = NullPointerException.class)
    public void testNullInput() {
        ByteConversions.toCharacter(null, null);
    }

    /**
     * Verify that providing a non‑null {@code Converter} does not influence the
     * conversion result.  Since the implementation never touches the converter,
     * the output must be identical to the case where {@code null} is supplied.
     *
     * The test constructs a minimal {@code Converter} instance using a
     * {@code null} {@code ConverterOptions} because the options are not used
     * by the method under test.
     */
    @Test
    public void testConversionWithNonNullConverter() {
        // The public API of Converter offers a constructor that takes ConverterOptions.
        // The options are irrelevant for this test, so we pass null.
        Converter dummyConverter = new Converter(null);
        Byte input = 100; // 'd' in ASCII
        Character result = ByteConversions.toCharacter(input, dummyConverter);
        assertEquals(Character.valueOf('d'), result);
    }
}
