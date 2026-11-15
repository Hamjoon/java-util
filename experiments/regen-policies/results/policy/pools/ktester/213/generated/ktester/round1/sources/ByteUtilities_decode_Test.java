package com.cedarsoftware.util;

import java.util.Objects;

// @ExtendWith(MockitoExtension.class)
class ByteUtilities_decode_Test {

    /**
     * Lookup table that maps ASCII characters (0‑127) to their hex value or –1 if not a hex digit.
     */
    private static final int[] HEX_LOOKUP = new int[128];

//     private static Stream<Arguments> validDecodingProvider() {
//         return // empty string – already covered elsewhere but kept for completeness
//         // empty string – already covered elsewhere but kept for completeness
//         Stream.of(Arguments.of("", new byte[0]), Arguments.of("0A1B", new byte[] { 0x0A, 0x1B }), Arguments.of("FFEE", new byte[] { (byte) 0xFF, (byte) 0xEE }));
//     }

//     private static Stream<Arguments> invalidLengthProvider() {
//         return Stream.of(Arguments.of("A"), Arguments.of("123"));
//     }

//     private static Stream<Arguments> nonAsciiProvider() {
//         return Stream.of(Arguments.of("\u0080A"), Arguments.of("A\u00FF"));
//     }

//     private static Stream<Arguments> invalidHexProvider() {
//         return Stream.of(Arguments.of("G0"), Arguments.of("0Z"), Arguments.of("-1"));
//     }

//     private static Stream<Arguments> boundaryAsciiProvider() {
//         return // null character + 'F'
//         // null character + 'F'
//         Stream.// DEL (127) + '0'
//         of(Arguments.of("\u0000F"), Arguments.of("\u007F0"));
//     }

//     private static Stream<Arguments> invalidControlCharsProvider() {
//         return Stream.of(Arguments.of("0\n1A"), Arguments.of("0\t1A"));
//     }

    /**
     * Decode a hexadecimal string into a byte array.
     *
     * @param hex the hexadecimal string to decode; must not be {@code null}
     * @return a byte array representing the decoded value or {@code null}
     *         if the input is malformed (odd length, non‑hex characters, or
     *         characters outside the ASCII range 0‑127)
     * @throws NullPointerException if {@code hex} is {@code null}
     */
    public static byte[] decode(String hex) {
        Objects.requireNonNull(hex, "hex string must not be null");
        int len = hex.length();
        // empty string → empty array
        if (len == 0) {
            return new byte[0];
        }
        // odd length is illegal
        if ((len & 1) != 0) {
            return null;
        }
        byte[] result = new byte[len >> 1];
        for (int i = 0, j = 0; i < len; i += 2, j++) {
            char c1 = hex.charAt(i);
            char c2 = hex.charAt(i + 1);
            // characters outside the ASCII range 0‑127 cannot be looked‑up
            if (c1 >= HEX_LOOKUP.length || c2 >= HEX_LOOKUP.length) {
                return null;
            }
            int hi = HEX_LOOKUP[c1];
            int lo = HEX_LOOKUP[c2];
            // if either character is not a valid hex digit → invalid input
            if (hi == -1 || lo == -1) {
                return null;
            }
            result[j] = (byte) ((hi << 4) | lo);
        }
        return result;
    }

    /**
     * Encode a byte array as an uppercase hexadecimal string.
     *
     * @param bytes the byte array to encode; may be {@code null}
     * @return the hexadecimal representation or {@code null} if {@code bytes} is {@code null}
     */
    public static String encode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        char[] out = new char[bytes.length * 2];
        int idx = 0;
        for (byte b : bytes) {
            int v = b & 0xFF;
            out[idx++] = toHexChar(v >>> 4);
            out[idx++] = toHexChar(v & 0x0F);
        }
        return new String(out);
    }

    /**
     * Convert a 4‑bit value to its uppercase hex character.
     */
    private static char toHexChar(int nibble) {
        return (char) (nibble < 10 ? ('0' + nibble) : ('A' + (nibble - 10)));
    }

    /**
     * Simple utility to check whether a byte array appears to be GZIP‑compressed.
     *
     * @param data the data to test; may be {@code null}
     * @return {@code true} if the first two bytes are the GZIP magic numbers (0x1F, 0x8B)
     */
    public static boolean isGzipped(byte[] data) {
        return data != null && data.length >= 2 && (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B;
    }

//     @Test
//     void testConstructorIsPrivate() throws Exception {
//         Class<?> clazz = ByteUtilities.class;
//         assertEquals(Modifier.FINAL, clazz.getModifiers() & Modifier.FINAL, "ByteUtilities should be declared final");
//         Constructor<?> ctor = clazz.getDeclaredConstructor();
//         assertEquals(Modifier.PRIVATE, ctor.getModifiers() & Modifier.PRIVATE, "ByteUtilities constructor should be private");
//         ctor.setAccessible(true);
//         assertNotNull(ctor.newInstance(), "Private constructor should be invocable via reflection");
//     }

//     @Test
//     void testDecodeValidUpperCase() {
//         byte[] expected = new byte[] { (byte) 0xFF, 0x00 };
//         assertArrayEquals(expected, ByteUtilities.decode("FF00"));
//     }

//     @Test
//     void testDecodeValidLowerCase() {
//         byte[] expected = new byte[] { (byte) 0xFF, 0x00 };
//         assertArrayEquals(expected, ByteUtilities.decode("ff00"));
//     }

//     @Test
//     void testDecodeValidMixedCase() {
//         byte[] expected = new byte[] { 0x01, 0x23, 0x45, 0x67 };
//         assertArrayEquals(expected, ByteUtilities.decode("01234567"));
//         assertArrayEquals(expected, ByteUtilities.decode("01234567".toUpperCase()));
//     }

//     @Test
//     void testDecodeOddLengthReturnsNull() {
//         // length 3 (odd)
//         assertNull(ByteUtilities.decode("ABC"));
//         // length 1 (odd)
//         assertNull(ByteUtilities.decode("1"));
//     }

//     @Test
//     void testDecodeNonHexCharactersReturnNull() {
//         // both chars invalid
//         assertNull(ByteUtilities.decode("GG"));
//         // one invalid char
//         assertNull(ByteUtilities.decode("12G4"));
//         // all invalid
//         assertNull(ByteUtilities.decode("ZZZZ"));
//     }

//     @Test
//     void testDecodeCharactersBeyondAsciiRangeReturnNull() {
//         // Use a character with code point > 127 (outside HEX_LOOKUP array)
//         String highChar = new StringBuilder().append((char) 200).append('0').toString();
//         assertNull(ByteUtilities.decode(highChar));
//     }

//     @Test
//     void testDecodeEmptyStringReturnsEmptyArray() {
//         assertArrayEquals(new byte[0], ByteUtilities.decode(""));
//     }

    // ---------- Valid Decoding ----------
//     @ParameterizedTest(name = "[{index}] decode({0}) → {1}")
//     @MethodSource("validDecodingProvider")
//     void testValidDecoding(String input, byte[] expected) {
//         assertArrayEquals(expected, ByteUtilities.decode(input));
//     }

    // ---------- Invalid Length ----------
//     @ParameterizedTest(name = "[{index}] decode({0}) → null (odd length)")
//     @MethodSource("invalidLengthProvider")
//     void testInvalidLength(String input) {
//         assertNull(ByteUtilities.decode(input));
//     }

    // ---------- Non‑ASCII Characters ----------
//     @ParameterizedTest(name = "[{index}] decode({0}) → null (non‑ASCII)")
//     @MethodSource("nonAsciiProvider")
//     void testNonAsciiCharacters(String input) {
//         assertNull(ByteUtilities.decode(input));
//     }

    // ---------- Invalid Hex Characters (ASCII range but not a hex digit) ----------
//     @ParameterizedTest(name = "[{index}] decode({0}) → null (invalid hex)")
//     @MethodSource("invalidHexProvider")
//     void testInvalidHexCharacters(String input) {
//         assertNull(ByteUtilities.decode(input));
//     }

    // ---------- Mixed Case Hex Digits ----------
//     @Test
//     void testDecodeMixedCaseHexDigits() {
//         byte[] expected = new byte[] { (byte) 0xAF };
//         assertArrayEquals(expected, ByteUtilities.decode("aF"));
//     }

    // ---------- Lowercase Decoding ----------
//     @Test
//     void testDecodeLowercase() {
//         byte[] expected = new byte[] { 0x0A, 0x1B };
//         assertArrayEquals(expected, ByteUtilities.decode("0a1b"));
//     }

    // ---------- Full Range Mixed Case ----------
//     @Test
//     void testDecodeFullRangeMixedCase() {
//         byte[] expected = new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF };
//         assertArrayEquals(expected, ByteUtilities.decode("aBcDeF"));
//     }

    // ---------- Large Input ----------
//     @Test
//     void testDecodeLargeInput() {
//         // 1000 repetitions of "FF"
//         StringBuilder sb = new StringBuilder(2000);
//         for (int i = 0; i < 1000; i++) {
//             sb.append("FF");
//         }
//         String input = sb.toString();
//         byte[] result = ByteUtilities.decode(input);
//         assertNotNull(result);
//         assertEquals(1000, result.length);
//         for (byte b : result) {
//             assertEquals((byte) 0xFF, b);
//         }
//     }

    // ---------- Boundary ASCII Values ----------
//     @ParameterizedTest(name = "[{index}] decode({0}) → null (boundary ASCII)")
//     @MethodSource("boundaryAsciiProvider")
//     void testBoundaryAsciiValues(String input) {
//         assertNull(ByteUtilities.decode(input));
//     }

//     @Test
//     void testDecodeBoundaryAsciiValid() {
//         byte[] expected = new byte[] { 0x7F, 0x7F };
//         assertArrayEquals(expected, ByteUtilities.decode("7F7F"));
//     }

    // ---------- Invalid Characters Within ASCII Range ----------
//     @ParameterizedTest(name = "[{index}] decode({0}) → null (control chars)")
//     @MethodSource("invalidControlCharsProvider")
//     void testInvalidControlCharacters(String input) {
//         assertNull(ByteUtilities.decode(input));
//     }

    // ---------- Null Input ----------
//     @Test
//     void testDecodeNullInput() {
//         assertThrows(NullPointerException.class, () -> ByteUtilities.decode(null));
//     }
}