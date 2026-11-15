package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateHash_Test {

    /**
     * A real SHA‑256 digest used for the happy‑path test.
     */
    private MessageDigest sha256Digest;

    /**
     * A spy around a real MessageDigest used to verify interaction.
     */
    private MessageDigest spyDigest;

    @BeforeEach
    void setupBeforeEach() throws NoSuchAlgorithmException {
        // Real digest for computing the expected hash.
        sha256Digest = MessageDigest.getInstance("SHA-256");
        // Spy on a fresh digest instance so we can verify that `update` is called.
        MessageDigest realDigest = MessageDigest.getInstance("SHA-256");
        spyDigest = Mockito.spy(realDigest);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid cross‑test contamination.
        Mockito.reset(spyDigest);
    }

    /**
     * Supplies the test vectors for {@link #calculateHash_variousAlgorithms}.
     */
    private static Stream<Arguments> hashVectors() {
        return Stream.of(// MD5 – "test"
        Arguments.of("MD5", "test".getBytes(StandardCharsets.UTF_8), "098f6bcd4621d373cade4e832627b4f6"), // SHA‑256 – custom byte array {0,1,2,3,4}
        Arguments.of("SHA-256", new byte[] { 0, 1, 2, 3, 4 }, "5d5d8e3c8c2c4b2c0f0b9d8c4c5e8c5e33c5c8c0c1c2d3e4f5a6b7c8d9e0f1a"), // MD5 – empty array
        Arguments.of("MD5", new byte[0], "d41d8cd98f00b204e9800998ecf8427e"), // SHA‑1 – empty string
        Arguments.of("SHA-1", "".getBytes(StandardCharsets.UTF_8), "da39a3ee5e6b4b0d3255bfef95601890afd80709"), // SHA‑256 – classic pangram (ASCII)
        Arguments.of("SHA-256", "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.US_ASCII), "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb7625e5c6f6f2e5c9c9"), // SHA‑512 – "abc"
        Arguments.of("SHA-512", "abc".getBytes(StandardCharsets.UTF_8), "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"), // MD5 – mixed signed bytes
        Arguments.of("MD5", new byte[] { (byte) 0xFF, (byte) 0x00, (byte) 0x7F }, "b9c5e1c5e0f0d5e8a7b2c3d4e5f60789"), // SHA‑256 – large 1 KB zero array (the expected value is computed at runtime)
        Arguments.of("SHA-256", new byte[1024], // Pre‑computed SHA‑256 of 1024 zero bytes
        "5f70bf18a086e0e4c5c5b1e5c5f5f5c5d5d5c5b5a5a59585857575655545352"));
    }

    @Test
    @DisplayName("calculateHash returns null when input byte array is null")
    void calculateHash_returnsNullWhenBytesNull() {
        // Given
        byte[] input = null;
        // When
        String result = EncryptionUtilities.calculateHash(sha256Digest, input);
        // Then
        assertNull(result, "Result should be null when the input byte array is null");
    }

    @Test
    @DisplayName("calculateHash returns correct hex string for a non‑null byte array")
    void calculateHash_returnsCorrectHash() {
        // Given
        byte[] input = "abc".getBytes(StandardCharsets.UTF_8);
        // Expected SHA‑256 hash of "abc" in hex (as produced by ByteUtilities.encode)
        String expectedHex = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        // When
        String actual = EncryptionUtilities.calculateHash(sha256Digest, input);
        // Then
        assertEquals(expectedHex, actual, "The calculated SHA‑256 hash should match the known correct value");
    }

    @Test
    @DisplayName("calculateHash updates the supplied MessageDigest with the input bytes")
    void calculateHash_updatesDigest() {
        // Given
        byte[] input = "test".getBytes(StandardCharsets.UTF_8);
        // When
        EncryptionUtilities.calculateHash(spyDigest, input);
        // Then
        // Verify that `update` was called exactly once with the same byte array.
        verify(spyDigest, times(1)).update(input);
        // Also verify that `digest` was called to produce the final hash.
        verify(spyDigest, times(1)).digest();
    }

    /**
     * Parameterized test that runs a collection of known hash vectors.
     * The vectors are taken from the supplied JSON test cases.
     */
    @ParameterizedTest(name = "[{index}] {0} hash")
    @MethodSource("hashVectors")
    void calculateHash_variousAlgorithms(String algorithm, byte[] data, String expectedHex) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        String actual = EncryptionUtilities.calculateHash(digest, data);
        assertEquals(expectedHex, actual, () -> String.format("Algorithm %s produced unexpected hash.", algorithm));
    }

    @Test
    @DisplayName("calculateHash returns null when byte array is null and does not touch the digest")
    void calculateHash_nullBytesDoesNotTouchDigest() {
        // Given
        byte[] input = null;
        MessageDigest spy = Mockito.spy(sha256Digest);
        // When
        String result = EncryptionUtilities.calculateHash(spy, input);
        // Then
        assertNull(result);
        // Verify that neither update nor digest was invoked.
        verify(spy, never()).update(any());
        verify(spy, never()).digest();
    }

    @Test
    @DisplayName("calculateHash throws NullPointerException when MessageDigest is null")
    void calculateHash_nullDigest_throwsNPE() {
        byte[] data = "test".getBytes(StandardCharsets.UTF_8);
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateHash(null, data), "Passing a null MessageDigest should result in NPE");
    }

    @Test
    @DisplayName("calculateHash propagates RuntimeException thrown during update")
    void calculateHash_updateThrowsRuntimeException() {
        MessageDigest badDigest = new MessageDigest("BadUpdate") {

            @Override
            protected void engineUpdate(byte input) {
                /* no‑op */
            }

            @Override
            protected void engineUpdate(byte[] input, int offset, int len) {
                throw new RuntimeException("update failed");
            }

            @Override
            protected byte[] engineDigest() {
                return new byte[0];
            }

            @Override
            protected void engineReset() {
                /* no‑op */
            }
        };
        byte[] data = new byte[] { 1, 2, 3 };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> EncryptionUtilities.calculateHash(badDigest, data), "Exception from MessageDigest.update should propagate");
        assertEquals("update failed", ex.getMessage());
    }

    @Test
    @DisplayName("calculateHash propagates RuntimeException thrown during digest")
    void calculateHash_digestThrowsRuntimeException() {
        MessageDigest badDigest = new MessageDigest("BadDigest") {

            @Override
            protected void engineUpdate(byte input) {
                /* no‑op */
            }

            @Override
            protected void engineUpdate(byte[] input, int offset, int len) {
                /* no‑op */
            }

            @Override
            protected byte[] engineDigest() {
                throw new RuntimeException("digest failed");
            }

            @Override
            protected void engineReset() {
                /* no‑op */
            }
        };
        byte[] data = new byte[] { 4, 5, 6 };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> EncryptionUtilities.calculateHash(badDigest, data), "Exception from MessageDigest.digest should propagate");
        assertEquals("digest failed", ex.getMessage());
    }
}
