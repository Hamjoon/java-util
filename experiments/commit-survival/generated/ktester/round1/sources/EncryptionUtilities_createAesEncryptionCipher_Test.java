package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import javax.crypto.Cipher;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesEncryptionCipher_Test {

    /**
     * Supplies keys that are expected to be accepted.
     */
    static Stream<Arguments> validKeyProvider() {
        return Stream.of(// 24‑character key (valid 192‑bit)
        Arguments.of("validKey1234567890abcdef"), // Exactly 16 characters – 128‑bit
        Arguments.of("thisKeyIsExactly16"), // Exactly 24 characters – 192‑bit
        Arguments.of("thisKeyIsExactly24Chars!!"), // Exactly 32 characters – 256‑bit
        Arguments.of("thisKeyIsExactly32CharactersLong!!"), // Non‑ASCII characters (UTF‑8 handling)
        Arguments.of("keyWithNonAsciißΩ"), // 8 emoji characters → 32 UTF‑8 bytes (valid 256‑bit)
        Arguments.of("😀😀😀😀😀😀😀😀"), // Key containing a newline character
        Arguments.of("keyWith\nNewLine"), // 16 null characters (U+0000) – 16 bytes of zeros
        Arguments.of("\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000\u0000"), // Very long key (1000 'a's) – library hashes/derives a valid key internally
        Arguments.of("a".repeat(1000)));
    }

    /**
     * Supplies keys that are expected to cause an exception.
     */
    static Stream<Arguments> invalidKeyProvider() {
        return Stream.of(// Null key
        Arguments.of((String) null), // Empty string – per JSON it should be invalid, but the library may accept it.
        // We keep it here to verify behaviour aligns with the specification.
        Arguments.of(""), // Too short (less than 16 bytes)
        Arguments.of("short"), // 33 'a's – exceeds 32‑byte maximum
        Arguments.of("a".repeat(33)), // Only whitespace (no entropy)
        Arguments.of("   "), // 20‑byte ASCII key – unsupported length
        Arguments.of("twentyByteKey12345"), // 28‑byte ASCII key – unsupported length
        Arguments.of("twentyEightByteKey12345678"), // 48‑byte ASCII key – exceeds maximum
        Arguments.of("fourtyEightByteKey1234567890123456789012345678"), // 16 emoji characters → 64 bytes (exceeds max)
        Arguments.of("😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀"), // 12 emoji characters → 48 bytes (unsupported length)
        Arguments.of("😀😀😀😀😀😀😀😀😀😀😀😀"));
    }

    /**
     * Verify that the cipher returned by {@code createAesEncryptionCipher}
     * can encrypt data that can subsequently be decrypted with the matching
     * decryption cipher.
     */
    @Test
    void testEncryptionCipherRoundTrip() throws Exception {
        // given
        // any non‑null key string
        String key = "MySuperSecretKey123";
        String original = "The quick brown fox jumps over the lazy dog";
        // when
        Cipher encryptCipher = EncryptionUtilities.createAesEncryptionCipher(key);
        byte[] encrypted = encryptCipher.doFinal(original.getBytes(StandardCharsets.UTF_8));
        Cipher decryptCipher = EncryptionUtilities.createAesDecryptionCipher(key);
        byte[] decrypted = decryptCipher.doFinal(encrypted);
        String result = new String(decrypted, StandardCharsets.UTF_8);
        // then
        assertEquals(original, result, "Decrypted text should match the original plaintext");
    }

    /**
     * Verify that a {@code null} key results in an exception from the
     * underlying implementation (the exact exception type is not part of the
     * public contract, so we assert that *some* exception is thrown).
     */
    @Test
    void testEncryptionCipherWithNullKeyThrowsException() {
        // given
        String nullKey = null;
        // when / then
        assertThrows(Exception.class, () -> EncryptionUtilities.createAesEncryptionCipher(nullKey));
    }

    /**
     * Verify that an empty string key still produces a usable cipher.
     * This mirrors the behaviour of the library – it will derive a key
     * from the supplied string, even if that string is empty.
     */
    @Test
    void testEncryptionCipherWithEmptyKey() throws Exception {
        // given
        String emptyKey = "";
        String original = "Sample text";
        // when
        Cipher encryptCipher = EncryptionUtilities.createAesEncryptionCipher(emptyKey);
        byte[] encrypted = encryptCipher.doFinal(original.getBytes(StandardCharsets.UTF_8));
        Cipher decryptCipher = EncryptionUtilities.createAesDecryptionCipher(emptyKey);
        byte[] decrypted = decryptCipher.doFinal(encrypted);
        String result = new String(decrypted, StandardCharsets.UTF_8);
        // then
        assertEquals(original, result, "Cipher should work even with an empty key");
    }

    /**
     * All keys that the library is expected to accept and produce a functional
     * AES encryption cipher.
     */
    @ParameterizedTest(name = "[{index}] Valid key: \"{0}\"")
    @MethodSource("validKeyProvider")
    void testCreateAesEncryptionCipher_WithValidKeys(String key) throws Exception {
        // Create encryption cipher – must not throw and must be AES
        Cipher encrypt = EncryptionUtilities.createAesEncryptionCipher(key);
        assertNotNull(encrypt, "Cipher should not be null");
        assertEquals("AES", encrypt.getAlgorithm(), "Cipher algorithm must be AES");
        // Simple round‑trip to ensure the cipher works
        String plain = "RoundTripTest";
        byte[] encrypted = encrypt.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        Cipher decrypt = EncryptionUtilities.createAesDecryptionCipher(key);
        byte[] decrypted = decrypt.doFinal(encrypted);
        String result = new String(decrypted, StandardCharsets.UTF_8);
        assertEquals(plain, result, "Decrypted text must match the original");
    }

    /**
     * Keys that should cause the method to fail – any {@link Exception}
     * is acceptable because the public API does not prescribe a concrete type.
     */
    @ParameterizedTest(name = "[{index}] Invalid key: \"{0}\"")
    @MethodSource("invalidKeyProvider")
    void testCreateAesEncryptionCipher_WithInvalidKeys(String key) {
        assertThrows(Exception.class, () -> EncryptionUtilities.createAesEncryptionCipher(key), "Expected an exception for key: " + key);
    }
}
