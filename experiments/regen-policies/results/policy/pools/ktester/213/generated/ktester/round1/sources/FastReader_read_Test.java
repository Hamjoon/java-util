package com.cedarsoftware.util;

import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.Reader;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FastReader_read_Test {

    private FastReader fastReader;

    private StringReader sourceReader;

    /**
     * Underlying reader IOException – propagates from fill().
     * Uses a Mockito mock to simulate a Reader that always throws IOException.
     */
    @Mock
    private Reader // injected by MockitoExtension
    faultyReader;

    @BeforeEach
    void setupBeforeEach() {
        // Default fixture – a small string that fits comfortably in the default buffer
        sourceReader = new StringReader("abcde");
        fastReader = new FastReader(sourceReader);
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    @Test
    void testReadSingleChar() throws IOException {
        assertEquals('a', fastReader.read(), "First character should be 'a'");
    }

    @Test
    void testReadMultipleChars() throws IOException {
        assertEquals('a', fastReader.read());
        assertEquals('b', fastReader.read());
        assertEquals('c', fastReader.read());
        assertEquals('d', fastReader.read());
        assertEquals('e', fastReader.read());
    }

    @Test
    void testReadEndOfStream() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        assertEquals(-1, fastReader.read(), "Empty source must return -1 immediately");
    }

    @Test
    void testReadEndOfStreamAfterContent() throws IOException {
        assertEquals('a', fastReader.read());
        // consume the rest of the string
        // b
        fastReader.read();
        // c
        fastReader.read();
        // d
        fastReader.read();
        // e
        fastReader.read();
        assertEquals(-1, fastReader.read(), "After the last character the stream should be exhausted");
    }

    @Test
    void testReadFromClosedReader() throws IOException {
        fastReader.close();
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(), "Reading after close must throw IOException");
        assertEquals("FastReader stream is closed.", ex.getMessage(), "Exception message must indicate that the stream is closed");
    }

    @Test
    void testReadAfterPushback() throws IOException {
        // Push back a character and verify that read() returns it first
        fastReader.pushback('X');
        assertEquals('X', fastReader.read(), "Pushback character should be returned before underlying stream");
        // Subsequent reads continue from the original source
        assertEquals('a', fastReader.read(), "After pushback, the underlying stream should resume");
    }

    @Test
    void testReadWithSmallBufferCausingRefill() throws IOException {
        // Build a string longer than the custom buffer to force a refill
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        // e.g. "abcdefghijklmnopqrst"
        String data = sb.toString();
        // Use a tiny buffer (4 chars) and a pushback buffer of size 2
        fastReader = new FastReader(new StringReader(data), 4, 2);
        // Read all characters and collect them
        StringBuilder readBack = new StringBuilder();
        int ch;
        while ((ch = fastReader.read()) != -1) {
            readBack.append((char) ch);
        }
        assertEquals(data, readBack.toString(), "Reading with a small buffer must return the exact original content");
    }

    /**
     * Closed stream – throws IOException with the expected message.
     */
    @Test
    void testClosedStreamThrowsIOExceptionMessage() throws IOException {
        // closes the underlying Reader and sets `in` to null
        fastReader.close();
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(), "Calling read() on a closed FastReader must throw IOException");
        assertEquals("FastReader stream is closed.", ex.getMessage(), "Exception message must match the specification");
    }

    /**
     * Read from pushback buffer – returns the pushed character.
     */
    @Test
    void testReadFromPushbackBufferReturnsPushedChar() throws IOException {
        fastReader.pushback('a');
        assertEquals('a', fastReader.read(), "After pushback('a'), the next read() must return the integer value of 'a' (97)");
    }

    /**
     * Read from underlying reader – returns first character.
     */
    @Test
    void testReadFirstCharFromUnderlyingReader() throws IOException {
        fastReader = new FastReader(new StringReader("Hello"));
        assertEquals('H', fastReader.read(), "First call to read() should return 'H' (72) from the underlying StringReader");
    }

    /**
     * Read after buffer exhausted – triggers fill and returns next character.
     * Uses a small internal buffer (size 2) to force a refill after two reads.
     */
    @Test
    void testReadAfterBufferExhaustedTriggersFill() throws IOException {
        // buffer size = 2, pushback size = 2 (default is fine)
        fastReader = new FastReader(new StringReader("abc"), 2, 2);
        assertEquals('a', fastReader.read());
        assertEquals('b', fastReader.read());
        // third read must cause a refill
        assertEquals('c', fastReader.read(), "After the internal buffer is exhausted, fill() should be called and return the next character");
    }

    /**
     * Read at EOF – returns -1.
     */
    @Test
    void testReadAtEOFReturnsMinusOne() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        assertEquals(-1, fastReader.read(), "When the underlying reader is empty, read() must return -1 immediately");
    }

    /**
     * Read after EOF – subsequent calls continue returning -1.
     */
    @Test
    void testReadAfterEOFContinuesReturningMinusOne() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        assertEquals(-1, fastReader.read(), "First read at EOF should return -1");
        assertEquals(-1, fastReader.read(), "Subsequent reads after EOF must also return -1");
    }

    /**
     * Read after pushback then underlying – order preserved.
     */
    @Test
    void testReadPushbackThenUnderlyingOrderPreserved() throws IOException {
        fastReader = new FastReader(new StringReader("xyz"));
        fastReader.pushback('x');
        // First read: from pushback buffer
        assertEquals('x', fastReader.read(), "First read after pushback should return the pushed character");
        // Second read: from the original underlying stream (its first character)
        assertEquals('x', fastReader.read(), "Second read should return the original first character from the underlying reader");
    }

    @Test
    void testUnderlyingReaderIOExceptionPropagates() throws IOException {
        // Simulate the underlying Reader throwing IOException on any read operation.
        when(faultyReader.read(any(char[].class), anyInt(), anyInt())).thenThrow(new IOException("underlying error"));
        // Also cover the single‑char read() method in case FastReader calls it directly.
        when(faultyReader.read()).thenThrow(new IOException("underlying error"));
        fastReader = new FastReader(faultyReader);
        IOException ex = assertThrows(IOException.class, () -> fastReader.read(), "FastReader.read() must propagate IOException from the underlying Reader");
        assertTrue(ex.getMessage().contains("underlying error"), "Exception message should contain the original cause");
    }
}
