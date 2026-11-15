package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_getSHA256Digest_Test {

    /**
     * Sample data used for hashing tests.
     */
    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);

    /**
     * Expected SHA‑256 hash of {@link #SAMPLE_DATA} expressed as a lower‑case hex string.
     */
    private static String expectedHex;

    @BeforeAll
    static void setupBeforeAll() throws NoSuchAlgorithmException {
        // Compute the expected hash once for the whole test suite.
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(SAMPLE_DATA);
        expectedHex = bytesToHex(md.digest());
    }

    /**
     * Utility method to convert a byte array to a lower‑case hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return a lower‑case hex representation
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            // Mask with 0xff to treat the byte as unsigned, then format as two hex digits.
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    @Test
    void getSHA256Digest_returnsMessageDigestWithSHA256Algorithm() {
        MessageDigest digest = EncryptionUtilities.getSHA256Digest();
        assertNotNull(digest, "Digest should not be null");
        assertEquals("SHA-256", digest.getAlgorithm(), "Digest algorithm should be SHA-256");
    }

    @Test
    void calculateSHA256Hash_returnsCorrectHexString() {
        // The method under test indirectly calls getSHA256Digest().
        String actualHex = EncryptionUtilities.calculateSHA256Hash(SAMPLE_DATA);
        assertEquals(expectedHex, actualHex, "calculateSHA256Hash should produce the correct SHA‑256 hex string");
    }

    @Test
    void digestEmptyByteArray_matchesKnownHash() {
        MessageDigest digest = EncryptionUtilities.getSHA256Digest();
        byte[] empty = new byte[0];
        String hex = bytesToHex(digest.digest(empty));
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hex, "SHA‑256 of empty input must match the known constant");
    }

    @Test
    void digestString_abc_matchesKnownHash() {
        MessageDigest digest = EncryptionUtilities.getSHA256Digest();
        byte[] abc = "abc".getBytes(StandardCharsets.UTF_8);
        String hex = bytesToHex(digest.digest(abc));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hex, "SHA‑256 of \"abc\" must match the standard hash");
    }

    @Test
    void getSHA256Digest_returnsDistinctInstances() {
        MessageDigest d1 = EncryptionUtilities.getSHA256Digest();
        MessageDigest d2 = EncryptionUtilities.getSHA256Digest();
        assertNotSame(d1, d2, "Two calls should return distinct MessageDigest objects");
        assertEquals("SHA-256", d1.getAlgorithm());
        assertEquals("SHA-256", d2.getAlgorithm());
    }

    @Test
    void messageDigestCanBeReusedAfterReset() {
        MessageDigest digest = EncryptionUtilities.getSHA256Digest();
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        // First hash
        digest.update(data);
        String first = bytesToHex(digest.digest());
        // Reset and hash again
        digest.reset();
        digest.update(data);
        String second = bytesToHex(digest.digest());
        assertEquals(first, second, "Digest after reset must produce the same hash for identical input");
    }

    @Test
    void getSHA256Digest_isFunctionallyEquivalentToJDKInstance() throws NoSuchAlgorithmException {
        MessageDigest fromUtil = EncryptionUtilities.getSHA256Digest();
        MessageDigest fromJDK = MessageDigest.getInstance("SHA-256");
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        String utilHex = bytesToHex(fromUtil.digest(data));
        String jdkHex = bytesToHex(fromJDK.digest(data));
        assertEquals(jdkHex, utilHex, "Hashes from the utility and direct JDK instance must match");
    }

    @Test
    void hashOneMillionAsProducesKnownHash() {
        // Build a 1,000,000‑character string of 'a'
        char[] chars = new char[1_000_000];
        java.util.Arrays.fill(chars, 'a');
        byte[] data = new String(chars).getBytes(StandardCharsets.UTF_8);
        MessageDigest digest = EncryptionUtilities.getSHA256Digest();
        String hex = bytesToHex(digest.digest(data));
        assertEquals("cdc76e5c9914fb9281a1c7e284d73e67f1809a48a497200e046d39ccc7112cd0", hex, "SHA‑256 of one million 'a' characters must match the known value");
    }

    @Test
    void concurrentDigestInstancesAllProduceCorrectHash() throws InterruptedException, ExecutionException {
        final int THREAD_COUNT = 10;
        final byte[] abcBytes = "abc".getBytes(StandardCharsets.UTF_8);
        final String expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            tasks.add(() -> {
                MessageDigest md = EncryptionUtilities.getSHA256Digest();
                md.update(abcBytes);
                return bytesToHex(md.digest());
            });
        }
        List<Future<String>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        for (Future<String> f : futures) {
            assertEquals(expected, f.get(), "Each concurrent digest must produce the correct SHA‑256 hash of \"abc\"");
        }
    }

    @Test
    void getSHA256Digest_isDistinctFromOtherDigestFactories() {
        MessageDigest sha256 = EncryptionUtilities.getSHA256Digest();
        MessageDigest md5 = EncryptionUtilities.getMD5Digest();
        MessageDigest sha1 = EncryptionUtilities.getSHA1Digest();
        MessageDigest sha512 = EncryptionUtilities.getSHA512Digest();
        assertEquals("SHA-256", sha256.getAlgorithm());
        assertNotEquals(sha256.getAlgorithm(), md5.getAlgorithm());
        assertNotEquals(sha256.getAlgorithm(), sha1.getAlgorithm());
        assertNotEquals(sha256.getAlgorithm(), sha512.getAlgorithm());
    }

    @Test
    void getSHA256Digest_providerIsNonNull() {
        MessageDigest digest = EncryptionUtilities.getSHA256Digest();
        Provider provider = digest.getProvider();
        assertNotNull(provider, "MessageDigest provider should not be null");
    }

    @Test
    void getSHA256Digest_throwsIllegalArgumentExceptionWhenAlgorithmUnavailable() {
        // Mock the static MessageDigest.getInstance method to throw NoSuchAlgorithmException
        try (MockedStatic<MessageDigest> mdMock = mockStatic(MessageDigest.class)) {
            mdMock.when(() -> MessageDigest.getInstance("SHA-256")).thenThrow(new NoSuchAlgorithmException("SHA-256 not available"));
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, EncryptionUtilities::getSHA256Digest, "Expected IllegalArgumentException when SHA‑256 algorithm is missing");
            assertTrue(ex.getCause() instanceof NoSuchAlgorithmException, "Cause of the exception should be NoSuchAlgorithmException");
        }
    }
}
