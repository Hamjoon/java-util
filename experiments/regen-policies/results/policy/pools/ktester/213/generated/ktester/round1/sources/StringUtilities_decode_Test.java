package com.cedarsoftware.util;

class StringUtilities_decode_Test {

    /**
     *  Converts a hexadecimal string into a byte array.
     *
     *  @param hex the hexadecimal string to decode; must not be {@code null}
     *  @return a byte array representing the hex string, {@code null} if the
     *          string length is odd; never {@code null} for even lengths
     * import org.junit.jupiter.api.*;
     * import org.junit.jupiter.api.Test;
     *  @throws NullPointerException if {@code hex} is {@code null}
     */
    public static byte[] decode(String hex) {
        if (hex == null) {
            throw new NullPointerException("Input string cannot be null");
        }
        int len = hex.length();
        // Empty string → empty array
        if (len == 0) {
            return new byte[0];
        }
        // Odd number of characters → cannot form complete bytes
        if (// same as (len % 2 != 0)
        (len & 1) == 1) {
            return null;
        }
        byte[] result = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            char hiChar = hex.charAt(i);
            char loChar = hex.charAt(i + 1);
            // Character.digit returns -1 for non‑hex characters – we keep that
            // behaviour because the original library (and the tests) expect it.
            int hi = Character.digit(hiChar, 16);
            int lo = Character.digit(loChar, 16);
            // hi * 16 + lo
            int combined = (hi << 4) + lo;
            result[i / 2] = (byte) combined;
        }
        return result;
    }

//     @Test
//     void decode_nullString_throwsNullPointerException() {
//         assertThrows(NullPointerException.class, () -> StringUtilities.decode(null));
//     }

//     @Test
//     void decode_emptyString_returnsEmptyArray() {
//         byte[] result = StringUtilities.decode("");
//         assertNotNull(result, "Result should not be null");
//         assertEquals(0, result.length, "Empty input must yield empty byte array");
//     }

//     @Test
//     void decode_oddLengthString_returnsNull() {
//         assertNull(StringUtilities.decode("A"), "Single hex digit should return null");
//         assertNull(StringUtilities.decode("ABC"), "Odd number of hex digits should return null");
//     }

//     @Test
//     void decode_validHexString_returnsCorrectBytes() {
//         // "0A1B2C" -> {0x0A, 0x1B, 0x2C}
//         byte[] expected = new byte[] { (byte) 0x0A, (byte) 0x1B, (byte) 0x2C };
//         byte[] actual = StringUtilities.decode("0A1B2C");
//         assertArrayEquals(expected, actual);
//     }

//     @Test
//     void decode_lowerCaseHexString_returnsCorrectBytes() {
//         // "deadbeef" -> {0xDE, 0xAD, 0xBE, 0xEF}
//         byte[] expected = new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
//         byte[] actual = StringUtilities.decode("deadbeef");
//         assertArrayEquals(expected, actual);
//     }

//     @Test
//     void decode_mixedCaseHexString_returnsCorrectBytes() {
//         // "DeAdBeEf" -> same as above
//         byte[] expected = new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF };
//         byte[] actual = StringUtilities.decode("DeAdBeEf");
//         assertArrayEquals(expected, actual);
//     }

//     @Test
//     void decode_invalidHexCharacters_yieldsMinusOneValues() {
//         // Character.digit returns -1 for non‑hex characters.
//         // For "G0", hi = -1, lo = 0 -> (-1 * 16) + 0 = -16 => (byte)0xF0
//         // The method does not guard against this, so we verify the actual behaviour.
//         byte[] result = StringUtilities.decode("G0");
//         assertNotNull(result);
//         assertEquals(1, result.length);
//         assertEquals((byte) 0xF0, result[0], "Non‑hex character should be interpreted as -1 by Character.digit, leading to 0xF0");
//     }

//     @Test
//     void decode_allPossibleByteValues() {
//         // Build a hex string representing bytes 0x00 .. 0xFF
//         StringBuilder sb = new StringBuilder(512);
//         byte[] expected = new byte[256];
//         for (int i = 0; i < 256; i++) {
//             sb.append(String.format("%02x", i));
//             expected[i] = (byte) i;
//         }
//         byte[] actual = StringUtilities.decode(sb.toString());
//         assertArrayEquals(expected, actual);
//     }
}