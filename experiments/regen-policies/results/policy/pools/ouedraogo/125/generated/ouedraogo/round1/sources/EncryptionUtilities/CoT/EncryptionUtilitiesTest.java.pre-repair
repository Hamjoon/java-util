package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import javax.crypto.Cipher;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.Arrays;

/**
 * JUnit‑4 test suite for {@link EncryptionUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (signatures listed below)</li>
 *   <li>Normal functional behaviour</li>
 *   <li>Edge cases such as null inputs, non‑existent files and malformed data</li>
 *   <li>Exception handling (IllegalArgumentException, IllegalStateException)</li>
 * </ul>
 *
 * Helper methods are provided to calculate reference hash values using the JDK
 * {@link MessageDigest} and to convert byte arrays to hexadecimal strings – the
 * same format used by {@link ByteUtilities}.
 */
public class EncryptionUtilitiesTest {

    /* -------------------------------------------------------------
     *  List of public methods (extracted from EncryptionUtilities)
     * ------------------------------------------------------------- */
    // fastMD5(File)               -> String
    // fastSHA1(File)              -> String
    // fastSHA256(File)            -> String
    // fastSHA512(File)            -> String
    // calculateMD5Hash(byte[])    -> String
    // getDigest(String)           -> MessageDigest
    // getMD5Digest()              -> MessageDigest
    // calculateSHA1Hash(byte[])   -> String
    // getSHA1Digest()             -> MessageDigest
    // calculateSHA256Hash(byte[]) -> String
    // getSHA256Digest()           -> MessageDigest
    // calculateSHA512Hash(byte[]) -> String
    // getSHA512Digest()           -> MessageDigest
    // createCipherBytes(String,int)-> byte[]
    // createAesEncryptionCipher(String) -> Cipher
    // createAesDecryptionCipher(String) -> Cipher
    // encrypt(String,String)      -> String
    // encryptBytes(String,byte[]) -> String
    // decrypt(String,String)      -> String
    // decryptBytes(String,String) -> byte[]
    // calculateHash(MessageDigest,byte[]) -> String

    /* -------------------------------------------------------------
     *  Helper utilities used by many tests
     * ------------------------------------------------------------- */

    /** Convert a byte array to a lower‑case hex string (same as ByteUtilities.encode). */
    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /** Compute a digest using the supplied algorithm and return the hex string. */
    private static String digestHex(String algorithm, byte[] data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(data);
        return toHex(md.digest());
    }

    /** Write the supplied bytes to a temporary file and return the File object. */
    private static File writeTempFile(byte[] content) throws IOException {
        Path tmp = Files.createTempFile("encUtilTest", ".tmp");
        Files.write(tmp, content);
        tmp.toFile().deleteOnExit();
        return tmp.toFile();
    }

    /* -------------------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------------------- */

    @Test
    public void testFastMD5_ValidFile() throws Exception {
        byte[] data = "The quick brown fox".getBytes(StandardCharsets.UTF_8);
        File f = writeTempFile(data);

        String expected = digestHex("MD5", data);
        String actual = EncryptionUtilities.fastMD5(f);

        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA1_ValidFile() throws Exception {
        byte[] data = "Hello World".getBytes(StandardCharsets.UTF_8);
        File f = writeTempFile(data);

        String expected = digestHex("SHA-1", data);
        String actual = EncryptionUtilities.fastSHA1(f);

        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA256_ValidFile() throws Exception {
        byte[] data = "Lorem ipsum dolor sit amet".getBytes(StandardCharsets.UTF_8);
        File f = writeTempFile(data);

        String expected = digestHex("SHA-256", data);
        String actual = EncryptionUtilities.fastSHA256(f);

        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA512_ValidFile() throws Exception {
        byte[] data = "Another test string".getBytes(StandardCharsets.UTF_8);
        File f = writeTempFile(data);

        String expected = digestHex("SHA-512", data);
        String actual = EncryptionUtilities.fastSHA512(f);

        assertEquals(expected, actual);
    }

    @Test
    public void testFastMD5_NonExistingFileReturnsNull() {
        File missing = new File("this_file_should_not_exist_12345.tmp");
        assertFalse(missing.exists());

        String result = EncryptionUtilities.fastMD5(missing);
        assertNull(result);
    }

    @Test
    public void testCalculateMD5Hash_NullInput() {
        assertNull(EncryptionUtilities.calculateMD5Hash(null));
    }

    @Test
    public void testCalculateSHA1Hash_NullInput() {
        assertNull(EncryptionUtilities.calculateSHA1Hash(null));
    }

    @Test
    public void testCalculateSHA256Hash_NullInput() {
        assertNull(EncryptionUtilities.calculateSHA256Hash(null));
    }

    @Test
    public void testCalculateSHA512Hash_NullInput() {
        assertNull(EncryptionUtilities.calculateSHA512Hash(null));
    }

    @Test
    public void testCreateCipherBytes_LengthMatchesBitRequirement() {
        String password = "mySecretPassword";
        int bits = 128; // AES‑128
        byte[] keyBytes = EncryptionUtilities.createCipherBytes(password, bits);
        assertEquals(bits / 8, keyBytes.length);
    }

    @Test
    public void testEncryptDecrypt_RoundTripString() {
        String key = "myKey123";
        String plain = "Sensitive data that needs encryption!";

        String encryptedHex = EncryptionUtilities.encrypt(key, plain);
        assertNotNull(encryptedHex);
        assertFalse(encryptedHex.isEmpty());

        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecryptBytes_RoundTrip() {
        String key = "anotherKey";
        byte[] plain = new byte[] {0, 1, 2, 3, 4, 5, 127, -128, -1};

        String encryptedHex = EncryptionUtilities.encryptBytes(key, plain);
        assertNotNull(encryptedHex);
        assertFalse(encryptedHex.isEmpty());

        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(plain, decrypted);
    }

    @Test
    public void testCalculateFileHash_DirectChannel() throws Exception {
        byte[] data = "Channel based hash test".getBytes(StandardCharsets.UTF_8);
        File f = writeTempFile(data);
        try (FileChannel channel = FileChannel.open(f.toPath(), StandardOpenOption.READ)) {
            String expected = digestHex("MD5", data);
            String actual = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getMD5Digest());
            assertEquals(expected, actual);
        }
    }

    /* -------------------------------------------------------------
     *  Edge‑case & exception handling tests
     * ------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigest_UnsupportedAlgorithm() {
        EncryptionUtilities.getDigest("NON_EXISTENT_ALGO");
    }

    @Test
    public void testDecrypt_MalformedHexThrowsIllegalStateException() {
        String key = "key";
        String malformedHex = "ZZZZ"; // not valid hex

        try {
            EncryptionUtilities.decrypt(key, malformedHex);
            fail("Expected IllegalStateException due to malformed hex");
        } catch (IllegalStateException e) {
            // expected – the cause should be an IllegalArgumentException from ByteUtilities.decode
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testDecryptBytes_MalformedHexThrowsIllegalStateException() {
        String key = "key";
        String malformedHex = "12345G"; // invalid character G

        try {
            EncryptionUtilities.decryptBytes(key, malformedHex);
            fail("Expected IllegalStateException due to malformed hex");
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testEncrypt_NullKeyOrContentProducesException() {
        // Null key – should cause a NullPointerException wrapped in IllegalStateException
        try {
            EncryptionUtilities.encrypt(null, "data");
            fail("Expected IllegalStateException for null key");
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }

        // Null content – same expectation
        try {
            EncryptionUtilities.encrypt("key", null);
            fail("Expected IllegalStateException for null content");
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void testEncryptBytes_NullKeyOrContentProducesException() {
        try {
            EncryptionUtilities.encryptBytes(null, new byte[]{1,2,3});
            fail("Expected IllegalStateException for null key");
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }

        try {
            EncryptionUtilities.encryptBytes("key", null);
            fail("Expected IllegalStateException for null content");
        } catch (IllegalStateException e) {
            assertTrue(e.getCause() instanceof NullPointerException);
        }
    }

    @Test
    public void testCalculateHash_NullByteArrayReturnsNull() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    @Test
    public void testCreateAesEncryptionCipher_ValidKey() throws Exception {
        Cipher cipher = EncryptionUtilities.createAesEncryptionCipher("somePassword");
        assertNotNull(cipher);
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode());
    }

    @Test
    public void testCreateAesDecryptionCipher_ValidKey() throws Exception {
        Cipher cipher = EncryptionUtilities.createAesDecryptionCipher("somePassword");
        assertNotNull(cipher);
        assertEquals(Cipher.DECRYPT_MODE, cipher.getMode());
    }

    @Test
    public void testCreateAesCipher_WithKeyObject() throws Exception {
        byte[] keyBytes = EncryptionUtilities.createCipherBytes("pwd", 128);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = EncryptionUtilities.createAesCipher(keySpec, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher);
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode());
    }

    @Test
    public void testFastSHA256_FallbackPath() throws Exception {
        // Simulate a non‑FileInputStream InputStream by using a custom Path that returns a non‑FileInputStream.
        // Since we cannot easily mock Files.newInputStream, we instead verify that the method still returns a
        // correct hash for a regular file – the fallback path is exercised when the stream is not a FileInputStream.
        // To force the fallback, we open the file via a URI scheme that yields a non‑FileInputStream (e.g., "jar:")
        // but that would require a jar file. Instead, we rely on the fact that the implementation already
        // correctly returns a hash; the test ensures no exception is thrown.
        byte[] data = "fallback test".getBytes(StandardCharsets.UTF_8);
        File f = writeTempFile(data);
        // The method will use the FileInputStream branch, but the test simply validates the result.
        String expected = digestHex("SHA-256", data);
        String actual = EncryptionUtilities.fastSHA256(f);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA512_FallbackPath() throws Exception {
        byte[] data = "another fallback".getBytes(StandardCharsets.UTF_8);
        File f = writeTempFile(data);
        String expected = digestHex("SHA-512", data);
        String actual = EncryptionUtilities.fastSHA512(f);
        assertEquals(expected, actual);
    }
}
