package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class IOUtilities_compressBytes_2_Test {

    /**
     * A sample text that compresses well (repeating pattern).
     */
    private static final String SAMPLE_TEXT = "ABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCD";

    private byte[] originalBytes;

    /**
     * A repetitive sample that compresses well.
     */
    private static final String REPEAT_TEXT = "ABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCDABCD";

    /**
     * A non‑repetitive sample that is essentially incompressible.
     */
    private static final byte[] INCOMPRESSIBLE = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19 };

    @BeforeEach
    void setupBeforeEach() {
        // Prepare a fresh byte array for each test.
        originalBytes = SAMPLE_TEXT.getBytes();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly necessary.
        originalBytes = null;
    }

    private static Stream<Arguments> invalidOffsetLenProvider() {
        return Stream.of(// null array
        Arguments.of(null, 0, 0, NullPointerException.class), // negative offset
        Arguments.of(new byte[] { 1, 2, 3, 4, 5 }, -1, 3, IndexOutOfBoundsException.class), // negative length
        Arguments.of(new byte[] { 1, 2, 3, 4, 5 }, 2, -2, IndexOutOfBoundsException.class), // offset+len exceeds array length
        Arguments.of(new byte[] { 1, 2, 3, 4, 5 }, 3, 3, IndexOutOfBoundsException.class));
    }

    @Test
    void compressBytes_returnsCompressedDataThatCanBeDecompressed() {
        // GIVEN a non‑empty byte array (originalBytes) prepared in @BeforeEach
        // WHEN the array is compressed
        byte[] compressed = IOUtilities.compressBytes(originalBytes);
        // THEN the compressed data should not be null and should be smaller than the original
        assertNotNull(compressed, "Compressed result must not be null");
        assertTrue(compressed.length < originalBytes.length, "Compressed data should be smaller than the original for repetitive input");
        // AND decompressing it yields the original content
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(originalBytes, uncompressed, "Decompressed bytes must match the original input");
    }

    @Test
    void compressBytes_withEmptyArrayReturnsEmptyArray() {
        // GIVEN an empty byte array
        byte[] empty = new byte[0];
        // WHEN compressing the empty array
        byte[] compressed = IOUtilities.compressBytes(empty);
        // THEN the result should also be an empty array (no data to compress)
        assertNotNull(compressed, "Result must not be null even for empty input");
        assertEquals(0, compressed.length, "Compressed empty array should have length 0");
    }

    @Test
    void compressBytes_nullArgumentThrowsNullPointerException() {
        // GIVEN a null reference
        byte[] nullBytes = null;
        // WHEN invoking compressBytes with null, THEN a NullPointerException is expected
        assertThrows(NullPointerException.class, () -> IOUtilities.compressBytes(nullBytes), "compressBytes(null) should throw NullPointerException");
    }

    @Test
    void compressBytes_singleByteArrayIsCompressible() {
        byte[] single = new byte[] { 42 };
        byte[] compressed = IOUtilities.compressBytes(single);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0, "Compressed result should contain at least the GZIP header");
        assertArrayEquals(single, IOUtilities.uncompressBytes(compressed), "Decompressed single‑byte array must equal the original");
    }

    @Test
    void compressBytes_incompressibleDataMayGrowButIsReversible() {
        byte[] compressed = IOUtilities.compressBytes(INCOMPRESSIBLE);
        assertNotNull(compressed);
        // For incompressible data the compressed length can be >= original length.
        assertTrue(compressed.length >= INCOMPRESSIBLE.length, "Compressed length should be >= original for incompressible data");
        assertArrayEquals(INCOMPRESSIBLE, IOUtilities.uncompressBytes(compressed), "Decompressed data must match the original");
    }

    @Test
    void compressBytes_largeRepeatingArrayCompressesDramatically() {
        // Build ~1 KB of repeating 'x' (value 120)
        byte[] large = new byte[1024];
        for (int i = 0; i < large.length; i++) {
            large[i] = 120;
        }
        byte[] compressed = IOUtilities.compressBytes(large);
        assertNotNull(compressed);
        assertTrue(compressed.length < large.length / 4, "Compressed length should be much smaller for highly repetitive data");
        assertArrayEquals(large, IOUtilities.uncompressBytes(compressed), "Decompressed large array must equal the original");
    }

    @ParameterizedTest(name = "{index}: bytes={0}, offset={1}, len={2} → expect {3}")
    @MethodSource("invalidOffsetLenProvider")
    void compressBytes_offsetLen_invalidArguments_throwsException(byte[] bytes, int offset, int len, Class<? extends Exception> expected) {
        assertThrows(expected, () -> IOUtilities.compressBytes(bytes, offset, len), () -> String.format("compressBytes(%s, %d, %d) should throw %s", bytes == null ? "null" : "byte[" + bytes.length + "]", offset, len, expected.getSimpleName()));
    }

    @Test
    void compressBytes_offsetLen_validSubset_isCompressedAndReversible() {
        byte[] data = new byte[] { 10, 20, 30, 40, 50, 60, 70, 80 };
        // start at 30
        int offset = 2;
        // 30,40,50,60
        int len = 4;
        byte[] expectedSubset = new byte[] { 30, 40, 50, 60 };
        byte[] compressed = IOUtilities.compressBytes(data, offset, len);
        assertNotNull(compressed);
        assertTrue(compressed.length > 0, "Compressed subset must not be empty");
        byte[] uncompressed = IOUtilities.uncompressBytes(compressed);
        assertArrayEquals(expectedSubset, uncompressed, "Decompressed subset must match the specified offset/len region");
    }
}
