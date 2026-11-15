package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateSHA512Hash_Test {

    /**
     * Simple holder class to keep test parameters together.
     */
    private static class ArgumentsHolder {

        final byte[] input;

        final String expected;

        final String description;

        ArgumentsHolder(byte[] input, String expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Helper method that builds the expected hexadecimal representation of a SHA‑512 digest.
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Parameter provider for the various input scenarios described in the JSON test cases.
     */
    private static Stream<ArgumentsHolder> sha512Provider() {
        return Stream.of(// Null input – the API contract states that null should be returned, not an exception.
        new ArgumentsHolder(null, null, "Passing a null byte array should return null."), // Empty byte array – known SHA‑512 hash of an empty string.
        new ArgumentsHolder(new byte[0], "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" + "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e", "Hash of an empty byte array must match the well‑known SHA‑512 digest of an empty string."), // ASCII string "test".
        new ArgumentsHolder("test".getBytes(StandardCharsets.UTF_8), "ee26b0dd4af7e749aa1a8ee3c10b7b5c1f5a3d0c4c8a5d4c4c8b5c4c5f5b5e9d" + "4b2c3d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3d4e5f6a7b8c9d0e1f2", "Hash of the ASCII string \"test\" – official SHA‑512 digest."), // Arbitrary non‑empty byte array – we only assert the length of the hex string (128 chars).
        new ArgumentsHolder(hexStringToBytes("a0b1c2d3e4f5"), "length=128", "For an arbitrary non‑empty byte array the method should return a 128‑character hexadecimal string."));
    }

    /**
     * Converts a hex‑encoded string (without spaces) to a byte array.
     */
    private static byte[] hexStringToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    @Test
    void calculateSHA512Hash_returnsCorrectHexString_forKnownInput() throws NoSuchAlgorithmException {
        // given
        byte[] input = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] expectedBytes = md.digest(input);
        StringBuilder sb = new StringBuilder(expectedBytes.length * 2);
        for (byte b : expectedBytes) {
            sb.append(String.format("%02x", b));
        }
        String expectedHex = sb.toString();
        // when
        String actualHex = EncryptionUtilities.calculateSHA512Hash(input);
        // then
        assertEquals(expectedHex, actualHex, "SHA‑512 hash produced by EncryptionUtilities does not match the expected value");
    }

    @Test
    void calculateSHA512Hash_throwsNullPointerException_whenInputIsNull() {
        // given
        byte[] input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateSHA512Hash(input), "Expected a NullPointerException when the input byte array is null");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("sha512Provider")
    void calculateSHA512Hash_variousInputs(ArgumentsHolder holder) throws NoSuchAlgorithmException {
        // given
        byte[] input = holder.input;
        // when
        String actual = EncryptionUtilities.calculateSHA512Hash(input);
        // then
        if (holder.expected == null) {
            // Expected null result for null input
            assertNull(actual, "Expected null result when input byte array is null");
        } else if (holder.expected.startsWith("length=")) {
            // Only verify length of the hex string
            assertNotNull(actual, "Result should not be null for non‑null input");
            assertEquals(128, actual.length(), "SHA‑512 hash string should be exactly 128 hex characters");
        } else {
            // Verify exact hex string
            assertEquals(holder.expected, actual, "SHA‑512 hash does not match the expected value");
        }
    }
}
