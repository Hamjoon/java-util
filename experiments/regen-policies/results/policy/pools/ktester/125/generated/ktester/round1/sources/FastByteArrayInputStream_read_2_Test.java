package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_read_2_Test {

    /**
     * Shared source data for all tests
     */
    private static byte[] sharedData;

    /**
     * Instance under test – created fresh for each test
     */
    private FastByteArrayInputStream stream;

    @BeforeAll
    static void setupBeforeAll() {
        // a deterministic byte pattern that is easy to verify
        sharedData = new byte[] { 10, 20, 30, 40, 50 };
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a clone so each test works with an untouched buffer
        stream = new FastByteArrayInputStream(sharedData.clone());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stray state leaks between tests
        stream = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        sharedData = null;
    }

    @Test
    void readFullBuffer() {
        // given
        byte[] dst = new byte[5];
        // when
        int bytesRead = stream.read(dst, 0, dst.length);
        // then
        assertEquals(5, bytesRead);
        assertArrayEquals(sharedData, dst);
    }

    @Test
    void readPartialWhenLessAvailable() {
        // given
        byte[] dst = new byte[4];
        // when – first read consumes three bytes
        int first = stream.read(dst, 0, 3);
        // then
        assertEquals(3, first);
        assertArrayEquals(new byte[] { 10, 20, 30, 0 }, dst);
        // when – second read asks for four bytes but only two remain
        int second = stream.read(dst, 0, 4);
        // then
        assertEquals(2, second);
        // the first two positions are overwritten with the remaining bytes (40, 50)
        assertArrayEquals(new byte[] { 40, 50, 30, 0 }, dst);
    }

    @Test
    void readZeroLengthReturnsZero() {
        // given
        byte[] dst = new byte[3];
        // when
        int bytesRead = stream.read(dst, 1, 0);
        // then
        assertEquals(0, bytesRead);
        // buffer must stay untouched
        assertArrayEquals(new byte[3], dst);
    }

    @Test
    void readAfterEndReturnsMinusOne() {
        // given – consume the whole stream
        byte[] tmp = new byte[5];
        assertEquals(5, stream.read(tmp, 0, tmp.length));
        // when – attempt another read
        int result = stream.read(tmp, 0, tmp.length);
        // then
        assertEquals(-1, result);
    }

    @Test
    void readWithNullArrayThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> stream.read(null, 0, 1));
    }

    @Test
    void readWithInvalidOffsetOrLengthThrowsIndexOutOfBoundsException() {
        byte[] dst = new byte[2];
        // negative offset
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(dst, -1, 1));
        // negative length
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(dst, 0, -1));
        // length exceeds buffer capacity (off + len > dst.length)
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(dst, 1, 2));
    }

    @Test
    void readLengthGreaterThanAvailableTruncates() {
        // Stream buffer = {10,20,30}, count = 3
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[] { 10, 20, 30 });
        // all zeros initially
        byte[] dst = new byte[5];
        // request len = 10, off = 1 (larger than available)
        int read = s.read(dst, 1, 10);
        // only 3 bytes available
        assertEquals(3, read);
        assertArrayEquals(new byte[] { 0, 10, 20, 30, 0 }, dst);
        // ensure stream is now at EOF
        assertEquals(-1, s.read(dst, 0, 1));
    }

    @Test
    void normalSuccessfulReadWithOffset() {
        // Stream buffer = {5,6,7,8}, count = 4
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[] { 5, 6, 7, 8 });
        // all zeros
        byte[] dst = new byte[5];
        int read = s.read(dst, 2, 2);
        assertEquals(2, read);
        // Expect bytes 5,6 to be placed at positions 2 and 3
        assertArrayEquals(new byte[] { 0, 0, 5, 6, 0 }, dst);
        // Verify position advanced
        assertEquals(2, read);
    }

    @Test
    void emptyStreamReadReturnsMinusOne() {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        byte[] dst = new byte[3];
        int result = empty.read(dst, 0, 1);
        assertEquals(-1, result);
    }

    @Test
    void readAfterEofWithZeroLengthReturnsMinusOne() {
        // consume all bytes first
        byte[] tmp = new byte[5];
        assertEquals(5, stream.read(tmp, 0, tmp.length));
        // now pos == count, request len == 0
        int result = stream.read(tmp, 0, 0);
        assertEquals(-1, result, "When EOF is reached the method must return -1 even if len == 0");
    }

    @Test
    void zeroLengthReadOffsetEqualsArrayLengthReturnsZero() {
        byte[] dst = new byte[3];
        // off == length, len == 0
        int read = stream.read(dst, 3, 0);
        assertEquals(0, read);
        // ensure the array stayed untouched
        assertArrayEquals(new byte[3], dst);
    }

    @Test
    void offsetGreaterThanArrayLengthThrowsIndexOutOfBoundsException() {
        byte[] dst = new byte[3];
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(dst, 4, 0));
    }

    @Test
    void readExactRemainingBytes() {
        // Stream with exactly 3 bytes
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[] { 7, 8, 9 });
        byte[] dst = new byte[3];
        int read = s.read(dst, 0, 3);
        assertEquals(3, read);
        assertArrayEquals(new byte[] { 7, 8, 9 }, dst);
        // subsequent read should be EOF
        assertEquals(-1, s.read(dst, 0, 1));
    }

    @Test
    void sequentialReads() {
        // Stream with 3 bytes
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[] { 11, 22, 33 });
        byte[] dst = new byte[4];
        // first read: 2 bytes
        int first = s.read(dst, 0, 2);
        assertEquals(2, first);
        assertArrayEquals(new byte[] { 11, 22, 0, 0 }, dst);
        // second read: request 2 bytes, only 1 remains
        int second = s.read(dst, 2, 2);
        assertEquals(1, second);
        // only the third byte should be written at position 2
        assertArrayEquals(new byte[] { 11, 22, 33, 0 }, dst);
    }

    @Test
    void readWithContentVerificationNonZeroOffset() {
        // Stream buffer = {10,20,30}
        FastByteArrayInputStream s = new FastByteArrayInputStream(new byte[] { 10, 20, 30 });
        // initialized to zeros
        byte[] dst = new byte[4];
        int read = s.read(dst, 1, 2);
        assertEquals(2, read);
        // Expect: b[1]=10, b[2]=20, others unchanged
        assertArrayEquals(new byte[] { 0, 10, 20, 0 }, dst);
    }
}
