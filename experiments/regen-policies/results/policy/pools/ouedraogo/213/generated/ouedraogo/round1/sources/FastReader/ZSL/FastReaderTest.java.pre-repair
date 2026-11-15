/****************************************************************************************
 * FastReaderTest.java
 *
 * JUnit 4 test suite that provides comprehensive coverage of the FastReader class.
 * The tests verify correct behaviour of reading, push‑back, line/column tracking,
 * buffer handling, closing, and snippet extraction.
 *
 * The test class resides in the same package as FastReader so that protected members
 * can be accessed directly or via a small concrete subclass.
 *
 * Author: ChatGPT (OpenAI)
 * Date:   2026‑11‑14
 ****************************************************************************************/

package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;

public class FastReaderTest {

    private FastReader reader;

    /** Helper subclass that exposes the protected movePosition method for direct testing. */
    private static class TestableFastReader extends FastReader {
        TestableFastReader(Reader in) {
            super(in);
        }

        /** Expose protected movePosition so the test can call it directly. */
        @Override
        public void movePosition(char ch) {
            super.movePosition(ch);
        }
    }

    @After
    public void tearDown() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    /* -------------------------------------------------------------------------
     * Basic single‑character reads and line/column tracking
     * ------------------------------------------------------------------------- */

    @Test
    public void testReadSingleChar() throws IOException {
        reader = new FastReader(new StringReader("abc"));
        assertEquals('a', reader.read());
        assertEquals(1, reader.getLine());
        assertEquals(1, reader.getCol());

        assertEquals('b', reader.read());
        assertEquals(1, reader.getLine());
        assertEquals(2, reader.getCol());

        assertEquals('c', reader.read());
        assertEquals(1, reader.getLine());
        assertEquals(3, reader.getCol());

        // End of stream
        assertEquals(-1, reader.read());
    }

    @Test
    public void testReadWithNewline() throws IOException {
        reader = new FastReader(new StringReader("a\nb"));
        // read 'a'
        assertEquals('a', reader.read());
        assertEquals(1, reader.getLine());
        assertEquals(1, reader.getCol());

        // read newline
        assertEquals('\n', reader.read());
        assertEquals(2, reader.getLine()); // line increments
        assertEquals(0, reader.getCol());  // column reset

        // read 'b'
        assertEquals('b', reader.read());
        assertEquals(2, reader.getLine());
        assertEquals(1, reader.getCol());
    }

    /* -------------------------------------------------------------------------
     * Pushback functionality and its impact on line/column counters
     * ------------------------------------------------------------------------- */

    @Test
    public void testPushbackSingleChar() throws IOException {
        reader = new FastReader(new StringReader("xyz"));
        assertEquals('x', reader.read()); // line=1 col=1
        assertEquals(1, reader.getLine());
        assertEquals(1, reader.getCol());

        // push back the same character
        reader.pushback('x');            // should decrement column
        assertEquals(1, reader.getLine());
        assertEquals(0, reader.getCol());

        // read again, should get the pushed back character
        assertEquals('x', reader.read());
        assertEquals(1, reader.getLine());
        assertEquals(1, reader.getCol());
    }

    @Test
    public void testPushbackNewline() throws IOException {
        reader = new FastReader(new StringReader("\n"));
        assertEquals('\n', reader.read()); // line becomes 2, col 0
        assertEquals(2, reader.getLine());
        assertEquals(0, reader.getCol());

        // push back the newline, line should decrement
        reader.pushback('\n');
        assertEquals(1, reader.getLine()); // back to original line
        assertEquals(0, reader.getCol());

        // read again, line should increment again
        assertEquals('\n', reader.read());
        assertEquals(2, reader.getLine());
        assertEquals(0, reader.getCol());
    }

    @Test
    public void testPushbackOverflow() throws IOException {
        // Use the default pushback buffer size of 16
        reader = new FastReader(new StringReader("a"));
        // consume one character so we have something to push back
        assertEquals('a', reader.read());

        // Fill the pushback buffer completely
        for (int i = 0; i < 16; i++) {
            reader.pushback('x');
        }

        // The buffer is now full; the next pushback must fail
        try {
            reader.pushback('y');
            fail("Expected IOException due to pushback buffer overflow");
        } catch (IOException ex) {
            assertEquals("Pushback buffer overflow", ex.getMessage());
        }
    }

    /* -------------------------------------------------------------------------
     * read(char[], off, len) with and without pushback data
     * ------------------------------------------------------------------------- */

    @Test
    public void testReadCharArrayWithPushback() throws IOException {
        reader = new FastReader(new StringReader("abcde"));
        // read three characters normally
        assertEquals('a', reader.read());
        assertEquals('b', reader.read());
        assertEquals('c', reader.read());

        // push back 'c' and 'b' (note order: last pushed is read first)
        reader.pushback('c');
        reader.pushback('b');

        char[] buf = new char[3];
        int count = reader.read(buf, 0, 3);
        assertEquals(3, count);
        assertArrayEquals(new char[]{'b', 'c', 'd'}, buf);
    }

    @Test
    public void testReadCharArrayWithoutPushback() throws IOException {
        reader = new FastReader(new StringReader("hello"));
        char[] destination = new char[5];
        int count = reader.read(destination, 0, 5);
        assertEquals(5, count);
        assertArrayEquals("hello".toCharArray(), destination);
    }

    @Test
    public void testReadCharArrayPartialAndEOF() throws IOException {
        reader = new FastReader(new StringReader("hi"));
        char[] buf = new char[5];
        int count = reader.read(buf, 0, 5);
        // Only two characters are available; the method should return 2
        assertEquals(2, count);
        assertEquals('h', buf[0]);
        assertEquals('i', buf[1]);

        // Subsequent read should return -1 (EOF)
        int eof = reader.read(buf, 0, 5);
        assertEquals(-1, eof);
    }

    /* -------------------------------------------------------------------------
     * End‑of‑stream handling for single‑character and array reads
     * ------------------------------------------------------------------------- */

    @Test
    public void testReadReturnsMinusOneAtEOF() throws IOException {
        reader = new FastReader(new StringReader(""));
        assertEquals(-1, reader.read());

        char[] buf = new char[10];
        assertEquals(-1, reader.read(buf, 0, 10));
    }

    /* -------------------------------------------------------------------------
     * Close behaviour and subsequent illegal operations
     * ------------------------------------------------------------------------- */

    @Test
    public void testCloseAndReadAfterClose() throws IOException {
        reader = new FastReader(new StringReader("data"));
        reader.close();
        try {
            reader.read();
            fail("Expected IOException after reading from a closed FastReader");
        } catch (IOException ex) {
            assertEquals("FastReader stream is closed.", ex.getMessage());
        }
    }

    /* -------------------------------------------------------------------------
     * getLastSnippet – returns characters that have been buffered so far
     * ------------------------------------------------------------------------- */

    @Test
    public void testGetLastSnippet() throws IOException {
        reader = new FastReader(new StringReader("xyz"));
        // No characters read yet – snippet should be empty
        assertEquals("", reader.getLastSnippet());

        // Read first character
        assertEquals('x', reader.read());
        assertEquals("x", reader.getLastSnippet());

        // Read remaining characters
        assertEquals('y', reader.read());
        assertEquals('z', reader.read());
        assertEquals("xyz", reader.getLastSnippet());
    }

    /* -------------------------------------------------------------------------
     * Direct testing of the protected movePosition method via subclass
     * ------------------------------------------------------------------------- */

    @Test
    public void testProtectedMovePositionViaSubclass() {
        TestableFastReader tf = new TestableFastReader(new StringReader(""));
        // Initial state
        assertEquals(1, tf.getLine());
        assertEquals(0, tf.getCol());

        // Normal character should increment column
        tf.movePosition('a');
        assertEquals(1, tf.getLine());
        assertEquals(1, tf.getCol());

        // Newline should increment line and reset column
        tf.movePosition('\n');
        assertEquals(2, tf.getLine());
        assertEquals(0, tf.getCol());

        // Another character after newline
        tf.movePosition('b');
        assertEquals(2, tf.getLine());
        assertEquals(1, tf.getCol());
    }
}
