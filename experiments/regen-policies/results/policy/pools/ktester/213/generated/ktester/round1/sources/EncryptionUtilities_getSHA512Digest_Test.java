package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getSHA512Digest_Test {

    /**
     * Converts a byte array to a lower‑case hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return hexadecimal representation
     */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    @Test
    @DisplayName("getSHA512Digest returns a SHA‑512 MessageDigest")
    void testGetSHA512DigestProperties() throws NoSuchAlgorithmException {
        // Act
        MessageDigest digest = EncryptionUtilities.getSHA512Digest();
        // Assert
        assertNotNull(digest, "The returned MessageDigest should not be null");
        assertEquals("SHA-512", digest.getAlgorithm(), "The MessageDigest algorithm should be SHA-512");
        // SHA‑512 produces a 512‑bit (64‑byte) hash
        assertEquals(64, digest.getDigestLength(), "SHA-512 digest length should be 64 bytes");
    }

    @Test
    @DisplayName("Multiple calls to getSHA512Digest produce independent instances")
    void testMultipleCallsProduceIndependentInstances() {
        // Act
        MessageDigest first = EncryptionUtilities.getSHA512Digest();
        MessageDigest second = EncryptionUtilities.getSHA512Digest();
        // Assert
        assertNotSame(first, second, "Each call should return a new MessageDigest instance");
        // Both must still be correctly configured
        assertEquals("SHA-512", first.getAlgorithm());
        assertEquals("SHA-512", second.getAlgorithm());
    }

    /**
     * Compute the SHA‑512 hash of the UTF‑8 bytes of the string "abc"
     * and compare it to the well‑known reference value.
     */
    @Test
    @DisplayName("SHA‑512 hash of \"abc\" matches known digest")
    void testHashOfKnownString() {
        // Arrange
        MessageDigest digest = EncryptionUtilities.getSHA512Digest();
        byte[] input = "abc".getBytes(StandardCharsets.UTF_8);
        String expectedHex = "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f";
        // Act
        byte[] hashBytes = digest.digest(input);
        String actualHex = toHex(hashBytes);
        // Assert
        assertEquals(expectedHex, actualHex, "SHA‑512 hash of \"abc\" should match the reference value");
    }

    /**
     * Simulate an environment where the SHA‑512 algorithm is unavailable.
     * This is achieved by mocking the static {@code getDigest(String)} method
     * to throw a {@link NoSuchAlgorithmException}, which {@code getSHA512Digest}
     * should translate into an {@link IllegalArgumentException}.
     */
    @Test
    @DisplayName("getSHA512Digest propagates exception when algorithm is missing")
    void testExceptionWhenAlgorithmUnavailable() {
        // Mock the static method getDigest to throw NoSuchAlgorithmException
        try (MockedStatic<EncryptionUtilities> mockedStatic = mockStatic(EncryptionUtilities.class)) {
            mockedStatic.when(() -> EncryptionUtilities.getDigest("SHA-512")).thenThrow(new NoSuchAlgorithmException("SHA-512 not available"));
            // Act & Assert
            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, EncryptionUtilities::getSHA512Digest, "Expected getSHA512Digest() to throw IllegalArgumentException when SHA-512 is unavailable");
            assertTrue(thrown.getCause() instanceof NoSuchAlgorithmException, "Cause of the IllegalArgumentException should be NoSuchAlgorithmException");
        }
    }
}
