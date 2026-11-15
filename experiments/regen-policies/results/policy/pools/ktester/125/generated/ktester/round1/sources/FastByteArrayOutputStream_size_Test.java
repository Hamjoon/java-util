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
class FastByteArrayOutputStream_size_Test {

    /* --------------------------------------------------------------------- *
     *  Helper class to hold parameterized arguments                         *
     * --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final byte[] data;

        final int expectedSize;

        final String description;

        ArgumentsHolder(byte[] data, int expectedSize, String description) {
            this.data = data;
            this.expectedSize = expectedSize;
            this.description = description;
        }
    }

    /**
     * Supplies data for the {@code write(byte[])} related test cases.
     */
    private static Stream<ArgumentsHolder> byteArrayWriteProvider() {
        return Stream.of(// Multiple bytes write using byte array (5 bytes)
        new ArgumentsHolder(new byte[] { 1, 2, 3, 4, 5 }, 5, "After writing a 5‑byte array with write(byte[]) the size should equal the array length (5)."), // Write zero‑length array (0 bytes)
        new ArgumentsHolder(new byte[0], 0, "Writing an empty byte array must not change the size; it remains 0 for a fresh instance."), // Large data write (1024 bytes)
        new ArgumentsHolder(new byte[1024], 1024, "Writing a 1024‑byte array verifies that size correctly handles larger buffers."));
    }

    /**
     * Verify that a newly created stream reports a size of {@code 0}.
     */
    @Test
    void sizeIsZeroOnNewStream() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // then
        assertEquals(0, stream.size(), "A newly instantiated FastByteArrayOutputStream should have size 0");
    }

    /**
     * Verify that writing a single byte increments the size to {@code 1}.
     */
    @Test
    void sizeIncrementsAfterSingleByteWrite() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // when
        // ASCII 'A'
        stream.write(0x41);
        // then
        assertEquals(1, stream.size(), "Size should be 1 after writing a single byte");
    }

    /**
     * Verify that writing a byte array updates the size correctly.
     */
    @Test
    void sizeReflectsByteArrayWrite() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] data = "Hello".getBytes();
        // when
        stream.write(data, 0, data.length);
        // then
        assertEquals(data.length, stream.size(), "Size should match the number of bytes written from the array");
    }

    /**
     * Verify that {@code reset()} brings the size back to {@code 0}.
     */
    @Test
    void sizeIsZeroAfterReset() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        stream.write(0x41);
        stream.write(0x42);
        assertEquals(2, stream.size(), "Pre‑condition: size should be 2 before reset");
        // when
        stream.reset();
        // then
        assertEquals(0, stream.size(), "Size should be 0 after calling reset()");
    }

    /**
     * Verify that calling {@code close()} does not alter the size.
     */
    @Test
    void sizeUnaffectedByClose() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        stream.write(0x41);
        stream.write(0x42);
        int expectedSize = stream.size();
        // when
        assertDoesNotThrow(stream::close, "Close should not throw an exception");
        // then
        assertEquals(expectedSize, stream.size(), "Size should remain unchanged after close()");
    }

    /**
     * Verify that the size reported after multiple write operations accumulates correctly.
     */
    @Test
    void sizeAccumulatesAcrossMultipleWrites() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // when
        // 1 byte
        stream.write(0x41);
        // 2 bytes
        stream.write(new byte[] { 0x42, 0x43 });
        // 3 bytes
        stream.write("DEF".getBytes(), 0, 3);
        // then
        assertEquals(6, stream.size(), "Size should be the sum of all bytes written (1+2+3 = 6)");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("byteArrayWriteProvider")
    void sizeAfterWriteByteArray(ArgumentsHolder holder) throws IOException {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // Using OutputStream.write(byte[]) which delegates to write(byte[],0,len)
        stream.write(holder.data);
        assertEquals(holder.expectedSize, stream.size(), holder.description);
    }

    /**
     * Test write(byte[], off, len) with specific offset/length.
     */
    @Test
    void sizeAfterWriteWithOffsetAndLength() throws IOException {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // content irrelevant; just length matters
        byte[] source = new byte[7];
        // off = 2, len = 3 => should increase size by 3
        stream.write(source, 2, 3);
        assertEquals(3, stream.size(), "Calling write(byte[], off, len) with off=2 and len=3 on a 7‑byte source should increase size by 3.");
    }

    /**
     * Verify that after writing data and then resetting, size returns to zero.
     */
    @Test
    void sizeAfterWriteThenReset() throws IOException {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        stream.write("some data".getBytes());
        assertTrue(stream.size() > 0, "Pre‑condition: size should be > 0 after writing.");
        stream.reset();
        assertEquals(0, stream.size(), "After writing some data, calling reset() clears the buffer; subsequent size() returns 0.");
    }

    /**
     * Verify that size after close remains unchanged.
     */
    @Test
    void sizeAfterCloseRemainsUnchanged() throws IOException {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] payload = { 10, 20, 30, 40 };
        stream.write(payload);
        int expected = payload.length;
        // should not affect size
        stream.close();
        assertEquals(expected, stream.size(), "Calling close() does not affect the internal count; size after close should still reflect previously written bytes.");
    }

    /**
     * Verify that the convenience method {@code writeBytes(byte[])} updates the size correctly.
     */
    @Test
    void sizeAfterWriteBytesMethod() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] data = { 9, 8, 7 };
        stream.writeBytes(data);
        assertEquals(data.length, stream.size(), "The convenience method writeBytes(byte[]) should increase size by the length of the supplied array.");
    }

    /**
     * Verify that calling {@code toByteArray()} does not modify the internal size.
     */
    @Test
    void sizeUnchangedByToByteArray() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // 6 bytes
        byte[] data = "ABCDEF".getBytes();
        stream.write(data, 0, data.length);
        int sizeBefore = stream.size();
        // should be a copy
        byte[] copy = stream.toByteArray();
        assertArrayEquals(data, copy, "toByteArray should return a copy of the written bytes.");
        assertEquals(sizeBefore, stream.size(), "Calling toByteArray() returns a copy but does not modify the stream; size remains the number of bytes written.");
    }
}
