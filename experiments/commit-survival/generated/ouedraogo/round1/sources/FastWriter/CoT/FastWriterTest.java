package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link FastWriter}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and methods.</li>
 *   <li>Normal operation.</li>
 *   <li>Edge‑cases such as illegal arguments, buffer overflow handling and
 *       behaviour after the writer has been closed.</li>
 * </ul>
 */
public class FastWriterTest {

    private StringWriter backingWriter;
    private FastWriter fastWriter;

    @Before
    public void setUp() {
        backingWriter = new StringWriter();
    }

    @After
    public void tearDown() throws IOException {
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    /* --------------------------------------------------------------------- */
    /*  PUBLIC CONSTRUCTORS                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConstructorDefaultBufferSize() throws IOException {
        fastWriter = new FastWriter(backingWriter);
        fastWriter.write('A');
        fastWriter.flush();
        assertEquals("A", backingWriter.toString());
    }

    @Test
    public void testConstructorCustomBufferSize() throws IOException {
        // small buffer to force multiple flushes
        fastWriter = new FastWriter(backingWriter, 4);
        fastWriter.write('a');
        fastWriter.write('b');
        fastWriter.write('c');
        fastWriter.write('d'); // buffer now full, not yet flushed
        fastWriter.write('e'); // triggers flush of first 4 chars
        fastWriter.flush();
        assertEquals("abcde", backingWriter.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSize() {
        fastWriter = new FastWriter(backingWriter, 0);
    }

    /* --------------------------------------------------------------------- */
    /*  write(int)                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testWriteIntSingleChar() throws IOException {
        fastWriter = new FastWriter(backingWriter);
        fastWriter.write('Z');
        fastWriter.flush();
        assertEquals("Z", backingWriter.toString());
    }

    @Test
    public void testWriteIntBufferOverflow() throws IOException {
        fastWriter = new FastWriter(backingWriter, 3);
        fastWriter.write('1');
        fastWriter.write('2');
        fastWriter.write('3'); // buffer full now
        fastWriter.write('4'); // should flush first three chars
        fastWriter.flush();
        assertEquals("1234", backingWriter.toString());
    }

    @Test(expected = IOException.class)
    public void testWriteIntAfterCloseThrowsIOException() throws IOException {
        fastWriter = new FastWriter(backingWriter);
        fastWriter.close();
        fastWriter.write('X'); // should fail because underlying writer is null
    }

    /* --------------------------------------------------------------------- */
    /*  write(char[] , int , int)                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testWriteCharArrayValid() throws IOException {
        fastWriter = new FastWriter(backingWriter, 10);
        char[] data = "HelloWorld".toCharArray();
        fastWriter.write(data, 2, 5); // "lloWo"
        fastWriter.flush();
        assertEquals("lloWo", backingWriter.toString());
    }

    @Test
    public void testWriteCharArrayZeroLength() throws IOException {
        fastWriter = new FastWriter(backingWriter);
        char[] data = "nothing".toCharArray();
        fastWriter.write(data, 3, 0); // no-op
        fastWriter.flush();
        assertEquals("", backingWriter.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeOffset() throws IOException {
        fastWriter = new FastWriter(backingWriter);
        char[] data = "abc".toCharArray();
        fastWriter.write(data, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOffsetBeyondArray() throws IOException {
        fastWriter = new FastWriter(backingWriter);
        char[] data = "abc".toCharArray();
        fastWriter.write(data, 4, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeLength() throws IOException {
        fastWriter = new FastWriter(backingWriter);
        char[] data = "abc".toCharArray();
        fastWriter.write(data, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayLengthExceedsArray() throws IOException {
        fastWriter = new FastWriter(backingWriter);
        char[] data = "abc".toCharArray();
        fastWriter.write(data, 1, 3); // 1+3 > data.length
    }

    @Test
    public void testWriteCharArrayLenGreaterThanBuffer() throws IOException {
        // buffer size = 4, len = 6 -> should bypass internal buffer
        fastWriter = new FastWriter(backingWriter, 4);
        char[] data = "123456".toCharArray();
        fastWriter.write(data, 0, 6);
        fastWriter.flush(); // flush() is a no‑op because buffer is empty
        assertEquals("123456", backingWriter.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  write(String , int , int)                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testWriteStringAcrossBufferBoundaries() throws IOException {
        fastWriter = new FastWriter(backingWriter, 5);
        fastWriter.write("ABCDEFGHIJ", 0, 10); // 10 chars, buffer size 5 -> 2 flushes
        fastWriter.flush();
        assertEquals("ABCDEFGHIJ", backingWriter.toString());
    }

    @Test(expected = IOException.class)
    public void testWriteStringAfterCloseThrowsIOException() throws IOException {
        fastWriter = new FastWriter(backingWriter);
        fastWriter.close();
        fastWriter.write("test", 0, 4);
    }

    /* --------------------------------------------------------------------- */
    /*  flush()                                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testFlushWritesBufferedData() throws IOException {
        fastWriter = new FastWriter(backingWriter, 8);
        fastWriter.write('x');
        fastWriter.write('y');
        // nothing flushed yet
        fastWriter.flush(); // should push "xy" to the underlying writer
        assertEquals("xy", backingWriter.toString());
    }

    /* --------------------------------------------------------------------- */
    /*  close()                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCloseFlushesAndPreventsFurtherWrites() throws IOException {
        fastWriter = new FastWriter(backingWriter, 5);
        fastWriter.write('p');
        fastWriter.write('q');
        fastWriter.close(); // should flush "pq"
        assertEquals("pq", backingWriter.toString());

        // any subsequent write must fail
        try {
            fastWriter.write('r');
            fail("Expected IOException after close");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        fastWriter = new FastWriter(backingWriter);
        fastWriter.write('z');
        fastWriter.close(); // first close
        // second close must be a no‑op and must not throw
        fastWriter.close();
        assertEquals("z", backingWriter.toString());
    }
}
