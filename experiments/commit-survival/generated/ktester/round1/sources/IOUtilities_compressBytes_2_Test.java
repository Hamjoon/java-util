package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.Random;
import java.util.stream.Stream;

class IOUtilities_compressBytes_2_Test {

    /**
     * Sample data used in the tests.
     */
    private static final byte[] SAMPLE_DATA = "The quick brown fox jumps over the lazy dog".getBytes();

    private static Stream<Arguments> normalCompressionProvider() {
        return Stream.of(Arguments.of("Typical 10‑byte array", new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
    }

    /**
     * Verifies that {@link IOUtilities#compressBytes(byte[])} returns a non‑null,
     * non‑empty byte array that is smaller than the original (for compressible data)
     * and that the result can be restored to the original content via
     * {@link IOUtilities#uncompressBytes(byte[])}.
     */
    @Test
    void compressBytes_shouldCompressAndBeReversible() {
        // given
        byte[] original = SAMPLE_DATA;
        // when
        byte[] compressed = IOUtilities.compressBytes(original);
        // then
        assertNotNull(compressed, "Compressed array must not be null");
        assertTrue(compressed.length > 0, "Compressed array must contain data");
        // For reasonably compressible input the compressed size should be smaller.
        // The assertion is defensive – if the algorithm decides not to compress (e.g., for very small data)
        // the test would still be valid, so we only assert when the original length is > 0.
        if (original.length > 0) {
            assertTrue(compressed.length <= original.length, "Compressed data should not be larger than the original");
        }
        // Verify round‑trip integrity
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(original, uncompressed, "Uncompressed data must be identical to the original");
    }

    /**
     * Edge‑case test: compressing an empty byte array should return a non‑null,
     * possibly zero‑length array that can be uncompressed back to an empty array.
     */
    @Test
    void compressBytes_emptyArray() {
        // given
        byte[] empty = new byte[0];
        // when
        byte[] compressed = IOUtilities.compressBytes(empty);
        // then
        assertNotNull(compressed, "Compressed result for empty input must not be null");
        // The implementation may return an empty array or a small header; both are acceptable.
        assertTrue(compressed.length >= 0, "Compressed length must be non‑negative");
        // round‑trip
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(empty, uncompressed, "Uncompressed empty array must be empty");
    }

    /**
     * Edge‑case test: compressing a null reference should throw a {@link NullPointerException}
     * (the current implementation delegates to the overloaded method which expects a non‑null array).
     */
    @Test
    void compressBytes_nullArray_throwsException() {
        // given
        byte[] nullArray = null;
        // when / then
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(nullArray), "compressBytes(null) should throw NullPointerException");
    }

    /**
     * Normal compression tests – typical non‑empty arrays.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalCompressionProvider")
    void compressBytes_normalCompression(String description, byte[] input) {
        // when
        byte[] compressed = IOUtilities.compressBytes(input);
        // then
        assertNotNull(compressed, "Compressed result must not be null");
        assertTrue(compressed.length > 0, "Compressed result should contain data");
        assertTrue(compressed.length < input.length, "Compressed data should be smaller than the original for compressible input");
        // round‑trip verification
        byte[] roundTrip = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(input, roundTrip, "Decompressed data must match the original input");
    }

    /**
     * Single‑element array test.
     */
    @Test
    void compressBytes_singleElementArray() {
        byte[] input = new byte[] { 42 };
        byte[] compressed = IOUtilities.compressBytes(input);
        assertNotNull(compressed, "Compressed result must not be null");
        assertTrue(compressed.length > 0, "Compressed result should contain data");
        byte[] roundTrip = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(input, roundTrip, "Decompressed single‑element array must equal the original");
    }

    /**
     * Test handling of the full signed byte range (negative and positive values).
     */
    @Test
    void compressBytes_negativeByteValues() {
        byte[] input = new byte[] { -128, -1, 0, 1, 127 };
        byte[] compressed = IOUtilities.compressBytes(input);
        assertNotNull(compressed, "Compressed result must not be null");
        assertTrue(compressed.length > 0, "Compressed result should contain data");
        byte[] roundTrip = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(input, roundTrip, "Decompressed data must preserve negative byte values");
    }

    /**
     * Verify that compressing data that is already compressed still yields a
     * valid GZIP stream that can be decompressed back to the original zero‑filled payload.
     */
    @Test
    void compressBytes_alreadyCompressedData() {
        // 1 KB of zeros – highly compressible
        byte[] zeroFilled = new byte[1024];
        // First compression (baseline)
        byte[] firstCompressed = IOUtilities.compressBytes(zeroFilled);
        assertNotNull(firstCompressed, "First compression must not be null");
        // Second compression (compress already compressed data)
        byte[] secondCompressed = IOUtilities.compressBytes(firstCompressed);
        assertNotNull(secondCompressed, "Second compression must not be null");
        // Decompress twice to obtain the original zero‑filled array
        byte[] afterFirstDecompress = IOUtilities.uncompressBytes(secondCompressed);
        byte[] afterSecondDecompress = IOUtilities.uncompressBytes(afterFirstDecompress);
        assertArrayEquals(zeroFilled, afterSecondDecompress, "Double‑decompressed data must equal the original zero‑filled array");
    }

    /**
     * Large random data (64 KB) – ensures the algorithm works on larger, less‑compressible payloads.
     */
    @Test
    void compressBytes_largeRandomData() {
        Random r = new Random(12345L);
        byte[] randomData = new byte[64 * 1024];
        r.nextBytes(randomData);
        byte[] compressed = IOUtilities.compressBytes(randomData);
        assertNotNull(compressed, "Compressed result must not be null");
        assertTrue(compressed.length > 0, "Compressed result should contain data");
        // Even if compression does not reduce size, round‑trip must be correct
        byte[] roundTrip = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(randomData, roundTrip, "Decompressed large random data must match the original");
    }

    /**
     * Verify that the method returns a **new** array instance, never the original reference.
     */
    @Test
    void compressBytes_returnsNewArrayInstance() {
        byte[] input = new byte[] { 5, 4, 3, 2, 1 };
        byte[] compressed = IOUtilities.compressBytes(input);
        assertNotNull(compressed, "Compressed result must not be null");
        assertNotSame(input, compressed, "The returned array must be a different instance from the input");
    }

    /**
     * Null input test – separate from the earlier test to keep the parameterised
     * suite focused on valid data.
     */
    @Test
    void compressBytes_nullInput_throwsNPE() {
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(null), "compressBytes(null) should throw NullPointerException");
    }
}
