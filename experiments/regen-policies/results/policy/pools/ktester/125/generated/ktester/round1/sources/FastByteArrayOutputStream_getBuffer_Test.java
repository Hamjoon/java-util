package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_getBuffer_Test {

    private FastByteArrayOutputStream outputStream;

    private static class NormalCase {

        final byte[] buffer;

        final int count;

        final byte[] expected;

        NormalCase(byte[] buffer, int count, byte[] expected) {
            this.buffer = buffer;
            this.count = count;
            this.expected = expected;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor (initial capacity 32) for each test.
        outputStream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the stream is closed after each test to release any resources.
        // FastByteArrayOutputStream#close does not throw, but we guard against unexpected exceptions.
        assertDoesNotThrow(outputStream::close);
    }

    private static Stream<NormalCase> normalCases() {
        return Stream.of(// Internal buffer length 10, count = 0 → empty result
        new NormalCase(new byte[10], 0, new byte[0]), // Buffer = {1,2,3,4,5,6,7,8}, count = 5 → first five bytes
        new NormalCase(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, 5, new byte[] { 1, 2, 3, 4, 5 }), // Buffer = {1,2,3,4,5,6,7,8}, count = 8 → full copy
        new NormalCase(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }, 8, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }));
    }

    /**
     * Directly sets the private {@code buf} and {@code count} fields of the
     * {@link FastByteArrayOutputStream} instance using reflection.
     *
     * @param buf   the byte array to assign (may be {@code null})
     * @param count the count value to assign
     * @throws Exception if reflection fails
     */
    private void setInternalState(byte[] buf, int count) throws Exception {
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        bufField.setAccessible(true);
        countField.setAccessible(true);
        bufField.set(outputStream, buf);
        countField.setInt(outputStream, count);
    }

    @Test
    void testGetBufferOnEmptyStream() {
        byte[] buffer = outputStream.getBuffer();
        assertNotNull(buffer, "Buffer should never be null");
        assertEquals(0, buffer.length, "Buffer length should be zero for an empty stream");
    }

    @Test
    void testGetBufferAfterWritingBytes() {
        // 10 bytes
        byte[] data = "HelloWorld".getBytes();
        outputStream.write(data, 0, data.length);
        byte[] buffer = outputStream.getBuffer();
        assertArrayEquals(data, buffer, "Buffer should contain exactly the written bytes");
        assertEquals(data.length, buffer.length, "Buffer length should match the number of written bytes");
    }

    @Test
    void testGetBufferIsIndependentCopy() {
        // 8 bytes
        byte[] data = "CopyTest".getBytes();
        outputStream.write(data);
        byte[] buffer = outputStream.getBuffer();
        // Mutate the returned array
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (byte) (buffer[i] ^ 0xFF);
        }
        // The stream's internal state must remain unchanged.
        byte[] bufferAfterMutation = outputStream.getBuffer();
        assertArrayEquals(data, bufferAfterMutation, "Modifying the returned buffer must not affect the stream's internal data");
    }

    @Test
    void testGetBufferAfterReset() {
        outputStream.write("SomeData".getBytes());
        outputStream.reset();
        byte[] buffer = outputStream.getBuffer();
        assertNotNull(buffer, "Buffer should never be null even after reset");
        assertEquals(0, buffer.length, "Buffer should be empty after reset");
    }

    @Test
    void testGetBufferAfterBufferGrowth() {
        // Write more than the default initial capacity (32) to force internal buffer expansion.
        byte[] largeData = new byte[100];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) i;
        }
        outputStream.write(largeData);
        byte[] buffer = outputStream.getBuffer();
        assertEquals(largeData.length, buffer.length, "Buffer length should reflect the total written bytes");
        assertArrayEquals(largeData, buffer, "Buffer content should match the data written after growth");
    }

    @ParameterizedTest(name = "[{index}] count={1}, expectedLength={2}")
    @MethodSource("normalCases")
    void testNormalBehavior(NormalCase tc) throws Exception {
        setInternalState(tc.buffer, tc.count);
        byte[] result = outputStream.getBuffer();
        assertArrayEquals(tc.expected, result, "getBuffer() should return a copy of the first 'count' bytes");
    }

    @Test
    void testGetBufferWhenInternalBufferIsNull() throws Exception {
        setInternalState(null, 0);
        assertThrows(NullPointerException.class, () -> outputStream.getBuffer(), "When internal buf is null, Arrays.copyOf should throw NPE");
    }

    @Test
    void testGetBufferWhenCountGreaterThanBufferLength() throws Exception {
        // buf length = 3, count = 5 → copyOf pads with zeros
        setInternalState(new byte[] { 9, 10, 11 }, 5);
        byte[] result = outputStream.getBuffer();
        assertArrayEquals(new byte[] { 9, 10, 11, 0, 0 }, result, "Arrays.copyOf should pad with zeros when count > buf.length");
    }

    @Test
    void testGetBufferWithNegativeCount() throws Exception {
        setInternalState(new byte[] { 1, 2, 3 }, -1);
        assertThrows(NegativeArraySizeException.class, () -> outputStream.getBuffer(), "Negative count should cause NegativeArraySizeException");
    }

    @Test
    void testGetBufferWithZeroLengthInternalBuffer() throws Exception {
        setInternalState(new byte[0], 0);
        byte[] result = outputStream.getBuffer();
        assertNotNull(result);
        assertEquals(0, result.length, "Zero‑length internal buffer should yield empty result");
    }

    @Test
    void testDistinctCopiesOnSuccessiveCalls() throws Exception {
        // Prepare internal state {100,101,102}
        setInternalState(new byte[] { 100, 101, 102, 0, 0 }, 3);
        byte[] first = outputStream.getBuffer();
        // mutate first copy
        first[0] = 0;
        byte[] second = outputStream.getBuffer();
        assertArrayEquals(new byte[] { 100, 101, 102 }, second, "Second call must return original data, proving a new defensive copy");
    }

    @Test
    void testGetBufferAfterClose() throws Exception {
        byte[] data = { 9, 8, 7, 6 };
        outputStream.write(data);
        // close should not alter internal buffer
        outputStream.close();
        byte[] buffer = outputStream.getBuffer();
        assertArrayEquals(data, buffer, "Buffer after close must still contain previously written data");
    }
}
