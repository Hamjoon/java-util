package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link FastReader}.
 */
public class FastReaderTest {

    private static final String SAMPLE_TEXT = "abc\n123\nxyz";

    private Reader stringReader;
    private FastReader fastReader;

    @Before
    public void setUp() {
        // a fresh StringReader for each test (unless a test creates its own)
        stringReader = new StringReader(SAMPLE_TEXT);
        // use a small buffer to force multiple fill() calls and test edge conditions
        fastReader = new FastReader(stringReader, 4, 3);
    }

    @After
    public void tearDown() throws IOException {
        if (fastReader != null) {
            fastReader.close();
        }
    }

    /* --------------------------------------------------------------------- */
    /* Constructor validation                                                */
    /* --------------------------------------------------------------------- */

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
        new FastReader(new StringReader("test"), 5, -1);
    }

    /* --------------------------------------------------------------------- */
    /* Single‑character read and line/column tracking                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testReadSingleCharactersAndLineColTracking() throws IOException {
        // read 'a'
        assertEquals('a', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(1, fastReader.getCol());

        // read 'b'
        assertEquals('b', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(2, fastReader.getCol());

        // read 'c'
        assertEquals('c', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(3, fastReader.getCol());

        // read newline
        assertEquals('\n', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // read '1'
        assertEquals('1', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(1, fastReader.getCol());
    }

    /* --------------------------------------------------------------------- */
    /* Pushback functionality                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPushbackSingleCharacter() throws IOException {
        // read two chars
        assertEquals('a', fastReader.read()); // line 1 col 1
        assertEquals('b', fastReader.read()); // line 1 col 2

        // push back 'b'
        fastReader.pushback('b');
        assertEquals(1, fastReader.getLine());
        assertEquals(1, fastReader.getCol()); // col decremented

        // next read should return the pushed‑back 'b'
        assertEquals('b', fastReader.read());
        assertEquals(1, fastReader.getLine());
        assertEquals(2, fastReader.getCol());
    }

    @Test
    public void testPushbackNewlineAdjustsLineAndCol() throws IOException {
        // read until first newline
        fastReader.read(); // a
        fastReader.read(); // b
        fastReader.read(); // c
        fastReader.read(); // '\n' => line 2, col 0

        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // push back newline
        fastReader.pushback('\n');
        // line should have been decremented
        assertEquals(1, fastReader.getLine());
        assertEquals(0, fastReader.getCol());

        // read it again
        assertEquals('\n', fastReader.read());
        assertEquals(2, fastReader.getLine());
        assertEquals(0, fastReader.getCol());
    }

    @Test(expected = IOException.class)
    public void testPushbackBufferOverflow() throws IOException {
        // pushback buffer size is 3; fill it completely
        fastReader.pushback('x');
        fastReader.pushback('y');
        fastReader.pushback('z');
        // this fourth pushback must fail
        fastReader.pushback('w');
    }

    /* --------------------------------------------------------------------- */
    /* Bulk read (char[], off, len)                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testReadIntoCharArrayWithoutPushback() throws IOException {
        char[] buffer = new char[10];
        int n = fastReader.read(buffer, 0, 5); // should read "abc\n1"
        assertEquals(5, n);
        assertArrayEquals(new char[]{'a', 'b', 'c', '\n', '1', '\0', '\0', '\0', '\0', '\0'}, buffer);
        // after reading 5 chars, line should be 2 (because of the newline) and col 1
        assertEquals(2, fastReader.getLine());
        assertEquals(1, fastReader.getCol());
    }

    @Test
    public void testReadIntoCharArrayWithPushbackFirst() throws IOException {
        // read one char, then push it back
        assertEquals('a', fastReader.read());
        fastReader.pushback('a');

        char[] buf = new char[4];
        int n = fastReader.read(buf, 1, 3); // should first return the pushed‑back 'a' then 'b' and 'c'
        assertEquals(3, n);
        assertEquals('\0', buf[0]); // untouched
        assertArrayEquals(new char[]{'\0', 'a', 'b', 'c'}, buf);
        assertEquals(1, fastReader.getLine());
        assertEquals(3, fastReader.getCol()); // after 'c' col = 3
    }

    @Test
    public void testReadCharArrayReturnsMinusOneAtEof() throws IOException {
        // consume the whole content
        char[] all = new char[100];
        int total = fastReader.read(all, 0, all.length);
        assertTrue(total > 0);
        // further read should return -1
        assertEquals(-1, fastReader.read(all, 0, 1));
    }

    /* --------------------------------------------------------------------- */
    /* getLastSnippet behavior                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetLastSnippetReflectsBufferContent() throws IOException {
        // buffer size is 4; after reading 3 chars we are still within the first fill
        fastReader.read(); // a
        fastReader.read(); // b
        fastReader.read(); // c
        String snippet = fastReader.getLastSnippet();
        // snippet should contain the characters that have been placed into the internal buffer
        // up to the current position (which is 3)
        assertEquals("abc", snippet);
    }

    @Test
    public void testGetLastSnippetAfterRefill() throws IOException {
        // read more than the initial buffer size to force a refill
        fastReader.read(); // a
        fastReader.read(); // b
        fastReader.read(); // c
        fastReader.read(); // \n (fills buffer of size 4)
        fastReader.read(); // 1 (causes refill)
        String snippet = fastReader.getLastSnippet();
        // After the refill, position points inside the second buffer,
        // so snippet should contain the characters from the second buffer up to the current position.
        // The second buffer starts with '1','2','3','\n' – we have read only the first character ('1')
        assertEquals("1", snippet);
    }

    /* --------------------------------------------------------------------- */
    /* Close behavior                                                         */
    /* --------------------------------------------------------------------- */

    @Test(expected = IOException.class)
    public void testReadAfterCloseThrows() throws IOException {
        fastReader.close();
        fastReader.read();
    }

    @Test(expected = IOException.class)
    public void testReadArrayAfterCloseThrows() throws IOException {
        fastReader.close();
        fastReader.read(new char[1], 0, 1);
    }

    /* --------------------------------------------------------------------- */
    /* Additional edge cases                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testZeroLengthReadReturnsZero() throws IOException {
        char[] buf = new char[5];
        int n = fastReader.read(buf, 0, 0);
        assertEquals(0, n);
    }

    @Test(expected = IOException.class)
    public void testReadArrayWithNullBufferThrows() throws IOException {
        fastReader.read(null, 0, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadArrayWithInvalidOffsetThrows() throws IOException {
        char[] buf = new char[3];
        fastReader.read(buf, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testReadArrayWithInvalidLengthThrows() throws IOException {
        char[] buf = new char[3];
        fastReader.read(buf, 0, 4); // length exceeds buffer size
    }
}
