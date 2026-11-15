package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

// Mockito not actually used but kept from template
@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_mark_Test {

    private FastByteArrayInputStream stream;

    private static final byte[] SAMPLE_DATA = { 10, 20, 30, 40, 50 };

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh stream for each test to avoid cross‑test interference
        stream = new FastByteArrayInputStream(SAMPLE_DATA.clone());
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the stream is closed after each test – close is harmless for this class
        try {
            stream.close();
        } catch (Exception ignored) {
        }
    }

    private static Stream<Arguments> provideMarkScenarios() {
        return Stream.of(// Scenario 1: mark at start (readLimit = 0)
        Arguments.of(0, // no reads before mark
        new int[] {}, // reads after mark
        new int[] { 10, 20 }, // after reset we should see 10 again
        10), // Scenario 2: mark after three bytes (readLimit = 5)
        Arguments.of(5, // reads before mark
        new int[] { 10, 20, 30 }, // reads after mark
        new int[] { 40, 50 }, // after reset we should see 40 again
        40), // Scenario 3: negative readLimit (readLimit = -10)
        Arguments.of(-10, // reads before mark
        new int[] { 10 }, // reads after mark
        new int[] { 20, 30 }, // after reset we should see 20 again
        20), // Scenario 4: huge readLimit (Integer.MAX_VALUE)
        Arguments.of(Integer.MAX_VALUE, // reads before mark
        new int[] { 10, 20 }, // reads after mark
        new int[] { 30 }, // after reset we should see 30 again
        30));
    }

    @Test
    void markSupportedShouldReturnTrue() {
        assertTrue(stream.markSupported(), "FastByteArrayInputStream should support mark/reset");
    }

    @Test
    void markSetsCurrentPositionAndResetReturnsToIt() {
        // Read first two bytes
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // Mark at current position (which should be index 2)
        stream.mark(0);
        // Read next two bytes
        assertEquals(30, stream.read());
        assertEquals(40, stream.read());
        // Reset should bring us back to the marked position (index 2)
        stream.reset();
        // The next read must return the byte that was at the mark (30)
        assertEquals(30, stream.read());
    }

    @Test
    void resetWithoutPriorMarkReturnsToStart() {
        // No explicit mark call – default mark is 0 (start of stream)
        // advance one byte
        assertEquals(10, stream.read());
        // should go back to start
        stream.reset();
        assertEquals(10, stream.read(), "Reset without prior mark should return to start of stream");
    }

    @Test
    void markAfterSkipAndResetWorksCorrectly() {
        // Skip first two bytes
        assertEquals(2L, stream.skip(2));
        // Position is now at byte 30
        // readLimit is ignored
        stream.mark(5);
        // Read a byte, then reset
        assertEquals(30, stream.read());
        stream.reset();
        // After reset we should read the same byte again
        assertEquals(30, stream.read());
    }

    @Test
    void markIgnoresReadLimitParameter() {
        // Mark with a large read limit
        stream.mark(1000);
        // Read a byte and reset
        assertEquals(10, stream.read());
        stream.reset();
        assertEquals(10, stream.read());
        // Mark with zero read limit
        stream.mark(0);
        assertEquals(20, stream.read());
        stream.reset();
        assertEquals(20, stream.read());
    }

    @Test
    void multipleMarksOverwritePreviousMark() {
        // First mark at start
        stream.mark(0);
        // move to index 1
        assertEquals(10, stream.read());
        // Second mark at index 1
        stream.mark(0);
        // move to index 2
        assertEquals(20, stream.read());
        // should go back to index 1 (second mark)
        stream.reset();
        assertEquals(20, stream.read());
    }

    /**
     * Verify that calling mark(0) at the very beginning of the stream
     * records position 0 and that reset() returns to this start position.
     */
    @Test
    void markAtStartAndResetRestoresToStart() {
        // pos == 0 initially
        stream.mark(0);
        // read a few bytes
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // reset should bring us back to the beginning
        stream.reset();
        assertEquals(10, stream.read(), "After reset the first byte must be the same as before");
    }

    /**
     * Mark after three bytes have been consumed and verify that reset()
     * returns to the marked offset (pos == 3) regardless of the readLimit.
     */
    @Test
    void markAfterThreeBytesAndReset() {
        // consume three bytes
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        assertEquals(30, stream.read());
        // mark at position 3 with readLimit = 5
        stream.mark(5);
        // read two more bytes
        assertEquals(40, stream.read());
        assertEquals(50, stream.read());
        // reset should bring us back to position 3 (byte value 40)
        stream.reset();
        assertEquals(40, stream.read(), "Reset should return to the byte that was read after the mark");
    }

    /**
     * Pass a negative readLimit; the implementation should ignore the argument
     * and simply store the current position.
     */
    @Test
    void markWithNegativeReadLimitIsIgnored() {
        // advance one byte to make the test a bit more interesting
        assertEquals(10, stream.read());
        // mark with a negative limit
        stream.mark(-10);
        // read next byte and reset
        assertEquals(20, stream.read());
        stream.reset();
        // after reset we must see the same byte again
        assertEquals(20, stream.read());
    }

    /**
     * Use Integer.MAX_VALUE as readLimit; it must not affect the stored mark.
     */
    @Test
    void markWithMaxIntReadLimitIsIgnored() {
        // advance two bytes
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // mark with Integer.MAX_VALUE
        stream.mark(Integer.MAX_VALUE);
        // read next byte
        assertEquals(30, stream.read());
        // reset should bring us back to the position after the two initial reads (byte 30)
        stream.reset();
        assertEquals(30, stream.read());
    }

    /**
     * Verify that a readLimit smaller than the number of bytes read after the mark
     * does not restrict reset behavior.
     */
    @Test
    void smallReadLimitDoesNotRestrictReset() {
        // read two bytes, pos = 2
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // mark with readLimit = 1 (smaller than the following reads)
        stream.mark(1);
        // read three more bytes (pos = 5, EOF)
        assertEquals(30, stream.read());
        assertEquals(40, stream.read());
        assertEquals(50, stream.read());
        // reset should bring us back to pos = 2 (byte 30)
        stream.reset();
        assertEquals(30, stream.read());
    }

    /**
     * Mark when the stream is already at EOF; reset should keep the stream at EOF.
     */
    @Test
    void markAtEofAndResetKeepsEof() {
        // consume all bytes
        for (int i = 0; i < SAMPLE_DATA.length; i++) {
            assertEquals(SAMPLE_DATA[i] & 0xFF, stream.read());
        }
        assertEquals(-1, stream.read(), "Stream should be at EOF");
        // mark at EOF
        stream.mark(100);
        // reset should not move the position before EOF
        stream.reset();
        assertEquals(-1, stream.read(), "After reset at EOF, read() must still return -1");
    }

    /**
     * Verify that a later call to mark overwrites the previous mark.
     */
    @Test
    void laterMarkOverwritesEarlierMark() {
        // first mark at start (pos = 0)
        stream.mark(0);
        // read one byte (pos = 1)
        assertEquals(10, stream.read());
        // second mark at pos = 1
        stream.mark(0);
        // read two more bytes (pos = 3)
        assertEquals(20, stream.read());
        assertEquals(30, stream.read());
        // reset should go back to the second mark (pos = 1)
        stream.reset();
        assertEquals(20, stream.read(), "After reset we should see the byte that follows the second mark");
    }

    /**
     * Ensure that reset works after a skip operation; the position should be restored
     * to the original mark.
     */
    @Test
    void markBeforeSkipAndResetRestoresOriginalPosition() {
        // mark at start
        stream.mark(0);
        // skip two bytes (pos = 2)
        assertEquals(2L, stream.skip(2));
        // reset should bring us back to start (pos = 0)
        stream.reset();
        assertEquals(10, stream.read(), "After reset following a skip, the first byte must be returned");
    }

    /**
     * Verify that reset works even after the stream has been closed. The class does not
     * enforce a closed‑state check, so reset should still reposition the stream.
     */
    @Test
    void resetAfterCloseWorks() throws Exception {
        // read one byte (pos = 1)
        assertEquals(10, stream.read());
        // mark at pos = 1
        stream.mark(10);
        // close the stream
        stream.close();
        // reset should not throw and should reposition to the mark
        stream.reset();
        // the next read must be the second byte of the original array
        assertEquals(20, stream.read());
    }

    /**
     * Validate behavior on an empty underlying byte array.
     */
    @Test
    void markResetOnEmptyArray() {
        FastByteArrayInputStream emptyStream = new FastByteArrayInputStream(new byte[0]);
        // mark at position 0 (the only possible position)
        emptyStream.mark(0);
        // reset should keep us at position 0
        emptyStream.reset();
        // any read must return -1 immediately
        assertEquals(-1, emptyStream.read(), "Reading from an empty stream must return -1");
        // also verify that subsequent reset does not change this
        emptyStream.reset();
        assertEquals(-1, emptyStream.read());
    }

    /**
     * Parameterized test that runs a few representative scenarios
     * (start‑mark, mid‑stream‑mark, negative limit, max int limit).
     */
    @ParameterizedTest(name = "{index}: readLimit={0}")
    @MethodSource("provideMarkScenarios")
    void parameterizedMarkBehavior(int readLimit, int[] readsBeforeMark, int[] readsAfterMark, int expectedByteAfterReset) {
        // Advance the stream according to readsBeforeMark
        for (int i = 0; i < readsBeforeMark.length; i++) {
            assertEquals(readsBeforeMark[i] & 0xFF, stream.read(), "Pre‑mark read #" + i);
        }
        // Mark the current position
        stream.mark(readLimit);
        // Advance the stream according to readsAfterMark
        for (int i = 0; i < readsAfterMark.length; i++) {
            assertEquals(readsAfterMark[i] & 0xFF, stream.read(), "Post‑mark read #" + i);
        }
        // Reset and verify the next byte matches expectation
        stream.reset();
        assertEquals(expectedByteAfterReset & 0xFF, stream.read(), "Byte after reset should match the byte that was first read after the mark");
    }
}
