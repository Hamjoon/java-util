package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_mark_Test {

    /**
     * Test data used for all tests in this class.
     */
    private static final byte[] TEST_DATA = { 10, 20, 30, 40, 50 };

    /**
     * Instance under test – created fresh before each test.
     */
    private FastByteArrayInputStream stream;

    /**
     * Simple holder for the “mark at start” scenarios.
     */
    private static class MarkScenario {

        final int readLimit;

        final String expected;

        MarkScenario(int readLimit, String expected) {
            this.readLimit = readLimit;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "readLimit=" + readLimit;
        }
    }

    /**
     * Holds parameters for the “mark after reading some bytes” group.
     */
    private static class MarkAfterReadScenario {

        // how many bytes to consume before marking
        final int bytesRead;

        final int readLimit;

        // -1 for EOF
        final int expectedNextByte;

        final String expected;

        MarkAfterReadScenario(int bytesRead, int readLimit, int expectedNextByte, String expected) {
            this.bytesRead = bytesRead;
            this.readLimit = readLimit;
            this.expectedNextByte = expectedNextByte;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return "bytesRead=" + bytesRead + ", readLimit=" + readLimit;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a new stream for every test to guarantee isolation
        stream = new FastByteArrayInputStream(TEST_DATA.clone());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly close the stream – FastByteArrayInputStream does not
        // release external resources, but this mirrors typical usage.
        try {
            stream.close();
        } catch (Exception ignored) {
            // Ignored – close is safe even if it does nothing
        }
    }

    /**
     * Parameter source for groups that test the same logical behaviour
     * with different {@code readLimit} values.
     */
    private static Stream<MarkScenario> markAtStartProvider() {
        return Stream.of(new MarkScenario(0, "Calling reset() after mark() returns the stream to position 0, so the first byte read after reset() equals the first byte of the underlying array."), new MarkScenario(12345, "Calling reset() after mark() returns the stream to position 0, so the first byte read after reset() equals the first byte of the underlying array."));
    }

    /**
     * Test marking after having read a few bytes. Two sub‑cases:
     *   - readLimit = 10 (normal positive)
     *   - readLimit = 0   (zero)
     */
    private static Stream<MarkAfterReadScenario> markAfterReadProvider() {
        return Stream.of(new MarkAfterReadScenario(3, 10, 40, "After reading 3 bytes, calling mark(10) stores position 3. Reset returns to position 3, so the next read returns the 4th byte (40)."), new MarkAfterReadScenario(5, 0, -1, "After reading 5 bytes (EOF), calling mark(0) stores position 5. Reset returns to EOF; subsequent read yields -1."));
    }

    @Test
    void testMarkSupportedIsTrue() {
        assertTrue(stream.markSupported(), "FastByteArrayInputStream should report that mark is supported");
    }

    @Test
    void testMarkSetsMarkAtCurrentPosition() {
        // Read the first two bytes – position should now be 2
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // Mark the current position (readLimit is ignored)
        stream.mark(0);
        // Read two more bytes
        assertEquals(30, stream.read());
        assertEquals(40, stream.read());
        // Reset should bring us back to the marked position (byte value 30)
        stream.reset();
        assertEquals(30, stream.read(), "After reset, the stream should return the byte that was read right after the mark");
    }

    @Test
    void testMarkAtStartOfStream() {
        // Mark immediately – position is 0
        // arbitrary readLimit, ignored by implementation
        stream.mark(123);
        // Read a byte, then reset
        assertEquals(10, stream.read());
        stream.reset();
        // The next read must return the same first byte again
        assertEquals(10, stream.read(), "Reset without having read any bytes after the mark should return to the start");
    }

    @Test
    void testResetWithoutExplicitMarkFallsBackToStart() {
        // Do not call mark(); just read a byte
        assertEquals(10, stream.read());
        // Reset should revert to the default mark value (0)
        stream.reset();
        // The next read should again return the first byte
        assertEquals(10, stream.read(), "When no explicit mark is set, reset should revert to the beginning of the stream");
    }

    @Test
    void testMarkDoesNotAffectAvailableCount() {
        // Initial available count
        int before = stream.available();
        // Read a couple of bytes and mark
        stream.read();
        stream.read();
        stream.mark(0);
        // Available should reflect the number of remaining bytes, not the mark
        int after = stream.available();
        assertEquals(before - 2, after, "Mark operation must not change the number of bytes reported as available");
    }

    @Test
    void testMarkIgnoresReadLimitParameter() {
        // Read one byte, then mark with a large read limit
        // position = 1
        stream.read();
        // readLimit is ignored
        stream.mark(9999);
        // Read next byte, reset, and ensure we are back at position 1
        assertEquals(20, stream.read());
        stream.reset();
        assertEquals(20, stream.read(), "The readLimit argument to mark should have no effect on reset behavior");
    }

    @ParameterizedTest(name = "[{index}] readLimit={0}")
    @MethodSource("markAtStartProvider")
    void testMarkAtStartWithVariousReadLimits(MarkScenario scenario) {
        // Mark before any read operation
        stream.mark(scenario.readLimit);
        // Read a byte, then reset
        assertEquals(10, stream.read(), "First read after mark should be the first byte");
        stream.reset();
        // After reset we must obtain the same first byte again
        assertEquals(10, stream.read(), scenario.expected);
    }

    @ParameterizedTest(name = "[{index}] bytesRead={0}, readLimit={1}")
    @MethodSource("markAfterReadProvider")
    void testMarkAfterReadingSomeBytes(MarkAfterReadScenario scenario) {
        // Read the requested number of bytes to advance the position
        for (int i = 0; i < scenario.bytesRead; i++) {
            // we ignore the actual values here
            stream.read();
        }
        // Mark at the current position
        stream.mark(scenario.readLimit);
        // If we are not already at EOF, read one more byte to have something to reset to
        int nextByte = stream.read();
        // Reset should bring us back to the marked position
        stream.reset();
        // After reset the next read must yield the same byte we just read (or -1 at EOF)
        assertEquals(nextByte, stream.read(), scenario.expected);
    }

    /**
     * Verify that {@code mark()} works correctly after a {@code skip()} operation.
     */
    @Test
    void testMarkAfterSkippingBytes() {
        // Skip 4 bytes (position becomes 4)
        long skipped = stream.skip(4);
        assertEquals(4, skipped, "skip(4) should advance the cursor by 4 bytes");
        // Mark with an arbitrary readLimit
        stream.mark(100);
        // The next byte after the mark should be the 5th element (value 50)
        assertEquals(50, stream.read(), "Byte after skip should be 50");
        // Reset should bring us back to position 4 (the 5th byte)
        stream.reset();
        assertEquals(50, stream.read(), "After reset, the stream should return the same byte that follows the mark");
    }

    /**
     * Verify marking when the stream is already at EOF.
     */
    @Test
    void testMarkAtEndOfStream() throws Exception {
        // Exhaust the stream
        while (stream.read() != -1) {
            // consume all bytes
        }
        // At this point pos == length (EOF)
        assertEquals(-1, stream.read(), "Stream should be at EOF");
        // Mark at EOF
        stream.mark(1);
        // Reset should keep us at EOF; another read must still return -1
        stream.reset();
        assertEquals(-1, stream.read(), "Reset after marking at EOF should leave the cursor at EOF (read returns -1)");
    }

    /**
     * Multiple consecutive calls to {@code mark()} should overwrite the previous mark.
     */
    @Test
    void testMultipleConsecutiveMarksOverwritePrevious() {
        // Read two bytes, first mark
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // stores position 2
        stream.mark(5);
        // Read three more bytes
        assertEquals(30, stream.read());
        assertEquals(40, stream.read());
        assertEquals(50, stream.read());
        // Second mark at position 5 (EOF)
        stream.mark(5);
        // Reset should go to the most recent mark (position 5)
        stream.reset();
        // Since we are at EOF, read must return -1
        assertEquals(-1, stream.read(), "After resetting to the latest mark (EOF), further reads should return -1");
    }

    /**
     * The implementation should ignore a negative {@code readLimit}.
     */
    @Test
    void testMarkWithNegativeReadLimit() {
        // Read one byte to move cursor to position 1
        assertEquals(10, stream.read());
        // Mark with a negative limit
        stream.mark(-42);
        // Read the next byte, reset, and verify we are back at position 1
        assertEquals(20, stream.read());
        stream.reset();
        assertEquals(20, stream.read(), "Negative readLimit must be ignored; reset should return to the position when mark was called");
    }

    /**
     * Reset without any prior explicit {@code mark()} should revert to the implicit
     * start‑of‑stream mark (position 0).
     */
    @Test
    void testResetWithoutPriorMarkUsesImplicitStartMark() {
        // No explicit mark; just read a byte
        assertEquals(10, stream.read());
        // Reset should bring us back to the start (position 0)
        stream.reset();
        assertEquals(10, stream.read(), "When reset() is called without a prior mark, it should revert to the beginning of the stream");
    }

    /**
     * Verify that calling {@code mark()} after {@code close()} does not throw
     * and that {@code reset()} still works with the previously stored mark.
     */
    @Test
    void testMarkAfterStreamClosed() throws Exception {
        // Read a couple of bytes and set a mark
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // store position 2
        stream.mark(10);
        // Close the stream
        stream.close();
        // According to the implementation, close does nothing; mark should still be allowed
        assertDoesNotThrow(() -> stream.mark(10), "mark() after close() must not throw");
        // Read another byte (still possible because close is a no‑op)
        assertEquals(30, stream.read());
        // Reset should bring us back to the previously stored mark (position 2)
        stream.reset();
        assertEquals(30, stream.read(), "After reset following a close, the stream should return the byte at the stored mark");
    }
}
