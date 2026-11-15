package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link FastReader}.
 *
 * The tests are organised as follows:
 *
 * 1.  Public API extraction (for documentation purposes only).
 * 2.  Basic functional tests – one test per public method that checks the
 *     normal (happy‑path) behaviour.
 * 3.  Edge‑case / exception tests – covering constructor validation,
 *     push‑back overflow, reading after close, EOF handling, line/column
 *     tracking, etc.
 *
 * The test class is completely self‑contained and does not use any
 * mocking framework – it builds real {@link java.io.Reader} instances
 * (StringReader) as required by the {@code FastReader} API.
 */
public class FastReaderTest
{
    /* -----------------------------------------------------------------
     *  1.  Public methods of FastReader (extracted for reference)
     * -----------------------------------------------------------------
     *
     *  public FastReader(Reader in, int bufferSize, int pushbackBufferSize)
     *  public void          pushback(char ch) throws IOException
     *  public int           read() throws IOException
     *  public int           read(char[] cbuf, int off, int len) throws IOException
     *  public void          close() throws IOException
     *  public int           getLine()
     *  public int           getCol()
     *  public String        getLastSnippet()
     *
     * ----------------------------------------------------------------- */

    private static final int DEFAULT_BUF_SIZE = 8;
    private static final int DEFAULT_PUSHBACK_SIZE = 4;

    private Reader source;
    private FastReader fastReader;

    @Before
    public void setUp() {
        // a simple source string used by many tests
        source = new StringReader("Line1\nLine2");
        fastReader = new FastReader(source, DEFAULT_BUF_SIZE, DEFAULT_PUSHBACK_SIZE);
    }

    @After
    public void tearDown() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /* -----------------------------------------------------------------
     *  2.  Basic functional tests
     * ----------------------------------------------------------------- */

    @Test
    public void testConstructorValid() {
        FastReader fr = new FastReader(new StringReader("abc"), 10, 5);
        assertNotNull(fr);
    }

    @Test
    public void testReadSingleChar() throws IOException {
        assertEquals('L', fastReader.read());          // 'L'
        assertEquals('i', fastReader.read());          // 'i'
        assertEquals('n', fastReader.read());          // 'n'
        assertEquals('e', fastReader.read());          // 'e'
        assertEquals('1', fastReader.read());          // '1'
        assertEquals('\n', fastReader.read());         // newline
        assertEquals('L', fastReader.read());          // start of second line
        // ... continue until EOF
        while (fastReader.read() != -1) { /* consume */ }
        assertEquals(-1, fastReader.read());           // EOF
    }

    @Test
    public void testReadUpdatesLineAndColumn() throws IOException {
        // initial line/col
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        fastReader.read(); // 'L'
        assertEquals(1, fastReader.getLine());
        assertEquals(1, fastReader.getCol());

        fastReader.read(); // 'i'
        assertEquals(1, fastReader.getLine());
        assertEquals(2, fastReader.getCol());

        fastReader.read(); // 'n'
        fastReader.read(); // 'e'
        fastReader.read(); // '1'
        fastReader.read(); // '\n' -> line increment, col reset
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        fastReader.read(); // 'L' of second line
        assertEquals(2, fastReader.getLine());
        assertEquals(1, fastReader.getCol());
    }

    @Test
    public void testPushbackAndRead() throws IOException {
        int ch = fastReader.read();          // read 'L'
        assertEquals('L', ch);
        assertEquals(1, fastReader.getCol());

        fastReader.pushback((char) ch);      // push back 'L'
        // line/col should have been decremented
        assertEquals(0, fastReader.getCol());

        int ch2 = fastReader.read();         // read again, should be same char
        assertEquals('L', ch2);
        assertEquals(1, fastReader.getCol());
    }

    @Test
    public void testReadCharArray() throws IOException {
        char[] buffer = new char[20];
        int read = fastReader.read(buffer, 0, 6); // "Line1\n"
        assertEquals(6, read);
        assertArrayEquals(new char[]{'L','i','n','e','1','\n',
                0,0,0,0,0,0,0,0,0,0,0,0,0,0}, buffer);
        // verify that subsequent reads continue correctly
        read = fastReader.read(buffer, 6, 5); // "Line2"
        assertEquals(5, read);
        assertEquals('L', buffer[6]);
        assertEquals('2', buffer[10]);
    }

    @Test
    public void testGetLastSnippet() throws IOException {
        fastReader.read(); // 'L'
        fastReader.read(); // 'i'
        fastReader.read(); // 'n'
        String snippet = fastReader.getLastSnippet();
        assertEquals("Lin", snippet);
    }

    @Test
    public void testCloseAndRead() throws IOException {
        fastReader.close();
        try {
            fastReader.read();
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastReader stream is closed.", e.getMessage());
        }
    }

    /* -----------------------------------------------------------------
     *  3.  Edge‑case / exception tests
     * ----------------------------------------------------------------- */

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

    @Test
    public void testPushbackOverflow() throws IOException {
        // pushback buffer size is DEFAULT_PUSHBACK_SIZE (4)
        // Fill it completely
        fastReader.pushback('a');
        fastReader.pushback('b');
        fastReader.pushback('c');
        fastReader.pushback('d');
        // The next pushback must fail
        try {
            fastReader.pushback('e');
            fail("Expected IOException due to pushback buffer overflow");
        } catch (IOException e) {
            assertEquals("Pushback buffer overflow", e.getMessage());
        }
    }

    @Test
    public void testReadCharArrayZeroLength() throws IOException {
        char[] buf = new char[5];
        int read = fastReader.read(buf, 0, 0);
        assertEquals(0, read);
    }

    @Test
    public void testReadCharArrayEofReturnsMinusOneWhenNoDataRead() throws IOException {
        // Exhaust the underlying source
        while (fastReader.read() != -1) { /* consume */ }

        char[] buf = new char[10];
        int read = fastReader.read(buf, 0, 5);
        assertEquals(-1, read);
    }

    @Test
    public void testReadCharArrayEofReturnsBytesReadWhenPartialData() throws IOException {
        // Read only part of the source
        char[] buf = new char[4];
        int read = fastReader.read(buf, 0, 4); // "Line"
        assertEquals(4, read);
        // Exhaust the rest
        while (fastReader.read() != -1) { /* consume */ }

        // Now request more data – should get -1 because nothing left
        read = fastReader.read(buf, 0, 4);
        assertEquals(-1, read);
    }

    @Test
    public void testLineAndColumnAfterMultiplePushbacks() throws IOException {
        fastReader.read(); // 'L' col=1
        fastReader.read(); // 'i' col=2
        fastReader.pushback('i'); // col back to 1
        fastReader.pushback('L'); // col back to 0
        assertEquals(0, fastReader.getCol());
        assertEquals(1, fastReader.getLine());

        // Pushback a newline – line should decrement
        fastReader.read(); // 'L' again, col=1
        fastReader.read(); // 'i' col=2
        fastReader.read(); // 'n' col=3
        fastReader.read(); // 'e' col=4
        fastReader.read(); // '1' col=5
        fastReader.read(); // '\n' line becomes 2, col=0
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        fastReader.pushback('\n'); // line back to 1, col stays 0 (col-- not executed for newline)
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());
    }
}
