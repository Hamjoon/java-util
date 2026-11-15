package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateSHA1Hash_Test {

    /**
     * Helper that converts a byte array to a lower‑case hex string,
     * matching the format used by {@link EncryptionUtilities#calculateHash}.
     */
    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Provides the test vectors defined in the JSON file.
     *
     * @return a stream of arguments: byte[] input, expected hex string (or
     *         the literal "NullPointerException"), and a description.
     */
    private static Stream<Arguments> sha1TestCases() {
        return Stream.of(// Empty array
        Arguments.of(new byte[0], "da39a3ee5e6b4b0d3255bfef95601890afd80709", "Empty byte array should produce the SHA‑1 hash of an empty input."), // "abc"
        Arguments.of(new byte[] { 97, 98, 99 }, "a9993e364706816aba3e25717850c26c9cd0d89d", "Byte array representing the ASCII string \"abc\" should return its known SHA‑1 hash."), // "The quick brown fox jumps over the lazy dog"
        Arguments.of(new byte[] { 84, 104, 101, 32, 113, 117, 105, 99, 107, 32, 98, 114, 111, 119, 110, 32, 102, 111, 120, 32, 106, 117, 109, 112, 115, 32, 111, 118, 101, 114, 32, 116, 104, 101, 32, 108, 97, 122, 121, 32, 100, 111, 103 }, "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12", "Byte array for the sentence \"The quick brown fox jumps over the lazy dog\" should yield its standard SHA‑1 hash."), // null input – expect NPE
        Arguments.of(null, "NullPointerException", "Passing a null reference should cause a NullPointerException."), // Mixed positive/negative bytes
        Arguments.of(new byte[] { 0, -1, 127, -128, 1, 2, 3 }, "e2c569be17396eca2a2e3c11578123edb1f9d3b5", "A mixed byte array containing negative and positive values verifies that the method correctly handles the full byte range."), // Single byte 'a'
        Arguments.of(new byte[] { 97 }, "86f7e437faa5a7fce15d1ddcb9eaeaea377667b8", "Single‑byte array containing the ASCII character 'a' (0x61) – verifies the method works for the smallest non‑empty input."), // 56‑byte test vector "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
        Arguments.of(new byte[] { 97, 98, 99, 100, 98, 99, 100, 101, 99, 100, 101, 102, 100, 101, 102, 103, 101, 102, 103, 104, 102, 103, 104, 105, 103, 104, 105, 106, 104, 105, 106, 107, 105, 106, 107, 108, 106, 107, 108, 109, 107, 108, 109, 110, 108, 109, 110, 111, 109, 110, 111, 112, 110, 111, 112, 113, 111, 112, 113, 114, 112, 113, 114, 115, 113, 114, 115, 116, 114, 115, 116, 117, 115, 116, 117, 118, 116, 117, 118, 119, 117, 118, 119, 120, 118, 119, 120, 121, 118, 119, 120, 121, 122, 119, 120, 121, 122, 123, 120, 121, 122, 123, 124, 121, 122, 123, 124, 125, 122, 123, 124, 125, 126, 123, 124, 125, 126, 127, 124, 125, 126, 127, 128 }, "84983e441c3bd26ebaae4aa1f95129e5e54670f1", "Standard SHA‑1 test vector – the 56‑byte string \"abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq\"."), // One million 'a' characters
        Arguments.of(createRepeatedByteArray((byte) 0x61, 1_000_000), "34aa973cd4c4daa4f61eeb2bdbad27316534016f", "Very large input (one million 'a' characters) tests the method’s ability to handle big data streams without overflow or performance degradation."));
    }

    /**
     * Utility to build a large byte array filled with a repeated value.
     */
    private static byte[] createRepeatedByteArray(byte value, int count) {
        byte[] arr = new byte[count];
        Arrays.fill(arr, value);
        return arr;
    }

    @Test
    void calculateSHA1Hash_WithKnownString_ShouldMatchExpectedHash() throws NoSuchAlgorithmException {
        // GIVEN
        String input = "OpenAI ChatGPT";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
        // Expected SHA‑1 using the JDK directly
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] expectedDigest = md.digest(inputBytes);
        String expectedHash = toHexString(expectedDigest);
        // WHEN
        String actualHash = EncryptionUtilities.calculateSHA1Hash(inputBytes);
        // THEN
        assertEquals(expectedHash, actualHash, "SHA‑1 hash produced by EncryptionUtilities does not match JDK calculation");
    }

    @Test
    void calculateSHA1Hash_WithEmptyArray_ShouldReturnSha1OfEmpty() throws NoSuchAlgorithmException {
        // GIVEN
        byte[] empty = new byte[0];
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String expectedHash = toHexString(md.digest(empty));
        // WHEN
        String actualHash = EncryptionUtilities.calculateSHA1Hash(empty);
        // THEN
        assertEquals(expectedHash, actualHash, "SHA‑1 hash of an empty byte array should match the JDK result");
    }

    @Test
    void calculateSHA1Hash_WithNullInput_ShouldThrowNullPointerException() {
        // GIVEN
        byte[] nullBytes = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateSHA1Hash(nullBytes), "Passing null should result in NullPointerException");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("sha1TestCases")
    void calculateSHA1Hash_VariousInputs(byte[] input, String expected, String description) throws NoSuchAlgorithmException {
        if ("NullPointerException".equals(expected)) {
            // The contract of calculateSHA1Hash is to let the underlying
            // MessageDigest throw NPE when a null array is supplied.
            assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateSHA1Hash(input), "Expected NullPointerException for null input");
        } else {
            // Compute the expected hash using the JDK directly – this also
            // serves as a sanity check that the expected literal is correct.
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] jdkDigest = md.digest(input);
            String jdkHex = toHexString(jdkDigest);
            assertEquals(expected.toLowerCase(), jdkHex, "Sanity check: the hard‑coded expected hash does not match JDK calculation");
            // Actual method under test
            String actual = EncryptionUtilities.calculateSHA1Hash(input);
            assertEquals(expected.toLowerCase(), actual, "SHA‑1 hash produced by EncryptionUtilities does not match expected value");
        }
    }
}
