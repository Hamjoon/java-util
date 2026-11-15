package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getMD5Digest_Test {

    /**
     * Helper to convert a byte array to a lower‑case hex string (identical to typical MD5 output) **
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Provides test data: a byte array and the expected MD5 hex string.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> md5HashProvider() {
        // Empty input
        byte[] empty = new byte[0];
        // "hello"
        byte[] hello = "hello".getBytes();
        // Longer ASCII sentence
        byte[] longAscii = "The quick brown fox jumps over the lazy dog".getBytes();
        // Unicode string "こんにちは" (Japanese greeting) in UTF‑8
        byte[] unicode = "こんにちは".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // All possible byte values 0‑255
        byte[] allBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            allBytes[i] = (byte) i;
        }
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(empty, "d41d8cd98f00b204e9800998ecf8427e"), org.junit.jupiter.params.provider.Arguments.of(hello, "5d41402abc4b2a76b9719d911017c592"), org.junit.jupiter.params.provider.Arguments.of(longAscii, "9e107d9d372bb6826bd81d3542a419d6"), org.junit.jupiter.params.provider.Arguments.of(unicode, "c3fcd3d76192e4007dfb496cca67e13b"), org.junit.jupiter.params.provider.Arguments.of(allBytes, "e2fc714c4727ee9395f324cd2e7f331f"));
    }

    /**
     * Test that the method returns a non‑null MessageDigest instance **
     */
    @Test
    void getMD5Digest_ShouldReturnNonNullDigest() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNotNull(md, "getMD5Digest() must not return null");
    }

    /**
     * Test that the returned MessageDigest is configured for the MD5 algorithm **
     */
    @Test
    void getMD5Digest_ShouldBeConfiguredForMD5() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertEquals("MD5", md.getAlgorithm(), "Digest algorithm should be MD5");
    }

    /**
     * Test that each invocation returns a distinct MessageDigest instance **
     */
    @Test
    void getMD5Digest_ShouldReturnNewInstanceEachCall() {
        MessageDigest md1 = EncryptionUtilities.getMD5Digest();
        MessageDigest md2 = EncryptionUtilities.getMD5Digest();
        assertNotSame(md1, md2, "Each call should return a new MessageDigest instance");
        // Ensure they behave identically
        assertEquals(md1.getAlgorithm(), md2.getAlgorithm());
    }

    /**
     * Test that the digest produced matches the well‑known MD5 hash for a sample input **
     */
    @Test
    void getMD5Digest_ShouldProduceCorrectHash() throws NoSuchAlgorithmException {
        // Sample data and its expected MD5 hex string
        byte[] data = "test".getBytes();
        String expectedHex = "098f6bcd4621d373cade4e832627b4f6";
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        byte[] actualBytes = md.digest(data);
        String actualHex = bytesToHex(actualBytes);
        assertEquals(expectedHex, actualHex, "MD5 hash of \"test\" does not match expected value");
    }

    /**
     * Verifies that {@link EncryptionUtilities#getMD5Digest()} correctly hashes
     * the supplied byte arrays.
     */
    @ParameterizedTest(name = "[{index}] MD5 hash matches expected")
    @MethodSource("md5HashProvider")
    void getMD5Digest_ShouldHashVariousInputsCorrectly(byte[] input, String expectedHex) {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        byte[] actual = md.digest(input);
        assertEquals(expectedHex, bytesToHex(actual), "MD5 hash does not match expected value for input: " + bytesToHex(input));
    }

    /**
     * Simulates an environment where the MD5 algorithm is not available.
     * We mock {@link EncryptionUtilities#getDigest(String)} to throw
     * {@link NoSuchAlgorithmException} and verify that the exception propagates.
     */
    @Test
    void getMD5Digest_ShouldPropagateNoSuchAlgorithmException_WhenAlgorithmMissing() {
        try (MockedStatic<EncryptionUtilities> mocked = mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS)) {
            // Force getDigest("MD5") to throw NoSuchAlgorithmException
            mocked.when(() -> EncryptionUtilities.getDigest("MD5")).thenThrow(new NoSuchAlgorithmException("MD5 not available"));
            // The call should now throw the same exception
            assertThrows(NoSuchAlgorithmException.class, EncryptionUtilities::getMD5Digest, "Expected NoSuchAlgorithmException when MD5 algorithm is unavailable");
        }
    }

    /**
     * Simulates a security manager denying access to cryptographic services.
     * We mock {@code getDigest} to throw {@link SecurityException}.
     */
    @Test
    void getMD5Digest_ShouldPropagateSecurityException_WhenAccessDenied() {
        try (MockedStatic<EncryptionUtilities> mocked = mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> EncryptionUtilities.getDigest("MD5")).thenThrow(new SecurityException("Access denied to MD5"));
            assertThrows(SecurityException.class, EncryptionUtilities::getMD5Digest, "Expected SecurityException when access to MD5 is denied");
        }
    }
}
