package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.crypto.Cipher;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesDecryptionCipher_Test {

    /**
     * A simple key used for the tests.
     */
    private static final String TEST_KEY = "TestPassword123";

    /**
     * Sample plaintext used for encryption/decryption round‑trip.
     */
    private static final String PLAINTEXT = "The quick brown fox jumps over the lazy dog";

    /**
     * Retrieves the internal {@code opmode} field of a {@link Cipher} instance via
     * reflection. The field holds the mode the cipher was initialized with
     * (e.g., {@link Cipher#DECRYPT_MODE} or {@link Cipher#ENCRYPT_MODE}).
     *
     * @param cipher the cipher whose mode should be inspected
     * @return the integer value of the mode
     */
    private static int getCipherMode(Cipher cipher) {
        try {
            Field modeField = Cipher.class.getDeclaredField("opmode");
            modeField.setAccessible(true);
            return modeField.getInt(cipher);
        } catch (Exception e) {
            throw new AssertionError("Unable to access Cipher mode via reflection", e);
        }
    }

    /**
     * Provides a stream of valid hexadecimal AES keys (128‑, 192‑ and 256‑bit).
     */
    private static Stream<String> validHexKeys() {
        return Stream.of(// 128‑bit (16 bytes) → 32 hex chars
        "00112233445566778899aabbccddeeff", // 192‑bit (24 bytes) → 48 hex chars
        "00112233445566778899aabbccddeeff0011223344556677", // 256‑bit (32 bytes) → 64 hex chars
        "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff");
    }

    /**
     * Provides a stream of invalid key specifications that must trigger an
     * {@link IllegalArgumentException}.
     */
    private static Stream<String> invalidKeys() {
        return Stream.of(null, // empty
        "", // non‑hex chars
        "ZZ112233445566778899aabbccddeeff", // mixed invalid chars
        "00112233GG5566778899aabbccddeeff", // odd length (31 chars)
        "00112233445566778899aabbccddeef", // odd length (1 char)
        "0", // too short (4 bytes)
        "00112233", // 33 bytes
        "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011", // leading whitespace
        " 00112233445566778899aabbccddeeff", // 0x prefix
        "0x00112233445566778899aabbccddeeff", // internal spaces
        "00112233 44556677 8899aabb ccddeeff", // newline inside
        "00112233\n445566778899aabbccddeeff");
    }

    /**
     * Verifies that {@code createAesDecryptionCipher(String)} returns a Cipher
     * configured for decryption and that it can correctly decrypt data that was
     * encrypted with the matching encryption cipher.
     *
     * The test performs a full round‑trip:
     * <pre>
     *   encryptedHex = EncryptionUtilities.encrypt(TEST_KEY, PLAINTEXT);
     *   decrypted    = EncryptionUtilities.decrypt(TEST_KEY, encryptedHex);
     * </pre>
     * and asserts that the decrypted text matches the original plaintext.
     *
     * It also checks that the returned Cipher instance is in {@code DECRYPT_MODE}.
     */
    @Test
    void shouldCreateAesDecryptionCipherAndDecryptCorrectly() throws Exception {
        // GIVEN – a known key and plaintext (constants defined above)
        // WHEN – encrypt the plaintext using the public API
        String encryptedHex = EncryptionUtilities.encrypt(TEST_KEY, PLAINTEXT);
        // AND – obtain a decryption cipher directly via the method under test
        Cipher decryptionCipher = EncryptionUtilities.createAesDecryptionCipher(TEST_KEY);
        // THEN – the cipher must be configured for decryption
        int mode = getCipherMode(decryptionCipher);
        assertEquals(Cipher.DECRYPT_MODE, mode, "Cipher should be initialized in DECRYPT_MODE");
        // AND – using the cipher directly should produce the original plaintext
        byte[] decryptedBytes = decryptionCipher.doFinal(ByteUtilities.decode(encryptedHex));
        String decrypted = new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(PLAINTEXT, decrypted, "Decrypted text must match the original plaintext");
        // ALSO – the convenience decrypt method must yield the same result
        String decryptedViaConvenience = EncryptionUtilities.decrypt(TEST_KEY, encryptedHex);
        assertEquals(PLAINTEXT, decryptedViaConvenience, "Convenience decrypt method must return the original plaintext");
    }

    @ParameterizedTest(name = "Valid key [{index}]: {0}")
    @MethodSource("validHexKeys")
    void shouldCreateDecryptionCipherForValidHexKey(String hexKey) throws Exception {
        // WHEN
        Cipher cipher = EncryptionUtilities.createAesDecryptionCipher(hexKey);
        // THEN – basic non‑null checks
        assertNotNull(cipher, "Cipher must not be null for a valid key");
        assertEquals("AES", cipher.getAlgorithm(), "Cipher algorithm should be AES");
        // THEN – verify the cipher is in DECRYPT_MODE (via reflection)
        int mode = getCipherMode(cipher);
        assertEquals(Cipher.DECRYPT_MODE, mode, "Cipher should be initialized in DECRYPT_MODE for key: " + hexKey);
    }

    @ParameterizedTest(name = "Invalid key [{index}]: \"{0}\"")
    @MethodSource("invalidKeys")
    void shouldThrowIllegalArgumentExceptionForInvalidKey(String key) {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.createAesDecryptionCipher(key), "Expected IllegalArgumentException for key: " + String.valueOf(key));
    }
}
