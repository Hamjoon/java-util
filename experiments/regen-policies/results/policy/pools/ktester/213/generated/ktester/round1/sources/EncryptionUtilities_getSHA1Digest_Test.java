package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getSHA1Digest_Test {

    /**
     * Verifies that {@code getSHA1Digest()} returns a functional {@link MessageDigest}
     * instance with the expected algorithm name and that it correctly hashes a
     * known input.
     */
    @Test
    void getSHA1Digest_returnsValidMessageDigest() throws NoSuchAlgorithmException {
        // Act
        MessageDigest sha1Digest = EncryptionUtilities.getSHA1Digest();
        // Assert – basic contract
        assertNotNull(sha1Digest, "The returned MessageDigest should not be null");
        assertEquals("SHA-1", sha1Digest.getAlgorithm(), "The MessageDigest algorithm should be SHA-1");
        // Arrange – known input and its expected SHA‑1 hex string
        byte[] input = "The quick brown fox jumps over the lazy dog".getBytes();
        // Expected SHA‑1 (from any standard SHA‑1 calculator)
        String expectedHex = "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12";
        // Act – compute the digest
        byte[] actualBytes = sha1Digest.digest(input);
        String actualHex = HexFormat.of().formatHex(actualBytes);
        // Assert – hash correctness
        assertEquals(expectedHex, actualHex, "SHA‑1 digest of the known input should match the expected value");
    }

    /**
     * Ensures that each call to {@code getSHA1Digest()} yields a fresh
     * {@link MessageDigest} instance (i.e., the internal state of one instance
     * does not affect another).
     */
    @Test
    void getSHA1Digest_returnsIndependentInstances() {
        MessageDigest first = EncryptionUtilities.getSHA1Digest();
        MessageDigest second = EncryptionUtilities.getSHA1Digest();
        // Different objects
        assertNotSame(first, second, "Each call should return a new MessageDigest instance");
        // Updating the first must not affect the second
        first.update((byte) 0x01);
        // digest of empty input
        byte[] secondDigest = second.digest();
        String secondHex = HexFormat.of().formatHex(secondDigest);
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", secondHex, "Second digest must remain the SHA‑1 of an empty input");
    }

    /**
     * Functional test – SHA‑1 of an empty byte array.
     */
    @Test
    void getSHA1Digest_hashesEmptyInputCorrectly() {
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        byte[] hash = md.digest(new byte[0]);
        String hex = HexFormat.of().formatHex(hash);
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hex, "SHA‑1 of empty input does not match the known value");
    }

    /**
     * Functional test – SHA‑1 of the string "abc".
     */
    @Test
    void getSHA1Digest_hashesKnownInputCorrectly() {
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        byte[] hash = md.digest("abc".getBytes());
        String hex = HexFormat.of().formatHex(hash);
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", hex, "SHA‑1 of \"abc\" does not match the known value");
    }

    /**
     * Verify that a MessageDigest can be reused after a {@code digest()} call.
     */
    @Test
    void getSHA1Digest_canBeReusedAfterDigest() {
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        byte[] first = md.digest("test".getBytes());
        // After digest() the MessageDigest is automatically reset.
        byte[] second = md.digest("test".getBytes());
        assertArrayEquals(first, second, "Digest after reset should produce identical result");
    }

    /**
     * Concurrent access test – each thread receives an independent, functional
     * MessageDigest instance.
     */
    @Test
    void getSHA1Digest_concurrentAccessProducesCorrectHashes() throws InterruptedException, ExecutionException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Callable<String> task = () -> {
            MessageDigest md = EncryptionUtilities.getSHA1Digest();
            String threadString = Thread.currentThread().getName() + "-payload";
            byte[] hash = md.digest(threadString.getBytes());
            return HexFormat.of().formatHex(hash);
        };
        // Submit tasks
        var futures = IntStream.range(0, threadCount).mapToObj(i -> executor.submit(task)).toList();
        // Verify each result matches the expected hash for its payload
        for (int i = 0; i < threadCount; i++) {
            String resultHex = futures.get(i).get();
            String expectedPayload = "pool-" + (threadCount) + "-thread-" + (i + 1) + "-payload";
            String expectedHex = HexFormat.of().formatHex(EncryptionUtilities.getSHA1Digest().digest(expectedPayload.getBytes()));
            assertEquals(expectedHex, resultHex, "Thread " + i + " produced an unexpected SHA‑1 hash");
        }
        executor.shutdownNow();
    }

    /**
     * Error path – simulate an environment where the SHA‑1 algorithm is not
     * available. {@code getDigest("SHA-1")} is mocked to throw
     * {@link NoSuchAlgorithmException}, which should be wrapped into an unchecked
     * exception by {@code getSHA1Digest()}.
     */
    @Test
    void getSHA1Digest_throwsUncheckedWhenAlgorithmUnavailable() {
        // Mock static method getDigest to throw NoSuchAlgorithmException
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> EncryptionUtilities.getDigest("SHA-1")).thenThrow(new NoSuchAlgorithmException("SHA-1 not supported"));
            // Execute and verify that an unchecked exception propagates
            Executable call = EncryptionUtilities::getSHA1Digest;
            assertThrows(RuntimeException.class, call, "When SHA‑1 algorithm is unavailable, an unchecked exception should be thrown");
        }
    }
}
