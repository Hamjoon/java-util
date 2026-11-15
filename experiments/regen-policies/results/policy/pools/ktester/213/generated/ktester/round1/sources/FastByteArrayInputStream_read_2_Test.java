package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_read_2_Test {

    private FastByteArrayInputStream stream;

    private byte[] sourceData;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh stream before each test
        sourceData = new byte[] { 10, 20, 30, 40, 50 };
        stream = new FastByteArrayInputStream(sourceData);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern clear
        stream = null;
        sourceData = null;
    }

    private static Stream<Arguments> provideReadLengthLargerThanAvailable() {
        return Stream.of(// stream {1,2,3}, request len 5, expect 3 bytes read
        Arguments.of(new byte[] { 1, 2, 3 }, new byte[5], 0, 5, 3));
    }

    private static Stream<Arguments> provideExactAvailable() {
        return Stream.of(Arguments.of(new byte[] { 9, 10 }, new byte[2], 0, 2, 2));
    }

    @Test
    void readFullArray_returnsAllBytes() {
        byte[] buffer = new byte[5];
        int bytesRead = stream.read(buffer, 0, buffer.length);
        assertEquals(5, bytesRead, "Should read all available bytes");
        assertArrayEquals(sourceData, buffer, "Buffer should contain the original data");
    }

    @Test
    void readPartialArray_respectsLengthAndUpdatesPosition() {
        byte[] buffer = new byte[3];
        // request 2 bytes
        int bytesRead = stream.read(buffer, 0, 2);
        assertEquals(2, bytesRead);
        assertArrayEquals(new byte[] { 10, 20, 0 }, buffer, "Only first two bytes should be filled");
        // subsequent read should start from the third byte
        int nextByte = stream.read();
        assertEquals(30, nextByte);
    }

    @Test
    void readWithOffset_placesDataAtCorrectPosition() {
        byte[] buffer = new byte[5];
        // write into positions 2 and 3
        int bytesRead = stream.read(buffer, 2, 2);
        assertEquals(2, bytesRead);
        assertArrayEquals(new byte[] { 0, 0, 10, 20, 0 }, buffer);
    }

    @Test
    void readWhenNoMoreData_returnsMinusOne() {
        // Exhaust the stream
        stream.read(new byte[5], 0, 5);
        assertEquals(-1, stream.read(new byte[1], 0, 1), "Reading past end should return -1");
    }

    @Test
    void readZeroLength_returnsZeroWithoutModifyingBuffer() {
        byte[] buffer = new byte[] { 9, 9, 9 };
        int bytesRead = stream.read(buffer, 1, 0);
        assertEquals(0, bytesRead);
        // Buffer must stay untouched
        assertArrayEquals(new byte[] { 9, 9, 9 }, buffer);
    }

    @Test
    void readNullArray_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> stream.read(null, 0, 1));
    }

    @Test
    void readWithNegativeOffset_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[2], -1, 1));
    }

    @Test
    void readWithNegativeLength_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[2], 0, -5));
    }

    @Test
    void readWithLengthExceedingBuffer_throwsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[2], 0, 3));
    }

    @Test
    void readLengthGreaterThanAvailable_truncatesToAvailable() {
        byte[] buffer = new byte[10];
        // Request more than available (5) – should only read 5
        int bytesRead = stream.read(buffer, 0, 8);
        assertEquals(5, bytesRead);
        assertArrayEquals(new byte[] { 10, 20, 30, 40, 50, 0, 0, 0, 0, 0 }, buffer);
    }

    /**
     * Scenario: the stream contains fewer bytes than requested.
     * Expected: the method returns only the available bytes.
     */
    @ParameterizedTest(name = "{index} => bufLen={1}, requestLen={2}, expectedRead={3}")
    @MethodSource("provideReadLengthLargerThanAvailable")
    void readLengthLargerThanAvailable(byte[] streamData, byte[] buffer, int off, int len, int expectedRead) {
        FastByteArrayInputStream s = new FastByteArrayInputStream(streamData);
        int bytesRead = s.read(buffer, off, len);
        assertEquals(expectedRead, bytesRead, "Should return the number of bytes actually available");
        // Verify that the first 'expectedRead' bytes in the buffer match the source data
        for (int i = 0; i < expectedRead; i++) {
            assertEquals(streamData[i], buffer[off + i], "Byte at position " + (off + i) + " should match source");
        }
    }

    /**
     * Scenario: request exactly the number of bytes that exist in the stream.
     */
    @ParameterizedTest(name = "{index} => streamSize={1}, requestLen={2}")
    @MethodSource("provideExactAvailable")
    void readExactAvailableBytes(byte[] streamData, byte[] buffer, int off, int len, int expectedRead) {
        FastByteArrayInputStream s = new FastByteArrayInputStream(streamData);
        int bytesRead = s.read(buffer, off, len);
        assertEquals(expectedRead, bytesRead, "Should read the exact number of available bytes");
        // The buffer region should now contain the stream data
        for (int i = 0; i < expectedRead; i++) {
            assertEquals(streamData[i], buffer[off + i], "Byte at buffer offset " + (off + i) + " must equal source");
        }
    }

    /**
     * Scenario: read with a non‑zero offset in the destination array.
     */
    @Test
    void readWithNonZeroOffset_respectsOffset() {
        byte[] src = new byte[] { 7, 8 };
        FastByteArrayInputStream s = new FastByteArrayInputStream(src);
        // initially all zeros
        byte[] buffer = new byte[5];
        int bytesRead = s.read(buffer, 2, 2);
        assertEquals(2, bytesRead);
        assertArrayEquals(new byte[] { 0, 0, 7, 8, 0 }, buffer, "Bytes must be placed starting at the specified offset");
    }

    /**
     * Scenario: the underlying byte array is empty (count == 0).
     * Expected: any read returns -1 immediately.
     */
    @Test
    void readFromEmptyBuffer_returnsMinusOne() {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        int result = empty.read(new byte[1], 0, 1);
        assertEquals(-1, result, "Reading from an empty stream must return -1");
    }

    /**
     * Scenario: sequential reads on a stream larger than the buffer.
     * Expected: the first call consumes the first chunk, the second call
     * consumes the remaining bytes.
     */
    @Test
    void sequentialReads_returnCorrectChunks() {
        byte[] src = new byte[] { 10, 20, 30, 40 };
        FastByteArrayInputStream s = new FastByteArrayInputStream(src);
        byte[] buf = new byte[2];
        // first read – should get 10,20
        int first = s.read(buf, 0, 2);
        assertEquals(2, first);
        assertArrayEquals(new byte[] { 10, 20 }, buf);
        // second read – should get 30,40
        int second = s.read(buf, 0, 2);
        assertEquals(2, second);
        assertArrayEquals(new byte[] { 30, 40 }, buf);
    }
}
