package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getSHA256Digest_Test {

    /**
     * Helper that converts a byte array to a lower‑case hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return a hex representation (two characters per byte)
     */
    private static String toHexString(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }

    /**
     * Verify that the method returns a non‑null MessageDigest whose algorithm is "SHA-256".
     */
    @Test
    void testGetSHA256Digest_NotNullAndCorrectAlgorithm() {
        MessageDigest md = EncryptionUtilities.getSHA256Digest();
        assertNotNull(md, "The SHA‑256 MessageDigest should not be null");
        assertEquals("SHA-256", md.getAlgorithm(), "The MessageDigest algorithm should be SHA-256");
    }

    /**
     * Verify that the digest produces the correct SHA‑256 hash for a known input.
     *
     * The expected hash for the UTF‑8 bytes of the string "hello" is:
     * {@code 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824}
     */
    @Test
    void testGetSHA256Digest_ProducesCorrectHash() throws NoSuchAlgorithmException {
        // Arrange
        byte[] input = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String expectedHex = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        // Act
        MessageDigest md = EncryptionUtilities.getSHA256Digest();
        byte[] actualBytes = md.digest(input);
        String actualHex = toHexString(actualBytes);
        // Assert
        assertEquals(expectedHex, actualHex, "SHA‑256 hash of \"hello\" does not match the expected value");
    }

    /**
     * Simulate a situation where {@link EncryptionUtilities#getDigest(String)} throws a
     * {@link NoSuchAlgorithmException}. The wrapper method should propagate this as an unchecked
     * exception (typically a {@link RuntimeException}).
     */
    @Test
    void testGetSHA256Digest_WhenGetDigestThrowsNoSuchAlgorithmException_ThrowsRuntimeException() {
        // Mock the static method getDigest to throw NoSuchAlgorithmException
        try (MockedStatic<EncryptionUtilities> mockedStatic = Mockito.mockStatic(EncryptionUtilities.class)) {
            mockedStatic.when(() -> EncryptionUtilities.getDigest("SHA-256")).thenThrow(new NoSuchAlgorithmException("SHA-256 not available"));
            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, EncryptionUtilities::getSHA256Digest, "Expected a RuntimeException when underlying getDigest throws NoSuchAlgorithmException");
            assertTrue(ex.getCause() instanceof NoSuchAlgorithmException, "The cause of the RuntimeException should be NoSuchAlgorithmException");
        }
    }

    /**
     * Simulate a security manager that denies access to the SHA‑256 algorithm by throwing a
     * {@link SecurityException} from {@code getDigest}. The wrapper should again propagate an
     * unchecked exception.
     */
    @Test
    void testGetSHA256Digest_WhenGetDigestThrowsSecurityException_ThrowsRuntimeException() {
        // Mock the static method getDigest to throw SecurityException
        try (MockedStatic<EncryptionUtilities> mockedStatic = Mockito.mockStatic(EncryptionUtilities.class)) {
            mockedStatic.when(() -> EncryptionUtilities.getDigest("SHA-256")).thenThrow(new SecurityException("Access denied to SHA-256"));
            // Act & Assert
            RuntimeException ex = assertThrows(RuntimeException.class, EncryptionUtilities::getSHA256Digest, "Expected a RuntimeException when underlying getDigest throws SecurityException");
            assertTrue(ex.getCause() instanceof SecurityException, "The cause of the RuntimeException should be SecurityException");
        }
    }
}
