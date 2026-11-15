package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_write_2_Test {

    /**
     * The stream under test – created fresh for each test method.
     */
    private FastByteArrayOutputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Small initial capacity to verify automatic growth logic.
        stream = new FastByteArrayOutputStream(4);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – not strictly required but keeps
        // the pattern symmetrical with the template.
        stream = null;
    }

    /**
     * Converts an {@code int[]} literal (e.g. {@code [1,2,3]}) to a {@code byte[]}
     */
    private static byte[] toByteArray(int[] ints) {
        byte[] bytes = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            bytes[i] = (byte) ints[i];
        }
        return bytes;
    }

    static Stream<Arguments> invalidArgumentProvider() {
        return Stream.of(// b is null
        Arguments.of("b is null", null, 0, 1), // off is negative
        Arguments.of("off is negative", toByteArray(new int[] { 10, 20, 30 }), -1, 1), // len is negative
        Arguments.of("len is negative", toByteArray(new int[] { 10, 20, 30 }), 0, -5), // off greater than array length
        Arguments.of("off greater than array length", toByteArray(new int[] { 10, 20, 30 }), 4, 0), // off+len exceeds array length
        Arguments.of("off+len exceeds array length", toByteArray(new int[] { 10, 20, 30 }), 2, 2), // off+len overflows to negative (Integer.MAX_VALUE + offset)
        Arguments.of("off+len overflows to negative", toByteArray(new int[] { 1 }), Integer.MAX_VALUE, 1));
    }

    static Stream<Arguments> validWriteProvider() {
        return Stream.of(// whole array write
        Arguments.of("Write whole array (len == array length)", toByteArray(new int[] { 5, 6, 7, 8 }), 0, 4, 4, toByteArray(new int[] { 5, 6, 7, 8 })), // sub‑range write
        Arguments.of("Write sub‑range (offset 2, length 2)", toByteArray(new int[] { 1, 2, 3, 4, 5 }), 2, 2, 2, toByteArray(new int[] { 3, 4 })), // zero‑length write – should be a no‑op
        Arguments.of("Zero‑length write (len == 0)", toByteArray(new int[] { 9, 10 }), 1, 0, 0, // empty content
        new byte[0]), // write exactly ten bytes
        Arguments.of("Write exactly ten bytes", toByteArray(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }), 0, 10, 10, toByteArray(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })));
    }

    @Test
    void shouldWriteWholeArrayWhenOffsetZeroAndLengthEqualsArrayLength() {
        // given
        // length = 4, fits initial buffer
        byte[] data = "ABCD".getBytes();
        // when
        stream.write(data, 0, data.length);
        // then
        assertEquals(4, stream.size());
        assertArrayEquals(data, stream.toByteArray());
    }

    @Test
    void shouldWritePartialArrayUsingOffsetAndLength() {
        // given
        // length = 10
        byte[] data = "HelloWorld".getBytes();
        // when
        // write "lloWo"
        stream.write(data, 2, 5);
        // then
        assertEquals(5, stream.size());
        assertArrayEquals("lloWo".getBytes(), stream.toByteArray());
    }

    @Test
    void shouldGrowInternalBufferWhenWrittenBeyondInitialCapacity() {
        // given
        // fills initial capacity (4)
        byte[] first = "1234".getBytes();
        // forces growth
        byte[] second = "56789".getBytes();
        // when
        // size = 4
        stream.write(first, 0, first.length);
        // size = 9
        stream.write(second, 0, second.length);
        // then
        assertEquals(9, stream.size());
        byte[] expected = "123456789".getBytes();
        assertArrayEquals(expected, stream.toByteArray());
    }

    @Test
    void shouldThrowWhenArrayIsNull() {
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(null, 0, 1));
    }

    @Test
    void shouldThrowWhenOffsetIsNegative() {
        byte[] data = "abc".getBytes();
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(data, -1, 1));
    }

    @Test
    void shouldThrowWhenLengthIsNegative() {
        byte[] data = "abc".getBytes();
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(data, 0, -5));
    }

    @Test
    void shouldThrowWhenOffsetGreaterThanArrayLength() {
        // length = 3
        byte[] data = "abc".getBytes();
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(data, 4, 0));
    }

    @Test
    void shouldThrowWhenOffsetPlusLengthExceedsArrayLength() {
        // length = 4
        byte[] data = "abcd".getBytes();
        // offset = 2, length = 3 => 2+3 = 5 > 4
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(data, 2, 3));
    }

    @Test
    void shouldThrowWhenOffsetPlusLengthOverflowsToNegative() {
        // This reproduces the overflow branch in the original implementation.
        // We need a non‑null array and a legal offset, but a length that
        // causes (off + len) to wrap around to a negative value.
        byte[] data = new byte[10];
        // valid offset
        int offset = 5;
        // huge length -> overflow
        int length = Integer.MAX_VALUE;
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(data, offset, length));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidArgumentProvider")
    void shouldThrowIndexOutOfBoundsForInvalidArguments(String description, byte[] b, int off, int len) {
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(b, off, len), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validWriteProvider")
    void shouldWriteBytesCorrectly(String description, byte[] input, int off, int len, int expectedSize, byte[] expectedContent) {
        // when
        stream.write(input, off, len);
        // then
        assertEquals(expectedSize, stream.size(), "Size after write should match expected");
        assertArrayEquals(expectedContent, stream.toByteArray(), "Buffer content after write should match expected");
    }

    @Test
    void shouldThrowWhenOffsetPlusLengthOverflows() {
        // Arrange: a non‑null array, a legal offset, and a length that overflows.
        byte[] data = new byte[10];
        int offset = 1;
        // causes (offset + length) to wrap negative
        int length = Integer.MAX_VALUE;
        // Act & Assert
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(data, offset, length), "Overflow in off+len should trigger IndexOutOfBoundsException");
    }

    @Test
    void shouldGrowBufferMultipleTimesWhenRepeatedWritesExceedCapacity() {
        // Write a series of small chunks that together exceed the initial capacity.
        // 2 bytes
        byte[] chunk1 = "AB".getBytes();
        // 5 bytes
        byte[] chunk2 = "CDEFG".getBytes();
        // 5 bytes
        byte[] chunk3 = "HIJKL".getBytes();
        // size = 2
        stream.write(chunk1, 0, chunk1.length);
        // size = 7
        stream.write(chunk2, 0, chunk2.length);
        // size = 12
        stream.write(chunk3, 0, chunk3.length);
        assertEquals(12, stream.size());
        assertArrayEquals("ABCDEFGHIJKL".getBytes(), stream.toByteArray());
    }
}
