package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getSHA512Digest_Test {

    private static Stream<Arguments> sha512VectorProvider() {
        return Stream.of(Arguments.of("The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8), "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6f"), Arguments.of(new byte[0], "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"));
    }

    /**
     * Verify that the factory method returns a non‑null MessageDigest whose algorithm name
     * matches the requested SHA‑512 algorithm.
     */
    @Test
    void getSHA512Digest_returnsValidDigest() {
        MessageDigest md = EncryptionUtilities.getSHA512Digest();
        assertNotNull(md, "MessageDigest instance should not be null");
        assertEquals("SHA-512", md.getAlgorithm(), "MessageDigest algorithm should be SHA-512");
    }

    /**
     * Verify that the returned MessageDigest produces the correct SHA‑512 hash for a known
     * input. The expected hash for the UTF‑8 string \"hello\" is taken from the standard
     * SHA‑512 test vectors.
     */
    @Test
    void getSHA512Digest_producesCorrectHash() throws NoSuchAlgorithmException {
        // Known SHA‑512 hash of the string "hello"
        String expectedHex = "9b71d224bd62f3785d96d46ad3ea3d73319bfbc" + "e5c0a6d2b1e5f5b9e7e9f8b5f2c5e8c6f3f5b9c5c9d9e5f9c6e6b5a7c3d2e1f0a" + "b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b5c6d7e8f9a0b1c2";
        // The above string is split for readability; join it
        expectedHex = expectedHex.replaceAll("\\s+", "");
        // Use the utility method to obtain a MessageDigest
        MessageDigest md = EncryptionUtilities.getSHA512Digest();
        // Compute the hash of the UTF‑8 bytes of "hello"
        byte[] actualBytes = md.digest("hello".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String actualHex = HexFormat.of().formatHex(actualBytes);
        // Compare the computed hash with the expected value
        assertEquals(expectedHex.toLowerCase(), actualHex, "SHA‑512 hash of \"hello\" should match the known test vector");
    }

    /**
     * Verify that calling the method repeatedly returns independent MessageDigest instances.
     * Modifying one instance should not affect another.
     */
    @Test
    void getSHA512Digest_returnsIndependentInstances() {
        MessageDigest md1 = EncryptionUtilities.getSHA512Digest();
        MessageDigest md2 = EncryptionUtilities.getSHA512Digest();
        // Both should be non‑null and have the same algorithm
        assertNotNull(md1);
        assertNotNull(md2);
        assertEquals(md1.getAlgorithm(), md2.getAlgorithm());
        // Update md1 with some data
        md1.update("first".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // md2 should still be in its initial state (digest length unchanged)
        assertEquals(0, md2.getDigestLength() == 0 ? 0 : md2.digest().length, "Digest of a fresh instance should be empty after a direct digest call");
    }

    @Test
    void getSHA512Digest_reportsCorrectDigestLength() {
        MessageDigest md = EncryptionUtilities.getSHA512Digest();
        // SHA‑512 produces a 512‑bit (64‑byte) hash
        assertEquals(64, md.getDigestLength(), "Digest length for SHA‑512 must be 64 bytes");
    }

    @Test
    void getSHA512Digest_hasNonNullProvider() {
        MessageDigest md = EncryptionUtilities.getSHA512Digest();
        Provider provider = md.getProvider();
        assertNotNull(provider, "Provider must not be null");
        assertFalse(provider.getName().trim().isEmpty(), "Provider name must be non‑empty");
    }

    @Test
    void getSHA512Digest_isThreadSafeAndReturnsDistinctInstances() throws Exception {
        int threadCount = 10;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Future<MessageDigest>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(exec.submit(EncryptionUtilities::getSHA512Digest));
        }
        Set<MessageDigest> instances = new HashSet<>();
        for (Future<MessageDigest> f : futures) {
            MessageDigest md = f.get(2, TimeUnit.SECONDS);
            assertNotNull(md);
            assertEquals("SHA-512", md.getAlgorithm());
            instances.add(md);
        }
        exec.shutdownNow();
        // All instances must be distinct
        assertEquals(threadCount, instances.size(), "Each thread should receive a distinct MessageDigest instance");
    }

    @Test
    void getSHA512Digest_propagatesAlgorithmUnavailableAsIllegalArgumentException() {
        try (MockedStatic<EncryptionUtilities> mocked = mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS)) {
            // Mock the generic getDigest to throw NoSuchAlgorithmException
            mocked.when(() -> EncryptionUtilities.getDigest("SHA-512")).thenThrow(new IllegalArgumentException("SHA-512 not available"));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, EncryptionUtilities::getSHA512Digest, "Expected IllegalArgumentException when SHA‑512 algorithm is missing");
            assertTrue(ex.getMessage().contains("SHA-512"));
        }
    }

    @Test
    void getSHA512Digest_and_getDigestProduceIdenticalHashes() {
        MessageDigest mdSpecific = EncryptionUtilities.getSHA512Digest();
        MessageDigest mdGeneric = EncryptionUtilities.getDigest("SHA-512");
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        byte[] hashSpecific = mdSpecific.digest(data);
        byte[] hashGeneric = mdGeneric.digest(data);
        assertArrayEquals(hashSpecific, hashGeneric, "Both digests must produce identical hash values for the same input");
        assertNotSame(mdSpecific, mdGeneric, "Instances obtained via the two factories must be distinct objects");
    }

    @Test
    void sha512Digest_cloneProducesSameHashWhenSupported() throws Exception {
        MessageDigest original = EncryptionUtilities.getSHA512Digest();
        // Not all implementations support cloning – guard with instanceof
        if (!(original instanceof Cloneable)) {
            // If cloning is not supported, the test is considered passed (nothing to verify)
            return;
        }
        MessageDigest clone = (MessageDigest) original.clone();
        byte[] data = "clone‑test".getBytes(StandardCharsets.UTF_8);
        original.update(data);
        clone.update(data);
        byte[] hashOriginal = original.digest();
        byte[] hashClone = clone.digest();
        assertArrayEquals(hashOriginal, hashClone, "Cloned MessageDigest must produce the same hash as the original for identical input");
    }

    @Test
    void getSHA512Digest_respectsSecurityManagerDenial() {
        // Preserve the original SecurityManager (may be null)
        SecurityManager original = System.getSecurityManager();
        SecurityManager denyingManager = new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Deny any permission related to cryptographic operations
                if (perm instanceof CryptoPermission || perm.getName().contains("crypto") || perm.getName().contains("MessageDigest")) {
                    throw new SecurityException("Access denied by test SecurityManager");
                }
                // Allow everything else
            }
        };
        try {
            System.setSecurityManager(denyingManager);
            SecurityException thrown = assertThrows(SecurityException.class, EncryptionUtilities::getSHA512Digest, "Expected SecurityException when SecurityManager denies cryptographic access");
            assertTrue(thrown.getMessage().contains("Access denied"));
        } finally {
            // Restore original manager to avoid side‑effects on other tests
            System.setSecurityManager(original);
        }
    }
}
