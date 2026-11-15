package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.security.MessageDigest;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EncryptionUtilities_calculateHash_Test {

    @Mock
    MessageDigest // mock for the MessageDigest dependency
    mockDigest;

    // mock for the static ByteUtilities.encode method
    MockedStatic<ByteUtilities> mockedByteUtil;

    private static final class EmptyArrayTestData {

        final String algorithm;

        final String expectedHex;

        EmptyArrayTestData(String algorithm, String expectedHex) {
            this.algorithm = algorithm;
            this.expectedHex = expectedHex;
        }

        @Override
        public String toString() {
            return algorithm;
        }
    }

    private static final class KnownDataTestData {

        final String algorithm;

        final String expectedHex;

        KnownDataTestData(String algorithm, String expectedHex) {
            this.algorithm = algorithm;
            this.expectedHex = expectedHex;
        }

        @Override
        public String toString() {
            return algorithm;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise the static mock for ByteUtilities before each test
        mockedByteUtil = Mockito.mockStatic(ByteUtilities.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock to avoid leaking it into other tests
        if (mockedByteUtil != null) {
            mockedByteUtil.close();
        }
    }

    private static Stream<EmptyArrayTestData> emptyArrayProvider() {
        return Stream.of(new EmptyArrayTestData("MD5", "d41d8cd98f00b204e9800998ecf8427e"), new EmptyArrayTestData("SHA-1", "da39a3ee5e6b4b0d3255bfef95601890afd80709"), new EmptyArrayTestData("SHA-256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"), new EmptyArrayTestData("SHA-512", "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"));
    }

    private static Stream<KnownDataTestData> knownDataProvider() {
        return Stream.of(new KnownDataTestData("MD5", "900150983cd24fb0d6963f7d28e17f72"), new KnownDataTestData("SHA-1", "a9993e364706816aba3e25717850c26c9cd0d89d"), new KnownDataTestData("SHA-256", "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"), new KnownDataTestData("SHA-512", "ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f"));
    }

    @Test
    void calculateHash_NullBytes_ReturnsNull() {
        // GIVEN
        // (mockDigest is already a Mockito mock; no further stubbing required)
        // WHEN
        String result = EncryptionUtilities.calculateHash(mockDigest, null);
        // THEN
        assertNull(result, "When the byte array is null the method should return null");
        // The digest must not be used at all
        verifyNoInteractions(mockDigest);
        // ByteUtilities.encode must never be called
        mockedByteUtil.verifyNoInteractions();
    }

    @Test
    void calculateHash_ValidBytes_UpdatesDigestAndEncodesResult() {
        // GIVEN
        // arbitrary test data
        byte[] input = "sample".getBytes();
        // fake digest output
        byte[] digestBytes = new byte[] { 0x01, 0x02, 0x03 };
        String expectedEncoded = "FAKE_ENCODED";
        // Stub the digest behavior
        when(mockDigest.digest()).thenReturn(digestBytes);
        // Stub the static encode method
        mockedByteUtil.when(() -> ByteUtilities.encode(digestBytes)).thenReturn(expectedEncoded);
        // WHEN
        String actual = EncryptionUtilities.calculateHash(mockDigest, input);
        // THEN
        assertEquals(expectedEncoded, actual, "The method should return the value produced by ByteUtilities.encode");
        // Verify that the digest received the exact byte array
        verify(mockDigest, times(1)).update(eq(input));
        // Verify that digest() was called exactly once
        verify(mockDigest, times(1)).digest();
        // Verify that ByteUtilities.encode was called with the digest result
        mockedByteUtil.verify(() -> ByteUtilities.encode(digestBytes), times(1));
    }

    @Test
    void calculateHash_NullDigest_ThrowsNullPointerException() {
        // GIVEN
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EncryptionUtilities.calculateHash(null, data), "Passing a null MessageDigest should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] algorithm={0}")
    @MethodSource("emptyArrayProvider")
    void calculateHash_EmptyArray_ReturnsWellKnownHash(EmptyArrayTestData data) throws Exception {
        // GIVEN – real MessageDigest instance for the algorithm
        MessageDigest digest = MessageDigest.getInstance(data.algorithm);
        // Configure the static mock to call the real encode implementation
        mockedByteUtil.when(() -> ByteUtilities.encode(any())).thenCallRealMethod();
        // WHEN
        String result = EncryptionUtilities.calculateHash(digest, new byte[0]);
        // THEN
        assertEquals(data.expectedHex, result, () -> "Empty array hash for " + data.algorithm + " should match known value");
        // Verify that the digest was updated with an empty array (exactly one call)
        // Since we cannot directly verify the argument of update (it could be the same empty array),
        // we assert that digest length is zero after update (which is true for empty input).
        assertEquals(0, digest.getDigestLength() > 0 ? digest.digest().length : 0, "Digest should have processed an empty input");
    }

    @ParameterizedTest(name = "[{index}] algorithm={0}")
    @MethodSource("knownDataProvider")
    void calculateHash_KnownData_ProducesCorrectHash(KnownDataTestData data) throws Exception {
        // GIVEN
        MessageDigest digest = MessageDigest.getInstance(data.algorithm);
        byte[] input = "abc".getBytes(StandardCharsets.UTF_8);
        mockedByteUtil.when(() -> ByteUtilities.encode(any())).thenCallRealMethod();
        // WHEN
        String result = EncryptionUtilities.calculateHash(digest, input);
        // THEN
        assertEquals(data.expectedHex, result, () -> data.algorithm + " hash of \"abc\" should match known value");
    }

    @Test
    void calculateHash_PrepopulatedDigest_AppendsBytesCorrectly() throws Exception {
        // GIVEN – create a real MD5 digest and pre‑seed it with "abc"
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update("abc".getBytes(StandardCharsets.UTF_8));
        // The bytes we will hash now are "def"
        byte[] additional = "def".getBytes(StandardCharsets.UTF_8);
        mockedByteUtil.when(() -> ByteUtilities.encode(any())).thenCallRealMethod();
        // Expected hash is MD5("abcdef")
        MessageDigest verifier = MessageDigest.getInstance("MD5");
        verifier.update("abcdef".getBytes(StandardCharsets.UTF_8));
        String expected = ByteUtilities.encode(verifier.digest());
        // WHEN
        String result = EncryptionUtilities.calculateHash(md5, additional);
        // THEN
        assertEquals(expected, result, "Digest pre‑populated with \"abc\" then updated with \"def\" must equal MD5(\"abcdef\")");
    }
}
