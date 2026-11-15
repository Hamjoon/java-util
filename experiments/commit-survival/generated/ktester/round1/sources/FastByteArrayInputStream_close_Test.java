package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class FastByteArrayInputStream_close_Test {

    /**
     * Sample data used for all tests in this class.
     */
    private static final byte[] SAMPLE_DATA = { 10, 20, 30 };

    /**
     * Instance under test – created before each test method.
     */
    private FastByteArrayInputStream stream;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh stream for every test to guarantee isolation.
        stream = new FastByteArrayInputStream(SAMPLE_DATA.clone());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – helps the GC in large test suites.
        stream = null;
    }

    /**
     * Provides skip values for the parameterised test above.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> skipScenarios() {
        return Stream.of(// less than length
        org.junit.jupiter.params.provider.Arguments.of(1L), // far beyond length
        org.junit.jupiter.params.provider.Arguments.of(Long.MAX_VALUE));
    }

    @Test
    void closeDoesNotAffectAvailability() throws IOException {
        // Given – a newly created stream (setup in @BeforeEach)
        assertEquals(SAMPLE_DATA.length, stream.available(), "Initial available bytes should match the buffer length.");
        // When – close is called
        stream.close();
        // Then – the stream should still report the same number of available bytes
        assertEquals(SAMPLE_DATA.length, stream.available(), "Available bytes should remain unchanged after close().");
    }

    @Test
    void streamRemainsReadableAfterClose() throws IOException {
        // Given – a fresh stream with known content
        assertEquals(10, stream.read(), "First byte should be 10.");
        assertEquals(20, stream.read(), "Second byte should be 20.");
        // When – close is invoked
        stream.close();
        // Then – remaining bytes are still readable
        assertEquals(30, stream.read(), "Third byte should still be readable after close().");
        assertEquals(-1, stream.read(), "End of stream should be indicated by -1.");
    }

    @Test
    void closeIsIdempotent() throws IOException {
        // first call
        stream.close();
        assertDoesNotThrow(() -> stream.close(), "Second close() must not throw.");
        assertDoesNotThrow(() -> stream.close(), "Third close() must not throw.");
        // state must be unchanged
        assertEquals(SAMPLE_DATA.length, stream.available(), "Available bytes should stay the same after repeated close() calls.");
    }

    @Test
    @DisplayName("Calling close() on a newly created stream does not throw")
    void closeOnNewStreamDoesNotThrow() {
        assertDoesNotThrow(() -> stream.close(), "close() should complete normally on a fresh stream.");
    }

    @Test
    @DisplayName("close() after reading some bytes does not affect subsequent reads")
    void closeAfterPartialRead() throws IOException {
        assertEquals(10, stream.read(), "First byte should be 10.");
        assertEquals(20, stream.read(), "Second byte should be 20.");
        // close should be a no‑op
        assertDoesNotThrow(() -> stream.close());
        // remaining bytes must still be readable
        assertEquals(30, stream.read(), "Third byte must still be readable after close().");
        assertEquals(-1, stream.read(), "EOF should be indicated by -1.");
    }

    @Test
    @DisplayName("close() after reaching EOF does not throw")
    void closeAfterEof() throws IOException {
        // Exhaust the stream
        while (stream.read() != -1) {
            /* consume */
        }
        assertDoesNotThrow(() -> stream.close(), "close() must not throw even when the stream is already at EOF.");
    }

    @Test
    @DisplayName("close() after EOF is still idempotent")
    void closeAfterEofMultipleTimes() throws IOException {
        while (stream.read() != -1) {
            /* consume */
        }
        stream.close();
        assertDoesNotThrow(() -> stream.close(), "Second close() after EOF must not throw.");
    }

    @Test
    @DisplayName("close() after mark/reset does not affect stream functionality")
    void closeAfterMarkReset() throws IOException {
        stream.mark(10);
        assertEquals(10, stream.read(), "Read first byte after mark.");
        // back to start
        stream.reset();
        // close while stream is positioned at the beginning
        assertDoesNotThrow(() -> stream.close());
        // verify that the stream can still be read from the start
        assertEquals(10, stream.read(), "Byte after reset/close must be 10.");
        assertEquals(20, stream.read(), "Second byte after reset/close must be 20.");
        assertEquals(30, stream.read(), "Third byte after reset/close must be 30.");
    }

    @Test
    @DisplayName("close() after mark/reset, then further reads, works correctly")
    void closeAfterMarkResetThenRead() throws IOException {
        stream.mark(10);
        // consume first byte
        stream.read();
        // back to start
        stream.reset();
        // close after reset
        stream.close();
        // read whole content – should be unchanged
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        assertEquals(30, stream.read());
        assertEquals(-1, stream.read());
    }

    @ParameterizedTest(name = "{index}: skip={0}")
    @MethodSource("skipScenarios")
    @DisplayName("close() after various skip operations does not throw")
    void closeAfterSkip(long bytesToSkip) throws IOException {
        long skipped = stream.skip(bytesToSkip);
        assertTrue(skipped <= bytesToSkip, "skip() should not skip more bytes than requested.");
        assertDoesNotThrow(() -> stream.close(), "close() must not throw after skipping " + bytesToSkip + " bytes.");
        // Verify that remaining bytes (if any) are still readable
        int remaining = stream.available();
        for (int i = 0; i < remaining; i++) {
            assertNotEquals(-1, stream.read(), "Bytes remaining after skip and close should be readable.");
        }
        assertEquals(-1, stream.read(), "After reading all remaining bytes, EOF must be reported.");
    }

    @Test
    @DisplayName("close() on an empty underlying array does not throw")
    void closeOnEmptyArray() {
        FastByteArrayInputStream emptyStream = new FastByteArrayInputStream(new byte[0]);
        assertDoesNotThrow(() -> emptyStream.close(), "close() must succeed even when the underlying byte array is empty.");
    }

    @Test
    @DisplayName("close() on empty array does not affect available()")
    void closeOnEmptyArrayAvailableZero() throws IOException {
        FastByteArrayInputStream emptyStream = new FastByteArrayInputStream(new byte[0]);
        emptyStream.close();
        assertEquals(0, emptyStream.available(), "available() should return 0 for an empty stream even after close().");
    }

    @Test
    @DisplayName("close() does not affect subsequent reads (read first, then close)")
    void closeAfterFirstByteRead() throws IOException {
        assertEquals(10, stream.read(), "First byte must be 10.");
        stream.close();
        assertEquals(20, stream.read(), "Second byte must still be readable after close().");
        assertEquals(30, stream.read(), "Third byte must still be readable after close().");
        assertEquals(-1, stream.read(), "EOF must be reported after all bytes are consumed.");
    }

    @Test
    @DisplayName("close() before any read – stream behaves as if never closed")
    void closeBeforeAnyReadThenReadAll() throws IOException {
        // close immediately
        stream.close();
        // read the whole content – should be unchanged
        assertEquals(10, stream.read());
        assertEquals(20, stream.read());
        assertEquals(30, stream.read());
        assertEquals(-1, stream.read());
    }
}
