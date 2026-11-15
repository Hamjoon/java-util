package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_toByteArray_Test {

    private FastByteArrayOutputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for each test to guarantee isolation.
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly close the stream – the implementation does not throw,
        // but this mirrors good resource‑management practice.
        assertDoesNotThrow(stream::close);
        stream = null;
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// writeBytes [1,2,3]
        Arguments.of("After writeBytes([1,2,3]) the stream should return exactly those bytes.", (Runnable) () -> {
            stream.writeBytes(new byte[] { 1, 2, 3 });
        }, new byte[] { 1, 2, 3 }), // write int 255 and 0 (note: 255 becomes -1 as signed byte)
        Arguments.of("Writing int values 255 and 0 should be stored as signed bytes -1 and 0.", (Runnable) () -> {
            stream.write(255);
            stream.write(0);
        }, new byte[] { (byte) -1, 0 }), // write 100, reset, writeBytes [9,9]
        Arguments.of("After resetting, previous data is discarded; only post‑reset bytes remain.", (Runnable) () -> {
            stream.write(100);
            stream.reset();
            stream.writeBytes(new byte[] { 9, 9 });
        }, new byte[] { 9, 9 }), // bulk write then single byte write
        Arguments.of("Combination of bulk write and single‑byte write should be correctly represented.", (Runnable) () -> {
            byte[] bulk = new byte[256];
            for (int i = 0; i < bulk.length; i++) bulk[i] = (byte) i;
            stream.writeBytes(bulk);
            stream.write(10);
        }, concatArrays(range(0, 256), new byte[] { 10 })), // write single byte then close
        Arguments.of("Closing the stream does not affect the internal buffer; toByteArray() after close returns the written bytes.", (Runnable) () -> {
            stream.write(0);
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }, new byte[] { 0 }));
    }

    private static Stream<Arguments> invalidWriteArgumentsProvider() {
        return Stream.of(Arguments.of("Negative offset should cause IndexOutOfBoundsException", new byte[] { 1, 2, 3 }, -1, 2), Arguments.of("Length extending beyond array bounds should cause IndexOutOfBoundsException", new byte[] { 1, 2, 3 }, 1, 5));
    }

    /**
     * Concatenates two byte arrays.
     */
    private static byte[] concatArrays(byte[] a, byte[] b) {
        byte[] result = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    /**
     * Generates a byte array with values from {@code start} (inclusive) to {@code end} (exclusive).
     */
    private static byte[] range(int start, int end) {
        byte[] arr = new byte[end - start];
        for (int i = start; i < end; i++) {
            arr[i - start] = (byte) i;
        }
        return arr;
    }

    @Test
    void toByteArrayOnEmptyStreamReturnsEmptyArray() {
        // GIVEN an empty FastByteArrayOutputStream (created in @BeforeEach)
        // WHEN we request the byte array
        byte[] result = stream.toByteArray();
        // THEN the result is an empty array and size remains zero
        assertNotNull(result);
        assertEquals(0, result.length);
        assertEquals(0, stream.size());
    }

    @Test
    void toByteArrayReflectsWrittenData() {
        // GIVEN a stream with known content
        byte[] payload = "JUnit".getBytes();
        stream.write(payload, 0, payload.length);
        // WHEN we retrieve the byte array
        byte[] result = stream.toByteArray();
        // THEN the content matches exactly and size is correct
        assertArrayEquals(payload, result);
        assertEquals(payload.length, stream.size());
    }

    @Test
    void toByteArrayAfterResetReturnsEmptyArray() {
        // GIVEN a stream that has been written to and then reset
        stream.write("data".getBytes());
        stream.reset();
        // WHEN we call toByteArray()
        byte[] result = stream.toByteArray();
        // THEN we get an empty array and size is zero
        assertNotNull(result);
        assertEquals(0, result.length);
        assertEquals(0, stream.size());
    }

    @Test
    void toByteArrayReturnsDefensiveCopy() {
        // GIVEN a stream with some data
        byte[] original = "defensive".getBytes();
        stream.write(original);
        // WHEN we obtain the array and mutate the returned array
        byte[] returned = stream.toByteArray();
        returned[0] = (byte) 'X';
        // THEN the internal state of the stream is unchanged
        assertArrayEquals(original, stream.toByteArray(), "The stream's internal buffer must not be affected by modifications to the returned array.");
    }

    @Test
    void toByteArrayWorksWithCustomInitialSize() {
        // GIVEN a stream created with a larger initial buffer
        FastByteArrayOutputStream custom = new FastByteArrayOutputStream(64);
        byte[] data = "customSize".getBytes();
        custom.write(data);
        // WHEN we request the byte array
        byte[] result = custom.toByteArray();
        // THEN the content matches and size is correct
        assertArrayEquals(data, result);
        assertEquals(data.length, custom.size());
        // Cleanup
        assertDoesNotThrow(custom::close);
    }

    @Test
    void toByteArrayDoesNotExposeUnwrittenPortionOfBuffer() {
        // GIVEN a stream with initial capacity larger than the data written
        FastByteArrayOutputStream large = new FastByteArrayOutputStream(32);
        byte[] data = { 1, 2, 3 };
        large.write(data);
        // WHEN we retrieve the byte array
        byte[] result = large.toByteArray();
        // THEN the length of the returned array equals the number of written bytes,
        //      not the initial capacity (i.e., no trailing zeroes)
        assertEquals(data.length, result.length);
        assertArrayEquals(data, result);
    }

    /**
     * Basic functionality test cases derived from the JSON specification.
     */
    @ParameterizedTest(name = "{index} => description=''{0}''")
    @MethodSource("basicFunctionalityProvider")
    void basicFunctionalityTests(String description, Runnable actions, byte[] expected) {
        // GIVEN a fresh stream (already created in @BeforeEach) and the actions to perform
        actions.run();
        // WHEN we call toByteArray()
        byte[] result = stream.toByteArray();
        // THEN the result must match the expected array
        assertArrayEquals(expected, result, description);
    }

    /**
     * Buffer‑expansion test – writes more bytes than the default initial capacity.
     */
    @Test
    void toByteArrayHandlesBufferExpansion() {
        // GIVEN a stream and a payload larger than the typical default buffer (e.g., > 256 bytes)
        byte[] largePayload = new byte[300];
        for (int i = 0; i < largePayload.length; i++) {
            largePayload[i] = (byte) (i % 256);
        }
        stream.writeBytes(largePayload);
        // WHEN we retrieve the byte array
        byte[] result = stream.toByteArray();
        // THEN the result must contain all written bytes in the correct order
        assertArrayEquals(largePayload, result);
        assertEquals(largePayload.length, stream.size());
    }

    /**
     * Partial write using the three‑argument {@code write} method.
     */
    @Test
    void toByteArrayPartialWriteWithOffsetAndLength() {
        // GIVEN a bulk write followed by a partial write (offset 1, len 3)
        byte[] initial = { 10, 20, 30, 40, 50 };
        stream.writeBytes(initial);
        // writes 20,30,40
        stream.write(initial, 1, 3);
        // WHEN we request the byte array
        byte[] result = stream.toByteArray();
        // THEN the resulting array should contain the initial bulk data plus the selected slice
        byte[] expected = { 10, 20, 30, 40, 50, 20, 30, 40 };
        assertArrayEquals(expected, result);
    }

    /**
     * Immutability test – mutating a previously returned array must not affect subsequent calls.
     */
    @Test
    void toByteArrayReturnedArrayIsImmutableAcrossCalls() {
        // GIVEN a stream with known data
        byte[] data = { 5, 6, 7 };
        stream.writeBytes(data);
        // Capture first array and mutate it
        byte[] firstCapture = stream.toByteArray();
        firstCapture[0] = 99;
        // Capture second array without any further writes
        byte[] secondCapture = stream.toByteArray();
        // THEN the second capture must equal the original data, proving immutability
        assertArrayEquals(data, secondCapture);
        // Also ensure the first mutated array does not equal the stream's internal state
        assertNotEquals(99, secondCapture[0]);
    }

    /**
     * Multiple resets – only data after the last reset should be visible.
     */
    @Test
    void toByteArrayAfterMultipleResetsKeepsOnlyLastData() {
        // GIVEN a series of writes and resets
        stream.writeBytes(new byte[] { 1, 2 });
        stream.reset();
        stream.writeBytes(new byte[] { 3 });
        stream.reset();
        stream.writeBytes(new byte[] { 4, 5 });
        // WHEN we retrieve the byte array
        byte[] result = stream.toByteArray();
        // THEN only the bytes written after the final reset are present
        assertArrayEquals(new byte[] { 4, 5 }, result);
    }

    /**
     * Zero‑length bulk write should be a no‑op.
     */
    @Test
    void toByteArrayZeroLengthBulkWriteHasNoEffect() {
        // GIVEN an empty bulk write followed by a single‑byte write
        stream.writeBytes(new byte[0]);
        stream.write(9);
        // WHEN we request the byte array
        byte[] result = stream.toByteArray();
        // THEN the result contains only the single byte written after the empty bulk write
        assertArrayEquals(new byte[] { 9 }, result);
    }

    /**
     * Invalid offset/length arguments must throw {@link IndexOutOfBoundsException}.
     */
    @ParameterizedTest(name = "{index} => description=''{0}''")
    @MethodSource("invalidWriteArgumentsProvider")
    void toByteArrayInvalidWriteArguments(String description, byte[] source, int off, int len) {
        // GIVEN a fresh stream
        // WHEN we attempt an illegal write, THEN an exception is thrown and the stream stays unchanged
        assertThrows(IndexOutOfBoundsException.class, () -> stream.write(source, off, len), description);
        // The stream should still be empty
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
    }
}
