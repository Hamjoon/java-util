package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesDecryptionCipher_Test {

    /**
     * Provider for all test cases that expect a successful Cipher creation.
     */
    private static Stream<String> validKeyProvider() {
        return Stream.of(// 16‑byte key (32‑hex chars)
        "00112233445566778899aabbccddeeff", // 24‑byte key (48‑hex chars)
        "00112233445566778899aabbccddeeff112233445566", // 32‑byte key (64‑hex chars)
        "00112233445566778899aabbccddeeff112233445566778899aabbccddeeff", // mixed‑case hex (16‑byte)
        "aBcDeF00112233445566778899AaBbCc", // leading/trailing whitespace (24‑byte)
        "   00112233445566778899aabbccddeeff112233445566   ", // uppercase hex (32‑byte)
        "A1B2C3D4E5F60718293A4B5C6D7E8F90A1B2C3D4E5F60718293A4B5C6D7E8F9");
    }

    private static Stream<String> illegalArgumentKeyProvider() {
        return Stream.of(// null reference
        null, // empty string
        "", // odd‑length hex
        "ABC", // internal spaces
        "0011 2233 4455 6677 8899 aabb ccdd eeff", // odd number of chars after trim
        "00112233445566778899aabbcde", // 20‑byte (unsupported length)
        "00112233445566778899aabbccddeeff1122", // >64 chars
        "00112233445566778899aabbccddeeff112233445566778899aabbccddeeff00112233445566778899aabbccddeeff", // only whitespace
        "    ", // non‑hex character Z
        "00112233ZZ445566778899aabbccddeeff");
    }

    private static Stream<String> invalidKeyProvider() {
        return Stream.of(// 10‑byte key (20 hex chars) – not 16/24/32
        "00112233445566778899", // 20‑byte key (40 hex chars) – unsupported size
        "00112233445566778899aabbccddeeff1122");
    }

    /**
     * Verify that a Cipher created for decryption can correctly reverse the
     * encryption performed by {@link EncryptionUtilities#createAesEncryptionCipher(String)}.
     * The test encrypts a plain text string, then decrypts the resulting hex string
     * and asserts that the original plain text is recovered.
     */
    @Test
    void testCreateAesDecryptionCipher_RoundTrip() throws Exception {
        // given
        // any non‑null key
        String key = "mySecretKey12345";
        String original = "The quick brown fox jumps over the lazy dog";
        // when: encrypt the original text
        String encryptedHex = EncryptionUtilities.encrypt(key, original);
        // then: decrypt the hex string back to plain text
        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        // verify the round‑trip yields the original content
        assertEquals(original, decrypted, "Decrypted text should match the original plaintext");
    }

    /**
     * Ensure that the decryption cipher can also be used directly via the
     * lower‑level API that returns raw bytes.
     */
    @Test
    void testCreateAesDecryptionCipher_BytesRoundTrip() throws Exception {
        // given
        String key = "anotherSecretKey!";
        byte[] originalBytes = "Binary data \u00A9 \u20AC".getBytes();
        // when: encrypt the byte array
        byte[] encryptedBytes = EncryptionUtilities.encryptBytes(key, originalBytes);
        // encryptBytes returns a hex string, so we need to decode it back to bytes
        // using the public decryptBytes method which internally uses the decryption cipher.
        byte[] decryptedBytes = EncryptionUtilities.decryptBytes(key, EncryptionUtilities.encrypt(key, new String(originalBytes)));
        // then
        assertArrayEquals(originalBytes, decryptedBytes, "Decrypted byte array should be identical to the original");
    }

    /**
     * Verify that providing a null key results in an exception.
     * The exact exception type is not part of the public contract,
     * but we expect some form of IllegalArgumentException or a generic Exception.
     */
    @Test
    void testCreateAesDecryptionCipher_NullKey() {
        // given
        String nullKey = null;
        String someHex = "00";
        // when / then
        Exception ex = assertThrows(Exception.class, () -> EncryptionUtilities.decrypt(nullKey, someHex));
        assertNotNull(ex, "An exception should be thrown when the key is null");
    }

    /**
     * All valid keys should result in a non‑null {@link Cipher} whose
     * transformation is exactly {@code "AES/CBC/PKCS5Padding"}.
     */
    @ParameterizedTest(name = "[{index}] valid key: \"{0}\"")
    @MethodSource("validKeyProvider")
    void testCreateAesDecryptionCipher_ValidKey(String key) throws Exception {
        // when
        Cipher cipher = EncryptionUtilities.createAesDecryptionCipher(key);
        // then
        assertNotNull(cipher, "Cipher must not be null for a valid key");
        assertEquals("AES/CBC/PKCS5Padding", cipher.getAlgorithm(), "Cipher must use the expected AES transformation");
    }

    @ParameterizedTest(name = "[{index}] illegal argument key: \"{0}\"")
    @MethodSource("illegalArgumentKeyProvider")
    void testCreateAesDecryptionCipher_IllegalArgument(String key) {
        // when / then
        Exception ex = assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.createAesDecryptionCipher(key));
        assertNotNull(ex, "Expected IllegalArgumentException for key: " + key);
    }

    @ParameterizedTest(name = "[{index}] invalid key (InvalidKeyException): \"{0}\"")
    @MethodSource("invalidKeyProvider")
    void testCreateAesDecryptionCipher_InvalidKey(String key) {
        // when / then
        Exception ex = assertThrows(InvalidKeyException.class, () -> EncryptionUtilities.createAesDecryptionCipher(key));
        assertNotNull(ex, "Expected InvalidKeyException for key: " + key);
    }

    @Test
    void testCreateAesDecryptionCipher_NoSuchAlgorithm() {
        String key = "00112233445566778899aabbccddeeff";
        // Mock the internal static helper to throw NoSuchAlgorithmException
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> EncryptionUtilities.createAesCipher(Mockito.eq(key), Mockito.eq(Cipher.DECRYPT_MODE))).thenThrow(new NoSuchAlgorithmException("AES algorithm not available"));
            Exception ex = assertThrows(NoSuchAlgorithmException.class, () -> EncryptionUtilities.createAesDecryptionCipher(key));
            assertEquals("AES algorithm not available", ex.getMessage());
        }
    }

    @Test
    void testCreateAesDecryptionCipher_NoSuchPadding() {
        String key = "00112233445566778899aabbccddeeff";
        // Mock the internal static helper to throw NoSuchPaddingException
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> EncryptionUtilities.createAesCipher(Mockito.eq(key), Mockito.eq(Cipher.DECRYPT_MODE))).thenThrow(new NoSuchPaddingException("PKCS5Padding not available"));
            Exception ex = assertThrows(NoSuchPaddingException.class, () -> EncryptionUtilities.createAesDecryptionCipher(key));
            assertEquals("PKCS5Padding not available", ex.getMessage());
        }
    }

    @Test
    void testCreateAesDecryptionCipher_InvalidKeyDueToJCEPolicy() {
        // 32‑byte key
        String key = "00112233445566778899aabbccddeeff112233445566778899aabbccddeeff";
        // Simulate a JVM without unlimited strength jurisdiction policy
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class, Mockito.CALLS_REAL_METHODS)) {
            mocked.when(() -> EncryptionUtilities.createAesCipher(Mockito.eq(key), Mockito.eq(Cipher.DECRYPT_MODE))).thenThrow(new InvalidKeyException("Illegal key size for AES-256"));
            Exception ex = assertThrows(InvalidKeyException.class, () -> EncryptionUtilities.createAesDecryptionCipher(key));
            assertEquals("Illegal key size for AES-256", ex.getMessage());
        }
    }
}
