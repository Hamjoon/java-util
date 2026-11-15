package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;

public class FastReaderTest {

    private static final String SIMPLE_TEXT = "Hello World\nSecond Line";

    private FastReader reader;

    @Before
    public void setUp() {
        // default constructor uses default buffer sizes
        reader = new FastReader(new StringReader(SIMPLE_TEXT));
    }

    /* -----------------------------------------------------------------
     *  Public method list (extracted from FastReader)
     * -----------------------------------------------------------------
     * FastReader(Reader in)
     * FastReader(Reader in, int bufferSize, int pushbackBufferSize)
     * void pushback(char ch) throws IOException
     * int read() throws IOException
     * int read(char[] cbuf, int off, int len) throws IOException
     * void close() throws IOException
     * int getLine()
     * int getCol()
     * String getLastSnippet()
     */

    // ------------------- basic functionality tests --------------------

    @Test
    public void testReadSingleCharacters() throws IOException {
        assertEquals('H', reader.read());
        assertEquals('e', reader.read());
        assertEquals('l', reader.read());
        assertEquals('l', reader.read());
        assertEquals('o', reader.read());
    }

    @Test
    public void testReadCharArrayFull() throws IOException {
        char[] buffer = new char[12];
        int read = reader.read(buffer, 0, buffer.length);
        assertEquals(12, read);
        assertArrayEquals("Hello World".toCharArray(), buffer);
    }

    @Test
    public void testReadCharArrayPartial() throws IOException {
        char[] buffer = new char[5];
        int read = reader.read(buffer, 0, buffer.length);
        assertEquals(5, read);
        assertArrayEquals("Hello".toCharArray(), buffer);
    }

    @Test
    public void testReadCharArrayWithOffset() throws IOException {
        char[] buffer = new char[10];
        // pre‑fill with sentinel values
        for (int i = 0; i < buffer.length; i++) buffer[i] = '*';
        int read = reader.read(buffer, 2, 4); // should fill positions 2‑5
        assertEquals(4, read);
        assertEquals('*', buffer[0]);
        assertEquals('*', buffer[1]);
        assertArrayEquals("Hell".toCharArray(), new char[]{buffer[2], buffer[3], buffer[4], buffer[5]});
        assertEquals('*', buffer[6]);
        assertEquals('*', buffer[7]);
        assertEquals('*', buffer[8]);
        assertEquals('*', buffer[9]);
    }

    @Test
    public void testPushbackSingleCharacter() throws IOException {
        int first = reader.read();               // 'H'
        assertEquals('H', first);
        reader.pushback((char) first);           // push back 'H'
        int afterPushback = reader.read();
        assertEquals('H', afterPushback);        // should read the same char again
    }

    @Test
    public void testPushbackAndLineTracking() throws IOException {
        // read until the newline character
        for (int i = 0; i < "Hello World".length(); i++) {
            reader.read();
        }
        int nl = reader.read();                  // reads '\n'
        assertEquals('\n', nl);
        assertEquals(2, reader.getLine());       // line should have advanced
        assertEquals(0, reader.getCol());        // column reset after newline

        // push back the newline and verify line/col are restored
        reader.pushback('\n');
        assertEquals(1, reader.getLine());       // line decremented
        assertEquals(-1, reader.getCol());       // col decremented (as per implementation)

        // reading again should give the newline and line/col move forward again
        int nlAgain = reader.read();
        assertEquals('\n', nlAgain);
        assertEquals(2, reader.getLine());
        assertEquals(0, reader.getCol());
    }

    @Test
    public void testGetLastSnippet() throws IOException {
        // read first 5 characters: "Hello"
        for (int i = 0; i < 5; i++) {
            reader.read();
        }
        String snippet = reader.getLastSnippet();
        assertEquals("Hello", snippet);
    }

    @Test
    public void testCloseAndSubsequentRead() throws IOException {
        reader.close();
        try {
            reader.read();
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastReader stream is closed.", e.getMessage());
        }
    }

    // ------------------- edge case & exception tests --------------------

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSize() {
        new FastReader(new StringReader("test"), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidPushbackSize() {
        new FastReader(new StringReader("test"), 10, -1);
    }

    @Test
    public void testPushbackBufferOverflow() throws IOException {
        // create a reader with a tiny pushback buffer (size = 2)
        FastReader tinyPushback = new FastReader(new StringReader("ab"), 10, 2);
        // push back two characters – should be fine
        tinyPushback.pushback('x');
        tinyPushback.pushback('y');
        // third pushback must fail
        try {
            tinyPushback.pushback('z');
            fail("Expected IOException due to pushback buffer overflow");
        } catch (IOException e) {
            assertEquals("Pushback buffer overflow", e.getMessage());
        }
    }

    @Test
    public void testReadAfterEndOfStream() throws IOException {
        // consume the whole content
        char[] buffer = new char[100];
        int total = 0;
        int n;
        while ((n = reader.read(buffer, 0, buffer.length)) != -1) {
            total += n;
        }
        assertTrue(total > 0);

        // subsequent single‑character read returns -1
        assertEquals(-1, reader.read());

        // subsequent array read returns -1 as well
        assertEquals(-1, reader.read(new char[10], 0, 10));
    }

    @Test
    public void testReadCharArrayWhenPushbackPresent() throws IOException {
        // push back two characters before any normal read
        reader.pushback('Z');
        reader.pushback('Y');

        char[] buf = new char[4];
        int read = reader.read(buf, 0, 4);
        // first two chars come from pushback (Y, Z) – note order: pushback buffer is LIFO
        assertEquals(4, read);
        assertEquals('Y', buf[0]);
        assertEquals('Z', buf[1]);
        // the next two chars are from the underlying source ("He")
        assertEquals('H', buf[2]);
        assertEquals('e', buf[3]);
    }

    @Test
    public void testReadCharArrayWithZeroLength() throws IOException {
        char[] buf = new char[10];
        int read = reader.read(buf, 0, 0);
        assertEquals(0, read);
        // buffer should remain unchanged
        for (char c : buf) {
            assertEquals('\u0000', c);
        }
    }

    @Test
    public void testReadCharArrayWithNegativeLength() {
        char[] buf = new char[5];
        try {
            reader.read(buf, 0, -1);
            fail("Expected ArrayIndexOutOfBoundsException or IllegalArgumentException");
        } catch (Exception e) {
            // The implementation does not guard against negative length,
            // so the JVM will throw an ArrayIndexOutOfBoundsException.
            assertTrue(e instanceof ArrayIndexOutOfBoundsException ||
                       e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testCustomBufferSizes() throws IOException {
        // custom buffers: 8 chars read buffer, pushback buffer of size 4
        FastReader custom = new FastReader(new StringReader("abcdEFGH"), 8, 4);
        char[] buf = new char[8];
        int read = custom.read(buf, 0, 8);
        assertEquals(8, read);
        assertArrayEquals("abcdEFGH".toCharArray(), buf);
        custom.close();
    }
}
