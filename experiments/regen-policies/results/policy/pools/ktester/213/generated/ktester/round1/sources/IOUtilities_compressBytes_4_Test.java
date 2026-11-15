package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Arrays;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_compressBytes_4_Test {

    private static final byte[] SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog. The quick brown fox jumps over the lazy dog.".getBytes(StandardCharsets.UTF_8);

    private static Stream<Arguments> invalidOffsetLengthProvider() {
        byte[] hello = "Hello".getBytes(StandardCharsets.US_ASCII);
        return Stream.of(// negative offset
        Arguments.of(-1, 3, hello), // negative length
        Arguments.of(1, -2, hello), // offset+len exceeds array length
        Arguments.of(3, 5, hello), // offset beyond array length with len=0 (still illegal for DeflaterOutputStream.write)
        Arguments.of(5, 0, new byte[] { 1, 2, 3 }));
    }

    /**
     * Basic happy‑path test – compress and then uncompress should yield the original bytes.
     */
    @Test
    void compressBytes_happyPath_returnsCompressibleData() {
        // GIVEN
        byte[] source = SAMPLE_TEXT;
        int offset = 0;
        int len = source.length;
        // WHEN
        byte[] compressed = IOUtilities.compressBytes(source, offset, len);
        // THEN
        assertNotNull(compressed, "Compressed array must not be null");
        assertTrue(compressed.length > 0, "Compressed array must contain data");
        assertTrue(compressed.length < source.length, "Compressed data should be smaller than the source for repetitive text");
        // Verify that the data can be decompressed back to the original content.
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(source, decompressed, "Decompressed data must equal the original");
    }

    /**
     * Verify that a {@code null} source array triggers the expected exception.
     */
    @Test
    void compressBytes_nullSource_throwsRuntimeException() {
        // GIVEN
        byte[] source = null;
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(source, 0, 0));
        assertNotNull(ex.getCause(), "Cause should be present (from Convention.throwIfNull)");
    }

    /**
     * Offset/length that exceed the array bounds should result in a RuntimeException.
     */
    @Test
    void compressBytes_invalidOffsetOrLength_throwsRuntimeException() {
        // GIVEN
        byte[] source = SAMPLE_TEXT;
        // offset too large
        assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(source, source.length + 1, 1));
        // length too large
        assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(source, 0, source.length + 1));
        // negative offset
        assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(source, -1, 10));
        // negative length
        assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(source, 0, -5));
    }

    /**
     * Compress a sub‑range of the source array and verify that only that range is represented after decompression.
     */
    @Test
    void compressBytes_subRange_correctlyCompressedAndDecompressed() {
        // GIVEN
        byte[] source = "ABCDEFGHIJ".getBytes(StandardCharsets.US_ASCII);
        // start at 'C'
        int offset = 2;
        // "CDEFG"
        int len = 5;
        // WHEN
        byte[] compressed = IOUtilities.compressBytes(source, offset, len);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        // THEN
        byte[] expected = new byte[len];
        System.arraycopy(source, offset, expected, 0, len);
        assertArrayEquals(expected, decompressed, "Decompressed sub‑range must match the original sub‑range");
    }

    /**
     * A non‑compressible (random) byte array should still be processed without error, even if the result is larger.
     */
    @Test
    void compressBytes_randomData_processedEvenIfNotSmaller() {
        byte[] random = new byte[1024];
        for (int i = 0; i < random.length; i++) {
            random[i] = (byte) (Math.random() * 256);
        }
        byte[] compressed = IOUtilities.compressBytes(random, 0, random.length);
        assertNotNull(compressed, "Compressed result must not be null");
        // Decompress and verify loss‑less round‑trip
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(random, decompressed, "Decompressed random data must equal the original");
    }

    /* --------------------------------------------------------------------- *
     *  1. Null input array – should throw IllegalArgumentException with the
     *     exact message used by Convention.throwIfNull.
     *  --------------------------------------------------------------------- */
    @Test
    void compressBytes_nullInput_throwsIllegalArgumentException() {
        // GIVEN
        byte[] source = null;
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> IOUtilities.compressBytes(source, 0, 0));
        assertEquals("Byte array cannot be null", ex.getMessage(), "Exception message must match Convention.throwIfNull contract");
    }

    /* --------------------------------------------------------------------- *
     *  3. Successful compression – full array.
     *  --------------------------------------------------------------------- */
    @Test
    void compressBytes_fullArray_successfulCompressionAndDecompression() {
        byte[] source = "Hello World".getBytes(StandardCharsets.US_ASCII);
        int offset = 0;
        int len = source.length;
        byte[] compressed = IOUtilities.compressBytes(source, offset, len);
        assertNotNull(compressed, "Compressed result must not be null");
        assertTrue(compressed.length > 0, "Compressed result must contain data");
        // Decompress and verify equality
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(source, decompressed, "Decompressed data must equal the original");
    }

    /* --------------------------------------------------------------------- *
     *  4. Successful compression – partial slice.
     *  --------------------------------------------------------------------- */
    @Test
    void compressBytes_partialSlice_correctDecompression() {
        byte[] source = "abcdefghij".getBytes(StandardCharsets.US_ASCII);
        // 'c'
        int offset = 2;
        // "cdefg"
        int len = 5;
        byte[] compressed = IOUtilities.compressBytes(source, offset, len);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        byte[] expected = Arrays.copyOfRange(source, offset, offset + len);
        assertArrayEquals(expected, decompressed, "Decompressed slice must match the original sub‑range");
    }

    /* --------------------------------------------------------------------- *
     *  5. Zero‑length compression – should produce a valid (though possibly
     *     non‑empty) compressed array that decompresses to an empty array.
     *  --------------------------------------------------------------------- */
    @Test
    void compressBytes_zeroLength_returnsEmptyDecompressedArray() {
        byte[] source = { 1, 2, 3, 4, 5 };
        int offset = 2;
        // nothing to compress
        int len = 0;
        byte[] compressed = IOUtilities.compressBytes(source, offset, len);
        assertNotNull(compressed, "Compressed array must not be null");
        // Decompress and expect an empty array
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertEquals(0, decompressed.length, "Decompressed result should be empty for zero‑length input");
    }

    /* --------------------------------------------------------------------- *
     *  6. Boundary offset at array length with zero length – same expectation
     *     as the previous test.
     *  --------------------------------------------------------------------- */
    @Test
    void compressBytes_boundaryOffsetZeroLength_returnsEmptyDecompressedArray() {
        byte[] source = { 10, 20, 30, 40, 50 };
        // 5
        int offset = source.length;
        int len = 0;
        byte[] compressed = IOUtilities.compressBytes(source, offset, len);
        assertNotNull(compressed, "Compressed array must not be null");
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertEquals(0, decompressed.length, "Decompressed result should be empty when offset==length and len==0");
    }
}
