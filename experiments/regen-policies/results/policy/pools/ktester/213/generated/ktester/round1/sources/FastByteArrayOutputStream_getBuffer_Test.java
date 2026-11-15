package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_getBuffer_Test {

    private FastByteArrayOutputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test.
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – not strictly required.
        stream = null;
    }

    /**
     * Helper that uses reflection to obtain the private {@code buf} field.
     * This is only used to assert that {@code getBuffer()} returns a copy,
     * not to test internal implementation details.
     */
    private static byte[] getInternalBufferViaReflection(FastByteArrayOutputStream stream) {
        try {
            java.lang.reflect.Field f = FastByteArrayOutputStream.class.getDeclaredField("buf");
            f.setAccessible(true);
            return (byte[]) f.get(stream);
        } catch (Exception e) {
            throw new RuntimeException("Unable to access internal buffer for test verification", e);
        }
    }

    @Test
    void getBufferReturnsEmptyArrayWhenNoDataWritten() {
        // GIVEN – a newly constructed stream (handled in @BeforeEach)
        // WHEN
        byte[] buffer = stream.getBuffer();
        // THEN
        assertNotNull(buffer, "Buffer should never be null");
        assertEquals(0, buffer.length, "Buffer length should be zero for an empty stream");
    }

    @Test
    void getBufferReturnsCopyOfWrittenData() {
        // GIVEN
        byte[] data = "HelloWorld".getBytes();
        // write all bytes
        stream.write(data, 0, data.length);
        // WHEN
        byte[] buffer = stream.getBuffer();
        // THEN
        assertArrayEquals(data, buffer, "Buffer should contain the same bytes that were written");
        // Verify that the returned array is a copy, not the internal buffer reference
        assertNotSame(buffer, getInternalBufferViaReflection(stream), "Returned buffer must be a copy, not the internal array");
    }

    @Test
    void getBufferIsIndependentOfSubsequentWrites() {
        // GIVEN
        stream.write(new byte[] { 1, 2, 3 });
        byte[] firstCopy = stream.getBuffer();
        // WHEN – write more data after the first copy
        stream.write(new byte[] { 4, 5 });
        byte[] secondCopy = stream.getBuffer();
        // THEN
        assertArrayEquals(new byte[] { 1, 2, 3 }, firstCopy, "First copy must remain unchanged after later writes");
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, secondCopy, "Second copy must reflect all data written up to that point");
    }

    @Test
    void getBufferAfterSingleByteWrite() {
        // write(int) with value 0x5A (90)
        stream.write(0x5A);
        byte[] buffer = stream.getBuffer();
        assertArrayEquals(new byte[] { (byte) 0x5A }, buffer, "Buffer should contain exactly the single written byte [90]");
    }

    @Test
    void getBufferAfterWriteWithOffsetAndLength() {
        byte[] source = { 1, 2, 3, 4, 5 };
        // writes 2,3,4
        stream.write(source, 1, 3);
        assertArrayEquals(new byte[] { 2, 3, 4 }, stream.getBuffer(), "Only the selected slice should be stored");
    }

    @Test
    void getBufferAfterWriteBytesWholeArray() {
        byte[] data = { 9, 8, 7 };
        stream.writeBytes(data);
        assertArrayEquals(data, stream.getBuffer(), "writeBytes should store the whole array");
    }

    @Test
    void getBufferAfterWriting100SequentialBytes() {
        byte[] data = new byte[100];
        for (int i = 0; i < 100; i++) {
            data[i] = (byte) i;
        }
        stream.write(data, 0, data.length);
        assertArrayEquals(data, stream.getBuffer(), "Buffer must contain the 100 sequential bytes after expansion");
    }

    @Test
    void getBufferAfterReset() {
        stream.write(new byte[] { 1, 2, 3, 4 });
        stream.reset();
        assertEquals(0, stream.getBuffer().length, "After reset the buffer should be empty");
    }

    @Test
    void defensiveCopyModificationDoesNotAffectInternalState() {
        stream.write(new byte[] { 1, 2, 3 });
        byte[] first = stream.getBuffer();
        // modify the returned array
        first[0] = 9;
        // request again
        byte[] second = stream.getBuffer();
        assertArrayEquals(new byte[] { 1, 2, 3 }, second, "Modifications to the returned array must not affect the internal buffer");
    }

    @Test
    void multipleSequentialWritesProduceCorrectOrder() {
        // write(int)
        stream.write(5);
        // writeBytes
        stream.writeBytes(new byte[] { 6, 7, 8 });
        // write(byte[],off,len)
        stream.write(new byte[] { 9, 10, 11, 12 }, 0, 4);
        assertArrayEquals(new byte[] { 5, 6, 7, 8, 9, 10, 11, 12 }, stream.getBuffer(), "All writes must be concatenated in the order performed");
    }

    @Test
    void zeroLengthWriteDoesNotChangeBuffer() {
        byte[] source = { 1, 2, 3 };
        // len = 0
        stream.write(source, 1, 0);
        assertEquals(0, stream.getBuffer().length, "Zero‑length write must leave the buffer empty");
    }

    @Test
    void writeIntValueOutsideByteRange() {
        // 300 & 0xFF = 44
        stream.write(300);
        assertArrayEquals(new byte[] { 44 }, stream.getBuffer(), "Only low‑order 8 bits should be stored");
        stream.reset();
        // -1 & 0xFF = 255 (0xFF)
        stream.write(-1);
        assertArrayEquals(new byte[] { (byte) 0xFF }, stream.getBuffer(), "Low‑order bits of -1 should be 255");
    }

    @Test
    void writeEmptyArrayViaWriteBytesDoesNotChangeBuffer() {
        stream.writeBytes(new byte[0]);
        assertEquals(0, stream.getBuffer().length, "Writing an empty array should not modify the stream");
    }

    @Test
    void writeAfterResetWithNewData() {
        stream.write(new byte[] { 1, 2, 3, 4, 5 });
        stream.reset();
        stream.writeBytes(new byte[] { 7, 8, 9 });
        assertArrayEquals(new byte[] { 7, 8, 9 }, stream.getBuffer(), "After reset only the new data should be present");
    }

    @Test
    void fullArrayWriteUsingWriteByteArray() {
        byte[] data = { 10, 20, 30, 40 };
        stream.write(data, 0, data.length);
        assertArrayEquals(data, stream.getBuffer(), "Full‑array write must preserve the exact order");
    }

    @Test
    void multipleSmallWritesTriggeringExpansions() {
        for (int i = 0; i < 10; i++) {
            stream.write(5);
        }
        byte[] expected = new byte[10];
        Arrays.fill(expected, (byte) 5);
        assertArrayEquals(expected, stream.getBuffer(), "Ten successive write(5) calls must result in ten 5's");
    }

    @Test
    void repeatedGetBufferCallsReturnFreshCopies() {
        stream.write(new byte[] { 1, 2, 3 });
        byte[] first = stream.getBuffer();
        byte[] second = stream.getBuffer();
        assertArrayEquals(first, second, "Consecutive getBuffer calls must return equal content");
        assertNotSame(first, second, "Each call must return a fresh defensive copy");
    }

    @Test
    void getBufferWhenInternalBufIsNullThrowsNPE() throws Exception {
        // Use reflection to set the private field 'buf' to null
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        bufField.setAccessible(true);
        bufField.set(stream, null);
        // Also set count to a non‑zero value to make Arrays.copyOf attempt to copy
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.setInt(stream, 1);
        assertThrows(NullPointerException.class, stream::getBuffer, "Calling getBuffer with internal buf == null must throw NPE");
    }
}
