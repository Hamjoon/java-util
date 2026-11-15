package com.cedarsoftware.util;

import java.io.IOException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_close_Test {

    /**
     * The byte array used for all tests in this class.
     */
    private byte[] data;

    /**
     * Instance under test – created fresh for each test method.
     */
    private FastByteArrayInputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // GIVEN – a non‑empty buffer and a fresh stream instance.
        data = new byte[] { 10, 20, 30, 40, 50 };
        stream = new FastByteArrayInputStream(data);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release references – helps the garbage collector and isolates tests.
        stream = null;
        data = null;
    }

    @Test
    void closeDoesNotChangeAvailability() throws IOException {
        // WHEN
        stream.close();
        // THEN – the stream should still report the full number of remaining bytes.
        assertEquals(data.length, stream.available(), "available() should remain unchanged after close()");
    }

    @Test
    void closeIsIdempotent() throws IOException {
        // WHEN – close the stream twice
        stream.close();
        stream.close();
        // THEN – still readable and available count unchanged
        assertEquals(data.length, stream.available(), "available() should remain unchanged after multiple close() calls");
        assertEquals(10, stream.read(), "First byte should still be readable after close()");
    }

    @Test
    void closeAfterPartialReadKeepsRemainingDataIntact() throws IOException {
        // GIVEN – read two bytes
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        // WHEN – close the stream
        stream.close();
        // THEN – the remaining bytes are still available
        assertEquals(3, stream.available(), "available() should reflect the unread bytes after close()");
        assertEquals(30, stream.read(), "Reading after close() should continue from the correct position");
    }

    @Test
    void closeAfterSkipKeepsPosition() throws IOException {
        // GIVEN – skip three bytes
        long skipped = stream.skip(3);
        assertEquals(3, skipped);
        // WHEN – close the stream
        stream.close();
        // THEN – the next read should return the fourth byte
        assertEquals(40, stream.read(), "Read after close() should start from the position left by skip()");
        assertEquals(1, stream.available(), "available() should be consistent with the new position after close()");
    }

    @Test
    void closeOnEmptyStreamDoesNotThrow() throws IOException {
        // GIVEN – an empty stream
        FastByteArrayInputStream empty = new FastByteArrayInputStream(new byte[0]);
        // WHEN – close is invoked
        empty.close();
        // THEN – no exception and available() reports 0
        assertEquals(0, empty.available(), "available() on an empty stream should be 0 after close()");
        assertEquals(-1, empty.read(), "read() on an empty stream should still return -1 after close()");
    }

    @Test
    void closeAfterFullConsumptionKeepsEOFState() throws IOException {
        // GIVEN – consume the whole stream
        while (stream.read() != -1) {
            // consume
        }
        assertEquals(-1, stream.read(), "Stream should be at EOF");
        // WHEN – close is called
        stream.close();
        // THEN – still at EOF, no exception
        assertEquals(-1, stream.read(), "read() should continue to return -1 after close()");
        assertEquals(0, stream.available(), "available() should be 0 after full consumption and close()");
    }

    @Test
    void closeDoesNotDisturbMarkResetState() throws IOException {
        // GIVEN – a small stream with a known pattern
        FastByteArrayInputStream marked = new FastByteArrayInputStream(new byte[] { 0x10, 0x20, 0x30 });
        assertTrue(marked.markSupported(), "markSupported() should be true for FastByteArrayInputStream");
        // mark at the beginning
        marked.mark(0);
        // read one byte, moving the position
        assertEquals(0x10, marked.read(), "First read should return 0x10");
        // WHEN – close is invoked while a mark is active
        marked.close();
        // THEN – reset should bring us back to the marked position
        marked.reset();
        assertEquals(0x10, marked.read(), "After reset() following close(), read() should return the marked byte again");
        assertEquals(2, marked.available(), "available() after reset should reflect the remaining bytes from the mark");
    }
}
