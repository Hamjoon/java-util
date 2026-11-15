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
        writer = new FastWriter(underlying);
    }

    @After
    public void tearDown() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    /* ---------- Basic functionality tests ---------- */

    @Test
    public void testConstructorWithDefaultBuffer() throws IOException {
        FastWriter fw = new FastWriter(underlying);
        fw.write('a');
        fw.flush();
        assertEquals("a", underlying.toString());
    }

    @Test
    public void testConstructorWithCustomBuffer() throws IOException {
        FastWriter fw = new FastWriter(underlying, 10);
        fw.write("hello".toCharArray(), 0, 5);
        fw.flush();
        assertEquals("hello", underlying.toString());
    }

    @Test
    public void testWriteInt() throws IOException {
        FastWriter fw = new FastWriter(underlying, 4);
        fw.write('x');
        fw.write('y');
        fw.flush();
        assertEquals("xy", underlying.toString());
    }

    @Test
    public void testWriteCharArray() throws IOException {
        FastWriter fw = new FastWriter(underlying, 4);
        char[] data = "abcd".toCharArray();
        fw.write(data, 0, data.length);
        fw.flush();
        assertEquals("abcd", underlying.toString());
    }

    @Test
    public void testWriteString() throws IOException {
        FastWriter fw = new FastWriter(underlying, 4);
        fw.write("hello", 0, 5);
        fw.flush();
        assertEquals("hello", underlying.toString());
    }

    @Test
    public void testFlush() throws IOException {
        FastWriter fw = new FastWriter(underlying, 4);
        fw.write("abc".toCharArray(), 0, 3);
        fw.flush();
        assertEquals("abc", underlying.toString());
    }

    @Test
    public void testClose() throws IOException {
        FastWriter fw = new FastWriter(underlying);
        fw.write("test");
        fw.close();
        assertEquals("test", underlying.toString());
    }

    /* ---------- Edge‑case & exception tests ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithZeroBufferSize() {
        new FastWriter(new StringWriter(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeBufferSize() {
        new FastWriter(new StringWriter(), -5);
    }

    @Test
    public void testWriteAfterCloseThrowsIOException() throws IOException {
        FastWriter fw = new FastWriter(new StringWriter());
        fw.close();
        try {
            fw.write('z');
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertTrue(e.getMessage().contains("FastWriter stream is closed"));
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeOffset() throws IOException {
        FastWriter fw = new FastWriter(new StringWriter());
        char[] data = "abc".toCharArray();
        fw.write(data, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeLength() throws IOException {
        FastWriter fw = new FastWriter(new StringWriter());
        char[] data = "abc".toCharArray();
        fw.write(data, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOffsetBeyondArray() throws IOException {
        FastWriter fw = new FastWriter(new StringWriter());
        char[] data = "abc".toCharArray();
        fw.write(data, 4, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayLenBeyondArray() throws IOException {
        FastWriter fw = new FastWriter(new StringWriter());
        char[] data = "abc".toCharArray();
        fw.write(data, 1, 3); // 1+3 > 3
    }

    @Test
    public void testWriteCharArrayZeroLengthDoesNothing() throws IOException {
        FastWriter fw = new FastWriter(underlying);
        char[] data = "xyz".toCharArray();
        fw.write(data, 0, 0);
        fw.flush();
        assertEquals("", underlying.toString());
    }

    @Test
    public void testWriteStringZeroLengthDoesNothing() throws IOException {
        FastWriter fw = new FastWriter(underlying);
        fw.write("anything", 0, 0);
        fw.flush();
        assertEquals("", underlying.toString());
    }

    @Test
    public void testWriteLargeCharArrayDirectWrite() throws IOException {
        FastWriter fw = new FastWriter(underlying, 4);
        char[] data = "abcdefghijkl".toCharArray(); // length 12
        fw.write(data, 0, data.length);
        fw.flush();
        assertEquals("abcdefghijkl", underlying.toString());
    }

    @Test
    public void testWriteStringMultipleBufferCycles() throws IOException {
        FastWriter fw = new FastWriter(underlying, 5);
        String longStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        fw.write(longStr, 0, longStr.length());
        fw.flush();
        assertEquals(longStr, underlying.toString());
    }

    @Test
    public void testCloseTwiceIsSafe() throws IOException {
        FastWriter fw = new FastWriter(underlying);
        fw.write("once");
        fw.close();          // first close
        // second close should be a no‑op and not throw
        fw.close();
        assertEquals("once", underlying.toString());
    }

    @Test
    public void testFlushAfterCloseThrowsIOException() throws IOException {
        FastWriter fw = new FastWriter(new StringWriter());
        fw.close();
        try {
            fw.flush();
            fail("Expected IOException when flushing a closed FastWriter");
        } catch (IOException e) {
            // Expected exception
        }
    }

    @Test
    public void testBufferExactlyFullTriggersAutomaticFlush() throws IOException {
        FastWriter fw = new FastWriter(underlying, 3);
        fw.write('a');
        fw.write('b');
        fw.write('c'); // buffer reaches capacity and should flush automatically
        assertEquals("abc", underlying.toString());

        fw.write('d');
        fw.flush();
        assertEquals("abcd", underlying.toString());
    }

    @Test
    public void testWriteSingleCharThatForcesFlushWhenBufferSizeIsOne() throws IOException {
        FastWriter fw = new FastWriter(underlying, 1);
        fw.write('x');
        // Each write flushes immediately because buffer size = 1
        assertEquals("x", underlying.toString());

        fw.write('y');
        assertEquals("xy", underlying.toString());
    }
}
