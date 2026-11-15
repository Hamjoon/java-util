package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.security.Security;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getMD5Digest_Test {

    /**
     * Helper method to convert a byte array to a lower‑case hex string.
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // Mask with 0xff to treat the byte as unsigned
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * Verify that {@code getMD5Digest()} returns a non‑null MessageDigest
     * instance that is configured for the MD5 algorithm.
     */
    @Test
    void getMD5Digest_returnsNonNullDigest() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNotNull(md, "MessageDigest instance should not be null");
        assertEquals("MD5", md.getAlgorithm(), "Digest algorithm should be MD5");
    }

    /**
     * Verify that the MD5 digest produced by {@code getMD5Digest()}
     * matches the well‑known MD5 hash for a known input.
     *
     * The MD5 hash of the UTF‑8 string "abc" is:
     * 900150983cd24fb0d6963f7d28e17f72
     */
    @Test
    void getMD5Digest_producesCorrectHash() throws NoSuchAlgorithmException {
        byte[] input = "abc".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        byte[] digestBytes = md.digest(input);
        String actualHex = toHex(digestBytes);
        String expectedHex = "900150983cd24fb0d6963f7d28e17f72";
        assertEquals(expectedHex, actualHex, "MD5 hash of \"abc\" should match the known value");
    }

    @Test
    void getMD5Digest_returnsDistinctInstances() {
        MessageDigest md1 = EncryptionUtilities.getMD5Digest();
        MessageDigest md2 = EncryptionUtilities.getMD5Digest();
        assertNotSame(md1, md2, "Two calls should return distinct MessageDigest instances");
        assertEquals("MD5", md1.getAlgorithm());
        assertEquals("MD5", md2.getAlgorithm());
    }

    @Test
    void getMD5Digest_emptyInputHashMatchesSpecification() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        byte[] empty = new byte[0];
        md.update(empty);
        String actual = toHex(md.digest());
        String expected = "d41d8cd98f00b204e9800998ecf8427e";
        assertEquals(expected, actual, "MD5 of empty input must match the RFC‑1321 reference");
    }

    @Test
    void getMD5Digest_digestLengthIs16Bytes() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        byte[] result = md.digest("any".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(16, result.length, "MD5 digest length must be 16 bytes (128 bits)");
    }

    @Test
    void getMD5Digest_incrementalUpdateProducesCorrectHash() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        md.update("ab".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        md.update("c".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String actual = toHex(md.digest());
        String expected = "900150983cd24fb0d6963f7d28e17f72";
        assertEquals(expected, actual, "Incremental update must yield same hash as single‑step");
    }

    @Test
    void getMD5Digest_resetAllowsReuse() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        byte[] first = md.digest("test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        md.reset();
        byte[] second = md.digest("test".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertArrayEquals(first, second, "Digest after reset should be identical for same input");
    }

    @Test
    void getMD5Digest_largeDataHashIsCorrect() {
        // 1 MiB of the ASCII character 'a' (0x61)
        byte[] data = new byte[1_048_576];
        Arrays.fill(data, (byte) 'a');
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        md.update(data);
        String actual = toHex(md.digest());
        String expected = "7707d6ae4e027c70eea2a935c2296f21";
        assertEquals(expected, actual, "MD5 of 1 MiB of 'a' must match the known reference");
    }

    @Test
    void getMD5Digest_concurrentCallsReturnDistinctInstances() throws InterruptedException, ExecutionException {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<MessageDigest>> tasks = Collections.nCopies(threadCount, (Callable<MessageDigest>) EncryptionUtilities::getMD5Digest);
        List<Future<MessageDigest>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor should terminate");
        List<MessageDigest> digests = new ArrayList<>();
        for (Future<MessageDigest> f : futures) {
            digests.add(f.get());
        }
        // Verify all instances are distinct
        Set<MessageDigest> distinct = new HashSet<>(digests);
        assertEquals(threadCount, distinct.size(), "Each thread should receive a distinct MessageDigest instance");
        // Also verify algorithm
        for (MessageDigest md : digests) {
            assertEquals("MD5", md.getAlgorithm());
        }
    }

    @Test
    void getMD5Digest_whenAlgorithmUnavailable_throwsIllegalArgumentException() {
        // Mock the static helper getDigest to throw NoSuchAlgorithmException
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class)) {
            mocked.when(() -> EncryptionUtilities.getDigest("MD5")).thenThrow(new NoSuchAlgorithmException("MD5 not available"));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, EncryptionUtilities::getMD5Digest, "When MD5 is unavailable the method should wrap the exception in IllegalArgumentException");
            assertTrue(ex.getCause() instanceof NoSuchAlgorithmException, "Cause should be NoSuchAlgorithmException");
        }
    }

    @Test
    void getMD5Digest_whenSecurityExceptionPropagated() {
        // Mock the static helper to throw SecurityException
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class)) {
            mocked.when(() -> EncryptionUtilities.getDigest(anyString())).thenThrow(new SecurityException("Access denied"));
            SecurityException ex = assertThrows(SecurityException.class, EncryptionUtilities::getMD5Digest, "SecurityException from underlying call must be propagated");
            assertEquals("Access denied", ex.getMessage());
        }
    }

    @Test
    void getMD5Digest_nullReturnLeadsToNPEOnUse() {
        // Mock getDigest to return null
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class)) {
            mocked.when(() -> EncryptionUtilities.getDigest("MD5")).thenReturn(null);
            MessageDigest md = EncryptionUtilities.getMD5Digest();
            assertNull(md, "Mocked getDigest returns null, so getMD5Digest should return null");
            // Using the null reference should cause NPE
            assertThrows(NullPointerException.class, () -> md.update("data".getBytes()), "Using a null MessageDigest should throw NullPointerException");
        }
    }
}
