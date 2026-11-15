package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import javax.crypto.Cipher;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;

import static org.junit.Assert.*;

public class EncryptionUtilitiesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /* -------------------------------------------------------------
       Helper methods used by many tests
       ------------------------------------------------------------- */
    private static String hexDigest(MessageDigest md, byte[] data) {
        md.update(data);
        return ByteUtilities.encode(md.digest());
    }

    private File createTempFileWithContent(byte[] content) throws IOException {
        File f = tmp.newFile();
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(content);
        }
        return f;
    }

    /* -------------------------------------------------------------
       1. fastMD5
       ------------------------------------------------------------- */
    @Test
    public void testFastMD5_HashMatchesDigest() throws Exception {
        byte[] data = "The quick brown fox".getBytes(StandardCharsets.UTF_8);
        File f = createTempFileWithContent(data);

        String expected = hexDigest(MessageDigest.getInstance("MD5"), data);
        String actual = EncryptionUtilities.fastMD5(f);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastMD5_NonExistingFileReturnsNull() {
        File missing = new File(tmp.getRoot(), "does-not-exist.txt");
        assertFalse(missing.exists());
        assertNull(EncryptionUtilities.fastMD5(missing));
    }

    /* -------------------------------------------------------------
       2. fastSHA1
       ------------------------------------------------------------- */
    @Test
    public void testFastSHA1_HashMatchesDigest() throws Exception {
        byte[] data = "hello world".getBytes(StandardCharsets.UTF_8);
        File f = createTempFileWithContent(data);

        String expected = hexDigest(MessageDigest.getInstance("SHA-1"), data);
        String actual = EncryptionUtilities.fastSHA1(f);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA1_NonExistingFileReturnsNull() {
        File missing = new File(tmp.getRoot(), "nope.bin");
        assertNull(EncryptionUtilities.fastSHA1(missing));
    }

    /* -------------------------------------------------------------
       3. fastSHA256
       ------------------------------------------------------------- */
    @Test
    public void testFastSHA256_HashMatchesDigest() throws Exception {
        byte[] data = "some longer test string".getBytes(StandardCharsets.UTF_8);
        File f = createTempFileWithContent(data);

        String expected = hexDigest(MessageDigest.getInstance("SHA-256"), data);
        String actual = EncryptionUtilities.fastSHA256(f);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA256_NonExistingFileReturnsNull() {
        File missing = new File(tmp.getRoot(), "absent.dat");
        assertNull(EncryptionUtilities.fastSHA256(missing));
    }

    /* -------------------------------------------------------------
       4. fastSHA512
       ------------------------------------------------------------- */
    @Test
    public void testFastSHA512_HashMatchesDigest() throws Exception {
        byte[] data = "yet another test".getBytes(StandardCharsets.UTF_8);
        File f = createTempFileWithContent(data);

        String expected = hexDigest(MessageDigest.getInstance("SHA-512"), data);
        String actual = EncryptionUtilities.fastSHA512(f);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA512_NonExistingFileReturnsNull() {
        File missing = new File(tmp.getRoot(), "missing.bin");
        assertNull(EncryptionUtilities.fastSHA512(missing));
    }

    /* -------------------------------------------------------------
       5. calculateFileHash
       ------------------------------------------------------------- */
    @Test
    public void testCalculateFileHash_WithFileChannel() throws Exception {
        byte[] data = "channel test".getBytes(StandardCharsets.UTF_8);
        File f = createTempFileWithContent(data);
        try (FileChannel ch = new FileInputStream(f).getChannel()) {
            String hash = EncryptionUtilities.calculateFileHash(ch, MessageDigest.getInstance("MD5"));
            String expected = hexDigest(MessageDigest.getInstance("MD5"), data);
            assertEquals(expected, hash);
        }
    }

    @Test(expected = IOException.class)
    public void testCalculateFileHash_WithClosedChannelThrows() throws Exception {
        byte[] data = "closed".getBytes(StandardCharsets.UTF_8);
        File f = createTempFileWithContent(data);
        FileChannel ch = new FileInputStream(f).getChannel();
        ch.close(); // explicit close
        // This should propagate an IOException
        EncryptionUtilities.calculateFileHash(ch, MessageDigest.getInstance("MD5"));
    }

    /* -------------------------------------------------------------
       6‑9. Byte‑array hash helpers
       ------------------------------------------------------------- */
    @Test
    public void testCalculateMD5Hash_ValidBytes() throws Exception {
        byte[] data = "md5test".getBytes(StandardCharsets.UTF_8);
        String expected = hexDigest(MessageDigest.getInstance("MD5"), data);
        assertEquals(expected, EncryptionUtilities.calculateMD5Hash(data));
    }

    @Test
    public void testCalculateSHA1Hash_ValidBytes() throws Exception {
        byte[] data = "sha1test".getBytes(StandardCharsets.UTF_8);
        String expected = hexDigest(MessageDigest.getInstance("SHA-1"), data);
        assertEquals(expected, EncryptionUtilities.calculateSHA1Hash(data));
    }

    @Test
    public void testCalculateSHA256Hash_ValidBytes() throws Exception {
        byte[] data = "sha256test".getBytes(StandardCharsets.UTF_8);
        String expected = hexDigest(MessageDigest.getInstance("SHA-256"), data);
        assertEquals(expected, EncryptionUtilities.calculateSHA256Hash(data));
    }

    @Test
    public void testCalculateSHA512Hash_ValidBytes() throws Exception {
        byte[] data = "sha512test".getBytes(StandardCharsets.UTF_8);
        String expected = hexDigest(MessageDigest.getInstance("SHA-512"), data);
        assertEquals(expected, EncryptionUtilities.calculateSHA512Hash(data));
    }

    @Test
    public void testCalculateHash_NullInputReturnsNull() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    /* -------------------------------------------------------------
       10‑14. getDigest family
       ------------------------------------------------------------- */
    @Test
    public void testGetDigest_SupportedAlgorithms() {
        assertEquals("MD5", EncryptionUtilities.getDigest("MD5").getAlgorithm());
        assertEquals("SHA-1", EncryptionUtilities.getDigest("SHA-1").getAlgorithm());
        assertEquals("SHA-256", EncryptionUtilities.getDigest("SHA-256").getAlgorithm());
        assertEquals("SHA-512", EncryptionUtilities.getDigest("SHA-512").getAlgorithm());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigest_UnsupportedAlgorithmThrows() {
        EncryptionUtilities.getDigest("FOO-BAR");
    }

    @Test
    public void testGetMD5Digest_ReturnsMD5Instance() {
        assertEquals("MD5", EncryptionUtilities.getMD5Digest().getAlgorithm());
    }

    @Test
    public void testGetSHA1Digest_ReturnsSHA1Instance() {
        assertEquals("SHA-1", EncryptionUtilities.getSHA1Digest().getAlgorithm());
    }

    @Test
    public void testGetSHA256Digest_ReturnsSHA256Instance() {
        assertEquals("SHA-256", EncryptionUtilities.getSHA256Digest().getAlgorithm());
    }

    @Test
    public void testGetSHA512Digest_ReturnsSHA512Instance() {
        assertEquals("SHA-512", EncryptionUtilities.getSHA512Digest().getAlgorithm());
    }

    /* -------------------------------------------------------------
       15. createCipherBytes
       ------------------------------------------------------------- */
    @Test
    public void testCreateCipherBytes_ValidParameters() {
        String password = "mySecretPassword";
        byte[] keyBytes = EncryptionUtilities.createCipherBytes(password, 128);
        assertNotNull(keyBytes);
        assertEquals(128 / 8, keyBytes.length);
    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void testCreateCipherBytes_NonMultipleOfEightBitsThrows() {
        EncryptionUtilities.createCipherBytes("pwd", 130); // 130/8 = 16.25 → substring overflow
    }

    /* -------------------------------------------------------------
       16‑19. Cipher creation
       ------------------------------------------------------------- */
    @Test
    public void testCreateAesEncryptionCipher_NotNull() throws Exception {
        Cipher c = EncryptionUtilities.createAesEncryptionCipher("key123");
        assertNotNull(c);
        assertEquals(Cipher.ENCRYPT_MODE, c.getMode());
    }

    @Test
    public void testCreateAesDecryptionCipher_NotNull() throws Exception {
        Cipher c = EncryptionUtilities.createAesDecryptionCipher("key123");
        assertNotNull(c);
        assertEquals(Cipher.DECRYPT_MODE, c.getMode());
    }

    @Test
    public void testCreateAesCipher_WithStringAndMode() throws Exception {
        Cipher enc = EncryptionUtilities.createAesCipher("secret", Cipher.ENCRYPT_MODE);
        Cipher dec = EncryptionUtilities.createAesCipher("secret", Cipher.DECRYPT_MODE);
        assertNotNull(enc);
        assertNotNull(dec);
        assertEquals(Cipher.ENCRYPT_MODE, enc.getMode());
        assertEquals(Cipher.DECRYPT_MODE, dec.getMode());
    }

    @Test
    public void testCreateAesCipher_WithKeyAndMode() throws Exception {
        byte[] raw = EncryptionUtilities.createCipherBytes("anotherKey", 128);
        Key key = new SecretKeySpec(raw, "AES");
        Cipher cipher = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher);
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode());
    }

    /* -------------------------------------------------------------
       20‑23. Encryption / Decryption round‑trip
       ------------------------------------------------------------- */
    @Test
    public void testEncryptDecrypt_StringRoundTrip() {
        String key = "myKey";
        String plain = "This is a secret message!";
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        assertNotNull(encrypted);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecrypt_EmptyString() {
        String key = "emptyKey";
        String encrypted = EncryptionUtilities.encrypt(key, "");
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals("", decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void testDecrypt_WithWrongKeyThrows() {
        String key1 = "keyOne";
        String key2 = "keyTwo";
        String payload = EncryptionUtilities.encrypt(key1, "payload");
        // attempt to decrypt with a different key – should wrap the underlying exception
        EncryptionUtilities.decrypt(key2, payload);
    }

    @Test
    public void testEncryptDecrypt_ByteArrayRoundTrip() {
        String key = "byteKey";
        byte[] data = "binary data \u0000\u00FF".getBytes(StandardCharsets.UTF_8);
        String encrypted = EncryptionUtilities.encryptBytes(key, data);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encrypted);
        assertArrayEquals(data, decrypted);
    }

    @Test
    public void testEncryptBytes_EmptyArray() {
        String key = "emptyArrKey";
        byte[] empty = new byte[0];
        String encrypted = EncryptionUtilities.encryptBytes(key, empty);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encrypted);
        assertArrayEquals(empty, decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptBytes_WithCorruptHexThrows() {
        String key = "badHexKey";
        // not a valid hex string – decode will fail inside ByteUtilities
        EncryptionUtilities.decryptBytes(key, "ZZZNotHex");
    }

    /* -------------------------------------------------------------
       24. calculateHash (already exercised above, but add a direct test)
       ------------------------------------------------------------- */
    @Test
    public void testCalculateHash_ValidInput() throws Exception {
        MessageDigest md = EncryptionUtilities.getSHA256Digest();
        byte[] data = "hashme".getBytes(StandardCharsets.UTF_8);
        String expected = hexDigest(md, data);
        // Need a fresh digest because the previous call consumed it
        md = EncryptionUtilities.getSHA256Digest();
        String actual = EncryptionUtilities.calculateHash(md, data);
        assertEquals(expected, actual);
    }
}
