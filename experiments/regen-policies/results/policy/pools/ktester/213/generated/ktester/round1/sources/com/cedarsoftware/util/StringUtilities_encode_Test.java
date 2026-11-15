package com.cedarsoftware.util;

// @ExtendWith(MockitoExtension.class)
// class StringUtilitiesEncodeTest extends StringUtilitiesEncodeTest {

    /**
     * Simple holder for a test case (input bytes + expected hex string).
     * Kept for documentation purposes – the actual provider returns {@link Arguments}.
     */
    @SuppressWarnings("unused")
    private static final class EncodeCase {

        final byte[] input;

        final String expected;

        EncodeCase(byte[] input, String expected) {
            this.input = input;
            this.expected = expected;
        }
    }

    private static Stream<byte[]> byteArrayProvider() {
        return Stream.of(new byte[] { 0x01, 0x23, 0x45, 0x67 }, new byte[] { (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF }, new byte[] { 0x00, (byte) 0xFF, 0x10, (byte) 0x80 }, new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, 0x00, 0x01 });
    }

    /**
     * Provides a mixture of byte arrays together with the expected lower‑case
     * hexadecimal representation. The values include positive, negative and
     * boundary bytes to ensure full nibble coverage.
     */
    private static Stream<Arguments> encodeProvider() {
        return Stream.of(Arguments.of(new byte[] { 0x01, 0x23, 0x45, 0x67 }, "01234567"), Arguments.of(new byte[] { (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF }, "89abcdef"), Arguments.of(new byte[] { 0x00, (byte) 0xFF, 0x10, (byte) 0x80 }, "00ff1080"), Arguments.of(new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF, 0x00, 0x01 }, "deadbeef0001"), // Additional cases from the JSON spec
        Arguments.of(new byte[] { 0x12, 0x34 }, "1234"), Arguments.of(new byte[] { 0x0F, (byte) 0xA0 }, "0fa0"), Arguments.of(new byte[] { (byte) -1 }, "ff"), Arguments.of(new byte[] { (byte) -128 }, "80"), Arguments.of(new byte[] { 0x7F }, "7f"), Arguments.of(new byte[] { 0x00, 0x0F, 0x10, (byte) -1 }, "000f10ff"), Arguments.of(new byte[] { (byte) 0xAB, (byte) 0xCD, (byte) 0xEF }, "abcdef"), Arguments.of(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, "00000000000000000000"));
    }

    /**
     * Helper that converts a byte array to a readable hex string (used only in failure messages).
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int v = b & 0xFF;
            sb.append(Character.forDigit((v >>> 4) & 0xF, 16));
            sb.append(Character.forDigit(v & 0xF, 16));
        }
        return sb.toString();
    }

    @Test
    void encode_nullArray_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> StringUtilities.encode(null));
    }

    @Test
    void encode_emptyArray_returnsEmptyString() {
        byte[] empty = new byte[0];
        String result = StringUtilities.encode(empty);
        assertEquals("", result);
    }

    @Test
    void encode_singleByte_returnsTwoHexDigits() {
        // Basic sanity checks
        assertEquals("00", StringUtilities.encode(new byte[] { 0x00 }));
        assertEquals("0f", StringUtilities.encode(new byte[] { 0x0F }));
        assertEquals("a5", StringUtilities.encode(new byte[] { (byte) 0xA5 }));
        assertEquals("ff", StringUtilities.encode(new byte[] { (byte) 0xFF }));
        // Additional cases from the JSON specification
        assertEquals("0a", StringUtilities.encode(new byte[] { 0x0A }));
        assertEquals("ab", StringUtilities.encode(new byte[] { (byte) 0xAB }));
        // -128 signed
        assertEquals("80", StringUtilities.encode(new byte[] { (byte) 0x80 }));
        // 127
        assertEquals("7f", StringUtilities.encode(new byte[] { 0x7F }));
    }

    @ParameterizedTest
    @MethodSource("byteArrayProvider")
    void encode_variousByteArrays_matchesManualHexConversion(byte[] input) {
        // Build expected hex string manually
        StringBuilder expected = new StringBuilder(input.length * 2);
        for (byte b : input) {
            int v = b & 0xFF;
            expected.append(Character.forDigit((v >>> 4) & 0xF, 16));
            expected.append(Character.forDigit(v & 0xF, 16));
        }
        String actual = StringUtilities.encode(input);
        assertEquals("Failed for input: " + bytesToHex(input), expected.toString(), actual);
    }

    @ParameterizedTest(name = "encode({0}) → \"{1}\"")
    @MethodSource("encodeProvider")
    void encode_variousByteArrays_matchesExpected(byte[] input, String expected) {
        String actual = StringUtilities.encode(input);
        assertEquals("Unexpected encoding for input: " + bytesToHex(input), expected, actual);
    }
}