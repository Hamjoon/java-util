package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link FastReader}.
 */
public class FastReaderTest {

    private static final String SIMPLE_TEXT = "a\nb";
    private static final String MULTI_LINE = "Line1\nLine2\nLine3";

    private FastReader reader;

    @Before
    public void setUp() {
        // Default reader used by many tests – buffer large enough to hold the whole string.
        reader = new FastReader(new StringReader(SIMPLE_TEXT), 10, 5);
    }

    @After
    public void tearDown() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    /* --------------------------------------------------------------------- *
     *  Constructor validation
     * --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorZeroBufferSize() {
        new FastReader(new StringReader("x"), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativePushbackSize() {
        new FastReader(new StringReader("x"), 4, -1);
    }

    /* --------------------------------------------------------------------- *
     *  Basic single‑character reading and line/column tracking
     * --------------------------------------------------------------------- */

    @Test
    public void testReadSingleCharAndLineColTracking() throws IOException {
        // first character 'a'
        int ch1 = reader.read();
        assertEquals('a', ch1);
        assertEquals(1, reader.getLine());   // still line 1
        assertEquals(1, reader.getCol());    // column advanced to 1

        // newline '\n'
        int ch2 = reader.read();
        assertEquals('\n', ch2);
        assertEquals(2, reader.getLine());   // line incremented
        assertEquals(0, reader.getCol());    // column reset

        // character 'b'
        int ch3 = reader.read();
        assertEquals('b', ch3);
        assertEquals(2, reader.getLine());   // still on line 2
        assertEquals(1, reader.getCol());    // column advanced

        // EOF
        assertEquals(-1, reader.read());
    }

    /* --------------------------------------------------------------------- *
     *  Pushback functionality
     * --------------------------------------------------------------------- */

    @Test
    public void testPushbackSingleCharacter() throws IOException {
        // read a newline so that line becomes 2 and col 0
        reader.read(); // 'a'
        int nl = reader.read(); // '\n'
        assertEquals('\n', nl);
        assertEquals(2, reader.getLine());
        assertEquals(0, reader.getCol());

        // pushback the newline
        reader.pushback('\n');
        // line/col should be reverted
        assertEquals(1, reader.getLine());
        assertEquals(0, reader.getCol());

        // next read must return the pushed‑back newline again
        int ch = reader.read();
        assertEquals('\n', ch);
        assertEquals(2, reader.getLine());
        assertEquals(0, reader.getCol());
    }

    @Test
    public void testPushbackNonNewlineDecrementsColumn() throws IOException {
        // read two normal characters
        reader.read(); // 'a'
        reader.read(); // '\n' -> line 2 col 0
        int c = reader.read(); // 'b'
        assertEquals('b', c);
        assertEquals(2, reader.getLine());
        assertEquals(1, reader.getCol());

        // pushback 'b'
        reader.pushback('b');
        // column should decrement
        assertEquals(2, reader.getLine());
        assertEquals(0, reader.getCol());

        // read again, column goes back to 1
        int ch = reader.read();
        assertEquals('b', ch);
        assertEquals(2, reader.getLine());
        assertEquals(1, reader.getCol());
    }

    @Test
    public void testPushbackOverflow() throws IOException {
        FastReader smallPushback = new FastReader(new StringReader("x"), 4, 1);
        // first pushback – ok
        smallPushback.pushback('a');
        // second pushback – should overflow
        try {
            smallPushback.pushback('b');
            fail("Expected IOException due to pushback buffer overflow");
        } catch (IOException e) {
            assertEquals("Pushback buffer overflow", e.getMessage());
        } finally {
            smallPushback.close();
        }
    }

    /* --------------------------------------------------------------------- *
     *  read(char[],off,len) – normal operation and interaction with pushback
     * --------------------------------------------------------------------- */

    @Test
    public void testReadIntoArrayWithoutPushback() throws IOException {
        FastReader fr = new FastReader(new StringReader("HelloWorld"), 3, 0);
        char[] buf = new char[10];
        int read = fr.read(buf, 0, 10);
        assertEquals(10, read);
        assertArrayEquals("HelloWorld".toCharArray(), buf);
        fr.close();
    }

    @Test
    public void testReadIntoArrayWithPushbackConsumedFirst() throws IOException {
        FastReader fr = new FastReader(new StringReader("XYZ"), 2, 2);
        // read two characters: X and Y
        assertEquals('X', fr.read());
        assertEquals('Y', fr.read());

        // push them back in reverse order (Y then X)
        fr.pushback('Y');
        fr.pushback('X');

        char[] target = new char[5];
        // fill with placeholder to verify offset handling
        for (int i = 0; i < target.length; i++) target[i] = '-';

        // read 4 characters starting at offset 1
        int count = fr.read(target, 1, 4);
        assertEquals(4, count);
        // Expected sequence: X, Y (from pushback) then Z (from underlying) then EOF not read
        char[] expected = new char[]{ '-', 'X', 'Y', 'Z', '-' };
        assertArrayEquals(expected, target);
        fr.close();
    }

    @Test
    public void testReadArrayMultipleFillsAndEof() throws IOException {
        // buffer size 2 forces several fills for the 7‑char source
        FastReader fr = new FastReader(new StringReader("1234567"), 2, 0);
        char[] out = new char[10];
        int n = fr.read(out, 0, 10);
        assertEquals(7, n);
        assertArrayEquals("1234567".toCharArray(), java.util.Arrays.copyOf(out, 7));

        // subsequent read should signal EOF
        assertEquals(-1, fr.read(out, 0, 1));
        fr.close();
    }

    /* --------------------------------------------------------------------- *
     *  getLastSnippet – verifies internal buffer snapshot
     * --------------------------------------------------------------------- */

    @Test
    public void testGetLastSnippetMatchesReadCharacters() throws IOException {
        // buffer big enough to hold all characters without wrap‑around
        FastReader fr = new FastReader(new StringReader("abcde"), 10, 0);
        assertEquals("", fr.getLastSnippet());

        fr.read(); // a
        assertEquals("a", fr.getLastSnippet());

        fr.read(); // b
        assertEquals("ab", fr.getLastSnippet());

        // read remaining characters in bulk
        char[] tmp = new char[3];
        fr.read(tmp, 0, 3); // c d e
        assertEquals("abcde", fr.getLastSnippet());

        fr.close();
    }

    /* --------------------------------------------------------------------- *
     *  Closing the reader and subsequent error handling
     * --------------------------------------------------------------------- */

    @Test
    public void testCloseMakesSubsequentReadFail() throws IOException {
        reader.close();
        try {
            reader.read();
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastReader stream is closed.", e.getMessage());
        }
    }

    /* --------------------------------------------------------------------- *
     *  Additional sanity: ensure line/col start at expected values
     * --------------------------------------------------------------------- */

    @Test
    public void testInitialLineAndColumn() {
        assertEquals(1, reader.getLine());
        assertEquals(0, reader.getCol());
    }
}
