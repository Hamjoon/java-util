package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateHash_Test {

    @Mock
    MessageDigest // mock for the MessageDigest dependency
    mockDigest;

    // mock for the static ByteUtilities.encode method
    MockedStatic<ByteUtilities> byteUtilitiesMock;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the static mock before each test so that it can be verified/cleared independently
        byteUtilitiesMock = mockStatic(ByteUtilities.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Close the static mock to avoid leaking it into other tests
        if (byteUtilitiesMock != null) {
            byteUtilitiesMock.close();
        }
    }

    @Test
    void calculateHash_returnsNullWhenBytesAreNull() {
        // GIVEN
        byte[] input = null;
        // WHEN
        String result = EncryptionUtilities.calculateHash(mockDigest, input);
        // THEN
        assertNull(result, "Result should be null when the input byte array is null");
        // No interaction with the MessageDigest or ByteUtilities should have occurred
        verifyNoInteractions(mockDigest);
        // The static mock should not have been called
        byteUtilitiesMock.verifyNoInteractions();
    }

    @Test
    void calculateHash_processesBytesAndEncodesDigest() {
        // GIVEN
        byte[] input = "test-data".getBytes();
        byte[] digestBytes = new byte[] { 0x01, 0x02, 0x03 };
        String encoded = "encodedDigest";
        // Mock the behavior of MessageDigest
        when(mockDigest.digest()).thenReturn(digestBytes);
        // Mock the static encode method
        byteUtilitiesMock.when(() -> ByteUtilities.encode(digestBytes)).thenReturn(encoded);
        // WHEN
        String result = EncryptionUtilities.calculateHash(mockDigest, input);
        // THEN
        assertEquals(encoded, result, "The returned string should be the encoded digest");
        // Verify that the digest was updated with the exact byte array we supplied
        verify(mockDigest, times(1)).update(input);
        // Verify that digest() was called once
        verify(mockDigest, times(1)).digest();
        // Verify that the static encode method was called with the digest bytes
        byteUtilitiesMock.verify(() -> ByteUtilities.encode(digestBytes), times(1));
    }

    @Test
    void calculateHash_multipleCallsUseDigestIndependently() {
        // GIVEN
        byte[] firstInput = "first".getBytes();
        byte[] secondInput = "second".getBytes();
        byte[] firstDigest = new byte[] { 0x0A };
        byte[] secondDigest = new byte[] { 0x0B };
        when(mockDigest.digest()).thenReturn(firstDigest, secondDigest);
        byteUtilitiesMock.when(() -> ByteUtilities.encode(firstDigest)).thenReturn("firstEnc");
        byteUtilitiesMock.when(() -> ByteUtilities.encode(secondDigest)).thenReturn("secondEnc");
        // WHEN
        String firstResult = EncryptionUtilities.calculateHash(mockDigest, firstInput);
        String secondResult = EncryptionUtilities.calculateHash(mockDigest, secondInput);
        // THEN
        assertEquals("firstEnc", firstResult);
        assertEquals("secondEnc", secondResult);
        // Verify interactions for both calls
        verify(mockDigest, times(1)).update(firstInput);
        verify(mockDigest, times(1)).update(secondInput);
        verify(mockDigest, times(2)).digest();
        byteUtilitiesMock.verify(() -> ByteUtilities.encode(firstDigest), times(1));
        byteUtilitiesMock.verify(() -> ByteUtilities.encode(secondDigest), times(1));
    }

    /**
     * Verify that hashing an empty byte array with MD5 returns the well‑known
     * empty‑MD5 digest.
     */
    @Test
    void calculateHash_emptyBytes() throws Exception {
        // GIVEN
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] empty = new byte[0];
        // Let the static mock delegate to the real implementation
        byteUtilitiesMock.when(() -> ByteUtilities.encode(any())).thenCallRealMethod();
        // WHEN
        String result = EncryptionUtilities.calculateHash(md5, empty);
        // THEN
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result);
        // The static mock should have been called exactly once with the digest bytes
        byteUtilitiesMock.verify(() -> ByteUtilities.encode(md5.digest()), times(1));
    }

    /**
     * MD5 hash of the string "abc" – known reference value.
     */
    @Test
    void calculateHash_md5KnownValue() throws Exception {
        // GIVEN
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] input = "abc".getBytes(StandardCharsets.UTF_8);
        byteUtilitiesMock.when(() -> ByteUtilities.encode(any())).thenCallRealMethod();
        // WHEN
        String result = EncryptionUtilities.calculateHash(md5, input);
        // THEN
        assertEquals("900150983cd24fb0d6963f7d28e17f72", result);
    }

    /**
     * SHA‑1 hash of the string "abc" – known reference value.
     */
    @Test
    void calculateHash_sha1KnownValue() throws Exception {
        // GIVEN
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] input = "abc".getBytes(StandardCharsets.UTF_8);
        byteUtilitiesMock.when(() -> ByteUtilities.encode(any())).thenCallRealMethod();
        // WHEN
        String result = EncryptionUtilities.calculateHash(sha1, input);
        // THEN
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", result);
    }

    /**
     * SHA‑256 hash of the string "abc" – known reference value.
     */
    @Test
    void calculateHash_sha256KnownValue() throws Exception {
        // GIVEN
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] input = "abc".getBytes(StandardCharsets.UTF_8);
        byteUtilitiesMock.when(() -> ByteUtilities.encode(any())).thenCallRealMethod();
        // WHEN
        String result = EncryptionUtilities.calculateHash(sha256, input);
        // THEN
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", result);
    }

    /**
     * SHA‑512 hash of the string "abc" – known reference value.
     */
    @Test
    void calculateHash_sha512KnownValue() throws Exception {
        // GIVEN
        MessageDigest sha512 = MessageDigest.getInstance("SHA-512");
        byte[] input = "abc".getBytes(StandardCharsets.UTF_8);
        byteUtilitiesMock.when(() -> ByteUtilities.encode(any())).thenCallRealMethod();
        // WHEN
        String result = EncryptionUtilities.calculateHash(sha512, input);
        // THEN
        assertEquals("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" + "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f", result);
    }

    /**
     * Re‑using the same {@link MessageDigest} instance for two successive calls.
     * {@link MessageDigest#digest()} resets the internal state, therefore the
     * second call must hash only the second input.
     */
    @Test
    void calculateHash_digestReuse() throws Exception {
        // GIVEN
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] firstInput = "abc".getBytes(StandardCharsets.UTF_8);
        byte[] secondInput = "def".getBytes(StandardCharsets.UTF_8);
        byteUtilitiesMock.when(() -> ByteUtilities.encode(any())).thenCallRealMethod();
        // WHEN
        String firstResult = EncryptionUtilities.calculateHash(md5, firstInput);
        String secondResult = EncryptionUtilities.calculateHash(md5, secondInput);
        // THEN
        assertEquals("900150983cd24fb0d6963f7d28e17f72", firstResult, "First call should produce MD5 of \"abc\"");
        assertEquals("4ed9407630eb1000c0f6b63842defa7d", secondResult, "Second call should produce MD5 of \"def\" only");
    }

    /**
     * Passing a {@code null} {@link MessageDigest} must result in a
     * {@link NullPointerException} because the method attempts to invoke
     * {@code d.update(...)}.
     */
    @Test
    void calculateHash_nullDigest() {
        // GIVEN
        MessageDigest nullDigest = null;
        byte[] input = "any".getBytes(StandardCharsets.UTF_8);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateHash(nullDigest, input), "A null MessageDigest should cause NullPointerException");
        // No interaction with the static mock should have occurred
        byteUtilitiesMock.verifyNoInteractions();
    }
}
