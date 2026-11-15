package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

class EncryptionUtilities_decrypt_Test {

    /**
     * Sample key used for encryption/decryption.
     */
    private static final String SAMPLE_KEY = "testPassword";

    /**
     * Sample plain‑text used for the round‑trip test.
     */
    private static final String SAMPLE_PLAIN_TEXT = "The quick brown fox jumps over the lazy dog";

    /**
     * Another key used for wrong‑key scenario.
     */
    private static final String WRONG_KEY = "wrongkey12345678";

    private static Stream<Arguments> successfulDecryptionProvider() {
        return Stream.of(// basic case (key length 16)
        Arguments.of("0123456789abcdef", "SecretMessage"), // empty plaintext – encrypt yields a non‑null hex, but decrypting an empty hex should give empty string
        Arguments.of("mySuperSecretKey!", ""));
    }

    /**
     * Verifies that a string encrypted with {@link EncryptionUtilities#encrypt(String, String)}
     * can be correctly decrypted back to the original value.
     */
    @Test
    void decrypt_ShouldReturnOriginalString_WhenGivenValidKeyAndCipherText() {
        // given
        String encrypted = EncryptionUtilities.encrypt(SAMPLE_KEY, "The quick brown fox jumps over the lazy dog");
        // when
        String decrypted = EncryptionUtilities.decrypt(SAMPLE_KEY, encrypted);
        // then
        assertEquals("The quick brown fox jumps over the lazy dog", decrypted, "Decrypted text should match the original plain text");
    }

    /**
     * Ensures that an {@link IllegalStateException} is thrown when the supplied
     * hexadecimal string cannot be decoded (e.g., malformed input).
     */
    @Test
    void decrypt_ShouldThrowIllegalStateException_WhenHexStringIsInvalid() {
        // given: a malformed hex string (odd length, non‑hex characters)
        String malformedHex = "ZXY123";
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(SAMPLE_KEY, malformedHex));
        // The cause should be the underlying exception from the decode/cipher step.
        assertNotNull(ex.getCause(), "The IllegalStateException should wrap the original cause");
    }

    /* -------------------------- Successful decryption ------------------- */
    @ParameterizedTest(name = "[{index}] key={0}, plain={1}")
    @MethodSource("successfulDecryptionProvider")
    void decrypt_ShouldReturnOriginalString_ForValidInputs(String key, String plainText) {
        // encrypt the known plaintext with the supplied key
        String encrypted = EncryptionUtilities.encrypt(key, plainText);
        // decrypt and verify round‑trip
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plainText, decrypted, () -> "Decrypted text should equal the original plaintext for key='" + key + "'");
    }

    /* ---------------------- Successful decryption – Unicode -------------- */
    @Test
    void decrypt_ShouldHandleUnicodePlaintext() {
        // 16‑char key
        String key = "mySecretKey123456";
        // Japanese greeting
        String plain = "こんにちは";
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plain, decrypted, "Unicode text should survive encrypt‑decrypt round‑trip");
    }

    /* ---------------------- Successful decryption – mixed‑case hex -------- */
    @Test
    void decrypt_ShouldAcceptMixedCaseHexString() {
        String key = "0123456789abcdef";
        String plain = "SecretMessage";
        // encrypt normally (lower‑case hex)
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        // transform to upper‑case to test case‑insensitivity
        String upperCaseHex = encrypted.toUpperCase();
        String decrypted = EncryptionUtilities.decrypt(key, upperCaseHex);
        assertEquals(plain, decrypted, "Decryption should be case‑insensitive for hex input");
    }

    /* ---------------------- Successful decryption – large ciphertext ----- */
    @Test
    void decrypt_ShouldHandleLargeCiphertextSpanningMultipleBlocks() {
        String key = "0123456789abcdef";
        // 48 characters → 48 bytes (AES block size is 16 bytes)
        String plain = "A".repeat(48);
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plain, decrypted, "Decryption must correctly process data larger than a single AES block");
    }

    /* ---------------------- Decryption with wrong key -------------------- */
    @Test
    void decrypt_WithWrongKey_ShouldReturnIncorrectPlaintext() {
        String correctKey = "0123456789abcdef";
        String plain = "SecretMessage";
        String encrypted = EncryptionUtilities.encrypt(correctKey, plain);
        // Decrypt with a different key – no exception expected, but result must differ
        String decrypted = EncryptionUtilities.decrypt(WRONG_KEY, encrypted);
        assertNotEquals(plain, decrypted, "Decrypting with the wrong key should not yield the original plaintext");
    }

    /* ---------------------- Error handling – invalid hex ----------------- */
    @ParameterizedTest(name = "[{index}] hex={0}")
    @ValueSource(strings = { "ZZZZ", "ABC", " 6a2c8b5e9f1d3c4a7b8e9f0a1b2c3d4e " })
    void decrypt_ShouldThrowIllegalStateException_ForMalformedHex(String malformedHex) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt("0123456789abcdef", malformedHex));
        assertNotNull(ex.getCause(), "Exception should wrap the underlying cause");
    }

    /* ---------------------- Error handling – null key --------------------- */
    @Test
    void decrypt_ShouldThrowIllegalStateException_WhenKeyIsNull() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(null, "6a2c8b5e9f1d3c4a7b8e9f0a1b2c3d4e"));
        assertNotNull(ex.getCause(), "Exception should wrap the underlying cause");
    }

    /* ---------------------- Error handling – null hex string -------------- */
    @Test
    void decrypt_ShouldThrowIllegalStateException_WhenHexIsNull() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt("0123456789abcdef", null));
        assertNotNull(ex.getCause(), "Exception should wrap the underlying cause");
    }

    /* ---------------------- Error handling – empty key -------------------- */
    @Test
    void decrypt_ShouldThrowIllegalStateException_WhenKeyIsEmpty() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt("", "6a2c8b5e9f1d3c4a7b8e9f0a1b2c3d4e"));
        assertNotNull(ex.getCause(), "Exception should wrap the underlying cause");
    }

    /* ---------------------- Error handling – empty hex string ------------- */
    @Test
    void decrypt_ShouldReturnEmptyString_WhenHexIsEmpty() {
        // According to the specification, an empty hex string decodes to an empty byte array,
        // which the cipher turns into an empty plaintext.
        String result = EncryptionUtilities.decrypt("0123456789abcdef", "");
        assertEquals("", result, "Empty hex string should result in an empty decrypted string");
    }

    /* ---------------------- Error handling – whitespace in hex ------------ */
    @Test
    void decrypt_ShouldThrowIllegalStateException_WhenHexContainsWhitespace() {
        String hexWithSpaces = " 6a2c8b5e9f1d3c4a7b8e9f0a1b2c3d4e ";
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt("0123456789abcdef", hexWithSpaces));
        assertNotNull(ex.getCause(), "Exception should wrap the underlying cause");
    }
}
