package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_toByteArray_Test {

    /**
     * Shared instance used by several tests – created once for the whole test run.
     */
    private static FastByteArrayOutputStream sharedStream;

    /**
     * Instance created before each test to guarantee isolation.
     */
    private FastByteArrayOutputStream stream;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a stream that can be reused across tests if desired.
        sharedStream = new FastByteArrayOutputStream();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for each test to avoid cross‑test contamination.
        stream = new FastByteArrayOutputStream();
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up; resetting the instance is enough.
        stream.reset();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared instance.
        sharedStream = null;
    }

    // -------------------- Parameterized helper for large data --------------------
    private static Stream<Arguments> provideMixedWriteScenarios() {
        return Stream.of(Arguments.of(new byte[] { 100 }, new byte[] { 1, 2, 3, 4 }, 1, 2, new byte[] { 9, 9 }, new byte[] { 100, 2, 3, 9, 9 }));
    }

    @Test
    void toByteArray_returnsCopyOfWrittenData() {
        // given
        byte[] data = "FastByteArray".getBytes();
        stream.write(data, 0, data.length);
        // when
        byte[] result = stream.toByteArray();
        // then
        assertArrayEquals(data, result, "The returned array should contain exactly the bytes written");
        // Verify that the returned array is a copy, not the internal buffer
        assertNotSame(data, result, "The returned array must be a defensive copy");
    }

    @Test
    void toByteArray_afterResetReturnsEmptyArray() {
        // given
        stream.write("some data".getBytes());
        // clear the buffer
        stream.reset();
        // when
        byte[] result = stream.toByteArray();
        // then
        assertEquals(0, result.length, "After reset the byte array should be empty");
        assertEquals(0, stream.size(), "Size should be zero after reset");
    }

    @Test
    void toByteArray_multipleWritesAccumulatesData() {
        // given
        // 1 byte
        stream.write('A');
        // 2 bytes
        stream.write("BC".getBytes());
        // 2 bytes
        stream.write(new byte[] { 'D', 'E' }, 0, 2);
        // when
        byte[] result = stream.toByteArray();
        // then
        assertArrayEquals(new byte[] { 'A', 'B', 'C', 'D', 'E' }, result);
        assertEquals(5, stream.size(), "Size should reflect total bytes written");
    }

    @Test
    void toByteArray_doesNotAffectSubsequentWrites() {
        // given
        stream.write("first".getBytes());
        // capture current state
        byte[] snapshot = stream.toByteArray();
        // when
        stream.write("second".getBytes());
        // then
        assertArrayEquals("first".getBytes(), snapshot, "Snapshot must remain unchanged");
        assertArrayEquals("firstsecond".getBytes(), stream.toByteArray(), "Subsequent writes should be reflected in later calls");
    }

    @Test
    @DisplayName("Empty stream returns an empty byte array")
    void basic_emptyStream_returnsEmptyArray() {
        byte[] result = stream.toByteArray();
        assertNotNull(result, "Result must not be null");
        assertEquals(0, result.length, "Empty stream should produce empty array");
    }

    @Test
    @DisplayName("write(int) three times produces correct array")
    void basic_writeIntThreeBytes() {
        stream.write(1);
        stream.write(2);
        stream.write(3);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.toByteArray());
    }

    @Test
    @DisplayName("write(byte[], off, len) grows buffer correctly")
    void basic_writeByteArrayLargerThanInitialBuffer() {
        // The default internal buffer size is unknown; we force a large write.
        byte[] large = new byte[100];
        for (int i = 0; i < large.length; i++) large[i] = (byte) i;
        stream.write(large, 0, large.length);
        assertArrayEquals(large, stream.toByteArray());
    }

    @Test
    @DisplayName("reset clears count but not the underlying buffer")
    void basic_resetClearsCount() {
        stream.write(new byte[] { 5, 6, 7, 8 });
        stream.reset();
        byte[] result = stream.toByteArray();
        assertEquals(0, result.length, "After reset the returned array must be empty");
        assertEquals(0, stream.size(), "Size after reset must be zero");
    }

    @Test
    @DisplayName("Returned array is a defensive copy – modifying it does not affect internal buffer")
    void basic_returnedArrayIsCopy() {
        stream.write(new byte[] { 10, 20, 30 });
        byte[] copy = stream.toByteArray();
        // mutate the copy
        copy[0] = 99;
        // internal buffer should stay unchanged
        // public accessor
        byte[] internal = stream.getBuffer();
        assertEquals(10, internal[0], "Internal buffer must remain unchanged after external mutation");
    }

    @Test
    @DisplayName("writeBytes(byte[]) preserves order and values")
    void basic_writeBytesPreservesOrder() {
        stream.writeBytes(new byte[] { 9, 9, 9 });
        assertArrayEquals(new byte[] { 9, 9, 9 }, stream.toByteArray());
    }

    @Test
    @DisplayName("Mixed write methods produce correct ordering")
    void additional_mixedWrites() {
        // write(int)
        stream.write(100);
        // writes 2,3
        stream.write(new byte[] { 1, 2, 3, 4 }, 1, 2);
        stream.writeBytes(new byte[] { 9, 9 });
        assertArrayEquals(new byte[] { 100, 2, 3, 9, 9 }, stream.toByteArray());
    }

    @Test
    @DisplayName("Reset between writes keeps only later data")
    void additional_resetBetweenWrites() {
        stream.write(new byte[] { 1, 2, 3 });
        stream.reset();
        stream.write(new byte[] { 5, 6 });
        assertArrayEquals(new byte[] { 5, 6 }, stream.toByteArray());
    }

    @Test
    @DisplayName("Write exactly 32 bytes (default buffer size) returns exact array")
    void additional_writeExactlyDefaultSize() {
        byte[] data = new byte[32];
        for (int i = 0; i < 32; i++) data[i] = (byte) i;
        stream.write(data);
        assertArrayEquals(data, stream.toByteArray());
        assertEquals(32, stream.size());
    }

    @Test
    @DisplayName("Write 100 bytes (multiple growths) returns correct array")
    void additional_writeHundredBytes() {
        byte[] data = new byte[100];
        for (int i = 0; i < 100; i++) data[i] = (byte) i;
        stream.write(data);
        assertArrayEquals(data, stream.toByteArray());
        assertEquals(100, stream.size());
    }

    @Test
    @DisplayName("Each toByteArray call returns a fresh copy")
    void additional_freshCopyEachCall() {
        stream.write(new byte[] { 1, 2, 3 });
        byte[] first = stream.toByteArray();
        // corrupt the first result
        first[0] = 99;
        byte[] second = stream.toByteArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, second, "Second call must return original data unchanged");
    }

    @Test
    @DisplayName("Zero‑length write does not change content")
    void additional_zeroLengthWrite() {
        stream.write(new byte[] { 1, 2, 3 });
        // zero‑length write
        stream.write(new byte[] { 9, 9, 9 }, 0, 0);
        assertArrayEquals(new byte[] { 1, 2, 3 }, stream.toByteArray());
    }

    @Test
    @DisplayName("Closing the stream does not affect stored data")
    void additional_closeDoesNotAffectData() throws IOException {
        stream.write(new byte[] { 10, 20, 30 });
        // implementation may be a no‑op
        stream.close();
        assertArrayEquals(new byte[] { 10, 20, 30 }, stream.toByteArray(), "Data must remain after close()");
    }

    @Test
    @DisplayName("Negative and boundary byte values are preserved")
    void additional_signedBytesPreserved() {
        stream.write(new byte[] { -1, -128, 0, 127 });
        assertArrayEquals(new byte[] { -1, -128, 0, 127 }, stream.toByteArray());
    }

    @Test
    @DisplayName("toByteArray after close throws NullPointerException (if close nullifies buffer)")
    void exception_toByteArrayAfterClose() throws IOException {
        // The contract in the supplied JSON expects a NPE after close.
        // If the actual implementation does not nullify the buffer, this test will fail,
        // which correctly signals a contract mismatch.
        stream.write(new byte[] { 1, 2, 3 });
        // assume close nullifies internal buffer
        stream.close();
        assertThrows(NullPointerException.class, stream::toByteArray, "Calling toByteArray after close should throw NullPointerException");
    }

    @Test
    @DisplayName("toByteArray throws NullPointerException when internal buffer is manually set to null")
    void exception_internalBufferNullViaReflection() throws Exception {
        // Write something first so that the buffer is allocated.
        stream.write(new byte[] { 1, 2, 3 });
        // Use reflection to set the private field 'buf' to null.
        Field bufField = FastByteArrayOutputStream.class.getDeclaredField("buf");
        bufField.setAccessible(true);
        bufField.set(stream, null);
        assertThrows(NullPointerException.class, stream::toByteArray, "Null internal buffer must cause NullPointerException");
    }

    @Test
    @DisplayName("toByteArray throws NegativeArraySizeException when count is negative")
    void exception_negativeCountViaReflection() throws Exception {
        // Ensure buffer is non‑null.
        stream.write(new byte[] { 0 });
        // Set private 'count' to -1 via reflection.
        Field countField = FastByteArrayOutputStream.class.getDeclaredField("count");
        countField.setAccessible(true);
        countField.setInt(stream, -1);
        assertThrows(NegativeArraySizeException.class, stream::toByteArray, "Negative count must cause NegativeArraySizeException");
    }

    @ParameterizedTest(name = "{index} => intByte={0}, array={1}, off={2}, len={3}, bytes={4}, expected={5}")
    @MethodSource("provideMixedWriteScenarios")
    void parameterized_mixedWrites(byte[] intByte, byte[] arr, int off, int len, byte[] bytes, byte[] expected) {
        // intByte is a single‑element array representing the int to write
        stream.write(intByte[0] & 0xFF);
        stream.write(arr, off, len);
        stream.writeBytes(bytes);
        assertArrayEquals(expected, stream.toByteArray());
    }
}
