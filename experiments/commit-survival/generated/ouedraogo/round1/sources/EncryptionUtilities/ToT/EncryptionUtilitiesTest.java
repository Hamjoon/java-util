package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link EncryptionUtilities}.
 *
 * <p>The tests are grouped by the method they validate and cover:
 * <ul>
 *   <li>Typical usage – e.g., hash of known data, encrypt/decrypt round‑trip.</li>
 *   <li>Edge cases – empty inputs, null arguments, non‑existent files.</li>
 *   <li>Error scenarios – illegal algorithm name, wrong decryption key.</li>
 * </ul>
 *
 * <p>No mocking frameworks are used; all objects are created through the
 * constructors or factory methods that appear in {@code EncryptionUtilities}.
 */
public class EncryptionUtilitiesTest {

    private File tempFile;
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog";
    private static final String SAMPLE_KEY = "secret-passphrase";

    @Before
    public void setUp() throws Exception {
        // Create a temporary file that will be reused by the fast* hash tests.
        tempFile = Files.createTempFile("EncryptionUtilitiesTest", ".txt").toFile();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        }
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  fastMD5, fastSHA1, fastSHA256, fastSHA512 – file based hashing        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFastMD5_ValidFile() throws Exception {
        String expected = md5Hex(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String actual = EncryptionUtilities.fastMD5(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA1_ValidFile() throws Exception {
        String expected = sha1Hex(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String actual = EncryptionUtilities.fastSHA1(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA256_ValidFile() throws Exception {
        String expected = sha256Hex(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String actual = EncryptionUtilities.fastSHA256(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA512_ValidFile() throws Exception {
        String expected = sha512Hex(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String actual = EncryptionUtilities.fastSHA512(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastHash_NonExistentFile_ReturnsNull() {
        File missing = new File("nonexistent_" + System.nanoTime() + ".txt");
        assertFalse(missing.exists());

        assertNull(EncryptionUtilities.fastMD5(missing));
        assertNull(EncryptionUtilities.fastSHA1(missing));
        assertNull(EncryptionUtilities.fastSHA256(missing));
        assertNull(EncryptionUtilities.fastSHA512(missing));
    }

    @Test
    public void testFastMD5_EmptyFile() throws Exception {
        File empty = Files.createTempFile("empty", ".txt").toFile();
        // No content written => zero‑length file
        String expected = md5Hex(EMPTY_BYTES);
        String actual = EncryptionUtilities.fastMD5(empty);
        assertEquals(expected, actual);
        empty.delete();
    }

    /* ---------------------------------------------------------------------- */
    /*  calculateFileHash – low‑level helper                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCalculateFileHash_MatchesDirectDigest() throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            String expected = ByteUtilities.encode(md.digest(Files.readAllBytes(tempFile.toPath())));
            String actual = EncryptionUtilities.calculateFileHash(fis.getChannel(),
                    EncryptionUtilities.getMD5Digest());
            assertEquals(expected, actual);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Byte‑array hash helpers (MD5, SHA‑1, SHA‑256, SHA‑512)                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCalculateMD5Hash_Typical() {
        String expected = md5Hex(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String actual = EncryptionUtilities.calculateMD5Hash(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA1Hash_Typical() {
        String expected = sha1Hex(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String actual = EncryptionUtilities.calculateSHA1Hash(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA256Hash_Typical() {
        String expected = sha256Hex(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String actual = EncryptionUtilities.calculateSHA256Hash(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA512Hash_Typical() {
        String expected = sha512Hex(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        String actual = EncryptionUtilities.calculateSHA512Hash(SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateHash_NullInput_ReturnsNull() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    /* ---------------------------------------------------------------------- */
    /*  MessageDigest factory methods                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDigest_ValidAlgorithm() throws Exception {
        MessageDigest md = EncryptionUtilities.getDigest("MD5");
        assertNotNull(md);
        assertEquals("MD5", md.getAlgorithm());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigest_InvalidAlgorithm_Throws() {
        EncryptionUtilities.getDigest("NO‑SUCH‑ALG");
    }

    @Test
    public void testGetMD5Digest() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertEquals("MD5", md.getAlgorithm());
    }

    @Test
    public void testGetSHA1Digest() {
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        assertEquals("SHA-1", md.getAlgorithm());
    }

    @Test
    public void testGetSHA256Digest() {
        MessageDigest md = EncryptionUtilities.getSHA256Digest();
        assertEquals("SHA-256", md.getAlgorithm());
    }

    @Test
    public void testGetSHA512Digest() {
        MessageDigest md = EncryptionUtilities.getSHA512Digest();
        assertEquals("SHA-512", md.getAlgorithm());
    }

    /* ---------------------------------------------------------------------- */
    /*  Cipher creation helpers                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateCipherBytes_LengthMatchesBits() {
        // 128‑bit key => 16 bytes
        byte[] bytes = EncryptionUtilities.createCipherBytes("anyKey", 128);
        assertEquals(16, bytes.length);
        // 256‑bit key => 32 bytes
        byte[] bytes256 = EncryptionUtilities.createCipherBytes("anyKey", 256);
        assertEquals(32, bytes256.length);
    }

    @Test
    public void testCreateAesEncryptionCipherAndDecryptionCipher_RoundTrip() throws Exception {
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(SAMPLE_KEY);
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(SAMPLE_KEY);
        assertNotNull(enc);
        assertNotNull(dec);
        assertEquals(Cipher.ENCRYPT_MODE, enc.getMode());
        assertEquals(Cipher.DECRYPT_MODE, dec.getMode());
    }

    @Test
    public void testCreateAesCipher_WithKeyObject() throws Exception {
        // Use the same logic as the library to generate a SecretKeySpec
        byte[] keyBytes = EncryptionUtilities.createCipherBytes(SAMPLE_KEY, 128);
        Key secretKey = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        Cipher cipher = EncryptionUtilities.createAesCipher(secretKey, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher);
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode());
    }

    /* ---------------------------------------------------------------------- */
    /*  Encryption / Decryption (String)                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEncryptDecrypt_RoundTrip() {
        String encrypted = EncryptionUtilities.encrypt(SAMPLE_KEY, SAMPLE_TEXT);
        assertNotNull(encrypted);
        String decrypted = EncryptionUtilities.decrypt(SAMPLE_KEY, encrypted);
        assertEquals(SAMPLE_TEXT, decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void testDecrypt_WithWrongKey_Throws() {
        String encrypted = EncryptionUtilities.encrypt(SAMPLE_KEY, SAMPLE_TEXT);
        // Use a different key – decryption must fail and be wrapped in IllegalStateException
        EncryptionUtilities.decrypt("wrong-key", encrypted);
    }

    /* ---------------------------------------------------------------------- */
    /*  Encryption / Decryption (byte[])                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEncryptBytesDecryptBytes_RoundTrip() {
        byte[] data = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        String encryptedHex = EncryptionUtilities.encryptBytes(SAMPLE_KEY, data);
        assertNotNull(encryptedHex);
        byte[] decrypted = EncryptionUtilities.decryptBytes(SAMPLE_KEY, encryptedHex);
        assertArrayEquals(data, decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptBytes_WithInvalidHex_Throws() {
        // Pass a malformed hex string; the internal decode will throw an exception.
        EncryptionUtilities.decryptBytes(SAMPLE_KEY, "ZZZZ");
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper methods to compute expected hash strings using JDK MessageDigest */
    /* ---------------------------------------------------------------------- */

    private static String md5Hex(byte[] data) {
        return ByteUtilities.encode(digest("MD5", data));
    }

    private static String sha1Hex(byte[] data) {
        return ByteUtilities.encode(digest("SHA-1", data));
    }

    private static String sha256Hex(byte[] data) {
        return ByteUtilities.encode(digest("SHA-256", data));
    }

    private static String sha512Hex(byte[] data) {
        return ByteUtilities.encode(digest("SHA-512", data));
    }

    private static byte[] digest(String algorithm, byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Algorithm not available: " + algorithm, e);
        }
    }
}
