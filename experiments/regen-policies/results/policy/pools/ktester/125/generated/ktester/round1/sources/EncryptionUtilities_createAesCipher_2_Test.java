package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_createAesCipher_2_Test {

    /**
     * Holds a MessageDigest instance for MD5 that can be reused across tests.
     */
    private static MessageDigest md5Digest;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Initialise a single MD5 digest for the whole suite – cheaper than creating one per test.
        md5Digest = MessageDigest.getInstance("MD5");
    }

    @AfterAll
    static void teardownAfterAll() {
        // Clear the static reference – not strictly necessary but tidy.
        md5Digest = null;
    }

    /**
     * Provides test arguments for the parameterised test.
     *
     * @return a stream of {@link Arguments} containing:
     *         <ul>
     *           <li>key bytes (as a byte array)</li>
     *           <li>cipher mode (ENCRYPT_MODE or DECRYPT_MODE)</li>
     *         </ul>
     */
    private static Stream<Arguments> aesCipherProvider() {
        // 16‑byte (128‑bit) key
        byte[] key16 = new byte[16];
        // 32‑byte (256‑bit) key
        byte[] key32 = new byte[32];
        // Fill with deterministic values for reproducibility
        for (int i = 0; i < key16.length; i++) key16[i] = (byte) i;
        for (int i = 0; i < key32.length; i++) key32[i] = (byte) (i * 2);
        return Stream.of(Arguments.of(key16, Cipher.ENCRYPT_MODE, "16‑byte key, ENCRYPT_MODE"), Arguments.of(key32, Cipher.DECRYPT_MODE, "32‑byte key, DECRYPT_MODE"));
    }

    @Test
    void testCreateAesCipher_initialisesCipherCorrectly() throws Exception {
        // GIVEN – a deterministic key (16 bytes) and the ENCRYPT mode
        // 16‑byte key
        byte[] keyBytes = "testkey12345678".getBytes(StandardCharsets.UTF_8);
        Key key = new SecretKeySpec(keyBytes, "AES");
        int mode = Cipher.ENCRYPT_MODE;
        // WHEN – the cipher is created
        Cipher cipher = EncryptionUtilities.createAesCipher(key, mode);
        // THEN – basic properties are as expected
        assertNotNull(cipher, "Cipher should not be null");
        assertEquals("AES/CBC/PKCS5Padding", cipher.getAlgorithm(), "Cipher algorithm should be AES/CBC/PKCS5Padding");
        // Compute the expected IV (MD5 of the key bytes)
        md5Digest.reset();
        md5Digest.update(keyBytes);
        byte[] expectedIv = md5Digest.digest();
        assertArrayEquals(expectedIv, cipher.getIV(), "IV generated from the key should match the MD5 digest of the key");
        // AND – encryption followed by decryption yields the original plaintext
        byte[] plainText = "HelloWorld".getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = cipher.doFinal(plainText);
        // Create a decryption cipher using the same key
        Cipher decryptCipher = EncryptionUtilities.createAesCipher(key, Cipher.DECRYPT_MODE);
        byte[] decrypted = decryptCipher.doFinal(encrypted);
        assertArrayEquals(plainText, decrypted, "Decrypted bytes must match the original plaintext");
    }

    /**
     * Parameterised test that validates cipher creation for both supported key sizes
     * and modes.
     *
     * @param keyBytes   raw key material
     * @param mode       Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE
     * @param description textual description used in assertion messages
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("aesCipherProvider")
    void testCreateAesCipher_variousKeysAndModes(byte[] keyBytes, int mode, String description) throws Exception {
        // GIVEN – a SecretKeySpec built from the supplied key bytes
        Key key = new SecretKeySpec(keyBytes, "AES");
        // WHEN – the cipher is created
        Cipher cipher = EncryptionUtilities.createAesCipher(key, mode);
        // THEN – basic structural checks
        assertNotNull(cipher, description + " – cipher should not be null");
        assertEquals("AES/CBC/PKCS5Padding", cipher.getAlgorithm(), description + " – unexpected algorithm");
        // Compute the expected IV (MD5 digest of the key bytes)
        md5Digest.reset();
        md5Digest.update(keyBytes);
        byte[] expectedIv = md5Digest.digest();
        assertArrayEquals(expectedIv, cipher.getIV(), description + " – IV should be MD5(key)");
        // Additional functional verification:
        //   * For ENCRYPT_MODE we encrypt a sample payload and then decrypt it with a
        //     freshly created DECRYPT_MODE cipher.
        //   * For DECRYPT_MODE we perform the inverse (decrypt payload encrypted earlier).
        byte[] payload = "JUnit5‑Test‑Payload".getBytes(StandardCharsets.UTF_8);
        if (mode == Cipher.ENCRYPT_MODE) {
            // Encrypt
            byte[] encrypted = cipher.doFinal(payload);
            // Decrypt with a new cipher in DECRYPT_MODE
            Cipher decryptCipher = EncryptionUtilities.createAesCipher(key, Cipher.DECRYPT_MODE);
            byte[] decrypted = decryptCipher.doFinal(encrypted);
            assertArrayEquals(payload, decrypted, description + " – round‑trip encryption/decryption failed");
        } else {
            // DECRYPT_MODE
            // First create an encryption cipher to produce ciphertext
            Cipher encryptCipher = EncryptionUtilities.createAesCipher(key, Cipher.ENCRYPT_MODE);
            byte[] encrypted = encryptCipher.doFinal(payload);
            // Now decrypt with the cipher under test (DECRYPT_MODE)
            byte[] decrypted = cipher.doFinal(encrypted);
            assertArrayEquals(payload, decrypted, description + " – decryption of previously encrypted data failed");
        }
    }
}
