package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_reset_Test {

    private FastByteArrayInputStream stream;

    private byte[] data;

    @BeforeEach
    void setupBeforeEach() {
        // Default data used by the original test suite – individual tests can replace it.
        data = new byte[] { 10, 20, 30, 40 };
        stream = new FastByteArrayInputStream(data);
    }

    @AfterEach
    void teardownAfterEach() {
        stream = null;
        data = null;
    }

    /**
     * Helper that creates a new stream with the supplied byte array.
     */
    private FastByteArrayInputStream newStream(byte[] bytes) {
        return new FastByteArrayInputStream(bytes);
    }

    /**
     * Demonstrates how a parameterised test could be driven from the JSON
     * description.  The current suite uses explicit test methods for clarity,
     * but the method source is kept for reference.
     */
    static Stream<Arguments> resetBehaviorProvider() {
        return Stream.of(Arguments.of(new byte[] { 0x01, 0x02, 0x03 }, 2, false, 0, 0x01), Arguments.of(new byte[] { 0x0A, 0x0B, 0x0C, 0x0D, 0x0E }, 3, true, 3, 0x0D));
    }

    @Test
    void resetToDefaultMark() {
        // Read two bytes, then reset without an explicit mark (default mark = 0)
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // should revert to the beginning of the stream
        stream.reset();
        assertEquals(10, stream.read());
    }

    @Test
    void resetAfterExplicitMark() {
        // Mark at the start, read, reset and verify position
        stream.mark(0);
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // back to the marked position (0)
        stream.reset();
        assertEquals(10, stream.read());
        // Mark again after some reads
        stream.mark(0);
        assertEquals(20, stream.read());
        assertEquals(30, stream.read());
        // back to the second mark (position after first byte)
        stream.reset();
        assertEquals(20, stream.read());
    }

    @Test
    void resetAfterSkip() {
        stream.mark(0);
        long skipped = stream.skip(2);
        assertEquals(2, skipped);
        assertEquals(30, stream.read());
        // should return to the mark (position 0)
        stream.reset();
        assertEquals(10, stream.read());
    }

    @Test
    void resetAfterCloseDoesNotAffectBehaviour() throws Exception {
        stream.mark(0);
        // consume one byte
        assertEquals(10, stream.read());
        // close is a no‑op for this implementation
        stream.close();
        // should still reset to the mark
        stream.reset();
        assertEquals(10, stream.read());
    }

    @Test
    void resetDoesNotChangeAvailableBeyondMark() {
        assertEquals(4, stream.available());
        // advance to position 1
        stream.read();
        stream.mark(0);
        // position 2
        stream.read();
        // position 3
        stream.read();
        assertEquals(1, stream.available());
        // back to the mark (position 1)
        stream.reset();
        assertEquals(3, stream.available());
    }

    @Test
    @DisplayName("reset() without prior mark() returns to start of stream")
    void behavior_resetWithoutMark_returnsToStart() {
        // Use a distinct data set to make the expectation crystal clear.
        byte[] bytes = { 0x01, 0x02, 0x03, 0x04 };
        FastByteArrayInputStream s = newStream(bytes);
        assertEquals(0x01, s.read());
        assertEquals(0x02, s.read());
        // no explicit mark – should go back to implicit mark (0)
        s.reset();
        assertEquals(0x01, s.read(), "After reset the next byte must be the first one again");
    }

    @Test
    @DisplayName("reset() after a mark() restores the marked position")
    void behavior_resetAfterMarkRestoresPosition() {
        byte[] bytes = { 0x0A, 0x0B, 0x0C, 0x0D, 0x0E };
        FastByteArrayInputStream s = newStream(bytes);
        // read three bytes -> position 3
        assertEquals(0x0A, s.read());
        assertEquals(0x0B, s.read());
        assertEquals(0x0C, s.read());
        // mark at index 3
        s.mark(0);
        assertEquals(0x0D, s.read());
        assertEquals(0x0E, s.read());
        // should go back to index 3
        s.reset();
        assertEquals(0x0D, s.read(), "Byte after reset must be the one originally at the mark");
    }

    @Test
    @DisplayName("Successive calls to mark() overwrite previous mark")
    void behavior_successiveMarksOverwrite() {
        byte[] bytes = { 0x11, 0x22, 0x33, 0x44, 0x55 };
        FastByteArrayInputStream s = newStream(bytes);
        // first mark at position 2
        // index 0
        s.read();
        // index 1
        s.read();
        // mark = 2
        s.mark(0);
        // index 2
        s.read();
        // second mark at position 3
        // mark = 3
        s.mark(0);
        // index 3
        s.read();
        // should go back to index 3
        s.reset();
        assertEquals(0x44, s.read(), "reset() must use the latest mark");
    }

    @Test
    @DisplayName("reset() after reaching end‑of‑stream without a mark returns to start")
    void behavior_resetAfterEofWithoutMark() {
        byte[] bytes = { 0x5A, 0x5B };
        FastByteArrayInputStream s = newStream(bytes);
        // consume all bytes
        assertEquals(0x5A, s.read());
        assertEquals(0x5B, s.read());
        assertEquals(-1, s.read(), "EOF should be reported");
        // no explicit mark – should revert to 0
        s.reset();
        assertEquals(0x5A, s.read(), "After reset the stream must start from the beginning");
    }

    @Test
    @DisplayName("reset() after close() does not throw and stream remains usable")
    void behavior_resetAfterClose() throws IOException {
        byte[] bytes = { 0x01, 0x02 };
        FastByteArrayInputStream s = newStream(bytes);
        s.mark(0);
        // consume first byte
        s.read();
        // implementation defines close as a no‑op
        s.close();
        // must not throw
        s.reset();
        // The stream should still behave like an open stream (implementation‑specific)
        assertEquals(0x01, s.read(), "After reset the first byte must be readable again");
    }

    @Test
    @DisplayName("mark(0) at position 0 and reset() returns to start")
    void edge_markAtStartAndReset() {
        byte[] bytes = { 0x01, 0x02, 0x03 };
        FastByteArrayInputStream s = newStream(bytes);
        s.mark(0);
        assertEquals(0x01, s.read());
        assertEquals(0x02, s.read());
        s.reset();
        assertEquals(0x01, s.read(), "Reset should bring us back to the start");
    }

    @Test
    @DisplayName("readLimit argument of mark() is ignored by reset()")
    void edge_markReadLimitIgnored() {
        byte[] bytes = { 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F };
        FastByteArrayInputStream s = newStream(bytes);
        // read five bytes -> position 5
        for (int i = 0; i < 5; i++) {
            s.read();
        }
        // readLimit larger than needed
        s.mark(10);
        // position 6 (EOF)
        assertEquals(0x0F, s.read());
        // should go back to position 5
        s.reset();
        assertEquals(0x0F, s.read(), "reset() must ignore readLimit and return to the mark");
    }

    @Test
    @DisplayName("reset() is idempotent when called repeatedly without a new mark")
    void edge_resetIdempotent() {
        byte[] bytes = { 0x01, 0x02, 0x03 };
        FastByteArrayInputStream s = newStream(bytes);
        // index 0
        s.read();
        // mark = 1
        s.mark(0);
        // index 1
        s.read();
        // index 2
        s.read();
        // back to index 1
        s.reset();
        // still at index 1
        s.reset();
        assertEquals(0x02, s.read(), "After two resets the position must still be at the mark");
    }

    @Test
    @DisplayName("reset() on an empty underlying buffer")
    void edge_resetOnEmptyBuffer() {
        byte[] empty = {};
        FastByteArrayInputStream s = newStream(empty);
        // should be a no‑op
        s.reset();
        assertEquals(-1, s.read(), "Reading from an empty stream must return -1");
    }

    @Test
    @DisplayName("reset() after a skip() that moves past the mark")
    void edge_resetAfterSkipBeyondMark() {
        byte[] bytes = { 0x01, 0x02, 0x03, 0x04, 0x05 };
        FastByteArrayInputStream s = newStream(bytes);
        assertEquals(0x01, s.read());
        assertEquals(0x02, s.read());
        // mark at index 2
        s.mark(0);
        // attempt to skip beyond end
        long skipped = s.skip(5);
        assertEquals(3, skipped, "Only three bytes can be skipped");
        // should go back to index 2
        s.reset();
        assertEquals(0x03, s.read(), "After reset we must read the byte that originally followed the mark");
    }

    @Test
    @DisplayName("reset() after close() followed by mark() – no exception, stream stays closed")
    void edge_closeThenMarkThenReset() throws IOException {
        byte[] bytes = { 0x0A, 0x0B };
        FastByteArrayInputStream s = newStream(bytes);
        // consume first byte
        s.read();
        // close (no‑op)
        s.close();
        // should not throw
        s.mark(0);
        // also should not throw
        s.reset();
        // According to the implementation, read after close still works.
        assertEquals(0x0B, s.read(), "Read after close should still return remaining data");
    }

    @Test
    @DisplayName("reset() does not modify the stored mark")
    void edge_resetDoesNotAlterMark() {
        byte[] bytes = { 0x01, 0x02, 0x03, 0x04 };
        FastByteArrayInputStream s = newStream(bytes);
        // index 0
        s.read();
        // mark = 1
        s.mark(0);
        // index 1
        assertEquals(0x02, s.read());
        // index 2
        assertEquals(0x03, s.read());
        // back to index 1
        s.reset();
        // should read the same byte again
        assertEquals(0x02, s.read());
        assertEquals(0x03, s.read(), "Mark value must stay unchanged after reset");
    }

    @ParameterizedTest
    @MethodSource("resetBehaviorProvider")
    @DisplayName("Parameterized reset behaviour (reference)")
    void parameterizedResetBehaviour(byte[] buf, int readsBeforeMark, boolean markBeforeReset, int expectedPosAfterReset, int expectedNextByte) {
        FastByteArrayInputStream s = newStream(buf);
        for (int i = 0; i < readsBeforeMark; i++) {
            s.read();
        }
        if (markBeforeReset) {
            s.mark(0);
        }
        s.reset();
        assertEquals(expectedNextByte, s.read(), "After reset the stream should be positioned at the expected index");
    }
}
