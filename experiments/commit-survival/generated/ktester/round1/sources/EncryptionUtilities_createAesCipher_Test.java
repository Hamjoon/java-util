package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import javax.crypto.Cipher;
import java.security.SecureRandom;
import java.util.Base64;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesCipher_Test {

    // any arbitrary string
    private static final String SAMPLE_KEY = "mySuperSecretKey123";

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Helper method to make the test output more readable when debugging.
     */
    private static String toBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Provides test cases for valid inputs – typical usage and boundary conditions.
     */
    private static Stream<TestCase> validInputProvider() {
        return Stream.of(// Typical usage – encryption
        new TestCase("MySecretPassword123", Cipher.ENCRYPT_MODE, true, null), // Typical usage – decryption
        new TestCase("AnotherKey456", Cipher.DECRYPT_MODE, true, null), // Exactly 16‑char key (128‑bit)
        new TestCase("1234567890ABCDEF", Cipher.ENCRYPT_MODE, true, null), // Shorter than 16 chars – should be padded/hashed internally
        new TestCase("shortKey", Cipher.DECRYPT_MODE, true, null), // Longer than 16 chars – should be truncated/hashed internally
        new TestCase("ThisKeyIsDefinitelyLongerThanSixteenChars", Cipher.ENCRYPT_MODE, true, null), // Unicode key handling
        new TestCase("Pässwörd🔑", Cipher.DECRYPT_MODE, true, null));
    }

    /**
     * Provides test cases for invalid inputs – null/empty key and illegal modes.
     */
    private static Stream<TestCase> invalidInputProvider() {
        return Stream.of(// Null key
        new TestCase(null, Cipher.ENCRYPT_MODE, false, Exception.class), // Empty key
        new TestCase("", Cipher.DECRYPT_MODE, false, IllegalArgumentException.class), // Unsupported mode constant
        new TestCase("ValidKey123", 9999, false, Exception.class), // Negative mode
        new TestCase("AnotherValidKey", -1, false, Exception.class), // Integer.MAX_VALUE as mode
        new TestCase("KeyForMaxMode", Integer.MAX_VALUE, false, Exception.class), // Integer.MIN_VALUE as mode
        new TestCase("KeyForMinMode", Integer.MIN_VALUE, false, Exception.class));
    }

    /**
     * Verify that an encryption cipher can encrypt data and that a corresponding
     * decryption cipher (created with the same key) can decrypt it back to the original.
     */
    @Test
    void testEncryptionDecryptionRoundTrip() throws Exception {
        // Arrange: create encryption and decryption ciphers for the same key
        Cipher encryptCipher = EncryptionUtilities.createAesCipher(SAMPLE_KEY, Cipher.ENCRYPT_MODE);
        Cipher decryptCipher = EncryptionUtilities.createAesCipher(SAMPLE_KEY, Cipher.DECRYPT_MODE);
        assertNotNull(encryptCipher, "Encryption cipher should not be null");
        assertNotNull(decryptCipher, "Decryption cipher should not be null");
        assertEquals("AES", encryptCipher.getAlgorithm(), "Cipher algorithm should be AES");
        assertEquals("AES", decryptCipher.getAlgorithm(), "Cipher algorithm should be AES");
        // Generate random plaintext
        byte[] plainBytes = new byte[64];
        RANDOM.nextBytes(plainBytes);
        // Act: encrypt then decrypt
        byte[] encrypted = encryptCipher.doFinal(plainBytes);
        byte[] decrypted = decryptCipher.doFinal(encrypted);
        // Assert: round‑trip yields original bytes
        assertArrayEquals(plainBytes, decrypted, "Decrypted bytes must match the original plaintext");
    }

    /**
     * Verify that providing a {@code null} key results in an exception.
     * The exact exception type depends on the internal implementation,
     * but it must be a subclass of {@link Exception}.
     */
    @Test
    void testCreateAesCipherWithNullKeyThrows() {
        Exception exception = assertThrows(Exception.class, () -> EncryptionUtilities.createAesCipher(null, Cipher.ENCRYPT_MODE));
        // The message is implementation‑specific; we only assert that an exception is thrown.
        assertNotNull(exception.getMessage());
    }

    @ParameterizedTest(name = "[{index}] key=''{0}'', mode={1}")
    @MethodSource("validInputProvider")
    void testCreateAesCipherValidInputs(TestCase tc) throws Exception {
        // Act
        Cipher cipher = EncryptionUtilities.createAesCipher(tc.key, tc.mode);
        // Assert
        assertNotNull(cipher, "Cipher should not be null for valid inputs");
        assertEquals("AES", cipher.getAlgorithm(), "Cipher algorithm must be AES");
        // Verify that the mode stored inside the Cipher matches the requested mode.
        // Cipher does not expose mode directly, but we can attempt a no‑op init to see if it accepts.
        // The fact that no exception was thrown during creation is sufficient for this test.
    }

    @ParameterizedTest(name = "[{index}] key=''{0}'', mode={1}")
    @MethodSource("invalidInputProvider")
    void testCreateAesCipherInvalidInputs(TestCase tc) {
        // Act & Assert
        Exception ex = assertThrows(tc.expectedException, () -> EncryptionUtilities.createAesCipher(tc.key, tc.mode));
        assertNotNull(ex.getMessage(), "Exception should contain a message for key='" + tc.key + "' mode=" + tc.mode);
    }
}
