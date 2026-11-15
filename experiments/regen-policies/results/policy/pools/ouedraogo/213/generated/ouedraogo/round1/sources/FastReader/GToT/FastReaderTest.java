package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * JUnit‑4 test suite for {@link FastReader}.
 *
 * The suite is built step‑by‑step as if three independent experts were
 * contributing one test per public method, then merging the results.
 *
 * It covers:
 * <ul>
 *   <li>All public constructors and methods (signatures listed in the comment).</li>
 *   <li>Typical usage scenarios.</li>
 *   <li>Edge‑case handling (buffer over‑/under‑flow, EOF, illegal arguments).</li>
 *   <li>Exception paths (illegal constructor arguments, pushback overflow, read after close).</li>
 * </ul>
 */
public class FastReaderTest {

    /* --------------------------------------------------------------------- */
    /*  1.  Public API extracted from FastReader (for reference)            */
    /* --------------------------------------------------------------------- */
    // Constructors
    //   FastReader(Reader in)
    //   FastReader(Reader in, int bufferSize, int pushbackBufferSize)

    // Public methods
    //   void pushback(char ch) throws IOException
    //   int read() throws IOException
    //   int read(char[] cbuf, int off, int len) throws IOException
    //   void close() throws IOException
    //   int getLine()
    //   int getCol()
    //   String getLastSnippet()

    /* --------------------------------------------------------------------- */
    /*  2.  Helper fields and common test data                               */
    /* --------------------------------------------------------------------- */
    private static final String SIMPLE_TEXT = "Hello\nWorld\n!";
    private Reader stringReader;
    private FastReader fastReader;

    @Before
    public void setUp() {
        stringReader = new StringReader(SIMPLE_TEXT);
        fastReader = new FastReader(stringReader);   // default buffers (16384,16)
    }

    @After
    public void tearDown() throws IOException {
        fastReader.close();
    }

    /* --------------------------------------------------------------------- */
    /*  3.  Constructor tests                                                */
    /* --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNonPositiveBufferSize() {
        new FastReader(new StringReader("x"), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativePushbackSize() {
        new FastReader(new StringReader("x"), 10, -5);
    }

    @Test
    public void constructorInitialisesStateCorrectly() {
        FastReader r = new FastReader(new StringReader("abc"));
        assertEquals(1, r.getLine());   // line starts at 1
        assertEquals(0, r.getCol());    // column starts at 0
        assertEquals("", r.getLastSnippet());
    }

    /* --------------------------------------------------------------------- */
    /*  4.  pushback(char) tests                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void pushbackStoresCharacterAndAdjustsLineCol() throws IOException {
        // read a newline so line becomes 2, col = 0
        fastReader.read(); // 'H'
        fastReader.read(); // 'e'
        fastReader.read(); // 'l'
        fastReader.read(); // 'l'
        fastReader.read(); // 'o'
        fastReader.read(); // '\n' -> line 2, col 0
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // push back the newline
        fastReader.pushback('\n');
        // after pushback line should be decremented, col decremented (col becomes -1)
        assertEquals(1, fastReader.getLine());
        assertEquals(-1, fastReader.getCol());

        // next read must return the pushed‑back newline
        int ch = fastReader.read();
        assertEquals('\n', ch);
        // line/col are re‑applied by read()
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());
    }

    @Test(expected = IOException.class)
    public void pushbackThrowsOnOverflow() throws IOException {
        // pushback buffer size is 16 by default; fill it completely
        for (int i = 0; i < 16; i++) {
            fastReader.pushback('a');
        }
        // one more pushback must overflow
        fastReader.pushback('b');
    }

    /* --------------------------------------------------------------------- */
    /*  5.  read() (single‑character) tests                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void readSingleCharAdvancesLineAndColumn() throws IOException {
        int ch = fastReader.read(); // 'H'
        assertEquals('H', ch);
        assertEquals(1, fastReader.getLine());
        assertEquals(1, fastReader.getCol());

        // read up to newline
        for (int i = 0; i < 4; i++) fastReader.read(); // 'e','l','l','o'
        assertEquals(1, fastReader.getLine());
        assertEquals(5, fastReader.getCol());

        // read newline
        fastReader.read(); // '\n'
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());
    }

    @Test
    public void readReturnsMinusOneAtEndOfStream() throws IOException {
        // consume whole content
        while (fastReader.read() != -1) { /* loop */ }
        // another read must still be -1
        assertEquals(-1, fastReader.read());
    }

    @Test(expected = IOException.class)
    public void readThrowsAfterClose() throws IOException {
        fastReader.close();
        fastReader.read();
    }

    /* --------------------------------------------------------------------- */
    /*  6.  read(char[],off,len) tests                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void readIntoArrayRespectsOffsetAndLength() throws IOException {
        char[] buf = new char[20];
        int n = fastReader.read(buf, 5, 7); // should read "Hello\nW"
        assertEquals(7, n);
        assertArrayEquals(
                new char[]{0,0,0,0,0,'H','e','l','l','o','\n','W',0,0,0,0,0,0,0,0},
                buf);
        // verify line/col after reading 7 chars (includes newline)
        assertEquals(2, fastReader.getLine());
        assertEquals(1, fastReader.getCol()); // after '\n' col reset, then 'W' -> col 1
    }

    @Test
    public void readArrayReturnsZeroWhenLenIsZero() throws IOException {
        char[] buf = new char[5];
        int n = fastReader.read(buf, 0, 0);
        assertEquals(0, n);
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void readArrayThrowsWhenOffsetTooLarge() throws IOException {
        char[] buf = new char[5];
        fastReader.read(buf, 6, 1); // offset beyond array length
    }

    @Test
    public void readArrayUsesPushbackFirst() throws IOException {
        // push back two characters
        fastReader.pushback('x');
        fastReader.pushback('y');

        char[] buf = new char[3];
        int n = fastReader.read(buf, 0, 3);
        // first two chars are from pushback in LIFO order: 'y','x'
        assertEquals(3, n);
        assertEquals('y', buf[0]);
        assertEquals('x', buf[1]);
        assertEquals('H', buf[2]); // original stream's first char
    }

    @Test
    public void readArrayReturnsMinusOneWhenAtEofAndNoDataRead() throws IOException {
        // consume everything
        while (fastReader.read() != -1) { /* drain */ }
        char[] buf = new char[4];
        int n = fastReader.read(buf, 0, 4);
        assertEquals(-1, n);
    }

    /* --------------------------------------------------------------------- */
    /*  7.  close() tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void closeClearsUnderlyingReader() throws IOException {
        fastReader.close();
        // internal field is set to null; subsequent read must fail
        try {
            fastReader.read();
            fail("Expected IOException after close");
        } catch (IOException e) {
            // expected
        }
    }

    /* --------------------------------------------------------------------- */
    /*  8.  getLine() / getCol() tests                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void lineAndColumnTrackMultipleNewlines() throws IOException {
        // read through the whole string
        while (fastReader.read() != -1) { /* drain */ }
        // Expected line count: 3 (two '\n' characters)
        assertEquals(3, fastReader.getLine());
        // After final '!' column should be 1 (since newline reset column to 0 then '!')
        assertEquals(1, fastReader.getCol());
    }

    /* --------------------------------------------------------------------- */
    /*  9.  getLastSnippet() tests                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void getLastSnippetReturnsCharactersReadSoFar() throws IOException {
        // read first 5 characters ("Hello")
        for (int i = 0; i < 5; i++) fastReader.read();
        String snippet = fastReader.getLastSnippet();
        assertEquals("Hello", snippet);
    }

    @Test
    public void getLastSnippetIsEmptyBeforeAnyRead() {
        assertEquals("", fastReader.getLastSnippet());
    }

    /* --------------------------------------------------------------------- */
    /*  10.  Additional sanity test – large buffers and pushback size       */
    /* --------------------------------------------------------------------- */

    @Test
    public void customBufferSizesWorkCorrectly() throws IOException {
        FastReader r = new FastReader(new StringReader("ABCD"), 2, 2);
        // read two chars -> fills internal buffer of size 2
        assertEquals('A', r.read());
        assertEquals('B', r.read());
        // push back one char
        r.pushback('B');
        // next read must return that pushed char
        assertEquals('B', r.read());
        // continue reading remaining chars
        assertEquals('C', r.read());
        assertEquals('D', r.read());
        assertEquals(-1, r.read());
        r.close();
    }
}
