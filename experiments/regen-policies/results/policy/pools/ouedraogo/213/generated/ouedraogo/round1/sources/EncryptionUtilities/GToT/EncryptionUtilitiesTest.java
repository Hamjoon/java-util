package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class EncryptionUtilitiesTest {

    private File tempFile;
    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog";

    /* -------------------------------------------------------------
     * Helper methods
     * ------------------------------------------------------------- */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static byte[] digestBytes(String algorithm, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(data);
        return md.digest();
    }

    /* -------------------------------------------------------------
     * Setup / Teardown
     * ------------------------------------------------------------- */
    @Before
    public void setUp() throws IOException {
        tempFile = File.createTempFile("encUtilTest", ".txt");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        }
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* -------------------------------------------------------------
     * Basic public‑method tests
     * ------------------------------------------------------------- */

    @Test
    public void testFastMD5_Basic() {
        String expected = toHex(digestBytes("MD5", SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8)));
        String actual = EncryptionUtilities.fastMD5(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA1_Basic() {
        String expected = toHex(digestBytes("SHA-1", SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8)));
        String actual = EncryptionUtilities.fastSHA1(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA256_Basic() {
        String expected = toHex(digestBytes("SHA-256", SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8)));
        String actual = EncryptionUtilities.fastSHA256(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA512_Basic() {
        String expected = toHex(digestBytes("SHA-512", SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8)));
        String actual = EncryptionUtilities.fastSHA512(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastMD5_NonExistentFile() {
        File missing = new File("this_file_does_not_exist_12345.tmp");
        assertFalse(missing.exists());
        assertNull(EncryptionUtilities.fastMD5(missing));
    }

    @Test
    public void testCalculateMD5Hash() {
        byte[] bytes = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        String expected = toHex(digestBytes("MD5", bytes));
        String actual = EncryptionUtilities.calculateMD5Hash(bytes);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateMD5Hash_NullInput() {
        assertNull(EncryptionUtilities.calculateMD5Hash(null));
    }

    @Test
    public void testCalculateSHA1Hash() {
        byte[] bytes = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        String expected = toHex(digestBytes("SHA-1", bytes));
        String actual = EncryptionUtilities.calculateSHA1Hash(bytes);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA256Hash() {
        byte[] bytes = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        String expected = toHex(digestBytes("SHA-256", bytes));
        String actual = EncryptionUtilities.calculateSHA256Hash(bytes);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA512Hash() {
        byte[] bytes = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        String expected = toHex(digestBytes("SHA-512", bytes));
        String actual = EncryptionUtilities.calculateSHA512Hash(bytes);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetDigest_ValidAlgorithm() {
        MessageDigest md = EncryptionUtilities.getDigest("MD5");
        assertNotNull(md);
        assertEquals("MD5", md.getAlgorithm());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigest_InvalidAlgorithm() {
        EncryptionUtilities.getDigest("NON_EXISTENT_ALGO");
    }

    @Test
    public void testCreateCipherBytes_128Bits() {
        String key = "mySecretPassword";
        byte[] cipherBytes = EncryptionUtilities.createCipherBytes(key, 128);
        assertNotNull(cipherBytes);
        assertEquals(16, cipherBytes.length); // 128 bits = 16 bytes
    }

    @Test
    public void testCreateCipherBytes_256Bits() {
        String key = "mySecretPassword";
        byte[] cipherBytes = EncryptionUtilities.createCipherBytes(key, 256);
        assertNotNull(cipherBytes);
        assertEquals(32, cipherBytes.length);
    }

    @Test
    public void testCreateAesEncryptionCipher() throws Exception {
        Cipher cipher = EncryptionUtilities.createAesEncryptionCipher("key");
        assertNotNull(cipher);
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode());
    }

    @Test
    public void testCreateAesDecryptionCipher() throws Exception {
        Cipher cipher = EncryptionUtilities.createAesDecryptionCipher("key");
        assertNotNull(cipher);
        assertEquals(Cipher.DECRYPT_MODE, cipher.getMode());
    }

    @Test
    public void testEncryptDecrypt_String() {
        String key = "myKey";
        String plaintext = "Hello, World!";
        String encryptedHex = EncryptionUtilities.encrypt(key, plaintext);
        assertNotNull(encryptedHex);
        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals(plaintext, decrypted);
    }

    @Test
    public void testEncryptDecrypt_EmptyString() {
        String key = "emptyKey";
        String encryptedHex = EncryptionUtilities.encrypt(key, "");
        assertNotNull(encryptedHex);
        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals("", decrypted);
    }

    @Test
    public void testEncryptDecrypt_Bytes() {
        String key = "bytesKey";
        byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        String encryptedHex = EncryptionUtilities.encryptBytes(key, data);
        assertNotNull(encryptedHex);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(data, decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void testEncrypt_NullKey_ShouldThrow() {
        EncryptionUtilities.encrypt(null, "data");
    }

    @Test(expected = IllegalStateException.class)
    public void testDecrypt_InvalidHex_ShouldThrow() {
        EncryptionUtilities.decrypt("key", "not-a-hex-string");
    }

    @Test
    public void testCalculateHash_NullBytes() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    @Test
    public void testCalculateFileHash_DirectUsage() throws Exception {
        // Use the public calculateFileHash directly and compare to fastMD5 result
        try (FileChannel channel = new java.io.FileInputStream(tempFile).getChannel()) {
            String viaFileHash = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getMD5Digest());
            String viaFastMD5 = EncryptionUtilities.fastMD5(tempFile);
            assertEquals(viaFastMD5, viaFileHash);
        }
    }

    /* -------------------------------------------------------------
     * Edge‑case tests for cipher creation with explicit Key object
     * ------------------------------------------------------------- */
    @Test
    public void testCreateAesCipher_WithKeyObject() throws Exception {
        byte[] keyBytes = EncryptionUtilities.createCipherBytes("explicitKey", 128);
        Key secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        Cipher cipher = EncryptionUtilities.createAesCipher(secretKey, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher);
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode());
    }

    @Test(expected = IllegalStateException.class)
    public void testDecrypt_WithWrongKey_ShouldFail() {
        String key1 = "keyOne";
        String key2 = "keyTwo";
        String plaintext = "SecretMessage";
        String encrypted = EncryptionUtilities.encrypt(key1, plaintext);
        // Attempt decryption with a different key – should raise IllegalStateException
        EncryptionUtilities.decrypt(key2, encrypted);
    }
}
