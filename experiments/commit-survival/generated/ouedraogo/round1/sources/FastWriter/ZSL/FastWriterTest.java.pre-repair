package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

public class FastWriterTest {

    private StringWriter underlying;
    private FastWriter writer;

    @Before
    public void setUp() {
        underlying = new StringWriter();
        // default buffer size (8192) is fine for most tests
        writer = new FastWriter(underlying);
    }

    @After
    public void tearDown() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    /*** Constructor validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSize() {
        new FastWriter(underlying, 0);
    }

    /*** write(int) – basic single character ***/
    @Test
    public void testWriteIntSingleChar() throws IOException {
        writer.write('A');
        writer.flush(); // force buffer to underlying writer
        assertEquals("A", underlying.toString());
    }

    /*** write(int) – buffer overflow triggers flush ***/
    @Test
    public void testWriteIntBufferOverflow() throws IOException {
        // use a tiny buffer to force overflow quickly
        writer.close(); // close default writer
        writer = new FastWriter(underlying, 4); // buffer size = 4

        // write 5 characters, the first 4 should be flushed automatically
        writer.write('1');
        writer.write('2');
        writer.write('3');
        writer.write('4'); // buffer now full, but not yet flushed
        writer.write('5'); // this call should flush first 4 chars, then buffer '5'

        // At this point, underlying writer should contain the first four chars
        assertEquals("1234", underlying.toString());

        // Flush remaining data
        writer.flush();
        assertEquals("12345", underlying.toString());
    }

    /*** write(char[], off, len) – normal case within buffer ***/
    @Test
    public void testWriteCharArrayWithinBuffer() throws IOException {
        char[] data = "hello".toCharArray();
        writer.write(data, 0, data.length);
        // not flushed yet
        assertEquals("", underlying.toString());

        writer.flush();
        assertEquals("hello", underlying.toString());
    }

    /*** write(char[], off, len) – len == 0 does nothing ***/
    @Test
    public void testWriteCharArrayZeroLength() throws IOException {
        char[] data = "world".toCharArray();
        writer.write(data, 2, 0); // no-op
        writer.flush();
        assertEquals("", underlying.toString());
    }

    /*** write(char[], off, len) – length exceeds buffer size triggers direct write ***/
    @Test
    public void testWriteCharArrayDirectWriteWhenLenExceedsBuffer() throws IOException {
        writer.close(); // close default writer
        writer = new FastWriter(underlying, 4); // tiny buffer

        char[] data = "ABCDEFGHIJ".toCharArray(); // length 10 > buffer size 4

        writer.write(data, 0, data.length); // should flush any existing buffer (empty) and write directly

        // Since the write bypasses the buffer, data should be immediately in the underlying writer
        assertEquals("ABCDEFGHIJ", underlying.toString());

        // Buffer should be empty; flushing now should not add anything extra
        writer.flush();
        assertEquals("ABCDEFGHIJ", underlying.toString());
    }

    /*** write(char[], off, len) – invalid parameters ***/
    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidParameters_NegativeOffset() throws IOException {
        char[] data = "test".toCharArray();
        writer.write(data, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidParameters_OffsetBeyondLength() throws IOException {
        char[] data = "test".toCharArray();
        writer.write(data, 5, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidParameters_NegativeLength() throws IOException {
        char[] data = "test".toCharArray();
        writer.write(data, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidParameters_LengthTooLarge() throws IOException {
        char[] data = "test".toCharArray();
        writer.write(data, 1, 4); // 1+4 > data.length
    }

    /*** write(String, off, len) – normal case, may require multiple buffer flushes ***/
    @Test
    public void testWriteStringSpanningMultipleBuffers() throws IOException {
        writer.close(); // use small buffer
        writer = new FastWriter(underlying, 5); // buffer size = 5

        String source = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"; // 26 chars
        // Write the whole string in one call
        writer.write(source, 0, source.length());

        // At this point, the underlying writer should have received the first 25 chars (5 flushes of 5)
        // and the last character should still be in the buffer.
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXY", underlying.toString());

        // Flush remaining character
        writer.flush();
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ", underlying.toString());
    }

    /*** flush – should write any buffered data ***/
    @Test
    public void testFlushWritesBufferedData() throws IOException {
        writer.write('x');
        writer.write('y');
        // not flushed yet
        assertEquals("", underlying.toString());

        writer.flush();
        assertEquals("xy", underlying.toString());
    }

    /*** close – flushes, closes underlying writer, and prevents further writes ***/
    @Test
    public void testCloseFlushesAndPreventsFurtherWrites() throws IOException {
        writer.write("data".toCharArray(), 0, 4);
        writer.close();

        // Buffer should have been flushed during close
        assertEquals("data", underlying.toString());

        // Subsequent write should throw IOException because stream is closed
        try {
            writer.write('z');
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }

        // Subsequent flush should also throw IOException (out is null)
        try {
            writer.flush();
            fail("Expected NullPointerException or IOException after close");
        } catch (Exception e) {
            // The implementation will try to use out which is null, leading to NPE.
            // Accept any exception type here.
        }
    }

    /*** close – idempotent (second close does nothing) ***/
    @Test
    public void testCloseIdempotent() throws IOException {
        writer.write('a');
        writer.close(); // first close
        // second close should not throw
        writer.close();
        // underlying writer should have received the buffered 'a'
        assertEquals("a", underlying.toString());
    }

    /*** write after explicit close should throw IOException ***/
    @Test
    public void testWriteAfterCloseThrows() throws IOException {
        writer.close();
        try {
            writer.write('b');
            fail("Expected IOException after writer is closed");
        } catch (IOException e) {
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }
    }

    /*** write(String, off, len) with zero length does nothing ***/
    @Test
    public void testWriteStringZeroLength() throws IOException {
        writer.write("nothing", 2, 0);
        writer.flush();
        assertEquals("", underlying.toString());
    }
}
