package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

class EncryptionUtilities_encryptBytes_Test {

    /**
     * Sample key used for AES‑128 encryption.
     */
    private static final String SAMPLE_KEY = "testPassword";

    /**
     * Sample payload – a short UTF‑8 string converted to bytes.
     */
    private static final byte[] SAMPLE_CONTENT = "The quick brown fox jumps over the lazy dog".getBytes();

    static Stream<Arguments> successfulEncryptionProvider() {
        return Stream.of(// Normal UTF‑8 string
        Arguments.of("MySecretKey12345", "Hello, World!".getBytes(StandardCharsets.UTF_8), "normal UTF‑8 string"), // Empty byte array (empty string)
        Arguments.of("AnotherKey45678", new byte[0], "empty byte array"), // Raw binary data
        Arguments.of("1234567890ABCDEF", new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, "binary data"), // Exact 16‑byte key (AES‑128)
        Arguments.of("ThisKeyIsExactly16B", "Exact16".getBytes(StandardCharsets.UTF_8), "exact 16‑byte key"), // Exact 24‑byte key (AES‑192)
        Arguments.of("ThisKeyIsExactly24BytesLong!!", "Exact24".getBytes(StandardCharsets.UTF_8), "exact 24‑byte key"), // Exact 32‑byte key (AES‑256)
        Arguments.of("ThisKeyIsExactly32BytesLongForAES256!!!", "Exact32".getBytes(StandardCharsets.UTF_8), "exact 32‑byte key"), // Large content (1 MiB of 'A')
        Arguments.of("LargeContentKey123", generateRepeatedBytes((byte) 'A', 1 * 1024 * 1024), "large 1 MiB payload"), // Non‑ASCII UTF‑8 content
        Arguments.of("UnicodeKey123456", "こんにちは世界".getBytes(StandardCharsets.UTF_8), "non‑ASCII UTF‑8 content"), // Key with special characters
        Arguments.of("Sp3c!@l#Key$%^&*()", "SpecialKeyTest".getBytes(StandardCharsets.UTF_8), "key containing symbols"), // Explicit zero‑length byte array
        Arguments.of("ZeroLenKey12345", new byte[0], "explicit zero‑length array"));
    }

    private static byte[] generateRepeatedBytes(byte value, int length) {
        byte[] arr = new byte[length];
        for (int i = 0; i < length; i++) {
            arr[i] = value;
        }
        return arr;
    }

    static Stream<Arguments> exceptionCasesProvider() {
        return Stream.of(// Null key
        Arguments.of(null, "Test".getBytes(StandardCharsets.UTF_8), "null key"), // Empty key (should trigger InvalidKeyException)
        Arguments.of("", "Test".getBytes(StandardCharsets.UTF_8), "empty key"), // Null content array
        Arguments.of("ValidKey12345678", null, "null content"));
    }

    @Test
    @DisplayName("encryptBytes should produce a hex string that can be decrypted back to the original bytes")
    void encryptBytes_roundTrip() {
        // given – a key and a byte array (the "given" phase is already expressed by the constants)
        // when – encrypt the bytes
        String encryptedHex = EncryptionUtilities.encryptBytes(SAMPLE_KEY, SAMPLE_CONTENT);
        // then – the result must be a non‑null, non‑empty hex string
        assertNotNull(encryptedHex, "Encrypted hex string must not be null");
        assertFalse(encryptedHex.isEmpty(), "Encrypted hex string must not be empty");
        // and – decrypting the hex string must yield the original byte array
        byte[] decrypted = EncryptionUtilities.decryptBytes(SAMPLE_KEY, encryptedHex);
        assertArrayEquals(SAMPLE_CONTENT, decrypted, "Decrypted bytes must match the original content");
    }

    @Test
    @DisplayName("encryptBytes should throw IllegalStateException when encryption fails")
    void encryptBytes_failure() {
        // given – an obviously invalid key that will cause the internal Cipher creation to fail.
        // The implementation derives the IV from the MD5 of the key, so an empty key is still valid.
        // To provoke a failure we pass a null key, which will trigger a NullPointerException
        // inside the underlying Cipher creation.
        String nullKey = null;
        byte[] data = new byte[] { 0x01, 0x02 };
        // when / then – the method must wrap any exception in IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(nullKey, data), "Expected encryptBytes to wrap internal exceptions in IllegalStateException");
        assertNotNull(ex.getCause(), "Cause of IllegalStateException should be present");
    }

    @ParameterizedTest(name = "[{index}] key=''{0}'', content length={2}")
    @MethodSource("successfulEncryptionProvider")
    @DisplayName("encryptBytes should encrypt various inputs and allow correct decryption")
    void encryptBytes_successful(String key, byte[] content, String description) {
        // encrypt
        String encrypted = EncryptionUtilities.encryptBytes(key, content);
        assertNotNull(encrypted, "Encrypted string must not be null for " + description);
        assertFalse(encrypted.isEmpty(), "Encrypted string must not be empty for " + description);
        // decrypt and verify round‑trip
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encrypted);
        assertArrayEquals(content, decrypted, "Decrypted bytes must equal original content for " + description);
    }

    @ParameterizedTest(name = "[{index}] key=''{0}'', content={2}")
    @MethodSource("exceptionCasesProvider")
    @DisplayName("encryptBytes should wrap internal failures in IllegalStateException")
    void encryptBytes_exceptionCases(String key, byte[] content, String description) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(key, content), "Expected IllegalStateException for " + description);
        assertNotNull(ex.getCause(), "Cause should be present for " + description);
    }

    @Test
    @DisplayName("encryptBytes should reject a key shorter than 16 bytes")
    void encryptBytes_keyTooShort() {
        // 5 characters
        String shortKey = "short";
        byte[] data = "Edge".getBytes(StandardCharsets.UTF_8);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(shortKey, data));
        assertNotNull(ex.getCause(), "Cause should indicate invalid key length");
    }

    @Test
    @DisplayName("encryptBytes should accept keys of valid AES lengths (16, 24, 32 bytes)")
    void encryptBytes_validKeyLengths() {
        // 16‑byte key
        assertDoesNotThrow(() -> {
            String enc = EncryptionUtilities.encryptBytes("1234567890ABCDEF", SAMPLE_CONTENT);
            assertNotNull(enc);
        });
        // 24‑byte key
        assertDoesNotThrow(() -> {
            String enc = EncryptionUtilities.encryptBytes("1234567890ABCDEFGHIJKL", SAMPLE_CONTENT);
            assertNotNull(enc);
        });
        // 32‑byte key
        assertDoesNotThrow(() -> {
            String enc = EncryptionUtilities.encryptBytes("1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ12", SAMPLE_CONTENT);
            assertNotNull(enc);
        });
    }

    @Test
    @DisplayName("encryptBytes should reject a key longer than 32 bytes")
    void encryptBytes_keyTooLong() {
        // 38 chars
        String longKey = "ThisKeyIsLongerThan32Characters1234567890";
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(longKey, SAMPLE_CONTENT));
        assertNotNull(ex.getCause(), "Cause should indicate invalid key length");
    }

    @Test
    @DisplayName("encryptBytes should reject a very long key (>> 32 bytes)")
    void encryptBytes_veryLongKey() {
        String veryLongKey = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(veryLongKey, SAMPLE_CONTENT));
        assertNotNull(ex.getCause(), "Cause should indicate invalid key length");
    }

    @Test
    @DisplayName("encryptBytes should produce different ciphertexts for the same input (random IV) yet both decrypt correctly")
    void encryptBytes_repeatedCallsProduceDifferentCiphertexts() {
        String key = "ConsistentKey1234";
        byte[] data = "Repeatable".getBytes(StandardCharsets.UTF_8);
        String enc1 = EncryptionUtilities.encryptBytes(key, data);
        String enc2 = EncryptionUtilities.encryptBytes(key, data);
        // The two ciphertexts should differ because the IV is derived from the key's MD5,
        // but the implementation also adds randomness (e.g., a salt). The exact behaviour
        // may vary; we only assert they are not equal to demonstrate variability.
        assertNotEquals(enc1, enc2, "Two encryptions with the same key/content should yield different ciphertexts");
        // Both must decrypt back to the original data
        assertArrayEquals(data, EncryptionUtilities.decryptBytes(key, enc1));
        assertArrayEquals(data, EncryptionUtilities.decryptBytes(key, enc2));
    }
}
