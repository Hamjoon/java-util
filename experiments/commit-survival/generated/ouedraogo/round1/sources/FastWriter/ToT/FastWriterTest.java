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
 *   <li>Construction (default and custom buffer sizes, illegal arguments)</li>
 *   <li>{@code write(int)}</li>
 *   <li>{@code write(char[],int,int)} – normal, edge and error conditions</li>
 *   <li>{@code write(String,int,int)} – normal, buffer‑overflow and boundary cases</li>
 *   <li>{@code flush()} – ensures buffered data is emitted</li>
 *   <li>{@code close()} – flushing, resource release and post‑close behavior</li>
 * </ul>
 *
 * Only standard JDK classes (e.g., {@link StringWriter}) and the public API of {@code FastWriter}
 * are used – no mocking or subclassing of library types.
 */
public class FastWriterTest {

    private StringWriter underlying;
    private FastWriter fastWriter;

    @Before
    public void setUp() {
        underlying = new StringWriter();
        fastWriter = new FastWriter(underlying);
    }

    @After
    public void tearDown() throws IOException {
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    /* -------------------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesWriter() throws IOException {
        fastWriter.write('a');
        fastWriter.flush();
        assertEquals("a", underlying.toString());
    }

    @Test
    public void testCustomBufferSizeConstructor() throws IOException {
        FastWriter w = new FastWriter(underlying, 4);
        w.write('x');
        w.write('y');
        w.flush();
        assertEquals("xy", underlying.toString());
        w.close(); // clean up
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsZeroBufferSize() {
        new FastWriter(underlying, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNegativeBufferSize() {
        new FastWriter(underlying, -10);
    }

    /* -------------------------------------------------------------
     *  write(int) tests
     * ------------------------------------------------------------- */

    @Test
    public void testWriteIntSingleChar() throws IOException {
        fastWriter.write('Z');
        fastWriter.flush();
        assertEquals("Z", underlying.toString());
    }

    @Test
    public void testWriteIntBufferOverflow() throws IOException {
        FastWriter w = new FastWriter(underlying, 4);
        // write 6 characters -> two flushes (4 + 2)
        w.write('1');
        w.write('2');
        w.write('3');
        w.write('4'); // buffer full, next write triggers flush
        w.write('5');
        w.write('6');
        w.flush();
        assertEquals("123456", underlying.toString());
        w.close();
    }

    @Test(expected = IOException.class)
    public void testWriteIntAfterCloseThrows() throws IOException {
        fastWriter.close();
        fastWriter.write('x');
    }

    /* -------------------------------------------------------------
     *  write(char[],off,len) tests
     * ------------------------------------------------------------- */

    @Test
    public void testWriteCharArrayExactBufferSize() throws IOException {
        FastWriter w = new FastWriter(underlying, 5);
        char[] data = "hello".toCharArray();
        w.write(data, 0, data.length); // fills buffer exactly
        // not flushed yet
        assertEquals("", underlying.toString());
        w.flush();
        assertEquals("hello", underlying.toString());
        w.close();
    }

    @Test
    public void testWriteCharArrayLargerThanBufferWritesDirectly() throws IOException {
        FastWriter w = new FastWriter(underlying, 4);
        char[] data = "abcdefgh".toCharArray(); // length 8 >= buffer size
        w.write(data, 0, data.length);
        // direct write, buffer remains empty
        assertEquals("abcdefgh", underlying.toString());
        w.close();
    }

    @Test
    public void testWriteCharArrayPartialOverflow() throws IOException {
        FastWriter w = new FastWriter(underlying, 5);
        w.write('A'); // 1 char in buffer
        char[] data = "123456".toCharArray(); // length 6 > remaining (4)
        w.write(data, 0, data.length);
        // Expected sequence: buffer flushed (contains 'A' + first 4 chars), then remaining 2 chars in buffer
        w.flush();
        assertEquals("A123456", underlying.toString());
        w.close();
    }

    @Test
    public void testWriteCharArrayZeroLengthDoesNothing() throws IOException {
        char[] data = "nothing".toCharArray();
        fastWriter.write(data, 2, 0);
        fastWriter.flush();
        assertEquals("", underlying.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeOffset() throws IOException {
        char[] data = "abc".toCharArray();
        fastWriter.write(data, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeLength() throws IOException {
        char[] data = "abc".toCharArray();
        fastWriter.write(data, 0, -5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOffsetBeyondArray() throws IOException {
        char[] data = "abc".toCharArray();
        fastWriter.write(data, 4, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayLengthExceedsArray() throws IOException {
        char[] data = "abc".toCharArray();
        fastWriter.write(data, 1, 3); // 1+3 > 3
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOverflowIntegerWrap() throws IOException {
        char[] data = "abc".toCharArray();
        // Use values that cause off+len overflow to negative
        fastWriter.write(data, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Test(expected = IOException.class)
    public void testWriteCharArrayAfterCloseThrows() throws IOException {
        fastWriter.close();
        fastWriter.write(new char[]{'x'}, 0, 1);
    }

    /* -------------------------------------------------------------
     *  write(String,off,len) tests
     * ------------------------------------------------------------- */

    @Test
    public void testWriteStringFitsInBuffer() throws IOException {
        FastWriter w = new FastWriter(underlying, 10);
        w.write("hello", 0, 5);
        // not flushed yet
        assertEquals("", underlying.toString());
        w.flush();
        assertEquals("hello", underlying.toString());
        w.close();
    }

    @Test
    public void testWriteStringMultipleFlushes() throws IOException {
        FastWriter w = new FastWriter(underlying, 4);
        w.write("ABCDEFGHIJ", 0, 10); // 10 chars, buffer size 4 -> 3 flushes
        w.flush();
        assertEquals("ABCDEFGHIJ", underlying.toString());
        w.close();
    }

    @Test
    public void testWriteStringWithOffsetAndLength() throws IOException {
        FastWriter w = new FastWriter(underlying, 5);
        w.write("0123456789", 2, 5); // expects "23456"
        w.flush();
        assertEquals("23456", underlying.toString());
        w.close();
    }

    @Test(expected = IOException.class)
    public void testWriteStringAfterCloseThrows() throws IOException {
        fastWriter.close();
        fastWriter.write("test", 0, 4);
    }

    /* -------------------------------------------------------------
     *  flush() tests
     * ------------------------------------------------------------- */

    @Test
    public void testFlushEmitsBufferedData() throws IOException {
        FastWriter w = new FastWriter(underlying, 8);
        w.write('x');
        w.write('y');
        // buffer not full yet
        w.flush();
        assertEquals("xy", underlying.toString());
        w.close();
    }

    @Test
    public void testFlushWhenBufferEmptyDoesNotFail() throws IOException {
        fastWriter.flush(); // should be a no‑op
        assertEquals("", underlying.toString());
    }

    /* -------------------------------------------------------------
     *  close() tests
     * ------------------------------------------------------------- */

    @Test
    public void testCloseFlushesAndClosesUnderlyingWriter() throws IOException {
        FastWriter w = new FastWriter(underlying, 5);
        w.write("abc".toCharArray(), 0, 3);
        w.close(); // should flush "abc"
        assertEquals("abc", underlying.toString());
        // underlying StringWriter is still usable for verification, but FastWriter must be closed.
    }

    @Test
    public void testCloseIsIdempotent() throws IOException {
        fastWriter.close(); // first close
        // second close should be harmless
        fastWriter.close();
        // still no exception, and underlying writer is closed (StringWriter does not throw)
    }

    @Test(expected = IOException.class)
    public void testWriteAfterCloseThrowsIOException() throws IOException {
        fastWriter.close();
        fastWriter.write('z');
    }

    @Test(expected = IOException.class)
    public void testWriteCharArrayAfterCloseThrowsIOException() throws IOException {
        fastWriter.close();
        fastWriter.write(new char[]{'a'}, 0, 1);
    }

    @Test(expected = IOException.class)
    public void testWriteStringAfterCloseThrowsIOException() throws IOException {
        fastWriter.close();
        fastWriter.write("test", 0, 4);
    }
}
