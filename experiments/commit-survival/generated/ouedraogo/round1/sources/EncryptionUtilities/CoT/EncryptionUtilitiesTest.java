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
import java.nio.file.Files;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class EncryptionUtilitiesTest {

    private File tempFile;
    private final String sampleText = "The quick brown fox jumps over the lazy dog";

    /* ---------------------------------------------------------------------- */
    /* Helper methods                                                         */
    /* ---------------------------------------------------------------------- */

    /** Write the given bytes to a temporary file that will be deleted after each test. */
    private File createTempFile(byte[] data) throws IOException {
        File f = File.createTempFile("encUtilTest", ".tmp");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }
        f.deleteOnExit();
        return f;
    }

    /** Compute a hex string using the supplied MessageDigest (identical to ByteUtilities.encode). */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String expectedHash(String algorithm, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(data);
        return toHex(md.digest());
    }

    /* ---------------------------------------------------------------------- */
    /* 1. fastMD5 / fastSHA* – normal operation                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFastMD5() throws Exception {
        File f = createTempFile(sampleText.getBytes(StandardCharsets.UTF_8));
        String expected = expectedHash("MD5", sampleText.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, EncryptionUtilities.fastMD5(f));
    }

    @Test
    public void testFastSHA1() throws Exception {
        File f = createTempFile(sampleText.getBytes(StandardCharsets.UTF_8));
        String expected = expectedHash("SHA-1", sampleText.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, EncryptionUtilities.fastSHA1(f));
    }

    @Test
    public void testFastSHA256() throws Exception {
        File f = createTempFile(sampleText.getBytes(StandardCharsets.UTF_8));
        String expected = expectedHash("SHA-256", sampleText.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, EncryptionUtilities.fastSHA256(f));
    }

    @Test
    public void testFastSHA512() throws Exception {
        File f = createTempFile(sampleText.getBytes(StandardCharsets.UTF_8));
        String expected = expectedHash("SHA-512", sampleText.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, EncryptionUtilities.fastSHA512(f));
    }

    /* ---------------------------------------------------------------------- */
    /* 2. fast* edge cases – null or non‑existent file                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFastMD5_NullFile() {
        assertNull(EncryptionUtilities.fastMD5(null));
    }

    @Test
    public void testFastMD5_NonExistingFile() {
        File fake = new File("this_file_should_not_exist_12345.tmp");
        assertFalse(fake.exists());
        assertNull(EncryptionUtilities.fastMD5(fake));
    }

    /* ---------------------------------------------------------------------- */
    /* 3. calculateFileHash – empty file                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCalculateFileHash_EmptyFile() throws Exception {
        File empty = createTempFile(new byte[0]);
        try (FileChannel ch = new FileInputStream(empty).getChannel()) {
            String md5 = EncryptionUtilities.calculateFileHash(ch, EncryptionUtilities.getMD5Digest());
            assertEquals(expectedHash("MD5", new byte[0]), md5);
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 4. calculateMD5Hash / calculateSHA*Hash – normal operation               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCalculateMD5Hash() throws Exception {
        byte[] data = sampleText.getBytes(StandardCharsets.UTF_8);
        String expected = expectedHash("MD5", data);
        assertEquals(expected, EncryptionUtilities.calculateMD5Hash(data));
    }

    @Test
    public void testCalculateSHA1Hash() throws Exception {
        byte[] data = sampleText.getBytes(StandardCharsets.UTF_8);
        String expected = expectedHash("SHA-1", data);
        assertEquals(expected, EncryptionUtilities.calculateSHA1Hash(data));
    }

    @Test
    public void testCalculateSHA256Hash() throws Exception {
        byte[] data = sampleText.getBytes(StandardCharsets.UTF_8);
        String expected = expectedHash("SHA-256", data);
        assertEquals(expected, EncryptionUtilities.calculateSHA256Hash(data));
    }

    @Test
    public void testCalculateSHA512Hash() throws Exception {
        byte[] data = sampleText.getBytes(StandardCharsets.UTF_8);
        String expected = expectedHash("SHA-512", data);
        assertEquals(expected, EncryptionUtilities.calculateSHA512Hash(data));
    }

    /* ---------------------------------------------------------------------- */
    /* 5. calculateHash – null bytes                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCalculateHash_NullBytes() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    /* ---------------------------------------------------------------------- */
    /* 6. getDigest – unknown algorithm                                         */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigest_UnknownAlgorithm() {
        EncryptionUtilities.getDigest("THIS-ALGO-DOES-NOT-EXIST");
    }

    /* ---------------------------------------------------------------------- */
    /* 7. createCipherBytes – empty key & non‑multiple‑of‑8 bitsNeeded          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateCipherBytes_EmptyKey() {
        byte[] result = EncryptionUtilities.createCipherBytes("", 128);
        // MD5 of empty string is d41d8cd98f00b204e9800998ecf8427e; first 16 chars = 16 bytes when encoded as UTF‑8
        String expectedHex = "d41d8cd98f00b204e9800998ecf8427e".substring(0, 16);
        assertEquals(expectedHex, new String(result, StandardCharsets.UTF_8));
    }

    @Test
    public void testCreateCipherBytes_NonMultipleBits() {
        // bitsNeeded = 20 → 20/8 = 2 (integer division), so we expect first 2 bytes of the MD5 string
        byte[] result = EncryptionUtilities.createCipherBytes("key", 20);
        String md5 = EncryptionUtilities.calculateMD5Hash("key".getBytes(StandardCharsets.UTF_8));
        String expected = md5.substring(0, 2);
        assertEquals(expected, new String(result, StandardCharsets.UTF_8));
    }

    /* ---------------------------------------------------------------------- */
    /* 8. createAesCipher – invalid mode                                        */
    /* ---------------------------------------------------------------------- */

    @Test(expected = Exception.class)
    public void testCreateAesCipher_InvalidMode() throws Exception {
        Key dummy = new SecretKeySpec(EncryptionUtilities.createCipherBytes("key", 128), "AES");
        EncryptionUtilities.createAesCipher(dummy, 999); // invalid mode
    }

    /* ---------------------------------------------------------------------- */
    /* 9. encrypt / decrypt – round‑trip                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEncryptDecrypt_RoundTrip() {
        String key = "mySecretPassphrase";
        String encryptedHex = EncryptionUtilities.encrypt(key, sampleText);
        assertNotNull(encryptedHex);
        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals(sampleText, decrypted);
    }

    @Test
    public void testEncryptDecryptBytes_RoundTrip() {
        String key = "anotherKey";
        byte[] data = "binary\0data".getBytes(StandardCharsets.UTF_8);
        String encryptedHex = EncryptionUtilities.encryptBytes(key, data);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(data, decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void testDecrypt_WithWrongKey_ShouldThrow() {
        String correctKey = "rightKey";
        String wrongKey = "wrongKey";
        String encrypted = EncryptionUtilities.encrypt(correctKey, sampleText);
        // Decrypting with a different key must fail and be wrapped in IllegalStateException
        EncryptionUtilities.decrypt(wrongKey, encrypted);
    }

    /* ---------------------------------------------------------------------- */
    /* 10. encrypt / decrypt – empty content                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEncryptDecrypt_EmptyString() {
        String key = "emptyKey";
        String encrypted = EncryptionUtilities.encrypt(key, "");
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals("", decrypted);
    }

    @Test
    public void testEncryptDecryptBytes_EmptyArray() {
        String key = "emptyKey";
        String encrypted = EncryptionUtilities.encryptBytes(key, new byte[0]);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encrypted);
        assertArrayEquals(new byte[0], decrypted);
    }

    /* ---------------------------------------------------------------------- */
    /* 11. encrypt / decrypt – very long content (≈ 64 KB)                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEncryptDecrypt_LongContent() {
        String key = "longKey";
        // 64 KiB of 'a'
        byte[] data = new byte[64 * 1024];
        Arrays.fill(data, (byte) 'a');
        String encrypted = EncryptionUtilities.encryptBytes(key, data);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encrypted);
        assertArrayEquals(data, decrypted);
    }

    /* ---------------------------------------------------------------------- */
    /* 12. getDigest convenience methods – sanity check                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDigestConvenienceMethods() {
        assertNotNull(EncryptionUtilities.getMD5Digest());
        assertNotNull(EncryptionUtilities.getSHA1Digest());
        assertNotNull(EncryptionUtilities.getSHA256Digest());
        assertNotNull(EncryptionUtilities.getSHA512Digest());
    }

    /* ---------------------------------------------------------------------- */
    /* 13. createAesEncryptionCipher / createAesDecryptionCipher – basic usage */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateAesEncryptionDecryptionCipher() throws Exception {
        String key = "cipherKey";
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(key);
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(key);
        assertEquals(Cipher.ENCRYPT_MODE, enc.getMode());
        assertEquals(Cipher.DECRYPT_MODE, dec.getMode());
    }
}
