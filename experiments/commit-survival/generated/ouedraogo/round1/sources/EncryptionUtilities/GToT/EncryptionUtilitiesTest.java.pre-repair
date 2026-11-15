package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link EncryptionUtilities}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>All public static methods (basic happy‑path verification)</li>
 *   <li>Edge‑case handling (null arguments, empty inputs, invalid files)</li>
 *   <li>Exception‑wrapping behaviour of the encrypt/decrypt helpers</li>
 *   <li>Round‑trip AES encryption/decryption (String and byte[] variants)</li>
 * </ul>
 */
public class EncryptionUtilitiesTest {

    /** Temporary file used by the fast* hash tests */
    private File tempFile;

    /** Known content used for most hash calculations */
    private static final byte[] SAMPLE_BYTES = "The quick brown fox jumps over the lazy dog".getBytes();

    /** Expected hash values for {@code SAMPLE_BYTES} */
    private static final String MD5_EXPECTED    = "9e107d9d372bb6826bd81d3542a419d6";
    private static final String SHA1_EXPECTED   = "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12";
    private static final String SHA256_EXPECTED = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb7625e0c8f1a2c4c6d9";
    private static final String SHA512_EXPECTED = "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb64"
            + "2e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6";

    /** A short AES key used for encryption/decryption tests */
    private static final String AES_KEY = "mySecretPassword";

    @Before
    public void setUp() throws IOException {
        // Create a temporary file containing SAMPLE_BYTES.
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

    /* --------------------------------------------------------------------- */
    /* 1.  Basic hash‑function tests (fast* and calculate* helpers)          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFastMD5() {
        String hash = EncryptionUtilities.fastMD5(tempFile);
        assertEquals(MD5_EXPECTED, hash);
    }

    @Test
    public void testFastSHA1() {
        String hash = EncryptionUtilities.fastSHA1(tempFile);
        assertEquals(SHA1_EXPECTED, hash);
    }

    @Test
    public void testFastSHA256() {
        String hash = EncryptionUtilities.fastSHA256(tempFile);
        assertEquals(SHA256_EXPECTED, hash);
    }

    @Test
    public void testFastSHA512() {
        String hash = EncryptionUtilities.fastSHA512(tempFile);
        assertEquals(SHA512_EXPECTED, hash);
    }

    @Test
    public void testFastHashWhenFileNotFound() {
        File nonExistent = new File("this-file-does-not-exist-12345.tmp");
        assertFalse(nonExistent.exists());
        assertNull(EncryptionUtilities.fastMD5(nonExistent));
        assertNull(EncryptionUtilities.fastSHA1(nonExistent));
        assertNull(EncryptionUtilities.fastSHA256(nonExistent));
        assertNull(EncryptionUtilities.fastSHA512(nonExistent));
    }

    @Test
    public void testCalculateMD5Hash() {
        assertEquals(MD5_EXPECTED,
                EncryptionUtilities.calculateMD5Hash(SAMPLE_BYTES));
    }

    @Test
    public void testCalculateSHA1Hash() {
        assertEquals(SHA1_EXPECTED,
                EncryptionUtilities.calculateSHA1Hash(SAMPLE_BYTES));
    }

    @Test
    public void testCalculateSHA256Hash() {
        assertEquals(SHA256_EXPECTED,
                EncryptionUtilities.calculateSHA256Hash(SAMPLE_BYTES));
    }

    @Test
    public void testCalculateSHA512Hash() {
        assertEquals(SHA512_EXPECTED,
                EncryptionUtilities.calculateSHA512Hash(SAMPLE_BYTES));
    }

    @Test
    public void testCalculateHashWithNullBytesReturnsNull() {
        MessageDigest md5 = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md5, null));
    }

    @Test
    public void testCalculateHashWithEmptyArray() {
        MessageDigest sha1 = EncryptionUtilities.getSHA1Digest();
        String hash = EncryptionUtilities.calculateHash(sha1, new byte[0]);
        // SHA‑1 of empty string is a well‑known constant
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hash);
    }

    @Test
    public void testCalculateFileHashDirectly() throws IOException {
        try (FileChannel ch = new FileOutputStream(tempFile).getChannel()) {
            MessageDigest md = EncryptionUtilities.getMD5Digest();
            String hash = EncryptionUtilities.calculateFileHash(ch, md);
            assertEquals(MD5_EXPECTED, hash);
        }
    }

    /* --------------------------------------------------------------------- */
    /* 2.  Digest factory tests                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetDigestValidAlgorithm() throws NoSuchAlgorithmException {
        MessageDigest md = EncryptionUtilities.getDigest("MD5");
        assertEquals(MessageDigest.getInstance("MD5"), md);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigestInvalidAlgorithmThrows() {
        EncryptionUtilities.getDigest("NON_EXISTENT_ALGO");
    }

    @Test
    public void testSpecificDigestFactories() {
        assertEquals("MD5", EncryptionUtilities.getMD5Digest().getAlgorithm());
        assertEquals("SHA-1", EncryptionUtilities.getSHA1Digest().getAlgorithm());
        assertEquals("SHA-256", EncryptionUtilities.getSHA256Digest().getAlgorithm());
        assertEquals("SHA-512", EncryptionUtilities.getSHA512Digest().getAlgorithm());
    }

    /* --------------------------------------------------------------------- */
    /* 3.  AES cipher creation helpers                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCreateCipherBytesLength() {
        // 128‑bit key → 16 bytes, 256‑bit key → 32 bytes
        byte[] bytes128 = EncryptionUtilities.createCipherBytes("someKey", 128);
        byte[] bytes256 = EncryptionUtilities.createCipherBytes("someKey", 256);
        assertEquals(16, bytes128.length);
        assertEquals(32, bytes256.length);
    }

    @Test
    public void testCreateAesEncryptionAndDecryptionCipherRoundTrip() throws Exception {
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(AES_KEY);
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(AES_KEY);
        assertEquals(Cipher.ENCRYPT_MODE, enc.getMode());
        assertEquals(Cipher.DECRYPT_MODE, dec.getMode());
    }

    @Test
    public void testCreateAesCipherWithInvalidMode() {
        try {
            EncryptionUtilities.createAesCipher(AES_KEY, 9999);
            fail("Expected IllegalArgumentException from Cipher.getInstance for invalid mode");
        } catch (Exception e) {
            // The method wraps any exception in IllegalStateException only in encrypt/decrypt,
            // here the exception is propagated as‑is.
            assertTrue(e instanceof IllegalArgumentException || e instanceof java.security.NoSuchAlgorithmException);
        }
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Encryption / Decryption (String)                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEncryptDecryptStringRoundTrip() {
        String original = "Hello, World! 12345";
        String encrypted = EncryptionUtilities.encrypt(AES_KEY, original);
        assertNotNull(encrypted);
        assertNotEquals(original, encrypted); // should be different text

        String decrypted = EncryptionUtilities.decrypt(AES_KEY, encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    public void testEncryptStringWithNullKeyThrowsIllegalStateException() {
        try {
            EncryptionUtilities.encrypt(null, "any");
            fail("Expected IllegalStateException due to null key");
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void testDecryptStringWithInvalidHexThrowsIllegalStateException() {
        try {
            EncryptionUtilities.decrypt(AES_KEY, "ZZZZ"); // not valid hex
            fail("Expected IllegalStateException due to decode failure");
        } catch (IllegalStateException e) {
            // The underlying cause is likely IllegalArgumentException from ByteUtilities.decode
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    /* --------------------------------------------------------------------- */
    /* 5.  Encryption / Decryption (byte[])                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEncryptDecryptBytesRoundTrip() {
        byte[] original = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        String encrypted = EncryptionUtilities.encryptBytes(AES_KEY, original);
        assertNotNull(encrypted);

        byte[] decrypted = EncryptionUtilities.decryptBytes(AES_KEY, encrypted);
        assertArrayEquals(original, decrypted);
    }

    @Test
    public void testEncryptBytesWithNullKeyThrowsIllegalStateException() {
        try {
            EncryptionUtilities.encryptBytes(null, new byte[]{1, 2, 3});
            fail("Expected IllegalStateException due to null key");
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void testDecryptBytesWithInvalidHexThrowsIllegalStateException() {
        try {
            EncryptionUtilities.decryptBytes(AES_KEY, "GHIJKL");
            fail("Expected IllegalStateException due to invalid hex");
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    /* --------------------------------------------------------------------- */
    /* 6.  Miscellaneous utility methods                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCreateAesCipherFromKeyObject() throws Exception {
        // Use the same key bytes as the String‑based factory for consistency
        byte[] keyBytes = EncryptionUtilities.createCipherBytes(AES_KEY, 128);
        Key key = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        Cipher cipher = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher);
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode());
    }
}
