package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class EncryptionUtilitiesTest
{
    private File tempFile;

    @Before
    public void setUp() throws Exception
    {
        // create a temporary file that will be used by several tests
        tempFile = File.createTempFile("EncryptionUtilitiesTest", ".txt");
        try (FileOutputStream out = new FileOutputStream(tempFile))
        {
            out.write("The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8));
        }
    }

    @After
    public void tearDown() throws Exception
    {
        if (tempFile != null && tempFile.exists())
        {
            tempFile.delete();
        }
    }

    /* --------------------------------------------------------------------- */
    /* Helper method to convert a byte[] into a lower‑case hex string.       */
    private static String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testCalculateMD5Hash()
    {
        String input = "hello";
        String expected = "5d41402abc4b2a76b9719d911017c592";
        assertEquals(expected, EncryptionUtilities.calculateMD5Hash(input.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testCalculateSHA1Hash()
    {
        String input = "hello";
        String expected = "aaf4c61ddcc5e8a2dabede0f3b482cd9aea9434d";
        assertEquals(expected, EncryptionUtilities.calculateSHA1Hash(input.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testCalculateSHA256Hash()
    {
        String input = "hello";
        String expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        assertEquals(expected, EncryptionUtilities.calculateSHA256Hash(input.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testCalculateSHA512Hash()
    {
        String input = "hello";
        String expected = "9b71d224bd62f3785d96d46ad3ea3d73319bfbc" +
                "c4  0  5 2  9  0  9 0  1  5  6  5  5  0  5  2  2  4  2  6  9  6  5  5  1  0  4  1  2  1  6  2  2  5  4  5  0  2  5  2  0  5  1  2  5  2  2  2  2  5  1  2  5  2  5  3  2  5  2  1  2  5  5  3  2  5  2  5  2  2  5  2  5  3  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2  5  2";
        // Compute expected using Java's MessageDigest to avoid manual errors
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            expected = bytesToHex(digest);
        }
        catch (NoSuchAlgorithmException e)
        {
            fail("SHA-512 algorithm not available");
        }
        assertEquals(expected, EncryptionUtilities.calculateSHA512Hash(input.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void testCalculateHashNull()
    {
        assertNull(EncryptionUtilities.calculateHash(EncryptionUtilities.getMD5Digest(), null));
    }

    @Test
    public void testGetDigestInvalid()
    {
        try
        {
            EncryptionUtilities.getDigest("NON-EXISTENT-DIGEST");
            fail("Expected IllegalArgumentException for unknown digest algorithm");
        }
        catch (IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("does not exist"));
        }
    }

    @Test
    public void testCreateCipherBytes()
    {
        String key = "mySecretKey";
        int bits = 128; // 16 bytes
        byte[] cipherBytes = EncryptionUtilities.createCipherBytes(key, bits);
        assertEquals(bits / 8, cipherBytes.length);

        // The method uses the MD5 hash of the key and then truncates.
        String md5 = EncryptionUtilities.calculateMD5Hash(key.getBytes(StandardCharsets.UTF_8));
        String expectedPrefix = md5.substring(0, bits / 8);
        assertEquals(expectedPrefix, new String(cipherBytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testFastMD5()
    {
        String expected = bytesToHex(computeDigestForFile(tempFile, "MD5"));
        String actual = EncryptionUtilities.fastMD5(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA1()
    {
        String expected = bytesToHex(computeDigestForFile(tempFile, "SHA-1"));
        String actual = EncryptionUtilities.fastSHA1(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA256()
    {
        String expected = bytesToHex(computeDigestForFile(tempFile, "SHA-256"));
        String actual = EncryptionUtilities.fastSHA256(tempFile);
        assertEquals(expected, actual);
    }

    @Test
    public void testFastSHA512()
    {
        String expected = bytesToHex(computeDigestForFile(tempFile, "SHA-512"));
        String actual = EncryptionUtilities.fastSHA512(tempFile);
        assertEquals(expected, actual);
    }

    private byte[] computeDigestForFile(File file, String algorithm)
    {
        try (FileInputStream fis = new FileInputStream(file))
        {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1)
            {
                md.update(buffer, 0, read);
            }
            return md.digest();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCalculateFileHashDirect()
    {
        try (RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
             FileChannel channel = raf.getChannel())
        {
            MessageDigest md = EncryptionUtilities.getMD5Digest();
            String expected = bytesToHex(EncryptionUtilities.calculateFileHash(channel, md));
            // Reset channel position to start for a fresh read
            channel.position(0);
            String actual = EncryptionUtilities.fastMD5(tempFile);
            assertEquals(expected, actual);
        }
        catch (Exception e)
        {
            fail("Exception during testCalculateFileHashDirect: " + e);
        }
    }

    @Test
    public void testEncryptDecryptString()
    {
        String key = "myPassword";
        String plain = "Secret Message 123!";

        String encryptedHex = EncryptionUtilities.encrypt(key, plain);
        assertNotNull(encryptedHex);
        assertNotEquals(plain, encryptedHex); // ciphertext should differ

        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecryptEmptyString()
    {
        String key = "emptyKey";
        String plain = "";

        String encryptedHex = EncryptionUtilities.encrypt(key, plain);
        String decrypted = EncryptionUtilities.decrypt(key, encryptedHex);
        assertEquals(plain, decrypted);
    }

    @Test
    public void testEncryptDecryptBytes()
    {
        String key = "bytesKey";
        byte[] plainBytes = "Binary\0Data\1Test".getBytes(StandardCharsets.UTF_8);

        String encryptedHex = EncryptionUtilities.encryptBytes(key, plainBytes);
        byte[] decryptedBytes = EncryptionUtilities.decryptBytes(key, encryptedHex);
        assertArrayEquals(plainBytes, decryptedBytes);
    }

    @Test
    public void testCreateAesCipherModes()
    {
        try
        {
            Cipher enc = EncryptionUtilities.createAesEncryptionCipher("modeKey");
            Cipher dec = EncryptionUtilities.createAesDecryptionCipher("modeKey");
            assertEquals(Cipher.ENCRYPT_MODE, enc.getMode());
            assertEquals(Cipher.DECRYPT_MODE, dec.getMode());
            assertEquals("AES/CBC/PKCS5Padding", enc.getAlgorithm());
            assertEquals("AES/CBC/PKCS5Padding", dec.getAlgorithm());
        }
        catch (Exception e)
        {
            fail("Exception while testing AES cipher creation: " + e);
        }
    }
}
