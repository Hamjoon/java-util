package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

class FastByteArrayOutputStream_reset_Test {

    /**
     * Verify that {@code reset()} clears the internal count, making the stream appear empty.
     */
    @Test
    void testResetClearsData() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] payload = "HelloWorld".getBytes();
        // when – write some data
        stream.write(payload, 0, payload.length);
        // then – size and content reflect the write
        assertEquals(payload.length, stream.size(), "Size should match number of written bytes before reset");
        assertArrayEquals(payload, stream.toByteArray(), "Content should match what was written before reset");
        // when – reset is invoked
        stream.reset();
        // then – stream reports as empty
        assertEquals(0, stream.size(), "Size must be zero after reset");
        assertArrayEquals(new byte[0], stream.toByteArray(), "Byte array must be empty after reset");
    }

    /**
     * After a reset the stream must be reusable – subsequent writes should behave like a fresh instance.
     */
    @Test
    void testResetAllowsReuse() {
        // given
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] first = "First".getBytes();
        byte[] second = "Second".getBytes();
        // when – write first payload and reset
        stream.write(first);
        stream.reset();
        // then – stream is empty
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
        // when – write second payload
        stream.write(second);
        // then – only second payload is present
        assertEquals(second.length, stream.size(), "Size should reflect only the data written after reset");
        assertArrayEquals(second, stream.toByteArray(), "Content should be exactly the data written after reset");
    }

    /**
     * Reset on a newly created (empty) stream must leave it in a valid empty state.
     */
    @Test
    void testResetOnEmptyStream() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // pre‑condition: newly created stream is empty
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
        // when – reset is called
        stream.reset();
        // then – still empty, no exception thrown
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
    }

    /**
     * Calling {@code reset()} repeatedly must keep the stream in the cleared state without throwing.
     */
    @Test
    void testMultipleResetsIdempotent() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // invoke reset several times in a row
        stream.reset();
        stream.reset();
        stream.reset();
        // verify still empty
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
    }

    /**
     * Write a large payload that forces internal buffer growth, reset, then verify that the stream behaves
     * like a fresh instance for subsequent small writes.
     */
    @Test
    void testResetAfterLargeWrite() throws IOException {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // write 1024 bytes (all zero) to trigger buffer expansion
        byte[] large = new byte[1024];
        Arrays.fill(large, (byte) 7);
        stream.write(large);
        assertEquals(1024, stream.size(), "Pre‑reset size should be 1024");
        // reset clears logical count
        stream.reset();
        assertEquals(0, stream.size(), "Size must be zero after reset");
        assertArrayEquals(new byte[0], stream.toByteArray(), "Byte array must be empty after reset");
        // write a few bytes after reset
        byte[] small = { 1, 2, 3 };
        stream.write(small);
        assertEquals(3, stream.size(), "Size after small write should be 3");
        assertArrayEquals(small, stream.toByteArray(), "Content after small write must match the newly written bytes");
    }

    /**
     * Verify that {@code writeTo(OutputStream)} copies the current data to the target stream,
     * and that a subsequent {@code reset()} clears only the source stream.
     */
    @Test
    void testResetAfterWriteTo() throws IOException {
        FastByteArrayOutputStream source = new FastByteArrayOutputStream();
        byte[] data = { 10, 20, 30, 40, 50 };
        // write data to source
        source.write(data);
        assertEquals(5, source.size());
        // copy to a separate ByteArrayOutputStream
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        source.writeTo(target);
        assertArrayEquals(data, target.toByteArray(), "Target stream must contain the bytes written before reset");
        // reset source and verify it is empty
        source.reset();
        assertEquals(0, source.size());
        assertArrayEquals(new byte[0], source.toByteArray());
        // write new data after reset and ensure it does not affect the previously copied target
        byte[] newData = { 9, 9 };
        source.write(newData);
        assertArrayEquals(newData, source.toByteArray(), "Source must contain only the newly written bytes after reset");
        assertArrayEquals(data, target.toByteArray(), "Target should remain unchanged after source reset");
    }

    /**
     * The {@code close()} method is a no‑op for this implementation. After closing,
     * {@code reset()} should still allow the stream to be reused.
     */
    @Test
    void testResetAfterClose() throws IOException {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // close (no‑op)
        stream.close();
        // reset after close must not throw and must leave stream usable
        stream.reset();
        assertEquals(0, stream.size());
        // write after reset/close
        stream.write(new byte[] { 9, 9 });
        assertEquals(2, stream.size());
        assertArrayEquals(new byte[] { 9, 9 }, stream.toByteArray(), "Stream must accept writes after reset following close");
    }

    /**
     * Ensure that {@code reset()} only clears the logical count; the raw buffer content
     * remains unchanged. After reset, a new write should overwrite from index 0.
     */
    @Test
    void testBufferIntegrityViaGetBuffer() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        // write three distinct bytes
        byte[] first = { 5, 6, 7 };
        stream.write(first);
        assertEquals(3, stream.size());
        // capture the underlying buffer snapshot (may be larger than logical size)
        byte[] bufferBeforeReset = stream.getBuffer().clone();
        assertEquals(5, bufferBeforeReset[0]);
        assertEquals(6, bufferBeforeReset[1]);
        assertEquals(7, bufferBeforeReset[2]);
        // reset should not modify the raw buffer
        stream.reset();
        assertEquals(0, stream.size());
        byte[] bufferAfterReset = stream.getBuffer();
        // The first three positions should still hold the old values
        assertEquals(5, bufferAfterReset[0]);
        assertEquals(6, bufferAfterReset[1]);
        assertEquals(7, bufferAfterReset[2]);
        // write a new byte after reset
        stream.write(99);
        assertEquals(1, stream.size());
        assertArrayEquals(new byte[] { 99 }, stream.toByteArray(), "Only the newly written byte should be visible after reset");
        // underlying buffer now has 99 at index 0, but the old values beyond the logical count remain unchanged
        assertEquals(99, stream.getBuffer()[0]);
        // unchanged
        assertEquals(6, stream.getBuffer()[1]);
        // unchanged
        assertEquals(7, stream.getBuffer()[2]);
    }

    /**
     * After a failed write (e.g., illegal offset/length), the stream may be left in an inconsistent state.
     * Calling {@code reset()} should bring it back to a clean empty state.
     */
    @Test
    void testResetAfterExceptionScenario() {
        FastByteArrayOutputStream stream = new FastByteArrayOutputStream();
        byte[] data = { 1, 2, 3 };
        // provoke IndexOutOfBoundsException by using an invalid offset/length
        Exception exception = assertThrows(IndexOutOfBoundsException.class, () -> {
            stream.write(data, -1, 5);
        }, "Expected write with illegal arguments to throw IndexOutOfBoundsException");
        assertNotNull(exception);
        // after catching the exception, reset the stream
        stream.reset();
        // verify the stream is empty and usable
        assertEquals(0, stream.size());
        assertArrayEquals(new byte[0], stream.toByteArray());
        // a subsequent write should succeed
        stream.write(new byte[] { 9, 9 });
        assertEquals(2, stream.size());
        assertArrayEquals(new byte[] { 9, 9 }, stream.toByteArray(), "Stream must be functional after reset following an exception");
    }
}
