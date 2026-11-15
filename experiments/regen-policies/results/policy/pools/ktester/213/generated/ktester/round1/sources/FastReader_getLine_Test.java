package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class FastReader_getLine_Test {

    private FastReader fastReader;

    @BeforeEach
    void setupBeforeEach() {
        // Each test will create its own FastReader instance as needed.
        fastReader = null;
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /**
     * Verify that a newly constructed FastReader reports line 1.
     */
    @Test
    void testInitialLineIsOne() {
        fastReader = new FastReader(new StringReader("any content"));
        assertEquals(1, fastReader.getLine(), "A newly created FastReader should start with line number 1");
    }

    /**
     * Verify that the line number increments when a newline character is read.
     */
    @Test
    void testLineIncrementsOnNewline() throws IOException {
        fastReader = new FastReader(new StringReader("abc\ndef"));
        // Read characters up to and including the newline
        assertEquals('a', fastReader.read());
        assertEquals('b', fastReader.read());
        assertEquals('c', fastReader.read());
        // newline encountered
        assertEquals('\n', fastReader.read());
        // After reading a newline, line should be 2
        assertEquals(2, fastReader.getLine(), "Line number should increment after reading a newline character");
        // Continue reading to ensure line stays at 2
        assertEquals('d', fastReader.read());
        assertEquals('e', fastReader.read());
        assertEquals('f', fastReader.read());
        assertEquals(2, fastReader.getLine(), "Line number should remain unchanged after reading non‑newline characters");
    }

    /**
     * Verify that multiple newline characters correctly increment the line counter.
     */
    @Test
    void testMultipleNewlinesIncreaseLineCount() throws IOException {
        fastReader = new FastReader(new StringReader("line1\nline2\nline3\n"));
        // Read through the entire content
        int ch;
        while ((ch = fastReader.read()) != -1) {
            // just consume
        }
        // There are three newline characters, so line should be 4 (starting at 1)
        assertEquals(4, fastReader.getLine(), "Line number should be initial line plus number of newline characters read");
    }

    /**
     * Verify that a newline that is pushed back does not affect the line count
     * until it is read again.
     */
    @Test
    void testPushbackDoesNotPrematurelyChangeLine() throws IOException {
        fastReader = new FastReader(new StringReader("a\nb"));
        // Read 'a' and the newline
        assertEquals('a', fastReader.read());
        assertEquals('\n', fastReader.read());
        assertEquals(2, fastReader.getLine(), "Line should be 2 after reading the first newline");
        // Push back the newline
        fastReader.pushback('\n');
        // Line count should still be 2 because we have not *read* the pushed‑back newline again
        assertEquals(2, fastReader.getLine(), "Line number should remain unchanged after pushback of a newline");
        // Read the pushed‑back newline again
        assertEquals('\n', fastReader.read());
        // Now the line should increment to 3
        assertEquals(3, fastReader.getLine(), "Line number should increment when the pushed‑back newline is read again");
    }

    /**
     * Verify that reading after the end of the stream does not change the line number.
     */
    @Test
    void testLineUnchangedAfterEof() throws IOException {
        fastReader = new FastReader(new StringReader("a\nb"));
        // Consume all characters
        while (fastReader.read() != -1) {
            // no‑op
        }
        int lineAfterEof = fastReader.getLine();
        // Subsequent reads return -1 and must leave line unchanged
        assertEquals(-1, fastReader.read());
        assertEquals(lineAfterEof, fastReader.getLine(), "Line number should stay the same after reaching EOF");
    }

    /**
     * Verify that after manually setting the private {@code line} field via reflection,
     * {@code getLine()} returns the injected value.
     */
    @Test
    void testGetLineReflectivelySetLineField() throws Exception {
        fastReader = new FastReader(new StringReader("dummy"));
        // Use reflection to set the private 'line' field to 42
        Field lineField = FastReader.class.getDeclaredField("line");
        lineField.setAccessible(true);
        lineField.setInt(fastReader, 42);
        assertEquals(42, fastReader.getLine(), "After reflection sets line to 42, getLine() must return 42");
    }

    /**
     * After reading a single newline, the line counter should be 2.
     */
    @Test
    void testGetLineAfterSingleNewline() throws IOException {
        fastReader = new FastReader(new StringReader("first line\nsecond line"));
        // Consume characters up to and including the newline
        // 'f'
        fastReader.read();
        // 'i'
        fastReader.read();
        // fast-forward to the newline (position 10)
        while (fastReader.read() != '\n') {
            // consume until newline
        }
        assertEquals(2, fastReader.getLine(), "Line number should be 2 after a single newline has been read");
    }

    /**
     * Verify that bulk reading with {@code read(char[],int,int)} correctly counts
     * newline characters and updates the line counter.
     */
    @Test
    void testGetLineAfterBulkReadMultipleLines() throws IOException {
        String source = "line1\nline2\nline3";
        fastReader = new FastReader(new StringReader(source));
        // Buffer large enough to hold the whole string
        char[] buffer = new char[10];
        int read = fastReader.read(buffer, 0, buffer.length);
        assertTrue(read > 0, "Bulk read should return a positive count");
        // The content contains two newline characters, so line should be 3 (1 + 2)
        assertEquals(3, fastReader.getLine(), "After bulk read of two newlines, line counter should be 3");
    }

    /**
     * When no newline characters are present, the line counter must stay at 1
     * regardless of how many characters are read.
     */
    @Test
    void testGetLineRemainsOneWhenNoNewlines() throws IOException {
        fastReader = new FastReader(new StringReader("abcdefg"));
        int ch;
        while ((ch = fastReader.read()) != -1) {
            // consume all characters
        }
        assertEquals(1, fastReader.getLine(), "Line number must remain 1 when the source contains no newline characters");
    }

    /**
     * Pushback of a newline should revert the line counter to the state before the newline
     * was consumed. This test uses reflection to verify the internal line field after pushback.
     */
    @Test
    void testGetLineAfterPushbackRevertsLine() throws Exception {
        fastReader = new FastReader(new StringReader("first\nsecond"));
        // Read characters up to the newline (including it)
        while (fastReader.read() != '\n') {
            // consume until newline
        }
        // At this point line should be 2
        assertEquals(2, fastReader.getLine());
        // Push back the newline
        fastReader.pushback('\n');
        // Use reflection to inspect the private line field – it should have been decremented back to 1
        Field lineField = FastReader.class.getDeclaredField("line");
        lineField.setAccessible(true);
        int lineAfterPushback = lineField.getInt(fastReader);
        assertEquals(1, lineAfterPushback, "After pushing back a newline, internal line counter should revert to 1");
        // getLine() should also reflect the reverted state
        assertEquals(1, fastReader.getLine(), "getLine() must report the reverted line number after pushback");
    }

    /**
     * Closing the reader must not reset the line counter; it should retain its last value.
     */
    @Test
    void testGetLineAfterCloseDoesNotReset() throws IOException {
        fastReader = new FastReader(new StringReader("a\nb"));
        // Read up to and including the newline
        // 'a'
        fastReader.read();
        // '\n' -> line becomes 2
        fastReader.read();
        assertEquals(2, fastReader.getLine());
        // Close the reader
        fastReader.close();
        // The line number should still be 2 after close
        assertEquals(2, fastReader.getLine(), "Line number must remain unchanged after the FastReader is closed");
    }

    /**
     * When the underlying source is empty, repeated reads return -1 and the line
     * counter stays at its initial value of 1.
     */
    @Test
    void testGetLineWithEmptySourceAndRepeatedEof() throws IOException {
        fastReader = new FastReader(new StringReader(""));
        // First read returns EOF
        assertEquals(-1, fastReader.read());
        // Subsequent reads also return EOF
        assertEquals(-1, fastReader.read());
        assertEquals(-1, fastReader.read());
        // Line counter must still be 1
        assertEquals(1, fastReader.getLine(), "Line number should remain 1 when no characters (including newlines) have been read");
    }
}
