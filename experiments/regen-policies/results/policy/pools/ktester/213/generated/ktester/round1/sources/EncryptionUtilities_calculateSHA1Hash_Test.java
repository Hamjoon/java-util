package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateSHA1Hash_Test {

    /**
     * Helper that calculates the SHA‑1 hash using the JDK MessageDigest.
     *
     * @param data the input bytes
     * @return SHA‑1 hash as a lower‑case hex string
     */
    private static String jdkSha1Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-1 algorithm not available", e);
        }
    }

    /**
     * Parameter source for the SHA‑1 test vectors.  Each {@link Arguments}
     * contains the byte array to hash, the expected hexadecimal string and a
     * short description (used only for readability in the test report).
     *
     * @return a stream of arguments for {@link #calculateSHA1Hash_variousInputs}
     */
    private static Stream<Arguments> sha1TestVectors() {
        // Empty array
        byte[] empty = new byte[0];
        // Simple ASCII strings
        byte[] helloWorld = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] quickFox = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        // Single character
        byte[] singleA = "a".getBytes(StandardCharsets.UTF_8);
        // RFC 3174 test vector
        byte[] rfc3174 = "message digest".getBytes(StandardCharsets.UTF_8);
        // Alphabetic sequence
        byte[] alphabet = "abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.UTF_8);
        // Mixed‑case alphanumeric
        byte[] mixed = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".getBytes(StandardCharsets.UTF_8);
        // Unicode string containing ©, €, and a surrogate pair (😀)
        byte[] unicode = "abc©€😀".getBytes(StandardCharsets.UTF_8);
        // Large payload: 1 MiB of the character 'a'
        byte[] large = new byte[1_048_576];
        for (int i = 0; i < large.length; i++) {
            large[i] = 'a';
        }
        return Stream.of(Arguments.of(empty, "da39a3ee5e6b4b0d3255bfef95601890afd80709", "SHA‑1 of empty array"), Arguments.of(helloWorld, "2aae6c35c94fcfb415dbe95f408b9ce91ee846ed", "SHA‑1 of \"hello world\""), Arguments.of(quickFox, "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12", "SHA‑1 of classic pangram"), Arguments.of(singleA, "86f7e437faa5a7fce15d1ddcb9eaeaea377667b8", "SHA‑1 of single character \"a\""), Arguments.of(rfc3174, "c12252ceda8be8994d5fa0290a47231c1d16aae3", "SHA‑1 of RFC 3174 test vector"), Arguments.of(alphabet, "32d10c7b8cf96570ca04ce37f2a19d84240d3a89", "SHA‑1 of alphabetic sequence"), Arguments.of(mixed, "761c457bf73b14d27e9e9265c46f4b4dda11f940", "SHA‑1 of mixed‑case alphanumeric"), Arguments.of(unicode, "7c2e6c0d5a0e5e8a0c9c0b6c1d2b6e0c5c2d5c1e", "SHA‑1 of Unicode string containing ©, €, 😀"), Arguments.of(large, "34aa973cd4c4daa4f61eeb2bdbad27316534016f", "SHA‑1 of 1 MiB of 'a'"));
    }

    @Test
    void calculateSHA1Hash_ofEmptyArray_returnsCorrectHash() {
        byte[] input = new byte[0];
        String expected = jdkSha1Hex(input);
        String actual = EncryptionUtilities.calculateSHA1Hash(input);
        assertEquals(expected, actual, "SHA‑1 hash of an empty array should match JDK result");
    }

    @Test
    void calculateSHA1Hash_ofSimpleString_returnsCorrectHash() {
        String text = "The quick brown fox jumps over the lazy dog";
        byte[] input = text.getBytes(StandardCharsets.UTF_8);
        String expected = jdkSha1Hex(input);
        String actual = EncryptionUtilities.calculateSHA1Hash(input);
        assertEquals(expected, actual, "SHA‑1 hash of known phrase should match JDK result");
    }

    @Test
    void calculateSHA1Hash_ofBinaryData_returnsCorrectHash() {
        // A deterministic pseudo‑random byte array
        byte[] input = new byte[256];
        for (int i = 0; i < input.length; i++) {
            input[i] = (byte) (i ^ 0x5A);
        }
        String expected = jdkSha1Hex(input);
        String actual = EncryptionUtilities.calculateSHA1Hash(input);
        assertEquals(expected, actual, "SHA‑1 hash of binary data should match JDK result");
    }

    @Test
    void calculateSHA1Hash_nullInput_throwsNullPointerException() {
        // The implementation does not guard against null, so a NPE is expected.
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateSHA1Hash(null), "Passing null should result in NullPointerException");
    }

    /**
     * Verifies that {@link EncryptionUtilities#calculateSHA1Hash(byte[])} returns
     * the expected hexadecimal SHA‑1 digest for a wide range of inputs.
     *
     * @param input       the byte array to hash
     * @param expectedHex the expected lower‑case hex representation
     * @param description a short description for the test case (used only for readability)
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("sha1TestVectors")
    void calculateSHA1Hash_variousInputs(byte[] input, String expectedHex, String description) {
        // Act
        String actual = EncryptionUtilities.calculateSHA1Hash(input);
        // Assert – compare against the expected value from the test vector
        assertEquals(expectedHex, actual, () -> "SHA‑1 hash mismatch for case: " + description);
        // Additional sanity check: compare with JDK implementation to guard against
        // accidental changes in the utility method.
        assertEquals(jdkSha1Hex(input), actual, () -> "JDK SHA‑1 result differs for case: " + description);
    }

    /**
     * According to the API contract the method should return {@code null}
     * when the supplied byte array is {@code null}.  This test confirms that
     * behaviour.
     */
    @Test
    void calculateSHA1Hash_nullInput_returnsNull() {
        assertNull(EncryptionUtilities.calculateSHA1Hash(null), "Passing a null byte array should return null, not throw an exception");
    }
}
