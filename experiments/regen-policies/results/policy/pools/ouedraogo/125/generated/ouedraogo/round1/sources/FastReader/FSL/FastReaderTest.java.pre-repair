package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class FastReaderTest {

    // ---------- Constructor validation ----------
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorZeroBufferSize() {
        new FastReader(new StringReader("test"), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativeBufferSize() {
        new FastReader(new StringReader("test"), -5, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNegativePushbackSize() {
        new FastReader(new StringReader("test"), 10, -1);
    }

    // ---------- Basic read() and line/column tracking ----------
    @Test
    public void testReadSingleCharactersAndLineColTracking() throws IOException {
        // Buffer size 2 forces a refill after two characters
        FastReader fr = new FastReader(new StringReader("a\nb"), 2, 2);

        // read 'a'
        int ch1 = fr.read();
        assertEquals('a', ch1);
        assertEquals(1, fr.getLine()); // still first line
        assertEquals(1, fr.getCol());  // column after 'a'

        // read '\n'
        int ch2 = fr.read();
        assertEquals('\n', ch2);
        assertEquals(2, fr.getLine()); // line incremented
        assertEquals(0, fr.getCol());   // column reset after newline

        // read 'b'
        int ch3 = fr.read();
        assertEquals('b', ch3);
        assertEquals(2, fr.getLine()); // still second line
        assertEquals(1, fr.getCol());   // column after 'b'

        // end of stream
        assertEquals(-1, fr.read());
    }

    // ---------- Pushback behavior ----------
    @Test
    public void testPushbackAndReadRestoresState() throws IOException {
        FastReader fr = new FastReader(new StringReader("xy\nz"), 4, 3);

        // read 'x' and 'y'
        assertEquals('x', fr.read());
        assertEquals('y', fr.read());
        assertEquals(1, fr.getLine());
        assertEquals(2, fr.getCol());

        // push back 'y'
        fr.pushback('y');
        // line/col should move back
        assertEquals(1, fr.getLine());
        assertEquals(1, fr.getCol());

        // read again, should get same 'y'
        assertEquals('y', fr.read());
        assertEquals(1, fr.getLine());
        assertEquals(2, fr.getCol());

        // read newline, then push it back and verify line decrement
        assertEquals('\n', fr.read()); // line becomes 2, col 0
        assertEquals(2, fr.getLine());
        assertEquals(0, fr.getCol());

        fr.pushback('\n'); // line back to 1, col back to 2
        assertEquals(1, fr.getLine());
        assertEquals(2, fr.getCol());

        // reading again should give newline again
        assertEquals('\n', fr.read());
        assertEquals(2, fr.getLine());
        assertEquals(0, fr.getCol());
    }

    @Test
    public void testPushbackOverflow() throws IOException {
        FastReader fr = new FastReader(new StringReader("abc"), 2, 2);
        // Fill pushback buffer (size 2)
        fr.pushback('x');
        fr.pushback('y');
        // Next pushback should overflow
        try {
            fr.pushback('z');
            fail("Expected IOException due to pushback buffer overflow");
        } catch (IOException e) {
            assertEquals("Pushback buffer overflow", e.getMessage());
        }
    }

    // ---------- read(char[], off, len) with pushback ----------
    @Test
    public void testReadIntoArrayRespectsPushbackAndBuffer() throws IOException {
        FastReader fr = new FastReader(new StringReader("12345"), 3, 2);

        // Push back two characters manually (simulate a scenario)
        fr.pushback('b');
        fr.pushback('a'); // pushback buffer now holds ['a','b'] (a at lower index)

        char[] buf = new char[5];
        int read = fr.read(buf, 0, 5);
        assertEquals(5, read);
        // Expected order: a, b, then first three chars from underlying reader: '1','2','3'
        assertArrayEquals(new char[]{'a', 'b', '1', '2', '3'}, buf);
    }

    @Test
    public void testReadIntoArrayReturnsMinusOneAtEof() throws IOException {
        FastReader fr = new FastReader(new StringReader(""), 4, 1);
        char[] dest = new char[10];
        int result = fr.read(dest, 0, 10);
        assertEquals(-1, result);
    }

    // ---------- close() behavior ----------
    @Test
    public void testCloseAndReadAfterClose() throws IOException {
        FastReader fr = new FastReader(new StringReader("data"), 4, 1);
        fr.close();
        try {
            fr.read();
            fail("Expected IOException after closing the FastReader");
        } catch (IOException e) {
            assertEquals("FastReader stream is closed.", e.getMessage());
        }
    }

    // ---------- getLastSnippet ----------
    @Test
    public void testGetLastSnippetPartialBuffer() throws IOException {
        // bufferSize 4, source longer than buffer to cause refill
        FastReader fr = new FastReader(new StringReader("abcdefgh"), 4, 1);

        // read first three characters (no refill yet)
        assertEquals('a', fr.read());
        assertEquals('b', fr.read());
        assertEquals('c', fr.read());

        // getLastSnippet should contain "abc"
        assertEquals("abc", fr.getLastSnippet());

        // read two more characters, this will trigger a refill (buffer now contains "defg")
        assertEquals('d', fr.read());
        assertEquals('e', fr.read());

        // After refill, position should be 2 (pointing after 'e' in new buffer)
        // getLastSnippet should now be first two chars of the new buffer: "de"
        assertEquals("de", fr.getLastSnippet());
    }

    // ---------- Additional edge cases ----------
    @Test
    public void testReadZeroLengthArrayDoesNothing() throws IOException {
        FastReader fr = new FastReader(new StringReader("xyz"), 3, 1);
        char[] dest = new char[5];
        int read = fr.read(dest, 2, 0); // len = 0
        assertEquals(0, read);
        // buffer should remain unchanged (all zeros)
        for (char c : dest) {
            assertEquals('\u0000', c);
        }
    }
}
