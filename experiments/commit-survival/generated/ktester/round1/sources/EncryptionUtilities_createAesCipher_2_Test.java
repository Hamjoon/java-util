package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesCipher_2_Test {

    /* -------------------------------------------------------------
     *  Key length variations
     * ------------------------------------------------------------- */
    static Stream<Arguments> keyLengthProvider() {
        return Stream.of(// 24‑byte (AES‑192)
        Arguments.of("0123456789abcdef01234567", Cipher.ENCRYPT_MODE, true), // 32‑byte (AES‑256)
        Arguments.of("0123456789abcdef0123456789abcdef", Cipher.ENCRYPT_MODE, true), // too short
        Arguments.of("00112233", Cipher.ENCRYPT_MODE, false), // zero length
        Arguments.of("", Cipher.ENCRYPT_MODE, false));
    }

    @Test
    void testCreateAesCipher_WithValidKeyAndMode_ReturnsInitializedCipher() throws Exception {
        // GIVEN – a deterministic key and mode
        // 16‑byte key
        byte[] keyBytes = "testkey12345678".getBytes(StandardCharsets.UTF_8);
        Key key = new SecretKeySpec(keyBytes, "AES");
        int mode = Cipher.ENCRYPT_MODE;
        // Expected IV is MD5(keyBytes)
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] expectedIv = md5.digest(keyBytes);
        // WHEN – the method under test is invoked
        Cipher cipher = EncryptionUtilities.createAesCipher(key, mode);
        // THEN – verify the cipher is correctly initialised
        assertNotNull(cipher, "Cipher should not be null");
        assertEquals("AES/CBC/PKCS5Padding", cipher.getAlgorithm(), "Cipher algorithm should be AES/CBC/PKCS5Padding");
        assertArrayEquals(expectedIv, cipher.getIV(), "Cipher IV should be the MD5 digest of the key");
        // The mode is not directly exposed; we verify by performing a simple encryption
        byte[] plain = "hello world".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = cipher.doFinal(plain);
        assertNotNull(encrypted, "Encryption should produce a non‑null result");
        assertTrue(encrypted.length > 0, "Encryption should produce output bytes");
    }

    /* -------------------------------------------------------------
     *  Valid cipher creation (ENCRYPT / DECRYPT)
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_WithValidKeyAndEncryptMode_ReturnsInitializedCipher() throws Exception {
        Key key = new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher);
        assertEquals("AES/CBC/PKCS5Padding", cipher.getAlgorithm());
        // IV must be MD5(key)
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] expectedIv = md5.digest(key.getEncoded());
        assertArrayEquals(expectedIv, cipher.getIV());
    }

    @Test
    void testCreateAesCipher_WithValidKeyAndDecryptMode_ReturnsInitializedCipher() throws Exception {
        Key key = new SecretKeySpec("fedcba9876543210".getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = EncryptionUtilities.createAesCipher(key, Cipher.DECRYPT_MODE);
        assertNotNull(cipher);
        assertEquals("AES/CBC/PKCS5Padding", cipher.getAlgorithm());
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        assertArrayEquals(md5.digest(key.getEncoded()), cipher.getIV());
    }

    /* -------------------------------------------------------------
     *  Invalid mode handling
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_WithUnsupportedMode_ThrowsException() {
        Key key = new SecretKeySpec("0011223344556677".getBytes(StandardCharsets.UTF_8), "AES");
        int unsupportedMode = 9999;
        assertThrows(InvalidAlgorithmParameterException.class, () -> EncryptionUtilities.createAesCipher(key, unsupportedMode));
    }

    /* -------------------------------------------------------------
     *  Null key handling
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_NullKey_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.createAesCipher(null, Cipher.ENCRYPT_MODE));
    }

    /* -------------------------------------------------------------
     *  Key with null encoded bytes
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_KeyWithNullEncoded_ThrowsNPE() {
        Key badKey = new SecretKey() {

            @Override
            public String getAlgorithm() {
                return "AES";
            }

            @Override
            public String getFormat() {
                return null;
            }

            @Override
            public byte[] getEncoded() {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.createAesCipher(badKey, Cipher.ENCRYPT_MODE));
    }

    /* -------------------------------------------------------------
     *  Key with non‑AES algorithm name
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_KeyWithWrongAlgorithm_ThrowsInvalidKey() {
        Key key = new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "DES");
        assertThrows(InvalidKeyException.class, () -> EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE));
    }

    @ParameterizedTest
    @MethodSource("keyLengthProvider")
    void testCreateAesCipher_KeyLengthVariations(String keyHex, int mode, boolean shouldSucceed) {
        Key key = new SecretKeySpec(keyHex.getBytes(StandardCharsets.UTF_8), "AES");
        if (shouldSucceed) {
            assertDoesNotThrow(() -> {
                Cipher c = EncryptionUtilities.createAesCipher(key, mode);
                assertNotNull(c);
            });
        } else {
            assertThrows(InvalidKeyException.class, () -> EncryptionUtilities.createAesCipher(key, mode));
        }
    }

    /* -------------------------------------------------------------
     *  Mode variations (WRAP / UNWRAP)
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_WrapMode_Succeeds() throws Exception {
        Key key = new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES");
        Cipher c = EncryptionUtilities.createAesCipher(key, Cipher.WRAP_MODE);
        assertNotNull(c);
        assertEquals(Cipher.WRAP_MODE, c.getMode());
    }

    @Test
    void testCreateAesCipher_UnwrapMode_Succeeds() throws Exception {
        Key key = new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES");
        Cipher c = EncryptionUtilities.createAesCipher(key, Cipher.UNWRAP_MODE);
        assertNotNull(c);
        assertEquals(Cipher.UNWRAP_MODE, c.getMode());
    }

    /* -------------------------------------------------------------
     *  Deterministic IV generation
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_DeterministicIv_TwoCallsSameIv() throws Exception {
        Key key = new SecretKeySpec("deterministicIV123".getBytes(StandardCharsets.UTF_8), "AES");
        Cipher c1 = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
        Cipher c2 = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
        assertArrayEquals(c1.getIV(), c2.getIV());
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        assertArrayEquals(md5.digest(key.getEncoded()), c1.getIV());
    }

    /* -------------------------------------------------------------
     *  Different keys produce different IVs
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_DifferentKeys_DifferentIv() throws Exception {
        Key key1 = new SecretKeySpec("keyOne123456789".getBytes(StandardCharsets.UTF_8), "AES");
        Key key2 = new SecretKeySpec("keyTwo987654321".getBytes(StandardCharsets.UTF_8), "AES");
        Cipher c1 = EncryptionUtilities.createAesCipher(key1, Cipher.ENCRYPT_MODE);
        Cipher c2 = EncryptionUtilities.createAesCipher(key2, Cipher.ENCRYPT_MODE);
        assertFalse(Arrays.equals(c1.getIV(), c2.getIV()));
    }

    /* -------------------------------------------------------------
     *  Algorithm name case sensitivity
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_LowerCaseAlgorithm_ThrowsInvalidKey() {
        Key key = new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "aes");
        assertThrows(InvalidKeyException.class, () -> EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE));
    }

    /* -------------------------------------------------------------
     *  Key with null format but valid encoded bytes
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_KeyWithNullFormat_Succeeds() throws Exception {
        Key key = new SecretKey() {

            @Override
            public String getAlgorithm() {
                return "AES";
            }

            @Override
            public String getFormat() {
                return null;
            }

            @Override
            public byte[] getEncoded() {
                return "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
            }
        };
        Cipher c = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
        assertNotNull(c);
    }

    /* -------------------------------------------------------------
     *  MessageDigest acquisition failure (MD5 not available)
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_NoMD5Algorithm_PropagatesException() {
        try (MockedStatic<EncryptionUtilities> mocked = mockStatic(EncryptionUtilities.class, CALLS_REAL_METHODS)) {
            mocked.when(EncryptionUtilities::getMD5Digest).thenThrow(new NoSuchAlgorithmException("MD5 not available"));
            Key key = new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES");
            Exception ex = assertThrows(NoSuchAlgorithmException.class, () -> EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE));
            assertEquals("MD5 not available", ex.getMessage());
        }
    }

    /* -------------------------------------------------------------
     *  Cipher instance acquisition failure
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_NoCipherTransformation_PropagatesException() {
        try (MockedStatic<Cipher> mockedCipher = mockStatic(Cipher.class)) {
            mockedCipher.when(() -> Cipher.getInstance("AES/CBC/PKCS5Padding")).thenThrow(new NoSuchPaddingException("Padding not available"));
            Key key = new SecretKeySpec("0123456789abcdef".getBytes(StandardCharsets.UTF_8), "AES");
            Exception ex = assertThrows(NoSuchPaddingException.class, () -> EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE));
            assertEquals("Padding not available", ex.getMessage());
        }
    }

    /* -------------------------------------------------------------
     *  Key.getEncoded throws unchecked exception
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_KeyEncodedThrowsException_Propagates() {
        Key key = new SecretKey() {

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
                throw new UnsupportedOperationException("encoding not supported");
            }
        };
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE));
        assertEquals("encoding not supported", ex.getMessage());
    }

    /* -------------------------------------------------------------
     *  Key with null algorithm name
     * ------------------------------------------------------------- */
    @Test
    void testCreateAesCipher_KeyWithNullAlgorithm_ThrowsInvalidKey() {
        Key key = new SecretKey() {

            @Override
            public String getAlgorithm() {
                return null;
            }

            @Override
            public String getFormat() {
                return "RAW";
            }

            @Override
            public byte[] getEncoded() {
                return "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
            }
        };
        assertThrows(InvalidKeyException.class, () -> EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE));
    }
}
