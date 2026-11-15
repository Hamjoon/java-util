package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class EncryptionUtilitiesTest {

    private File tempFile;
    private byte[] fileContent;

    /** Helper to convert a byte array to a lower‑case hex string (the format used by ByteUtilities). */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Before
    public void setUp() throws IOException {
        // Create a temporary file with known content for the fast‑hash tests.
        tempFile = File.createTempFile("EncryptionUtilitiesTest", ".txt");
        fileContent = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            out.write(fileContent);
        }
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* -------------------------------------------------------------
       1. Tests for the low‑level hash helpers (MD5, SHA‑1, SHA‑256, SHA‑512)
       ------------------------------------------------------------- */
    @Test
    public void testCalculateMD5Hash() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        String expected = toHex(md.digest(fileContent));
        assertEquals(expected, EncryptionUtilities.calculateMD5Hash(fileContent));
    }

    @Test
    public void testCalculateSHA1Hash() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String expected = toHex(md.digest(fileContent));
        assertEquals(expected, EncryptionUtilities.calculateSHA1Hash(fileContent));
    }

    @Test
    public void testCalculateSHA256Hash() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String expected = toHex(md.digest(fileContent));
        assertEquals(expected, EncryptionUtilities.calculateSHA256Hash(fileContent));
    }

    @Test
    public void testCalculateSHA512Hash() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        String expected = toHex(md.digest(fileContent));
        assertEquals(expected, EncryptionUtilities.calculateSHA512Hash(fileContent));
    }

    @Test
    public void testCalculateHashWithNullBytes() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigestWithInvalidAlgorithm() {
        EncryptionUtilities.getDigest("NO-SUCH-ALG");
    }

    /* -------------------------------------------------------------
       2. Tests for the fast file‑hash methods (fastMD5, fastSHA1, …)
       ------------------------------------------------------------- */
    @Test
    public void testFastMD5() throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        String expected = toHex(md.digest(fileContent));
        assertEquals(expected, EncryptionUtilities.fastMD5(tempFile));
    }

    @Test
    public void testFastSHA1() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String expected = toHex(md.digest(fileContent));
        assertEquals(expected, EncryptionUtilities.fastSHA1(tempFile));
    }

    @Test
    public void testFastSHA256() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        String expected = toHex(md.digest(fileContent));
        assertEquals(expected, EncryptionUtilities.fastSHA256(tempFile));
    }

    @Test
    public void testFastSHA512() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        String expected = toHex(md.digest(fileContent));
        assertEquals(expected, EncryptionUtilities.fastSHA512(tempFile));
    }

    @Test
    public void testFastHashWhenFileDoesNotExist() {
        File nonExistent = new File("this_file_should_not_exist_12345.tmp");
        assertFalse(nonExistent.exists());
        assertNull(EncryptionUtilities.fastMD5(nonExistent));
        assertNull(EncryptionUtilities.fastSHA1(nonExistent));
        assertNull(EncryptionUtilities.fastSHA256(nonExistent));
        assertNull(EncryptionUtilities.fastSHA512(nonExistent));
    }

    @Test
    public void testCalculateFileHashDirectly() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileChannel ch = new FileOutputStream(tempFile, true).getChannel()) {
            // Move to start of file
            ch.position(0);
            String hash = EncryptionUtilities.calculateFileHash(ch, md);
            String expected = toHex(md.digest(fileContent));
            assertEquals(expected, hash);
        }
    }

    /* -------------------------------------------------------------
       3. Tests for AES cipher creation helpers
       ------------------------------------------------------------- */
    @Test
    public void testCreateCipherBytesLengthAndContent() {
        String key = "mySecretKey";
        // 128 bits → 16 bytes
        byte[] cipherBytes = EncryptionUtilities.createCipherBytes(key, 128);
        assertEquals(16, cipherBytes.length);

        // The bytes are the first 16 characters of the MD5 hex string of the key
        String md5Hex = EncryptionUtilities.calculateMD5Hash(key.getBytes(StandardCharsets.UTF_8));
        byte[] expected = md5Hex.substring(0, 16).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, cipherBytes);
    }

    @Test
    public void testCreateAesEncryptionAndDecryptionCiphers() throws Exception {
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher("key123");
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher("key123");

        assertNotNull(enc);
        assertNotNull(dec);
        assertEquals("AES/CBC/PKCS5Padding", enc.getAlgorithm());
        assertEquals("AES/CBC/PKCS5Padding", dec.getAlgorithm());

        // Encrypt a small payload and then decrypt it with the other cipher
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = enc.doFinal(payload);
        byte[] decrypted = dec.doFinal(encrypted);
        assertArrayEquals(payload, decrypted);
    }

    /* -------------------------------------------------------------
       4. Tests for high‑level encrypt / decrypt (String and byte[])
       ------------------------------------------------------------- */
    @Test
    public void testEncryptDecryptStringRoundTrip() {
        String key = "superSecret";
        String original = "Hello, EncryptionUtilities!";

        String encryptedHex = EncryptionUtilities.encrypt(key, original);
        assertNotNull(encryptedHex);
        assertFalse(encryptedHex.isEmpty());

        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals(original, decrypted);
    }

    @Test
    public void testEncryptDecryptEmptyString() {
        String key = "emptyKey";
        String encryptedHex = EncryptionUtilities.encrypt(key, "");
        assertNotNull(encryptedHex);
        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals("", decrypted);
    }

    @Test
    public void testEncryptDecryptBytesRoundTrip() {
        String key = "bytesKey";
        byte[] original = new byte[]{0, 1, 2, 3, 4, 5, -1, -2};

        String encryptedHex = EncryptionUtilities.encryptBytes(key, original);
        assertNotNull(encryptedHex);

        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(original, decrypted);
    }

    @Test
    public void testEncryptWithDifferentKeysProducesDifferentCiphertexts() {
        String key1 = "firstKey";
        String key2 = "secondKey";
        String plain = "samePlainText";

        String enc1 = EncryptionUtilities.encrypt(key1, plain);
        String enc2 = EncryptionUtilities.encrypt(key2, plain);

        assertNotEquals(enc1, enc2);
        // Ensure each can be decrypted with its own key
        assertEquals(plain, EncryptionUtilities.decrypt(key1, enc1));
        assertEquals(plain, EncryptionUtilities.decrypt(key2, enc2));
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptWithWrongKeyThrowsException() {
        String correctKey = "correctKey";
        String wrongKey = "wrongKey";
        String plain = "secretMessage";

        String encrypted = EncryptionUtilities.encrypt(correctKey, plain);
        // Decrypting with the wrong key should cause an IllegalStateException (wrapped Cipher exception)
        EncryptionUtilities.decrypt(wrongKey, encrypted);
    }

    /* -------------------------------------------------------------
       5. Additional sanity checks for the public getters
       ------------------------------------------------------------- */
    @Test
    public void testGetDigestInstances() throws NoSuchAlgorithmException {
        assertEquals(MessageDigest.getInstance("MD5").getAlgorithm(),
                EncryptionUtilities.getMD5Digest().getAlgorithm());

        assertEquals(MessageDigest.getInstance("SHA-1").getAlgorithm(),
                EncryptionUtilities.getSHA1Digest().getAlgorithm());

        assertEquals(MessageDigest.getInstance("SHA-256").getAlgorithm(),
                EncryptionUtilities.getSHA256Digest().getAlgorithm());

        assertEquals(MessageDigest.getInstance("SHA-512").getAlgorithm(),
                EncryptionUtilities.getSHA512Digest().getAlgorithm());
    }
}
