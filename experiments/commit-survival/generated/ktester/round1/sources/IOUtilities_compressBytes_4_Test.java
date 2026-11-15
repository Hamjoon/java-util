package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.Random;
import java.util.stream.Stream;

class IOUtilities_compressBytes_4_Test {

    private byte[] fullData;

    private byte[] partialData;

    /**
     * Simple holder for normal‑scenario test data.
     */
    private static final class NormalCase {

        final byte[] input;

        final int offset;

        final int len;

        final byte[] expected;

        final String description;

        NormalCase(byte[] input, int offset, int len, byte[] expected, String description) {
            this.input = input;
            this.offset = offset;
            this.len = len;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Simple holder for edge‑case test data.
     */
    private static final class EdgeCase {

        final byte[] input;

        final int offset;

        final int len;

        final byte[] expected;

        final String description;

        EdgeCase(byte[] input, int offset, int len, byte[] expected, String description) {
            this.input = input;
            this.offset = offset;
            this.len = len;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Simple holder for invalid‑argument test data.
     */
    private static final class InvalidCase {

        final byte[] bytes;

        final int offset;

        final int len;

        final Class<? extends Throwable> cause;

        final String description;

        InvalidCase(byte[] bytes, int offset, int len, Class<? extends Throwable> cause, String description) {
            this.bytes = bytes;
            this.offset = offset;
            this.len = len;
            this.cause = cause;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Sample data used by the tests
        fullData = "The quick brown fox jumps over the lazy dog".getBytes();
        // A longer array to test offset/len handling
        partialData = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes();
    }

    /* -------------------------------------------------------------
     *  Normal compression scenarios (parameterized)
     * ------------------------------------------------------------- */
    private static Stream<NormalCase> normalCases() {
        return Stream.of(// whole array
        new NormalCase(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, 0, 10, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, "Compress the whole byte array"), // sub‑range
        new NormalCase(new byte[] { 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 }, 2, 5, new byte[] { 30, 40, 50, 60, 70 }, "Compress a sub‑range of the array"), // empty slice (len = 0)
        new NormalCase(new byte[] { 0, 0, 0, 0 }, 0, 0, new byte[0], "Compress an empty slice"));
    }

    /* -------------------------------------------------------------
     *  Edge offset/length combinations (parameterized)
     * ------------------------------------------------------------- */
    private static Stream<EdgeCase> edgeCases() {
        return Stream.of(new EdgeCase(new byte[] { 42 }, 0, 1, new byte[] { 42 }, "Single‑byte array"), new EdgeCase(new byte[] { 5, 6, 7, 8, 9 }, 4, 0, new byte[0], "Offset at last element with zero length"), new EdgeCase(new byte[] { 9, 8, 7, 6, 5, 4, 3, 2, 1 }, 8, 1, new byte[] { 1 }, "Last element only"), new EdgeCase(new byte[] { 10, 20, 30, 40, 50 }, 0, 5, new byte[] { 10, 20, 30, 40, 50 }, "Full array with different data"));
    }

    /* -------------------------------------------------------------
     *  Invalid argument handling – expect RuntimeException wrapping
     * ------------------------------------------------------------- */
    private static Stream<InvalidCase> invalidCases() {
        return Stream.of(new InvalidCase(null, 0, 5, NullPointerException.class, "Null byte array should cause NPE wrapped in RuntimeException"), new InvalidCase(new byte[] { 1, 2, 3 }, -1, 2, IndexOutOfBoundsException.class, "Negative offset"), new InvalidCase(new byte[] { 1, 2, 3 }, 2, -1, IndexOutOfBoundsException.class, "Negative length"), new InvalidCase(new byte[] { 1, 2, 3 }, 3, 1, IndexOutOfBoundsException.class, "Offset equal to length with positive len"), new InvalidCase(new byte[] { 1, 2, 3 }, 1, 5, IndexOutOfBoundsException.class, "Length runs past end of array"), new InvalidCase(null, 0, 0, NullPointerException.class, "Null array with zero length"), new InvalidCase(new byte[] {}, 1, 0, IndexOutOfBoundsException.class, "Offset beyond empty array"), new InvalidCase(new byte[] { 1, 2, 3 }, 2, 2, IndexOutOfBoundsException.class, "Offset+len exceeds array length"), new InvalidCase(new byte[] { 1, 2, 3 }, -5, 1, IndexOutOfBoundsException.class, "Large negative offset"), new InvalidCase(new byte[] { 1, 2, 3 }, 0, -10, IndexOutOfBoundsException.class, "Large negative length"));
    }

    /* -------------------------------------------------------------
     *  Helper methods and data classes
     * ------------------------------------------------------------- */
    private static byte[] randomBytes(int size) {
        byte[] arr = new byte[size];
        // deterministic seed for reproducibility
        new Random(0xDEADBEEF).nextBytes(arr);
        return arr;
    }

    private static byte[] repeatingBytes(int size, byte value) {
        byte[] arr = new byte[size];
        for (int i = 0; i < size; i++) {
            arr[i] = value;
        }
        return arr;
    }

    @Test
    @DisplayName("compressBytes should correctly compress the whole array")
    void testCompressBytesFullArray() {
        // given
        byte[] input = fullData;
        // when
        byte[] compressed = IOUtilities.compressBytes(input, 0, input.length);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        // then
        assertArrayEquals(input, decompressed, "Decompressed bytes should be identical to the original full array");
    }

    @Test
    @DisplayName("compressBytes should correctly compress a sub‑range of the array")
    void testCompressBytesPartialRange() {
        // given
        // start at 'C'
        int offset = 2;
        // "CDEFG"
        int len = 5;
        byte[] expected = new byte[len];
        System.arraycopy(partialData, offset, expected, 0, len);
        // when
        byte[] compressed = IOUtilities.compressBytes(partialData, offset, len);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        // then
        assertArrayEquals(expected, decompressed, "Decompressed bytes should match the specified sub‑range of the original array");
    }

    @Test
    @DisplayName("compressBytes should throw an exception for an invalid offset/length")
    void testCompressBytesInvalidOffsetLength() {
        // given
        byte[] input = fullData;
        // clearly out of bounds
        int offset = input.length + 1;
        int len = 1;
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtilities.compressBytes(input, offset, len), "compressBytes should throw IndexOutOfBoundsException when offset/len are invalid");
    }

    @Test
    @DisplayName("compressBytes should throw NullPointerException when the input array is null")
    void testCompressBytesNullInput() {
        // given
        byte[] input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(input, 0, 0), "compressBytes should throw NullPointerException for a null byte array");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalCases")
    @DisplayName("compressBytes normal scenarios")
    void testCompressBytesNormal(NormalCase c) {
        // when
        byte[] compressed = IOUtilities.compressBytes(c.input, c.offset, c.len);
        assertNotNull(compressed, "Compressed result must not be null");
        assertTrue(compressed.length > 0, "Compressed result should have positive length");
        // decompress and verify
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(c.expected, decompressed, "Decompressed bytes must match expected data for case: " + c.description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeCases")
    @DisplayName("compressBytes edge offset/length combinations")
    void testCompressBytesEdge(EdgeCase c) {
        byte[] compressed = IOUtilities.compressBytes(c.input, c.offset, c.len);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0);
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(c.expected, decompressed, "Edge case failed: " + c.description);
    }

    /* -------------------------------------------------------------
     *  Large data compression
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("compressBytes large random payload (1 KB)")
    void testCompressBytesLargeRandom() {
        byte[] random = randomBytes(1024);
        byte[] compressed = IOUtilities.compressBytes(random, 0, random.length);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0);
        // On average compression should shrink the data, but we tolerate the rare case where it does not.
        assertTrue(compressed.length <= random.length, "Compressed data should not be larger than the original for random payload");
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(random, decompressed, "Decompressed random payload must equal original");
    }

    @Test
    @DisplayName("compressBytes large highly‑compressible slice")
    void testCompressBytesLargeCompressible() {
        // 5000 bytes of value 7, but we will compress a slice starting at 100 with length 4800
        byte[] data = repeatingBytes(5000, (byte) 7);
        byte[] compressed = IOUtilities.compressBytes(data, 100, 4800);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0);
        // The compressed size should be far smaller than the slice length.
        assertTrue(compressed.length < 4800, "Highly compressible data should shrink dramatically");
        byte[] decompressed = IOUtilities.uncompressBytes(compressed);
        assertEquals(4800, decompressed.length);
        for (byte b : decompressed) {
            assertEquals(7, b);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidCases")
    @DisplayName("compressBytes invalid arguments handling")
    void testCompressBytesInvalid(InvalidCase c) {
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> IOUtilities.compressBytes(c.bytes, c.offset, c.len), c.description);
        assertNotNull(thrown.getCause(), "RuntimeException should have a cause");
        assertEquals(c.cause, thrown.getCause().getClass(), "Cause should be " + c.cause.getSimpleName());
    }
}
