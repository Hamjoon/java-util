package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_reset_Test {

    private FastByteArrayInputStream stream;

    private byte[] data;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh stream for each test to guarantee isolation.
        data = new byte[] { 0x01, 0x02, 0x03, 0x04 };
        stream = new FastByteArrayInputStream(data);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release references – helps the GC and makes the intent clear.
        stream = null;
        data = null;
    }

    private static Stream<Arguments> provideResetWithoutPriorMarkScenarios() {
        return Stream.of(// Scenario 1: read two bytes, then reset
        Arguments.of("Read two bytes then reset", (Runnable) () -> {
            // read two bytes
            assertEquals(0x01, stream.read());
            assertEquals(0x02, stream.read());
            // reset
            stream.reset();
        }), // Scenario 2: read whole array, then reset and check available()
        Arguments.of("Read whole array then reset", (Runnable) () -> {
            // consume all bytes
            while (stream.read() != -1) {
                /* no‑op */
            }
            // reset
            stream.reset();
            // available should be the full length again
            assertEquals(4, stream.available(), "available() must report full length after reset");
        }));
    }

    @Test
    void resetWithoutExplicitMarkReturnsToStart() throws Exception {
        // read a couple of bytes so the internal position moves forward
        assertEquals(0x01, stream.read());
        assertEquals(0x02, stream.read());
        // now reset – the default mark is 0, so we should be back at the beginning
        stream.reset();
        // reading again should yield the first byte
        assertEquals(0x01, stream.read());
        // available should reflect the full length again (minus the byte just read)
        assertEquals(3, stream.available());
    }

    @Test
    void resetAfterMarkReturnsToMarkedPosition() throws Exception {
        // set a mark at the current position (0)
        stream.mark(0);
        // advance the stream
        assertEquals(0x01, stream.read());
        assertEquals(0x02, stream.read());
        // reset should bring us back to the mark (position 0)
        stream.reset();
        // the next read must return the first byte again
        assertEquals(0x01, stream.read());
    }

    @Test
    void resetAfterChangingMarkRespectsLatestMark() throws Exception {
        // first mark at position 0
        stream.mark(0);
        // move to position 1
        assertEquals(0x01, stream.read());
        // set a new mark at current position (1)
        stream.mark(0);
        // move to position 2
        assertEquals(0x02, stream.read());
        // move to position 3
        assertEquals(0x03, stream.read());
        // reset should now go back to the second mark (position 1)
        stream.reset();
        // next read must be the byte at index 1
        assertEquals(0x02, stream.read());
    }

    @Test
    void resetDoesNotAlterUnderlyingBuffer() throws Exception {
        stream.mark(0);
        // consume first byte
        stream.read();
        stream.reset();
        // modify the returned array to ensure the stream still reads original data
        byte[] external = new byte[4];
        int read = stream.read(external, 0, external.length);
        assertEquals(4, read);
        assertArrayEquals(data, external);
    }

    @Test
    void resetAfterEndOfStreamKeepsPositionAtMark() throws Exception {
        // read everything to reach end-of-stream
        while (stream.read() != -1) {
            // consume
        }
        // confirm EOF
        assertEquals(-1, stream.read());
        // set a mark now (position is at count)
        stream.mark(0);
        // reset should keep us at the end (still EOF)
        stream.reset();
        assertEquals(-1, stream.read());
    }

    /**
     * Group: Reset without prior mark (default mark = 0)
     * Two scenarios are exercised via a parameterised test:
     *   1) After reading two bytes, reset() should bring us back to the start.
     *   2) After reading the whole array, reset() should restore the full length.
     */
    @ParameterizedTest(name = "{index} => scenario={0}")
    @MethodSource("provideResetWithoutPriorMarkScenarios")
    void resetWithoutPriorMark(String scenario, Runnable actions) {
        // Execute the scenario‑specific actions (reads, asserts, etc.)
        actions.run();
        // After the scenario actions have called reset(), verify that the stream
        // is positioned at the beginning (pos == 0) by reading the first byte.
        assertEquals(0x01, stream.read(), "After reset the first byte must be 0x01");
    }

    /**
     * Group: Reset after setting a custom mark.
     * Verify that reset() returns to the user‑defined mark.
     */
    @Test
    void resetAfterCustomMark() throws Exception {
        // Read first byte, then set a mark at position 1
        assertEquals(0x01, stream.read());
        stream.mark(0);
        // Read next byte
        assertEquals(0x02, stream.read());
        // Reset should bring us back to position 1 (the mark)
        stream.reset();
        // The next read must return the same byte we just read before reset
        assertEquals(0x02, stream.read());
    }

    /**
     * Group: Reset after reading beyond a previously set mark.
     * The mark is set after two bytes have been consumed.
     */
    @Test
    void resetAfterReadingBeyondMark() throws Exception {
        // Consume two bytes
        assertEquals(0x01, stream.read());
        assertEquals(0x02, stream.read());
        // Set the mark at the current position (2)
        stream.mark(0);
        // Read the third byte
        assertEquals(0x03, stream.read());
        // Reset should bring us back to the mark (position 2)
        stream.reset();
        // The next read must return the third byte again
        assertEquals(0x03, stream.read());
    }

    /**
     * Group: Reset after skip() operation.
     */
    @Test
    void resetAfterSkip() throws Exception {
        // Mark at the start
        stream.mark(0);
        // Skip two bytes
        long skipped = stream.skip(2);
        assertEquals(2L, skipped, "skip(2) must advance two positions");
        // Reset should restore the position to the mark (0)
        stream.reset();
        // The next read must return the first byte again
        assertEquals(0x01, stream.read());
    }

    /**
     * Group: Reset when stream is already at the beginning.
     */
    @Test
    void resetAtBeginningIsNoOp() throws Exception {
        // No reads or marks performed; position is already 0
        stream.reset();
        // The first read must still return the first byte
        assertEquals(0x01, stream.read());
    }

    /**
     * Group: Reset after bulk read into a buffer.
     */
    @Test
    void resetAfterBulkRead() throws Exception {
        // Mark at the start
        stream.mark(0);
        // Bulk read two bytes into a buffer
        byte[] buf = new byte[2];
        int readCount = stream.read(buf, 0, buf.length);
        assertEquals(2, readCount, "Bulk read should fill the buffer with two bytes");
        assertArrayEquals(new byte[] { 0x01, 0x02 }, buf, "Buffer content must match the first two bytes");
        // Reset should bring us back to the beginning
        stream.reset();
        // The next read must return the first byte again
        assertEquals(0x01, stream.read());
    }

    /**
     * Group: Reset after reaching EOF with a prior mark.
     */
    @Test
    void resetAfterEofWithPriorMark() throws Exception {
        // Read first byte and set a mark at position 1
        assertEquals(0x01, stream.read());
        stream.mark(0);
        // Consume the rest of the stream until EOF
        while (stream.read() != -1) {
            /* consume */
        }
        // At this point we are at EOF; reset should move back to the mark (position 1)
        stream.reset();
        // available() should now report length‑mark = 3 (bytes remaining)
        assertEquals(3, stream.available(), "available() after reset should reflect remaining bytes");
        // The next read must return the second byte (0x02)
        assertEquals(0x02, stream.read());
    }
}
