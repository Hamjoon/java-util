package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
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
        // Default data – individual tests can replace it with their own arrays.
        data = new byte[] { 10, 20, 30, 40, 50 };
        stream = new FastByteArrayInputStream(data);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release references – helpful for IDEs / static analysis.
        stream = null;
        data = null;
    }

    private static Stream<byte[]> noMarkDataProvider() {
        return Stream.of(new byte[] { 10, 20, 30, 40, 50 }, new byte[] { 5, 15, 25, 35 }, new byte[] { -1, 0, 1 });
    }

    private static Stream<byte[]> markAndReadDataProvider() {
        return Stream.of(new byte[] { 10, 20, 30, 40, 50 }, new byte[] { 1, 2, 3, 4, 5, 6 });
    }

    private static Stream<byte[]> multipleMarksDataProvider() {
        return Stream.of(new byte[] { 5, 15, 25, 35 }, new byte[] { 9, 19, 29, 39, 49 });
    }

    @Test
    void resetWithoutPriorMarkShouldReturnToStart() {
        // GIVEN: a stream positioned after reading two bytes
        // pos = 1
        assertEquals(10, stream.read());
        // pos = 2
        assertEquals(20, stream.read());
        // WHEN: reset is invoked without an explicit mark
        stream.reset();
        // THEN: the next read should return the first byte again
        assertEquals(10, stream.read());
        // The available count should be restored to the original length minus one read
        assertEquals(data.length - 1, stream.available());
    }

    @Test
    void resetAfterMarkReturnsToMarkedPosition() {
        // GIVEN: read two bytes, then mark the current position
        // pos = 1
        assertEquals(10, stream.read());
        // pos = 2
        assertEquals(20, stream.read());
        // mark = 2 (parameter ignored)
        stream.mark(0);
        // Read two more bytes to move past the mark
        // pos = 3
        assertEquals(30, stream.read());
        // pos = 4
        assertEquals(40, stream.read());
        // WHEN: reset is called
        stream.reset();
        // THEN: the stream should be positioned at the marked index (value 30)
        assertEquals(30, stream.read());
        // And the available count should reflect the remaining bytes (including the one just read)
        // bytes 40 and 50 remain
        assertEquals(2, stream.available());
    }

    @Test
    void resetCanBeCalledMultipleTimesAfterSameMark() {
        // GIVEN: mark at the start, read a byte, reset, read again
        // mark = 0
        stream.mark(0);
        // pos = 1
        assertEquals(10, stream.read());
        // WHEN: first reset
        stream.reset();
        // THEN: reading again yields the first byte
        assertEquals(10, stream.read());
        // WHEN: second reset without moving the mark
        stream.reset();
        // THEN: still returns the first byte
        assertEquals(10, stream.read());
    }

    @Test
    void resetAfterMarkThenMarkAgainChangesResetTarget() {
        // GIVEN: initial mark at position 1
        // pos = 1
        assertEquals(10, stream.read());
        // mark = 1
        stream.mark(0);
        // Move forward and set a new mark at position 3
        // pos = 2
        assertEquals(20, stream.read());
        // pos = 3
        assertEquals(30, stream.read());
        // new mark = 3
        stream.mark(0);
        // WHEN: reset
        stream.reset();
        // THEN: should return to the most recent mark (value 40)
        assertEquals(40, stream.read());
    }

    @Test
    void resetDoesNotAffectMarkSupportedFlag() {
        // GIVEN: a stream with known mark support
        assertTrue(stream.markSupported());
        // WHEN: reset is called (no prior mark)
        stream.reset();
        // THEN: the flag remains true
        assertTrue(stream.markSupported());
    }

    /**
     * Verify that {@code reset()} works correctly after a {@code skip()} operation.
     *
     * <p>Scenario:
     * <ul>
     *   <li>skip(2) → cursor at index 2 (value 9)</li>
     *   <li>mark() records this position</li>
     *   <li>skip(1) → cursor at index 3 (value 10)</li>
     *   <li>reset() → cursor returns to index 2</li>
     *   <li>next read() should return 9</li>
     * </ul>
     */
    @Test
    void resetAfterSkipOperationRestoresMarkedPosition() throws IOException {
        data = new byte[] { 7, 8, 9, 10, 11 };
        stream = new FastByteArrayInputStream(data);
        // skip to index 2 (value 9)
        assertEquals(2L, stream.skip(2L));
        // mark the current position (index 2)
        stream.mark(0);
        // skip one more byte to index 3 (value 10)
        assertEquals(1L, stream.skip(1L));
        // reset should bring us back to index 2
        stream.reset();
        // the next read must be the byte 9
        assertEquals(9, stream.read());
    }

    /**
     * Validate that resetting to a mark positioned at the end of the stream
     * leaves the stream at EOF (read() returns -1).
     */
    @Test
    void resetWhenMarkAtEndOfStreamKeepsEof() throws IOException {
        data = new byte[] { 1, 2 };
        stream = new FastByteArrayInputStream(data);
        // consume all bytes
        assertEquals(1, stream.read());
        assertEquals(2, stream.read());
        // EOF
        assertEquals(-1, stream.read());
        // mark at EOF
        stream.mark(0);
        // reset should keep us at EOF
        stream.reset();
        assertEquals(-1, stream.read());
    }

    /**
     * Check that a mark set when the cursor is already at the end behaves like an EOF marker.
     */
    @Test
    void resetAfterReachingEndThenMarkKeepsEof() throws IOException {
        data = new byte[] { 100, 101, 102 };
        stream = new FastByteArrayInputStream(data);
        // skip to the end of the stream
        assertEquals(3L, stream.skip(data.length));
        // mark at EOF
        stream.mark(0);
        // reset should not move the cursor backwards
        stream.reset();
        assertEquals(-1, stream.read());
    }

    /**
     * Ensure that {@code reset()} works after the stream has been closed.
     *
     * <p>Scenario:
     * <ul>
     *   <li>read one byte (55)</li>
     *   <li>mark() records position 1</li>
     *   <li>close() the stream</li>
     *   <li>reset() should still reposition to the marked index</li>
     *   <li>next read() returns the second byte (66)</li>
     * </ul>
     */
    @Test
    void resetAfterCloseRestoresMarkedPosition() throws IOException {
        data = new byte[] { 55, 66, 77 };
        stream = new FastByteArrayInputStream(data);
        // read first byte
        assertEquals(55, stream.read());
        // mark at position 1
        stream.mark(0);
        // close the stream (FastByteArrayInputStream's close is a no‑op but we call it for completeness)
        stream.close();
        // reset should still work
        stream.reset();
        // next read should be the second byte
        assertEquals(66, stream.read());
    }

    /**
     * Parameterized test covering the "Reset behavior without prior mark" group.
     *
     * The test data is supplied via {@link #noMarkDataProvider()}.
     */
    @ParameterizedTest(name = "[{index}] bytes={0}")
    @MethodSource("noMarkDataProvider")
    void resetWithoutPriorMarkParameterized(byte[] bytes) {
        stream = new FastByteArrayInputStream(bytes);
        // read all bytes to move the cursor to the end
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(Byte.toUnsignedInt(bytes[i]), stream.read());
        }
        // reset without a prior explicit mark – should go back to start (pos = 0)
        stream.reset();
        // first read after reset must be the first byte again
        assertEquals(Byte.toUnsignedInt(bytes[0]), stream.read());
        // available should be length - 1
        assertEquals(bytes.length - 1, stream.available());
    }

    /**
     * Parameterized test for the "Reset after mark and subsequent read" group.
     *
     * The test data is supplied via {@link #markAndReadDataProvider()}.
     */
    @ParameterizedTest(name = "[{index}] bytes={0}")
    @MethodSource("markAndReadDataProvider")
    void resetAfterMarkAndReadParameterized(byte[] bytes) {
        stream = new FastByteArrayInputStream(bytes);
        // read two bytes
        assertEquals(Byte.toUnsignedInt(bytes[0]), stream.read());
        assertEquals(Byte.toUnsignedInt(bytes[1]), stream.read());
        // mark at position 2
        stream.mark(0);
        // read two more bytes
        assertEquals(Byte.toUnsignedInt(bytes[2]), stream.read());
        assertEquals(Byte.toUnsignedInt(bytes[3]), stream.read());
        // reset should bring us back to the third byte
        stream.reset();
        // next read must be the third byte again
        assertEquals(Byte.toUnsignedInt(bytes[2]), stream.read());
    }

    /**
     * Parameterized test for the "Reset after multiple successive marks" group.
     *
     * Demonstrates that each new {@code mark()} overwrites the previous one.
     */
    @ParameterizedTest(name = "[{index}] bytes={0}")
    @MethodSource("multipleMarksDataProvider")
    void resetAfterMultipleSuccessiveMarksParameterized(byte[] bytes) {
        stream = new FastByteArrayInputStream(bytes);
        // read first byte, then mark (position 1)
        assertEquals(Byte.toUnsignedInt(bytes[0]), stream.read());
        // mark = 1
        stream.mark(0);
        // read second byte, then mark again (position 2)
        assertEquals(Byte.toUnsignedInt(bytes[1]), stream.read());
        // mark = 2
        stream.mark(0);
        // read third byte
        assertEquals(Byte.toUnsignedInt(bytes[2]), stream.read());
        // first reset → back to position 2 (third byte)
        stream.reset();
        assertEquals(Byte.toUnsignedInt(bytes[2]), stream.read());
        // second reset → still at position 2
        stream.reset();
        assertEquals(Byte.toUnsignedInt(bytes[2]), stream.read());
    }
}
