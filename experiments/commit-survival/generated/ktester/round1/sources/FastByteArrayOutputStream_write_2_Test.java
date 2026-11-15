package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_write_2_Test {

    private FastByteArrayOutputStream outputStream;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor for a fresh instance before each test
        outputStream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent
        outputStream = null;
    }

    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(// Write a sub‑range of a valid byte array
        Arguments.of(new byte[] { 1, 2, 3, 4, 5 }, 1, 3, new byte[] { 2, 3, 4 }), // Write the whole array
        Arguments.of(new byte[] { 10, 20, 30 }, 0, 3, new byte[] { 10, 20, 30 }), // Write zero length – expect no change
        Arguments.of(new byte[] { 7, 8, 9 }, 0, 0, new byte[0]), // Write that forces internal buffer growth
        Arguments.of(new byte[] { 5, 6, 7, 8, 9 }, 0, 5, new byte[] { 5, 6, 7, 8, 9 }));
    }

    private static Stream<Arguments> invalidParameterProvider() {
        return Stream.of(// null array
        Arguments.of(null, 0, 1), // negative offset
        Arguments.of(new byte[] { 1, 2, 3 }, -1, 1), // negative length
        Arguments.of(new byte[] { 1, 2, 3 }, 0, -1), // offset > length
        Arguments.of(new byte[] { 1, 2, 3 }, 4, 0), // off+len exceeds length
        Arguments.of(new byte[] { 1, 2, 3 }, 2, 2), // overflow case
        Arguments.of(new byte[] { 1, 2, 3 }, Integer.MAX_VALUE, 1));
    }

    private static Stream<Arguments> boundaryConditionProvider() {
        return Stream.of(// off equals array length with len zero – no‑op
        Arguments.of(new byte[] { 10, 20, 30 }, 3, 0, new byte[0]), // off+len exactly equals array length – writes the tail
        Arguments.of(new byte[] { 1, 2, 3, 4, 5 }, 2, 3, new byte[] { 3, 4, 5 }));
    }

    @Test
    void writeValidRangeCopiesCorrectBytes() {
        // 10 bytes
        byte[] data = "HelloWorld".getBytes();
        // copy "lloWo" (offset 2, length 5)
        outputStream.write(data, 2, 5);
        assertEquals(5, outputStream.size());
        assertArrayEquals("lloWo".getBytes(), outputStream.toByteArray());
    }

    @Test
    void writeZeroLengthLeavesStreamUnchanged() {
        byte[] data = "sample".getBytes();
        outputStream.write(data, 0, 0);
        assertEquals(0, outputStream.size());
        assertArrayEquals(new byte[0], outputStream.toByteArray());
    }

    @Test
    void writeLargeArrayTriggersCapacityIncrease() {
        // start with a very small buffer to force growth
        outputStream = new FastByteArrayOutputStream(4);
        byte[] large = new byte[128];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) i;
        }
        outputStream.write(large, 0, large.length);
        assertEquals(128, outputStream.size());
        assertArrayEquals(large, outputStream.toByteArray());
    }

    @Test
    void writeNullArrayThrowsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> outputStream.write(null, 0, 1));
    }

    @Test
    void writeNegativeOffsetThrowsIndexOutOfBoundsException() {
        byte[] data = "abc".getBytes();
        assertThrows(IndexOutOfBoundsException.class, () -> outputStream.write(data, -1, 2));
    }

    @Test
    void writeNegativeLengthThrowsIndexOutOfBoundsException() {
        byte[] data = "abc".getBytes();
        assertThrows(IndexOutOfBoundsException.class, () -> outputStream.write(data, 0, -2));
    }

    @Test
    void writeOffsetGreaterThanArrayLengthThrowsIndexOutOfBoundsException() {
        byte[] data = "abc".getBytes();
        assertThrows(IndexOutOfBoundsException.class, () -> outputStream.write(data, 4, 1));
    }

    @Test
    void writeOffsetPlusLengthExceedsArrayLengthThrowsIndexOutOfBoundsException() {
        byte[] data = "abc".getBytes();
        // off=1, len=3 => off+len = 4 > data.length (3)
        assertThrows(IndexOutOfBoundsException.class, () -> outputStream.write(data, 1, 3));
    }

    @Test
    void writeOffsetPlusLengthOverflowThrowsIndexOutOfBoundsException() {
        byte[] data = new byte[10];
        // Choose values that cause integer overflow when added
        int off = Integer.MAX_VALUE - 5;
        // off + len wraps negative
        int len = 10;
        assertThrows(IndexOutOfBoundsException.class, () -> outputStream.write(data, off, len));
    }

    /**
     * Parameterized version of the normal‑operation cases described in the JSON file.
     */
    @ParameterizedTest(name = "{index}: write({2} bytes from offset {1})")
    @MethodSource("normalOperationProvider")
    void writeNormalOperationParameterized(byte[] source, int off, int len, byte[] expected) {
        outputStream.write(source, off, len);
        assertEquals(expected.length, outputStream.size(), "Size after write should match number of bytes written");
        assertArrayEquals(expected, outputStream.toByteArray(), "Internal buffer content does not match expected data");
    }

    /**
     * Parameterized version of the invalid‑parameter cases described in the JSON file.
     */
    @ParameterizedTest(name = "{index}: write({1}, off={2}, len={3}) → expect exception")
    @MethodSource("invalidParameterProvider")
    void writeInvalidParametersThrowsException(byte[] source, int off, int len) {
        assertThrows(IndexOutOfBoundsException.class, () -> outputStream.write(source, off, len));
    }

    @ParameterizedTest(name = "{index}: off={1}, len={2}")
    @MethodSource("boundaryConditionProvider")
    void writeBoundaryConditions(byte[] source, int off, int len, byte[] expected) {
        outputStream.write(source, off, len);
        assertEquals(expected.length, outputStream.size(), "Size after boundary write should match expected");
        assertArrayEquals(expected, outputStream.toByteArray(), "Buffer content after boundary write is incorrect");
    }

    @Test
    void writeSequentially_AppendsDataCorrectly() {
        // First write
        byte[] first = new byte[] { 5, 6, 7 };
        outputStream.write(first, 0, first.length);
        assertEquals(3, outputStream.size());
        assertArrayEquals(new byte[] { 5, 6, 7 }, outputStream.toByteArray());
        // Second write – sub‑range of another array
        byte[] second = new byte[] { 8, 9, 10, 11 };
        // writes 9,10
        outputStream.write(second, 1, 2);
        assertEquals(5, outputStream.size());
        assertArrayEquals(new byte[] { 5, 6, 7, 9, 10 }, outputStream.toByteArray());
    }

    @Test
    void writeDoesNotShareBufferWithSourceArray() {
        byte[] source = new byte[] { 42, 43, 44 };
        outputStream.write(source, 0, source.length);
        // Mutate the original array after the write
        source[0] = 99;
        // The internal buffer must still contain the original values
        assertArrayEquals(new byte[] { 42, 43, 44 }, outputStream.toByteArray(), "Internal buffer should be independent of the source array");
        assertEquals(3, outputStream.size());
    }

    @Test
    void writeVeryLargeArrayTriggersMultipleGrowthSteps() {
        // Create a 2000‑byte array with a predictable pattern
        byte[] large = new byte[2000];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) (i % 256);
        }
        outputStream.write(large, 0, large.length);
        assertEquals(2000, outputStream.size(), "Size after writing 2000 bytes should be 2000");
        assertArrayEquals(large, outputStream.toByteArray(), "Content of the buffer after large write does not match source");
    }
}
