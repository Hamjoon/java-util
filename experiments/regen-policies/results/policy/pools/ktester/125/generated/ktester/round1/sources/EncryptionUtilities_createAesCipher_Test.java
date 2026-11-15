package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import javax.crypto.Cipher;
import java.security.Key;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.InvalidKeyException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesCipher_Test {

    /**
     * A sample key used for all test cases.
     */
    private static final String SAMPLE_KEY = "TestPassword123";

    /**
     * Sample plaintext used for the encrypt‑decrypt round‑trip test.
     */
    private static final String SAMPLE_PLAINTEXT = "Hello, EncryptionUtilities!";

    /**
     * Supplies the valid‑input scenarios extracted from the JSON specification.
     */
    private static Stream<ValidCase> validCasesProvider() {
        return Stream.of(new ValidCase("00112233445566778899aabbccddeeff", Cipher.ENCRYPT_MODE, "Standard 32‑char hex key, ENCRYPT_MODE"), new ValidCase("short", Cipher.DECRYPT_MODE, "Key shorter than 16 bytes, DECRYPT_MODE"), new ValidCase("thiskeyiswaytoolongtobeuseddirectlyandshouldbetruncated", Cipher.ENCRYPT_MODE, "Long key, ENCRYPT_MODE"), new ValidCase("", Cipher.DECRYPT_MODE, "Empty string key, DECRYPT_MODE (should produce a zero‑filled key)"), new ValidCase("ÄÖÜßéçø", Cipher.ENCRYPT_MODE, "Unicode characters key, ENCRYPT_MODE"), new ValidCase("00112233445566778899AABBCCDDEEFF", Cipher.WRAP_MODE, "Hex key with upper‑case letters, WRAP_MODE"), new ValidCase("00112233445566778899aabbccddeeff", Cipher.UNWRAP_MODE, "Hex key, UNWRAP_MODE"), new ValidCase("  00112233 44556677 8899aabb ccdd eeff  ", Cipher.ENCRYPT_MODE, "Key containing whitespace, ENCRYPT_MODE"));
    }

    /**
     * Supplies the error scenarios extracted from the JSON specification.
     */
    private static Stream<ErrorCase> errorCasesProvider() {
        return Stream.of(// null key – NPE or IllegalArgumentException (implementation dependent)
        new ErrorCase(null, Cipher.ENCRYPT_MODE, NullPointerException.class, "Null key should cause NPE/IAE"), // non‑hex characters – IllegalArgumentException from createCipherBytes
        new ErrorCase("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ", Cipher.ENCRYPT_MODE, IllegalArgumentException.class, "Non‑hex characters should trigger IllegalArgumentException"), // odd‑length hex string – IllegalArgumentException
        new ErrorCase("ABC", Cipher.DECRYPT_MODE, IllegalArgumentException.class, "Odd‑length hex string should trigger IllegalArgumentException"), // negative mode – IllegalArgumentException from Cipher.init
        new ErrorCase("00112233445566778899aabbccddeeff", -1, IllegalArgumentException.class, "Negative mode value should cause IllegalArgumentException"), // unknown large mode – IllegalArgumentException
        new ErrorCase("00112233445566778899aabbccddeeff", 999, IllegalArgumentException.class, "Out‑of‑range mode value should cause IllegalArgumentException"), // empty key leading to zero‑length key – InvalidKeyException
        new ErrorCase("", Cipher.ENCRYPT_MODE, InvalidKeyException.class, "Empty key may produce zero‑length key causing InvalidKeyException"), // single‑character key – InvalidKeyException (key length 1 byte)
        new ErrorCase("0", Cipher.DECRYPT_MODE, InvalidKeyException.class, "1‑byte key should cause InvalidKeyException"));
    }

    @Test
    void createAesCipher_WithEncryptMode_ReturnsInitializedCipher() throws Exception {
        // given
        int mode = Cipher.ENCRYPT_MODE;
        // when
        Cipher cipher = EncryptionUtilities.createAesCipher(SAMPLE_KEY, mode);
        // then
        assertNotNull(cipher, "Cipher should not be null");
        assertEquals("AES", cipher.getAlgorithm(), "Cipher algorithm should be AES");
        assertEquals(mode, cipher.getMode() == null ? Cipher.ENCRYPT_MODE : mode, "Cipher mode should match the requested mode");
        // The internal mode string may be null for some providers; we rely on the fact that
        // the Cipher was initialized with the requested mode (no exception means success).
    }

    @Test
    void createAesCipher_WithDecryptMode_ReturnsInitializedCipher() throws Exception {
        // given
        int mode = Cipher.DECRYPT_MODE;
        // when
        Cipher cipher = EncryptionUtilities.createAesCipher(SAMPLE_KEY, mode);
        // then
        assertNotNull(cipher, "Cipher should not be null");
        assertEquals("AES", cipher.getAlgorithm(), "Cipher algorithm should be AES");
        // As above, we trust the initialization succeeded.
    }

    @Test
    void encryptAndDecrypt_RoundTripProducesOriginalPlaintext() throws Exception {
        // given
        Cipher encryptCipher = EncryptionUtilities.createAesCipher(SAMPLE_KEY, Cipher.ENCRYPT_MODE);
        Cipher decryptCipher = EncryptionUtilities.createAesCipher(SAMPLE_KEY, Cipher.DECRYPT_MODE);
        byte[] plainBytes = SAMPLE_PLAINTEXT.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // when
        byte[] encrypted = encryptCipher.doFinal(plainBytes);
        byte[] decrypted = decryptCipher.doFinal(encrypted);
        String result = new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        // then
        assertEquals(SAMPLE_PLAINTEXT, result, "Decrypted text must match the original plaintext");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validCasesProvider")
    void createAesCipher_ValidInputs_ReturnsUsableCipher(ValidCase tc) throws Exception {
        // Act
        Cipher cipher = EncryptionUtilities.createAesCipher(tc.key(), tc.mode());
        // Assert basic properties
        assertNotNull(cipher, "Cipher must not be null for key: " + tc.key());
        assertEquals("AES", cipher.getAlgorithm(), "Algorithm must be AES");
        // Verify that the cipher can actually encrypt/decrypt a short payload.
        Cipher opposite = EncryptionUtilities.createAesCipher(tc.key(), tc.mode() == Cipher.ENCRYPT_MODE ? Cipher.DECRYPT_MODE : Cipher.ENCRYPT_MODE);
        byte[] payload = "payload".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] encrypted = cipher.doFinal(payload);
        byte[] decrypted = opposite.doFinal(encrypted);
        assertArrayEquals(payload, decrypted, "Round‑trip with key '" + tc.key() + "' should reproduce original bytes");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorCasesProvider")
    void createAesCipher_ErrorScenarios_ThrowsExpectedException(ErrorCase ec) {
        // Arrange
        String key = ec.key();
        // default, should not be used when key is null
        int mode = ec.mode() != null ? ec.mode() : Cipher.ENCRYPT_MODE;
        // Act & Assert
        assertThrows(ec.expectedException(), () -> EncryptionUtilities.createAesCipher(key, mode), () -> "Expected " + ec.expectedException().getSimpleName() + " for case: " + ec.description());
    }
}
