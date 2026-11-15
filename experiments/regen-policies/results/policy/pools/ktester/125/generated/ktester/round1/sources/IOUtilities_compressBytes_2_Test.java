package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.io.IOException;
import java.util.Random;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class IOUtilities_compressBytes_2_Test {

    /**
     * Sample text that compresses well (repeated pattern).
     */
    private static final String SAMPLE_TEXT = "ABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCD";

    /**
     * Fixed seed for reproducible random data.
     */
    private static final long RANDOM_SEED = 0xDEADBEEFL;

    /**
     * Generates a stream of arguments: each entry contains a description and the
     * byte array to be compressed. The test verifies that:
     * <ul>
     *   <li>The input array is not mutated.</li>
     *   <li>The compressed output starts with the GZIP header (0x1f,0x8b) for
     *       any non‑empty payload.</li>
     *   <li>Round‑tripping via {@link IOUtilities#uncompressBytes(byte[])} yields
     *       the original data.</li>
     * </ul>
     */
    private static Stream<Arguments> provideByteArraysForCompression() {
        return Stream.of(// empty array (generated via new byte[0])
        arguments("empty array new byte[0]", new byte[0]), // single zero byte
        arguments("single zero byte", new byte[] { 0 }), // single 127 byte (max positive signed value)
        arguments("single byte 127", new byte[] { 127 }), // array with negative values
        arguments("mixed negative/positive bytes", new byte[] { -1, -128, 0, 1, 2, 3, 4, 5 }), // ten zero bytes (low entropy)
        arguments("ten zero bytes", new byte[10]), // 256‑byte array covering full unsigned range 0‑255
        arguments("full range 0‑255", createFullByteRange()), // 1 024 random bytes
        arguments("1024 random bytes", randomBytes(1024)), // 4 096 random bytes (exactly internal buffer size)
        arguments("4096 random bytes", randomBytes(4096)), // 4 097 random bytes (just over buffer size)
        arguments("4097 random bytes", randomBytes(4097)), // 10 000 repetitions of 'a' (highly compressible)
        arguments("10_000 'a' characters", repeatStringAsBytes("a", 10_000)), // 2 048 repetitions of "ab" → 4 096 bytes (highly compressible, >4KB)
        arguments("2048 repetitions of \"ab\"", repeatStringAsBytes("ab", 2_048)), // original SAMPLE_TEXT (moderately compressible)
        arguments("sample repetitive text", SAMPLE_TEXT.getBytes()));
    }

    /**
     * Creates a byte array containing values 0,1,2,…,255 (as signed bytes).
     */
    private static byte[] createFullByteRange() {
        byte[] arr = new byte[256];
        for (int i = 0; i < 256; i++) {
            arr[i] = (byte) i;
        }
        return arr;
    }

    /**
     * Returns a deterministic random byte array of the requested length.
     */
    private static byte[] randomBytes(int length) {
        Random rng = new Random(RANDOM_SEED);
        byte[] data = new byte[length];
        rng.nextBytes(data);
        return data;
    }

    /**
     * Repeats the given string {@code count} times and returns its UTF‑8 bytes.
     */
    private static byte[] repeatStringAsBytes(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString().getBytes();
    }

    @Test
    void compressBytes_returnsCompressedData_andCanBeUncompressed() {
        // given
        byte[] original = SAMPLE_TEXT.getBytes();
        // when
        byte[] compressed = IOUtilities.compressBytes(original);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        // then
        assertNotNull(compressed, "Compressed result should not be null");
        assertTrue(compressed.length > 0, "Compressed result should contain data");
        // For a highly repetitive string the compressed form should be smaller.
        assertTrue(compressed.length < original.length, "Compressed data should be smaller than the original for compressible input");
        assertArrayEquals(original, uncompressed, "Uncompressing the compressed data should yield the original byte array");
    }

    @Test
    void compressBytes_emptyArray_returnsEmptyArray() {
        // given
        byte[] empty = new byte[0];
        // when
        byte[] compressed = IOUtilities.compressBytes(empty);
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        // then
        assertNotNull(compressed, "Result should not be null even for empty input");
        // The implementation may return a zero‑length array or a valid GZIP stream.
        // Both behaviours are acceptable as long as uncompressBytes works.
        assertTrue(compressed.length == 0 || (compressed.length >= 2 && compressed[0] == (byte) 0x1f && compressed[1] == (byte) 0x8b), "Compressed empty array should be either empty or a valid GZIP header");
        assertArrayEquals(empty, uncompressed, "Uncompressing an empty array should yield an empty array");
    }

    @Test
    void compressBytes_nullInput_throwsNullPointerException() {
        // given
        byte[] nullBytes = null;
        // when / then
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(nullBytes), "compressBytes(null) should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideByteArraysForCompression")
    void compressBytes_variousInputs_roundTripAndHeader(String description, byte[] input) {
        // Preserve a copy of the original to verify that the method does not modify it.
        byte[] originalCopy = input.clone();
        // Perform compression.
        byte[] compressed = IOUtilities.compressBytes(input);
        // Basic sanity checks.
        assertNotNull(compressed, description + ": compressed result must not be null");
        // For non‑empty inputs we expect a GZIP header.
        if (input.length > 0) {
            assertTrue(compressed.length >= 2, description + ": compressed data should contain at least the GZIP header");
            assertEquals((byte) 0x1f, compressed[0], description + ": first byte should be GZIP magic number 0x1f");
            assertEquals((byte) 0x8b, compressed[1], description + ": second byte should be GZIP magic number 0x8b");
        } else {
            // Empty input may yield an empty array or a valid GZIP header.
            assertTrue(compressed.length == 0 || (compressed.length >= 2 && compressed[0] == (byte) 0x1f && compressed[1] == (byte) 0x8b), description + ": empty input should result in empty array or GZIP header");
        }
        // Verify that the original input has not been altered.
        assertArrayEquals(originalCopy, input, description + ": compressBytes must not modify the original array");
        // Round‑trip through uncompressBytes.
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(originalCopy, uncompressed, description + ": uncompressed data must match the original");
    }
}
