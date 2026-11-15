package com.cedarsoftware.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ByteUtilities_toHexChar_Test {

    /**
     * Hexadecimal characters used by {@link #encode(byte[])} and {@link #toHexChar(int)}.
     */
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    /**
     * Convert the specified value (0 .. 15) to the corresponding hex digit.
     *
     * @param value the integer value to convert; only the low 4 bits are used.
     * @return the hexadecimal character ('0' … 'F').
     */
    public static char toHexChar(int value) {
        // Mask to the low‑order nibble and look up the character.
        return HEX_ARRAY[value & 0xF];
    }

    /**
     * Converts a byte array into a string of hex digits.
     *
     * @param bytes the byte array to encode; may be {@code null}.
     * @return a string containing two hex characters per byte, or {@code null}
     *         if the input array is {@code null}.
     */
    public static String encode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        // Each byte becomes two hex characters.
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // High nibble
            sb.append(toHexChar((b >>> 4) & 0xF));
            // Low nibble
            sb.append(toHexChar(b & 0xF));
        }
        return sb.toString();
    }

    /**
     * Decodes a hexadecimal string into a byte array.
     *
     * @param hex a string containing an even number of hex characters; may be {@code null}.
     * @return the decoded byte array, or {@code null} if the input is {@code null}
     *         or has an odd length or contains non‑hex characters.
     */
    public static byte[] decode(String hex) {
        if (hex == null) {
            return null;
        }
        int len = hex.length();
        if (// odd length – cannot be valid hex
        (len & 1) != 0) {
            return null;
        }
        byte[] result = new byte[len / 2];
        for (int i = 0; i < result.length; i++) {
            int hi = convertDigit(hex.charAt(i * 2));
            int lo = convertDigit(hex.charAt(i * 2 + 1));
            if (hi == -1 || lo == -1) {
                // invalid character
                return null;
            }
            result[i] = (byte) ((hi << 4) | lo);
        }
        return result;
    }

    /**
     * Checks whether the supplied byte array appears to be GZIP‑compressed.
     *
     * @param data the byte array to test; may be {@code null}.
     * @return {@code true} if the array starts with the GZIP magic header (0x1F, 0x8B).
     */
    public static boolean isGzipped(byte[] data) {
        return data != null && data.length >= 2 && (data[0] == (byte) 0x1F) && (data[1] == (byte) 0x8B);
    }

    /**
     * Helper that converts a single hexadecimal digit character to its integer value.
     *
     * @param c a character in the ranges {@code '0'‑'9'}, {@code 'A'‑'F'} or {@code 'a'‑'f'}.
     * @return the numeric value 0‑15, or {@code -1} if the character is not a valid hex digit.
     */
    private static int convertDigit(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        return -1;
    }

    private static Stream<Arguments> validRangeProvider() {
        return Stream.of(Arguments.of(0, '0', "Lowest valid value, should map to character '0'"), Arguments.of(1, '1', "Simple positive value within 0‑15 range, should map to '1'"), Arguments.of(9, '9', "Digit 9, verifies mapping for numeric characters"), Arguments.of(10, 'A', "First hexadecimal letter, value 10 should map to 'A'"), Arguments.of(15, 'F', "Upper bound of the 4‑bit range, should map to 'F'"));
    }

    private static Stream<Arguments> maskingHigherBitsProvider() {
        return Stream.of(Arguments.of(16, '0', "Value 16 (0x10) masked to lower 4 bits yields 0, should return '0'."), Arguments.of(31, 'F', "Value 31 (0x1F) masked yields 0xF, should return 'F'."), Arguments.of(255, 'F', "Value 255 (0xFF) masked yields 0xF, should return 'F'."), Arguments.of(256, '0', "Value 256 (0x100) masked yields 0, should return '0'."));
    }

    private static Stream<Arguments> negativeValuesProvider() {
        return Stream.of(Arguments.of(-1, 'F', "Negative one (-1) has all bits set; masked to 0xF yields 15, should return 'F'."), Arguments.of(-2, 'E', "Negative two (-2) masked yields 0xE, should return 'E'."), Arguments.of(-16, '0', "-16 (0x...FFF0) masked yields 0, should return '0'."), Arguments.of(-17, 'F', "-17 masked yields 0xF, should return 'F'."));
    }

    private static Stream<Arguments> extremeBoundariesProvider() {
        return Stream.of(Arguments.of(Integer.MAX_VALUE, 'F', "Integer.MAX_VALUE masked yields 0xF, should return 'F'."), Arguments.of(Integer.MIN_VALUE, '0', "Integer.MIN_VALUE masked yields 0x0, should return '0'."));
    }

    @Test
    void testToHexCharAllNibbles() {
        char[] expected = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < expected.length; i++) {
            // capture for lambda readability
            final int index = i;
            char actual = ByteUtilities.toHexChar(i);
            assertEquals(expected[i], actual, () -> "Incorrect hex char for value " + index);
        }
    }

    @Test
    void testToHexCharMaskingHigherBits() {
        // 0x1A -> low nibble 0xA -> 'A'
        assertEquals('A', ByteUtilities.toHexChar(0x1A));
        // 0xFF -> low nibble 0xF -> 'F'
        assertEquals('F', ByteUtilities.toHexChar(0xFF));
        // 0x100 -> low nibble 0x0 -> '0'
        assertEquals('0', ByteUtilities.toHexChar(0x100));
        // -1 (0xFFFFFFFF) -> low nibble 0xF -> 'F'
        assertEquals('F', ByteUtilities.toHexChar(-1));
        // -16 (0xFFFFFFF0) -> low nibble 0x0 -> '0'
        assertEquals('0', ByteUtilities.toHexChar(-16));
    }

    @Test
    void testToHexCharBoundaryValues() {
        assertEquals('0', ByteUtilities.toHexChar(0));
        assertEquals('F', ByteUtilities.toHexChar(15));
        // 16 should wrap to 0
        assertEquals('0', ByteUtilities.toHexChar(16));
        // 31 (0x1F) -> low nibble 0xF -> 'F'
        assertEquals('F', ByteUtilities.toHexChar(31));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validRangeProvider")
    void testValidRangeConversions(int value, char expected, String description) {
        assertEquals(expected, ByteUtilities.toHexChar(value), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("maskingHigherBitsProvider")
    void testMaskingHigherBits(int value, char expected, String description) {
        assertEquals(expected, ByteUtilities.toHexChar(value), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("negativeValuesProvider")
    void testNegativeValues(int value, char expected, String description) {
        assertEquals(expected, ByteUtilities.toHexChar(value), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("extremeBoundariesProvider")
    void testExtremeIntegerBoundaries(int value, char expected, String description) {
        assertEquals(expected, ByteUtilities.toHexChar(value), description);
    }
}
