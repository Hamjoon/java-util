package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class EncryptionUtilities_encrypt_Test {

    /**
     * Supplies a stream of arguments for {@link #encrypt_successful_cases}.
     *
     * <p>Cases include:
     * <ul>
     *   <li>Normal ASCII input.</li>
     *   <li>Unicode characters in both key and content.</li>
     *   <li>Empty key (the utility internally derives a key from the empty string).</li>
     *   <li>Empty content (should encrypt a zero‑length byte array).</li>
     * </ul>
     */
    private static Stream<Arguments> provideSuccessfulEncryptionCases() {
        return Stream.of(Arguments.of("mySecretKey12345", "Hello World"), Arguments.of("短键", "测试中文"), Arguments.of("", "empty key test"), Arguments.of("mySecretKey12345", ""));
    }

    private static Stream<Arguments> provideInvalidKeyLengthCases() {
        return Stream.of(// Key shorter than 16 bytes (AES‑128 minimum)
        Arguments.of("short", "any content"), // Key far longer than 32 bytes (AES‑256 maximum)
        Arguments.of("thisKeyIsWayTooLongForAESAlgorithmAndShouldCauseExceptionBecauseItExceedsMaximumAllowedLength", "any content"));
    }

    private static Stream<Arguments> provideNullInputCases() {
        return Stream.of(Arguments.of(null, "some content"), Arguments.of("validKey123", null), Arguments.of(null, null));
    }

    /**
     * Verify that encrypt() produces a reversible ciphertext.
     * The test encrypts a sample string and then decrypts it,
     * asserting that the original content is recovered.
     */
    @Test
    void encrypt_thenDecrypt_returnsOriginalContent() {
        // given
        String key = "mySecretPassword";
        String original = "The quick brown fox jumps over the lazy dog";
        // when
        String encrypted = EncryptionUtilities.encrypt(key, original);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        // then
        assertNotNull(encrypted, "Encrypted value should not be null");
        assertNotEquals(original, encrypted, "Encrypted value should differ from original");
        assertEquals(original, decrypted, "Decrypted value should match the original content");
    }

    /**
     * Verify that encrypt() throws an IllegalStateException when the underlying
     * cipher creation fails (e.g., a null key). The method catches any Exception
     * and wraps it in an IllegalStateException.
     */
    @Test
    void encrypt_withNullKey_throwsIllegalStateException() {
        // given
        String nullKey = null;
        String content = "sample";
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(nullKey, content));
        assertNotNull(ex.getCause(), "Cause of IllegalStateException should be present");
    }

    /**
     * Verify that encrypt() throws an IllegalStateException when the content is null.
     * The implementation attempts to call getBytes on the content, which will raise
     * a NullPointerException that is wrapped.
     */
    @Test
    void encrypt_withNullContent_throwsIllegalStateException() {
        // given
        String key = "anyKey";
        String nullContent = null;
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, nullContent));
        assertNotNull(ex.getCause(), "Cause of IllegalStateException should be present");
    }

    /**
     * Successful encryption/decryption scenarios.
     *
     * <p>Each argument pair consists of a {@code key} and a {@code content}.
     * The test asserts that:
     * <ul>
     *   <li>The encrypted string is non‑null and non‑empty.</li>
     *   <li>Decrypting the result yields the original content.</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] key=''{0}'', content=''{1}''")
    @MethodSource("provideSuccessfulEncryptionCases")
    void encrypt_successful_cases(String key, String content) {
        // when
        String encrypted = EncryptionUtilities.encrypt(key, content);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        // then
        assertNotNull(encrypted, "Encrypted value must not be null");
        assertFalse(encrypted.isEmpty(), "Encrypted value must not be empty");
        assertEquals(content, decrypted, "Decrypted value must equal the original content");
    }

    /**
     * Encryption should fail with an {@link IllegalStateException} when the
     * supplied key is clearly unsuitable for AES‑128 (too short or too long).
     *
     * <p>The underlying implementation is expected to throw an {@code InvalidKeyException}
     * or {@code IllegalArgumentException}, which is wrapped by the method under test.
     */
    @ParameterizedTest(name = "[{index}] invalid key=''{0}''")
    @MethodSource("provideInvalidKeyLengthCases")
    void encrypt_invalidKeyLength_throwsIllegalStateException(String key, String content) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content), "Expected IllegalStateException for key: " + key);
        assertNotNull(ex.getCause(), "Cause should be present for invalid key");
    }

    /**
     * Null‑input combinations should all result in an {@link IllegalStateException}
     * because the method attempts to operate on the supplied arguments without
     * null‑checking.
     */
    @ParameterizedTest(name = "[{index}] key={0}, content={1}")
    @MethodSource("provideNullInputCases")
    void encrypt_nullInputs_throwsIllegalStateException(String key, String content) {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encrypt(key, content));
        assertNotNull(ex.getCause(), "Cause should be present when null arguments are supplied");
    }
}
