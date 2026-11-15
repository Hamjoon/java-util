package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link EncryptionUtilities}.
 *
 * <p>This class exercises typical usage, edge‑cases and error handling for every public
 * method in {@code EncryptionUtilities}.  All test data are created on‑the‑fly – no external
 * resources are required.</p>
 */
public class EncryptionUtilitiesTest {

    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog";
    private static final byte[] SAMPLE_BYTES = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
    private File tempFile;

    @Before
    public void setUp() throws IOException {
        // Create a temporary file containing SAMPLE_TEXT – used for the fast* hash tests.
        tempFile = File.createTempFile("EncryptionUtilitiesTest", ".txt");
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(SAMPLE_BYTES);
        }
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  Hashing of files (fastMD5, fastSHA1, fastSHA256, fastSHA512)         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFastMD5() throws Exception {
        String expected = ByteUtilities.encode(digest("MD5", SAMPLE_BYTES));
        String actual = EncryptionUtilities.fastMD5(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA1() throws Exception {
        String expected = ByteUtilities.encode(digest("SHA-1", SAMPLE_BYTES));
        String actual = EncryptionUtilities.fastSHA1(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA256() throws Exception {
        String expected = ByteUtilities.encode(digest("SHA-256", SAMPLE_BYTES));
        String actual = EncryptionUtilities.fastSHA256(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA512() throws Exception {
        String expected = ByteUtilities.encode(digest("SHA-512", SAMPLE_BYTES));
        String actual = EncryptionUtilities.fastSHA512(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastHashFileNotFoundReturnsNull() {
        File missing = new File("nonexistent_file_12345.tmp");
        assertFalse(missing.exists());

        assertNull(EncryptionUtilities.fastMD5(missing));
        assertNull(EncryptionUtilities.fastSHA1(missing));
        assertNull(EncryptionUtilities.fastSHA256(missing));
        assertNull(EncryptionUtilities.fastSHA512(missing));
    }

    /* ---------------------------------------------------------------------- */
    /*  Byte‑array hashing (calculate*Hash)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCalculateMD5Hash() {
        String expected = ByteUtilities.encode(digest("MD5", SAMPLE_BYTES));
        String actual = EncryptionUtilities.calculateMD5Hash(SAMPLE_BYTES);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA1Hash() {
        String expected = ByteUtilities.encode(digest("SHA-1", SAMPLE_BYTES));
        String actual = EncryptionUtilities.calculateSHA1Hash(SAMPLE_BYTES);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA256Hash() {
        String expected = ByteUtilities.encode(digest("SHA-256", SAMPLE_BYTES));
        String actual = EncryptionUtilities.calculateSHA256Hash(SAMPLE_BYTES);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateSHA512Hash() {
        String expected = ByteUtilities.encode(digest("SHA-512", SAMPLE_BYTES));
        String actual = EncryptionUtilities.calculateSHA512Hash(SAMPLE_BYTES);
        assertEquals(expected, actual);
    }

    @Test
    public void testCalculateHashWithNullReturnsNull() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    /* ---------------------------------------------------------------------- */
    /*  MessageDigest factory methods                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDigestValid() throws NoSuchAlgorithmException {
        MessageDigest md = EncryptionUtilities.getDigest("MD5");
        assertEquals("MD5", md.getAlgorithm());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigestInvalidThrows() {
        EncryptionUtilities.getDigest("NO_SUCH_ALGO");
    }

    @Test
    public void testGetSpecificDigestMethods() {
        assertEquals("MD5", EncryptionUtilities.getMD5Digest().getAlgorithm());
        assertEquals("SHA-1", EncryptionUtilities.getSHA1Digest().getAlgorithm());
        assertEquals("SHA-256", EncryptionUtilities.getSHA256Digest().getAlgorithm());
        assertEquals("SHA-512", EncryptionUtilities.getSHA512Digest().getAlgorithm());
    }

    /* ---------------------------------------------------------------------- */
    /*  Cipher key derivation (createCipherBytes)                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateCipherBytesLength() {
        String password = "mySecretPassword";
        int bits = 128;
        byte[] keyBytes = EncryptionUtilities.createCipherBytes(password, bits);
        assertEquals(bits / 8, keyBytes.length);
    }

    @Test
    public void testCreateCipherBytesContentMatchesMD5Truncation() {
        String password = "password123";
        String md5Hex = EncryptionUtilities.calculateMD5Hash(password.getBytes(StandardCharsets.UTF_8));
        byte[] expected = md5Hex.substring(0, 16).getBytes(StandardCharsets.UTF_8); // 128 bits = 16 chars
        byte[] actual = EncryptionUtilities.createCipherBytes(password, 128);
        assertArrayEquals(expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /*  AES Cipher creation (encryption & decryption)                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateAesEncryptionAndDecryptionCipherRoundTrip() throws Exception {
        String key = "encryptionKey";
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(key);
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(key);
        assertNotNull(enc);
        assertNotNull(dec);
        assertEquals(Cipher.ENCRYPT_MODE, enc.getMode());
        assertEquals(Cipher.DECRYPT_MODE, dec.getMode());
    }

    @Test
    public void testCreateAesCipherWithKeyObject() throws Exception {
        byte[] rawKey = EncryptionUtilities.createCipherBytes("anotherKey", 128);
        SecretKeySpec spec = new SecretKeySpec(rawKey, "AES");
        Cipher cipher = EncryptionUtilities.createAesCipher(spec, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher);
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode());
    }

    /* ---------------------------------------------------------------------- */
    /*  Encryption / Decryption (String & byte[] overloads)                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEncryptDecryptStringRoundTrip() {
        String key = "myKey";
        String original = "Sensitive information 123!";
        String encryptedHex = EncryptionUtilities.encrypt(key, original);
        assertNotNull(encryptedHex);
        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals(original, decrypted);
    }

    @Test
    public void testEncryptDecryptBytesRoundTrip() {
        String key = "bytesKey";
        byte[] encryptedHex = EncryptionUtilities.encryptBytes(key, SAMPLE_BYTES).getBytes(StandardCharsets.UTF_8);
        // decryptBytes returns the raw byte[]; we compare to original.
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, new String(encryptedHex, StandardCharsets.UTF_8));
        assertArrayEquals(SAMPLE_BYTES, decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptWithWrongKeyThrows() {
        String correctKey = "correctKey";
        String wrongKey = "wrongKey";
        String plaintext = "Will fail to decrypt";
        String encrypted = EncryptionUtilities.encrypt(correctKey, plaintext);
        // Attempt decryption with a different key – should raise IllegalStateException.
        EncryptionUtilities.decrypt(wrongKey, encrypted);
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper method to compute a MessageDigest directly (used for expectations) */
    /* ---------------------------------------------------------------------- */
    private static byte[] digest(String algorithm, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        return md.digest(data);
    }
}
