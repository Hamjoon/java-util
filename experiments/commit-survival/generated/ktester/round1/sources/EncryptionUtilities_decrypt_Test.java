package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_decrypt_Test {

    // 16‑byte key works for AES‑128
    private static final String KEY = "TestKey12345678";

    private static final String PLAINTEXT = "The quick brown fox jumps over the lazy dog";

    private static Stream<Arguments> successfulDecryptionProvider() {
        // case 1 – normal payload
        String key1 = "TestKey12345678";
        String plain1 = "HelloWorld";
        String encrypted1 = EncryptionUtilities.encrypt(key1, plain1);
        // case 2 – empty hex string
        String key2 = "AnotherKey987654";
        // empty payload
        String encrypted2 = "";
        return Stream.of(arguments(key1, encrypted1, plain1), arguments(key2, encrypted2, ""));
    }

    private static Stream<Arguments> invalidKeyProvider() {
        return Stream.of(// null key
        arguments((String) null), // 5‑char key – not 16/24/32
        arguments("short"), // >32 chars
        arguments("ThisKeyIsWayTooLongForAESAlgorithm"));
    }

    private static Stream<Arguments> invalidHexProvider() {
        return Stream.of(// null hex string
        arguments((String) null), // non‑hex characters
        arguments("ZZZZ"), // odd number of hex chars
        arguments("ABC"));
    }

    /**
     * Verify that decrypt correctly reverses encrypt for a valid key / payload.
     */
    @Test
    void decryptShouldReturnOriginalPlaintextWhenKeyMatches() {
        // given: an encrypted representation of {@code PLAINTEXT} using {@code KEY}
        String encrypted = EncryptionUtilities.encrypt(KEY, PLAINTEXT);
        // when: we decrypt with the same key
        String result = EncryptionUtilities.decrypt(KEY, encrypted);
        // then: the result must equal the original plaintext
        assertEquals(PLAINTEXT, result);
    }

    /**
     * Decrypting with a different key must fail and be wrapped in an IllegalStateException.
     */
    @Test
    void decryptWithWrongKeyShouldThrowIllegalStateException() {
        // given: ciphertext generated with the correct key
        String encrypted = EncryptionUtilities.encrypt(KEY, PLAINTEXT);
        String wrongKey = "WrongKey87654321";
        // when / then: decryption with a wrong key throws IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(wrongKey, encrypted));
        // underlying cause should be a crypto exception
        assertNotNull(ex.getCause());
    }

    /**
     * Supplying a malformed hex string should also result in an IllegalStateException.
     */
    @Test
    void decryptWithInvalidHexShouldThrowIllegalStateException() {
        // given: an obviously invalid hex string
        String invalidHex = "ZZZZ_not_a_hex";
        // when / then: decryption throws IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(KEY, invalidHex));
        assertNotNull(ex.getCause());
    }

    /**
     * Successful decryption scenarios.
     *
     * <ul>
     *   <li>Decrypt a normal encrypted string.</li>
     *   <li>Decrypt an empty hex string (should yield an empty result).</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] key={0}, hexStr={1}")
    @MethodSource("successfulDecryptionProvider")
    void successfulDecryption(String key, String hexStr, String expected) {
        String result = EncryptionUtilities.decrypt(key, hexStr);
        assertEquals(expected, result);
    }

    /**
     * Invalid key scenarios – null, too short, or too long keys.
     * All must result in an {@link IllegalStateException}.
     */
    @ParameterizedTest(name = "[{index}] key={0}")
    @MethodSource("invalidKeyProvider")
    void invalidKeyScenarios(String key) {
        // ciphertext generated with a *valid* key so that the failure originates
        // from the supplied (invalid) decryption key.
        String validKey = "ValidKey12345678";
        String payload = "payload";
        String cipherText = EncryptionUtilities.encrypt(validKey, payload);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(key, cipherText));
        assertNotNull(ex.getCause());
    }

    /**
     * Invalid hex string scenarios – null, non‑hex characters, odd length.
     * All must result in an {@link IllegalStateException}.
     */
    @ParameterizedTest(name = "[{index}] hexStr={0}")
    @MethodSource("invalidHexProvider")
    void invalidHexScenarios(String hexStr) {
        String validKey = "ValidKey12345678";
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(validKey, hexStr));
        assertNotNull(ex.getCause());
    }

    /**
     * Decryption failure due to mismatched encryption key.
     * The underlying {@code BadPaddingException} is wrapped in {@link IllegalStateException}.
     */
    @Test
    void mismatchedKeyDecryptionShouldThrowIllegalStateException() {
        // ciphertext encrypted with a different key
        String encryptingKey = "OtherKey87654321";
        String decryptingKey = "ValidKey12345678";
        String secretMessage = "SecretMessage";
        String cipherText = EncryptionUtilities.encrypt(encryptingKey, secretMessage);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(decryptingKey, cipherText));
        assertNotNull(ex.getCause());
    }
}
