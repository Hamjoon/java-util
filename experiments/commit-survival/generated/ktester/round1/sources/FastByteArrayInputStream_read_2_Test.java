package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
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
        // Initialise a fresh stream for each test; individual tests may replace this.
        sourceData = new byte[] { 10, 20, 30, 40, 50 };
        stream = new FastByteArrayInputStream(sourceData);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release references – not strictly required but keeps the pattern clear.
        stream = null;
        sourceData = null;
    }

    private static Stream<Arguments> nullBufferProvider() {
        return Stream.of(Arguments.of("When the destination byte array is null the method must throw NullPointerException before any other checks.", null, 0, 1, NullPointerException.class));
    }

    private static Stream<Arguments> readByteArrayInputStreamProvider() {
        return Stream.of(// 1) Reading zero bytes should return 0 without modifying the stream position.
        Arguments.of("Reading zero bytes should return 0 without modifying the stream position.", new byte[10], 0, 0, 0), // 2) Read exactly the remaining bytes (3) into a valid slice of the destination array.
        //    The underlying stream has 5 bytes; we request len=3 starting at off=2.
        Arguments.of("Read exactly the remaining bytes (3) into a valid slice of the destination array.", new byte[5], 2, 3, 3), // 3) Read fewer bytes than requested when the stream has fewer bytes left.
        //    After reading 3 bytes we request 2 more – only 2 are left, so we get 2.
        Arguments.of("Read fewer bytes than requested when the stream has fewer bytes left; ensure correct copy and position update.", new byte[4], 1, 2, 2), // 4) Negative offset should trigger IndexOutOfBoundsException.
        Arguments.of("Negative offset should trigger IndexOutOfBoundsException.", new byte[3], -1, 2, IndexOutOfBoundsException.class), // 5) Negative length should trigger IndexOutOfBoundsException.
        Arguments.of("Negative length should trigger IndexOutOfBoundsException.", new byte[3], 1, -2, IndexOutOfBoundsException.class), // 6) Length exceeding array bounds (off + len > b.length) should trigger IndexOutOfBoundsException.
        Arguments.of("Length exceeding array bounds (off + len > b.length) should trigger IndexOutOfBoundsException.", new byte[3], 2, 2, IndexOutOfBoundsException.class), // 7) When the stream is exhausted (pos >= count) the method must return -1.
        Arguments.of("When the stream is exhausted (pos >= count) the method must return -1.", new byte[5], 0, 5, -1), // 8) Requested length larger than available bytes; method should truncate to available and return the actual number read.
        Arguments.of("Requested length larger than available bytes; method should truncate to available and return the actual number read.", new byte[5], 0, 10, 5));
    }

    @Test
    void readFullBuffer() {
        byte[] target = new byte[5];
        int bytesRead = stream.read(target, 0, target.length);
        assertEquals(5, bytesRead, "Should read the whole buffer");
        assertArrayEquals(sourceData, target, "Target buffer must contain the source data");
    }

    @Test
    void readWithOffsetAndLength() {
        // extra space to verify offset handling
        byte[] target = new byte[7];
        int bytesRead = stream.read(target, 2, 3);
        assertEquals(3, bytesRead);
        // Expected layout: [0,0,10,20,30,0,0]
        byte[] expected = new byte[] { 0, 0, 10, 20, 30, 0, 0 };
        assertArrayEquals(expected, target);
    }

    @Test
    void readWhenRequestedLengthExceedsAvailable() {
        // Request more bytes than are left in the stream.
        byte[] target = new byte[10];
        int bytesRead = stream.read(target, 0, target.length);
        assertEquals(sourceData.length, bytesRead);
        // Verify that only the first 5 positions are filled.
        for (int i = 0; i < sourceData.length; i++) {
            assertEquals(sourceData[i], target[i]);
        }
        for (int i = sourceData.length; i < target.length; i++) {
            assertEquals(0, target[i], "Bytes beyond the read length should remain untouched (default 0)");
        }
    }

    @Test
    void readZeroLengthReturnsZero() {
        byte[] target = new byte[3];
        int bytesRead = stream.read(target, 1, 0);
        assertEquals(0, bytesRead, "Reading zero bytes must return 0");
        // Buffer must stay unchanged (all zeros by default)
        assertArrayEquals(new byte[3], target);
    }

    @Test
    void readAfterEndOfStreamReturnsMinusOne() {
        // Exhaust the stream first.
        byte[] discard = new byte[5];
        assertEquals(5, stream.read(discard, 0, discard.length));
        // Subsequent read should signal EOF.
        int result = stream.read(discard, 0, discard.length);
        assertEquals(-1, result);
    }

    @Test
    void readNullArrayThrowsNPE() {
        assertThrows(NullPointerException.class, () -> stream.read(null, 0, 1));
    }

    @Test
    void readWithNegativeOffsetThrowsIOOBE() {
        byte[] target = new byte[4];
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(target, -1, 2));
    }

    @Test
    void readWithNegativeLengthThrowsIOOBE() {
        byte[] target = new byte[4];
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(target, 0, -2));
    }

    @Test
    void readWithOffsetLengthBeyondArrayBoundsThrowsIOOBE() {
        byte[] target = new byte[4];
        // off + len = 5 > target.length (4)
        assertThrows(IndexOutOfBoundsException.class, () -> stream.read(target, 2, 3));
    }

    @Test
    void consecutivePartialReadsAccumulateCorrectly() {
        byte[] target = new byte[5];
        int first = stream.read(target, 0, 2);
        assertEquals(2, first);
        int second = stream.read(target, 2, 2);
        assertEquals(2, second);
        int third = stream.read(target, 4, 1);
        assertEquals(1, third);
        assertArrayEquals(sourceData, target);
    }

    /**
     * Tests that verify the method throws the correct exception when the
     * destination buffer is {@code null}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullBufferProvider")
    void readNullBufferThrows(String description, byte[] b, int off, int len, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> stream.read(b, off, len), description);
    }

    /**
     * Parameterised test covering the bulk of the JSON cases under the
     * {@code ReadByteArrayInputStream} group.
     *
     * <p>Each argument contains:
     * <ul>
     *   <li>a description (used as the test name),</li>
     *   <li>the destination byte array,</li>
     *   <li>offset, length,</li>
     *   <li>the expected result – either an {@code int} (return value) or an exception class.</li>
     * </ul>
     * </p>
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("readByteArrayInputStreamProvider")
    void readByteArrayInputStream(String description, byte[] b, int off, int len, Object expected) {
        // Prepare the stream state for the special "-1" case (exhausted stream)
        if (expected instanceof Integer && ((Integer) expected) == -1) {
            // Exhaust the stream once before the actual call.
            stream.read(new byte[5], 0, 5);
        }
        if (expected instanceof Class) {
            // Expected an exception.
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> stream.read(b, off, len), description);
        } else {
            // Expected a normal integer return value.
            int expectedInt = (Integer) expected;
            int actual = stream.read(b, off, len);
            assertEquals(expectedInt, actual, description);
            // For successful reads, also verify that the bytes were copied correctly.
            if (actual > 0) {
                // Build the expected view of the destination array.
                byte[] expectedArray = new byte[b.length];
                System.arraycopy(sourceData, 0, expectedArray, off, Math.min(actual, sourceData.length));
                assertArrayEquals(expectedArray, b, "Destination array content must match the source data for successful reads.");
            }
        }
    }
}
