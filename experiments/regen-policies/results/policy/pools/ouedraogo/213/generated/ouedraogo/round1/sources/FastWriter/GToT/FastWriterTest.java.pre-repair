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
 * The suite covers:
 *   • All public constructors
 *   • All public {@code write} overloads
 *   • {@code flush()} and {@code close()}
 *   • Normal operation, edge‑cases and exception scenarios
 */
public class FastWriterTest {

    private static final int SMALL_BUFFER = 8;      // easy to trigger overflow
    private static final int MEDIUM_BUFFER = 32;   // used for medium‑size tests

    private StringWriter backingWriter;
    private FastWriter fastWriter;

    @Before
    public void setUp() {
        backingWriter = new StringWriter();
        fastWriter = new FastWriter(backingWriter, SMALL_BUFFER);
    }

    @After
    public void tearDown() throws IOException {
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    /* -------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesWriterWithDefaultBuffer() throws IOException {
        FastWriter writer = new FastWriter(new StringWriter());
        writer.write('a');
        writer.flush();
        assertEquals("a", ((StringWriter) writer.out).toString());
        writer.close();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithZeroBufferSizeThrows() {
        new FastWriter(new StringWriter(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeBufferSizeThrows() {
        new FastWriter(new StringWriter(), -10);
    }

    /* -------------------------------------------------
     *  write(int) tests
     * ------------------------------------------------- */

    @Test
    public void testWriteIntSingleChar() throws IOException {
        fastWriter.write('X');
        fastWriter.flush();
        assertEquals("X", backingWriter.toString());
    }

    @Test
    public void testWriteIntTriggersFlushWhenBufferFull() throws IOException {
        // Fill buffer completely
        for (int i = 0; i < SMALL_BUFFER; i++) {
            fastWriter.write('a' + i);
        }
        // At this point buffer is full but not flushed yet
        fastWriter.write('Z'); // this call should cause an implicit flush
        fastWriter.flush();    // flush any remaining data

        // Expect first SMALL_BUFFER chars + the 'Z' written after flush
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < SMALL_BUFFER; i++) {
            expected.append((char) ('a' + i));
        }
        expected.append('Z');
        assertEquals(expected.toString(), backingWriter.toString());
    }

    @Test(expected = IOException.class)
    public void testWriteIntAfterCloseThrows() throws IOException {
        fastWriter.close();
        fastWriter.write('Q');
    }

    /* -------------------------------------------------
     *  write(char[], off, len) tests
     * ------------------------------------------------- */

    @Test
    public void testWriteCharArrayNormal() throws IOException {
        char[] data = "hello".toCharArray();
        fastWriter.write(data, 0, data.length);
        fastWriter.flush();
        assertEquals("hello", backingWriter.toString());
    }

    @Test
    public void testWriteCharArrayZeroLengthNoOp() throws IOException {
        char[] data = "unchanged".toCharArray();
        fastWriter.write(data, 2, 0);
        fastWriter.flush();
        assertEquals("", backingWriter.toString());
    }

    @Test
    public void testWriteCharArrayLengthGreaterThanBufferWritesDirectly() throws IOException {
        // Create a char array larger than the buffer
        char[] large = new char[SMALL_BUFFER * 2];
        for (int i = 0; i < large.length; i++) {
            large[i] = (char) ('a' + (i % 26));
        }
        fastWriter.write(large, 0, large.length);
        fastWriter.flush();

        assertEquals(new String(large), backingWriter.toString());
    }

    @Test
    public void testWriteCharArraySpanningBufferBoundary() throws IOException {
        // Fill part of the buffer, then write a chunk that exceeds remaining space
        fastWriter.write("1234".toCharArray(), 0, 4); // 4 chars, buffer size 8, 4 left
        char[] extra = "ABCDEFG".toCharArray(); // 7 chars, will overflow
        fastWriter.write(extra, 0, extra.length);
        fastWriter.flush();

        // Expected: "1234ABCD" (first overflow flushes after 4+4 chars) + "EFG"
        assertEquals("1234ABCDEFG", backingWriter.toString());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeOffsetThrows() throws IOException {
        fastWriter.write("abc".toCharArray(), -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOffsetBeyondArrayThrows() throws IOException {
        fastWriter.write("abc".toCharArray(), 5, 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayNegativeLengthThrows() throws IOException {
        fastWriter.write("abc".toCharArray(), 0, -2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOffsetPlusLengthOutOfBoundsThrows() throws IOException {
        fastWriter.write("abc".toCharArray(), 1, 5);
    }

    @Test(expected = IOException.class)
    public void testWriteCharArrayAfterCloseThrows() throws IOException {
        fastWriter.close();
        fastWriter.write("test".toCharArray(), 0, 4);
    }

    /* -------------------------------------------------
     *  write(String, off, len) tests
     * ------------------------------------------------- */

    @Test
    public void testWriteStringFitsInBuffer() throws IOException {
        fastWriter.write("hello", 0, 5);
        fastWriter.flush();
        assertEquals("hello", backingWriter.toString());
    }

    @Test
    public void testWriteStringZeroLengthNoOp() throws IOException {
        fastWriter.write("anything", 3, 0);
        fastWriter.flush();
        assertEquals("", backingWriter.toString());
    }

    @Test
    public void testWriteStringLongerThanBufferMultipleFlushes() throws IOException {
        // String longer than buffer size 8, length 20
        String longStr = "ABCDEFGHIJKLMNOPQRST";
        fastWriter.write(longStr, 0, longStr.length());
        fastWriter.flush();

        assertEquals(longStr, backingWriter.toString());
    }

    @Test
    public void testWriteStringExactBufferBoundary() throws IOException {
        // Write exactly the buffer size (8 chars)
        fastWriter.write("12345678", 0, 8);
        // At this point the internal buffer should have flushed automatically
        assertEquals("12345678", backingWriter.toString());
    }

    @Test
    public void testWriteStringPartialBufferThenChunkThenRemainder() throws IOException {
        // Fill part of the buffer, then write a string that forces several internal steps
        fastWriter.write("ABCD", 0, 4); // 4 chars in buffer, 4 slots left
        String src = "WXYZ1234567890"; // 14 chars
        fastWriter.write(src, 0, src.length());
        fastWriter.flush();

        // Expected: first 4 from buffer + 4 to fill buffer => flushed "ABCDWXYZ"
        // Then remaining 10 chars written directly in chunks of 8 (buffer size)
        // => "12345678" flushed, remainder "90" left in buffer then flushed at end
        String expected = "ABCDWXYZ1234567890";
        assertEquals(expected, backingWriter.toString());
    }

    @Test(expected = IOException.class)
    public void testWriteStringAfterCloseThrows() throws IOException {
        fastWriter.close();
        fastWriter.write("closed", 0, 6);
    }

    /* -------------------------------------------------
     *  flush() tests
     * ------------------------------------------------- */

    @Test
    public void testFlushWritesBufferedData() throws IOException {
        fastWriter.write("abc".toCharArray(), 0, 3);
        // Nothing written yet because buffer not full
        assertEquals("", backingWriter.toString());

        fastWriter.flush();
        assertEquals("abc", backingWriter.toString());
    }

    @Test(expected = IOException.class)
    public void testFlushAfterCloseThrows() throws IOException {
        fastWriter.close();
        fastWriter.flush();
    }

    /* -------------------------------------------------
     *  close() tests
     * ------------------------------------------------- */

    @Test
    public void testCloseFlushesAndPreventsFurtherUse() throws IOException {
        fastWriter.write("beforeClose", 0, "beforeClose".length());
        fastWriter.close();

        // close should have flushed the data
        assertEquals("beforeClose", backingWriter.toString());

        // Subsequent operations must fail
        try {
            fastWriter.write('X');
            fail("Expected IOException after close");
        } catch (IOException e) {
            // expected
        }

        try {
            fastWriter.flush();
            fail("Expected IOException after close");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testCloseIdempotentWhenAlreadyClosed() throws IOException {
        fastWriter.close(); // first close
        fastWriter.close(); // second close should be a no‑op and not throw
    }

    /* -------------------------------------------------
     *  Additional edge case: extremely large string (>> buffer)
     * ------------------------------------------------- */

    @Test
    public void testWriteVeryLargeString() throws IOException {
        // Use a buffer of 32 chars for this test to keep it fast
        FastWriter largeWriter = new FastWriter(new StringWriter(), MEDIUM_BUFFER);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            sb.append('A' + (i % 26));
        }
        String data = sb.toString();

        largeWriter.write(data, 0, data.length());
        largeWriter.flush();

        assertEquals(data, ((StringWriter) largeWriter.out).toString());
        largeWriter.close();
    }
}
