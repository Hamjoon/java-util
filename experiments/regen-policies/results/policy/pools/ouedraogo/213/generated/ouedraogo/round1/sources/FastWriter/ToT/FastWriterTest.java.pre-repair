package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.*;

public class FastWriterTest {

    private static final int SMALL_BUFFER = 4;   // tiny buffer to force flushes
    private static final int DEFAULT_BUFFER = 8192;

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

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsZeroBufferSize() {
        new FastWriter(new StringWriter(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNegativeBufferSize() {
        new FastWriter(new StringWriter(), -10);
    }

    @Test
    public void constructorWithCustomBufferCreatesWriter() throws IOException {
        FastWriter w = new FastWriter(new StringWriter(), SMALL_BUFFER);
        w.write('a');
        w.flush();
        // No exception means the constructor worked with a small buffer
    }

    /*** write(int) tests ***/

    @Test
    public void writeIntSingleCharFitsInBuffer() throws IOException {
        fastWriter.write('x');
        fastWriter.flush();               // force buffer to be written out
        assertEquals("x", backingWriter.toString());
    }

    @Test
    public void writeIntTriggersFlushWhenBufferFull() throws IOException {
        FastWriter w = new FastWriter(new StringWriter(), SMALL_BUFFER);
        // Fill buffer-1 chars
        for (int i = 0; i < SMALL_BUFFER - 1; i++) {
            w.write('a' + i);
        }
        // Next write will cause a flush because nextChar+1 >= buffer length
        w.write('z');
        w.flush();
        assertEquals("abcdz".substring(0, SMALL_BUFFER), w.backingContent(w));
    }

    @Test
    public void writeIntAfterCloseThrowsIOException() throws IOException {
        fastWriter.close();
        try {
            fastWriter.write('c');
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }
    }

    /*** write(char[], off, len) tests ***/

    @Test
    public void writeCharArraySmallFitsInBuffer() throws IOException {
        char[] data = "hello".toCharArray();
        fastWriter.write(data, 0, data.length);
        fastWriter.flush();
        assertEquals("hello", backingWriter.toString());
    }

    @Test
    public void writeCharArrayLargerThanBufferWritesDirectly() throws IOException {
        FastWriter w = new FastWriter(new StringWriter(), SMALL_BUFFER);
        char[] data = "abcdefgh".toCharArray(); // length 8 > SMALL_BUFFER
        w.write(data, 0, data.length);
        w.flush();
        assertEquals("abcdefgh", w.backingContent(w));
    }

    @Test
    public void writeCharArrayWithInvalidParametersThrowsIndexOutOfBounds() throws IOException {
        char[] data = "test".toCharArray();
        try {
            fastWriter.write(data, -1, 2);
            fail("Expected IndexOutOfBoundsException for negative offset");
        } catch (IndexOutOfBoundsException ignored) {}

        try {
            fastWriter.write(data, 0, -5);
            fail("Expected IndexOutOfBoundsException for negative length");
        } catch (IndexOutOfBoundsException ignored) {}

        try {
            fastWriter.write(data, 3, 2); // off+len exceeds array length
            fail("Expected IndexOutOfBoundsException for out‑of‑range length");
        } catch (IndexOutOfBoundsException ignored) {}
    }

    @Test
    public void writeCharArrayZeroLengthIsNoOp() throws IOException {
        char[] data = "nothing".toCharArray();
        fastWriter.write(data, 0, 0); // should not affect output
        fastWriter.flush();
        assertEquals("", backingWriter.toString());
    }

    /*** write(String, off, len) tests ***/

    @Test
    public void writeStringShortFitsInBuffer() throws IOException {
        fastWriter.write("abc", 0, 3);
        fastWriter.flush();
        assertEquals("abc", backingWriter.toString());
    }

    @Test
    public void writeStringExactBufferBoundaryTriggersFlush() throws IOException {
        FastWriter w = new FastWriter(new StringWriter(), SMALL_BUFFER);
        w.write("ABCD", 0, 4); // fills buffer exactly
        // internal flush should have been called automatically
        w.write('X');          // start new buffer
        w.flush();
        assertEquals("ABCDX", w.backingContent(w));
    }

    @Test
    public void writeStringLongerThanBufferIsChunkedCorrectly() throws IOException {
        FastWriter w = new FastWriter(new StringWriter(), SMALL_BUFFER);
        String longStr = "ABCDEFGHIJ"; // length 10, > SMALL_BUFFER (4)
        w.write(longStr, 0, longStr.length());
        w.flush();
        assertEquals(longStr, w.backingContent(w));
    }

    @Test
    public void writeStringZeroLengthIsNoOp() throws IOException {
        fastWriter.write("anything", 0, 0);
        fastWriter.flush();
        assertEquals("", backingWriter.toString());
    }

    @Test
    public void writeStringAfterCloseThrowsIOException() throws IOException {
        fastWriter.close();
        try {
            fastWriter.write("oops", 0, 4);
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }
    }

    /*** flush() tests ***/

    @Test
    public void flushWritesBufferedData() throws IOException {
        fastWriter.write("partial");
        fastWriter.flush();
        assertEquals("partial", backingWriter.toString());
    }

    @Test
    public void flushWhenBufferEmptyIsNoOp() throws IOException {
        fastWriter.flush(); // nothing written yet
        assertEquals("", backingWriter.toString());
    }

    /*** close() tests ***/

    @Test
    public void closeFlushesRemainingBuffer() throws IOException {
        fastWriter.write("lastbits");
        fastWriter.close();
        assertEquals("lastbits", backingWriter.toString());
    }

    @Test
    public void closeIdempotentAndSubsequentWriteFails() throws IOException {
        fastWriter.close();
        // second close should be harmless
        fastWriter.close();

        try {
            fastWriter.write('a');
            fail("Expected IOException after close");
        } catch (IOException e) {
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }
    }

    /*** Helper to extract underlying StringWriter content ***/
    private String backingContent(FastWriter writer) throws IOException {
        // The FastWriter always writes to the Writer supplied in the constructor.
        // Since we always pass a StringWriter, we can safely cast.
        Writer underlying = getUnderlyingWriter(writer);
        return ((StringWriter) underlying).toString();
    }

    private Writer getUnderlyingWriter(FastWriter writer) throws IOException {
        // Use reflection to retrieve the private 'out' field.
        // This is permissible for testing internal state without altering the production code.
        try {
            java.lang.reflect.Field f = FastWriter.class.getDeclaredField("out");
            f.setAccessible(true);
            return (Writer) f.get(writer);
        } catch (ReflectiveOperationException e) {
            throw new IOException("Unable to access underlying writer", e);
        }
    }
}
