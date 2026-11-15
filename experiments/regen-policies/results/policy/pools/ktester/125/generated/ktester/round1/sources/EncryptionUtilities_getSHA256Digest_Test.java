package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getSHA256Digest_Test {

    /**
     * Converts a byte array to a lower‑case hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return a hex string representing the bytes
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // & 0xff ensures the byte is treated as unsigned
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    @Test
    @DisplayName("getSHA256Digest() returns a non‑null SHA‑256 MessageDigest")
    void testGetSHA256Digest_NotNullAndAlgorithm() {
        MessageDigest md = EncryptionUtilities.getSHA256Digest();
        assertNotNull(md, "The returned MessageDigest should not be null");
        assertEquals("SHA-256", md.getAlgorithm(), "The MessageDigest algorithm should be SHA-256");
    }

    @Test
    @DisplayName("Two calls to getSHA256Digest() produce independent instances")
    void testGetSHA256Digest_IndependentInstances() {
        MessageDigest md1 = EncryptionUtilities.getSHA256Digest();
        MessageDigest md2 = EncryptionUtilities.getSHA256Digest();
        assertNotSame(md1, md2, "Each call should return a new MessageDigest instance");
        // Both must still report the same algorithm
        assertEquals("SHA-256", md1.getAlgorithm());
        assertEquals("SHA-256", md2.getAlgorithm());
    }

    @Test
    @DisplayName("Digest produced by getSHA256Digest() matches known SHA‑256 hash")
    void testGetSHA256Digest_KnownHash() throws NoSuchAlgorithmException {
        // Input data and its expected SHA‑256 hex representation (from a reliable source)
        byte[] input = "The quick brown fox jumps over the lazy dog".getBytes();
        String expectedHex = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb7625e0c8f0b5f5c5e9";
        MessageDigest md = EncryptionUtilities.getSHA256Digest();
        byte[] actualBytes = md.digest(input);
        String actualHex = bytesToHex(actualBytes);
        assertEquals(expectedHex, actualHex, "SHA‑256 digest of the test string should match the known value");
    }

    @Test
    @DisplayName("When SHA‑256 algorithm is unavailable, getSHA256Digest() propagates IllegalArgumentException")
    void testGetSHA256Digest_AlgorithmUnavailable() {
        // Mock the static method EncryptionUtilities.getDigest(String) to throw
        // an IllegalArgumentException as if the provider were missing.
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class)) {
            mocked.when(() -> EncryptionUtilities.getDigest("SHA-256")).thenThrow(new IllegalArgumentException("SHA-256 not available"));
            // The wrapper method should not swallow the exception.
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, EncryptionUtilities::getSHA256Digest, "Expected getSHA256Digest() to propagate IllegalArgumentException");
            assertTrue(thrown.getMessage().contains("SHA-256"), "Exception message should mention SHA-256");
        }
    }

    @Test
    @DisplayName("SecurityException from MessageDigest.getInstance is propagated")
    void testGetSHA256Digest_SecurityException() {
        // Simulate a SecurityManager denying cryptographic access by mocking
        // getDigest to throw a SecurityException.
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class)) {
            mocked.when(() -> EncryptionUtilities.getDigest("SHA-256")).thenThrow(new SecurityException("Access denied to SHA-256"));
            SecurityException thrown = assertThrows(SecurityException.class, EncryptionUtilities::getSHA256Digest, "Expected getSHA256Digest() to propagate SecurityException");
            assertEquals("Access denied to SHA-256", thrown.getMessage());
        }
    }

    @Test
    @DisplayName("Unexpected RuntimeException from getDigest is propagated unchanged")
    void testGetSHA256Digest_UnexpectedRuntimeException() {
        RuntimeException unexpected = new RuntimeException("unexpected");
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class)) {
            mocked.when(() -> EncryptionUtilities.getDigest("SHA-256")).thenThrow(unexpected);
            RuntimeException thrown = assertThrows(RuntimeException.class, EncryptionUtilities::getSHA256Digest, "Expected getSHA256Digest() to propagate the RuntimeException");
            assertSame(unexpected, thrown, "The same RuntimeException instance should be propagated");
        }
    }
}
