package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class EncryptionUtilitiesTest {

    private File tempFile;
    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
    private static final String SAMPLE_STRING = "The quick brown fox jumps over the lazy dog";

    @Before
    public void setUp() throws IOException {
        tempFile = Files.createTempFile("encUtilTest", ".txt").toFile();
        Files.write(tempFile.toPath(), SAMPLE_DATA);
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* -------------------------------------------------------------
     *  Hashing helpers – compute expected values using JDK MessageDigest
     * ------------------------------------------------------------- */
    private static String digestHex(String algorithm, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(data);
        return ByteUtilities.encode(md.digest());
    }

    /* ---------------------- fast* file hashing ------------------- */
    @Test
    public void testFastMD5() throws Exception {
        String expected = digestHex("MD5", SAMPLE_DATA);
        String actual = EncryptionUtilities.fastMD5(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA1() throws Exception {
        String expected = digestHex("SHA-1", SAMPLE_DATA);
        String actual = EncryptionUtilities.fastSHA1(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA256() throws Exception {
        String expected = digestHex("SHA-256", SAMPLE_DATA);
        String actual = EncryptionUtilities.fastSHA256(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA512() throws Exception {
        String expected = digestHex("SHA-512", SAMPLE_DATA);
        String actual = EncryptionUtilities.fastSHA512(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastHashOnMissingFileReturnsNull() {
        File missing = new File("nonexistent_file_12345.tmp");
        assertFalse(missing.exists());
        assertNull(EncryptionUtilities.fastMD5(missing));
        assertNull(EncryptionUtilities.fastSHA1(missing));
        assertNull(EncryptionUtilities.fastSHA256(missing));
        assertNull(EncryptionUtilities.fastSHA512(missing));
    }

    /* ------------------- byte[] hash methods -------------------- */
    @Test
    public void testCalculateMD5Hash() throws Exception {
        String expected = digestHex("MD5", SAMPLE_DATA);
        String actual = EncryptionUtilities.calculateMD5Hash(SAMPLE_DATA);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA1Hash() throws Exception {
        String expected = digestHex("SHA-1", SAMPLE_DATA);
        String actual = EncryptionUtilities.calculateSHA1Hash(SAMPLE_DATA);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA256Hash() throws Exception {
        String expected = digestHex("SHA-256", SAMPLE_DATA);
        String actual = EncryptionUtilities.calculateSHA256Hash(SAMPLE_DATA);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA512Hash() throws Exception {
        String expected = digestHex("SHA-512", SAMPLE_DATA);
        String actual = EncryptionUtilities.calculateSHA512Hash(SAMPLE_DATA);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateHashNullInputReturnsNull() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    /* -------------------------- Digest factories -------------------------- */
    @Test
    public void testGetDigestValidAlgorithm() {
        MessageDigest md = EncryptionUtilities.getDigest("SHA-256");
        assertNotNull(md);
        assertEquals("SHA-256", md.getAlgorithm());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigestInvalidAlgorithmThrows() {
        EncryptionUtilities.getDigest("NON-EXISTENT-ALG");
    }

    @Test
    public void testSpecificDigestFactories() {
        assertEquals("MD5", EncryptionUtilities.getMD5Digest().getAlgorithm());
        assertEquals("SHA-1", EncryptionUtilities.getSHA1Digest().getAlgorithm());
        assertEquals("SHA-256", EncryptionUtilities.getSHA256Digest().getAlgorithm());
        assertEquals("SHA-512", EncryptionUtilities.getSHA512Digest().getAlgorithm());
    }

    /* --------------------- Cipher byte creation ---------------------- */
    @Test
    public void testCreateCipherBytesLength() {
        byte[] keyBytes = EncryptionUtilities.createCipherBytes("mySecretPassword", 128);
        assertEquals(16, keyBytes.length); // 128 bits / 8 = 16 bytes
    }

    @Test
    public void testCreateCipherBytesDeterministic() {
        byte[] first = EncryptionUtilities.createCipherBytes("sameKey", 128);
        byte[] second = EncryptionUtilities.createCipherBytes("sameKey", 128);
        assertArrayEquals(first, second);
    }

    /* ----------------------- AES encryption/decryption ----------------------- */
    @Test
    public void testEncryptDecryptStringRoundTrip() {
        String key = "encryptionKey123";
        String original = "Hello, World! こんにちは 🌍";

        String encryptedHex = EncryptionUtilities.encrypt(key, original);
        assertNotNull(encryptedHex);
        assertFalse(encryptedHex.isEmpty());

        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals(original, decrypted);
    }

    @Test
    public void testEncryptDecryptEmptyString() {
        String key = "blankKey";
        String encrypted = EncryptionUtilities.encrypt(key, "");
        assertNotNull(encrypted);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals("", decrypted);
    }

    @Test
    public void testEncryptDecryptByteArrayRoundTrip() {
        String key = "byteArrayKey";
        byte[] original = new byte[]{0, 1, 2, 3, 4, 5, 127, -128, -1};

        String encryptedHex = EncryptionUtilities.encryptBytes(key, original);
        assertNotNull(encryptedHex);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(original, decrypted);
    }

    @Test
    public void testCreateAesEncryptionCipherAndDecryptionCipher() throws Exception {
        String key = "cipherTestKey";
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(key);
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(key);
        assertEquals(Cipher.ENCRYPT_MODE, enc.getMode());
        assertEquals(Cipher.DECRYPT_MODE, dec.getMode());
    }

    @Test
    public void testCreateAesCipherWithKeyObject() throws Exception {
        byte[] rawKey = EncryptionUtilities.createCipherBytes("rawKey", 128);
        java.security.Key secretKey = new javax.crypto.spec.SecretKeySpec(rawKey, "AES");
        Cipher cipher = EncryptionUtilities.createAesCipher(secretKey, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher);
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode());
    }

    /* -------------------------- Exception handling -------------------------- */
    @Test(expected = IllegalStateException.class)
    public void testEncryptWithInvalidKeyThrows() {
        // Passing null will cause a NullPointerException inside createCipherBytes,
        // which is wrapped as IllegalStateException by encrypt()
        EncryptionUtilities.encrypt(null, "data");
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptWithInvalidHexThrows() {
        // Hex string that cannot be decoded will cause an exception inside ByteUtilities.decode,
        // which is wrapped as IllegalStateException by decrypt()
        EncryptionUtilities.decrypt("key", "ZZZZ");
    }
}
