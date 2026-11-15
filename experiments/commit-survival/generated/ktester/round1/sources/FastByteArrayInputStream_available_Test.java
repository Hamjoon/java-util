package com.cedarsoftware.util;

import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_available_Test {

    private FastByteArrayInputStream stream;

    private byte[] data;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh stream before each test.
        data = new byte[] { 10, 20, 30, 40, 50 };
        stream = new FastByteArrayInputStream(data);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly close the stream – the implementation allows further reads.
        try {
            stream.close();
        } catch (IOException ignored) {
            // Ignored – close is not expected to throw in this implementation.
        }
        stream = null;
        data = null;
    }

    @Test
    void givenNewStream_whenAvailable_thenReturnsFullLength() {
        // given – stream is created in @BeforeEach with 5 bytes
        // when
        int available = stream.available();
        // then
        assertEquals(5, available, "Available should equal the number of bytes in the buffer");
    }

    @Test
    void givenStreamAfterReadingOneByte_whenAvailable_thenDecrementsByOne() {
        // given
        // consume first byte (10)
        stream.read();
        // when
        int available = stream.available();
        // then
        assertEquals(4, available, "Available should decrease after a read operation");
    }

    @Test
    void givenStreamAfterSkippingTwoBytes_whenAvailable_thenReflectsRemaining() {
        // given
        // skip 10 and 20
        stream.skip(2);
        // when
        int available = stream.available();
        // then
        assertEquals(3, available, "Available should reflect bytes remaining after skip");
    }

    @Test
    void givenStreamAfterReadingAllBytes_whenAvailable_thenZero() {
        // given
        while (stream.read() != -1) {
            // consume all bytes
        }
        // when
        int available = stream.available();
        // then
        assertEquals(0, available, "Available should be zero after the entire content is read");
    }

    @Test
    void givenEmptyStream_whenAvailable_thenZero() {
        // given
        stream = new FastByteArrayInputStream(new byte[0]);
        // when
        int available = stream.available();
        // then
        assertEquals(0, available, "Available must be zero for an empty buffer");
    }

    @Test
    void givenClosedStream_whenAvailable_thenStillReportsRemaining() throws IOException {
        // given
        // consume one byte, leaving 4
        stream.read();
        // close should not affect availability
        stream.close();
        // when
        int available = stream.available();
        // then
        assertEquals(4, available, "Close should not change the reported available bytes");
    }

    /**
     * Using reflection to create an illegal internal state where {@code pos > count}.
     * The method does not guard against this, so a negative result is expected.
     */
    @Test
    void givenIllegalState_whenAvailable_thenReturnsNegative() throws Exception {
        // given – stream with 5 bytes, then force pos to 7 via reflection
        Field posField = FastByteArrayInputStream.class.getDeclaredField("pos");
        posField.setAccessible(true);
        // pos = 7, count stays 5
        posField.setInt(stream, 7);
        // when
        int available = stream.available();
        // then
        assertEquals(-2, available, "When pos (7) > count (5) the calculation count - pos yields -2");
    }

    /**
     * Verify that {@code skip} correctly advances the internal position and that
     * {@code available} reflects the remaining bytes.
     */
    @Test
    void givenSkipTwoBytes_whenAvailable_thenReflectsRemaining() {
        // given
        long skipped = stream.skip(2);
        assertEquals(2L, skipped, "skip should report the number of bytes actually skipped");
        // when
        int available = stream.available();
        // then
        assertEquals(3, available, "Available should be count - pos = 5 - 2 = 3");
    }

    /**
     * Test the interaction of {@code mark}, {@code read} and {@code reset}.
     * After resetting, the position should be restored to the marked position.
     */
    @Test
    void givenMarkReadReset_whenAvailable_thenReflectsCurrentPos() {
        // given
        // mark at position 0
        stream.mark(0);
        // consume one byte, pos = 1
        stream.read();
        // back to marked position (0)
        stream.reset();
        // when
        int available = stream.available();
        // then
        // after reset the position is back to 0, so available = 5
        assertEquals(5, available, "After reset to position 0, available should be 5");
    }

    /**
     * Reading the whole content via {@code read(byte[], int, int)} should move the position
     * to the end, resulting in zero bytes available.
     */
    @Test
    void givenReadAllBytesIntoBuffer_whenAvailable_thenZero() {
        // given
        byte[] buffer = new byte[10];
        int bytesRead = stream.read(buffer, 0, buffer.length);
        assertEquals(5, bytesRead, "read should return the number of bytes actually read");
        // when
        int available = stream.available();
        // then
        assertEquals(0, available, "After reading all bytes, available must be zero");
    }

    /**
     * A zero‑length read must not modify the internal position.
     */
    @Test
    void givenZeroLengthRead_whenAvailable_thenUnchanged() {
        // given
        byte[] buffer = new byte[10];
        int bytesRead = stream.read(buffer, 0, 0);
        assertEquals(0, bytesRead, "Zero‑length read should return 0");
        // when
        int available = stream.available();
        // then
        assertEquals(5, available, "Zero‑length read must not affect available bytes");
    }

    /**
     * Calling {@code available} repeatedly without intervening reads must return the same value.
     */
    @Test
    void givenTwoConsecutiveAvailableCalls_whenNoRead_thenIdempotent() {
        // given
        // consume one byte, pos = 1
        stream.read();
        // when
        int first = stream.available();
        int second = stream.available();
        // then
        assertEquals(4, first, "First call should report 4 remaining bytes");
        assertEquals(4, second, "Second call must be identical – method is idempotent");
    }

    /**
     * Closing the stream does not affect the internal counters.
     */
    @Test
    void givenCloseAfterPartialRead_whenAvailable_thenUnaffected() throws IOException {
        // given
        // pos = 1
        stream.read();
        // pos = 2
        stream.read();
        // close should be a no‑op for counters
        stream.close();
        // when
        int available = stream.available();
        // then
        assertEquals(3, available, "Close must not change the remaining byte count");
    }

    /**
     * Reset without a prior {@code mark} should revert the position to the start (0).
     */
    @Test
    void givenResetWithoutMark_whenAvailable_thenFullLength() {
        // given
        // pos = 1
        stream.read();
        // pos = 2
        stream.read();
        // no explicit mark – default behavior resets to 0
        stream.reset();
        // when
        int available = stream.available();
        // then
        assertEquals(5, available, "Reset without a prior mark should restore position to 0");
    }
}
