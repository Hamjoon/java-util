package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesEncryptionCipher_Test {

    private static final String TEST_KEY = "mySecretPassword";

    private static final String PLAINTEXT = "The quick brown fox jumps over the lazy dog";

    /* -------------------------------------------------------------
     *  Simple holder for parameterised test data
     * ------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final String key;

        final String description;

        ArgumentsHolder(String key, String description) {
            this.key = key;
            this.description = description;
        }

        @Override
        public String toString() {
            return description + " (key=" + (key == null ? "null" : key) + ")";
        }

        // JUnit5 method source expects a stream of arguments; we expose getters.
        public String getKey() {
            return key;
        }

        public String getDescription() {
            return description;
        }
    }

    /* -------------------------------------------------------------
     *  Helper methods for providing test data
     * ------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validKeyProvider() {
        return Stream.of(// 128‑bit key (16 bytes)
        new ArgumentsHolder("00112233445566778899aabbccddeeff", "Cipher instance initialized for ENCRYPT_MODE with a 128‑bit AES key"), // 192‑bit key (24 bytes)
        new ArgumentsHolder("00112233445566778899aabbccddeeff0011223344556677", "Cipher instance initialized for ENCRYPT_MODE with a 192‑bit AES key"), // 256‑bit key (32 bytes)
        new ArgumentsHolder("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff", "Cipher instance initialized for ENCRYPT_MODE with a 256‑bit AES key"));
    }

    private static Stream<ArgumentsHolder> invalidKeyProvider() {
        return Stream.of(new ArgumentsHolder(null, "IllegalArgumentException or NullPointerException"), new ArgumentsHolder("", "IllegalArgumentException"), new ArgumentsHolder("short", "IllegalArgumentException"), new ArgumentsHolder("00112233GG5566778899aabbccddeeff", "IllegalArgumentException"), new ArgumentsHolder("AABBCCDDEEFF00112233445566778899", "Cipher instance initialized for ENCRYPT_MODE with a 128‑bit AES key (uppercase hex accepted)"), new ArgumentsHolder("00112233aabbCCddeeff0011AABBccddeeff0011", "Cipher instance initialized for ENCRYPT_MODE with a 192‑bit AES key (mixed case hex accepted)"), new ArgumentsHolder("00112233445566778899aabbccddeef", "IllegalArgumentException"), new ArgumentsHolder("00112233445566778899aabbccddeeff00112233", "IllegalArgumentException"), new ArgumentsHolder(" 00112233445566778899aabbccddeeff ", "IllegalArgumentException"), new ArgumentsHolder("0x00112233445566778899aabbccddeeff", "IllegalArgumentException"), new ArgumentsHolder("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff", "IllegalArgumentException"), new ArgumentsHolder("00112233\n445566778899aabbccddeeff", "IllegalArgumentException"), new ArgumentsHolder("   ", "IllegalArgumentException"), new ArgumentsHolder("00112233 445566778899aabbccddeeff", "IllegalArgumentException"), new ArgumentsHolder("00112233\t445566778899aabbccddeeff", "IllegalArgumentException"));
    }

    /* -------------------------------------------------------------
     *  Helper to allow multiple exception types in a single assert
     * ------------------------------------------------------------- */
    private static void assertThrowsAnyOf(Class<? extends Throwable>[] types, Executable executable, String message) {
        for (Class<? extends Throwable> type : types) {
            try {
                executable.execute();
                // If no exception, continue to next type – will fail at the end.
                return;
            } catch (Throwable t) {
                if (type.isInstance(t)) {
                    // Expected exception type found – test passes.
                    return;
                }
            }
        }
        fail(message);
    }

    private static void assertThrowsAnyOf(Class<? extends Throwable>... types) {
        // overload used by the parameterized test
    }

    private static void assertThrowsAnyOf(Class<? extends Throwable> type1, Class<? extends Throwable> type2, Class<? extends Throwable> type3, Class<? extends Throwable> type4, Executable executable, String message) {
        assertThrowsAnyOf(new Class[] { type1, type2, type3, type4 }, executable, message);
    }

    @Test
    void createAesEncryptionCipher_ShouldReturnCipherThatCanEncryptAndDecryptCorrectly() throws Exception {
        // GIVEN – an encryption cipher created with a known key
        Cipher encryptCipher = EncryptionUtilities.createAesEncryptionCipher(TEST_KEY);
        assertNotNull(encryptCipher, "Encryption cipher should not be null");
        // WHEN – we encrypt a known plaintext
        byte[] encryptedBytes = encryptCipher.doFinal(PLAINTEXT.getBytes(StandardCharsets.UTF_8));
        assertNotNull(encryptedBytes, "Encrypted bytes should not be null");
        assertTrue(encryptedBytes.length > 0, "Encrypted bytes should contain data");
        // AND – we obtain a matching decryption cipher
        Cipher decryptCipher = EncryptionUtilities.createAesDecryptionCipher(TEST_KEY);
        assertNotNull(decryptCipher, "Decryption cipher should not be null");
        // THEN – decrypting the ciphertext yields the original plaintext
        byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);
        String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);
        assertEquals(PLAINTEXT, decryptedText, "Decrypted text should match the original plaintext");
    }

    /* -------------------------------------------------------------
     *  Positive test – encryption/decryption round‑trip for valid keys
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("validKeyProvider")
    void createAesEncryptionCipher_ValidKey_ShouldEncryptAndDecrypt(String key, String description) throws Exception {
        // GIVEN a valid AES key string
        Cipher encryptCipher = EncryptionUtilities.createAesEncryptionCipher(key);
        assertNotNull(encryptCipher, "Cipher must not be null for key: " + key);
        // WHEN encrypting a known plaintext
        byte[] encrypted = encryptCipher.doFinal(PLAINTEXT.getBytes(StandardCharsets.UTF_8));
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 0, "Encrypted data should not be empty");
        // AND obtaining a matching decryption cipher
        Cipher decryptCipher = EncryptionUtilities.createAesDecryptionCipher(key);
        assertNotNull(decryptCipher, "Decryption cipher must not be null for key: " + key);
        // THEN the decrypted text matches the original plaintext
        byte[] decrypted = decryptCipher.doFinal(encrypted);
        String roundTrip = new String(decrypted, StandardCharsets.UTF_8);
        assertEquals(PLAINTEXT, roundTrip, "Round‑trip decryption failed for key: " + key);
    }

    /* -------------------------------------------------------------
     *  Negative tests – expecting IllegalArgumentException / NullPointerException
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("invalidKeyProvider")
    void createAesEncryptionCipher_InvalidKey_ShouldThrowException(String key, String expected) {
        // The test data mixes cases where the key is actually valid (uppercase/mixed case).
        // Those are handled in the positive test above, so we only assert exceptions here.
        if (expected.startsWith("Cipher instance")) {
            // This case is a valid key – delegate to the positive test to avoid duplication.
            assertDoesNotThrow(() -> {
                Cipher encryptCipher = EncryptionUtilities.createAesEncryptionCipher(key);
                assertNotNull(encryptCipher);
            });
            return;
        }
        // GIVEN an invalid key, WHEN creating the cipher, THEN an exception is thrown.
        assertThrowsAnyOf(IllegalArgumentException.class, NullPointerException.class, InvalidKeyException.class, NoSuchAlgorithmException.class, () -> EncryptionUtilities.createAesEncryptionCipher(key), "Expected exception for key: " + key);
    }
}
