package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.security.MessageDigest;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link EncryptionUtilities}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>All fast‑hash methods (MD5, SHA‑1, SHA‑256, SHA‑512)</li>
 *   <li>Byte‑array hash helpers</li>
 *   <li>MessageDigest factory methods</li>
 *   <li>Key‑generation utilities</li>
 *   <li>AES encryption/decryption (String and byte[] variants)</li>
 *   <li>Generic {@code calculateHash} behaviour</li>
 *   <li>Error handling (missing file, unknown digest, malformed hex, wrong key)</li>
 * </ul>
 * </p>
 */
public class EncryptionUtilitiesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File testFile;
    private byte[] fileContent;

    @Before
    public void setUp() throws IOException {
        // Create a temporary file with deterministic content
        fileContent = "The quick brown fox jumps over the lazy dog".getBytes("UTF-8");
        testFile = tmp.newFile("sample.txt");
        try (FileOutputStream out = new FileOutputStream(testFile)) {
            out.write(fileContent);
        }
    }

    /* -------------------------------------------------------------
     * Helper methods to compute expected hashes using standard JDK APIs
     * ------------------------------------------------------------- */
    private static String toHex(byte[] bytes) {
        return ByteUtilities.encode(bytes);
    }

    private static String md5Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        return toHex(md.digest());
    }

    private static String sha1Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(data);
        return toHex(md.digest());
    }

    private static String sha256Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data);
        return toHex(md.digest());
    }

    private static String sha512Hex(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(data);
        return toHex(md.digest());
    }

    /* -------------------------------------------------------------
     * fast* hash methods – file based
     * ------------------------------------------------------------- */
    @Test
    public void testFastMD5() throws Exception {
        String expected = md5Hex(fileContent);
        assertEquals(expected, EncryptionUtilities.fastMD5(testFile));
    }

    @Test
    public void testFastSHA1() throws Exception {
        String expected = sha1Hex(fileContent);
        assertEquals(expected, EncryptionUtilities.fastSHA1(testFile));
    }

    @Test
    public void testFastSHA256() throws Exception {
        String expected = sha256Hex(fileContent);
        assertEquals(expected, EncryptionUtilities.fastSHA256(testFile));
    }

    @Test
    public void testFastSHA512() throws Exception {
        String expected = sha512Hex(fileContent);
        assertEquals(expected, EncryptionUtilities.fastSHA512(testFile));
    }

    @Test
    public void testFastHashFileNotFoundReturnsNull() {
        File missing = new File(tmp.getRoot(), "does-not-exist.txt");
        assertFalse(missing.exists());
        assertNull(EncryptionUtilities.fastMD5(missing));
        assertNull(EncryptionUtilities.fastSHA1(missing));
        assertNull(EncryptionUtilities.fastSHA256(missing));
        assertNull(EncryptionUtilities.fastSHA512(missing));
    }

    /* -------------------------------------------------------------
     * calculateFileHash – direct channel usage
     * ------------------------------------------------------------- */
    @Test
    public void testCalculateFileHashMatchesFastMethods() throws Exception {
        try (FileChannel channel = new FileInputStream(testFile).getChannel()) {
            String viaChannel = EncryptionUtilities.calculateFileHash(channel, EncryptionUtilities.getMD5Digest());
            String viaFast = EncryptionUtilities.fastMD5(testFile);
            assertEquals(viaFast, viaChannel);
        }
    }

    /* -------------------------------------------------------------
     * Byte‑array hash helpers
     * ------------------------------------------------------------- */
    @Test
    public void testCalculateMD5Hash() throws Exception {
        assertEquals(md5Hex(fileContent), EncryptionUtilities.calculateMD5Hash(fileContent));
    }

    @Test
    public void testCalculateSHA1Hash() throws Exception {
        assertEquals(sha1Hex(fileContent), EncryptionUtilities.calculateSHA1Hash(fileContent));
    }

    @Test
    public void testCalculateSHA256Hash() throws Exception {
        assertEquals(sha256Hex(fileContent), EncryptionUtilities.calculateSHA256Hash(fileContent));
    }

    @Test
    public void testCalculateSHA512Hash() throws Exception {
        assertEquals(sha512Hex(fileContent), EncryptionUtilities.calculateSHA512Hash(fileContent));
    }

    @Test
    public void testCalculateHashNullInputReturnsNull() {
        MessageDigest md = EncryptionUtilities.getMD5Digest();
        assertNull(EncryptionUtilities.calculateHash(md, null));
    }

    /* -------------------------------------------------------------
     * MessageDigest factory methods
     * ------------------------------------------------------------- */
    @Test
    public void testGetDigestValidAlgorithms() throws Exception {
        assertEquals("MD5", EncryptionUtilities.getMD5Digest().getAlgorithm());
        assertEquals("SHA-1", EncryptionUtilities.getSHA1Digest().getAlgorithm());
        assertEquals("SHA-256", EncryptionUtilities.getSHA256Digest().getAlgorithm());
        assertEquals("SHA-512", EncryptionUtilities.getSHA512Digest().getAlgorithm());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetDigestInvalidAlgorithmThrows() {
        EncryptionUtilities.getDigest("THIS-ALGO-DOES-NOT-EXIST");
    }

    /* -------------------------------------------------------------
     * Cipher key generation
     * ------------------------------------------------------------- */
    @Test
    public void testCreateCipherBytesProducesExpectedKey() throws Exception {
        // MD5 of "password"
        String md5 = md5Hex("password".getBytes("UTF-8"));
        // first 16 characters (128 bits / 8)
        String expectedHex = md5.substring(0, 16);
        byte[] expectedBytes = expectedHex.getBytes("UTF-8");
        byte[] actual = EncryptionUtilities.createCipherBytes("password", 128);
        assertArrayEquals(expectedBytes, actual);
    }

    /* -------------------------------------------------------------
     * AES encryption / decryption (String)
     * ------------------------------------------------------------- */
    @Test
    public void testEncryptDecryptStringRoundTrip() {
        String key = "mySecretKey";
        String plain = "Hello, EncryptionUtilities!";
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        assertNotNull(encrypted);
        // Encrypted data must be hex string (even length)
        assertTrue(encrypted.matches("[0-9a-fA-F]+"));
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecryptEmptyString() {
        String key = "emptyKey";
        String plain = "";
        String encrypted = EncryptionUtilities.encrypt(key, plain);
        assertNotNull(encrypted);
        String decrypted = EncryptionUtilities.decrypt(key, encrypted);
        assertEquals(plain, decrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptWithWrongKeyThrows() {
        String key1 = "keyOne";
        String key2 = "keyTwo";
        String encrypted = EncryptionUtilities.encrypt(key1, "secret");
        // Attempt decryption with a different key – should fail and be wrapped in IllegalStateException
        EncryptionUtilities.decrypt(key2, encrypted);
    }

    @Test(expected = IllegalStateException.class)
    public void testDecryptWithMalformedHexThrows() {
        EncryptionUtilities.decrypt("anyKey", "ZZZZ"); // not valid hex
    }

    /* -------------------------------------------------------------
     * AES encryption / decryption (byte[])
     * ------------------------------------------------------------- */
    @Test
    public void testEncryptDecryptBytesRoundTrip() {
        String key = "binaryKey";
        byte[] original = new byte[]{0, 1, 2, 3, 4, 5, (byte) 255, (byte) 128};
        String encryptedHex = EncryptionUtilities.encryptBytes(key, original);
        assertNotNull(encryptedHex);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(original, decrypted);
    }

    @Test
    public void testEncryptBytesEmptyArray() {
        String key = "emptyBytesKey";
        byte[] original = new byte[0];
        String encryptedHex = EncryptionUtilities.encryptBytes(key, original);
        assertNotNull(encryptedHex);
        byte[] decrypted = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(original, decrypted);
    }

    /* -------------------------------------------------------------
     * Cipher creation helpers – basic sanity checks
     * ------------------------------------------------------------- */
    @Test
    public void testCreateAesEncryptionCipherIsNotNull() throws Exception {
        Cipher enc = EncryptionUtilities.createAesEncryptionCipher("testKey");
        assertNotNull(enc);
        assertEquals(Cipher.ENCRYPT_MODE, enc.getMode());
    }

    @Test
    public void testCreateAesDecryptionCipherIsNotNull() throws Exception {
        Cipher dec = EncryptionUtilities.createAesDecryptionCipher("testKey");
        assertNotNull(dec);
        assertEquals(Cipher.DECRYPT_MODE, dec.getMode());
    }

    @Test
    public void testCreateAesCipherWithKeyObject() throws Exception {
        // Build a SecretKeySpec manually and compare the IV generation
        byte[] keyBytes = EncryptionUtilities.createCipherBytes("myKey", 128);
        SecretKeySpec spec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = EncryptionUtilities.createAesCipher(spec, Cipher.ENCRYPT_MODE);
        assertNotNull(cipher);
        assertEquals(Cipher.ENCRYPT_MODE, cipher.getMode());
    }

    /* -------------------------------------------------------------
     * Additional edge cases
     * ------------------------------------------------------------- */
    @Test
    public void testFastHashWithEmptyFile() throws IOException, Exception {
        File empty = tmp.newFile("empty.txt");
        // Ensure file is truly empty
        try (FileOutputStream out = new FileOutputStream(empty)) {
            out.write(new byte[0]);
        }
        assertEquals(md5Hex(new byte[0]), EncryptionUtilities.fastMD5(empty));
        assertEquals(sha1Hex(new byte[0]), EncryptionUtilities.fastSHA1(empty));
        assertEquals(sha256Hex(new byte[0]), EncryptionUtilities.fastSHA256(empty));
        assertEquals(sha512Hex(new byte[0]), EncryptionUtilities.fastSHA512(empty));
    }

    @Test
    public void testCalculateHashWithEmptyByteArray() throws Exception {
        MessageDigest md = EncryptionUtilities.getSHA256Digest();
        String expected = toHex(md.digest()); // digest of zero‑length update is just the initial digest
        // Reset the digest for a fair comparison
        md = EncryptionUtilities.getSHA256Digest();
        String actual = EncryptionUtilities.calculateHash(md, new byte[0]);
        assertEquals(expected, actual);
    }
}
