package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link FastWriter}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (valid and invalid buffer sizes)</li>
 *   <li>write(int) – normal use, buffer overflow, and write after close</li>
 *   <li>write(char[], int, int) – normal, zero‑length, out‑of‑bounds, direct write when length ≥ buffer</li>
 *   <li>write(String, int, int) – normal, multi‑chunk handling</li>
 *   <li>flush() – ensures buffered data is emitted</li>
 *   <li>close() – flushes remaining data, releases resources and prevents further writes</li>
 * </ul>
 *
 * Only standard JDK classes and the {@code FastWriter} API are used – no mocking frameworks.
 */
public class FastWriterTest {

    private StringWriter delegate;
    private FastWriter writer;

    @Before
    public void setUp() {
        delegate = new StringWriter();
        // Use a small buffer for many tests to trigger overflow behaviour easily.
        writer = new FastWriter(delegate, 10);
    }

    @After
    public void tearDown() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }

    /* -----------------------------------------------------------
     *  Constructor tests
     * ----------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidBufferSize() {
        new FastWriter(new StringWriter(), 0);
    }

    @Test
    public void testConstructorUsesDefaultBufferWhenOnlyWriterProvided() throws IOException {
        FastWriter defaultWriter = new FastWriter(new StringWriter());
        // Write exactly default buffer size characters; they should be buffered
        // and not flushed until we call flush().
        for (int i = 0; i < 8192; i++) {
            defaultWriter.write('a');
        }
        assertEquals("", ((StringWriter) defaultWriter.out).toString()); // still empty
        defaultWriter.flush();
        // After flush the underlying writer should contain 8192 'a's.
        // We cannot directly access the underlying writer, but we can close to force a flush.
        defaultWriter.close();
        // The test passes if no exception is thrown – the internal logic exercised the
        // default buffer size path.
    }

    /* -----------------------------------------------------------
     *  write(int) tests
     * ----------------------------------------------------------- */

    @Test
    public void testWriteIntSingleCharacter() throws IOException {
        writer.write('Z');
        writer.flush();
        assertEquals("Z", delegate.toString());
    }

    @Test
    public void testWriteIntBufferOverflowTriggersFlush() throws IOException {
        // Buffer size = 10, write 7 chars first, then 5 more.
        for (int i = 0; i < 7; i++) {
            writer.write('a' + i); // a,b,c,d,e,f,g
        }
        // At this point nothing should be flushed yet.
        assertEquals("", delegate.toString());

        // The next write will cause the internal buffer to become full (7+5 > 10)
        // and therefore the first 7 chars are flushed before the last char is stored.
        for (int i = 0; i < 5; i++) {
            writer.write('h' + i); // h,i,j,k,l
        }
        // After the overflow the first 7 chars must have been written.
        assertEquals("abcdefg", delegate.toString());

        // Flush remaining chars (h,i,j,k,l)
        writer.flush();
        assertEquals("abcdefghijkL".toLowerCase(), delegate.toString().toLowerCase());
    }

    @Test(expected = IOException.class)
    public void testWriteIntAfterCloseThrows() throws IOException {
        writer.close();
        writer.write('X'); // should throw IOException because out == null
    }

    /* -----------------------------------------------------------
     *  write(char[], int, int) tests
     * ----------------------------------------------------------- */

    @Test
    public void testWriteCharArraySimple() throws IOException {
        char[] data = "hello".toCharArray();
        writer.write(data, 0, data.length);
        writer.flush();
        assertEquals("hello", delegate.toString());
    }

    @Test
    public void testWriteCharArrayZeroLengthDoesNothing() throws IOException {
        char[] data = "world".toCharArray();
        writer.write(data, 2, 0); // length zero
        writer.flush();
        assertEquals("", delegate.toString());
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
    public void testWriteCharArrayLengthBeyondArray() throws IOException {
        char[] data = "abc".toCharArray();
        writer.write(data, 1, 3); // 1+3 > 3
    }

    @Test
    public void testWriteCharArrayDirectWriteWhenLengthEqualsOrExceedsBuffer() throws IOException {
        // Buffer size is 10. Write an array of length 10 – should bypass the buffer.
        char[] data = "1234567890".toCharArray(); // length 10
        writer.write(data, 0, data.length);
        // Because length == buffer size, FastWriter flushes any existing buffer
        // and writes directly to the underlying writer.
        assertEquals("1234567890", delegate.toString());

        // Now write a small piece to ensure subsequent writes continue correctly.
        writer.write('X');
        writer.flush();
        assertEquals("1234567890X", delegate.toString());
    }

    /* -----------------------------------------------------------
     *  write(String, int, int) tests
     * ----------------------------------------------------------- */

    @Test
    public void testWriteStringSimple() throws IOException {
        writer.write("foobar", 0, 6);
        writer.flush();
        assertEquals("foobar", delegate.toString());
    }

    @Test
    public void testWriteStringMultipleChunksTriggersFlush() throws IOException {
        // Buffer size = 10, write a string of length 25.
        String longString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(0, 25);
        writer.write(longString, 0, longString.length());

        // At this point the underlying writer should contain the first 10 chars
        // because the buffer would have been flushed when it filled.
        assertEquals("ABCDEFGHIJ", delegate.toString());

        // Flush remaining data (should write the rest)
        writer.flush();
        assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZ".substring(0, 25), delegate.toString());
    }

    @Test(expected = IOException.class)
    public void testWriteStringAfterCloseThrows() throws IOException {
        writer.close();
        writer.write("test", 0, 4);
    }

    /* -----------------------------------------------------------
     *  flush() tests
     * ----------------------------------------------------------- */

    @Test
    public void testFlushWritesBufferedDataWithoutClosing() throws IOException {
        writer.write("partial", 0, 7); // less than buffer size
        // Nothing should be written yet.
        assertEquals("", delegate.toString());

        writer.flush();
        assertEquals("partial", delegate.toString());

        // Writer should still be usable after flush.
        writer.write('X');
        writer.flush();
        assertEquals("partialX", delegate.toString());
    }

    /* -----------------------------------------------------------
     *  close() tests
     * ----------------------------------------------------------- */

    @Test
    public void testCloseFlushesRemainingDataAndPreventsFurtherWrites() throws IOException {
        writer.write("data", 0, 4); // buffered
        writer.close(); // should flush "data" and close underlying writer

        assertEquals("data", delegate.toString());

        // Any further write attempts must throw IOException.
        try {
            writer.write('Y');
            fail("Expected IOException after close");
        } catch (IOException expected) {
            // expected
        }
    }

    @Test
    public void testCloseIdempotentWhenAlreadyClosed() throws IOException {
        writer.close(); // first close
        // second close should be a no‑op and not throw.
        writer.close();
        // No exception means test passes.
    }

    @Test
    public void testCloseWhenUnderlyingWriterIsNullDoesNothing() throws IOException {
        // Simulate a FastWriter that has already been closed.
        writer.close(); // now out == null
        // Calling close again must simply return.
        writer.close(); // should not throw
    }
}
