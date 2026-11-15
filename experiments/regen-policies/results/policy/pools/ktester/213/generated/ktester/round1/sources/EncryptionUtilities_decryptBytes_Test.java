package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.security.SecureRandom;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_decryptBytes_Test {

    /**
     * Provides a mix of 16‑, 24‑ and 32‑byte keys together with a sample plaintext.
     */
    static Stream<Arguments> validKeyProvider() {
        return Stream.of(// 16‑byte key (32 hex chars)
        Arguments.of(16, "00112233445566778899AABBCCDDEEFF", "HelloWorld"), // 24‑byte key (48 hex chars)
        Arguments.of(24, "00112233445566778899AABBCCDDEEFF1122334455667788", "The quick brown fox jumps over the lazy dog"), // 32‑byte key (64 hex chars)
        Arguments.of(32, "00112233445566778899AABBCCDDEEFF112233445566778899AABBCCDDEEFF00", "Lorem ipsum dolor sit amet"));
    }

    static Stream<Arguments> malformedInputProvider() {
        return Stream.of(// non‑hex characters in ciphertext
        Arguments.of("Hex string contains non‑hex characters", "0123456789ABCDEF0123456789ABCDEF", "ZZZZ"), // null key
        Arguments.of("Null key leads to exception inside cipher creation", null, "6bc1bee22e409f96e93d7e117393172a"), // key too short for AES
        Arguments.of("Key length insufficient for AES", "short", "6bc1bee22e409f96e93d7e117393172a"), // odd‑length hex string
        Arguments.of("Odd‑length hex string should cause decode failure", "00112233445566778899AABBCCDDEEFF", "ABC"), // null ciphertext
        Arguments.of("Null ciphertext should cause decode failure", "00112233445566778899AABBCCDDEEFF", null));
    }

    static Stream<Arguments> invalidKeyProvider() {
        return Stream.of(// non‑hex characters in key (still passed as a string)
        Arguments.of("Key contains non‑hex characters", "GHIJKL1234567890ABCDEF1234567890", "6bc1bee22e409f96e93d7e117393172a"), // odd number of hex characters in key
        Arguments.of("Key has odd number of hex characters", // 31 chars
        "00112233445566778899AABBCCDDE", "6bc1bee22e409f96e93d7e117393172a"), // empty key string
        Arguments.of("Empty key string", "", "6bc1bee22e409f96e93d7e117393172a"), // key length not 16/24/32 bytes (20 bytes -> 40 hex chars)
        Arguments.of("Key length 20 bytes (invalid for AES)", "00112233445566778899AABBCCDDEEFF1122", "6bc1bee22e409f96e93d7e117393172a"));
    }

    @Test
    void decryptBytes_returnsOriginalBytes_whenGivenValidKeyAndHex() {
        // GIVEN
        final String key = "mySecretPassword";
        final byte[] original = "The quick brown fox jumps over the lazy dog".getBytes();
        // encrypt the original bytes to obtain a hex string
        final String encryptedHex = EncryptionUtilities.encryptBytes(key, original);
        assertNotNull(encryptedHex, "Encryption should produce a non‑null hex string");
        // WHEN
        final byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        // THEN
        assertArrayEquals(original, decrypted, "Decrypted bytes must match the original input bytes");
    }

    @Test
    void decryptBytes_throwsIllegalStateException_whenHexIsMalformed() {
        // GIVEN
        final String key = "anyKey";
        final String malformedHex = "ZZZ_not_a_hex_string";
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes(key, malformedHex), "A malformed hex string should cause an IllegalStateException");
        // Optional: verify that the cause is a decoding exception (implementation detail)
        assertNotNull(ex.getCause(), "Exception should have a cause");
    }

    @Test
    void decryptBytes_throwsIllegalStateException_whenKeyIsIncorrect() {
        // GIVEN
        // any string; length not critical for test
        final String correctKey = "correctKey123456";
        final String wrongKey = "wrongKey654321";
        final byte[] original = "Sample data".getBytes();
        // encrypt with the correct key
        final String encryptedHex = EncryptionUtilities.encryptBytes(correctKey, original);
        assertNotNull(encryptedHex);
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes(wrongKey, encryptedHex), "Decrypting with an incorrect key should result in an IllegalStateException");
        assertNotNull(ex.getCause(), "Exception should have a cause");
    }

    /* -------------------------------------------------------------
     *  Normal decryption – valid keys of different sizes
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] keySize={0} bytes")
    @MethodSource("validKeyProvider")
    void decryptBytes_returnsOriginalBytes_forValidKey(int keyBytes, String keyHex, String plainText) {
        // GIVEN
        byte[] original = plainText.getBytes();
        // encrypt using the same key string; the key may be any string – we use the hex
        // representation directly because the utility treats the key as an opaque string.
        String encryptedHex = EncryptionUtilities.encryptBytes(keyHex, original);
        assertNotNull(encryptedHex, "Encryption must produce a non‑null hex string");
        // WHEN
        byte[] decrypted = EncryptionUtilities.decryptBytes(keyHex, encryptedHex);
        // THEN
        assertArrayEquals(original, decrypted, "Decrypted bytes must match the original plaintext");
    }

    /* -------------------------------------------------------------
     *  Empty ciphertext
     * ------------------------------------------------------------- */
    @Test
    void decryptBytes_returnsEmptyArray_whenHexIsEmpty() {
        // GIVEN
        final String key = "00112233445566778899AABBCCDDEEFF";
        final String emptyHex = "";
        // WHEN
        byte[] result = EncryptionUtilities.decryptBytes(key, emptyHex);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.length, "Decryption of empty hex string must yield empty byte array");
    }

    /* -------------------------------------------------------------
     *  Large payload (1 KB) round‑trip
     * ------------------------------------------------------------- */
    @Test
    void decryptBytes_roundTripLargePayload_1KB() {
        // GIVEN
        final String key = "A1B2C3D4E5F60718293A4B5C6D7E8F90";
        byte[] original = new byte[1024];
        new SecureRandom().nextBytes(original);
        // encrypt the random payload
        String encryptedHex = EncryptionUtilities.encryptBytes(key, original);
        assertNotNull(encryptedHex);
        // WHEN
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        // THEN
        assertArrayEquals(original, decrypted, "Decrypted 1 KB payload must be identical to the original");
    }

    /* -------------------------------------------------------------
     *  Error handling – malformed input
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("malformedInputProvider")
    void decryptBytes_throwsIllegalStateException_forMalformedInput(String description, String key, String hexStr) {
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes(key, hexStr), description);
        assertNotNull(ex.getCause(), "Wrapped exception should have a cause");
    }

    /* -------------------------------------------------------------
     *  Error handling – invalid key format
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidKeyProvider")
    void decryptBytes_throwsIllegalStateException_forInvalidKey(String description, String key, String hexStr) {
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes(key, hexStr), description);
        assertNotNull(ex.getCause(), "Wrapped exception should have a cause");
    }
}
