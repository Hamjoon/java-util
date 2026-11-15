package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

public class FastWriterTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSize() {
        new FastWriter(new StringWriter(), 0);
    }

    @Test
    public void testWriteIntSingleChar() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 8);
        fw.write('A');
        fw.flush();
        assertEquals("A", sw.toString());
    }

    @Test
    public void testWriteIntBufferOverflow() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4); // tiny buffer
        // write 6 characters, which forces one automatic flush
        fw.write('a');
        fw.write('b');
        fw.write('c');
        fw.write('d'); // buffer now full
        fw.write('e'); // should trigger flush of "abcd"
        fw.write('f'); // remains in buffer
        fw.flush();    // flush remaining "ef"
        assertEquals("abcdef", sw.toString());
    }

    @Test
    public void testWriteCharArraySmallLen() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 10);
        char[] data = "hello".toCharArray();
        fw.write(data, 0, data.length);
        fw.flush();
        assertEquals("hello", sw.toString());
    }

    @Test
    public void testWriteCharArrayExactBufferSize() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5);
        char[] data = "12345".toCharArray(); // length == buffer size
        fw.write(data, 0, data.length);
        // According to implementation, len >= cb.length -> flush buffer (empty) then direct write
        fw.flush();
        assertEquals("12345", sw.toString());
    }

    @Test
    public void testWriteCharArrayLargerThanBuffer() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5);
        char[] data = "ABCDEFGHIJ".toCharArray(); // length 10 > buffer size 5
        fw.write(data, 0, data.length);
        fw.flush();
        assertEquals("ABCDEFGHIJ", sw.toString());
    }

    @Test
    public void testWriteCharArrayZeroLen() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4);
        char[] data = "xyz".toCharArray();
        fw.write(data, 1, 0); // zero length, should be a no‑op
        fw.flush();
        assertEquals("", sw.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeOffset() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4);
        char[] data = "test".toCharArray();
        fw.write(data, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOffsetBeyondLength() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4);
        char[] data = "test".toCharArray();
        fw.write(data, 5, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeLength() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4);
        char[] data = "test".toCharArray();
        fw.write(data, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayLengthExceedsArray() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4);
        char[] data = "test".toCharArray();
        fw.write(data, 2, 3); // 2+3 > data.length
    }

    @Test
    public void testWriteStringSplitsAcrossBuffer() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 4); // force many splits
        String longStr = "ABCDEFGHIJ"; // 10 chars
        fw.write(longStr, 0, longStr.length());
        fw.flush();
        assertEquals(longStr, sw.toString());
    }

    @Test
    public void testFlushWritesPartialBuffer() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 8);
        fw.write('x');
        fw.write('y');
        // buffer not full yet
        fw.flush();
        assertEquals("xy", sw.toString());
    }

    @Test
    public void testCloseFlushesAndPreventsFurtherWrites() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5);
        fw.write("abc".toCharArray(), 0, 3);
        fw.close(); // should flush "abc"
        assertEquals("abc", sw.toString());

        // After close, any write must throw IOException
        try {
            fw.write('z');
            fail("Expected IOException after close");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5);
        fw.close(); // first close
        // second close should be a no‑op and not throw
        fw.close();
        // nothing written, still empty
        assertEquals("", sw.toString());
    }

    @Test
    public void testWriteAfterCloseThrowsIOException() throws IOException {
        StringWriter sw = new StringWriter();
        FastWriter fw = new FastWriter(sw, 5);
        fw.close();
        try {
            fw.write("data".toCharArray(), 0, 4);
            fail("Expected IOException when writing after close");
        } catch (IOException e) {
            // expected
        }
    }
}
