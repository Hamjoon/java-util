package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.crypto.Cipher;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.util.HexFormat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesDecryptionCipher_Test {

    /**
     * Test data **
     */
    private static final String KEY = "mySecretPassword";

    private static final String PLAINTEXT = "The quick brown fox jumps over the lazy dog";

    /**
     * Verify that the returned cipher is configured for AES/CBC/PKCS5Padding
     *  and is in DECRYPT_MODE.
     */
    private static void assertCipherIsValidForDecryption(Cipher cipher) {
        assertEquals(Cipher.DECRYPT_MODE, cipher.getMode(), "Cipher should be in DECRYPT_MODE");
        assertEquals("AES/CBC/PKCS5Padding", cipher.getAlgorithm(), "Algorithm/Mode/Padding mismatch");
    }

    /**
     * Perform a quick encrypt‑then‑decrypt round‑trip using the supplied key.
     *  The method asserts that the decrypted text equals the original plaintext.
     */
    private static void assertRoundTripWorks(String key) throws Exception {
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(key);
        byte[] encrypted = enc.doFinal(PLAINTEXT.getBytes());
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(key);
        byte[] decrypted = dec.doFinal(encrypted);
        assertEquals(PLAINTEXT, new String(decrypted), "Round‑trip with key [" + key + "] failed");
    }

    /**
     * Valid 128‑bit and 256‑bit keys (hex strings, case‑insensitive).
     */
    private static Stream<Arguments> validKeyProvider() {
        return Stream.of(// 128‑bit (32 hex chars)
        Arguments.of("00112233445566778899aabbccddeeff"), // 256‑bit (64 hex chars)
        Arguments.of("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff"), // 192‑bit (48 hex chars) – separate group but also valid
        Arguments.of("00112233445566778899aabbccddeeff0011223344556677"), // Mixed case – should be accepted
        Arguments.of("00112233445566778899AABBCCDDEEFF"));
    }

    /**
     * Keys with illegal length (too short, non‑multiple of 16 bytes, >256‑bit, odd length).
     */
    private static Stream<Arguments> invalidLengthProvider() {
        return Stream.of(// Too short (8 hex chars → 4 bytes)
        Arguments.of("00112233"), // Not a multiple of 16 bytes (36 hex chars → 18 bytes)
        Arguments.of("00112233445566778899aabbccddeeff0011"), // Exceeds 256‑bit (66 hex chars → 33 bytes)
        Arguments.of("00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff00"), // Odd number of hex chars (31 chars)
        Arguments.of("00112233445566778899aabbccddee"), // Odd number of hex chars (17 chars)
        Arguments.of("0011223344556677"));
    }

    /**
     * Keys containing characters that are not hexadecimal digits.
     */
    private static Stream<Arguments> nonHexProvider() {
        return Stream.of(Arguments.of("00112233GG445566778899aabbccddeeff"), Arguments.of("zz112233445566778899aabbccddeeff"), Arguments.of("00112233445566778899aabbccddeeff+"), Arguments.of("00112233445566778899aabbccddeeff-"), Arguments.of("00112233445566778899aabbccddeeff!"), // Hex prefix “0x” / “0X”
        Arguments.of("0x00112233445566778899aabbccddeeff"), Arguments.of("0X00112233445566778899aabbccddeeff"));
    }

    /**
     * Keys that contain any whitespace (leading, trailing or internal).
     */
    private static Stream<Arguments> whitespaceProvider() {
        return Stream.of(// leading space
        Arguments.of(" 00112233445566778899aabbccddeeff"), // trailing space
        Arguments.of("00112233445566778899aabbccddeeff "), // internal spaces
        Arguments.of("0011 2233 4455 6677 8899 aabb ccdd eeff"), // internal tab
        Arguments.of("00112233\t445566778899aabbccddeeff"), // leading newline
        Arguments.of("\n00112233445566778899aabbccddeeff"), // trailing carriage‑return
        Arguments.of("00112233445566778899aabbccddeeff\r"));
    }

    /**
     * Null and empty strings.
     */
    private static Stream<Arguments> nullOrEmptyProvider() {
        return Stream.of(Arguments.of((String) null), Arguments.of(""));
    }

    /**
     * Verify that a Cipher created for decryption can correctly decrypt data that was
     * encrypted with the matching encryption Cipher.
     */
    @Test
    void createAesDecryptionCipher_ShouldDecryptDataEncryptedWithSameKey() throws Exception {
        // GIVEN – an encryption Cipher and an encrypted payload
        Cipher encryptionCipher = EncryptionUtilities.createAesEncryptionCipher(KEY);
        byte[] encryptedBytes = encryptionCipher.doFinal(PLAINTEXT.getBytes());
        // Convert encrypted bytes to hex (the format used by the public API)
        String encryptedHex = HexFormat.of().formatHex(encryptedBytes);
        // WHEN – we obtain a decryption Cipher via the focal method and decrypt the payload
        Cipher decryptionCipher = EncryptionUtilities.createAesDecryptionCipher(KEY);
        byte[] decodedCipherBytes = HexFormat.of().parseHex(encryptedHex);
        byte[] decryptedBytes = decryptionCipher.doFinal(decodedCipherBytes);
        String decryptedText = new String(decryptedBytes);
        // THEN – the decrypted text matches the original plaintext
        assertEquals(PLAINTEXT, decryptedText);
    }

    /**
     * The Cipher returned for decryption must be configured with the correct algorithm,
     * mode and padding (AES/CBC/PKCS5Padding) and be in DECRYPT_MODE.
     */
    @Test
    void createAesDecryptionCipher_ShouldBeConfiguredForDecryption() throws Exception {
        // WHEN
        Cipher cipher = EncryptionUtilities.createAesDecryptionCipher(KEY);
        // THEN – verify configuration
        assertEquals(Cipher.DECRYPT_MODE, cipher.getMode(), "Cipher should be in DECRYPT_MODE");
        assertEquals("AES/CBC/PKCS5Padding", cipher.getAlgorithm(), "Algorithm/Mode/Padding mismatch");
    }

    /**
     * Passing a {@code null} key should result in an exception (the underlying
     * {@code SecretKeySpec} constructor will throw a {@link NullPointerException}).
     */
    @Test
    void createAesDecryptionCipher_NullKey_ShouldThrowException() {
        assertThrows(Exception.class, () -> EncryptionUtilities.createAesDecryptionCipher(null));
    }

    /**
     * Passing an empty string as a key is allowed by the implementation, but it should
     * still produce a usable Cipher. This test ensures that no unexpected exception is
     * thrown and that encryption/decryption round‑trip works.
     */
    @Test
    void createAesDecryptionCipher_EmptyKey_RoundTripWorks() throws Exception {
        final String emptyKey = "";
        final String data = "sample";
        // Encrypt with empty key
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(emptyKey);
        byte[] encrypted = enc.doFinal(data.getBytes());
        // Decrypt with empty key using the focal method
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(emptyKey);
        byte[] decrypted = dec.doFinal(encrypted);
        String result = new String(decrypted);
        assertEquals(data, result);
    }

    @ParameterizedTest(name = "[{index}] valid key: \"{0}\"")
    @MethodSource("validKeyProvider")
    void createAesDecryptionCipher_ValidKey_ShouldReturnConfiguredCipher(String key) throws Exception {
        // WHEN
        Cipher cipher = EncryptionUtilities.createAesDecryptionCipher(key);
        // THEN – cipher is correctly configured
        assertCipherIsValidForDecryption(cipher);
        // AND – a quick encrypt/decrypt round‑trip works (covers key‑to‑IV derivation)
        assertRoundTripWorks(key);
    }

    @ParameterizedTest(name = "[{index}] invalid length key: \"{0}\"")
    @MethodSource("invalidLengthProvider")
    void createAesDecryptionCipher_InvalidLength_ShouldThrowException(String key) {
        // The implementation throws a generic Exception for any validation failure.
        assertThrows(Exception.class, () -> EncryptionUtilities.createAesDecryptionCipher(key), "Expected exception for invalid key length: " + key);
    }

    @ParameterizedTest(name = "[{index}] non‑hex key: \"{0}\"")
    @MethodSource("nonHexProvider")
    void createAesDecryptionCipher_NonHexCharacters_ShouldThrowException(String key) {
        assertThrows(Exception.class, () -> EncryptionUtilities.createAesDecryptionCipher(key), "Expected exception for non‑hex characters in key: " + key);
    }

    @ParameterizedTest(name = "[{index}] whitespace key: \"{0}\"")
    @MethodSource("whitespaceProvider")
    void createAesDecryptionCipher_WhitespaceInKey_ShouldThrowException(String key) {
        assertThrows(Exception.class, () -> EncryptionUtilities.createAesDecryptionCipher(key), "Expected exception for whitespace in key: [" + key + "]");
    }

    @ParameterizedTest(name = "[{index}] null or empty key")
    @MethodSource("nullOrEmptyProvider")
    void createAesDecryptionCipher_NullOrEmpty_ShouldThrowException(String key) {
        // The Javadoc mentions a NullPointerException for null; implementation may
        // also throw for empty strings.  We assert any Exception to stay implementation‑agnostic.
        assertThrows(Exception.class, () -> EncryptionUtilities.createAesDecryptionCipher(key), "Expected exception for null/empty key");
    }
}
