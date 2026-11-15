package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class EncryptionUtilitiesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    /* ----------------------------------------------------------------------
     *  Hash utilities – byte array based
     * ---------------------------------------------------------------------- */

    @Test
    public void testCalculateMD5Hash_KnownVector() {
        byte[] data = "abc".getBytes();
        String expected = "900150983cd24fb0d6963f7d28e17f72";
        assertEquals(expected, EncryptionUtilities.calculateMD5Hash(data));
    }

    @Test
    public void testCalculateSHA1Hash_KnownVector() {
        byte[] data = "abc".getBytes();
        String expected = "a9993e364706816aba3e25717850c26c9cd0d89d";
        assertEquals(expected, EncryptionUtilities.calculateSHA1Hash(data));
    }

    @Test
    public void testCalculateSHA256Hash_KnownVector() {
        byte[] data = "abc".getBytes();
        String expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        assertEquals(expected, EncryptionUtilities.calculateSHA256Hash(data));
    }

    @Test
    public void testCalculateSHA512Hash_KnownVector() {
        byte[] data = "abc".getBytes();
        String expected = "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a"
                        + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f";
        assertEquals(expected, EncryptionUtilities.calculateSHA512Hash(data));
    }

    @Test
    public void testCalculateHash_NullInputReturnsNull() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    @Test
    public void testGetDigest_ValidAlgorithm() throws NoSuchAlgorithmException {
        MessageDigest md = EncryptionUtilities.getDigest("MD5");
        assertEquals("MD5", md.getAlgorithm());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigest_InvalidAlgorithm() {
        EncryptionUtilities.getDigest("NON_EXISTENT_ALGO");
    }

    @Test
    public void testCreateCipherBytes_LengthAndContent() {
        String password = "secretPassword";
        // 128 bits => 16 bytes
        byte[] keyBytes = EncryptionUtilities.createCipherBytes(password, 128);
        assertEquals(16, keyBytes.length);

        // Verify deterministic output by recomputing
        byte[] keyBytes2 = EncryptionUtilities.createCipherBytes(password, 128);
        assertArrayEquals(keyBytes, keyBytes2);
    }

    /* ----------------------------------------------------------------------
     *  File‑based fast hash methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testFastMD5_File() throws Exception {
        File f = folder.newFile("md5.txt");
        writeStringToFile(f, "Hello World");
        String expected = EncryptionUtilities.calculateMD5Hash("Hello World".getBytes());
        assertEquals(expected, EncryptionUtilities.fastMD5(f));
    }

    @Test
    public void testFastSHA1_File() throws Exception {
        File f = folder.newFile("sha1.txt");
        writeStringToFile(f, "Hello World");
        String expected = EncryptionUtilities.calculateSHA1Hash("Hello World".getBytes());
        assertEquals(expected, EncryptionUtilities.fastSHA1(f));
    }

    @Test
    public void testFastSHA256_File() throws Exception {
        File f = folder.newFile("sha256.txt");
        writeStringToFile(f, "Hello World");
        String expected = EncryptionUtilities.calculateSHA256Hash("Hello World".getBytes());
        assertEquals(expected, EncryptionUtilities.fastSHA256(f));
    }

    @Test
    public void testFastSHA512_File() throws Exception {
        File f = folder.newFile("sha512.txt");
        writeStringToFile(f, "Hello World");
        String expected = EncryptionUtilities.calculateSHA512Hash("Hello World".getBytes());
        assertEquals(expected, EncryptionUtilities.fastSHA512(f));
    }

    @Test
    public void testFastMD5_NonExistentFileReturnsNull() {
        File f = new File(folder.getRoot(), "doesNotExist.txt");
        assertFalse(f.exists());
        assertNull(EncryptionUtilities.fastMD5(f));
    }

    @Test
    public void testCalculateFileHash_DirectChannel() throws Exception {
        File f = folder.newFile("channel.txt");
        byte[] data = "Channel test data".getBytes();
        try (OutputStream os = new FileOutputStream(f)) {
            os.write(data);
        }

        try (FileChannel channel = new FileInputStream(f).getChannel()) {
            String expected = EncryptionUtilities.calculateMD5Hash(data);
            String actual = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getMD5Digest());
            assertEquals(expected, actual);
        }
    }

    /* ----------------------------------------------------------------------
     *  AES encryption / decryption (string & byte[])
     * ---------------------------------------------------------------------- */

    @Test
    public void testEncryptDecrypt_String_RoundTrip() {
        String key = "mySecretKey";
        String plain = "The quick brown fox jumps over the lazy dog";

        String encrypted = EncryptionUtilities.encrypt(key, plain);
        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted);

        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecrypt_EmptyString() {
        String key = "emptyKey";
        String plain = "";

        String encrypted = EncryptionUtilities.encrypt(key, plain);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecrypt_Bytes_RoundTrip() {
        String key = "byteKey123";
        byte[] plain = "binary data \u0000\u00FF".getBytes();

        String encryptedHex = EncryptionUtilities.encryptBytes(key, plain);
        assertNotNull(encryptedHex);

        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecrypt_EmptyBytes() {
        String key = "emptyBytesKey";
        byte[] plain = new byte[0];

        String encryptedHex = EncryptionUtilities.encryptBytes(key, plain);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(plain, decrypted);
    }

    @Test
    public void testCreateAesCipher_EncryptThenDecrypt_WithSameKey() throws Exception {
        String password = "cipherPassword";
        byte[] data = "sample data for cipher".getBytes();

        // Encryption cipher
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(password);
        byte[] encrypted = enc.doFinal(data);

        // Decryption cipher
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(password);
        byte[] decrypted = dec.doFinal(encrypted);

        assertArrayEquals(data, decrypted);
    }

    /* ----------------------------------------------------------------------
     *  Helper methods
     * ---------------------------------------------------------------------- */

    private void writeStringToFile(File file, String content) throws Exception {
        try (OutputStream os = new FileOutputStream(file)) {
            os.write(content.getBytes());
        }
    }
}
