package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

/**
 * JUnit 4 test cases for {@link FastWriter}.
 * These tests exercise all public methods, constructor validation,
 * buffer handling, edge‑cases and proper exception throwing.
 */
public class FastWriterTest {

    // ---------- Constructor validation ----------
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSizeZero() {
        new FastWriter(new StringWriter(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSizeNegative() {
        new FastWriter(new StringWriter(), -5);
    }

    @Test
    public void testConstructorDefaultBufferSize() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw); // uses DEFAULT_BUFFER_SIZE (8192)
        fw.write('a');
        fw.flush();
        assertEquals("a", sw.toString());
        fw.close();
    }

    // ---------- write(int) ----------
    @Test
    public void testWriteIntSingleChar() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4); // small buffer to force flush
        fw.write('x');
        fw.flush();
        assertEquals("x", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteIntBufferOverflow() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 3);
        // Fill buffer
        fw.write('a');
        fw.write('b');
        fw.write('c');
        // Next write should cause flush of previous three chars
        fw.write('d');
        fw.flush();
        assertEquals("abcd", sw.toString());
        fw.close();
    }

    @Test(expected = IOException.class)
    public void testWriteIntAfterCloseThrows() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw);
        fw.close();
        fw.write('z'); // should throw IOException because out == null
    }

    // ---------- write(char[], int, int) ----------
    @Test
    public void testWriteCharArrayZeroLength() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5);
        char[] data = "hello".toCharArray();
        fw.write(data, 2, 0); // no-op
        fw.flush();
        assertEquals("", sw.toString());
        fw.close();
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidOffset() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw);
        char[] data = "abc".toCharArray();
        fw.write(data, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidLength() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw);
        char[] data = "abc".toCharArray();
        fw.write(data, 1, 5); // exceeds array bounds
    }

    @Test
    public void testWriteCharArraySmallerThanBuffer() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 10);
        char[] data = "12345".toCharArray();
        fw.write(data, 0, data.length);
        fw.flush();
        assertEquals("12345", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteCharArrayExactBufferSizeFlushesDirectly() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4);
        char[] data = "ABCD".toCharArray();
        // len == buffer size => should trigger direct write after flushing (buffer empty)
        fw.write(data, 0, data.length);
        fw.flush();
        assertEquals("ABCD", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteCharArrayLargerThanBufferWritesDirectly() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5);
        char[] data = "ABCDEFGHIJ".toCharArray(); // length 10 > buffer size 5
        fw.write(data, 0, data.length);
        fw.flush();
        assertEquals("ABCDEFGHIJ", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteCharArrayCausesIntermediateFlush() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5);
        // First fill half the buffer
        fw.write("12".toCharArray(), 0, 2); // buffer now has "12"
        // Next write needs 4 chars, which does not fit (5 - 2 = 3), so buffer should flush first
        char[] more = "3456".toCharArray(); // 4 chars
        fw.write(more, 0, more.length);
        fw.flush();
        // Expected order: "12" flushed, then "3456" written
        assertEquals("123456", sw.toString());
        fw.close();
    }

    // ---------- write(String, int, int) ----------
    @Test
    public void testWriteStringSimple() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 8);
        fw.write("hello", 0, 5);
        fw.flush();
        assertEquals("hello", sw.toString());
        fw.close();
    }

    @Test
    public void testWriteStringSpanningMultipleFlushes() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4); // tiny buffer
        String src = "ABCDEFGHIJ"; // 10 chars
        fw.write(src, 0, src.length());
        fw.flush();
        assertEquals(src, sw.toString());
        fw.close();
    }

    @Test
    public void testWriteStringPartialOffsets() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 6);
        String src = "0123456789";
        fw.write(src, 2, 5); // writes "23456"
        fw.flush();
        assertEquals("23456", sw.toString());
        fw.close();
    }

    @Test(expected = IOException.class)
    public void testWriteStringAfterCloseThrows() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw);
        fw.close();
        fw.write("oops", 0, 4);
    }

    // ---------- flush ----------
    @Test
    public void testFlushWritesBufferedData() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 10);
        fw.write("abc".toCharArray(), 0, 3); // buffered
        fw.flush(); // should push to underlying writer
        assertEquals("abc", sw.toString());
        fw.close();
    }

    // ---------- close ----------
    @Test
    public void testCloseFlushesRemainingData() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5);
        fw.write("xyz".toCharArray(), 0, 3); // stays in buffer
        fw.close(); // should flush before closing
        assertEquals("xyz", sw.toString());
        // underlying StringWriter remains open; further writes to it should work
        sw.write("extra");
        assertEquals("xyzextra", sw.toString());
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw);
        fw.close(); // first close
        // second close must be a no‑op and must not throw
        fw.close();
        // verify that no exception was thrown and underlying writer is closed (StringWriter does nothing)
        assertTrue(true);
    }

    @Test
    public void testWriteAfterCloseThrowsIOException() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw);
        fw.close();
        try {
            fw.write('a');
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }
    }
}
