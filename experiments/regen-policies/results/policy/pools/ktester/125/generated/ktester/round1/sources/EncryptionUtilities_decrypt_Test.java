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

    // Common test data
    private static final String KEY = "mySecretKey";

    private static final String WRONG_KEY = "otherSecretKey";

    private static final String PLAINTEXT = "The quick brown fox jumps over the lazy dog";

    // Encrypted representation of PLAINTEXT using KEY; computed before each test
    private String encryptedHex;

    @BeforeEach
    void setupBeforeEach() {
        // Produce a fresh encrypted value for each test to avoid cross‑test contamination
        encryptedHex = EncryptionUtilities.encrypt(KEY, PLAINTEXT);
        assertNotNull(encryptedHex, "Encryption should produce a non‑null hex string");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the instance field – not strictly necessary but keeps the test tidy
        encryptedHex = null;
    }

    /**
     * Provides arguments for {@link #decrypt_EdgeCases(String, String, Class)}.
     * Each {@code Arguments} entry contains:
     * <ul>
     *   <li>key – the decryption key (may be empty, short, null, etc.)</li>
     *   <li>hexStr – the ciphertext (may be empty, malformed, too short, etc.)</li>
     *   <li>expectedException – the type of exception we expect, or {@code null}
     *        if a successful result is expected.</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> edgeCaseProvider() {
        return Stream.of(// Empty key – cipher creation fails
        arguments("", "d4f5c2e1a9b8c7d6e5f4a3b2c1d0e9f8", IllegalStateException.class), // Short key – InvalidKeyException wrapped
        arguments("short", "d4f5c2e1a9b8c7d6e5f4a3b2c1d0e9f8", IllegalStateException.class), // Empty hex string – should decrypt to empty string (no exception)
        arguments("mySecretKey123", "", null), // Malformed hex string – decoding fails
        arguments("mySecretKey123", "invalidhex", IllegalStateException.class), // Valid hex but not a proper AES block size – BadPaddingException wrapped
        arguments("mySecretKey123", "00ff", IllegalStateException.class), // Null key – NPE wrapped
        arguments(null, "d4f5c2e1a9b8c7d6e5f4a3b2c1d0e9f8", IllegalStateException.class), // Null hexStr – NPE wrapped
        arguments("mySecretKey123", null, IllegalStateException.class));
    }

    @Test
    void decrypt_WithValidKeyAndCiphertext_ReturnsOriginalPlaintext() {
        // given – encryptedHex is prepared in @BeforeEach
        // when
        String result = EncryptionUtilities.decrypt(KEY, encryptedHex);
        // then
        assertEquals(PLAINTEXT, result, "Decrypted text should match the original plaintext");
    }

    @Test
    void decrypt_WithWrongKey_ThrowsIllegalStateException() {
        // given – encryptedHex was created with KEY, we will use a different key
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(WRONG_KEY, encryptedHex));
        assertNotNull(ex.getCause(), "Underlying cause should be present");
    }

    @Test
    void decrypt_WithMalformedHexString_ThrowsIllegalStateException() {
        // given – a string that is not valid hex
        // contains non‑hex characters
        String malformedHex = "ZXY123";
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(KEY, malformedHex));
        assertNotNull(ex.getCause(), "Underlying cause should be present");
    }

    @Test
    void decrypt_WithNullKey_ThrowsIllegalStateException() {
        // given – a null key
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(null, encryptedHex));
        assertNotNull(ex.getCause(), "Underlying cause should be present");
    }

    @Test
    void decrypt_WithNullCiphertext_ThrowsIllegalStateException() {
        // given – a null ciphertext
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(KEY, null));
        assertNotNull(ex.getCause(), "Underlying cause should be present");
    }

    @ParameterizedTest(name = "[{index}] key=''{0}'', hexStr=''{1}'' → expect {2}")
    @MethodSource("edgeCaseProvider")
    void decrypt_EdgeCases(String key, String hexStr, Class<? extends Throwable> expectedException) {
        // When an exception type is supplied we assert that it is thrown.
        // When the expectedException is null we assert a successful (possibly empty) result.
        if (expectedException != null) {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(key, hexStr), () -> "Expected IllegalStateException for key='" + key + "' and hexStr='" + hexStr + "'");
            assertNotNull(ex.getCause(), "Underlying cause should be present");
        } else {
            // Expected successful decryption – the only scenario in the matrix is empty hexStr → empty string
            String result = EncryptionUtilities.decrypt(key, hexStr);
            assertEquals("", result, "Decryption of an empty hex string should yield an empty string");
        }
    }

    @Test
    void decrypt_WithMismatchedKey_ThrowsIllegalStateException() {
        // Produce a ciphertext with a known good key
        String goodKey = "mySecretKey123";
        String badKey = "differentSecretKey";
        String plaintext = "HelloWorld";
        String ciphertext = EncryptionUtilities.encrypt(goodKey, plaintext);
        assertNotNull(ciphertext, "Ciphertext must not be null");
        // Decrypting with a different key must fail
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decrypt(badKey, ciphertext));
        assertNotNull(ex.getCause(), "Underlying cause should be present");
    }
}
