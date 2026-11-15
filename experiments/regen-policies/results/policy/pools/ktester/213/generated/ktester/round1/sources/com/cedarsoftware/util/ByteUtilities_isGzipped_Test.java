package com.cedarsoftware.util;

// @ExtendWith(MockitoExtension.class)
class ByteUtilities_isGzipped_Test {

    /**
     * GZIP magic number – first two bytes of a GZIP stream.
     */
    private static final int GZIP_MAGIC_FIRST = 0x1F;

    private static final int GZIP_MAGIC_SECOND = 0x8B;

//     private static Stream<Arguments> provideStandardCases() {
//         return Stream.of(// null input
//         Arguments.of(null, false, "Input is null – method should return false (first branch of null check)."), // empty array
//         Arguments.of(new byte[] {}, false, "Empty array (length 0) – fails the length >= 2 condition, returns false."), // single‑byte array
//         Arguments.of(new byte[] { 0x1f }, false, "Array length 1 – still fails the length >= 2 condition, returns false."), // two‑byte array, first byte wrong
//         Arguments.of(new byte[] { 0, 0 }, false, "Two‑byte array where the first byte is not 0x1F – should return false."), // two‑byte array, first correct, second wrong
//         Arguments.of(new byte[] { 0x1f, 0 }, false, "Two‑byte array where the first byte is correct (0x1F) but the second byte is not 0x8B – returns false."), // exact gzip header
//         Arguments.of(new byte[] { 0x1f, (byte) 0x8b }, true, "Exact GZIP header bytes (0x1F, 0x8B) – method should return true."), // longer array starting with gzip header
//         Arguments.of(new byte[] { 0x1f, (byte) 0x8b, 0, 1, 2, 3 }, true, "Longer array beginning with the GZIP header – extra bytes are ignored, method returns true."));
//     }

//     private static Stream<Arguments> provideAdditionalCases() {
//         return Stream.of(// second byte as signed -117 (0x8B)
//         Arguments.of(new byte[] { 0x1f, (byte) -117 }, true, "Second byte supplied as signed value -117 (0x8B). Verifies method correctly matches header when bytes are represented with Java signed byte values."), // first byte -1 (0xFF)
//         Arguments.of(new byte[] { (byte) -1, (byte) -117 }, false, "First byte is -1 (0xFF) which is not the GZIP magic number 0x1F. Confirms false is returned when the first header byte is incorrect, even if the second matches."), // second byte 138 (0x8A)
//         Arguments.of(new byte[] { 0x1f, (byte) 138 }, false, "Second byte is 138 (0x8A), one less than the required 0x8B. Checks that the method rejects near‑match headers."), // array length >2 but first byte 0
//         Arguments.of(new byte[] { 0, 0x1f, (byte) 0x8b }, false, "Array length >2 but the first byte is 0, not the GZIP magic number. Validates that only the first two bytes are examined."), // longer array with mixed negative, max, zero bytes
//         Arguments.of(new byte[] { 0x1f, (byte) 0x8b, (byte) -128, 127, 0 }, true, "Longer array starting with correct GZIP header followed by a mix of negative, max, and zero bytes. Ensures extra bytes are ignored and true is returned."));
//     }

    /**
     * Encode a byte array as an upper‑case hexadecimal string.
     *
     * @param bytes the byte array to encode; may be {@code null}
     * @return a hexadecimal representation, or {@code null} if {@code bytes} is {@code null}
     */
    public static String encode(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int v = b & 0xFF;
            sb.append(toHexChar(v >>> 4));
            sb.append(toHexChar(v & 0x0F));
        }
        return sb.toString();
    }

    /**
     * Decode a hexadecimal string into a byte array.
     *
     * @param hex a string containing an even number of hexadecimal characters; may be {@code null}
     * @return the decoded byte array, or {@code null} if the input is {@code null} or malformed
     */
    public static byte[] decode(String hex) {
        if (hex == null) {
            return null;
        }
        // Remove optional whitespace – being tolerant does not hurt.
        hex = hex.replaceAll("\\s", "");
        if ((hex.length() & 1) != 0) {
            // Odd number of characters – cannot form complete bytes.
            return null;
        }
        int len = hex.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi == -1 || lo == -1) {
                // Invalid hex character.
                return null;
            }
            result[i] = (byte) ((hi << 4) + lo);
        }
        return result;
    }

    /**
     * Checks whether the supplied byte array appears to be GZIP‑compressed data.
     * The check is limited to the two‑byte GZIP magic header (0x1F, 0x8B).
     *
     * @param data the byte array to examine; may be {@code null}
     * @return {@code true} if the array is at least two bytes long and starts with the GZIP header,
     *         {@code false} otherwise
     */
    public static boolean isGzipped(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }
        // Use unsigned comparison to avoid sign‑extension problems.
        return (data[0] & 0xFF) == GZIP_MAGIC_FIRST && (data[1] & 0xFF) == GZIP_MAGIC_SECOND;
    }

    /**
     * Convert a 4‑bit value to its upper‑case hexadecimal character.
     *
     * @param nibble a value between 0 and 15 inclusive
     * @return the corresponding hexadecimal character ('0'‑'9', 'A'‑'F')
     */
    private static char toHexChar(int nibble) {
        return (char) (nibble < 10 ? ('0' + nibble) : ('A' + (nibble - 10)));
    }

//     @Test
//     void givenNullArray_thenReturnsFalse() {
//         assertFalse(ByteUtilities.isGzipped(null), "isGzipped should return false for a null array");
//     }

//     @Test
//     void givenArrayShorterThanTwoBytes_thenReturnsFalse() {
//         assertFalse(ByteUtilities.isGzipped(new byte[] {}), "Empty array must not be considered gzipped");
//         assertFalse(ByteUtilities.isGzipped(new byte[] { 0x1f }), "Single‑byte array must not be considered gzipped");
//     }

//     @Test
//     void givenArrayWithWrongHeader_thenReturnsFalse() {
//         // Different first byte
//         assertFalse(ByteUtilities.isGzipped(new byte[] { 0x00, (byte) 0x8b }), "Wrong first header byte should result in false");
//         // Different second byte
//         assertFalse(ByteUtilities.isGzipped(new byte[] { (byte) 0x1f, 0x00 }), "Wrong second header byte should result in false");
//         // Both bytes wrong
//         assertFalse(ByteUtilities.isGzipped(new byte[] { 0x00, 0x00 }), "Both header bytes wrong should result in false");
//     }

//     @Test
//     void givenValidGzipHeader_thenReturnsTrue() {
//         // Minimal valid gzip header (just the two magic bytes)
//         byte[] minimalGzip = new byte[] { (byte) 0x1f, (byte) 0x8b };
//         assertTrue(ByteUtilities.isGzipped(minimalGzip), "Two‑byte gzip magic header must be recognised as gzipped");
//         // A more realistic payload (gzip header + some data)
//         byte[] realistic = new byte[] { // magic
//         (byte) 0x1f, // magic
//         (byte) 0x8b, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00 };
//         assertTrue(ByteUtilities.isGzipped(realistic), "Array starting with gzip magic bytes must be recognised as gzipped");
//     }

//     @Test
//     void givenArrayWithGzipHeaderEmbeddedLater_thenReturnsFalse() {
//         // Header appears after the first two bytes – should still be false
//         byte[] embedded = new byte[] { 0x00, 0x00, (byte) 0x1f, (byte) 0x8b };
//         assertFalse(ByteUtilities.isGzipped(embedded), "Gzip header not at position 0 must not be recognised as gzipped");
//     }

    /* -----------------------------------------------------------
       Parameterized tests – group 1: "isGzipped(byte[]) tests"
       ----------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("provideStandardCases")
//     void isGzipped_StandardCases(byte[] input, boolean expected, String description) {
//         assertEquals(expected, ByteUtilities.isGzipped(input), description);
//     }

    /* -----------------------------------------------------------
       Parameterized tests – group 2: "isGzipped(byte[]) additional tests"
       ----------------------------------------------------------- */
//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("provideAdditionalCases")
//     void isGzipped_AdditionalCases(byte[] input, boolean expected, String description) {
//         assertEquals(expected, ByteUtilities.isGzipped(input), description);
//     }
}