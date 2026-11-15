package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.crypto.Cipher;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesEncryptionCipher_Test {

    /**
     * Sample key used for all tests – it can be any non‑empty string.
     */
    private static final String SAMPLE_KEY = "TestKey123";

    /**
     * Sample plain‑text used for encryption/decryption round‑trip tests.
     */
    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog";

    /**
     * Supplies valid hexadecimal key strings of supported lengths (16, 24, 32 bytes).
     */
    private static Stream<Arguments> validKeyProvider() {
        return Stream.of(// 128‑bit (32 hex chars)
        Arguments.of("00112233445566778899aabbccddeeff"), // 192‑bit (48 hex chars)
        Arguments.of("00112233445566778899aabbccddeeff0011223344556677"), // 256‑bit (64 hex chars)
        Arguments.of("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff"), // Upper‑case hex accepted (128‑bit)
        Arguments.of("00112233445566778899AABBCCDDEEFF"));
    }

    /**
     * Supplies every malformed key scenario described in the JSON test cases.
     */
    private static Stream<Arguments> invalidKeyProvider() {
        return Stream.of(// Null key
        Arguments.of((String) null), // Empty string
        Arguments.of(""), // Non‑hex characters
        Arguments.of("GHIJKL1234567890"), // Odd number of hex digits
        Arguments.of("0011223344556677A"), // Too short (<128‑bit, e.g., 4 bytes)
        Arguments.of("00112233"), // Length not matching 16/24/32 bytes (20 bytes)
        Arguments.of("00112233445566778899aabbccddeeff00112233"), // Length exceeds 32 bytes (33 bytes)
        Arguments.of("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff0011"), // Leading/trailing whitespace
        Arguments.of(" 00112233445566778899aabbccddeeff "), // Prefixed with 0x
        Arguments.of("0x00112233445566778899aabbccddeeff"), // Newline character at the end
        Arguments.of("00112233445566778899aabbccddeeff\n"), // Internal space
        Arguments.of("00112233 445566778899aabbccddeeff"), // Internal tab
        Arguments.of("00112233\t445566778899aabbccddeeff"), // Non‑ASCII Unicode character
        Arguments.of("00112233é445566778899aabbccddeeff"));
    }

    /**
     * Verifies that {@code createAesEncryptionCipher(String)} returns a Cipher
     * configured for AES encryption in CBC mode with PKCS5 padding and that the
     * returned Cipher can successfully encrypt data that can later be decrypted
     * with the matching decryption Cipher.
     */
    @Test
    void createAesEncryptionCipher_ShouldReturnEncryptModeCipher_AndRoundTripWorks() throws Exception {
        // GIVEN – a key string (already defined as SAMPLE_KEY)
        // WHEN – we obtain an encryption Cipher from the utility method
        Cipher encryptCipher = EncryptionUtilities.createAesEncryptionCipher(SAMPLE_KEY);
        // THEN – the Cipher must be non‑null and be in ENCRYPT_MODE
        assertNotNull(encryptCipher, "Cipher should not be null");
        assertEquals(Cipher.ENCRYPT_MODE, encryptCipher.getMode(), "Cipher should be initialised for encryption");
        // AND – encrypt some known plaintext
        byte[] encryptedBytes = encryptCipher.doFinal(SAMPLE_TEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertNotNull(encryptedBytes, "Encrypted bytes should not be null");
        assertTrue(encryptedBytes.length > 0, "Encrypted output should have content");
        // WHEN – we obtain a matching decryption Cipher
        Cipher decryptCipher = EncryptionUtilities.createAesDecryptionCipher(SAMPLE_KEY);
        assertNotNull(decryptCipher, "Decryption Cipher should not be null");
        assertEquals(Cipher.DECRYPT_MODE, decryptCipher.getMode(), "Decryption Cipher should be initialised for decryption");
        // THEN – decrypt the previously encrypted data and verify the original text is recovered
        byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);
        String decryptedText = new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(SAMPLE_TEXT, decryptedText, "Decrypted text must match the original plaintext");
    }

    /**
     * Ensures that the same key always produces a Cipher that can encrypt and
     * decrypt the same data, i.e., the IV generation is deterministic as described
     * in the class Javadoc.
     */
    @Test
    void createAesEncryptionCipher_DeterministicIv_WithSameKey() throws Exception {
        // GIVEN – the same key used twice
        Cipher firstCipher = EncryptionUtilities.createAesEncryptionCipher(SAMPLE_KEY);
        Cipher secondCipher = EncryptionUtilities.createAesEncryptionCipher(SAMPLE_KEY);
        // WHEN – we retrieve the IV parameters from both Ciphers
        byte[] iv1 = firstCipher.getParameters().getParameterSpec(javax.crypto.spec.IvParameterSpec.class).getIV();
        byte[] iv2 = secondCipher.getParameters().getParameterSpec(javax.crypto.spec.IvParameterSpec.class).getIV();
        // THEN – the IVs must be identical because the implementation derives the IV from the key
        assertArrayEquals(iv1, iv2, "IV generated from the same key should be identical");
    }

    /**
     * Valid AES keys (hex strings) should result in a non‑null Cipher that
     * is configured for {@link Cipher#ENCRYPT_MODE}.
     *
     * <p>The test data mirrors the JSON test cases marked as “valid”.</p>
     */
    @ParameterizedTest(name = "[{index}] valid key \"{0}\"")
    @MethodSource("validKeyProvider")
    void createAesEncryptionCipher_ValidKey_ReturnsEncryptModeCipher(String key) throws Exception {
        // WHEN – request an encryption Cipher for a valid key
        Cipher cipher = EncryptionUtilities.createAesEncryptionCipher(key);
        // THEN – the cipher is created, non‑null and in ENCRYPT_MODE
        assertNotNull(cipher, "Cipher should not be null for a valid key");
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode(), "Cipher must be initialised for encryption");
    }

    /**
     * Invalid inputs must cause {@link IllegalArgumentException} to be thrown.
     *
     * <p>All malformed keys from the JSON test suite are exercised here.</p>
     */
    @ParameterizedTest(name = "[{index}] invalid key \"{0}\"")
    @MethodSource("invalidKeyProvider")
    void createAesEncryptionCipher_InvalidKey_ThrowsIllegalArgumentException(String key) {
        // WHEN / THEN – the method should reject the key with IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.createAesEncryptionCipher(key), "Expected IllegalArgumentException for key: " + key);
    }
}
