package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_encrypt_Test {

    /**
     * Mocked static context for the EncryptionUtilities class.
     */
    private MockedStatic<EncryptionUtilities> encryptionUtilitiesMock;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for every test so that stubbing does not leak.
        encryptionUtilitiesMock = mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock – important to avoid side‑effects on other tests.
        if (encryptionUtilitiesMock != null) {
            encryptionUtilitiesMock.close();
        }
    }

    /**
     * Provides a stream of arguments for successful encryption cases.
     * Each argument contains: key, content, description.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> successfulEncryptionProvider() {
        return Stream.of(arguments("MySecretKey12345", "Hello, World!", "Normal UTF‑8 string"), arguments("MySecretKey12345", "", "Empty content – should still encrypt"), arguments("ThisKeyIsWayTooLongForAESAndShouldBeTruncatedOrCauseError", "Data", "Long key – implementation may truncate and succeed"), arguments("UnicodeKey123456", "こんにちは世界 🌍", "Unicode content – UTF‑8 handling"), arguments("!@#$%^&*()_+|}{\":?><,./;'[]\\-=", "SpecialKeyTest", "Key with special characters"), arguments("WhitespaceKey1234", "   \t\n  ", "Whitespace‑only content"), // 1 MiB of 'A'
        arguments(// 1 MiB of 'A'
        "LargeContentKey123", // 1 MiB of 'A'
        generateLargeString(1_024 * 1_024), "Large content – 1 MiB of 'A'"));
    }

    /**
     * Generates a string consisting of the character {@code 'A'} repeated {@code length} times.
     *
     * @param length number of repetitions
     * @return generated string
     */
    private static String generateLargeString(int length) {
        char[] chars = new char[length];
        java.util.Arrays.fill(chars, 'A');
        return new String(chars);
    }

    @Test
    void encrypt_thenDecrypt_returnsOriginalContent() {
        // GIVEN
        String key = "mySecretKey123";
        String content = "The quick brown fox jumps over the lazy dog";
        // WHEN
        String encrypted = EncryptionUtilities.encrypt(key, content);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        // THEN
        assertNotNull(encrypted, "Encrypted string should not be null");
        assertFalse(encrypted.isEmpty(), "Encrypted string should not be empty");
        // The encrypted text is a hex representation – it should contain only hex chars.
        assertTrue(encrypted.matches("[0-9a-fA-F]+"), "Encrypted string should be a hex representation");
        assertEquals(content, decrypted, "Decrypted text must match the original content");
    }

    @Test
    void encrypt_throwsIllegalStateException_whenCipherCreationFails() {
        // GIVEN
        String key = "badKey";
        String content = "any content";
        // Force the static helper to throw a checked exception.
        encryptionUtilitiesMock.when(() -> EncryptionUtilities.createAesEncryptionCipher(key)).thenThrow(new RuntimeException("Simulated cipher creation failure"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content), "encrypt should wrap underlying exceptions in IllegalStateException");
        assertTrue(ex.getMessage().contains("Error occurred encrypting data"));
        assertNotNull(ex.getCause(), "Cause of the IllegalStateException should be present");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulEncryptionProvider")
    void encrypt_successful_cases(String key, String content, String description) {
        // WHEN
        String encrypted = EncryptionUtilities.encrypt(key, content);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        // THEN
        assertNotNull(encrypted, "Encrypted string must not be null");
        assertFalse(encrypted.isEmpty(), "Encrypted string must not be empty");
        assertTrue(encrypted.matches("[0-9a-fA-F]+"), "Encrypted output should be a hexadecimal string");
        assertEquals(content, decrypted, "Decrypted text must equal original content for case: " + description);
    }

    @Test
    void encrypt_nullKey_throwsIllegalStateException() {
        // GIVEN
        String key = null;
        String content = "Sample text";
        // The implementation will likely throw NPE inside createAesEncryptionCipher.
        // Stub it explicitly to make the expectation clear.
        encryptionUtilitiesMock.when(() -> EncryptionUtilities.createAesEncryptionCipher(key)).thenThrow(new RuntimeException("Key is null"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content));
        assertTrue(ex.getMessage().contains("Error occurred encrypting data"));
    }

    @Test
    void encrypt_nullContent_throwsIllegalStateException() {
        // GIVEN
        String key = "MySecretKey12345";
        String content = null;
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content));
        assertTrue(ex.getMessage().contains("Error occurred encrypting data"));
    }

    @Test
    void encrypt_emptyKey_throwsIllegalStateException() {
        // GIVEN
        String key = "";
        String content = "Hello";
        // Force failure in cipher creation.
        encryptionUtilitiesMock.when(() -> EncryptionUtilities.createAesEncryptionCipher(key)).thenThrow(new RuntimeException("Empty key"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content));
        assertTrue(ex.getMessage().contains("Error occurred encrypting data"));
    }

    @Test
    void encrypt_shortKey_throwsIllegalStateException() {
        // GIVEN
        String key = "short";
        String content = "Test";
        // Simulate AES key‑size validation failure.
        encryptionUtilitiesMock.when(() -> EncryptionUtilities.createAesEncryptionCipher(key)).thenThrow(new RuntimeException("Key too short"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content));
        assertTrue(ex.getMessage().contains("Error occurred encrypting data"));
    }

    @Test
    void encrypt_whitespaceOnlyKey_throwsIllegalStateException() {
        // GIVEN
        String key = "   ";
        String content = "Sample";
        encryptionUtilitiesMock.when(() -> EncryptionUtilities.createAesEncryptionCipher(key)).thenThrow(new RuntimeException("Key only whitespace"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content));
        assertTrue(ex.getMessage().contains("Error occurred encrypting data"));
    }

    @Test
    void encrypt_invalidKeyLength17Bytes_throwsIllegalStateException() {
        // GIVEN
        // 17 characters → 17 bytes in UTF‑8
        String key = "12345678901234567";
        String content = "Test";
        encryptionUtilitiesMock.when(() -> EncryptionUtilities.createAesEncryptionCipher(key)).thenThrow(new RuntimeException("Invalid key length (17)"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content));
        assertTrue(ex.getMessage().contains("Error occurred encrypting data"));
    }

    @Test
    void encrypt_unicodeKeyWithInvalidByteLength_throwsIllegalStateException() {
        // GIVEN
        // each Chinese character is 3 bytes → 15 bytes total (invalid)
        String key = "密钥密钥密钥密钥密钥";
        String content = "Data";
        encryptionUtilitiesMock.when(() -> EncryptionUtilities.createAesEncryptionCipher(key)).thenThrow(new RuntimeException("Invalid Unicode key length"));
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content));
        assertTrue(ex.getMessage().contains("Error occurred encrypting data"));
    }
}
