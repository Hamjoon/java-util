package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_decryptBytes_Test {

    /**
     * Provides a set of (key, plaintext) pairs that are known to work.
     * Includes:
     * <ul>
     *   <li>16‑byte key → AES‑128</li>
     *   <li>24‑byte key → AES‑192</li>
     *   <li>Unicode characters in the key</li>
     * </ul>
     */
    static Stream<Arguments> successfulDecryptionProvider() {
        return Stream.of(// 16‑byte key (AES‑128)
        arguments("testKey12345678", "Hello"), // 24‑byte key (AES‑192)
        arguments("twentyFourByteKey123456", "World"), // Unicode key
        arguments("pässwörd🔑123456", "Unicode"));
    }

    /**
     * Supplies various malformed inputs that should trigger an
     * {@link IllegalStateException}.  For cases that need a *valid* encrypted
     * payload, the payload is generated once using a known good key.
     */
    static Stream<Arguments> errorDecryptionProvider() {
        // A valid encrypted payload used as a baseline for several negative tests.
        String baselineKey = "testKey12345678";
        byte[] baselinePlain = "dummy".getBytes(StandardCharsets.UTF_8);
        String validHex = EncryptionUtilities.encryptBytes(baselineKey, baselinePlain);
        return Stream.of(// Invalid hex string (non‑hex characters)
        arguments(baselineKey, "ZZZZ", "Invalid hex string – non‑hex characters"), // Key too short for AES (AES requires 16/24/32‑byte keys)
        arguments("short", validHex, "Invalid key – insufficient length"), // Null key
        arguments(null, validHex, "Null key parameter"), // Null hex string
        arguments(baselineKey, null, "Null hex string parameter"), // Empty key string
        arguments("", validHex, "Empty key string"), // Empty hex string (results in zero‑length ciphertext)
        arguments(baselineKey, "", "Empty hex string"), // Ciphertext length not a multiple of AES block size (5 bytes → 10 hex chars)
        arguments(baselineKey, "0011223344", "Invalid ciphertext length – not a multiple of block size"), // Tampered ciphertext (modify first hex digit)
        arguments(baselineKey, tamperHex(validHex), "Tampered ciphertext – altered byte"));
    }

    /**
     * Helper that flips the first hex digit to create a *tampered* ciphertext.
     *
     * @param hex original hex string
     * @return a new hex string with the first character altered
     */
    private static String tamperHex(String hex) {
        if (hex == null || hex.isEmpty()) {
            return hex;
        }
        char[] chars = hex.toCharArray();
        // Flip between '0' and '1' to guarantee a change while staying valid hex.
        chars[0] = (chars[0] == '0') ? '1' : '0';
        return new String(chars);
    }

    @Test
    void decryptBytes_ShouldReturnOriginalBytes_WhenGivenValidKeyAndHex() {
        // given
        // 16‑character key → 128‑bit AES
        String key = "testkey12345678";
        byte[] original = "Hello EncryptionUtilities!".getBytes(StandardCharsets.UTF_8);
        String encryptedHex = EncryptionUtilities.encryptBytes(key, original);
        // when
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        // then
        assertArrayEquals(original, decrypted, "The decrypted byte array should match the original input.");
    }

    @Test
    void decryptBytes_ShouldThrowIllegalStateException_WhenHexIsInvalid() {
        // given
        String key = "testkey12345678";
        // not a valid hex representation
        String malformedHex = "ZZZZ";
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes(key, malformedHex));
        assertTrue(ex.getMessage().contains("Error occurred decrypting data"), "Exception message should indicate a decryption error.");
        assertNotNull(ex.getCause(), "The thrown IllegalStateException should wrap the underlying cause.");
    }

    /**
     * Verifies that {@code decryptBytes} returns the original byte array for
     * a variety of valid keys and plaintexts.
     */
    @ParameterizedTest(name = "[{index}] key=''{0}'', plaintext=''{1}''")
    @MethodSource("successfulDecryptionProvider")
    void decryptBytes_ShouldReturnOriginalBytes_WhenValidKeyAndHex(String key, String plaintext) {
        // given – encrypt the plaintext with the same key
        byte[] original = plaintext.getBytes(StandardCharsets.UTF_8);
        String encryptedHex = EncryptionUtilities.encryptBytes(key, original);
        // when – decrypt the previously generated hex string
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        // then – the decrypted bytes must match the original payload
        assertArrayEquals(original, decrypted, "Decrypted bytes should be identical to the original input.");
    }

    /**
     * Upper‑case hex strings must be decoded correctly (hex decoding is case‑insensitive).
     */
    @Test
    void decryptBytes_ShouldHandleUpperCaseHexString() {
        // given
        String key = "testKey12345678";
        byte[] original = "Hello".getBytes(StandardCharsets.UTF_8);
        // uppercase representation
        String encryptedHex = EncryptionUtilities.encryptBytes(key, original).toUpperCase();
        // when
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        // then
        assertArrayEquals(original, decrypted, "Decryption must succeed even when the hex string is uppercase.");
    }

    /**
     * All invalid‑input cases must result in an {@link IllegalStateException}
     * that wraps the underlying cause (e.g., {@link IllegalArgumentException},
     * {@link NullPointerException}, {@link javax.crypto.BadPaddingException}, …).
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("errorDecryptionProvider")
    void decryptBytes_ShouldThrowIllegalStateException_OnInvalidInput(String key, String hexStr, String description) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes(key, hexStr), "Expected IllegalStateException for: " + description);
        // The wrapper must contain the original exception for debugging purposes.
        assertNotNull(ex.getCause(), "IllegalStateException should wrap the original cause.");
    }
}
