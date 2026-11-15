package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.Assert.*;

public class EncryptionUtilitiesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /*** Helper methods ***/

    /** Returns the hex string for the MD5 of the given text (used for expected values). */
    private static String md5Hex(String text) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(text.getBytes(StandardCharsets.UTF_8));
        return ByteUtilities.encode(md.digest());
    }

    /** Returns the hex string for the SHA‑1 of the given text (used for expected values). */
    private static String sha1Hex(String text) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(text.getBytes(StandardCharsets.UTF_8));
        return ByteUtilities.encode(md.digest());
    }

    /** Returns the hex string for the SHA‑256 of the given text (used for expected values). */
    private static String sha256Hex(String text) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(text.getBytes(StandardCharsets.UTF_8));
        return ByteUtilities.encode(md.digest());
    }

    /** Returns the hex string for the SHA‑512 of the given text (used for expected values). */
    private static String sha512Hex(String text) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(text.getBytes(StandardCharsets.UTF_8));
        return ByteUtilities.encode(md.digest());
    }

    /** Creates a temporary file with the given content (UTF‑8). */
    private File createTempFile(String name, String content) throws IOException {
        File f = folder.newFile(name);
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return f;
    }

    /** Creates a large temporary file (size in bytes). */
    private File createLargeTempFile(String name, int size) throws IOException {
        File f = folder.newFile(name);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
            byte[] block = new byte[8192];
            Random rnd = new Random(0);
            int remaining = size;
            while (remaining > 0) {
                rnd.nextBytes(block);
                int toWrite = Math.min(remaining, block.length);
                out.write(block, 0, toWrite);
                remaining -= toWrite;
            }
        }
        return f;
    }

    /*** Tests for fastMD5 / fastSHA1 / fastSHA256 / fastSHA512 ***/

    @Test
    public void testFastMD5_SmallFile() throws Exception {
        String content = "The quick brown fox jumps over the lazy dog";
        File f = createTempFile("md5.txt", content);
        String expected = md5Hex(content);
        assertEquals(expected, EncryptionUtilities.fastMD5(f));
    }

    @Test
    public void testFastSHA1_SmallFile() throws Exception {
        String content = "Hello World!";
        File f = createTempFile("sha1.txt", content);
        String expected = sha1Hex(content);
        assertEquals(expected, EncryptionUtilities.fastSHA1(f));
    }

    @Test
    public void testFastSHA256_SmallFile() throws Exception {
        String content = "JUnit test content";
        File f = createTempFile("sha256.txt", content);
        String expected = sha256Hex(content);
        assertEquals(expected, EncryptionUtilities.fastSHA256(f));
    }

    @Test
    public void testFastSHA512_SmallFile() throws Exception {
        String content = "Another test string";
        File f = createTempFile("sha512.txt", content);
        String expected = sha512Hex(content);
        assertEquals(expected, EncryptionUtilities.fastSHA512(f));
    }

    @Test
    public void testFastMD5_LargeFile() throws Exception {
        // 5 MB file – larger than the internal 64KB buffer to ensure looping works
        File f = createLargeTempFile("large.bin", 5 * 1024 * 1024);
        // Compute expected hash using MessageDigest directly (same algorithm)
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream in = Files.newInputStream(f.toPath())) {
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = in.read(buf)) != -1) {
                md.update(buf, 0, r);
            }
        }
        String expected = ByteUtilities.encode(md.digest());
        assertEquals(expected, EncryptionUtilities.fastMD5(f));
    }

    @Test
    public void testFastSHA256_NonexistentFile() {
        File missing = new File(folder.getRoot(), "does-not-exist.txt");
        assertNull(EncryptionUtilities.fastSHA256(missing));
    }

    @Test
    public void testFastSHA512_EmptyFile() throws Exception {
        File empty = folder.newFile("empty.bin");
        // Expected hash of empty input
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        String expected = ByteUtilities.encode(md.digest());
        assertEquals(expected, EncryptionUtilities.fastSHA512(empty));
    }

    /*** Tests for calculateHash (byte‑array based) ***/

    @Test
    public void testCalculateMD5Hash_Null() {
        assertNull(EncryptionUtilities.calculateMD5Hash(null));
    }

    @Test
    public void testCalculateMD5Hash_Regular() throws Exception {
        byte[] data = "sample".getBytes(StandardCharsets.UTF_8);
        String expected = md5Hex("sample");
        assertEquals(expected, EncryptionUtilities.calculateMD5Hash(data));
    }

    @Test
    public void testCalculateSHA1Hash_Null() {
        assertNull(EncryptionUtilities.calculateSHA1Hash(null));
    }

    @Test
    public void testCalculateSHA1Hash_Regular() throws Exception {
        byte[] data = "sample".getBytes(StandardCharsets.UTF_8);
        String expected = sha1Hex("sample");
        assertEquals(expected, EncryptionUtilities.calculateSHA1Hash(data));
    }

    @Test
    public void testCalculateSHA256Hash_Null() {
        assertNull(EncryptionUtilities.calculateSHA256Hash(null));
    }

    @Test
    public void testCalculateSHA256Hash_Regular() throws Exception {
        byte[] data = "sample".getBytes(StandardCharsets.UTF_8);
        String expected = sha256Hex("sample");
        assertEquals(expected, EncryptionUtilities.calculateSHA256Hash(data));
    }

    @Test
    public void testCalculateSHA512Hash_Null() {
        assertNull(EncryptionUtilities.calculateSHA512Hash(null));
    }

    @Test
    public void testCalculateSHA512Hash_Regular() throws Exception {
        byte[] data = "sample".getBytes(StandardCharsets.UTF_8);
        String expected = sha512Hex("sample");
        assertEquals(expected, EncryptionUtilities.calculateSHA512Hash(data));
    }

    /*** Tests for createCipherBytes ***/

    @Test
    public void testCreateCipherBytes_Length128Bits() throws Exception {
        String password = "mySecretPassword";
        byte[] keyBytes = EncryptionUtilities.createCipherBytes(password, 128);
        // The method returns the first 16 bytes (128/8) of the MD5 hex string.
        String md5Hex = md5Hex(password);
        byte[] expected = md5Hex.substring(0, 16).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, keyBytes);
        assertEquals(16, keyBytes.length);
    }

    @Test
    public void testCreateCipherBytes_DifferentLengths() throws Exception {
        String password = "anotherPassword";
        // 192‑bit key = 24 bytes
        byte[] key192 = EncryptionUtilities.createCipherBytes(password, 192);
        assertEquals(24, key192.length);
        // 256‑bit key = 32 bytes
        byte[] key256 = EncryptionUtilities.createCipherBytes(password, 256);
        assertEquals(32, key256.length);
    }

    /*** Tests for encryption / decryption round‑trip ***/

    @Test
    public void testEncryptDecrypt_String_RoundTrip() {
        String key = "myKey123";
        String plain = "This is a secret message!";
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted); // ciphertext should differ
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecrypt_EmptyString() {
        String key = "emptyKey";
        String plain = "";
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        assertNotNull(encrypted);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecrypt_UnicodeString() {
        String key = "unicodeKey";
        String plain = "漢字テスト🚀"; // contains multibyte Unicode characters
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecrypt_Bytes_RoundTrip() {
        String key = "bytesKey";
        byte[] plain = new byte[256];
        new Random(1).nextBytes(plain);
        String encryptedHex = EncryptionUtilities.encryptBytes(key, plain);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(plain, decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void testEncrypt_InvalidKey_Throws() {
        // Passing a null key should cause cipher creation to fail and be wrapped.
        EncryptionUtilities.encrypt(null, "data");
    }

    @Test(expected = IllegalStateException.class)
    public void testDecrypt_InvalidHex_Throws() {
        // Hex string that cannot be decoded (odd length) should trigger IllegalStateException.
        EncryptionUtilities.decrypt("anyKey", "ABC");
    }

    /*** Tests for getDigest and related getters ***/

    @Test
    public void testGetDigest_ValidAlgorithms() {
        assertNotNull(EncryptionUtilities.getMD5Digest());
        assertNotNull(EncryptionUtilities.getSHA1Digest());
        assertNotNull(EncryptionUtilities.getSHA256Digest());
        assertNotNull(EncryptionUtilities.getSHA512Digest());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigest_InvalidAlgorithm() {
        EncryptionUtilities.getDigest("NON-EXISTENT-ALG");
    }

    /*** Tests for calculateFileHash (direct channel) ***/

    @Test
    public void testCalculateFileHash_DirectChannel() throws Exception {
        String content = "Direct channel test content";
        File f = createTempFile("direct.txt", content);
        try (FileChannel channel = new FileInputStream(f).getChannel()) {
            String result = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getMD5Digest());
            assertEquals(md5Hex(content), result);
        }
    }

    @Test
    public void testCalculateFileHash_EmptyChannel() throws Exception {
        File empty = folder.newFile("emptyChannel.bin");
        try (FileChannel channel = new FileInputStream(empty).getChannel()) {
            String result = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getSHA256Digest());
            // SHA‑256 of empty input
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String expected = ByteUtilities.encode(md.digest());
            assertEquals(expected, result);
        }
    }

    /*** Tests for calculateHash generic method ***/

    @Test
    public void testCalculateHash_NullInput() throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    @Test
    public void testCalculateHash_ValidInput() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] data = "hashme".getBytes(StandardCharsets.UTF_8);
        String expected = sha1Hex("hashme");
        assertEquals(expected, EncryptionUtilities.calculateHash(md, data));
    }

    /*** Edge cases for file‑related methods ***/

    @Test
    public void testFastMD5_DirectoryReturnsNull() throws IOException {
        File dir = folder.newFolder("subdir");
        assertNull(EncryptionUtilities.fastMD5(dir));
    }

    @Test
    public void testFastSHA1_ReadOnlyFile() throws Exception {
        String content = "read‑only content";
        File f = createTempFile("readonly.txt", content);
        assertTrue(f.setReadable(true, false)); // make it readable by everyone
        assertTrue(f.setWritable(false, false));
        // Even though it's not writable, reading should succeed.
        assertEquals(sha1Hex(content), EncryptionUtilities.fastSHA1(f));
    }

    /*** Verify that the IV is derived consistently from the key ***/

    @Test
    public void testIvDerivation_IsDeterministic() throws Exception {
        String key = "consistentKey";
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(key);
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(key);
        // The IV parameter spec inside both ciphers must be equal.
        // Unfortunately Cipher does not expose the IV directly, but we can compare the
        // encrypted output of the same plaintext – it must be identical for both ciphers.
        byte[] plain = "samePlain".getBytes(StandardCharsets.UTF_8);
        byte[] encBytes = enc.doFinal(plain);
        byte[] decBytes = dec.doFinal(encBytes); // decrypt back to original
        assertArrayEquals(plain, decBytes);
    }

    /*** Ensure that encrypt/decrypt handle very large data (multiple buffer cycles) ***/

    @Test
    public void testEncryptDecrypt_LargeData() {
        String key = "largeDataKey";
        // 2 MB of random data
        byte[] data = new byte[2 * 1024 * 1024];
        new Random(12345).nextBytes(data);
        String hex = EncryptionUtilities.encryptBytes(key, data);
        byte[] result = EncryptionUtilities.decryptBytes(key, hex);
        assertArrayEquals(data, result);
    }
}
