package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;

public class FastReaderTest {

    private static final int DEFAULT_BUFFER_SIZE = 16;
    private static final int DEFAULT_PUSHBACK_SIZE = 4;

    private FastReader fastReader;
    private Reader backingReader;

    @Before
    public void setUp() {
        // default set‑up uses a simple string; individual tests can replace it
        backingReader = new StringReader("");
        fastReader = new FastReader(backingReader, DEFAULT_BUFFER_SIZE, DEFAULT_PUSHBACK_SIZE);
    }

    @After
    public void tearDown() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /*** Constructor edge cases ***/

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsZeroBufferSize() {
        new FastReader(new StringReader("x"), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativePushbackSize() {
        new FastReader(new StringReader("x"), 8, -1);
    }

    /*** Basic read() functionality ***/

    @Test
    public void readSingleCharactersAndTrackLineCol() throws IOException {
        String data = "ab\nc";
        fastReader = new FastReader(new StringReader(data), 8, 2);

        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        assertEquals('a', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(1, fastReader.getCol());

        assertEquals('b', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(2, fastReader.getCol());

        assertEquals('\n', fastReader.read()); // newline
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        assertEquals('c', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(1, fastReader.getCol());

        assertEquals(-1, fastReader.read()); // EOF
    }

    /*** pushback() basic usage ***/

    @Test
    public void pushbackThenReadRestoresCharacterAndUpdatesLineCol() throws IOException {
        String data = "x\ny";
        fastReader = new FastReader(new StringReader(data), 8, 2);

        // read 'x'
        assertEquals('x', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(1, fastReader.getCol());

        // pushback 'x'
        fastReader.pushback('x');
        // line/col should be rolled back
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // read again – should get the same 'x'
        assertEquals('x', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(1, fastReader.getCol());

        // read newline
        assertEquals('\n', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // pushback newline
        fastReader.pushback('\n');
        assertEquals(1, fastReader.getLine()); // line decremented
        assertEquals(0, fastReader.getCol()); // col stays at 0 because newline resets

        // read again
        assertEquals('\n', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // read 'y'
        assertEquals('y', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(1, fastReader.getCol());
    }

    @Test(expected = IOException.class)
    public void pushbackOverflowThrowsIOException() throws IOException {
        // pushback buffer size is 2
        fastReader = new FastReader(new StringReader("abc"), 8, 2);
        fastReader.pushback('a'); // 1st
        fastReader.pushback('b'); // 2nd – buffer now full
        fastReader.pushback('c'); // 3rd – should overflow
    }

    /*** read(char[], int, int) basic functionality ***/

    @Test
    public void readIntoArrayWithoutPushback() throws IOException {
        String data = "hello world";
        fastReader = new FastReader(new StringReader(data), 5, 2); // small buffer to force multiple fills

        char[] buf = new char[11];
        int n = fastReader.read(buf, 0, buf.length);
        assertEquals(11, n);
        assertArrayEquals(data.toCharArray(), buf);
        assertEquals(1, fastReader.getLine()); // no newline encountered
        assertEquals(11, fastReader.getCol());
    }

    @Test
    public void readIntoArrayWithPushbackDataFirst() throws IOException {
        String data = "XYZ";
        fastReader = new FastReader(new StringReader(data), 4, 3);
        // push back two characters
        fastReader.pushback('B');
        fastReader.pushback('A');

        char[] out = new char[5];
        int n = fastReader.read(out, 0, out.length);
        // Expected order: A, B, X, Y, Z
        assertEquals(5, n);
        assertArrayEquals(new char[]{'A', 'B', 'X', 'Y', 'Z'}, out);
    }

    @Test
    public void readCharArrayReturnsMinusOneWhenAtEofAndNoDataRead() throws IOException {
        fastReader = new FastReader(new StringReader(""), 4, 2);
        char[] out = new char[10];
        int n = fastReader.read(out, 0, out.length);
        assertEquals(-1, n);
    }

    @Test
    public void readCharArrayPartialReadWhenEofMidOperation() throws IOException {
        fastReader = new FastReader(new StringReader("ab"), 4, 2);
        char[] out = new char[5];
        int n = fastReader.read(out, 0, out.length);
        assertEquals(2, n);
        assertArrayEquals(new char[]{'a', 'b', '\0', '\0', '\0'}, out);
    }

    /*** close() behavior ***/

    @Test(expected = IOException.class)
    public void readAfterCloseThrowsIOException() throws IOException {
        fastReader = new FastReader(new StringReader("data"), 8, 2);
        fastReader.close();
        fastReader.read(); // should fail
    }

    @Test(expected = IOException.class)
    public void readArrayAfterCloseThrowsIOException() throws IOException {
        fastReader = new FastReader(new StringReader("data"), 8, 2);
        fastReader.close();
        fastReader.read(new char[1], 0, 1);
    }

    /*** getLine() and getCol() initial values ***/

    @Test
    public void initialLineAndColumnValues() {
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());
    }

    /*** getLastSnippet() functionality ***/

    @Test
    public void getLastSnippetReflectsCharactersReadSoFar() throws IOException {
        String data = "abcde";
        fastReader = new FastReader(new StringReader(data), 3, 2);
        // read first three chars (buffer size forces internal fill)
        assertEquals('a', fastReader.read());
        assertEquals('b', fastReader.read());
        assertEquals('c', fastReader.read());

        // At this point, position == 3, limit == 3, so snippet should be "abc"
        assertEquals("abc", fastReader.getLastSnippet());

        // read next two chars (causes another fill)
        assertEquals('d', fastReader.read());
        assertEquals('e', fastReader.read());

        // snippet now contains the characters currently in the internal buffer up to position
        // Since buffer was refilled, it now holds "de" at indices 0‑1, position == 2
        assertEquals("de", fastReader.getLastSnippet());
    }

    /*** Additional edge case: zero‑length read ***/

    @Test
    public void readZeroLengthArrayReturnsZero() throws IOException {
        fastReader = new FastReader(new StringReader("abc"), 4, 2);
        char[] buf = new char[5];
        int n = fastReader.read(buf, 0, 0);
        assertEquals(0, n);
        // Ensure no state change
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());
    }

    /*** Additional edge case: off/len validation (negative values) ***/

    @Test(expected = IndexOutOfBoundsException.class)
    public void readArrayWithNegativeOffsetThrowsException() throws IOException {
        fastReader = new FastReader(new StringReader("abc"), 4, 2);
        char[] buf = new char[3];
        fastReader.read(buf, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void readArrayWithNegativeLengthThrowsException() throws IOException {
        fastReader = new FastReader(new StringReader("abc"), 4, 2);
        char[] buf = new char[3];
        fastReader.read(buf, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void readArrayWithOffsetPlusLengthBeyondArrayThrowsException() throws IOException {
        fastReader = new FastReader(new StringReader("abc"), 4, 2);
        char[] buf = new char[3];
        fastReader.read(buf, 2, 2); // 2+2 > 3
    }
}
