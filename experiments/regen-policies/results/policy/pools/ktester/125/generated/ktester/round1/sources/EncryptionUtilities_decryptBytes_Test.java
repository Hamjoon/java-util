package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchPaddingException;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_decryptBytes_Test {

    /**
     * Test data used by several test cases
     */
    private static final String KEY = "testPassword123";

    private static final String OTHER_KEY = "differentPassword456";

    private static final byte[] ORIGINAL_BYTES = "The quick brown fox jumps over the lazy dog".getBytes();

    /**
     * Supplies all test cases defined in the JSON matrix.
     *
     * @return a {@link Stream} of {@link Arguments} where each argument list is:
     *         <ul>
     *           <li>key (String, may be {@code null})</li>
     *           <li>hexStr (String, may be {@code null})</li>
     *           <li>expected result – either a {@code byte[]} or {@code IllegalStateException.class}</li>
     *           <li>description (String) – used as the display name</li>
     *         </ul>
     */
    private static Stream<Arguments> provideTestCases() {
        return Stream.of(// ---------- Successful decryption scenarios ----------
        Arguments.of("0123456789abcdef0123456789abcdef", "5f8d3c2a1b0e", new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF }, "Valid 256‑bit key and well‑formed hex string"), Arguments.of("00112233445566778899aabbccddeeff", "", new byte[] {}, "Empty hex string → empty byte array"), // ---------- Error handling (exception translation) ----------
        Arguments.of("validkey1234567890abcdef12345678", "ZZZZ", IllegalStateException.class, "Hex string contains non‑hex characters – ByteUtilities.decode throws IllegalArgumentException"), Arguments.of(null, "5f8d3c2a1b0e", IllegalStateException.class, "Null key – createAesDecryptionCipher throws NullPointerException"), Arguments.of("short", "5f8d3c2a1b0e", IllegalStateException.class, "Key too short – createAesDecryptionCipher throws InvalidKeyException"), Arguments.of("validkey1234567890abcdef12345678", "5f8d3c2a1b0e", IllegalStateException.class, "Cipher.doFinal throws BadPaddingException (corrupted ciphertext)"), // ---------- Key length variations ----------
        Arguments.of("0011223344556677", "a1b2c3d4e5f6", new byte[] { (byte) 0xAA, (byte) 0xBB, (byte) 0xCC }, "128‑bit key with valid hex string"), Arguments.of("00112233445566778899aabb", "deadbeef", new byte[] { 0x11, 0x22, 0x33, 0x44 }, "192‑bit key with valid hex string"), // ---------- Hex string format edge cases ----------
        Arguments.of("0123456789abcdef0123456789abcdef", "5F8D3C2A1B0E", new byte[] { (byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF }, "Upper‑case hex characters"), Arguments.of("0123456789abcdef0123456789abcdef", "abc", IllegalStateException.class, "Odd number of hex characters – decode throws IllegalArgumentException"), Arguments.of("0123456789abcdef0123456789abcdef", "5f 8d 3c 2a 1b 0e", IllegalStateException.class, "Whitespace in hex string – decode throws IllegalArgumentException"), Arguments.of("0123456789abcdef0123456789abcdef", null, IllegalStateException.class, "Null hexStr – decode throws NullPointerException"), // ---------- Null and invalid key handling ----------
        Arguments.of("", "5f8d3c2a1b0e", IllegalStateException.class, "Empty key string – InvalidKeyException from cipher creation"), Arguments.of("g1234567890abcdef0123456789abcdef", "5f8d3c2a1b0e", IllegalStateException.class, "Key contains non‑hex character 'g' – InvalidKeyException"), // ---------- Large ciphertext handling ----------
        Arguments.of("0123456789abcdef0123456789abcdef", "a1b2c3d4e5f6", generateLargePlaintext(), "Large ciphertext (1024 bytes) – deterministic mock response"), // ---------- Cipher initialization exception scenarios ----------
        Arguments.of("0123456789abcdef0123456789abcdef", "deadbeef", IllegalStateException.class, "createAesDecryptionCipher throws NoSuchAlgorithmException"), Arguments.of("0123456789abcdef0123456789abcdef", "deadbeef", IllegalStateException.class, "createAesDecryptionCipher throws NoSuchPaddingException"), Arguments.of("0123456789abcdef0123456789abcdef", "deadbeef", IllegalStateException.class, "createAesDecryptionCipher throws InvalidAlgorithmParameterException"), // ---------- Cipher.doFinal exception scenarios ----------
        Arguments.of("0123456789abcdef0123456789abcdef", "deadbeef", IllegalStateException.class, "Cipher.doFinal throws IllegalBlockSizeException"), // ---------- ByteUtilities.decode unexpected runtime exception ----------
        Arguments.of("0123456789abcdef0123456789abcdef", "deadbeef", IllegalStateException.class, "ByteUtilities.decode throws RuntimeException (ArrayIndexOutOfBoundsException)"));
    }

    /**
     * Converts a hex string (without any prefix) to a byte array.
     *
     * @param hex hex string – may be empty but not null
     * @return byte array representation
     */
    private static byte[] hexStringToBytes(String hex) {
        if (hex.isEmpty()) {
            return new byte[0];
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Invalid hex character");
            }
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }

    /**
     * Generates a deterministic 1024‑byte array used for the “large ciphertext”
     * test case.
     *
     * @return byte array containing values 0x00, 0x01, …, 0xFF repeated four times
     */
    private static byte[] generateLargePlaintext() {
        byte[] result = new byte[1024];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (i % 256);
        }
        return result;
    }

    /**
     * Maps the textual description of a test case to the concrete exception that
     * should be thrown when {@code EncryptionUtilities.createAesDecryptionCipher}
     * is invoked.
     *
     * @param description test case description
     * @return an {@link Exception} instance matching the scenario
     */
    private static Exception mapCreationException(String description) {
        if (description.toLowerCase().contains("null key")) {
            return new NullPointerException("key is null");
        }
        if (description.toLowerCase().contains("key too short") || description.toLowerCase().contains("empty key") || description.toLowerCase().contains("non‑hex character")) {
            return new InvalidKeyException("invalid key");
        }
        if (description.toLowerCase().contains("nosuchalgorithmexception")) {
            return new NoSuchAlgorithmException("algorithm not available");
        }
        if (description.toLowerCase().contains("nosuchpaddingexception")) {
            return new NoSuchPaddingException("padding not available");
        }
        if (description.toLowerCase().contains("invalidalgorithmparameterexception")) {
            return new InvalidAlgorithmParameterException("bad parameters");
        }
        // default fallback
        return new IllegalArgumentException("unexpected creation error");
    }

    /**
     * Maps the textual description of a test case to the concrete exception that
     * should be thrown when {@code Cipher#doFinal} is invoked.
     *
     * @param description test case description
     * @return an {@link Exception} instance matching the scenario, or {@code null}
     *         if the scenario does not involve a Cipher failure.
     */
    private static Exception mapCipherException(String description) {
        String lower = description.toLowerCase();
        if (lower.contains("badpaddingexception")) {
            return new BadPaddingException("bad padding");
        }
        if (lower.contains("illegalblocksizeexception")) {
            return new IllegalBlockSizeException("illegal block size");
        }
        if (lower.contains("byteutilities.decode throws illegalargumentexception")) {
            // This case is handled by stubbing ByteUtilities.decode directly.
            return null;
        }
        if (lower.contains("byteutilities.decode throws runtimeexception")) {
            return new RuntimeException(new ArrayIndexOutOfBoundsException("simulated bug"));
        }
        // default fallback – simulate a generic decryption failure
        return new BadPaddingException("generic decryption error");
    }

    /**
     * Verify that a byte array encrypted with {@code encryptBytes} can be
     * successfully decrypted with {@code decryptBytes} using the same key.
     */
    @Test
    void decryptBytes_returnsOriginalBytes_whenKeyIsCorrect() {
        // given: encrypt the original bytes
        String encryptedHex = EncryptionUtilities.encryptBytes(KEY, ORIGINAL_BYTES);
        // when: decrypt using the same key
        byte[] decrypted = EncryptionUtilities.decryptBytes(KEY, encryptedHex);
        // then: the decrypted bytes must be equal to the original
        assertArrayEquals(ORIGINAL_BYTES, decrypted, "Decrypted bytes should match the original input when the correct key is used");
    }

    /**
     * Verify that an invalid hex string causes an {@link IllegalStateException}
     * to be thrown (the underlying {@code ByteUtilities.decode} will fail).
     */
    @Test
    void decryptBytes_throwsIllegalStateException_whenHexIsInvalid() {
        // given: a malformed hex string
        String badHex = "ZZZZ_not_a_hex";
        // when / then: decrypt should wrap the exception in IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes(KEY, badHex));
        assertNotNull(ex.getCause(), "The cause of the IllegalStateException should contain the original decoding exception");
    }

    /**
     * Verify that using a different key for decryption results in a failure.
     * The AES decryption will throw a {@code BadPaddingException} which is wrapped
     * in {@link IllegalStateException}.
     */
    @Test
    void decryptBytes_throwsIllegalStateException_whenKeyIsIncorrect() {
        // given: encrypt with the original key
        String encryptedHex = EncryptionUtilities.encryptBytes(KEY, ORIGINAL_BYTES);
        // when / then: attempt to decrypt with a different key
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes(OTHER_KEY, encryptedHex));
        assertNotNull(ex.getCause(), "Decryption with a wrong key should result in an exception wrapped in IllegalStateException");
    }

    /**
     * Parameterised test that covers successful decryption scenarios,
     * error‑translation cases and edge‑cases.  The {@link #provideTestCases()}
     * method supplies a {@link Arguments} object for each JSON entry.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideTestCases")
    void decryptBytes_variousScenarios(String key, String hexStr, Object expected, String description) throws Exception {
        // -----------------------------------------------------------------
        // 1️⃣  Set up mocks for the static collaborators
        // -----------------------------------------------------------------
        try (MockedStatic<EncryptionUtilities> mockedEncryption = mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS);
            MockedStatic<ByteUtilities> mockedByteUtil = mockStatic(ByteUtilities.class)) {
            // By default, keep the real implementation of everything else.
            // Only stub the two static methods we need to control.
            // -----------------------------------------------------------------
            // Prepare the decoded byte array (the “ciphertext” that the Cipher will see)
            // -----------------------------------------------------------------
            byte[] decodedBytes = (hexStr == null) ? null : hexStringToBytes(hexStr);
            // When hexStr is null we let ByteUtilities.decode throw NPE later.
            // -----------------------------------------------------------------
            // Stub ByteUtilities.decode – either return a byte[] or throw
            // -----------------------------------------------------------------
            if (expected instanceof Class && ((Class<?>) expected).equals(IllegalStateException.class) && description.toLowerCase().contains("byteutilities.decode")) {
                // Simulate a decoding failure (e.g., illegal argument, runtime exception)
                mockedByteUtil.when(() -> ByteUtilities.decode(hexStr)).thenThrow(new IllegalArgumentException("bad hex"));
            } else if (hexStr != null) {
                mockedByteUtil.when(() -> ByteUtilities.decode(hexStr)).thenReturn(decodedBytes);
            }
            // -----------------------------------------------------------------
            // Stub createAesDecryptionCipher – either return a mock Cipher or throw
            // -----------------------------------------------------------------
            Cipher mockCipher = mock(Cipher.class);
            if (expected instanceof Class && ((Class<?>) expected).equals(IllegalStateException.class) && description.toLowerCase().contains("createaesdecryptioncipher")) {
                // Simulate an exception while creating the Cipher
                Exception creationEx = mapCreationException(description);
                mockedEncryption.when(() -> EncryptionUtilities.createAesDecryptionCipher(key)).thenThrow(creationEx);
            } else {
                // Normal path – return a mock Cipher that will behave as the test expects
                mockedEncryption.when(() -> EncryptionUtilities.createAesDecryptionCipher(key)).thenReturn(mockCipher);
            }
            // -----------------------------------------------------------------
            // Stub Cipher#doFinal – either return expected bytes or throw
            // -----------------------------------------------------------------
            if (expected instanceof byte[]) {
                // Successful decryption – return the expected plaintext
                when(mockCipher.doFinal(decodedBytes)).thenReturn((byte[]) expected);
            } else if (expected instanceof Class && ((Class<?>) expected).equals(IllegalStateException.class)) {
                // Simulate a Cipher failure (BadPaddingException, IllegalBlockSizeException, etc.)
                Exception cipherEx = mapCipherException(description);
                if (cipherEx != null) {
                    when(mockCipher.doFinal(decodedBytes)).thenThrow(cipherEx);
                } else {
                    // If we are testing a creation‑time exception, the mockCipher is never used.
                }
            }
            // -----------------------------------------------------------------
            // 2️⃣  Invoke the method under test
            // -----------------------------------------------------------------
            if (expected instanceof byte[]) {
                byte[] actual = EncryptionUtilities.decryptBytes(key, hexStr);
                assertArrayEquals((byte[]) expected, actual, "Decrypted bytes differ from expected for case: " + description);
            } else if (expected instanceof Class && ((Class<?>) expected).equals(IllegalStateException.class)) {
                IllegalStateException ise = assertThrows(IllegalStateException.class, () -> EncryptionUtilities.decryptBytes(key, hexStr), "Expected IllegalStateException for case: " + description);
                assertNotNull(ise.getCause(), "Cause should be present for IllegalStateException in case: " + description);
            } else {
                fail("Test data configuration error – unexpected expected value: " + expected);
            }
        }
    }
}
