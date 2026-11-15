package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.security.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesCipher_2_Test {

    // 16‑bytes → 128‑bit AES key
    private static final byte[] RAW_KEY = "TestKey12345678".getBytes();

    private static Key secretKey;

    /**
     * Plain text used for encryption/decryption round‑trip verification.
     */
    private static final String PLAINTEXT = "The quick brown fox jumps over the lazy dog";

    @BeforeAll
    static void setupBeforeAll() {
        // Build the SecretKeySpec that will be passed to the method under test.
        secretKey = new SecretKeySpec(RAW_KEY, "AES");
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none in this case).
        secretKey = null;
    }

    /**
     * Helper that creates a {@link SecretKeySpec} from a byte array.
     */
    private static SecretKeySpec secretKey(byte[] bytes) {
        return new SecretKeySpec(bytes, "AES");
    }

    /**
     * Provides a stream of arguments for normal‑operation tests.
     *
     * Each entry contains:
     * <ul>
     *   <li>key bytes (16, 24 or 32 length)</li>
     *   <li>cipher mode (ENCRYPT, DECRYPT, WRAP, UNWRAP)</li>
     * </ul>
     */
    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(Arguments.of(new byte[16], Cipher.ENCRYPT_MODE), Arguments.of(new byte[24], Cipher.ENCRYPT_MODE), Arguments.of(new byte[32], Cipher.ENCRYPT_MODE), Arguments.of(new byte[16], Cipher.DECRYPT_MODE), Arguments.of(new byte[16], Cipher.WRAP_MODE), Arguments.of(new byte[16], Cipher.UNWRAP_MODE));
    }

    /**
     * Utility to convert a hex string to a byte array.
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Verifies that {@code EncryptionUtilities.createAesCipher(Key,int)} returns a Cipher that can
     * encrypt and decrypt data using the supplied key and mode. The test also checks that the IV
     * generation is deterministic – encrypting the same plaintext twice with the same key yields
     * identical ciphertexts.
     */
    @Test
    void testCreateAesCipherEncryptDecryptRoundTrip() throws Exception {
        // Create encryption cipher
        Cipher encryptCipher = EncryptionUtilities.createAesCipher(secretKey, Cipher.ENCRYPT_MODE);
        assertNotNull(encryptCipher, "Encryption cipher should not be null");
        assertEquals("AES/CBC/PKCS5Padding", encryptCipher.getAlgorithm(), "Cipher algorithm should be AES/CBC/PKCS5Padding");
        // Encrypt the plaintext
        byte[] encryptedBytes = encryptCipher.doFinal(PLAINTEXT.getBytes());
        assertNotNull(encryptedBytes, "Encrypted bytes should not be null");
        assertTrue(encryptedBytes.length > 0, "Encrypted bytes should have positive length");
        // Create a second encryption cipher with the same key – because the IV is derived
        // deterministically from the key, the ciphertext must be identical.
        Cipher encryptCipher2 = EncryptionUtilities.createAesCipher(secretKey, Cipher.ENCRYPT_MODE);
        byte[] encryptedBytes2 = encryptCipher2.doFinal(PLAINTEXT.getBytes());
        assertArrayEquals(encryptedBytes, encryptedBytes2, "Ciphertext must be identical for the same key and deterministic IV");
        // Create decryption cipher
        Cipher decryptCipher = EncryptionUtilities.createAesCipher(secretKey, Cipher.DECRYPT_MODE);
        assertNotNull(decryptCipher, "Decryption cipher should not be null");
        // Decrypt and verify round‑trip
        byte[] decryptedBytes = decryptCipher.doFinal(encryptedBytes);
        String decrypted = new String(decryptedBytes);
        assertEquals(PLAINTEXT, decrypted, "Decrypted text must match the original plaintext");
    }

    /**
     * Ensures that an exception is thrown when a {@code null} key is supplied.
     */
    @Test
    void testCreateAesCipherWithNullKey() {
        Exception exception = assertThrows(Exception.class, () -> EncryptionUtilities.createAesCipher(null, Cipher.ENCRYPT_MODE));
        assertNotNull(exception, "Exception should be thrown for null key");
    }

    /**
     * Checks that the method works for both encryption and decryption modes.
     */
    @Test
    void testCreateAesCipherSupportsBothModes() throws Exception {
        Cipher enc = EncryptionUtilities.createAesCipher(secretKey, Cipher.ENCRYPT_MODE);
        Cipher dec = EncryptionUtilities.createAesCipher(secretKey, Cipher.DECRYPT_MODE);
        assertNotNull(enc);
        assertNotNull(dec);
        // Simple sanity check: encrypt then decrypt using the two ciphers.
        byte[] cipherText = enc.doFinal(PLAINTEXT.getBytes());
        byte[] plainBytes = dec.doFinal(cipherText);
        assertEquals(PLAINTEXT, new String(plainBytes));
    }

    @ParameterizedTest(name = "keyLength={0.length}, mode={1}")
    @MethodSource("normalOperationProvider")
    void testCreateAesCipherNormalOperation(byte[] keyBytes, int mode) throws Exception {
        // arrange
        SecretKeySpec key = secretKey(keyBytes);
        // act
        Cipher cipher = EncryptionUtilities.createAesCipher(key, mode);
        // assert
        assertNotNull(cipher, "Cipher must not be null");
        assertEquals("AES/CBC/PKCS5Padding", cipher.getAlgorithm(), "Algorithm must be AES/CBC/PKCS5Padding");
        // Verify that the IV is the MD5 digest of the key bytes
        MessageDigest md5 = EncryptionUtilities.getMD5Digest();
        byte[] expectedIv = md5.digest(keyBytes);
        assertArrayEquals(expectedIv, cipher.getIV(), "IV must be MD5(keyBytes)");
        // Additional sanity check for ENCRYPT/DECRYPT modes – round‑trip
        if (mode == Cipher.ENCRYPT_MODE) {
            Cipher decrypt = EncryptionUtilities.createAesCipher(key, Cipher.DECRYPT_MODE);
            byte[] encrypted = cipher.doFinal(PLAINTEXT.getBytes());
            byte[] decrypted = decrypt.doFinal(encrypted);
            assertEquals(PLAINTEXT, new String(decrypted), "Decrypted text must match original plaintext");
        }
    }

    @Test
    void testCreateAesCipherKeyWithNullEncodedBytes() {
        // custom Key that returns null from getEncoded()
        Key badKey = new Key() {

            @Override
            public String getAlgorithm() {
                return "AES";
            }

            @Override
            public String getFormat() {
                return "RAW";
            }

            @Override
            public byte[] getEncoded() {
                return null;
            }
        };
        Exception ex = assertThrows(NullPointerException.class, () -> EncryptionUtilities.createAesCipher(badKey, Cipher.ENCRYPT_MODE));
        assertNotNull(ex);
    }

    @Test
    void testCreateAesCipherInvalidKeyLength() {
        // 64‑bit, invalid for AES
        SecretKeySpec shortKey = secretKey(new byte[8]);
        assertThrows(InvalidKeyException.class, () -> EncryptionUtilities.createAesCipher(shortKey, Cipher.ENCRYPT_MODE));
    }

    @Test
    void testCreateAesCipherUnsupportedMode() {
        SecretKeySpec key = secretKey(new byte[16]);
        // not a defined Cipher mode
        int unsupportedMode = 999;
        Exception ex = assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.createAesCipher(key, unsupportedMode));
        assertNotNull(ex);
    }

    @Test
    void testCreateAesCipherKeyAlgorithmNotAES() {
        // SecretKeySpec with algorithm "DES"
        SecretKeySpec desKey = new SecretKeySpec(new byte[8], "DES");
        assertThrows(InvalidKeyException.class, () -> EncryptionUtilities.createAesCipher(desKey, Cipher.ENCRYPT_MODE));
    }

    @Test
    void testCreateAesCipherZeroLengthKey() {
        SecretKeySpec zeroKey = secretKey(new byte[0]);
        assertThrows(InvalidKeyException.class, () -> EncryptionUtilities.createAesCipher(zeroKey, Cipher.ENCRYPT_MODE));
    }

    @Test
    void testCreateAesCipherNegativeMode() {
        SecretKeySpec key = secretKey(new byte[16]);
        assertThrows(IllegalArgumentException.class, () -> EncryptionUtilities.createAesCipher(key, -1));
    }

    @Test
    void testCreateAesCipherKeyGetEncodedThrowsRuntimeException() {
        Key explodingKey = new Key() {

            @Override
            public String getAlgorithm() {
                return "AES";
            }

            @Override
            public String getFormat() {
                return "RAW";
            }

            @Override
            public byte[] getEncoded() {
                throw new RuntimeException("boom");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> EncryptionUtilities.createAesCipher(explodingKey, Cipher.ENCRYPT_MODE));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void testCreateAesCipherWhenMD5DigestUnavailable() {
        SecretKeySpec key = secretKey(new byte[16]);
        try (MockedStatic<EncryptionUtilities> mocked = Mockito.mockStatic(EncryptionUtilities.class)) {
            mocked.when(EncryptionUtilities::getMD5Digest).thenThrow(new NoSuchAlgorithmException("MD5 not available"));
            Exception ex = assertThrows(NoSuchAlgorithmException.class, () -> EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE));
            assertEquals("MD5 not available", ex.getMessage());
        }
    }

    @Test
    void testCreateAesCipherWhenCipherGetInstanceFails() throws Exception {
        SecretKeySpec key = secretKey(new byte[16]);
        // Mock static Cipher.getInstance to throw NoSuchAlgorithmException
        try (MockedStatic<Cipher> mockedCipher = Mockito.mockStatic(Cipher.class)) {
            mockedCipher.when(() -> Cipher.getInstance(any())).thenThrow(new NoSuchAlgorithmException("AES/CBC/PKCS5Padding not supported"));
            Exception ex = assertThrows(NoSuchAlgorithmException.class, () -> EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE));
            assertEquals("AES/CBC/PKCS5Padding not supported", ex.getMessage());
        }
    }

    @Test
    void testIvDerivedFromKnownKey() throws Exception {
        // Known 16‑byte key
        byte[] keyBytes = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10 };
        SecretKeySpec key = secretKey(keyBytes);
        Cipher cipher = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
        // Expected MD5 digest of the key (pre‑computed)
        String expectedIvHex = "7e240de74fb5a0a5e5d5b0c8b4b5a8c0";
        byte[] expectedIv = hexStringToByteArray(expectedIvHex);
        assertArrayEquals(expectedIv, cipher.getIV(), "IV must match the MD5 digest of the supplied key");
    }

    @Test
    void testCreateAesCipherWithCustomKeyReturningEncoded16() throws Exception {
        // Custom Key that returns a proper 16‑byte array
        Key customKey = new Key() {

            private final byte[] bytes = new byte[16];

            @Override
            public String getAlgorithm() {
                return "AES";
            }

            @Override
            public String getFormat() {
                return "RAW";
            }

            @Override
            public byte[] getEncoded() {
                return bytes;
            }
        };
        Cipher cipher = EncryptionUtilities.createAesCipher(customKey, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher);
        // Verify IV is MD5 of the custom key bytes (all zeros)
        MessageDigest md5 = EncryptionUtilities.getMD5Digest();
        byte[] expectedIv = md5.digest(customKey.getEncoded());
        assertArrayEquals(expectedIv, cipher.getIV(), "IV must be derived from custom key bytes");
    }
}
