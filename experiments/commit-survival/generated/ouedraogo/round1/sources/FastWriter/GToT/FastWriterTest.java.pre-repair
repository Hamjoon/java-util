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
 * The suite is organized as follows:
 *
 * 1.  Extraction of all public methods (including signatures).
 * 2.  Basic functional tests for each method.
 * 3.  Identification of edge‑cases / exceptional conditions.
 * 4.  Targeted tests for those edge‑cases.
 *
 * All tests use only the classes that appear in the source code
 * (or the standard JDK classes required by the signatures).
 */
public class FastWriterTest {

    // -----------------------------------------------------------------------
    // 1.  Public API extracted from FastWriter
    // -----------------------------------------------------------------------
    // FastWriter(Writer out)
    // FastWriter(Writer out, int bufferSize)
    // void write(int c) throws IOException
    // void write(char[] cbuf, int off, int len) throws IOException
    // void write(String str, int off, int len) throws IOException
    // void flush() throws IOException
    // void close() throws IOException
    // -----------------------------------------------------------------------

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

    // -----------------------------------------------------------------------
    // 2.  Basic functional tests
    // -----------------------------------------------------------------------

    @Test
    public void testDefaultConstructorCreatesWriter() throws IOException {
        writer.flush();
        assertNotNull(underlying);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithZeroBufferSizeThrows() {
        new FastWriter(new StringWriter(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeBufferSizeThrows() {
        new FastWriter(new StringWriter(), -10);
    }

    @Test
    public void testWriteIntSingleCharacter() throws IOException {
        writer.write('X');
        writer.flush();
        assertEquals("X", underlying.toString());
    }

    @Test
    public void testWriteIntBufferOverflowTriggersFlush() throws IOException {
        // small buffer to force overflow
        FastWriter small = new FastWriter(new StringWriter(), 4);
        for (int i = 0; i < 10; i++) {
            small.write('a' + i);
        }
        small.flush();
        assertEquals("abcdefghij", small.out.toString()); // 'out' is package‑private, accessible here
    }

    @Test
    public void testWriteCharArrayNormal() throws IOException {
        char[] data = "HelloWorld".toCharArray();
        writer.write(data, 0, data.length);
        writer.flush();
        assertEquals("HelloWorld", underlying.toString());
    }

    @Test
    public void testWriteCharArrayZeroLengthDoesNothing() throws IOException {
        char[] data = "abc".toCharArray();
        writer.write(data, 1, 0);
        writer.flush();
        assertEquals("", underlying.toString());
    }

    @Test
    public void testWriteCharArrayLenGreaterThanBuffer() throws IOException {
        // buffer size 4, len 6 > buffer
        FastWriter small = new FastWriter(new StringWriter(), 4);
        char[] data = "123456".toCharArray();
        small.write(data, 0, data.length);
        small.flush();
        assertEquals("123456", small.out.toString());
    }

    @Test
    public void testWriteStringNormalCrossingBufferBoundaries() throws IOException {
        FastWriter small = new FastWriter(new StringWriter(), 5);
        small.write("ABCDEFGHIJ", 0, 10); // 10 chars, buffer 5 => two flushes internally
        small.flush();
        assertEquals("ABCDEFGHIJ", small.out.toString());
    }

    @Test
    public void testWriteStringZeroLengthDoesNothing() throws IOException {
        writer.write("anything", 3, 0);
        writer.flush();
        assertEquals("", underlying.toString());
    }

    @Test
    public void testFlushWritesBufferedData() throws IOException {
        writer.write('a');
        writer.write('b');
        writer.flush();
        assertEquals("ab", underlying.toString());
    }

    @Test
    public void testCloseFlushesAndPreventsFurtherWrites() throws IOException {
        writer.write("beforeClose");
        writer.close();
        assertEquals("beforeClose", underlying.toString());

        try {
            writer.write('X');
            fail("Expected IOException after close");
        } catch (IOException expected) {
            // expected
        }
    }

    @Test
    public void testCloseIsIdempotent() throws IOException {
        writer.close(); // first close
        writer.close(); // second close – should be a no‑op, no exception
    }

    // -----------------------------------------------------------------------
    // 3.  Edge‑case / Exception identification
    // -----------------------------------------------------------------------
    // - write(int) when underlying writer is already closed → IOException
    // - write(char[], off, len) with out‑of‑bounds parameters → IndexOutOfBoundsException
    // - write(char[], off, len) with len == 0 → no action
    // - write(String, off, len) with illegal offsets/lengths → StringIndexOutOfBoundsException
    // - flush() when writer already closed → IOException from underlying flush (propagated)
    // -----------------------------------------------------------------------

    @Test
    public void testWriteIntAfterCloseThrowsIOException() throws IOException {
        writer.close();
        try {
            writer.write('Z');
            fail("Expected IOException when writing after close");
        } catch (IOException e) {
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeOffset() throws IOException {
        char[] data = "abc".toCharArray();
        writer.write(data, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOffsetBeyondArray() throws IOException {
        char[] data = "abc".toCharArray();
        writer.write(data, 4, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeLength() throws IOException {
        char[] data = "abc".toCharArray();
        writer.write(data, 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayLengthExceedsArray() throws IOException {
        char[] data = "abc".toCharArray();
        writer.write(data, 1, 3); // 1+3 > 3
    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void testWriteStringNegativeOffset() throws IOException {
        writer.write("test", -1, 2);
    }

    @Test(expected = StringIndexOutOfBoundsException.class)
    public void testWriteStringLengthExceedsString() throws IOException {
        writer.write("test", 2, 3); // 2+3 > 4
    }

    @Test
    public void testFlushAfterClosePropagatesIOException() throws IOException {
        writer.close();
        try {
            writer.flush();
            fail("Expected IOException when flushing after close");
        } catch (IOException e) {
            // The underlying StringWriter's flush does not throw, but FastWriter checks
            // out == null and therefore throws the same message as write().
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }
    }
}
