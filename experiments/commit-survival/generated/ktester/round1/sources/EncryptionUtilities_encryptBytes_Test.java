package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Random;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_encryptBytes_Test {

    /**
     * Mocked static scope for {@link EncryptionUtilities}.
     */
    private MockedStatic<EncryptionUtilities> encryptionUtilitiesMock;

    /**
     * Mocked static scope for {@link ByteUtilities}.
     */
    private MockedStatic<ByteUtilities> byteUtilitiesMock;

    /**
     * Functional tests that exercise the *real* encryption/decryption logic.
     * <p>
     * No static mocking is used here – we rely on the actual implementations of
     * {@code EncryptionUtilities.createAesEncryptionCipher} and {@code ByteUtilities.encode}.
     * These tests verify that the output of {@code encryptBytes} can be round‑tripped
     * with {@code decryptBytes} for a variety of keys and payload sizes.
     */
    class EncryptionUtilities_encryptBytes_FunctionalTest {

        /**
         * Helper that creates a byte array filled with deterministic pseudo‑random data.
         */
        private static byte[] randomBytes(int length, int seed) {
            Random rnd = new Random(seed);
            byte[] data = new byte[length];
            rnd.nextBytes(data);
            return data;
        }

        /**
         * Provides all successful‑path test data (key, content).
         */
        private static Stream<Arguments> successfulCases() {
            return Stream.of(// Normal small payload
            Arguments.of("mySecretKey12345", new byte[] { 1, 2, 3, 4, 5 }), // Empty payload
            Arguments.of("anotherKey!@#456", new byte[0]), // 1 KB random payload, 32‑char key
            Arguments.of("longKeyForTestingPurposesThatIsExactly32Chars!", randomBytes(1024, 1)), // Empty key
            Arguments.of("", new byte[] { 10, 20, 30 }), // Unicode key
            Arguments.of("ключ🔑", new byte[] { 42, 43, 44 }), // Short key (less than 16 bytes)
            Arguments.of("short", new byte[] { 5, 6, 7, 8 }), // Full‑range byte array (0‑255)
            Arguments.of("fullRangeKey!", fullRangeArray()), // Very large payload (64 KB)
            Arguments.of("bigPayloadKey123", randomBytes(65536, 2)));
        }

        private static byte[] fullRangeArray() {
            byte[] arr = new byte[256];
            for (int i = 0; i < 256; i++) {
                arr[i] = (byte) i;
            }
            return arr;
        }

        @ParameterizedTest(name = "[{index}] key=''{0}'' payloadLength={1.length}")
        @MethodSource("successfulCases")
        void encryptBytes_roundTrip_success(String key, byte[] content) {
            // ----- WHEN ----------------------------------------------------------
            String encrypted = EncryptionUtilities.encryptBytes(key, content);
            // ----- THEN ----------------------------------------------------------
            assertNotNull(encrypted, "Encrypted string must not be null");
            assertFalse(encrypted.isEmpty(), "Encrypted string must not be empty");
            // Decrypt and verify round‑trip.
            byte[] decrypted = EncryptionUtilities.decryptBytes(key, encrypted);
            assertArrayEquals(content, decrypted, "Decrypted bytes must be identical to the original payload");
        }

        @Test
        void encryptBytes_throwsIllegalStateException_whenContentIsNull() {
            // ----- GIVEN ---------------------------------------------------------
            String key = "validKey123456";
            byte[] content = null;
            // ----- WHEN & THEN ---------------------------------------------------
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(key, content), "Null content should result in IllegalStateException");
            assertNotNull(ex.getCause(), "The cause of the IllegalStateException should be the original exception");
        }

        @Test
        void encryptBytes_throwsIllegalStateException_whenKeyIsNull() {
            // ----- GIVEN ---------------------------------------------------------
            String key = null;
            byte[] content = new byte[] { 1, 2, 3 };
            // ----- WHEN & THEN ---------------------------------------------------
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(key, content), "Null key should result in IllegalStateException");
            assertNotNull(ex.getCause(), "The cause of the IllegalStateException should be the original exception");
        }

        @Test
        void encryptBytes_throwsIllegalStateException_whenKeyIsEmptyAndContentIsNull() {
            // ----- GIVEN ---------------------------------------------------------
            String key = "";
            byte[] content = null;
            // ----- WHEN & THEN ---------------------------------------------------
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(key, content), "Empty key with null content must still wrap the underlying exception");
            assertNotNull(ex.getCause(), "The cause should be the original NullPointerException (or similar)");
        }

        @Test
        void encryptBytes_roundTrip_withKeyContainingEmbeddedNullChar() {
            // ----- GIVEN ---------------------------------------------------------
            String key = "invalid\u0000key";
            byte[] content = new byte[] { 1, 2, 3 };
            // ----- WHEN ----------------------------------------------------------
            String encrypted = EncryptionUtilities.encryptBytes(key, content);
            assertNotNull(encrypted);
            assertFalse(encrypted.isEmpty());
            // ----- THEN ----------------------------------------------------------
            byte[] decrypted = EncryptionUtilities.decryptBytes(key, encrypted);
            assertArrayEquals(content, decrypted, "Decryption must succeed even when the key contains an embedded null character");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // CALLS_REAL_METHODS allows us to stub only the methods we care about.
        encryptionUtilitiesMock = mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS);
        byteUtilitiesMock = mockStatic(ByteUtilities.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Close static mocks to avoid leaking state between tests.
        if (encryptionUtilitiesMock != null) {
            encryptionUtilitiesMock.close();
        }
        if (byteUtilitiesMock != null) {
            byteUtilitiesMock.close();
        }
    }

    @Test
    void encryptBytes_returnsEncodedCipherText_whenAllOperationsSucceed() throws Exception {
        // ----- GIVEN ---------------------------------------------------------
        String key = "mySecretKey";
        byte[] content = "Hello, world!".getBytes(StandardCharsets.UTF_8);
        byte[] encryptedBytes = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        String expectedEncoded = "deadbeef";
        // Mock a Cipher that returns the predefined encrypted bytes.
        Cipher mockCipher = mock(Cipher.class);
        when(mockCipher.doFinal(eq(content))).thenReturn(encryptedBytes);
        // Stub the static factory method to return our mock Cipher.
        encryptionUtilitiesMock.when(() -> EncryptionUtilities.createAesEncryptionCipher(eq(key))).thenReturn(mockCipher);
        // Stub the static encoder to return a known string.
        byteUtilitiesMock.when(() -> ByteUtilities.encode(eq(encryptedBytes))).thenReturn(expectedEncoded);
        // ----- WHEN ----------------------------------------------------------
        String actual = EncryptionUtilities.encryptBytes(key, content);
        // ----- THEN ----------------------------------------------------------
        assertEquals(expectedEncoded, actual, "The encoded string should be returned unchanged.");
        // Verify that the Cipher was asked to encrypt the exact payload.
        verify(mockCipher, times(1)).doFinal(eq(content));
        // Verify that the static helpers were each called exactly once.
        encryptionUtilitiesMock.verify(() -> EncryptionUtilities.createAesEncryptionCipher(eq(key)), times(1));
        byteUtilitiesMock.verify(() -> ByteUtilities.encode(eq(encryptedBytes)), times(1));
    }

    @Test
    void encryptBytes_wrapsExceptionInIllegalStateException_whenCipherCreationFails() {
        // ----- GIVEN ---------------------------------------------------------
        String key = "badKey";
        byte[] content = "payload".getBytes(StandardCharsets.UTF_8);
        RuntimeException cause = new RuntimeException("cipher creation failure");
        // Force the static factory to throw an exception.
        encryptionUtilitiesMock.when(() -> EncryptionUtilities.createAesEncryptionCipher(eq(key))).thenThrow(cause);
        // ----- WHEN & THEN ---------------------------------------------------
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.encryptBytes(key, content), "encryptBytes should wrap any exception in IllegalStateException");
        assertSame(cause, thrown.getCause(), "The original exception should be set as the cause of the IllegalStateException");
    }
}
