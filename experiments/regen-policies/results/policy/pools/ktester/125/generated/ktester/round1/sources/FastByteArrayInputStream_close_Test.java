package com.cedarsoftware.util;

import java.io.IOException;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_close_Test {

    /**
     * Verify that invoking {@link FastByteArrayInputStream#close()} does not
     * affect the ability to read from the stream afterwards. The implementation
     * is a no‑op, so the stream should remain fully functional.
     */
    @Test
    void testCloseDoesNotAffectStream() throws IOException {
        // given
        byte[] data = { 10, 20, 30, 40 };
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        // when
        // consume one byte
        int firstByte = stream.read();
        // close the stream (no‑op)
        stream.close();
        // then
        assertEquals(10, firstByte, "First byte read before close should be 10");
        // The stream should still report the correct number of remaining bytes
        assertEquals(3, stream.available(), "Available bytes after close should be unchanged");
        // Reading after close should continue normally
        assertEquals(20, stream.read(), "Second byte after close should be 20");
        assertEquals(30, stream.read(), "Third byte after close should be 30");
        assertEquals(40, stream.read(), "Fourth byte after close should be 40");
        assertEquals(-1, stream.read(), "End of stream should be indicated by -1");
    }

    /**
     * Verify that calling {@code close()} repeatedly does not throw an exception.
     */
    @Test
    void testCloseIdempotent() throws IOException {
        // given
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[] { 1, 2, 3 });
        // when / then
        // first close – should succeed
        stream.close();
        assertDoesNotThrow(stream::close, "Calling close a second time should not throw");
        assertDoesNotThrow(stream::close, "Calling close a third time should not throw");
    }

    /**
     * Calling close() on a newly created stream should complete without throwing.
     */
    @Test
    void testCloseOnNewStream() throws IOException {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[] { 5, 6, 7 });
        assertDoesNotThrow(stream::close, "Close on a fresh stream must not throw");
    }

    /**
     * After reading some bytes, invoking close() should still be safe.
     */
    @Test
    void testCloseAfterPartialRead() throws IOException {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[] { 9, 8, 7, 6 });
        // read two bytes
        assertEquals(9, stream.read());
        assertEquals(8, stream.read());
        // close should not throw
        assertDoesNotThrow(stream::close, "Close after partial read must not throw");
        // verify stream continues to work
        assertEquals(7, stream.read());
    }

    /**
     * After marking the stream and reading, close() must not cause any exception.
     */
    @Test
    void testCloseAfterMarkAndRead() throws IOException {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[] { 1, 2, 3, 4 });
        // mark at position 0
        stream.mark(10);
        // read one byte
        assertEquals(1, stream.read());
        assertDoesNotThrow(stream::close, "Close after mark+read must not throw");
        // reset should still work because close is a no‑op
        stream.reset();
        assertEquals(1, stream.read(), "After reset, first byte should be 1 again");
    }

    /**
     * After skipping bytes, close() should behave normally.
     */
    @Test
    void testCloseAfterSkip() throws IOException {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[] { 10, 20, 30, 40, 50 });
        long skipped = stream.skip(2);
        assertEquals(2, skipped, "Should have skipped two bytes");
        assertDoesNotThrow(stream::close, "Close after skip must not throw");
        // verify remaining data is still readable
        assertEquals(30, stream.read());
    }

    /**
     * Calling close() on a stream that has already reached EOF should not throw.
     */
    @Test
    void testCloseAfterEof() throws IOException {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[] { 1 });
        // consume the only byte
        assertEquals(1, stream.read());
        assertEquals(-1, stream.read(), "Stream should be at EOF");
        assertDoesNotThrow(stream::close, "Close after EOF must not throw");
    }

    /**
     * Calling close() after invoking reset() without a prior mark should complete normally.
     */
    @Test
    void testCloseAfterResetWithoutMark() throws IOException {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[] { 5, 6, 7 });
        // per InputStream contract this should set position to 0
        stream.reset();
        assertDoesNotThrow(stream::close, "Close after reset (no prior mark) must not throw");
        // ensure stream still works
        assertEquals(5, stream.read());
    }

    /**
     * Close on a zero‑length byte array should not throw any exception.
     */
    @Test
    void testCloseOnZeroLengthArray() throws IOException {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[0]);
        assertDoesNotThrow(stream::close, "Close on zero‑length stream must not throw");
        assertEquals(-1, stream.read(), "Zero‑length stream should immediately return EOF");
    }

    /**
     * Close after a read() that returned -1 (empty stream) should be safe.
     */
    @Test
    void testCloseAfterReadReturnsMinusOne() throws IOException {
        FastByteArrayInputStream stream = new FastByteArrayInputStream(new byte[0]);
        assertEquals(-1, stream.read(), "Empty stream should return -1 on first read");
        assertDoesNotThrow(stream::close, "Close after read returning -1 must not throw");
    }

    /**
     * Close from a different thread while another thread is reading should not cause an IOException.
     */
    @Test
    void testCloseConcurrentWithRead() throws Exception {
        byte[] data = new byte[1024];
        for (int i = 0; i < data.length; i++) data[i] = (byte) i;
        FastByteArrayInputStream stream = new FastByteArrayInputStream(data);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch readLatch = new CountDownLatch(1);
        Thread reader = new Thread(() -> {
            try {
                // signal that the thread has started
                startLatch.countDown();
                // Continuously read until EOF or interruption
                while (stream.read() != -1) {
                    // busy‑read
                }
                // signal that reading finished
                readLatch.countDown();
            } catch (IOException ignored) {
                // ignore – we are testing that close does not throw
            }
        });
        reader.start();
        // Wait for the reader thread to start
        assertTrue(startLatch.await(1, TimeUnit.SECONDS), "Reader thread did not start in time");
        // Close while the other thread may still be reading
        assertDoesNotThrow(stream::close, "Concurrent close must not throw");
        // Ensure the reader thread eventually finishes (it should see EOF after close)
        assertTrue(readLatch.await(2, TimeUnit.SECONDS), "Reader thread did not finish after close");
        reader.join();
    }
}
