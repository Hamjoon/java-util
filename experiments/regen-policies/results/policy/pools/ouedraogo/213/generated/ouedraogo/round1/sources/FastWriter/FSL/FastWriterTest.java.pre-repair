package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link FastWriter}.
 * The tests exercise constructors, all overloads of {@code write},
 * as well as {@code flush} and {@code close} handling of edge cases.
 */
public class FastWriterTest {

    /*--------------------------------------------------------------------
     * Constructor tests
     *--------------------------------------------------------------------*/

    @Test
    public void testDefaultConstructorCreatesBuffer() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw);           // uses default 8192 size
        fw.write('a');
        fw.flush();                                   // forces buffer to underlying writer
        assertEquals("a", sw.toString());
        fw.close();
    }

    @Test
    public void testCustomBufferSize() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 10);       // tiny buffer for easier testing
        for (int i = 0; i < 9; i++) {
            fw.write('x');
        }
        // nothing flushed yet
        assertEquals("", sw.toString());

        // writing the 10th char triggers flush of the previous 9 chars
        fw.write('y');
        fw.flush();
        assertEquals("xxxxxxxxxy", sw.toString());
        fw.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroBuffer() {
        new FastWriter(new StringWriter(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeBuffer() {
        new FastWriter(new StringWriter(), -5);
    }

    /*--------------------------------------------------------------------
     * write(int) tests
     *--------------------------------------------------------------------*/

    @Test
    public void testWriteIntSingleChar() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4);
        fw.write('A');
        fw.flush();
        assertEquals("A", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteIntBufferOverflow() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 3); // very small buffer
        fw.write('1'); // buffer: 1
        fw.write('2'); // buffer: 12
        fw.write('3'); // buffer: 123 (full)
        // next write forces flush of previous 3 chars
        fw.write('4'); // buffer now contains only '4'
        fw.flush();    // flush remaining '4'
        assertEquals("1234", sw.toString());
        fw.close();
    }

    @Test(expected = IOException.class)
    public void testWriteIntAfterCloseThrows() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw);
        fw.close();
        fw.write('x'); // should throw because underlying stream is closed
    }

    /*--------------------------------------------------------------------
     * write(char[], off, len) tests
     *--------------------------------------------------------------------*/

    @Test
    public void testWriteCharArrayFitsInBuffer() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5);
        char[] data = "abc".toCharArray();
        fw.write(data, 0, data.length); // fits
        fw.flush();
        assertEquals("abc", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteCharArrayExceedsBufferDirectWrite() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4);
        char[] data = "123456".toCharArray(); // length 6 > buffer size 4
        fw.write(data, 0, data.length); // should flush buffer (empty) then write directly
        fw.flush(); // ensure any remaining data is flushed
        assertEquals("123456", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteCharArrayZeroLengthDoesNothing() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4);
        char[] data = "hello".toCharArray();
        fw.write(data, 2, 0); // len == 0
        fw.flush();
        assertEquals("", sw.toString());
        fw.close();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeOffset() throws IOException {
        FastWriter fw = new FastWriter(new StringWriter(), 4);
        fw.write("test".toCharArray(), -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOffsetTooLarge() throws IOException {
        FastWriter fw = new FastWriter(new StringWriter(), 4);
        fw.write("test".toCharArray(), 5, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayLengthTooLarge() throws IOException {
        FastWriter fw = new FastWriter(new StringWriter(), 4);
        fw.write("test".toCharArray(), 1, 4); // off+len =5 > array length 4
    }

    /*--------------------------------------------------------------------
     * write(String, off, len) tests
     *--------------------------------------------------------------------*/

    @Test
    public void testWriteStringZeroLength() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4);
        fw.write("", 0, 0); // nothing should happen
        fw.flush();
        assertEquals("", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteStringFitsInBuffer() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 10);
        String src = "quick";
        fw.write(src, 0, src.length()); // fits
        fw.flush();
        assertEquals("quick", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteStringLongerThanBuffer() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4); // buffer of 4 chars

        // 10‑character string, will require several flushes
        String src = "ABCDEFGHIJ";
        fw.write(src, 0, src.length());

        // At this point the writer may still have unwritten chars in its buffer,
        // so we call flush to push everything.
        fw.flush();

        assertEquals("ABCDEFGHIJ", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteStringPartialBufferThenDirectChunks() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5); // small buffer

        // First write fills the buffer partially, then we write a long string.
        fw.write("12", 0, 2); // buffer now has "12"
        fw.write("ABCDEFGHIJ", 0, 10); // length 10 > buffer size

        // Expected flow:
        // - "12" + first 3 chars of long string fill buffer (5) -> flushed => "12ABC"
        // - remaining 7 chars written in 5‑char chunk then 2‑char remainder
        // Final output should be "12ABCDEFGHIJ"
        fw.flush();
        assertEquals("12ABCDEFGHIJ", sw.toString());
        fw.close();
    }

    @Test(expected = IOException.class)
    public void testWriteStringAfterCloseThrows() throws IOException {
        FastWriter fw = new FastWriter(new StringWriter());
        fw.close();
        fw.write("oops", 0, 4);
    }

    /*--------------------------------------------------------------------
     * flush() tests
     *--------------------------------------------------------------------*/

    @Test
    public void testFlushPushesBufferedData() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 8);
        fw.write("hello".toCharArray(), 0, 5); // buffered, not yet written
        // no output yet
        assertEquals("", sw.toString());
        fw.flush(); // forces write
        assertEquals("hello", sw.toString());
        fw.close();
    }

    /*--------------------------------------------------------------------
     * close() tests
     *--------------------------------------------------------------------*/

    @Test
    public void testCloseFlushesBufferAndClosesUnderlyingWriter() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 6);
        fw.write("12345".toCharArray(), 0, 5); // stays in buffer
        fw.close(); // should flush "12345" and close the StringWriter

        // StringWriter remains usable after close, but its content must be present
        assertEquals("12345", sw.toString());

        // Subsequent write attempts must fail with IOException
        try {
            fw.write('x');
            fail("Expected IOException after close");
        } catch (IOException ignored) {
        }
    }

    @Test
    public void testCloseIdempotentWhenAlreadyClosed() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw);
        fw.close(); // first close
        // second close should be a no‑op and not throw
        fw.close();
    }
}
