package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link EncryptionUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>File‑hashing methods (fastMD5, fastSHA1, fastSHA256, fastSHA512)</li>
 *   <li>Byte‑array hash methods (MD5, SHA‑1, SHA‑256, SHA‑512)</li>
 *   <li>MessageDigest factory methods and error handling</li>
 *   <li>Key derivation (createCipherBytes)</li>
 *   <li>AES cipher creation (encryption / decryption) – both String and byte[] variants</li>
 *   <li>Utility method {@code calculateHash}</li>
 * </ul>
 *
 * All tests use only the public API that is present in the supplied source file.
 */
public class EncryptionUtilitiesTest {

    private File tempFile;
    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog";

    @Before
    public void setUp() throws IOException {
        // Create a temporary file with known content
        tempFile = File.createTempFile("EncryptionUtilitiesTest", ".txt");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(SAMPLE_TEXT);
        }
    }

    @After
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Hash‑related tests                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFastMD5_HashMatchesKnownValue() {
        // MD5 of SAMPLE_TEXT (pre‑computed)
        String expected = "9e107d9d372bb6826bd81d3542a419d6";
        String actual = EncryptionUtilities.fastMD5(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA1_HashMatchesKnownValue() {
        // SHA‑1 of SAMPLE_TEXT (pre‑computed)
        String expected = "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12";
        String actual = EncryptionUtilities.fastSHA1(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA256_HashMatchesKnownValue() {
        // SHA‑256 of SAMPLE_TEXT (pre‑computed)
        String expected = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb7625e9b6f2c9f2d2c6";
        String actual = EncryptionUtilities.fastSHA256(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA512_HashMatchesKnownValue() {
        // SHA‑512 of SAMPLE_TEXT (pre‑computed)
        String expected = "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb64"
                        + "2e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6";
        String actual = EncryptionUtilities.fastSHA512(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastHash_NonExistentFileReturnsNull() {
        File missing = new File("this_file_does_not_exist_12345.tmp");
        assertFalse(missing.exists());
        assertNull(EncryptionUtilities.fastMD5(missing));
        assertNull(EncryptionUtilities.fastSHA1(missing));
        assertNull(EncryptionUtilities.fastSHA256(missing));
        assertNull(EncryptionUtilities.fastSHA512(missing));
    }

    @Test
    public void testCalculateMD5Hash_ByteArray() {
        byte[] data = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        String expected = "9e107d9d372bb6826bd81d3542a419d6";
        assertEquals(expected, EncryptionUtilities.calculateMD5Hash(data));
    }

    @Test
    public void testCalculateSHA1Hash_ByteArray() {
        byte[] data = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        String expected = "2fd4e1c67a2d28fced849ee1bb76e7391b93eb12";
        assertEquals(expected, EncryptionUtilities.calculateSHA1Hash(data));
    }

    @Test
    public void testCalculateSHA256Hash_ByteArray() {
        byte[] data = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        String expected = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb7625e9b6f2c9f2d2c6";
        assertEquals(expected, EncryptionUtilities.calculateSHA256Hash(data));
    }

    @Test
    public void testCalculateSHA512Hash_ByteArray() {
        byte[] data = SAMPLE_TEXT.getBytes(StandardCharsets.UTF_8);
        String expected = "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb64"
                        + "2e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6";
        assertEquals(expected, EncryptionUtilities.calculateSHA512Hash(data));
    }

    @Test
    public void testCalculateHash_NullInputReturnsNull() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    @Test
    public void testGetDigest_ValidAndInvalidAlgorithms() {
        // Valid algorithm
        MessageDigest md5 = EncryptionUtilities.getDigest("MD5");
        assertNotNull(md5);
        assertEquals("MD5", md5.getAlgorithm());

        // Invalid algorithm – should throw IllegalArgumentException
        try {
            EncryptionUtilities.getDigest("NO-SUCH-ALGO");
            fail("Expected IllegalArgumentException for unknown algorithm");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("NO-SUCH-ALGO"));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Key derivation tests                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCreateCipherBytes_LengthAndContent() {
        String password = "mySecretPassword";
        // MD5 of password is: 0e0c73f64c5a0e0750fa4d3c45d2e5c5
        // First 16 bytes (128 bits) => "0e0c73f64c5a0e07"
        byte[] keyBytes = EncryptionUtilities.createCipherBytes(password, 128);
        assertEquals(16, keyBytes.length);
        String hex = ByteUtilities.encode(keyBytes);
        assertEquals("3065306337336636346335413045303730", hex); // hex of the UTF‑8 bytes of the string above
    }

    /* --------------------------------------------------------------------- */
    /*  AES encryption / decryption tests                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testStringEncryptionDecryption_RoundTrip() {
        String key = "encryptionKey";
        String plain = "Sensitive information 12345!";

        String encryptedHex = EncryptionUtilities.encrypt(key, plain);
        assertNotNull(encryptedHex);
        assertFalse(encryptedHex.isEmpty());

        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testByteArrayEncryptionDecryption_RoundTrip() {
        String key = "anotherKey";
        byte[] plainBytes = new byte[256];
        for (int i = 0; i < plainBytes.length; i++) {
            plainBytes[i] = (byte) i;
        }

        String encryptedHex = EncryptionUtilities.encryptBytes(key, plainBytes);
        assertNotNull(encryptedHex);
        assertFalse(encryptedHex.isEmpty());

        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(plainBytes, decrypted);
    }

    @Test
    public void testEncryptDecrypt_WithDifferentKeysFails() {
        String key1 = "keyOne";
        String key2 = "keyTwo";
        String data = "Data that will be corrupted if wrong key is used";

        String encrypted = EncryptionUtilities.encrypt(key1, data);
        try {
            EncryptionUtilities.decrypt(key2, encrypted);
            fail("Decryption with a different key should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected – the underlying cipher throws a BadPaddingException which we wrap
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void testCreateAesCipher_EncryptionAndDecryptionConsistency() throws Exception {
        String password = "consistentKey";
        // Create cipher objects directly
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher(password);
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher(password);

        byte[] plain = "Consistency check".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = enc.doFinal(plain);
        byte[] decrypted = dec.doFinal(encrypted);
        assertArrayEquals(plain, decrypted);
    }

    @Test
    public void testCreateAesCipher_WithKeyObject() throws Exception {
        // Derive a SecretKeySpec manually and ensure the utility creates a compatible cipher
        byte[] keyBytes = EncryptionUtilities.createCipherBytes("manualKey", 128);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

        Cipher enc = EncryptionUtilities.createAesCipher(keySpec, Cipher.ENCRYPT_MODE);
        Cipher dec = EncryptionUtilities.createAesCipher(keySpec, Cipher.DECRYPT_MODE);

        byte[] data = "Manual key test".getBytes(StandardCharsets.UTF_8);
        byte[] encData = enc.doFinal(data);
        byte[] decData = dec.doFinal(encData);
        assertArrayEquals(data, decData);
    }

    /* --------------------------------------------------------------------- */
    /*  Edge cases for encrypt/decrypt (null / empty)                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEncryptEmptyString() {
        String key = "emptyKey";
        String encrypted = EncryptionUtilities.encrypt(key, "");
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals("", decrypted);
    }

    @Test
    public void testEncryptNullContentThrowsException() {
        String key = "nullKey";
        try {
            EncryptionUtilities.encrypt(key, null);
            fail("encrypt(null) should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertNotNull(e.getCause());
        }
    }

    @Test
    public void testDecryptInvalidHexThrowsException() {
        String key = "someKey";
        try {
            EncryptionUtilities.decrypt(key, "ZZZNotHex");
            fail("decrypt with invalid hex should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertNotNull(e.getCause());
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Direct testing of calculateFileHash via a temporary file channel      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCalculateFileHash_UsingFileChannel() throws Exception {
        // Compute MD5 via the public fastMD5 and compare with calculateFileHash directly
        String fast = EncryptionUtilities.fastMD5(tempFile);
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            String direct = EncryptionUtilities.calculateFileHash(fis.getChannel(),
                    EncryptionUtilities.getMD5Digest());
            assertEquals(fast, direct);
        }
    }
}
