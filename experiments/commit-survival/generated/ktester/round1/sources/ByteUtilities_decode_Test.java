package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

class ByteUtilities_decode_Test {

    private static Stream<Arguments> validEvenLengthProvider() {
        return Stream.of(// empty string → empty array
        Arguments.of("", new byte[0], "Empty string should be considered valid (length 0, even) and decode to an empty byte array."), // single byte 0x00
        Arguments.of("00", new byte[] { 0 }, "Single byte represented by \"00\" should decode to a byte array containing 0."), // single byte 0x0F (15)
        Arguments.of("0F", new byte[] { 0x0F }, "Hex \"0F\" (15 decimal) should decode correctly."), // single byte 0xFF → -1 as signed byte
        Arguments.of("FF", new byte[] { (byte) 0xFF }, "Hex \"FF\" represents -1 in signed byte representation."), // full range of hex digits
        Arguments.of("0123456789ABCDEF", new byte[] { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF }, "Longer even‑length string covering all hex digits should decode to the corresponding byte values."));
    }

    private static Stream<Arguments> oddLengthProvider() {
        return Stream.of(Arguments.of("0", "String with length 1 (odd) should be considered invalid and return null."), Arguments.of("ABC", "String with length 3 (odd) should be considered invalid and return null."), Arguments.of("12345", "String with length 5 (odd) should be considered invalid and return null."));
    }

    private static Stream<Arguments> invalidHexProvider() {
        return Stream.of(// "GG" → hi=-1, lo=-1 → (-1*16)+(-1) = -17 → (byte)-17 == 0xEF
        Arguments.of("GG", new byte[] { (byte) -17 }, "Both characters are non‑hex; Character.digit returns -1 for each, resulting in byte -17 (0xEF)."), // "0G" → hi=0, lo=-1 → (0*16)+(-1) = -1
        Arguments.of("0G", new byte[] { (byte) -1 }, "High nibble is valid (0), low nibble is invalid (G -> -1); result is -1."), // "G0" → hi=-1, lo=0 → (-1*16)+0 = -16
        Arguments.of("G0", new byte[] { (byte) -16 }, "High nibble invalid (G -> -1), low nibble valid (0); result is -16."), // "Zz" → both invalid → -17
        Arguments.of("Zz", new byte[] { (byte) -17 }, "Both characters are invalid (Z and z); each maps to -1, yielding -17."));
    }

    private static Stream<Arguments> caseInsensitiveProvider() {
        return Stream.of(// "aF" → 0xAF → -81
        Arguments.of("aF", new byte[] { (byte) 0xAF }, "Lower‑case 'a' and upper‑case 'F' should be interpreted as 0xAF, which as a signed byte is -81."), // "AbCdEf" → 0xAB,0xCD,0xEF → -85,-51,-17
        Arguments.of("AbCdEf", new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF }, "Mixed case string should decode to bytes 0xAB, 0xCD, 0xEF (signed values -85, -51, -17)."), // "deadBEEF" → 0xDE,0xAD,0xBE,0xEF → -34,-83,-66,-17
        Arguments.of("deadBEEF", new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF }, "Mixed‑case string \"deadBEEF\" should decode to 0xDE,0xAD,0xBE,0xEF (signed values -34, -83, -66, -17)."));
    }

    @Test
    void decode_ValidEvenLengthHex_ReturnsCorrectByteArray() {
        // given
        String hex = "0A1B2C3D";
        // when
        byte[] result = ByteUtilities.decode(hex);
        // then
        byte[] expected = new byte[] { (byte) 0x0A, (byte) 0x1B, (byte) 0x2C, (byte) 0x3D };
        assertArrayEquals(expected, result, "The decoded byte array does not match the expected values.");
    }

    @Test
    void decode_OddLengthString_ReturnsNull() {
        // given
        // length 3 – not divisible by 2
        String oddHex = "ABC";
        // when
        byte[] result = ByteUtilities.decode(oddHex);
        // then
        assertNull(result, "decode should return null for strings with odd length.");
    }

    @Test
    void decode_EmptyString_ReturnsEmptyArray() {
        // given
        String empty = "";
        // when
        byte[] result = ByteUtilities.decode(empty);
        // then
        assertNotNull(result, "Result should not be null for an empty string.");
        assertEquals(0, result.length, "Resulting array should be empty.");
    }

    @Test
    void decode_InvalidHexCharacters_ProducesByteValuesBasedOnDigitMinusOne() {
        // given
        // 'G' is not a valid hexadecimal digit; Character.digit returns -1
        String invalidHex = "GG";
        // when
        byte[] result = ByteUtilities.decode(invalidHex);
        // then
        // hi = lo = -1 -> (hi * 16) + lo = (-1 * 16) + (-1) = -17 -> (byte)-17 == 0xEF
        byte[] expected = new byte[] { (byte) 0xEF };
        assertArrayEquals(expected, result, "When invalid hex characters are supplied, the method uses -1 from Character.digit, resulting in byte 0xEF.");
    }

    /* --------------------------------------------------------------
     *  Valid even‑length hex strings
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] decode(\"{0}\") → {2}")
    @MethodSource("validEvenLengthProvider")
    void decode_ValidEvenLengthHex_ReturnsExpectedByteArray(String hex, byte[] expected, String description) {
        // when
        byte[] result = ByteUtilities.decode(hex);
        // then
        assertArrayEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Invalid hex characters – Character.digit returns -1
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] decode(\"{0}\") → {1}")
    @MethodSource("invalidHexProvider")
    void decode_InvalidHexCharacters_ProducesExpectedBytes(String invalidHex, byte[] expected, String description) {
        // when
        byte[] result = ByteUtilities.decode(invalidHex);
        // then
        assertArrayEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Case‑insensitivity
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] decode(\"{0}\") → {1}")
    @MethodSource("caseInsensitiveProvider")
    void decode_CaseInsensitivity_ReturnsCorrectBytes(String hex, byte[] expected, String description) {
        // when
        byte[] result = ByteUtilities.decode(hex);
        // then
        assertArrayEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Null input – should throw NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void decode_NullInput_ThrowsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> ByteUtilities.decode(null), "Passing null should cause a NullPointerException when the method attempts to call s.length().");
    }

    /* --------------------------------------------------------------
     *  Longer even‑length hex string
     * -------------------------------------------------------------- */
    @Test
    void decode_LongEvenLengthHex_ReturnsCorrectByteArray() {
        // given
        String hex = "00112233445566778899AABBCCDDEEFF0011223344556677";
        // expected bytes (24 bytes)
        byte[] expected = new byte[] { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xAA, (byte) 0xBB, (byte) 0xCC, (byte) 0xDD, (byte) 0xEE, (byte) 0xFF, 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77 };
        // when
        byte[] result = ByteUtilities.decode(hex);
        // then
        assertArrayEquals(expected, result, "A 48‑character hex string (24 bytes) covering a range of values should decode correctly.");
    }
}
