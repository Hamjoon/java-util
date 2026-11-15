package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.io.IOException;
import java.util.stream.Stream;

class FastByteArrayInputStream_read_Test {

    private FastByteArrayInputStream stream;

    private static final byte[] SAMPLE_DATA = { 10, 20, 30, 40, 50 };

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh stream for each test to guarantee isolation
        stream = new FastByteArrayInputStream(SAMPLE_DATA.clone());
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the stream; FastByteArrayInputStream's close is a no‑op but we call it for completeness
        stream.close();
    }

    /**
     * Supplies test data for the basic/edge‑case, zero‑byte, two‑byte and
     * full‑range groups.  Each {@link Arguments} instance contains:
     * <ul>
     *   <li>a byte array to initialise the stream</li>
     *   <li>the expected integer result of the next {@code read()}</li>
     *   <li>a description used in the assertion message</li>
     * </ul>
     */
    private static Stream<Arguments> readBasicAndEdgeCases() {
        return Stream.of(// --- FastByteArrayInputStream.read() - basic and edge cases
        Arguments.of(new byte[] { (byte) 0x7F }, 127, "0x7F should be read as 127"), Arguments.of(new byte[] { (byte) 0xFF, (byte) 0x80, 0x00 }, 255, "0xFF -> 255"), Arguments.of(new byte[] { (byte) 0xFF, (byte) 0x80, 0x00 }, 128, "0x80 -> 128"), Arguments.of(new byte[] { (byte) 0xFF, (byte) 0x80, 0x00 }, 0, "0x00 -> 0"), // --- FastByteArrayInputStream.read() - zero byte handling
        Arguments.of(new byte[] { 0x00 }, 0, "single zero byte should be read as 0"), // --- FastByteArrayInputStream.read() - two‑byte mixed values
        Arguments.of(new byte[] { 0x01, (byte) 0xFE }, 1, "0x01 -> 1"), Arguments.of(new byte[] { 0x01, (byte) 0xFE }, 254, "0xFE -> 254"), // --- FastByteArrayInputStream.read() - sampled full unsigned range
        Arguments.of(new byte[] { 0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFE, (byte) 0xFF }, 0, "0x00 -> 0"), Arguments.of(new byte[] { 0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFE, (byte) 0xFF }, 1, "0x01 -> 1"), Arguments.of(new byte[] { 0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFE, (byte) 0xFF }, 127, "0x7F -> 127"), Arguments.of(new byte[] { 0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFE, (byte) 0xFF }, 128, "0x80 -> 128"), Arguments.of(new byte[] { 0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFE, (byte) 0xFF }, 254, "0xFE -> 254"), Arguments.of(new byte[] { 0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFE, (byte) 0xFF }, 255, "0xFF -> 255"));
    }

    /**
     * Supplies the sequence of expected values after a stream has been
     * exhausted, to verify that subsequent reads return {@code -1}.
     */
    private static Stream<Arguments> readExhaustionCases() {
        return Stream.of(// Single‑byte stream
        Arguments.of(new byte[] { (byte) 0x7F }, new int[] { 127, -1 }), // Three‑byte stream (FF,80,00)
        Arguments.of(new byte[] { (byte) 0xFF, (byte) 0x80, 0x00 }, new int[] { 255, 128, 0, -1 }), // Zero‑byte stream
        Arguments.of(new byte[] {}, new int[] { -1 }));
    }

    @Test
    void readReturnsFirstByte() {
        int first = stream.read();
        assertEquals(10, first, "The first call to read() should return the first byte value");
    }

    @Test
    void readSequentiallyReturnsAllBytes() {
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        assertEquals(30, stream.read());
        assertEquals(40, stream.read());
        assertEquals(50, stream.read());
    }

    @Test
    void readAfterExhaustingStreamReturnsMinusOne() {
        // Exhaust the stream
        for (int i = 0; i < SAMPLE_DATA.length; i++) {
            assertNotEquals(-1, stream.read());
        }
        // Now the stream is at EOF
        assertEquals(-1, stream.read(), "read() should return -1 when the stream is exhausted");
    }

    @Test
    void readFromEmptyStreamImmediatelyReturnsMinusOne() {
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        assertEquals(-1, empty.read(), "Empty stream should return -1 on first read");
    }

    @Test
    void readAfterSkipReturnsCorrectByte() {
        long skipped = stream.skip(2);
        assertEquals(2L, skipped, "skip(2) should skip exactly two bytes");
        assertEquals(30, stream.read(), "After skipping two bytes, the next read should return the third byte");
    }

    @Test
    void readAfterMarkAndResetReturnsMarkedByte() {
        // Read first two bytes
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // Mark the current position (third byte)
        stream.mark(0);
        // Read next two bytes
        assertEquals(30, stream.read());
        assertEquals(40, stream.read());
        // Reset back to the mark (third byte)
        stream.reset();
        // The next read should again return the third byte (30)
        assertEquals(30, stream.read());
    }

    /**
     * Executes the {@link #readBasicAndEdgeCases()} data set.
     *
     * <p>Each test creates a fresh {@code FastByteArrayInputStream} from the
     * supplied byte array, invokes {@code read()} once and asserts the
     * returned unsigned value.</p>
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("readBasicAndEdgeCases")
    void readReturnsExpectedUnsignedValue(byte[] data, int expected, String description) {
        FastByteArrayInputStream s = new FastByteArrayInputStream(data);
        int actual = s.read();
        assertEquals(expected, actual, description);
        // Clean up – close is a no‑op but keeps the pattern consistent
        try {
            s.close();
        } catch (IOException ignored) {
        }
    }

    @ParameterizedTest(name = "[{index}] Exhaustion test for array {0}")
    @MethodSource("readExhaustionCases")
    void readReturnsMinusOneAfterExhaustion(byte[] data, int[] expectedSequence) {
        FastByteArrayInputStream s = new FastByteArrayInputStream(data);
        for (int expected : expectedSequence) {
            int actual = s.read();
            assertEquals(expected, actual, () -> "Expected " + expected + " but got " + actual + " for array " + java.util.Arrays.toString(data));
        }
    }

    @Test
    void readWithNullBufferThrowsNullPointerException() {
        // Constructing with a null buffer should cause a NPE when read() dereferences it.
        FastByteArrayInputStream s = new FastByteArrayInputStream(null);
        assertThrows(NullPointerException.class, s::read, "Calling read() on a stream constructed with a null buffer should throw NullPointerException");
    }

    @Test
    void readWithInvalidLengthThrowsArrayIndexOutOfBoundsException() {
        // Create a buffer of length 1 but request a length of 2 via the 3‑arg constructor.
        byte[] buf = new byte[] { 0x01 };
        // The FastByteArrayInputStream class mirrors ByteArrayInputStream, so we assume this constructor exists.
        FastByteArrayInputStream s = new FastByteArrayInputStream(buf, 0, 2);
        // The first read will try to access index 1, which is out of bounds.
        assertThrows(ArrayIndexOutOfBoundsException.class, s::read, "Reading from a stream whose declared length exceeds the backing array should throw ArrayIndexOutOfBoundsException");
    }
}
