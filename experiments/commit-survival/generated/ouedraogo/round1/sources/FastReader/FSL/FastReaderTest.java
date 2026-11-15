package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;

/**
 * JUnit 4 test cases for {@link FastReader}.  The tests cover constructor validation,
 * single‑character reads, buffered reads, push‑back handling, line/column tracking,
 * snippet retrieval and proper closing behaviour.
 */
public class FastReaderTest {

    /* ---------- Constructor validation ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorZeroBufferSize() {
        new FastReader(new StringReader(""), 0, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeBufferSize() {
        new FastReader(new StringReader(""), -5, 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativePushbackSize() {
        new FastReader(new StringReader(""), 10, -1);
    }

    /* ---------- Single‑character read and line/col tracking ---------- */

    @Test
    public void testReadSingleCharactersAndLineCol() throws IOException {
        String data = "a\nb\nc";
        FastReader fr = new FastReader(new StringReader(data), 4, 2);

        // read 'a'
        assertEquals('a', fr.read());
        assertEquals(1, fr.getLine()); // still line 1
        assertEquals(1, fr.getCol());

        // read '\n' – line should increase, col reset to 0
        assertEquals('\n', fr.read());
        assertEquals(2, fr.getLine());
        assertEquals(0, fr.getCol());

        // read 'b'
        assertEquals('b', fr.read());
        assertEquals(2, fr.getLine());
        assertEquals(1, fr.getCol());

        // read '\n'
        assertEquals('\n', fr.read());
        assertEquals(3, fr.getLine());
        assertEquals(0, fr.getCol());

        // read 'c'
        assertEquals('c', fr.read());
        assertEquals(3, fr.getLine());
        assertEquals(1, fr.getCol());

        // EOF
        assertEquals(-1, fr.read());
    }

    /* ---------- Pushback handling ---------- */

    @Test
    public void testPushbackSingleChar() throws IOException {
        FastReader fr = new FastReader(new StringReader("xy"), 2, 2);

        // read 'x' and 'y'
        assertEquals('x', fr.read());
        assertEquals('y', fr.read());

        // push back 'y' and then 'x'
        fr.pushback('y');
        fr.pushback('x');

        // now reading should return the pushed characters in LIFO order
        assertEquals('x', fr.read());
        assertEquals('y', fr.read());

        // EOF
        assertEquals(-1, fr.read());
    }

    @Test
    public void testPushbackLineColAdjustment() throws IOException {
        FastReader fr = new FastReader(new StringReader("a\nb"), 3, 3);

        // read 'a' then newline
        assertEquals('a', fr.read());
        assertEquals('\n', fr.read());

        // after newline: line = 2, col = 0
        assertEquals(2, fr.getLine());
        assertEquals(0, fr.getCol());

        // push back the newline
        fr.pushback('\n');

        // line should decrement, col should become -1 (as per implementation)
        assertEquals(1, fr.getLine());
        assertEquals(-1, fr.getCol());

        // reading again yields the newline
        assertEquals('\n', fr.read());
        assertEquals(2, fr.getLine());
        assertEquals(0, fr.getCol());
    }

    @Test(expected = IOException.class)
    public void testPushbackBufferOverflow() throws IOException {
        FastReader fr = new FastReader(new StringReader(""), 5, 1);
        // first pushback succeeds
        fr.pushback('x');
        // second pushback exceeds capacity -> IOException
        fr.pushback('y');
    }

    /* ---------- Buffered read (char array) with pushback precedence ---------- */

    @Test
    public void testReadIntoArrayRespectsPushbackFirst() throws IOException {
        FastReader fr = new FastReader(new StringReader("12345"), 3, 3);

        // push back three characters
        fr.pushback('c');
        fr.pushback('b');
        fr.pushback('a'); // pushback buffer now contains a,b,c (a will be read first)

        char[] buf = new char[6];
        int read = fr.read(buf, 1, 3); // read into buf[1..3]

        assertEquals(3, read);
        assertEquals('a', buf[1]);
        assertEquals('b', buf[2]);
        assertEquals('c', buf[3]);

        // remaining characters should come from the underlying stream
        read = fr.read(buf, 4, 2);
        assertEquals(2, read);
        assertEquals('1', buf[4]);
        assertEquals('2', buf[5]);
    }

    @Test
    public void testReadIntoArrayAcrossMultipleFills() throws IOException {
        // underlying data longer than bufferSize to force multiple fill() calls
        String data = "ABCDEFGHIJ";
        FastReader fr = new FastReader(new StringReader(data), 4, 0);

        char[] out = new char[10];
        int read = fr.read(out, 0, 10);
        assertEquals(10, read);
        assertArrayEquals(data.toCharArray(), out);
    }

    @Test
    public void testReadIntoArrayReturnsMinusOneWhenEmpty() throws IOException {
        FastReader fr = new FastReader(new StringReader(""), 5, 0);
        char[] buf = new char[5];
        int read = fr.read(buf, 0, 5);
        assertEquals(-1, read);
    }

    /* ---------- getLastSnippet ---------- */

    @Test
    public void testGetLastSnippetReflectsBufferContent() throws IOException {
        // bufferSize 5, data longer than 5 to ensure only part of data is in the buffer at a time
        FastReader fr = new FastReader(new StringReader("HelloWorld"), 5, 0);

        // read first three characters
        assertEquals('H', fr.read());
        assertEquals('e', fr.read());
        assertEquals('l', fr.read());

        // getLastSnippet should contain the characters that have been read from the internal buffer
        assertEquals("Hel", fr.getLastSnippet());
    }

    /* ---------- Close behaviour ---------- */

    @Test
    public void testCloseThenReadThrowsIOException() throws IOException {
        FastReader fr = new FastReader(new StringReader("data"), 4, 2);
        fr.close();

        try {
            fr.read();
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastReader stream is closed.", e.getMessage());
        }

        try {
            fr.read(new char[1], 0, 1);
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastReader stream is closed.", e.getMessage());
        }
    }

    /* ---------- Additional edge cases ---------- */

    @Test
    public void testReadZeroLengthArrayReturnsZero() throws IOException {
        FastReader fr = new FastReader(new StringReader("abc"), 3, 0);
        char[] buf = new char[5];
        int read = fr.read(buf, 0, 0);
        assertEquals(0, read);
    }

    @Test
    public void testMultiplePushbacksAndReadsMaintainCorrectLineCol() throws IOException {
        FastReader fr = new FastReader(new StringReader("x\ny\nz"), 4, 4);

        // read 'x' and newline
        assertEquals('x', fr.read());
        assertEquals('\n', fr.read());

        // line = 2, col = 0
        assertEquals(2, fr.getLine());
        assertEquals(0, fr.getCol());

        // push back newline and 'x'
        fr.pushback('\n');
        fr.pushback('x');

        // line should be back to 1, col -2 (x then newline)
        assertEquals(1, fr.getLine());
        assertEquals(-2, fr.getCol());

        // read back the two characters
        assertEquals('x', fr.read());
        assertEquals('\n', fr.read());

        // line/col restored
        assertEquals(2, fr.getLine());
        assertEquals(0, fr.getCol());

        // continue reading remaining characters normally
        assertEquals('y', fr.read());
        assertEquals('\n', fr.read());
        assertEquals('z', fr.read());
        assertEquals(-1, fr.read());
    }
}
