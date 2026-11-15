package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_getBuffer_Test {

    private FastByteArrayOutputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test.
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent.
        stream = null;
    }

    // -----------------------------------------------------------------
    private static Stream<byte[]> largeDataProvider() {
        // Example provider for future parameterized large‑data tests
        return Stream.of(new byte[0], new byte[1024], new byte[10_000]);
    }

    @Test
    void getBuffer_returnsEmptyArrayWhenNoDataWritten() {
        // given – a newly created stream (handled in @BeforeEach)
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertNotNull(buffer, "Buffer should never be null");
        assertEquals(0, buffer.length, "Buffer length must be zero for an empty stream");
    }

    @Test
    void getBuffer_containsWrittenBytesInCorrectOrder() {
        // given
        // 6 bytes
        byte[] data = "OpenAI".getBytes();
        // write whole array
        stream.write(data, 0, data.length);
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertArrayEquals(data, buffer, "Buffer must contain exactly the bytes that were written");
        assertEquals(data.length, buffer.length, "Buffer length must match number of written bytes");
    }

    @Test
    void getBuffer_isIndependentCopy() {
        // given
        byte[] original = "Immutable".getBytes();
        stream.write(original);
        // when
        byte[] buffer = stream.getBuffer();
        // mutate the returned array
        for (int i = 0; i < buffer.length; i++) {
            // invert bits
            buffer[i] = (byte) (buffer[i] ^ 0xFF);
        }
        // then – the stream's internal state must remain unchanged
        // fresh copy from the stream
        byte[] secondCall = stream.getBuffer();
        assertArrayEquals(original, secondCall, "Modifying the array returned by getBuffer() must not affect the stream's internal buffer");
    }

    @Test
    void getBuffer_reflectsPartialWritesAfterReset() {
        // given
        // 5 bytes
        stream.write("Hello".getBytes());
        // clear the stream
        stream.reset();
        // 5 bytes again
        stream.write("World".getBytes());
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertArrayEquals("World".getBytes(), buffer, "After a reset, getBuffer() should reflect only the data written after the reset");
    }

    @Test
    void getBuffer_doesNotExposeInternalArrayReference() {
        // given
        stream.write(new byte[] { 1, 2, 3, 4, 5 });
        // when
        byte[] buffer = stream.getBuffer();
        // attempt to change the internal array via reflection would be illegal,
        // but we can verify that the returned reference is not the same as the
        // internal one by checking that the stream's subsequent writes do not
        // affect the previously obtained buffer.
        // extend the stream
        stream.write(new byte[] { 9, 9, 9 });
        // then
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, buffer, "Previously obtained buffer must remain unchanged after further writes");
    }

    @ParameterizedTest(name = "single byte {0}")
    @ValueSource(ints = { 42 })
    void getBuffer_containsSingleByteWritten(int value) {
        // given
        stream.write(value);
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertArrayEquals(new byte[] { (byte) value }, buffer, "Buffer must contain exactly the byte that was written");
    }

    @Test
    void getBuffer_containsMultipleBytesLessThanInternalCapacity() {
        // given
        byte[] data = new byte[] { 10, 20, 30, 40, 50 };
        // write whole array
        stream.write(data, 0, data.length);
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertArrayEquals(data, buffer, "Buffer must contain exactly the bytes that were written");
        assertEquals(data.length, buffer.length, "Buffer length must match the number of written bytes");
    }

    @Test
    void getBuffer_afterCloseStillReturnsWrittenData() throws IOException {
        // given
        byte[] data = { 1, 2, 3 };
        stream.write(data);
        // close should not clear the buffer
        stream.close();
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertArrayEquals(data, buffer, "Closing the stream must not alter the data returned by getBuffer()");
    }

    @Test
    void getBuffer_afterWriteBytesMethod() {
        // given
        byte[] data = { 9, 10, 11 };
        stream.writeBytes(data);
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertArrayEquals(data, buffer, "writeBytes should store the exact bytes and getBuffer must return them");
    }

    @Test
    void getBuffer_writeWithOffsetAndLength() {
        // given
        byte[] source = { 0, 1, 2, 3, 4, 5 };
        int off = 2;
        // expects {2,3,4}
        int len = 3;
        stream.write(source, off, len);
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertArrayEquals(new byte[] { 2, 3, 4 }, buffer, "Only the selected slice should be present in the buffer");
    }

    @Test
    void getBuffer_zeroLengthWriteLeavesBufferEmpty() {
        // given
        byte[] source = { 1, 2, 3 };
        // len == 0
        stream.write(source, 0, 0);
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertEquals(0, buffer.length, "Zero‑length write must not change the buffer");
    }

    @Test
    void getBuffer_largeDataCausesBufferGrowth() {
        // given – generate 10 000 bytes (larger than any default capacity)
        byte[] large = new byte[10_000];
        for (int i = 0; i < large.length; i++) {
            large[i] = (byte) (i & 0xFF);
        }
        stream.write(large, 0, large.length);
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertArrayEquals(large, buffer, "getBuffer must return the full data even after internal buffer growth");
        assertEquals(large.length, buffer.length, "Returned buffer length must match the amount of data written");
    }

    @Test
    void getBuffer_writeAfterResetOnlyReturnsNewData() {
        // given
        stream.write(new byte[] { 1, 2, 3 });
        stream.reset();
        stream.write(new byte[] { 4, 5 });
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertArrayEquals(new byte[] { 4, 5 }, buffer, "After reset, only data written after the reset should be present");
    }

    @Test
    void getBuffer_multipleResetsResultInEmptyBuffer() {
        // given
        stream.write(new byte[] { 9, 9, 9 });
        stream.reset();
        stream.write(new byte[] { 8, 8 });
        stream.reset();
        // when
        byte[] buffer = stream.getBuffer();
        // then
        assertEquals(0, buffer.length, "After multiple resets the buffer must be empty");
    }

    @Test
    void getBuffer_nullInternalBufferThrowsNPE() throws Exception {
        // given – use reflection to set the private field `buf` to null
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        bufField.setAccessible(true);
        bufField.set(stream, null);
        // when / then
        assertThrows(NullPointerException.class, () -> stream.getBuffer(), "If internal buf is null, getBuffer() should propagate NullPointerException");
    }
}
