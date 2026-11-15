package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * JUnit‑4 test suite for {@link FastReader}.
 *
 * The tests are organised in the following sections:
 *
 * 1.  Public API extraction (for documentation purposes only).
 * 2.  Basic functionality tests – one test per public method that verifies the
 *     “happy path”.
 * 3.  Edge‑case / exception tests – covering illegal arguments, EOF handling,
 *     push‑back overflow, line/column tracking, array‑read corner cases, etc.
 * 4.  Combined test class – a single, compilable JUnit‑4 source file.
 *
 * The class is deliberately exercised with tiny internal buffers so that
 * refilling, push‑back handling and line/column bookkeeping are exercised many
 * times during a single test run.
 */
public class FastReaderTest {

    /* --------------------------------------------------------------------
     * 1.  PUBLIC API extracted from FastReader
     * --------------------------------------------------------------------
     *
     * public FastReader(Reader in, int bufferSize, int pushbackBufferSize)
     * public void      pushback(char ch)               throws IOException
     * public int       read()                         throws IOException
     * public int       read(char[] cbuf, int off, int len) throws IOException
     * public void      close()                        throws IOException
     * public int       getLine()
     * public int       getCol()
     * public String    getLastSnippet()
     *
     */

    private static final String SAMPLE_TEXT = "Line1\nLine2\nLine3";

    /** Helper to create a FastReader with small buffers – this forces many
     *  internal fill‑operations and makes push‑back behaviour easier to test. */
    private FastReader createReader(String data, int bufferSize, int pushbackSize) {
        Reader src = new StringReader(data);
        return new FastReader(src, bufferSize, pushbackSize);
    }

    /* --------------------------------------------------------------------
     * 2.  BASIC FUNCTIONALITY TESTS
     * -------------------------------------------------------------------- */

    @Test
    public void testReadSingleCharacters() throws IOException {
        FastReader fr = createReader("abc", 2, 2);
        assertEquals('a', fr.read());
        assertEquals('b', fr.read());
        assertEquals('c', fr.read());

        // after three reads we are at EOF
        assertEquals(-1, fr.read());
    }

    @Test
    public void testReadCharArraySimple() throws IOException {
        FastReader fr = createReader("hello", 4, 2);
        char[] buf = new char[5];
        int n = fr.read(buf, 0, 5);
        assertEquals(5, n);
        assertArrayEquals("hello".toCharArray(), buf);
    }

    @Test
    public void testPushbackAndReadAgain() throws IOException {
        FastReader fr = createReader("xyz", 2, 2);
        int first = fr.read();               // 'x'
        assertEquals('x', first);
        fr.pushback((char) first);           // push back 'x'
        int second = fr.read();              // should get the same 'x' again
        assertEquals('x', second);
    }

    @Test
    public void testLineAndColumnTracking() throws IOException {
        FastReader fr = createReader("a\nb", 2, 2);
        assertEquals(1, fr.getLine());
        assertEquals(0, fr.getCol());

        fr.read();               // 'a'
        assertEquals(1, fr.getLine());
        assertEquals(1, fr.getCol());

        fr.read();               // '\n'
        assertEquals(2, fr.getLine());
        assertEquals(0, fr.getCol());

        fr.read();               // 'b'
        assertEquals(2, fr.getLine());
        assertEquals(1, fr.getCol());
    }

    @Test
    public void testGetLastSnippet() throws IOException {
        FastReader fr = createReader("hello world", 5, 2);
        // read first 5 characters – fills the internal buffer once
        char[] tmp = new char[5];
        fr.read(tmp, 0, 5);
        assertEquals("hello", fr.getLastSnippet());

        // read next 5 characters – buffer refills, position resets
        fr.read(tmp, 0, 5);
        assertEquals(" worl", fr.getLastSnippet()); // note leading space
    }

    @Test
    public void testCloseBehaviour() throws IOException {
        FastReader fr = createReader("data", 2, 2);
        fr.close();
        try {
            fr.read();
            fail("Expected IOException after closing the reader");
        } catch (IOException expected) {
            // expected
        }
    }

    /* --------------------------------------------------------------------
     * 3.  EDGE‑CASE / EXCEPTION TESTS
     * -------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSizeZero() {
        new FastReader(new StringReader("x"), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSizeNegative() {
        new FastReader(new StringReader("x"), -5, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidPushbackNegative() {
        new FastReader(new StringReader("x"), 2, -1);
    }

    @Test
    public void testPushbackOverflow() throws IOException {
        FastReader fr = createReader("a", 2, 1); // pushback buffer can hold only 1 char
        fr.read();                               // consume 'a'
        fr.pushback('a');                        // first pushback – OK
        try {
            fr.pushback('b');                    // second pushback – should overflow
            fail("Expected IOException due to pushback buffer overflow");
        } catch (IOException e) {
            assertEquals("Pushback buffer overflow", e.getMessage());
        }
    }

    @Test
    public void testPushbackNewlineDecrementsLine() throws IOException {
        FastReader fr = createReader("x\n", 2, 2);
        fr.read();               // 'x'
        fr.read();               // '\n' → line becomes 2
        assertEquals(2, fr.getLine());

        fr.pushback('\n');       // should decrement line back to 1
        assertEquals(1, fr.getLine());
    }

    @Test
    public void testReadCharArrayWithZeroLength() throws IOException {
        FastReader fr = createReader("abc", 2, 2);
        char[] buf = new char[10];
        int n = fr.read(buf, 0, 0);
        assertEquals(0, n);
        // buffer must stay unchanged
        assertArrayEquals(new char[10], buf);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testReadCharArrayNegativeOffset() throws IOException {
        FastReader fr = createReader("abc", 2, 2);
        char[] buf = new char[5];
        fr.read(buf, -1, 3);   // illegal offset → should throw AIOOBE from System.arraycopy
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testReadCharArrayNegativeLength() throws IOException {
        FastReader fr = createReader("abc", 2, 2);
        char[] buf = new char[5];
        fr.read(buf, 0, -2);   // illegal length → should throw AIOOBE
    }

    @Test
    public void testReadCharArrayAcrossPushbackAndBuffer() throws IOException {
        // bufferSize=2, pushbackSize=2, source = "12AB"
        FastReader fr = createReader("12AB", 2, 2);
        // read two chars from underlying buffer
        assertEquals('1', fr.read());
        assertEquals('2', fr.read());
        // push them back
        fr.pushback('2');
        fr.pushback('1');

        char[] out = new char[4];
        int n = fr.read(out, 0, 4);
        assertEquals(4, n);
        assertArrayEquals("12AB".toCharArray(), out);
    }

    @Test
    public void testReadReturnsMinusOneAtEOF() throws IOException {
        FastReader fr = createReader("z", 2, 2);
        assertEquals('z', fr.read());
        assertEquals(-1, fr.read()); // first EOF
        assertEquals(-1, fr.read()); // subsequent reads stay at EOF
    }

    /* --------------------------------------------------------------------
     * 4.  COMPLETED TEST CLASS
     * -------------------------------------------------------------------- */
}
