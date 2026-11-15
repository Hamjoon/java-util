package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_uncompressBytes_Test {

    /**
     * Sample data used for compression / decompression round‑trip tests.
     */
    private static final byte[] SAMPLE = "The quick brown fox jumps over the lazy dog".getBytes();

    /**
     * Helper to decode a Base64 string into a byte array.
     */
    private static byte[] b64(String s) {
        return Base64.getDecoder().decode(s);
    }

    /**
     * Parameter source for the *valid* compression/decompression scenarios.
     */
    private static Stream<Arguments> validCompressionProvider() {
        String gzBase64 = "H4sIAAAAAAAAA/NIzcnJVwjPL8pJAQAA//8DAAD//w==";
        byte[] compressed = b64(gzBase64);
        byte[] expected = "Hello, World!".getBytes(StandardCharsets.UTF_8);
        return Stream.of(// plain overload
        Arguments.of(compressed, expected, false), // overload with explicit offset/len (offset=0, len=bytes.length)
        Arguments.of(compressed, expected, true));
    }

    /**
     * Verify that a byte array that has been compressed with {@link IOUtilities#compressBytes(byte[])}
     * can be restored to its original form using {@link IOUtilities#uncompressBytes(byte[])}.
     */
    @Test
    void roundTripCompressUncompress() {
        // compress the sample data
        byte[] compressed = IOUtilities.compressBytes(SAMPLE);
        assertNotNull(compressed, "Compressed data must not be null");
        assertTrue(compressed.length > 0, "Compressed data should contain at least one byte");
        // now uncompress it
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertNotNull(uncompressed, "Uncompressed data must not be null");
        assertArrayEquals(SAMPLE, uncompressed, "Uncompressed bytes must match the original input");
    }

    /**
     * An empty byte array should round‑trip without error and remain empty.
     */
    @Test
    void uncompressEmptyArray() {
        byte[] empty = new byte[0];
        byte[] compressed = IOUtilities.compressBytes(empty);
        assertNotNull(compressed, "Compressed empty array must not be null");
        // Some implementations may return an empty array directly.
        // The contract of {@code uncompressBytes} should still return an empty array.
        byte[] result = IOUtilities.uncompressBytes(compressed);
        assertNotNull(result, "Result of uncompressing an empty array must not be null");
        assertEquals(0, result.length, "Resulting array should be empty");
    }

    /**
     * Passing {@code null} to {@code uncompressBytes} is expected to throw a
     * {@link NullPointerException}. The method does not perform a silent
     * conversion to an empty array.
     */
    @Test
    void uncompressNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> IOUtilities.uncompressBytes(null), "uncompressBytes(null) should throw NullPointerException");
    }

    /**
     * Verify that the overload {@code uncompressBytes(byte[], int, int)} works
     * correctly when called indirectly via the public {@code uncompressBytes(byte[])}.
     * This test uses a slice of a larger compressed buffer.
     */
    @Test
    void uncompressWithOffsetAndLength() {
        // Create a larger buffer that contains the compressed data surrounded by padding.
        byte[] compressed = IOUtilities.compressBytes(SAMPLE);
        byte[] padded = new byte[compressed.length + 10];
        // offset = 5
        System.arraycopy(compressed, 0, padded, 5, compressed.length);
        // Call the public method which internally delegates to the offset/len overload.
        byte[] result = IOUtilities.uncompressBytes(padded, 5, compressed.length);
        assertArrayEquals(SAMPLE, result, "Uncompressed slice must equal the original data");
    }

    @ParameterizedTest(name = "[{index}] valid compression – useOffsetLen={2}")
    @MethodSource("validCompressionProvider")
    void validCompressionAndDecompression(byte[] compressed, byte[] expected, boolean useOffsetLen) {
        byte[] result;
        if (useOffsetLen) {
            result = IOUtilities.uncompressBytes(compressed, 0, compressed.length);
        } else {
            result = IOUtilities.uncompressBytes(compressed);
        }
        assertArrayEquals(expected, result, "Decompressed bytes should match the original string");
    }

    @Test
    void nullInputThrowsNPE() {
        assertThrows(NullPointerException.class, () -> IOUtilities.uncompressBytes(null), "Passing null should throw NullPointerException");
    }

    @Test
    void emptyInputReturnsEmptyArray() {
        byte[] empty = new byte[0];
        byte[] result = IOUtilities.uncompressBytes(empty);
        assertNotNull(result);
        assertEquals(0, result.length, "Empty input must produce an empty output");
    }

    @Test
    void nonGzipDataIsReturnedUnchanged() {
        // "Just plain text" encoded as Base64
        byte[] plain = b64("SnVzdCBwbGFpbiB0ZXh0");
        byte[] result = IOUtilities.uncompressBytes(plain);
        assertArrayEquals(plain, result, "Non‑gzip data should be returned unchanged");
    }

    @Test
    void corruptedGzipDataFallsBackToOriginal() {
        // Corrupted gzip (last byte altered)
        byte[] corrupted = b64("H4sIAAAAAAAAA/NIzcnJVwjPL8pJAQAA//8DAAD//wA");
        byte[] result = IOUtilities.uncompressBytes(corrupted);
        // According to the specification, the method should return the original bytes
        assertArrayEquals(corrupted, result, "When decompression fails, the original byte array should be returned");
    }

    @Test
    void negativeOffsetThrowsIAE() {
        byte[] data = b64("H4sIAAAAAAAAA/NIzcnJVwjPL8pJAQAA//8DAAD//w==");
        assertThrows(IllegalArgumentException.class, () -> IOUtilities.uncompressBytes(data, -1, 10), "Negative offset must cause IllegalArgumentException");
    }

    @Test
    void negativeLengthThrowsIAE() {
        byte[] data = b64("H4sIAAAAAAAAA/NIzcnJVwjPL8pJAQAA//8DAAD//w==");
        assertThrows(IllegalArgumentException.class, () -> IOUtilities.uncompressBytes(data, 0, -5), "Negative length must cause IllegalArgumentException");
    }

    @Test
    void offsetPlusLengthExceedsArrayThrowsIAE() {
        byte[] data = b64("H4sIAAAAAAAAA/NIzcnJVwjPL8pJAQAA//8DAAD//w==");
        assertThrows(IllegalArgumentException.class, () -> IOUtilities.uncompressBytes(data, 5, 10), "offset+len beyond array bounds must cause IllegalArgumentException");
    }

    @Test
    void threeArgOverloadNullInputThrowsNPE() {
        assertThrows(NullPointerException.class, () -> IOUtilities.uncompressBytes(null, 0, 0), "Three‑arg overload with null array should throw NullPointerException");
    }

    @Test
    void offsetLengthIntegerOverflowThrowsIAE() {
        // Create a dummy array of length 10 – the actual content is irrelevant.
        byte[] dummy = new byte[10];
        // 2147483642
        int hugeOffset = Integer.MAX_VALUE - 5;
        // offset + len would overflow
        int len = 10;
        assertThrows(IllegalArgumentException.class, () -> IOUtilities.uncompressBytes(dummy, hugeOffset, len), "Integer overflow in offset+len must cause IllegalArgumentException");
    }
}
