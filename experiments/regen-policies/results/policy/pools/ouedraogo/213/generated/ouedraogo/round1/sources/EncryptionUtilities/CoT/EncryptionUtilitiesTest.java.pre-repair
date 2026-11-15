package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Arrays;

public class EncryptionUtilitiesTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    /* -------------------- helper methods -------------------- */

    private static String toHex(MessageDigest md, byte[] data) {
        md.update(data);
        return ByteUtilities.encode(md.digest());
    }

    private static File createTempFileWithContent(String name, byte[] content) throws IOException {
        File f = new File(name);
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(content);
        }
        return f;
    }

    /* -------------------- basic functionality tests -------------------- */

    @Test
    public void testFastMD5_HappyPath() throws Exception {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        File f = tempFolder.newFile("md5.txt");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }

        String expected = toHex(MessageDigest.getInstance("MD5"), data);
        String actual = EncryptionUtilities.fastMD5(f);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA1_HappyPath() throws Exception {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        File f = tempFolder.newFile("sha1.txt");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }

        String expected = toHex(MessageDigest.getInstance("SHA-1"), data);
        String actual = EncryptionUtilities.fastSHA1(f);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA256_HappyPath() throws Exception {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        File f = tempFolder.newFile("sha256.txt");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }

        String expected = toHex(MessageDigest.getInstance("SHA-256"), data);
        String actual = EncryptionUtilities.fastSHA256(f);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA512_HappyPath() throws Exception {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        File f = tempFolder.newFile("sha512.txt");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }

        String expected = toHex(MessageDigest.getInstance("SHA-512"), data);
        String actual = EncryptionUtilities.fastSHA512(f);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testCalculateFileHash_HappyPath() throws Exception {
        byte[] data = "some longer content for file hash".getBytes(StandardCharsets.UTF_8);
        File f = tempFolder.newFile("filehash.bin");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(data);
        }

        MessageDigest md = MessageDigest.getInstance("MD5");
        try (FileChannel channel = new FileInputStream(f).getChannel()) {
            String actual = EncryptionUtilities.calculateFileHash(channel, md);
            String expected = toHex(MessageDigest.getInstance("MD5"), data);
            Assert.assertEquals(expected, actual);
        }
    }

    @Test
    public void testCalculateMD5Hash_HappyPath() throws Exception {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        String expected = toHex(MessageDigest.getInstance("MD5"), data);
        String actual = EncryptionUtilities.calculateMD5Hash(data);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetDigest_HappyPath() {
        MessageDigest md = EncryptionUtilities.getDigest("MD5");
        Assert.assertEquals("MD5", md.getAlgorithm());
    }

    @Test
    public void testGetMD5Digest() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        Assert.assertEquals("MD5", md.getAlgorithm());
    }

    @Test
    public void testCalculateSHA1Hash_HappyPath() throws Exception {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        String expected = toHex(MessageDigest.getInstance("SHA-1"), data);
        String actual = EncryptionUtilities.calculateSHA1Hash(data);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetSHA1Digest() {
        MessageDigest md = EncryptionUtilities.getSHA1Digest();
        Assert.assertEquals("SHA-1", md.getAlgorithm());
    }

    @Test
    public void testCalculateSHA256Hash_HappyPath() throws Exception {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        String expected = toHex(MessageDigest.getInstance("SHA-256"), data);
        String actual = EncryptionUtilities.calculateSHA256Hash(data);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetSHA256Digest() {
        MessageDigest md = EncryptionUtilities.getSHA256Digest();
        Assert.assertEquals("SHA-256", md.getAlgorithm());
    }

    @Test
    public void testCalculateSHA512Hash_HappyPath() throws Exception {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        String expected = toHex(MessageDigest.getInstance("SHA-512"), data);
        String actual = EncryptionUtilities.calculateSHA512Hash(data);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testGetSHA512Digest() {
        MessageDigest md = EncryptionUtilities.getSHA512Digest();
        Assert.assertEquals("SHA-512", md.getAlgorithm());
    }

    @Test
    public void testCreateCipherBytes_HappyPath() throws Exception {
        String password = "mySecretPassword";
        byte[] md5 = MessageDigest.getInstance("MD5").digest(password.getBytes(StandardCharsets.UTF_8));
        String md5Hex = ByteUtilities.encode(md5);
        String expected = md5Hex.substring(0, 16); // 128 bits / 8 = 16 chars
        byte[] result = EncryptionUtilities.createCipherBytes(password, 128);
        Assert.assertArrayEquals(expected.getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    public void testCreateAesEncryptionCipher_HappyPath() throws Exception {
        Cipher c = EncryptionUtilities.createAesEncryptionCipher("key123");
        Assert.assertEquals("AES/CBC/PKCS5Padding", c.getAlgorithm());
        Assert.assertEquals(Cipher.ENCRYPT_MODE, c.getMode());
    }

    @Test
    public void testCreateAesDecryptionCipher_HappyPath() throws Exception {
        Cipher c = EncryptionUtilities.createAesDecryptionCipher("key123");
        Assert.assertEquals("AES/CBC/PKCS5Padding", c.getAlgorithm());
        Assert.assertEquals(Cipher.DECRYPT_MODE, c.getMode());
    }

    @Test
    public void testCreateAesCipher_WithString_HappyPath() throws Exception {
        Cipher c = EncryptionUtilities.createAesCipher("myKey", Cipher.ENCRYPT_MODE);
        Assert.assertEquals("AES/CBC/PKCS5Padding", c.getAlgorithm());
        Assert.assertEquals(Cipher.ENCRYPT_MODE, c.getMode());
    }

    @Test
    public void testCreateAesCipher_WithKey_HappyPath() throws Exception {
        byte[] keyBytes = EncryptionUtilities.createCipherBytes("myKey", 128);
        Key key = new SecretKeySpec(keyBytes, "AES");
        Cipher c = EncryptionUtilities.createAesCipher(key, Cipher.DECRYPT_MODE);
        Assert.assertEquals("AES/CBC/PKCS5Padding", c.getAlgorithm());
        Assert.assertEquals(Cipher.DECRYPT_MODE, c.getMode());
    }

    @Test
    public void testEncryptDecrypt_RoundTrip_String() {
        String key = "password";
        String plain = "The quick brown fox jumps over the lazy dog";
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        Assert.assertNotNull(encrypted);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        Assert.assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecrypt_RoundTrip_EmptyString() {
        String key = "password";
        String plain = "";
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        Assert.assertNotNull(encrypted);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        Assert.assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptBytesDecryptBytes_RoundTrip() {
        String key = "password";
        byte[] data = new byte[]{0, 1, 2, 3, 4, 5, 100, -1, -128};
        String encrypted = EncryptionUtilities.encryptBytes(key, data);
        Assert.assertNotNull(encrypted);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encrypted);
        Assert.assertArrayEquals(data, decrypted);
    }

    @Test
    public void testCalculateHash_HappyPath() throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] data = "sample".getBytes(StandardCharsets.UTF_8);
        String expected = ByteUtilities.encode(md.digest(data));
        // Note: calculateHash updates the digest internally, so we need a fresh instance
        String actual = EncryptionUtilities.calculateHash(MessageDigest.getInstance("MD5"), data);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testCalculateHash_NullBytes_ReturnsNull() {
        String result = EncryptionUtilities.calculateHash(MessageDigest.getInstance("MD5"), null);
        Assert.assertNull(result);
    }

    /* -------------------- edge‑case / exception tests -------------------- */

    @Test
    public void testFastMD5_NonExistingFile_ReturnsNull() {
        File missing = new File(tempFolder.getRoot(), "nonexistent.txt");
        Assert.assertNull(EncryptionUtilities.fastMD5(missing));
    }

    @Test
    public void testFastSHA1_NonExistingFile_ReturnsNull() {
        File missing = new File(tempFolder.getRoot(), "nonexistent.txt");
        Assert.assertNull(EncryptionUtilities.fastSHA1(missing));
    }

    @Test
    public void testFastSHA256_NonExistingFile_ReturnsNull() {
        File missing = new File(tempFolder.getRoot(), "nonexistent.txt");
        Assert.assertNull(EncryptionUtilities.fastSHA256(missing));
    }

    @Test
    public void testFastSHA512_NonExistingFile_ReturnsNull() {
        File missing = new File(tempFolder.getRoot(), "nonexistent.txt");
        Assert.assertNull(EncryptionUtilities.fastSHA512(missing));
    }

    @Test
    public void testCalculateMD5Hash_NullInput_ReturnsNull() {
        Assert.assertNull(EncryptionUtilities.calculateMD5Hash(null));
    }

    @Test
    public void testCalculateSHA1Hash_NullInput_ReturnsNull() {
        Assert.assertNull(EncryptionUtilities.calculateSHA1Hash(null));
    }

    @Test
    public void testCalculateSHA256Hash_NullInput_ReturnsNull() {
        Assert.assertNull(EncryptionUtilities.calculateSHA256Hash(null));
    }

    @Test
    public void testCalculateSHA512Hash_NullInput_ReturnsNull() {
        Assert.assertNull(EncryptionUtilities.calculateSHA512Hash(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigest_UnsupportedAlgorithm_ThrowsException() {
        EncryptionUtilities.getDigest("NO_SUCH_ALGO");
    }

    @Test
    public void testCreateCipherBytes_BitsNotMultipleOfEight_TruncatesCorrectly() {
        String password = "pwd";
        int bits = 130; // 130/8 = 16 (integer division) – method will return 16‑byte array
        byte[] result = EncryptionUtilities.createCipherBytes(password, bits);
        Assert.assertEquals(bits / 8, result.length);
    }

    @Test
    public void testDecrypt_InvalidHex_ThrowsIllegalStateException() {
        try {
            EncryptionUtilities.decrypt("anyKey", "ZZZZ");
            Assert.fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testDecryptBytes_InvalidHex_ThrowsIllegalStateException() {
        try {
            EncryptionUtilities.decryptBytes("anyKey", "GGGG");
            Assert.fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    @Test
    public void testCreateAesCipher_InvalidMode_ThrowsException() {
        try {
            EncryptionUtilities.createAesCipher("key", 0); // 0 is not a valid mode
            Assert.fail("Expected Exception");
        } catch (Exception e) {
            // Expected – any exception is fine because the method declares `throws Exception`
        }
    }

}
