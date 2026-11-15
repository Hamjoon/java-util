package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getSHA512Digest_Test {

    /**
     * Convert a byte array to a hexadecimal string.
     *
     * @param bytes the byte array
     * @return lower‑case hex representation
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    @Test
    void getSHA512Digest_returnsMessageDigestWithSHA512Algorithm() throws NoSuchAlgorithmException {
        // given
        MessageDigest expected = MessageDigest.getInstance("SHA-512");
        // when
        MessageDigest actual = EncryptionUtilities.getSHA512Digest();
        // then
        assertNotNull(actual, "The returned MessageDigest should not be null");
        assertEquals(expected.getAlgorithm(), actual.getAlgorithm(), "Algorithm name should be SHA-512");
        // The digest length for SHA‑512 is 64 bytes (512 bits)
        assertEquals(64, actual.getDigestLength(), "SHA-512 digest length should be 64 bytes");
    }

    @Test
    void getSHA512Digest_isReusableAndProducesConsistentLength() {
        // given
        MessageDigest md1 = EncryptionUtilities.getSHA512Digest();
        MessageDigest md2 = EncryptionUtilities.getSHA512Digest();
        byte[] data = "test data".getBytes();
        // when
        byte[] hash1 = md1.digest(data);
        byte[] hash2 = md2.digest(data);
        // then
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertEquals(64, hash1.length, "SHA-512 hash length should be 64 bytes");
        assertEquals(64, hash2.length, "SHA-512 hash length should be 64 bytes");
        assertArrayEquals(hash1, hash2, "Two independent digests of the same data should be equal");
    }

    /**
     * Verify that two successive calls return *different* objects (no shared singleton).
     */
    @Test
    void getSHA512Digest_returnsDistinctInstances() {
        MessageDigest first = EncryptionUtilities.getSHA512Digest();
        MessageDigest second = EncryptionUtilities.getSHA512Digest();
        assertNotSame(first, second, "Each call should create a new MessageDigest instance");
    }

    /**
     * Compute the SHA‑512 hash of an empty byte array and compare it with the
     * known reference value (RFC 4634 test vector).
     */
    @Test
    void getSHA512Digest_computesCorrectHashForEmptyInput() {
        // Known SHA‑512 of empty input (hex, lower case)
        final String EXPECTED_EMPTY_SHA512 = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9c" + "e47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e";
        MessageDigest md = EncryptionUtilities.getSHA512Digest();
        // empty array
        byte[] hash = md.digest(new byte[0]);
        assertEquals(64, hash.length, "Empty‑input SHA‑512 hash must be 64 bytes");
        assertEquals(EXPECTED_EMPTY_SHA512, bytesToHex(hash).toLowerCase(), "Hash of empty input does not match the known reference value");
    }

    /**
     * Ensure that a previously used MessageDigest does not affect a newly obtained one.
     */
    @Test
    void getSHA512Digest_newInstanceUnaffectedByPreviousState() {
        // First digest – update with some data
        MessageDigest used = EncryptionUtilities.getSHA512Digest();
        used.update("some random data".getBytes());
        // Second (fresh) digest – compute hash of empty array
        MessageDigest fresh = EncryptionUtilities.getSHA512Digest();
        byte[] freshHash = fresh.digest(new byte[0]);
        // Expected hash of empty input (same as previous test)
        final String EXPECTED_EMPTY_SHA512 = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9c" + "e47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e";
        assertEquals(EXPECTED_EMPTY_SHA512, bytesToHex(freshHash).toLowerCase(), "A fresh MessageDigest must produce the correct empty‑input hash regardless of prior updates");
    }

    /**
     * Run the method concurrently from many threads and verify each result is
     * non‑null and correctly configured. This demonstrates thread‑safety.
     */
    @Test
    void getSHA512Digest_threadSafety() throws InterruptedException, ExecutionException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<MessageDigest>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> EncryptionUtilities.getSHA512Digest());
        }
        List<Future<MessageDigest>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in time");
        for (Future<MessageDigest> f : futures) {
            MessageDigest md = f.get();
            assertNotNull(md, "Digest obtained from a thread must not be null");
            assertEquals("SHA-512", md.getAlgorithm(), "Digest obtained from a thread must be configured for SHA-512");
        }
    }

    /**
     * Simulate the situation where the SHA‑512 algorithm is not available.
     * We mock {@link MessageDigest#getInstance(String)} to throw
     * {@link NoSuchAlgorithmException} and verify that {@code getSHA512Digest}
     * propagates the problem (wrapped in a RuntimeException or as the original).
     */
    @Test
    void getSHA512Digest_propagatesNoSuchAlgorithmException() {
        try (MockedStatic<MessageDigest> mocked = Mockito.mockStatic(MessageDigest.class)) {
            mocked.when(() -> MessageDigest.getInstance("SHA-512")).thenThrow(new NoSuchAlgorithmException("SHA-512 not supported"));
            // The method under test may either throw the checked exception wrapped
            // in a RuntimeException or let it bubble up as a RuntimeException.
            // We therefore assert that *some* RuntimeException is thrown and that
            // its cause is the NoSuchAlgorithmException we injected.
            RuntimeException ex = assertThrows(RuntimeException.class, EncryptionUtilities::getSHA512Digest, "Expected a RuntimeException when SHA-512 algorithm is unavailable");
            assertTrue(ex.getCause() instanceof NoSuchAlgorithmException, "Cause of the RuntimeException should be NoSuchAlgorithmException");
        }
    }

    /**
     * Simulate a {@link SecurityManager} that denies access to the SHA‑512 algorithm.
     * The static call to {@code MessageDigest.getInstance} should throw a
     * {@link SecurityException}. The test verifies that this exception (or a
     * wrapped version) propagates out of {@code getSHA512Digest}.
     */
    @Test
    void getSHA512Digest_propagatesSecurityException() {
        try (MockedStatic<MessageDigest> mocked = Mockito.mockStatic(MessageDigest.class)) {
            mocked.when(() -> MessageDigest.getInstance("SHA-512")).thenThrow(new SecurityException("Access denied to SHA-512"));
            RuntimeException ex = assertThrows(RuntimeException.class, EncryptionUtilities::getSHA512Digest, "Expected a RuntimeException when a SecurityException is thrown");
            assertTrue(ex.getCause() instanceof SecurityException, "Cause of the RuntimeException should be SecurityException");
        }
    }
}
