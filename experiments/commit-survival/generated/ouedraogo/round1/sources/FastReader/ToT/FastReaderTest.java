package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit4 test suite for {@link FastReader}.
 *
 * The tests are divided among three "experts":
 *   1. Constructor & basic single‑character reading.
 *   2. Push‑back handling, multi‑character reading and edge conditions.
 *   3. Line/column tracking, snippet extraction and resource closing.
 *
 * Each expert contributes one test per public method (or protected method via a subclass)
 * and the suite combines them into a single file.
 */
public class FastReaderTest {

    private static final String SAMPLE_TEXT = "Line1\nLine2\r\nLine3";

    private FastReader reader;
    private StringReader underlying;

    @Before
    public void setUp() {
        // Use a small buffer size to force multiple fills during tests.
        underlying = new StringReader(SAMPLE_TEXT);
        reader = new FastReader(underlying, 4, 3);
    }

    @After
    public void tearDown() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    /* --------------------------------------------------------------
     *  Expert 1 – Constructor & single‑character read()
     * -------------------------------------------------------------- */

    @Test
    public void testConstructorValidParameters() {
        // Normal construction should not throw.
        FastReader r = new FastReader(new StringReader("abc"), 10, 2);
        assertNotNull(r);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSize() {
        new FastReader(new StringReader("abc"), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidPushbackSize() {
        new FastReader(new StringReader("abc"), 5, -1);
    }

    @Test
    public void testReadSingleCharUpdatesLineAndCol() throws IOException {
        // First character is 'L' (not a newline)
        int ch = reader.read();
        assertEquals('L', ch);
        assertEquals(1, reader.getLine());
        assertEquals(1, reader.getCol());

        // Read until first newline
        while (ch != '\n') {
            ch = reader.read();
        }
        // After reading '\n' line should be incremented, col reset to 0
        assertEquals('\n', ch);
        assertEquals(2, reader.getLine());
        assertEquals(0, reader.getCol());

        // Next character after newline is 'L' of second line
        ch = reader.read();
        assertEquals('L', ch);
        assertEquals(2, reader.getLine());
        assertEquals(1, reader.getCol());
    }

    @Test
    public void testReadReturnsMinusOneAtEof() throws IOException {
        // Consume the whole underlying string
        int total = 0;
        while (reader.read() != -1) {
            total++;
        }
        assertEquals(SAMPLE_TEXT.length(), total);
        // Subsequent reads should return -1
        assertEquals(-1, reader.read());
        assertEquals(-1, reader.read());
    }

    /* --------------------------------------------------------------
     *  Expert 2 – Pushback handling & read(char[],off,len)
     * -------------------------------------------------------------- */

    @Test
    public void testPushbackAndReadFromPushbackBuffer() throws IOException {
        // Read two characters: 'L' and 'i'
        assertEquals('L', reader.read());
        assertEquals('i', reader.read());

        // Push back both characters (in reverse order)
        reader.pushback('i');
        reader.pushback('L');

        // Now read again; should get the same characters in original order
        assertEquals('L', reader.read());
        assertEquals('i', reader.read());

        // Verify line/col are consistent (no newline encountered)
        assertEquals(1, reader.getLine());
        assertEquals(2, reader.getCol());
    }

    @Test(expected = IOException.class)
    public void testPushbackOverflowThrows() throws IOException {
        // Pushback buffer size is 3; push back four characters to overflow.
        reader.pushback('a');
        reader.pushback('b');
        reader.pushback('c');
        // This fourth pushback must fail
        reader.pushback('d');
    }

    @Test
    public void testReadCharArrayWithOffsetAndLength() throws IOException {
        char[] buffer = new char[20];
        // Fill buffer with sentinel values to detect overwrites
        for (int i = 0; i < buffer.length; i++) buffer[i] = '*';

        // Read 5 chars starting at offset 2
        int read = reader.read(buffer, 2, 5);
        assertEquals(5, read);
        // Verify that characters are placed correctly
        String expected = SAMPLE_TEXT.substring(0, 5);
        assertEquals(expected, new String(buffer, 2, 5));

        // Verify untouched parts remain unchanged
        assertEquals('*', buffer[0]);
        assertEquals('*', buffer[1]);
        assertEquals('*', buffer[7]); // position after read
    }

    @Test
    public void testReadCharArrayWhenPushbackPresent() throws IOException {
        // Read first three characters
        assertEquals('L', reader.read());
        assertEquals('i', reader.read());
        assertEquals('n', reader.read());

        // Push back two characters ('i' and 'L')
        reader.pushback('n'); // push back last read char
        reader.pushback('i');
        reader.pushback('L');

        char[] dest = new char[4];
        int read = reader.read(dest, 0, 4);
        // Should read the three pushback chars then the next real char ('e')
        assertEquals(4, read);
        assertArrayEquals(new char[]{'L', 'i', 'n', 'e'}, dest);
    }

    @Test
    public void testReadCharArrayReturnsMinusOneWhenEofAndNoDataRead() throws IOException {
        // Exhaust the stream
        while (reader.read() != -1) { /* drain */ }

        char[] buf = new char[5];
        int result = reader.read(buf, 0, 5);
        assertEquals(-1, result);
    }

    /* --------------------------------------------------------------
     *  Expert 3 – Line/Column tracking, getLastSnippet, close()
     * -------------------------------------------------------------- */

    @Test
    public void testGetLineAndColAfterMultipleLines() throws IOException {
        // Read through whole text, tracking line/col after each newline
        int ch;
        while ((ch = reader.read()) != -1) {
            if (ch == '\n') {
                // After newline, col should be 0 and line incremented
                assertEquals(0, reader.getCol());
                // line value is verified at the end of the loop
            }
        }
        // Expected line count: three lines (the last line may not end with newline)
        assertEquals(3, reader.getLine());
    }

    @Test
    public void testGetLastSnippetReflectsBufferContentUpToPosition() throws IOException {
        // The internal buffer size is 4; after reading 6 chars we will have wrapped.
        for (int i = 0; i < 6; i++) {
            reader.read();
        }
        // The snippet should contain the characters that are currently in buf[0..position-1]
        // Since buffer size is 4 and we read 6 chars, the buffer now holds characters 3..6
        // (zero‑based). Let's compute expected snippet.
        String expected = SAMPLE_TEXT.substring(2, 2 + reader.position);
        assertEquals(expected, reader.getLastSnippet());
    }

    @Test(expected = IOException.class)
    public void testReadAfterCloseThrowsIOException() throws IOException {
        reader.close();
        reader.read();
    }

    @Test(expected = IOException.class)
    public void testReadCharArrayAfterCloseThrowsIOException() throws IOException {
        reader.close();
        reader.read(new char[1], 0, 1);
    }

    @Test
    public void testProtectedMovePositionViaSubclass() throws IOException {
        // Subclass exposing the protected method for direct testing
        class TestableFastReader extends FastReader {
            TestableFastReader(Reader in, int bufferSize, int pushbackBufferSize) {
                super(in, bufferSize, pushbackBufferSize);
            }
            void exposeMovePosition(char ch) {
                super.movePosition(ch);
            }
        }

        TestableFastReader r = new TestableFastReader(new StringReader("\nA"), 2, 0);
        // Initially line=1, col=0
        assertEquals(1, r.getLine());
        assertEquals(0, r.getCol());

        // Simulate reading a newline
        r.exposeMovePosition('\n');
        assertEquals(2, r.getLine());
        assertEquals(0, r.getCol());

        // Simulate reading a regular character
        r.exposeMovePosition('A');
        assertEquals(2, r.getLine());
        assertEquals(1, r.getCol());
    }

    /* --------------------------------------------------------------
     *  Additional sanity checks
     * -------------------------------------------------------------- */

    @Test
    public void testMultiplePushbacksAndReadCharArrayMix() throws IOException {
        // Read two chars
        assertEquals('L', reader.read());
        assertEquals('i', reader.read());

        // Push back three chars (including one that wasn't read yet)
        reader.pushback('i');
        reader.pushback('L');
        reader.pushback('X'); // push back arbitrary char

        char[] dest = new char[5];
        int read = reader.read(dest, 1, 4);
        // Expected sequence: X L i (next real char 'n')
        assertEquals(4, read);
        assertArrayEquals(new char[]{'*', 'X', 'L', 'i', 'n'}, dest);
    }
}
