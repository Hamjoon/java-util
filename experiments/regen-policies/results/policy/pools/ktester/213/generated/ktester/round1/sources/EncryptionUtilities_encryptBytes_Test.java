package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_encryptBytes_Test {

    /**
     * Stream of successful encryption cases.
     */
    private static Stream<SuccessCase> successfulEncryptionProvider() {
        return Stream.of(new SuccessCase("mySecretKey123456", new byte[] { 1, 2, 3, 4, 5 }, "Encrypt a normal byte array with a valid 16‑byte key."), new SuccessCase("anotherKey!@#4567", new byte[0], "Encrypt an empty byte array; padding should still produce output."), new // 24‑byte key (AES‑192)
        SuccessCase(// 24‑byte key (AES‑192)
        "123456789012345678901234", new byte[] { 10, 20, 30, 40 }, "Encrypt with a valid 24‑byte key."), new // 32‑byte key (AES‑256)
        SuccessCase(// 32‑byte key (AES‑256)
        "12345678901234567890123456789012", new byte[] { 5, 6, 7, 8, 9 }, "Encrypt with a valid 32‑byte key."), new SuccessCase("mySecretKey123456", new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }, "Encrypt exactly one AES block (16 bytes)."), new SuccessCase("mySecretKey123456", new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 }, "Encrypt 15‑byte payload (one byte short of a block)."), new SuccessCase("mySecretKey123456", new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 }, "Encrypt two full AES blocks (32 bytes)."), new SuccessCase("mySecretKey123456", new byte[1024], "Encrypt a 1 KB zero‑filled array."), new SuccessCase("ключ🔑12345678", new byte[] { 1, 2, 3, 4, 5 }, "Unicode key containing Cyrillic characters and an emoji."));
    }

    /**
     * Stream of cases that should result in IllegalStateException.
     */
    private static Stream<ExceptionCase> exceptionProvider() {
        return Stream.of(new ExceptionCase(null, new byte[] { 1, 2, 3 }, "Null key should cause cipher creation to fail."), new ExceptionCase("short", new byte[] { 10, 20, 30 }, "Key length not suitable for AES should cause InvalidKeyException."), new ExceptionCase("validKey12345678", null, "Null content should cause Cipher.doFinal to throw NPE."), new ExceptionCase("", new byte[] { 1, 2, 3 }, "Empty string key is invalid for AES."), new ExceptionCase(null, null, "Both parameters null should fail during cipher creation."));
    }

    /**
     * Verify that encryptBytes produces a reversible ciphertext.
     * The ciphertext is compared by decrypting it back to the original byte array.
     */
    @Test
    void encryptBytes_shouldReturnReversibleHexString() {
        // given
        String key = "mySecretPassword";
        byte[] original = "The quick brown fox jumps over the lazy dog".getBytes();
        // when
        String encryptedHex = EncryptionUtilities.encryptBytes(key, original);
        // then
        assertNotNull(encryptedHex, "Encrypted hex string must not be null");
        assertFalse(encryptedHex.isEmpty(), "Encrypted hex string must not be empty");
        // round‑trip decryption to verify correctness
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(original, decrypted, "Decrypted bytes must match the original input");
    }

    /**
     * Verify that a null key results in an IllegalStateException.
     * The underlying implementation should throw an exception which is wrapped.
     */
    @Test
    void encryptBytes_withNullKey_shouldThrowIllegalStateException() {
        // given
        String nullKey = null;
        byte[] data = "sample".getBytes();
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(nullKey, data), "Calling encryptBytes with a null key should throw IllegalStateException");
        // optional: verify that the cause is not null (it should be the original exception)
        assertNotNull(ex.getCause(), "The thrown IllegalStateException should have a cause");
    }

    /**
     * Verify that an empty byte array can be encrypted and decrypted without error.
     */
    @Test
    void encryptBytes_withEmptyContent_shouldReturnNonNullHexAndDecryptToEmptyArray() {
        // given
        String key = "anotherKey";
        byte[] empty = new byte[0];
        // when
        String encryptedHex = EncryptionUtilities.encryptBytes(key, empty);
        // then
        assertNotNull(encryptedHex, "Encrypted hex for empty content must not be null");
        assertFalse(encryptedHex.isEmpty(), "Encrypted hex for empty content should not be empty");
        // round‑trip
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(empty, decrypted, "Decrypted empty array must be equal to the original empty array");
    }

    /**
     * Verify that encryptBytes returns a non‑null, non‑empty encoded string
     * for all successful scenarios and that decryption yields the original content.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulEncryptionProvider")
    void encryptBytes_successfulCases(SuccessCase tc) {
        // when
        String encrypted = EncryptionUtilities.encryptBytes(tc.key(), tc.content());
        // then
        assertNotNull(encrypted, tc.description() + " – encrypted string must not be null");
        assertFalse(encrypted.isEmpty(), tc.description() + " – encrypted string must not be empty");
        // round‑trip decryption
        byte[] decrypted = EncryptionUtilities.decryptBytes(tc.key(), encrypted);
        assertArrayEquals(tc.content(), decrypted, tc.description() + " – decrypted bytes must match original");
    }

    /**
     * Verify that encryptBytes wraps underlying failures in IllegalStateException.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void encryptBytes_invalidInputs_throwIllegalStateException(ExceptionCase ec) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(ec.key(), ec.content()), ec.description());
        assertNotNull(ex.getCause(), ec.description() + " – the IllegalStateException should contain a cause");
    }

    /**
     * Verify that the input byte array is not mutated by encryptBytes.
     */
    @Test
    void encryptBytes_inputArrayImmutability() {
        // given
        String key = "mySecretKey123456";
        byte[] original = new byte[] { 9, 8, 7, 6, 5 };
        byte[] copyBefore = original.clone();
        // when
        EncryptionUtilities.encryptBytes(key, original);
        // then
        assertArrayEquals(copyBefore, original, "encryptBytes must not modify the supplied byte array");
    }

    /**
     * Verify deterministic output: two consecutive calls with identical parameters
     * must return exactly the same ciphertext string.
     */
    @Test
    void encryptBytes_deterministicOutputConsistency() {
        // given
        String key = "mySecretKey123456";
        byte[] payload = new byte[] { 11, 22, 33, 44, 55 };
        // when
        String first = EncryptionUtilities.encryptBytes(key, payload);
        String second = EncryptionUtilities.encryptBytes(key, payload);
        // then
        assertEquals(first, second, "Two encryptBytes calls with same key/content must produce identical output");
    }
}
