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
    }

    @After
    public void tearDown() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    /* ---------- Constructor tests ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNonPositiveBufferSize() {
        new FastWriter(underlying, 0);
    }

    @Test
    public void testDefaultConstructorCreatesWriter() throws IOException {
        writer = new FastWriter(underlying);
        writer.write('A');
        writer.flush();
        assertEquals("A", underlying.toString());
    }

    /* ---------- write(int) tests ---------- */

    @Test
    public void testWriteIntSingleCharacter() throws IOException {
        writer = new FastWriter(underlying, 8);
        writer.write('x');
        // not flushed yet, underlying writer should be empty
        assertEquals("", underlying.toString());
        writer.flush();
        assertEquals("x", underlying.toString());
    }

    @Test
    public void testWriteIntBufferOverflowTriggersFlush() throws IOException {
        writer = new FastWriter(underlying, 4);
        writer.write('a'); // buffer: a
        writer.write('b'); // buffer: a b
        writer.write('c'); // buffer: a b c
        writer.write('d'); // buffer full -> not flushed yet (flush will happen on next write)
        writer.write('e'); // should flush "abcd" automatically, buffer now contains 'e'
        // At this point, the underlying writer must have received "abcd"
        assertEquals("abcd", underlying.toString());
        writer.flush(); // flush remaining 'e'
        assertEquals("abcde", underlying.toString());
    }

    /* ---------- write(char[], off, len) tests ---------- */

    @Test
    public void testWriteCharArrayNormalPath() throws IOException {
        writer = new FastWriter(underlying, 10);
        char[] data = "hello".toCharArray();
        writer.write(data, 0, data.length);
        writer.flush();
        assertEquals("hello", underlying.toString());
    }

    @Test
    public void testWriteCharArrayZeroLengthDoesNothing() throws IOException {
        writer = new FastWriter(underlying, 10);
        char[] data = "nothing".toCharArray();
        writer.write(data, 2, 0); // zero length
        writer.flush();
        assertEquals("", underlying.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidOffset() throws IOException {
        writer = new FastWriter(underlying, 10);
        char[] data = "abc".toCharArray();
        writer.write(data, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidLength() throws IOException {
        writer = new FastWriter(underlying, 10);
        char[] data = "abc".toCharArray();
        writer.write(data, 0, 5); // length exceeds array
    }

    @Test
    public void testWriteCharArrayLenGreaterThanBufferWritesDirectly() throws IOException {
        writer = new FastWriter(underlying, 4);
        char[] data = "123456".toCharArray(); // length 6 > buffer size 4
        writer.write(data, 0, data.length);
        // Since len >= buffer, it should be written directly without buffering
        assertEquals("123456", underlying.toString());
        writer.flush(); // no additional data expected
        assertEquals("123456", underlying.toString());
    }

    /* ---------- write(String, off, len) tests ---------- */

    @Test
    public void testWriteStringEmptyLengthDoesNothing() throws IOException {
        writer = new FastWriter(underlying, 8);
        writer.write("", 0, 0);
        writer.flush();
        assertEquals("", underlying.toString());
    }

    @Test
    public void testWriteStringFitsInBuffer() throws IOException {
        writer = new FastWriter(underlying, 10);
        writer.write("test", 0, 4);
        // not flushed yet
        assertEquals("", underlying.toString());
        writer.flush();
        assertEquals("test", underlying.toString());
    }

    @Test
    public void testWriteStringExactBufferSizeCausesImmediateFlush() throws IOException {
        writer = new FastWriter(underlying, 4);
        writer.write("ABCD", 0, 4); // fills buffer exactly
        // The implementation flushes when buffer becomes full
        assertEquals("ABCD", underlying.toString());
    }

    @Test
    public void testWriteStringMediumPathPartialFillAndChunkWrites() throws IOException {
        writer = new FastWriter(underlying, 4);
        // First write 2 chars, leaves space for 2 more
        writer.write("AB", 0, 2); // buffer: AB
        // Second write 5 chars, will fill remaining 2, flush, then write chunk of 4, then remainder
        writer.write("CDEFG", 0, 5);
        // At this point, underlying writer should have received "ABCD"
        assertEquals("ABCD", underlying.toString());
        // Buffer now holds "EFG"
        writer.flush();
        assertEquals("ABCDEFG", underlying.toString());
    }

    @Test
    public void testWriteStringLargeMultipleFullBufferChunks() throws IOException {
        writer = new FastWriter(underlying, 4);
        String src = "0123456789"; // length 10
        writer.write(src, 0, src.length());
        // The method writes full buffer chunks directly; after the call, nothing may be left in buffer
        // because the last chunk (2 chars) stays in the buffer.
        assertEquals("0123456789".substring(0, 8), underlying.toString()); // first two full chunks written
        writer.flush(); // flush the remaining "89"
        assertEquals("0123456789", underlying.toString());
    }

    /* ---------- flush() tests ---------- */

    @Test
    public void testFlushWritesBufferedData() throws IOException {
        writer = new FastWriter(underlying, 5);
        writer.write('X');
        writer.write('Y');
        // Buffer contains "XY", not yet flushed
        assertEquals("", underlying.toString());
        writer.flush();
        assertEquals("XY", underlying.toString());
    }

    /* ---------- close() tests ---------- */

    @Test
    public void testCloseFlushesRemainingData() throws IOException {
        writer = new FastWriter(underlying, 4);
        writer.write('a');
        writer.write('b');
        writer.write('c');
        writer.close(); // should flush "abc"
        assertEquals("abc", underlying.toString());
    }

    @Test
    public void testClosePreventsFurtherWrites() throws IOException {
        writer = new FastWriter(underlying, 4);
        writer.write('z');
        writer.close();
        try {
            writer.write('x');
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        writer = new FastWriter(underlying, 4);
        writer.write('1');
        writer.close();
        // second close should be a no‑op and not throw
        writer.close();
        assertEquals("1", underlying.toString());
    }
}
