/*****************************************************************************************
 * JUnit 4 test suite for com.cedarsoftware.util.FastWriter
 *
 * The tests cover:
 *   • Constructor validation (default and custom buffer sizes, illegal size)
 *   • write(int) – normal write and buffer overflow handling
 *   • write(char[], int, int) – normal, zero‑length, large‑buffer direct write,
 *     and argument‑validation errors
 *   • write(String, int, int) – writing across buffer boundaries
 *   • flush() – ensures buffered data is emitted
 *   • close() – proper flushing, resource release and post‑close behaviour
 *
 * No external mocking frameworks are used; a java.io.StringWriter supplies the
 * underlying Writer implementation.
 *****************************************************************************************/

package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.*;

public class FastWriterTest {

    private StringWriter backingWriter;
    private FastWriter fastWriter;

    @Before
    public void setUp() {
        backingWriter = new StringWriter();
        fastWriter = new FastWriter(backingWriter);
    }

    @After
    public void tearDown() throws IOException {
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    /*** Constructor tests ***/

    @Test
    public void testDefaultConstructorCreatesWriter() throws IOException {
        fastWriter.write('A');
        fastWriter.flush();
        assertEquals("A", backingWriter.toString());
    }

    @Test
    public void testCustomBufferSizeConstructor() throws IOException {
        // Use a tiny buffer to force multiple flushes.
        FastWriter tiny = new FastWriter(backingWriter, 2);
        tiny.write('X');
        tiny.write('Y'); // buffer full now
        tiny.write('Z'); // should trigger flush of XY, then buffer Z
        tiny.flush();
        assertEquals("XYZ", backingWriter.toString());
        tiny.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNonPositiveBufferSize() {
        new FastWriter(backingWriter, 0);
    }

    /*** write(int) tests ***/

    @Test
    public void testWriteIntSingleChar() throws IOException {
        fastWriter.write('h');
        fastWriter.flush();
        assertEquals("h", backingWriter.toString());
    }

    @Test
    public void testWriteIntBufferOverflow() throws IOException {
        // Force overflow with a small buffer.
        FastWriter w = new FastWriter(backingWriter, 3);
        w.write('a');
        w.write('b');
        w.write('c'); // buffer now full
        w.write('d'); // should flush "abc" then buffer "d"
        w.flush();
        assertEquals("abcd", backingWriter.toString());
        w.close();
    }

    @Test(expected = IOException.class)
    public void testWriteIntAfterCloseThrows() throws IOException {
        fastWriter.close();
        fastWriter.write('x'); // should throw IOException
    }

    /*** write(char[], off, len) tests ***/

    @Test
    public void testWriteCharArrayNormal() throws IOException {
        char[] data = "hello".toCharArray();
        fastWriter.write(data, 0, data.length);
        fastWriter.flush();
        assertEquals("hello", backingWriter.toString());
    }

    @Test
    public void testWriteCharArrayZeroLengthDoesNothing() throws IOException {
        char[] data = "world".toCharArray();
        fastWriter.write(data, 2, 0); // zero length
        fastWriter.flush();
        assertEquals("", backingWriter.toString());
    }

    @Test
    public void testWriteCharArrayLengthGreaterThanBufferWritesDirectly() throws IOException {
        // Buffer size is default 8192; we use a custom tiny buffer to test the path.
        FastWriter w = new FastWriter(backingWriter, 4);
        char[] data = "1234567".toCharArray(); // length 7 > buffer size 4
        w.write(data, 0, data.length);
        // No explicit flush needed – the method writes directly when len >= buffer.
        w.close(); // close will flush any remaining buffered data (none in this case)
        assertEquals("1234567", backingWriter.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidOffsetThrows() throws IOException {
        char[] data = "abc".toCharArray();
        fastWriter.write(data, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidLengthThrows() throws IOException {
        char[] data = "abc".toCharArray();
        fastWriter.write(data, 1, 5); // 1+5 exceeds array length
    }

    /*** write(String, off, len) tests ***/

    @Test
    public void testWriteStringFitsInOneBuffer() throws IOException {
        fastWriter.write("xyz", 0, 3);
        fastWriter.flush();
        assertEquals("xyz", backingWriter.toString());
    }

    @Test
    public void testWriteStringSpansMultipleBuffers() throws IOException {
        // Use a small buffer to force multiple internal flushes.
        FastWriter w = new FastWriter(backingWriter, 5);
        String longStr = "ABCDEFGHIJ"; // 10 chars, buffer size 5 => two flushes
        w.write(longStr, 0, longStr.length());
        w.flush();
        assertEquals("ABCDEFGHIJ", backingWriter.toString());
        w.close();
    }

    @Test(expected = IOException.class)
    public void testWriteStringAfterCloseThrows() throws IOException {
        fastWriter.close();
        fastWriter.write("test", 0, 4);
    }

    /*** flush() tests ***/

    @Test
    public void testFlushEmitsBufferedData() throws IOException {
        fastWriter.write("partial".toCharArray(), 0, 3); // "par"
        fastWriter.flush(); // should write "par"
        assertEquals("par", backingWriter.toString());

        // Write more and flush again
        fastWriter.write("tial".toCharArray(), 0, 4); // now buffer "tial"
        fastWriter.flush();
        assertEquals("partial", backingWriter.toString());
    }

    /*** close() tests ***/

    @Test
    public void testCloseFlushesRemainingData() throws IOException {
        fastWriter.write("data".toCharArray(), 0, 4);
        fastWriter.close(); // should flush "data"
        assertEquals("data", backingWriter.toString());
    }

    @Test
    public void testCloseIdempotentAndSubsequentWritesFail() throws IOException {
        fastWriter.write('a');
        fastWriter.close();

        // Second close should be a no‑op, not throw.
        fastWriter.close();

        // Any further write must fail.
        try {
            fastWriter.write('b');
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }
    }

    @Test
    public void testUnderlyingWriterClosedAfterFastWriterClose() throws IOException {
        fastWriter.write('z');
        fastWriter.close();

        // StringWriter does not expose a closed flag, but attempting to write to it
        // directly will throw an IOException, confirming it was closed.
        try {
            backingWriter.write('x');
            fail("Expected IOException because StringWriter has been closed");
        } catch (IOException ignored) {
            // Expected – StringWriter throws IOException when closed.
        }
    }
}
