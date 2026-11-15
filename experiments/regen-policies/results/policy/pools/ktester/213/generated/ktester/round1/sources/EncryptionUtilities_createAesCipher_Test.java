package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.crypto.Cipher;
import java.security.Key;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesCipher_Test {

    private static final String SAMPLE_KEY = "mySecretPassword";

    private static final String PLAINTEXT = "The quick brown fox jumps over the lazy dog";

    private static Stream<Arguments> validKeyModeProvider() {
        return Stream.of(Arguments.of("0123456789abcdef", Cipher.ENCRYPT_MODE), Arguments.of("0123456789abcdef", Cipher.DECRYPT_MODE), // 15‑character key – will be padded/hashed internally
        Arguments.of("mySecretKey12345", Cipher.ENCRYPT_MODE), Arguments.of("mySecretKey12345", Cipher.DECRYPT_MODE));
    }

    /**
     * Verify that {@code createAesCipher(String, int)} returns a functional Cipher that can encrypt
     * data and that a Cipher created for decryption with the same key can correctly decrypt the data.
     */
    @Test
    void testCreateAesCipherEncryptionDecryptionRoundTrip() throws Exception {
        // Create encryption and decryption ciphers using the public helper methods
        Cipher encryptCipher = EncryptionUtilities.createAesEncryptionCipher(SAMPLE_KEY);
        Cipher decryptCipher = EncryptionUtilities.createAesDecryptionCipher(SAMPLE_KEY);
        assertNotNull(encryptCipher, "Encryption cipher should not be null");
        assertNotNull(decryptCipher, "Decryption cipher should not be null");
        // Ensure the algorithm is AES (the implementation uses "AES")
        assertEquals("AES", encryptCipher.getAlgorithm(), "Encryption cipher algorithm should be AES");
        assertEquals("AES", decryptCipher.getAlgorithm(), "Decryption cipher algorithm should be AES");
        // Perform encryption
        byte[] plainBytes = PLAINTEXT.getBytes();
        byte[] encryptedBytes = encryptCipher.doFinal(plainBytes);
        assertNotNull(encryptedBytes, "Encrypted bytes should not be null");
        assertFalse(java.util.Arrays.equals(plainBytes, encryptedBytes), "Encrypted data must differ from the original plaintext");
        // Perform decryption
        byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);
        assertNotNull(decryptedBytes, "Decrypted bytes should not be null");
        assertArrayEquals(plainBytes, decryptedBytes, "Decrypted data must match the original plaintext");
    }

    /**
     * Verify that an unsupported mode results in an exception.
     */
    @Test
    void testCreateAesCipherWithInvalidModeThrows() {
        // Not Cipher.ENCRYPT_MODE nor Cipher.DECRYPT_MODE
        int invalidMode = 999;
        Exception exception = assertThrows(Exception.class, () -> EncryptionUtilities.createAesCipher(SAMPLE_KEY, invalidMode));
        // The exact exception type may vary (e.g., IllegalArgumentException, InvalidKeyException, etc.)
        // We only assert that an exception is thrown.
        assertNotNull(exception, "Exception should be thrown for an invalid mode");
    }

    @Test
    void testCreateAesCipher_NullKey_ThrowsException() {
        Exception ex = assertThrows(Exception.class, () -> EncryptionUtilities.createAesCipher(null, Cipher.ENCRYPT_MODE));
        assertNotNull(ex, "An exception should be thrown when key is null");
    }

    @Test
    void testCreateAesCipher_EmptyKey_ThrowsException() {
        Exception ex = assertThrows(Exception.class, () -> EncryptionUtilities.createAesCipher("", Cipher.ENCRYPT_MODE));
        assertNotNull(ex, "An exception should be thrown for empty key");
    }

    @Test
    void testCreateAesCipher_ShortKey_ThrowsException() {
        // "short" is 5 characters – insufficient for 128‑bit material
        Exception ex = assertThrows(Exception.class, () -> EncryptionUtilities.createAesCipher("short", Cipher.ENCRYPT_MODE));
        assertNotNull(ex, "An exception should be thrown for a short key");
    }

    @Test
    void testCreateAesCipher_LongKey_Succeeds() throws Exception {
        // 32‑character key – implementation should truncate / hash to 128‑bit
        String longKey = "0123456789abcdef0123456789abcdef";
        Cipher cipher = EncryptionUtilities.createAesCipher(longKey, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher, "Cipher should be created for a long key");
        assertEquals("AES", cipher.getAlgorithm(), "Algorithm must be AES");
    }

    @Test
    void testCreateAesCipher_InvalidMode_Negative_ThrowsException() {
        Exception ex = assertThrows(Exception.class, () -> EncryptionUtilities.createAesCipher("0123456789abcdef", -1));
        assertNotNull(ex, "Exception expected for negative mode value");
    }

    @Test
    void testCreateAesCipher_InvalidMode_Arbitrary_ThrowsException() {
        Exception ex = assertThrows(Exception.class, () -> EncryptionUtilities.createAesCipher("0123456789abcdef", 12345));
        assertNotNull(ex, "Exception expected for arbitrary invalid mode value");
    }

    /**
     * Parameterized test for valid key/mode combinations.
     * Verifies that a Cipher instance is returned and can be used for encryption
     * (or decryption) without throwing.
     */
    @ParameterizedTest
    @MethodSource("validKeyModeProvider")
    void testCreateAesCipher_ValidKeyAndMode_ReturnsUsableCipher(String key, int mode) throws Exception {
        Cipher cipher = EncryptionUtilities.createAesCipher(key, mode);
        assertNotNull(cipher, "Cipher must not be null for valid inputs");
        assertEquals("AES", cipher.getAlgorithm(), "Cipher algorithm should be AES");
        // Simple functional check: encrypt if mode is ENCRYPT_MODE, otherwise decrypt a known payload.
        byte[] payload = "test".getBytes();
        if (mode == Cipher.ENCRYPT_MODE) {
            byte[] encrypted = cipher.doFinal(payload);
            assertNotNull(encrypted, "Encryption should produce a non‑null byte array");
            assertFalse(java.util.Arrays.equals(payload, encrypted), "Ciphertext must differ from plaintext");
        } else if (mode == Cipher.DECRYPT_MODE) {
            // To test decryption we need a matching encryption cipher first.
            Cipher encryptCipher = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
            byte[] encrypted = encryptCipher.doFinal(payload);
            byte[] decrypted = cipher.doFinal(encrypted);
            assertArrayEquals(payload, decrypted, "Decrypted bytes must match original plaintext");
        }
    }

    /**
     * Full round‑trip test using {@code createAesCipher(String,int)} directly.
     * Demonstrates that an encryption cipher and a decryption cipher created with the same key
     * are compatible.
     */
    @Test
    void testCreateAesCipher_RoundTrip_UsingDirectMethod() throws Exception {
        String key = "mySecretKey12345";
        String message = "HelloWorld";
        Cipher encryptCipher = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
        Cipher decryptCipher = EncryptionUtilities.createAesCipher(key, Cipher.DECRYPT_MODE);
        assertNotNull(encryptCipher);
        assertNotNull(decryptCipher);
        assertEquals("AES", encryptCipher.getAlgorithm());
        assertEquals("AES", decryptCipher.getAlgorithm());
        byte[] encrypted = encryptCipher.doFinal(message.getBytes());
        assertNotNull(encrypted);
        assertFalse(java.util.Arrays.equals(message.getBytes(), encrypted));
        byte[] decrypted = decryptCipher.doFinal(encrypted);
        assertArrayEquals(message.getBytes(), decrypted, "Decrypted bytes must equal original message");
    }
}
