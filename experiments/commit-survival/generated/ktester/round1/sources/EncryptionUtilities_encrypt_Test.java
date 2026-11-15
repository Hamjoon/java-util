package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_encrypt_Test {

    /**
     * A sample key used for the normal round‑trip test.
     */
    private static final String SAMPLE_KEY = "my-secret-key-12345";

    /**
     * Sample plain‑text content.
     */
    private static final String SAMPLE_CONTENT = "The quick brown fox jumps over the lazy dog";

    /**
     * Supplies the successful encryption scenarios extracted from the JSON matrix.
     *
     * @return stream of arguments: key, content, description
     */
    private static Stream<Arguments> successfulEncryptionProvider() {
        return Stream.of(// 1. Happy‑path 16‑character key
        Arguments.of("mySecretKey12345", "Hello, World!", "valid 16‑char key, normal payload"), // 2. Empty content (should still encrypt)
        Arguments.of("mySecretKey12345", "", "valid key, empty content"), // 3. Large payload (≈5 KB)
        Arguments.of("mySecretKey12345", "a".repeat(5_000), "valid key, large payload (5 KB)"), // 4. AES‑192 key length (24 chars)
        Arguments.of("mySecretKey123456789012", "Sample payload for AES‑192 key", "24‑char key (AES‑192)"), // 5. AES‑256 key length (32 chars)
        Arguments.of("mySecretKey1234567890123456ABCD", "Another sample for AES‑256", "32‑char key (AES‑256)"), // 6. Special characters in key and whitespace in content
        Arguments.of("!@#$%^&*()_+-=SecretKey", "   Text with spaces, tabs\t, and newlines\nEnd", "special characters in key, mixed whitespace in content"), // 7. Unicode characters in both key and content
        Arguments.of("UnicodeКлюч12345", "😀🚀🌟 Unicode emojis inside text", "Unicode key and content"), // 8. Content consisting solely of newline and tab characters
        Arguments.of("mySecretKey12345", "\n\tHello\nWorld\t", "content only newlines and tabs"), // 9. Very long key (exceeds 32 chars) – utility should handle it gracefully
        Arguments.of("thisIsAVeryLongKeyThatExceedsTypicalLengthsAndShouldBeHandledGracefullyByTheUtilityMethod", "Long‑key test payload", "very long key (>32 chars)"));
    }

    /**
     * Supplies the failure scenarios extracted from the JSON matrix.
     *
     * @return stream of arguments: key, content, description
     */
    private static Stream<Arguments> failureEncryptionProvider() {
        return Stream.of(// 1. Null key
        Arguments.of(null, "sample", "null key"), // 2. Empty key string
        Arguments.of("", "sample", "empty key"), // 3. Null content
        Arguments.of("mySecretKey12345", null, "null content"), // 4. Short key (5 characters) – insufficient for AES‑128
        Arguments.of("short", "any content", "key too short (5 chars)"));
    }

    /**
     * Verifies that encrypting a string and then decrypting the result yields the original string.
     */
    @Test
    void encryptDecryptRoundTrip() {
        // GIVEN
        String key = SAMPLE_KEY;
        String original = SAMPLE_CONTENT;
        // WHEN
        String encrypted = EncryptionUtilities.encrypt(key, original);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        // THEN
        assertNotNull(encrypted, "Encrypted value should not be null");
        assertNotEquals(original, encrypted, "Encrypted value should differ from the original");
        assertEquals(original, decrypted, "Decrypted value should match the original");
    }

    /**
     * Verifies that a {@link IllegalStateException} is thrown when the encryption process fails.
     * Passing {@code null} as the key forces {@code createAesEncryptionCipher} to throw an exception,
     * which is wrapped by {@code encrypt}.
     */
    @Test
    void encryptWithInvalidKeyThrowsIllegalStateException() {
        // GIVEN
        String invalidKey = null;
        String content = "any content";
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(invalidKey, content), "encrypt should wrap underlying exceptions in IllegalStateException");
        // The cause should be a non‑null exception from the cipher creation path.
        assertNotNull(ex.getCause(), "Cause of IllegalStateException should not be null");
    }

    /**
     * Parameterized test that encrypts the supplied {@code content} with {@code key},
     * then decrypts the ciphertext and verifies that the original content is recovered.
     *
     * @param key       encryption key (may be of any supported length)
     * @param content   plaintext to encrypt; may be empty or contain Unicode characters
     * @param scenario  textual description used only for test readability
     */
    @ParameterizedTest(name = "[{index}] success – {2}")
    @MethodSource("successfulEncryptionProvider")
    void encryptDecryptSuccess(String key, String content, String scenario) {
        // WHEN
        String encrypted = EncryptionUtilities.encrypt(key, content);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        // THEN
        assertNotNull(encrypted, "Encrypted string must not be null – " + scenario);
        assertFalse(encrypted.isEmpty(), "Encrypted string must not be empty – " + scenario);
        assertEquals(content, decrypted, "Decrypted content must match original – " + scenario);
    }

    /**
     * Parameterized test that verifies {@link EncryptionUtilities#encrypt(String, String)}
     * throws an {@link IllegalStateException} for invalid inputs.
     *
     * @param key       the key supplied to the method (may be {@code null} or malformed)
     * @param content   the content supplied to the method (may be {@code null})
     * @param scenario  description of the test case
     */
    @ParameterizedTest(name = "[{index}] failure – {2}")
    @MethodSource("failureEncryptionProvider")
    void encryptFailureThrowsIllegalStateException(String key, String content, String scenario) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content), "Expected IllegalStateException – " + scenario);
        assertNotNull(ex.getCause(), "Cause of IllegalStateException should be non‑null – " + scenario);
    }
}
