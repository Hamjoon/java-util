package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link FastWriter}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and methods.</li>
 *   <li>Typical usage scenarios.</li>
 *   <li>Edge‑case handling (buffer overflow, invalid parameters, etc.).</li>
 *   <li>Exception paths (illegal arguments, closed stream, etc.).</li>
 * </ul>
 */
public class FastWriterTest {

    private StringWriter delegate;
    private FastWriter writer;

    @Before
    public void setUp() {
        delegate = new StringWriter();
        writer = new FastWriter(delegate);
    }

    @After
    public void tearDown() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    /* -------------------------------------------------
     * Constructor tests
     * ------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesWriter() throws IOException {
        FastWriter w = new FastWriter(new StringWriter());
        w.write('A');
        w.flush();
        // No exception – cannot inspect internal buffer, just ensure no failure.
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNonPositiveBufferSize() {
        new FastWriter(new StringWriter(), 0);
    }

    /* -------------------------------------------------
     * write(int) tests
     * ------------------------------------------------- */

    @Test
    public void testWriteIntSingleChar() throws IOException {
        writer.write('X');
        writer.flush();
        assertEquals("X", delegate.toString());
    }

    @Test
    public void testWriteIntBufferOverflowTriggersFlush() throws IOException {
        // Fill the default buffer (8192 chars) with 'a'
        for (int i = 0; i < FastWriter.DEFAULT_BUFFER_SIZE; i++) {
            writer.write('a');
        }
        // At this point the buffer is full but not yet flushed.
        // Write one more char, which must cause a flush of the previous buffer.
        writer.write('b');
        writer.flush(); // ensure final flush
        String expected = repeat('a', FastWriter.DEFAULT_BUFFER_SIZE) + "b";
        assertEquals(expected, delegate.toString());
    }

    @Test(expected = IOException.class)
    public void testWriteIntAfterCloseThrowsIOException() throws IOException {
        writer.close();
        writer.write('c');
    }

    /* -------------------------------------------------
     * write(char[], int, int) tests
     * ------------------------------------------------- */

    @Test
    public void testWriteCharArraySimple() throws IOException {
        char[] data = "hello".toCharArray();
        writer.write(data, 0, data.length);
        writer.flush();
        assertEquals("hello", delegate.toString());
    }

    @Test
    public void testWriteCharArrayExactBufferSize() throws IOException {
        char[] data = repeat('x', FastWriter.DEFAULT_BUFFER_SIZE).toCharArray();
        writer.write(data, 0, data.length);
        writer.flush();
        assertEquals(repeat('x', FastWriter.DEFAULT_BUFFER_SIZE), delegate.toString());
    }

    @Test
    public void testWriteCharArrayLargerThanBuffer() throws IOException {
        int bigSize = FastWriter.DEFAULT_BUFFER_SIZE * 2 + 5;
        char[] data = repeat('z', bigSize).toCharArray();

        // Write in one call – should flush internal buffer then write directly.
        writer.write(data, 0, data.length);
        writer.flush();

        assertEquals(repeat('z', bigSize), delegate.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeOffset() throws IOException {
        char[] data = "abc".toCharArray();
        writer.write(data, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeLength() throws IOException {
        char[] data = "abc".toCharArray();
        writer.write(data, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOffsetLengthOutOfBounds() throws IOException {
        char[] data = "abc".toCharArray();
        writer.write(data, 2, 2); // 2+2 exceeds array length
    }

    @Test
    public void testWriteCharArrayZeroLengthDoesNothing() throws IOException {
        char[] data = "nothing".toCharArray();
        writer.write(data, 0, 0);
        writer.flush();
        assertEquals("", delegate.toString());
    }

    /* -------------------------------------------------
     * write(String, int, int) tests
     * ------------------------------------------------- */

    @Test
    public void testWriteStringSimple() throws IOException {
        String s = "FastWriter";
        writer.write(s, 0, s.length());
        writer.flush();
        assertEquals(s, delegate.toString());
    }

    @Test
    public void testWriteStringWithPartialOffsetsAndMultipleFlushes() throws IOException {
        // Build a string longer than the buffer to force several flushes.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < FastWriter.DEFAULT_BUFFER_SIZE * 3; i++) {
            sb.append((char) ('a' + (i % 26)));
        }
        String longString = sb.toString();

        // Write a middle slice of the string.
        int off = FastWriter.DEFAULT_BUFFER_SIZE / 2;
        int len = FastWriter.DEFAULT_BUFFER_SIZE * 2; // spans across buffer boundaries
        writer.write(longString, off, len);
        writer.flush();

        String expected = longString.substring(off, off + len);
        assertEquals(expected, delegate.toString());
    }

    @Test(expected = IOException.class)
    public void testWriteStringAfterCloseThrowsIOException() throws IOException {
        writer.close();
        writer.write("test", 0, 4);
    }

    /* -------------------------------------------------
     * flush() tests
     * ------------------------------------------------- */

    @Test
    public void testFlushWritesBufferedData() throws IOException {
        writer.write('p');
        writer.flush();
        assertEquals("p", delegate.toString());
    }

    @Test
    public void testFlushWhenBufferEmptyDoesNothing() throws IOException {
        writer.flush(); // should not throw
        assertEquals("", delegate.toString());
    }

    /* -------------------------------------------------
     * close() tests
     * ------------------------------------------------- */

    @Test
    public void testCloseFlushesRemainingData() throws IOException {
        writer.write('q');
        writer.close(); // close should flush the pending 'q'
        assertEquals("q", delegate.toString());
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        writer.close(); // first close
        writer.close(); // second close – must not throw
    }

    @Test(expected = IOException.class)
    public void testWriteAfterCloseThrowsIOException() throws IOException {
        writer.close();
        writer.write('r');
    }

    /* -------------------------------------------------
     * Helper methods
     * ------------------------------------------------- */

    private static String repeat(char c, int count) {
        char[] arr = new char[count];
        for (int i = 0; i < count; i++) {
            arr[i] = c;
        }
        return new String(arr);
    }
}
