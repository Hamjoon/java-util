package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;

public class FastReaderTest {

    private static final int DEFAULT_BUFFER_SIZE = 8;
    private static final int DEFAULT_PUSHBACK_SIZE = 4;

    private StringReader source;
    private FastReader fastReader;

    /*** Helper to create a FastReader with default sizes ***/
    private FastReader createFastReader(String data) {
        source = new StringReader(data);
        return new FastReader(source, DEFAULT_BUFFER_SIZE, DEFAULT_PUSHBACK_SIZE);
    }

    @After
    public void tearDown() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
        if (source != null) {
            source.close();
        }
    }

    /**
     * Test basic single‑character reads, line/column tracking and getLastSnippet().
     */
    @Test
    public void testReadSingleCharAndSnippet() throws IOException {
        fastReader = createFastReader("A\nBC");
        // read 'A'
        assertEquals('A', fastReader.read());
        assertEquals(1, fastReader.getLine()); // still line 1
        assertEquals(1, fastReader.getCol());  // column after 'A'

        // read '\n'
        assertEquals('\n', fastReader.read());
        assertEquals(2, fastReader.getLine()); // line incremented
        assertEquals(0, fastReader.getCol());  // column reset

        // read 'B' and 'C'
        assertEquals('B', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(1, fastReader.getCol());

        assertEquals('C', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(2, fastReader.getCol());

        // end of stream
        assertEquals(-1, fastReader.read());

        // getLastSnippet should contain everything that has been buffered up to current position
        // Since the internal buffer size is 8, all characters are in the buffer.
        assertEquals("A\nBC", fastReader.getLastSnippet());
    }

    /**
     * Test reading into a char array with a mix of pushback and normal buffer data.
     */
    @Test
    public void testReadCharArrayWithPushback() throws IOException {
        fastReader = createFastReader("12345");
        // read first two characters normally
        assertEquals('1', fastReader.read());
        assertEquals('2', fastReader.read());

        // push back 'X' and then push back '\n'
        fastReader.pushback('X');
        fastReader.pushback('\n'); // pushback buffer now contains "\nX"

        char[] dst = new char[5];
        int n = fastReader.read(dst, 0, 5);
        // Expect to get '\n', 'X', then the remaining three chars '3','4','5'
        assertEquals(5, n);
        assertArrayEquals(new char[]{'\n', 'X', '3', '4', '5'}, dst);

        // verify line/col after reading the pushed‑back newline
        assertEquals(2, fastReader.getLine()); // newline advanced line
        assertEquals(2, fastReader.getCol());  // after 'X' and '3'

        // end of stream
        assertEquals(-1, fastReader.read());
    }

    /**
     * Verify that pushback buffer overflow throws IOException.
     */
    @Test(expected = IOException.class)
    public void testPushbackOverflow() throws IOException {
        fastReader = createFastReader("");
        // pushback buffer size is 4; fill it completely
        fastReader.pushback('a');
        fastReader.pushback('b');
        fastReader.pushback('c');
        fastReader.pushback('d');
        // next pushback should overflow
        fastReader.pushback('e');
    }

    /**
     * Verify that calling read() after close() throws IOException.
     */
    @Test(expected = IOException.class)
    public void testReadAfterClose() throws IOException {
        fastReader = createFastReader("data");
        fastReader.close();
        fastReader.read(); // should throw
    }

    /**
     * Test read(char[],off,len) returning -1 when the stream is empty.
     */
    @Test
    public void testReadCharArrayEmptyStream() throws IOException {
        fastReader = createFastReader("");
        char[] buf = new char[10];
        int n = fastReader.read(buf, 0, 10);
        assertEquals(-1, n);
    }

    /**
     * Test that pushback correctly updates line/column counters.
     */
    @Test
    public void testPushbackLineColAdjustment() throws IOException {
        fastReader = createFastReader("x\ny");
        // read 'x' and '\n' to move to line 2, col 0
        assertEquals('x', fastReader.read());
        assertEquals('\n', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // push back 'z' (non‑newline)
        fastReader.pushback('z');
        assertEquals(2, fastReader.getLine());
        assertEquals(-1, fastReader.getCol()); // col decremented

        // push back newline, line should decrement
        fastReader.pushback('\n');
        assertEquals(1, fastReader.getLine());
        assertEquals(-2, fastReader.getCol()); // column also decremented

        // Now read again to verify the characters come back in LIFO order
        assertEquals('\n', fastReader.read());
        assertEquals('z', fastReader.read());
    }

    /**
     * Ensure that the constructor validates buffer sizes.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidBufferSizes() {
        new FastReader(new StringReader(""), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNegativePushbackSize() {
        new FastReader(new StringReader(""), 5, -1);
    }

    /**
     * Verify that getLastSnippet only includes characters up to the current position
     * in the internal buffer, not the whole source.
     */
    @Test
    public void testGetLastSnippetPartialBuffer() throws IOException {
        // Use a buffer size smaller than the source length to force multiple fills
        fastReader = new FastReader(new StringReader("abcdefghij"), 4, 2);
        // Read first 3 chars; they are in the first fill
        assertEquals('a', fastReader.read());
        assertEquals('b', fastReader.read());
        assertEquals('c', fastReader.read());

        // At this point position == 3, limit == 4, buffer contains "abcd"
        assertEquals("abcd".substring(0, 3), fastReader.getLastSnippet());

        // Read two more chars, causing a refill (position wraps to 0 after limit)
        assertEquals('d', fastReader.read()); // consumes last char of first buffer
        assertEquals('e', fastReader.read()); // triggers refill, buffer now "efgh"
        // position is 1 (after reading 'e')
        assertEquals("efgh".substring(0, 1), fastReader.getLastSnippet());
    }
}
