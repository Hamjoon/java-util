package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import static org.junit.Assert.*;

public class FastReaderTest {

    private static final String SIMPLE_TEXT = "Hello World";
    private static final String MULTILINE_TEXT = "First line\nSecond line\nThird line";

    private FastReader reader;
    private Reader underlying;

    @Before
    public void setUp() {
        // default reader used by most tests – can be re‑assigned in individual tests
        underlying = new StringReader(SIMPLE_TEXT);
        reader = new FastReader(underlying);
    }

    @After
    public void tearDown() throws IOException {
        if (reader != null) {
            reader.close();
        }
    }

    /*** Constructor validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSize() {
        new FastReader(new StringReader("x"), 0, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidPushbackSize() {
        new FastReader(new StringReader("x"), 10, -1);
    }

    /*** Single‑character read, line/column tracking and EOF ***/
    @Test
    public void testReadSingleCharAndPosition() throws IOException {
        // read all characters of SIMPLE_TEXT
        for (int i = 0; i < SIMPLE_TEXT.length(); i++) {
            int ch = reader.read();
            assertEquals(SIMPLE_TEXT.charAt(i), (char) ch);
            assertEquals(1, reader.getLine());               // no newlines → line stays 1
            assertEquals(i + 1, reader.getCol());            // column increments
        }
        // EOF
        assertEquals(-1, reader.read());
        // after EOF line/col stay unchanged
        assertEquals(1, reader.getLine());
        assertEquals(SIMPLE_TEXT.length(), reader.getCol());
    }

    /*** Read with newline characters – verify line/column handling ***/
    @Test
    public void testReadWithNewLines() throws IOException {
        reader.close(); // close default and replace with multiline source
        underlying = new StringReader(MULTILINE_TEXT);
        reader = new FastReader(underlying, 8, 4); // small buffers to force multiple fills

        int pos = 0;
        int expectedLine = 1;
        int expectedCol = 0;

        while (true) {
            int ch = reader.read();
            if (ch == -1) break;

            char c = (char) ch;
            if (c == '\n') {
                expectedLine++;
                expectedCol = 0;
            } else {
                expectedCol++;
            }
            pos++;

            assertEquals(expectedLine, reader.getLine());
            assertEquals(expectedCol, reader.getCol());
        }

        // Verify total length read matches source
        assertEquals(MULTILINE_TEXT.length(), pos);
        // After full read, line should be 3 (last line) and column equal to length of last line
        assertEquals(3, reader.getLine());
        assertEquals("Third line".length(), reader.getCol());
    }

    /*** Pushback of characters and its effect on line/column ***/
    @Test
    public void testPushbackAndRead() throws IOException {
        // read first three characters: 'H','e','l'
        assertEquals('H', reader.read());
        assertEquals('e', reader.read());
        assertEquals('l', reader.read());

        // push back the last two characters
        reader.pushback('l');
        reader.pushback('e');

        // after pushback, line/col should have been decremented
        assertEquals(1, reader.getLine());
        assertEquals(2, reader.getCol()); // position back at after 'H'

        // read again – should get 'e' then 'l' again
        assertEquals('e', reader.read());
        assertEquals('l', reader.read());

        // column now at 4 (H e l l)
        assertEquals(4, reader.getCol());
    }

    /*** Pushback overflow – exceeds configured pushback buffer ***/
    @Test
    public void testPushbackOverflow() throws IOException {
        // Use a small pushback buffer to trigger overflow quickly
        reader.close();
        underlying = new StringReader("abc");
        reader = new FastReader(underlying, 4, 2); // pushback buffer size = 2

        // push back two characters – should succeed
        reader.pushback('x');
        reader.pushback('y');

        // third pushback must fail
        try {
            reader.pushback('z');
            fail("Expected IOException due to pushback buffer overflow");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Pushback buffer overflow"));
        }
    }

    /*** read(char[],off,len) – normal operation mixing pushback and underlying buffer ***/
    @Test
    public void testReadCharArrayMixedSources() throws IOException {
        // small buffers to force many fill cycles
        reader.close();
        underlying = new StringReader("0123456789");
        reader = new FastReader(underlying, 3, 2); // bufferSize=3, pushbackSize=2

        // push back two chars that will be read first
        reader.pushback('A');
        reader.pushback('B');

        char[] target = new char[7];
        int read = reader.read(target, 1, 5); // leave slot 0 untouched

        assertEquals(5, read);
        // Expected sequence: B A 0 1 2
        assertEquals('\0', target[0]); // untouched
        assertArrayEquals(new char[]{'B', 'A', '0', '1', '2', '\0', '\0'}, target);
    }

    /*** read(char[],off,len) – EOF handling ***/
    @Test
    public void testReadCharArrayEOF() throws IOException {
        reader.close();
        underlying = new StringReader("xy");
        reader = new FastReader(underlying, 4, 2);

        char[] buf = new char[5];
        int first = reader.read(buf, 0, 5);
        assertEquals(2, first);
        assertArrayEquals(new char[]{'x', 'y', '\0', '\0', '\0'}, buf);

        // subsequent read should return -1
        int second = reader.read(buf, 0, 5);
        assertEquals(-1, second);
    }

    /*** close() and subsequent read attempts ***/
    @Test
    public void testCloseAndReadAfterClose() throws IOException {
        reader.close();
        try {
            reader.read();
            fail("Expected IOException after closing the stream");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("FastReader stream is closed"));
        }

        try {
            reader.read(new char[1], 0, 1);
            fail("Expected IOException after closing the stream");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("FastReader stream is closed"));
        }
    }

    /*** getLastSnippet() – returns characters read so far from the internal buffer ***/
    @Test
    public void testGetLastSnippet() throws IOException {
        // use a small buffer so that the internal buffer is filled multiple times
        reader.close();
        underlying = new StringReader("abcdefg");
        reader = new FastReader(underlying, 4, 2);

        // read first three characters
        assertEquals('a', reader.read());
        assertEquals('b', reader.read());
        assertEquals('c', reader.read());

        // getLastSnippet should contain exactly those three chars
        assertEquals("abc", reader.getLastSnippet());

        // read two more – cause a refill of the internal buffer
        assertEquals('d', reader.read());
        assertEquals('e', reader.read());

        // snippet now contains the first five characters read
        assertEquals("abcde", reader.getLastSnippet());
    }

    /*** movePosition is protected – verify its contract via a subclass that exposes it ***/
    @Test
    public void testProtectedMovePositionViaSubclass() throws IOException {
        class TestFastReader extends FastReader {
            TestFastReader(Reader in) { super(in); }
            void exposeMove(char ch) { super.movePosition(ch); }
        }

        TestFastReader fr = new TestFastReader(new StringReader("\nX"));
        // initial line/col
        assertEquals(1, fr.getLine());
        assertEquals(0, fr.getCol());

        // simulate reading a newline via protected method
        fr.exposeMove('\n');
        assertEquals(2, fr.getLine());
        assertEquals(0, fr.getCol());

        // simulate reading a normal character
        fr.exposeMove('X');
        assertEquals(2, fr.getLine());
        assertEquals(1, fr.getCol());
    }
}
