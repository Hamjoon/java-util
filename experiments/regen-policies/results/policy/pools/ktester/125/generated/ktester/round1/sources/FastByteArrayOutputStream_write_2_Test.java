package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_write_2_Test {

    private FastByteArrayOutputStream out;

    @BeforeEach
    void setupBeforeEach() {
        // default capacity = 32
        out = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
        out = null;
    }

    /**
     * Provider for all *invalid* parameter combinations.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidParametersProvider() {
        return Stream.of(// b is null
        arguments(null, 0, 1, "b is null – the method must throw IndexOutOfBoundsException before any other checks."), // off is negative
        arguments(new byte[] { 10, 20, 30 }, -1, 1, "off is negative – should trigger the off < 0 branch."), // len is negative
        arguments(new byte[] { 10, 20, 30 }, 0, -2, "len is negative – should trigger the len < 0 branch."), // off > b.length
        arguments(new byte[] { 5, 6, 7 }, 4, 0, "off is greater than b.length – should trigger the off > b.length branch."), // off + len exceeds b.length
        arguments(new byte[] { 1, 2, 3, 4 }, 2, 3, "off + len exceeds b.length – should trigger the (off + len) > b.length branch."));
    }

    /**
     * Provider for all *valid* write scenarios (single write).
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> validWritesProvider() {
        return Stream.of(// Write the whole array
        arguments(toByteArray(1, 2, 3, 4, 5), 0, 5, toByteArray(1, 2, 3, 4, 5), "Write the whole array; buffer should contain exactly the written bytes."), // Write a sub‑range
        arguments(toByteArray(10, 20, 30, 40, 50, 60), 2, 3, toByteArray(30, 40, 50), "Write a sub‑range (offset 2, length 3); resulting buffer should contain only those three bytes."), // Write exactly the initial capacity (trigger growth)
        arguments(toByteArray(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 0, 10, toByteArray(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), "Write exactly the initial capacity (assuming default capacity < 10) to trigger buffer expansion via ensureCapacity."), // Large slice that forces another growth
        arguments(toByteArray(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), 4, 8, toByteArray(4, 5, 6, 7, 8, 9, 10, 11), "Write a slice that causes the internal buffer to grow beyond its current size (large write after previous small writes)."), // Zero‑length write (normal path, no data)
        arguments(toByteArray(0, 1, 2), 0, 0, new byte[0], "len is zero and off is within bounds – normal path, no data is written; toByteArray() returns an empty array."));
    }

    /**
     * Helper to build a byte[] from a list of int literals (0‑255).
     */
    private static byte[] toByteArray(int... values) {
        byte[] arr = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            arr[i] = (byte) values[i];
        }
        return arr;
    }

    @Test
    void writePartialArray_updatesBufferCorrectly() {
        // 10 bytes
        byte[] source = "HelloWorld".getBytes();
        // "lloWo"
        out.write(source, 2, 5);
        assertEquals(5, out.size());
        assertArrayEquals("lloWo".getBytes(), out.toByteArray());
    }

    @Test
    void writeFullArray_updatesBufferCorrectly() {
        byte[] source = "ABC".getBytes();
        out.write(source, 0, source.length);
        assertEquals(3, out.size());
        assertArrayEquals(source, out.toByteArray());
    }

    @Test
    void writeZeroLengthLeavesStreamUnchanged() {
        byte[] source = "Data".getBytes();
        // length zero
        out.write(source, 1, 0);
        assertEquals(0, out.size());
        assertArrayEquals(new byte[0], out.toByteArray());
    }

    @Test
    void writeBeyondInitialCapacity_isHandledByEnsureCapacity() {
        // default capacity is 32 – we write 100 bytes to force growth
        byte[] many = new byte[100];
        for (int i = 0; i < many.length; i++) {
            many[i] = (byte) i;
        }
        out.write(many, 0, many.length);
        assertEquals(100, out.size());
        assertArrayEquals(many, out.toByteArray());
    }

    @Test
    void writeNullArray_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> out.write(null, 0, 1));
    }

    @Test
    void writeNegativeOffset_throwsIndexOutOfBoundsException() {
        byte[] data = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> out.write(data, -1, 2));
    }

    @Test
    void writeNegativeLength_throwsIndexOutOfBoundsException() {
        byte[] data = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> out.write(data, 0, -5));
    }

    @Test
    void writeOffsetGreaterThanArrayLength_throwsIndexOutOfBoundsException() {
        byte[] data = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> out.write(data, 4, 0));
    }

    @Test
    void writeOffsetPlusLengthExceedsArrayLength_throwsIndexOutOfBoundsException() {
        byte[] data = { 1, 2, 3, 4, 5 };
        // off=3, len=3 => off+len = 6 > data.length (5)
        assertThrows(IndexOutOfBoundsException.class, () -> out.write(data, 3, 3));
    }

    @Test
    void writeOffsetPlusLengthOverflow_throwsIndexOutOfBoundsException() {
        byte[] data = new byte[10];
        int off = Integer.MAX_VALUE - 5;
        // off + len overflows to negative
        int len = 10;
        assertThrows(IndexOutOfBoundsException.class, () -> out.write(data, off, len));
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("invalidParametersProvider")
    void writeInvalidParameters_throwsException(byte[] b, int off, int len, String description) {
        assertThrows(IndexOutOfBoundsException.class, () -> out.write(b, off, len), description);
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("validWritesProvider")
    void writeValidParameters_writesCorrectly(byte[] b, int off, int len, byte[] expected, String description) {
        out.write(b, off, len);
        assertEquals(expected.length, out.size(), "Size mismatch – " + description);
        assertArrayEquals(expected, out.toByteArray(), "Buffer content mismatch – " + description);
    }

    @Test
    void writeSameArrayTwice_concatenatesData() {
        byte[] src = toByteArray(7, 8, 9);
        // first write
        out.write(src, 0, src.length);
        // second write (same array)
        out.write(src, 0, src.length);
        byte[] expected = toByteArray(7, 8, 9, 7, 8, 9);
        assertEquals(expected.length, out.size(), "Size after two writes");
        assertArrayEquals(expected, out.toByteArray(), "Buffer after two writes");
    }
}
