package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getSHA1Digest_Test {

    /**
     * Convert a byte array to a lower‑case hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return hex representation
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // Mask with 0xff to treat the byte as unsigned, then format as two hex digits
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    @Test
    void getSHA1Digest_returnsMessageDigestWithSHA1Algorithm() throws NoSuchAlgorithmException {
        // GIVEN
        // (nothing to arrange – the method is static)
        // WHEN
        MessageDigest digest = EncryptionUtilities.getSHA1Digest();
        // THEN
        assertNotNull(digest, "Digest should not be null");
        assertEquals("SHA-1", digest.getAlgorithm(), "Algorithm should be SHA-1");
        // Ensure the returned instance behaves like a fresh SHA‑1 digest
        byte[] emptyHash = digest.digest();
        MessageDigest reference = MessageDigest.getInstance("SHA-1");
        assertArrayEquals(reference.digest(), emptyHash, "Digest of empty input should match reference implementation");
    }

    @Test
    void calculateSHA1Hash_producesCorrectHexString() {
        // GIVEN
        // known SHA‑1: a9993e364706816aba3e25717850c26c9cd0d89d
        byte[] data = "abc".getBytes();
        // WHEN
        String hash = EncryptionUtilities.calculateSHA1Hash(data);
        // THEN
        assertNotNull(hash, "Hash string should not be null");
        assertEquals(40, hash.length(), "SHA‑1 hash should be 40 hex characters");
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", hash, "SHA‑1 hash of \"abc\" does not match expected value");
    }

    /**
     * Verify that the SHA‑1 digest produced by the factory matches the known hash of an empty input.
     *
     * The SHA‑1 of an empty byte array is:
     * {@code da39a3ee5e6b4b0d3255bfef95601890afd80709}
     */
    @Test
    void getSHA1Digest_correctlyHashesEmptyByteArray() throws NoSuchAlgorithmException {
        // GIVEN
        MessageDigest digest = EncryptionUtilities.getSHA1Digest();
        byte[] empty = new byte[0];
        // WHEN
        byte[] actualHash = digest.digest(empty);
        String actualHex = bytesToHex(actualHash);
        // THEN
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", actualHex, "SHA‑1 hash of empty input should match the well‑known value");
    }

    /**
     * Ensure that two consecutive calls return distinct MessageDigest objects.
     *
     * This guarantees that callers cannot accidentally share mutable state between
     * digest instances.
     */
    @Test
    void getSHA1Digest_returnsDistinctInstancesOnEachCall() {
        // WHEN
        MessageDigest first = EncryptionUtilities.getSHA1Digest();
        MessageDigest second = EncryptionUtilities.getSHA1Digest();
        // THEN
        assertNotSame(first, second, "Each call should return a new MessageDigest instance");
        // Also verify that they are functionally independent
        first.update("first".getBytes());
        second.update("second".getBytes());
        assertNotEquals(bytesToHex(first.digest()), bytesToHex(second.digest()), "Distinct instances should produce different digests after different updates");
    }

    /**
     * Parameterized test that confirms the digest behaves like a fresh instance for several
     * different empty‑input scenarios (empty array, zero‑length slice, etc.).
     *
     * Although the core logic is the same, this demonstrates the use of JUnit 5 parameterized tests.
     */
    @ParameterizedTest
    @ValueSource(strings = { "", " ", "\n" })
    void getSHA1Digest_emptyInputVariations(String input) throws NoSuchAlgorithmException {
        // GIVEN
        MessageDigest digest = EncryptionUtilities.getSHA1Digest();
        // may be empty or contain whitespace
        byte[] bytes = input.getBytes();
        // WHEN
        byte[] hash = digest.digest(bytes);
        String hex = bytesToHex(hash);
        // THEN
        // For a truly empty input we expect the well‑known hash; for non‑empty we just ensure
        // the method does not throw and returns a 40‑character hex string.
        if (bytes.length == 0) {
            assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hex, "Empty input should produce the known SHA‑1 hash");
        } else {
            assertEquals(40, hex.length(), "SHA‑1 hash string should always be 40 hex characters");
        }
    }
}
