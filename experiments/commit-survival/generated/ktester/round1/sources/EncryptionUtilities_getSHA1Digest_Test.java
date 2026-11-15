package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getSHA1Digest_Test {

    /**
     * Helper to convert a byte array to a lower‑case hex string.
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    void getSHA1Digest_ReturnsNonNullMessageDigest() {
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        assertNotNull(md, "The SHA‑1 MessageDigest should not be null");
    }

    @Test
    void getSHA1Digest_HasCorrectAlgorithmName() {
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        assertEquals("SHA-1", md.getAlgorithm(), "The MessageDigest algorithm should be SHA-1");
    }

    @Test
    void getSHA1Digest_ProducesCorrectHashForKnownInput() throws NoSuchAlgorithmException {
        // Known SHA‑1 hash for the UTF‑8 string "OpenAI"
        final String input = "OpenAI";
        final String expectedHex = "2ef7bde608ce5404e97d5f042f95f89f1c232871";
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        byte[] actualBytes = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String actualHex = toHex(actualBytes);
        assertEquals(expectedHex, actualHex, "SHA‑1 hash of \"" + input + "\" should match the known value");
    }

    @Test
    void getSHA1Digest_ReturnsNewInstanceEachCall() {
        MessageDigest first = EncryptionUtilities.getSHA1Digest();
        MessageDigest second = EncryptionUtilities.getSHA1Digest();
        // The two objects should not be the same instance (MessageDigest is mutable)
        assertNotSame(first, second, "Each call should return a new MessageDigest instance");
        // However, they must be functionally equivalent (same algorithm)
        assertEquals(first.getAlgorithm(), second.getAlgorithm(), "Both instances should use the SHA-1 algorithm");
    }

    @Test
    void getSHA1Digest_IsThreadSafeWhenUsedConcurrently() throws InterruptedException {
        // Run the method concurrently to ensure no hidden static state causes failures.
        final int threads = 10;
        Thread[] workers = new Thread[threads];
        MessageDigest[] results = new MessageDigest[threads];
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            workers[i] = new Thread(() -> results[idx] = EncryptionUtilities.getSHA1Digest());
        }
        Arrays.stream(workers).forEach(Thread::start);
        for (Thread t : workers) {
            t.join();
        }
        // Verify each result is a valid SHA‑1 MessageDigest
        for (MessageDigest md : results) {
            assertNotNull(md);
            assertEquals("SHA-1", md.getAlgorithm());
        }
    }

    /**
     * Verify that the SHA‑1 digest of the literal string "test" matches the
     * well‑known SHA‑1 value.
     */
    @Test
    void getSHA1Digest_KnownHashOfStringTest() {
        final String input = "test";
        final String expectedHex = "a94a8fe5ccb19ba61c4c0873d391e987982fbbd3";
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        byte[] actual = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(expectedHex, toHex(actual), "SHA‑1 hash of \"test\" should match the expected digest");
    }

    /**
     * Compute the SHA‑1 hash of an empty byte array and compare it to the
     * official SHA‑1 hash of the empty string.
     */
    @Test
    void getSHA1Digest_EmptyInputHash() {
        final byte[] empty = new byte[0];
        final String expectedHex = "da39a3ee5e6b4b0d3255bfef95601890afd80709";
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        byte[] actual = md.digest(empty);
        assertEquals(expectedHex, toHex(actual), "SHA‑1 hash of an empty byte array should match the known empty‑string digest");
    }

    /**
     * Ensure that calling the underlying {@code getDigest} helper with a null
     * algorithm name throws a {@link NullPointerException}.
     */
    @Test
    void getDigest_NullAlgorithm_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.getDigest(null), "Calling getDigest(null) should raise NullPointerException");
    }

    /**
     * Ensure that calling the underlying {@code getDigest} helper with an
     * unknown algorithm name propagates {@link NoSuchAlgorithmException}.
     */
    @Test
    void getDigest_UnsupportedAlgorithm_ThrowsNoSuchAlgorithmException() {
        assertThrows(NoSuchAlgorithmException.class, () -> EncryptionUtilities.getDigest("UNKNOWN-ALG"), "Calling getDigest with an unsupported algorithm should raise NoSuchAlgorithmException");
    }
}
