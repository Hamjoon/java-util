package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
class FastByteArrayOutputStream_size_Test {

    /**
     * Supplies arguments for {@link #sizeAfterWritesParameterized}.
     *
     * The data mirrors the JSON test cases supplied in the task description.
     */
    private static Stream<Arguments> provideSizeAfterWritesCases() {
        return Stream.of(// Case: write a byte array of length 3
        Arguments.of("After writing a byte array of length 3 using write(byte[]), size() should return 3.", new byte[] { 1, 2, 3 }, null, null, null, false, 3), // Case: write one single byte then a one‑element array
        Arguments.of("Write one byte via write(int) then another via write(byte[] of length 1); size() should be 2.", new byte[] { 10 }, 5, null, null, false, 2), // Case: writing an empty array does not change size
        Arguments.of("Writing an empty byte array should not change size; remains 0.", new byte[] {}, null, null, null, false, 0), // Case: write with offset=2, len=5
        Arguments.of("Using write(byte[] b, int off, int len) with off=2, len=5 should increase size by 5.", new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 }, null, 2, 5, false, 5), // Case: after reset, size must be 0 regardless of previous writes
        Arguments.of("After reset() is called, size() should return 0 regardless of previous writes.", new byte[] { 100, 101, 102 }, null, null, null, true, 0));
    }

    @Test
    void sizeIsZeroOnNewStream() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // then
        assertEquals(0, stream.size(), "Newly created stream should have size 0");
    }

    @Test
    void sizeIncrementsWhenWritingSingleBytes() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // when
        // 'A'
        stream.write(0x41);
        // 'B'
        stream.write(0x42);
        // then
        assertEquals(2, stream.size(), "Size should reflect two written bytes");
    }

    @Test
    void sizeReflectsWriteByteArrayWithOffsetAndLength() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // length 10
        byte[] data = "HelloWorld".getBytes();
        // when
        // writes "lloWo"
        stream.write(data, 2, 5);
        // then
        assertEquals(5, stream.size(), "Size should equal the number of bytes written via offset/len");
        assertArrayEquals("lloWo".getBytes(), stream.toByteArray(), "Internal buffer should contain exactly the written slice");
    }

    @Test
    void sizeMatchesLengthAfterWritingWholeArray() throws IOException {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] payload = "Payload".getBytes();
        // when
        stream.write(payload);
        // then
        assertEquals(payload.length, stream.size(), "Size should equal the length of the whole array written");
        assertArrayEquals(payload, stream.toByteArray(), "toByteArray should return the exact data written");
    }

    @Test
    void sizeResetsToZeroAfterReset() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        stream.write(0x7F);
        stream.write(0x80);
        assertEquals(2, stream.size(), "Pre‑condition: size should be 2 before reset");
        // when
        stream.reset();
        // then
        assertEquals(0, stream.size(), "Size must be 0 after reset");
    }

    @Test
    void sizeUnchangedAfterClose() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        stream.write(1);
        stream.write(2);
        assertEquals(2, stream.size(), "Pre‑condition: size should be 2 before close");
        // when / then
        assertDoesNotThrow(stream::close, "close() should not throw");
        assertEquals(2, stream.size(), "Size should remain unchanged after close because close does not alter the buffer");
    }

    @Test
    void sizeWorksWhenUsedInCompressionUtility() throws IOException {
        // This mirrors the example usage of size() in a compression helper.
        // given
        FastByteArrayOutputStream original = new FastByteArrayOutputStream();
        original.write("DataToCompress".getBytes());
        FastByteArrayOutputStream compressed = new FastByteArrayOutputStream();
        // when
        // Simulate the utility method without pulling in external compression classes.
        // We only need to verify that size() can be called safely.
        int originalSize = original.size();
        assertTrue(originalSize > 0, "Original stream should contain data");
        // then
        // No actual compression performed; just ensure size() is accessible and returns a sensible value.
        assertEquals("DataToCompress".getBytes().length, originalSize);
    }

    /**
     * Executes the collection of JSON‑driven scenarios that verify {@code size()}
     * after various write operations (including reset).
     *
     * @param description human‑readable description of the scenario
     * @param bytes       byte array to write (may be {@code null})
     * @param singleByte a single int byte to write before the array (may be {@code null})
     * @param offset      offset for the {@code write(byte[],off,len)} call (may be {@code null})
     * @param len         length for the {@code write(byte[],off,len)} call (may be {@code null})
     * @param reset       whether {@code reset()} should be invoked before checking size
     * @param expected    the expected value returned by {@code size()}
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSizeAfterWritesCases")
    void sizeAfterWritesParameterized(String description, byte[] bytes, Integer singleByte, Integer offset, Integer len, boolean reset, int expected) {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // 1️⃣ optional single‑byte write
        if (singleByte != null) {
            stream.write(singleByte);
        }
        // 2️⃣ optional array write (full or partial)
        if (bytes != null) {
            if (offset != null && len != null) {
                stream.write(bytes, offset, len);
            } else {
                stream.write(bytes);
            }
        }
        // 3️⃣ optional reset before size check
        if (reset) {
            stream.reset();
        }
        // 4️⃣ assertion
        assertEquals(expected, stream.size(), description);
    }
}
