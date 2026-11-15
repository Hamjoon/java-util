package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.security.*;
import java.util.Objects;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getMD5Digest_Test {

    class DenyingSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(java.security.Permission perm) {
            // Allow everything except cryptographic algorithm access.
            if (perm instanceof java.security.SecurityPermission && perm.getName().contains("getAlgorithm")) {
                throw new SecurityException("Access to MD5 algorithm denied by test SecurityManager");
            }
            // otherwise allow
        }
    }

    class DenyingSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(java.security.Permission perm) {
            // Allow everything except cryptographic algorithm access.
            if (perm instanceof java.security.SecurityPermission && perm.getName().contains("getAlgorithm")) {
                throw new SecurityException("Access to MD5 algorithm denied by test SecurityManager");
            }
            // otherwise allow
        }
    }

    /**
     * Helper method to convert a byte array to a lower‑case hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return a hex string representation
     */
    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // Mask with 0xff to treat the byte as unsigned, then format as two hex digits.
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /**
     * Verify that {@code getMD5Digest()} returns a non‑null MessageDigest
     * whose algorithm name is exactly {@code MD5}.
     */
    @Test
    void testGetMD5DigestReturnsNonNullAndCorrectAlgorithm() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNotNull(md, "The MD5 MessageDigest should not be null.");
        assertEquals("MD5", md.getAlgorithm(), "The algorithm name should be MD5.");
    }

    /**
     * Verify that the digest produced by {@code getMD5Digest()} matches the
     * well‑known MD5 hash of the string {@code "abc"}.
     */
    @Test
    void testMD5DigestComputesExpectedHash() throws NoSuchAlgorithmException {
        // Arrange
        // ASCII bytes for "abc"
        byte[] input = "abc".getBytes();
        String expectedHex = "900150983cd24fb0d6963f7d28e17f72";
        // Act
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        byte[] actualBytes = md.digest(input);
        String actualHex = toHexString(actualBytes);
        // Assert
        assertEquals(expectedHex, actualHex, "MD5 hash of \"abc\" should match the expected value.");
    }

    /**
     * Verify that two separate calls return distinct objects (different references).
     */
    @Test
    void testDistinctInstanceReferences() {
        MessageDigest md1 = EncryptionUtilities.getMD5Digest();
        MessageDigest md2 = EncryptionUtilities.getMD5Digest();
        assertNotSame(md1, md2, "Each call should return a fresh MessageDigest instance.");
    }

    /**
     * Verify that two digests are independent – updating one does not affect the other.
     */
    @Test
    void testIndependentInstancesDoNotShareState() {
        MessageDigest mdA = EncryptionUtilities.getMD5Digest();
        MessageDigest mdB = EncryptionUtilities.getMD5Digest();
        mdA.update("a".getBytes());
        mdB.update("b".getBytes());
        String hashA = toHexString(mdA.digest());
        String hashB = toHexString(mdB.digest());
        assertNotEquals(hashA, hashB, "Digests updated with different data must produce different hashes.");
    }

    /**
     * Verify that a MessageDigest can be reused after {@code digest()} – it is automatically reset.
     */
    @Test
    void testReusableAfterDigest() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        byte[] data = "repeat".getBytes();
        // First computation
        md.update(data);
        String first = toHexString(md.digest());
        // Second computation (digest() should have reset the internal state)
        md.update(data);
        String second = toHexString(md.digest());
        assertEquals(first, second, "After calling digest(), the MessageDigest should be reset and produce the same hash on identical data.");
        assertEquals("5c6ffbdd40d9556b73a21e63c3e0e904", first, "Hash of \"repeat\" must match the known MD5 value.");
    }

    /**
     * Verify that cloning a MessageDigest yields an independent copy.
     */
    @Test
    void testCloneIndependence() throws CloneNotSupportedException {
        MessageDigest original = EncryptionUtilities.getMD5Digest();
        // The MD5 implementation supplied by SunJCE supports cloning.
        MessageDigest clone = (MessageDigest) original.clone();
        original.update("first".getBytes());
        clone.update("second".getBytes());
        String originalHash = toHexString(original.digest());
        String cloneHash = toHexString(clone.digest());
        assertNotEquals(originalHash, cloneHash, "Cloned MessageDigest must maintain independent internal state.");
    }

    /**
     * Verify that the method never throws a checked exception under normal JVM configurations.
     */
    @Test
    void testNoUnexpectedExceptions() {
        assertDoesNotThrow(() -> EncryptionUtilities.getMD5Digest(), "getMD5Digest() should not propagate any checked exceptions.");
    }

    /**
     * Simulate an environment where the MD5 algorithm is unavailable.
     * The method is expected to wrap the {@link NoSuchAlgorithmException} into an
     * {@link IllegalArgumentException}.
     */
    @Test
    void testAlgorithmNotAvailable() {
        try (MockedStatic<MessageDigest> mdMock = Mockito.mockStatic(MessageDigest.class)) {
            mdMock.when(() -> MessageDigest.getInstance(eq("MD5"))).thenThrow(new NoSuchAlgorithmException("MD5 not available"));
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, EncryptionUtilities::getMD5Digest, "When MD5 is unavailable, an IllegalArgumentException should be thrown.");
            assertTrue(thrown.getMessage().contains("MD5"), "Exception message should mention MD5.");
        }
    }

    /**
     * Install a restrictive {@link SecurityManager} that denies the use of cryptographic
     * algorithms.  The call to {@code getMD5Digest()} should propagate a {@link SecurityException}.
     */
    @Test
    void testSecurityManagerDeniesAccess() {
        // Save the original security manager to restore it after the test.
        SecurityManager original = System.getSecurityManager();
        class DenyingSecurityManager extends SecurityManager {

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Allow everything except cryptographic algorithm access.
                if (perm instanceof java.security.SecurityPermission && perm.getName().contains("getAlgorithm")) {
                    throw new SecurityException("Access to MD5 algorithm denied by test SecurityManager");
                }
                // otherwise allow
            }
        }
        try {
            System.setSecurityManager(new DenyingSecurityManager());
            SecurityException se = assertThrows(SecurityException.class, EncryptionUtilities::getMD5Digest, "A SecurityException should be thrown when the SecurityManager denies algorithm access.");
            assertTrue(se.getMessage().contains("MD5"), "Exception message should indicate MD5 was denied.");
        } finally {
            // Restore the original security manager to avoid side‑effects on other tests.
            System.setSecurityManager(original);
        }
    }

    /**
     * Parameterized test for a few well‑known MD5 vectors.
     *
     * @param input   the plain‑text string
     * @param expectedHex the expected MD5 hash in lower‑case hex
     */
    @ParameterizedTest(name = "MD5 of \"{0}\"")
    @CsvSource({ "'hello', 5d41402abc4b2a76b9719d911017c592", "'world', 7d793037a0760186574b0282f2f435e7", "'', d41d8cd98f00b204e9800998ecf8427e" })
    void testKnownHashes(String input, String expectedHex) {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        byte[] actual = md.digest(input.getBytes());
        assertEquals(expectedHex, toHexString(actual), "MD5 hash did not match expected value for input: " + input);
    }
}
