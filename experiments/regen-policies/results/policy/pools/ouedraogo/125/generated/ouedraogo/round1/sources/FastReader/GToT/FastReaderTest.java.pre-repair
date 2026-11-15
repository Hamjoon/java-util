package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;

public class FastReaderTest {

    private static final int DEFAULT_BUFFER_SIZE = 8;
    private static final int DEFAULT_PUSHBACK_SIZE = 4;

    private Reader source;
    private FastReader fr;

    @Before
    public void setUp() {
        source = new StringReader("");
        fr = new FastReader(source, DEFAULT_BUFFER_SIZE, DEFAULT_PUSHBACK_SIZE);
    }

    /* ----------------------------------------------------------------------
     *  Constructor validation
     * ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorZeroBufferSize() {
        new FastReader(new StringReader("x"), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeBufferSize() {
        new FastReader(new StringReader("x"), -5, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativePushbackSize() {
        new FastReader(new StringReader("x"), 5, -1);
    }

    /* ----------------------------------------------------------------------
     *  read() – single character
     * ---------------------------------------------------------------------- */
    @Test
    public void testReadSingleChar() throws IOException {
        fr = new FastReader(new StringReader("abc"), 2, 2);
        assertEquals('a', fr.read());
        assertEquals(1, fr.getLine());
        assertEquals(1, fr.getCol());

        assertEquals('b', fr.read());
        assertEquals(1, fr.getLine());
        assertEquals(2, fr.getCol());

        assertEquals('c', fr.read());
        assertEquals(1, fr.getLine());
        assertEquals(3, fr.getCol());
    }

    @Test(expected = IOException.class)
    public void testReadAfterClose() throws IOException {
        fr = new FastReader(new StringReader("test"), 4, 2);
        fr.close();
        fr.read(); // should throw IOException because stream is closed
    }

    @Test
    public void testReadReturnsMinusOneAtEOF() throws IOException {
        fr = new FastReader(new StringReader(""), 4, 2);
        assertEquals(-1, fr.read());
    }

    /* ----------------------------------------------------------------------
     *  pushback(char) – normal use and overflow
     * ---------------------------------------------------------------------- */
    @Test
    public void testPushbackAndReadAgain() throws IOException {
        fr = new FastReader(new StringReader("z"), 4, 2);
        int first = fr.read();
        assertEquals('z', first);
        assertEquals(1, fr.getLine());
        assertEquals(1, fr.getCol());

        // push back the character
        fr.pushback((char) first);
        // line/col should have been decremented
        assertEquals(1, fr.getLine());
        assertEquals(0, fr.getCol());

        // read again – should get the same character
        int second = fr.read();
        assertEquals('z', second);
        assertEquals(1, fr.getLine());
        assertEquals(1, fr.getCol());
    }

    @Test(expected = IOException.class)
    public void testPushbackOverflow() throws IOException {
        // pushback buffer size = 1
        FastReader smallPushback = new FastReader(new StringReader("ab"), 4, 1);
        // first pushback – ok
        smallPushback.pushback('x');
        // second pushback – should overflow
        smallPushback.pushback('y');
    }

    @Test
    public void testPushbackUpdatesLineAndColumnOnNewline() throws IOException {
        fr = new FastReader(new StringReader("\n"), 4, 2);
        int ch = fr.read();
        assertEquals('\n', ch);
        assertEquals(2, fr.getLine()); // line incremented
        assertEquals(0, fr.getCol());  // column reset

        // push back the newline
        fr.pushback('\n');
        // line and column should revert
        assertEquals(1, fr.getLine());
        assertEquals(-1, fr.getCol()); // col was 0 then decremented
    }

    /* ----------------------------------------------------------------------
     *  read(char[], off, len) – normal usage, zero length, EOF, and pushback
     * ---------------------------------------------------------------------- */
    @Test
    public void testReadIntoArraySimple() throws IOException {
        fr = new FastReader(new StringReader("hello"), 3, 2);
        char[] buf = new char[5];
        int n = fr.read(buf, 0, buf.length);
        assertEquals(5, n);
        assertArrayEquals(new char[]{'h', 'e', 'l', 'l', 'o'}, buf);
        assertEquals(1, fr.getLine());
        assertEquals(5, fr.getCol());
    }

    @Test
    public void testReadIntoArrayWithZeroLength() throws IOException {
        fr = new FastReader(new StringReader("data"), 4, 2);
        char[] buf = new char[10];
        int n = fr.read(buf, 0, 0);
        assertEquals(0, n);
    }

    @Test
    public void testReadIntoArrayWhenEOFImmediately() throws IOException {
        fr = new FastReader(new StringReader(""), 4, 2);
        char[] buf = new char[4];
        int n = fr.read(buf, 0, buf.length);
        assertEquals(-1, n);
    }

    @Test
    public void testReadIntoArrayUsesPushbackFirst() throws IOException {
        // Prepare a reader with more data than buffer to force pushback usage
        fr = new FastReader(new StringReader("ABCD"), 2, 2);
        // Read two characters normally
        assertEquals('A', fr.read());
        assertEquals('B', fr.read());

        // Push them back
        fr.pushback('B');
        fr.pushback('A');

        // Now read into array – should retrieve the pushed back chars first
        char[] out = new char[4];
        int n = fr.read(out, 0, 4);
        assertEquals(4, n);
        assertArrayEquals(new char[]{'A', 'B', 'C', 'D'}, out);
    }

    /* ----------------------------------------------------------------------
     *  getLine() / getCol() – line/column tracking across newlines
     * ---------------------------------------------------------------------- */
    @Test
    public void testLineAndColumnTrackingWithMultipleNewlines() throws IOException {
        String text = "line1\nline2\n\nline4";
        fr = new FastReader(new StringReader(text), 5, 2);
        int total = 0;
        while (fr.read() != -1) {
            total++;
        }
        // After reading whole string: line count = 4 (three '\n' characters)
        assertEquals(4, fr.getLine());
        // Column after the final character (which is '4') should be 5
        assertEquals(5, fr.getCol());
    }

    /* ----------------------------------------------------------------------
     *  getLastSnippet() – returns characters currently in the internal buffer
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetLastSnippetPartialBuffer() throws IOException {
        // bufferSize larger than the string to keep everything in the buffer
        fr = new FastReader(new StringReader("xyz"), 10, 2);
        fr.read(); // 'x'
        fr.read(); // 'y'
        // At this point, position should be 2, buf[0]='x', buf[1]='y'
        assertEquals("xy", fr.getLastSnippet());
    }

    @Test
    public void testGetLastSnippetAfterBufferWrap() throws IOException {
        // Fill the buffer, then read more to cause a refill
        fr = new FastReader(new StringReader("ABCDEFGHIJ"), 4, 2);
        // Read first 6 chars – this will cause two fills
        for (int i = 0; i < 6; i++) {
            fr.read();
        }
        // At this point position points inside the second buffer fill,
        // which contains chars 'E','F','G','H' (the last fill)
        // getLastSnippet should return those four chars
        assertEquals("EFGH", fr.getLastSnippet());
    }

    /* ----------------------------------------------------------------------
     *  close() – normal close and idempotent close
     * ---------------------------------------------------------------------- */
    @Test
    public void testCloseIdempotent() throws IOException {
        fr = new FastReader(new StringReader("close"), 4, 2);
        fr.close(); // first close – should close underlying reader
        // second close should be a no‑op and not throw
        fr.close();
    }

    /* ----------------------------------------------------------------------
     *  Additional edge cases for read(char[], off, len) with non‑zero offset
     * ---------------------------------------------------------------------- */
    @Test
    public void testReadIntoArrayWithOffset() throws IOException {
        fr = new FastReader(new StringReader("12345"), 5, 2);
        char[] target = new char[10];
        // Fill first part with sentinel values
        for (int i = 0; i < target.length; i++) {
            target[i] = '-';
        }
        int n = fr.read(target, 3, 4); // should read "1234" into positions 3‑6
        assertEquals(4, n);
        assertArrayEquals(new char[]{
                '-', '-', '-', '1', '2', '3', '4', '-', '-', '-'
        }, target);
    }
}
