package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link FastWriter}.
 *
 * The test suite is built in four logical parts:
 *
 * 1. **Public API extraction** – a comment block that lists every public constructor
 *    and method together with its exact signature.
 *
 * 2. **Basic functionality tests** – one simple test per public method that verifies
 *    the normal (happy‑path) behaviour.
 *
 * 3. **Edge‑case & exception tests** – tests that exercise boundary conditions,
 *    illegal arguments and state‑related errors (e.g. using the writer after it has
 *    been closed).
 *
 * 4. **Combined test class** – all the above tests are merged into a single JUnit‑4
 *    test class that can be compiled and run directly.
 *
 * The tests use only the standard JDK classes (`StringWriter`, `IllegalArgumentException`,
 * `IndexOutOfBoundsException`, `IOException`) as required by the specification:
 * no mocking frameworks or external libraries are employed.
 */
public class FastWriterTest {

    /*** -----------------------------------------------------------------------
     *  1. PUBLIC API OF FastWriter (extracted from the source)
     * ----------------------------------------------------------------------- */
    // Constructors
    //   FastWriter(Writer out)
    //   FastWriter(Writer out, int bufferSize)

    // Instance methods
    //   void write(int c) throws IOException
    //   void write(char[] cbuf, int off, int len) throws IOException
    //   void write(String str, int off, int len) throws IOException
    //   void flush() throws IOException
    //   void close() throws IOException

    /*** -----------------------------------------------------------------------
     *  Test harness fields
     * ----------------------------------------------------------------------- */
    private StringWriter underlying;
    private FastWriter fastWriter;

    @Before
    public void setUp() {
        underlying = new StringWriter();
        // use the default buffer size (8192) for most tests
        fastWriter = new FastWriter(underlying);
    }

    @After
    public void tearDown() throws IOException {
        // Ensure the writer is closed after each test to avoid contamination
        if (fastWriter != null) {
            fastWriter.close();
        }
    }

    /*** -----------------------------------------------------------------------
     *  2. BASIC FUNCTIONALITY TESTS
     * ----------------------------------------------------------------------- */

    // 2.1 Constructor with custom buffer size
    @Test
    public void testConstructorWithCustomBufferSize() throws IOException {
        int customSize = 10;
        FastWriter fw = new FastWriter(new StringWriter(), customSize);
        // Write exactly `customSize` characters – they should be buffered,
        // not flushed yet.
        for (int i = 0; i < customSize; i++) {
            fw.write('a' + i);
        }
        // Underlying writer must still be empty because the buffer hasn't been flushed.
        assertEquals("", ((StringWriter) ((FastWriter) fw).out).toString());

        // Flush now and verify the data appears.
        fw.flush();
        assertEquals("abcdefghij", ((StringWriter) ((FastWriter) fw).out).toString());

        fw.close();
    }

    // 2.2 write(int) – normal case
    @Test
    public void testWriteInt() throws IOException {
        fastWriter.write('X');
        fastWriter.flush(); // force buffer to underlying writer
        assertEquals("X", underlying.toString());
    }

    // 2.3 write(char[], off, len) – small array fits into buffer
    @Test
    public void testWriteCharArrayFitsBuffer() throws IOException {
        char[] data = "hello".toCharArray();
        fastWriter.write(data, 0, data.length);
        fastWriter.flush();
        assertEquals("hello", underlying.toString());
    }

    // 2.4 write(char[], off, len) – length >= buffer size triggers direct write
    @Test
    public void testWriteCharArrayLargerThanBuffer() throws IOException {
        // Create a FastWriter with a tiny buffer to force the direct‑write path.
        FastWriter tiny = new FastWriter(new StringWriter(), 4);
        char[] big = new char[10];
        for (int i = 0; i < big.length; i++) {
            big[i] = (char) ('a' + i);
        }
        tiny.write(big, 0, big.length); // should bypass the internal buffer
        tiny.flush();
        assertEquals("abcdefghij", ((StringWriter) ((FastWriter) tiny).out).toString());
        tiny.close();
    }

    // 2.5 write(String, off, len) – normal case
    @Test
    public void testWriteString() throws IOException {
        String src = "The quick brown fox";
        fastWriter.write(src, 4, 5); // writes "quick"
        fastWriter.flush();
        assertEquals("quick", underlying.toString());
    }

    // 2.6 flush() – ensures buffered data is written
    @Test
    public void testFlushWritesBufferedData() throws IOException {
        fastWriter.write('A');
        fastWriter.write('B');
        // No explicit flush yet – underlying writer should be empty.
        assertEquals("", underlying.toString());

        fastWriter.flush();
        assertEquals("AB", underlying.toString());
    }

    // 2.7 close() – flushes remaining data and releases resources
    @Test
    public void testCloseFlushesAndNullsInternalState() throws IOException {
        fastWriter.write('Z');
        fastWriter.close(); // close should flush the 'Z'
        assertEquals("Z", underlying.toString());

        // After close the internal `out` reference is set to null;
        // a subsequent write must throw IOException.
        try {
            fastWriter.write('Y');
            fail("Expected IOException after close");
        } catch (IOException expected) {
            // expected
        }
    }

    /*** -----------------------------------------------------------------------
     *  3. EDGE‑CASE & EXCEPTION TESTS
     * ----------------------------------------------------------------------- */

    // 3.1 Constructor with illegal buffer size
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorIllegalBufferSize() {
        new FastWriter(new StringWriter(), 0);
    }

    // 3.2 write(int) after the writer has been closed
    @Test
    public void testWriteIntAfterCloseThrows() throws IOException {
        fastWriter.close();
        try {
            fastWriter.write('Q');
            fail("Expected IOException because writer is closed");
        } catch (IOException e) {
            assertEquals("FastWriter stream is closed.", e.getMessage());
        }
    }

    // 3.3 write(char[], off, len) with invalid arguments
    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidOffset() throws IOException {
        char[] buf = "abc".toCharArray();
        fastWriter.write(buf, -1, 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayInvalidLength() throws IOException {
        char[] buf = "abc".toCharArray();
        fastWriter.write(buf, 0, 4); // len exceeds array size
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testWriteCharArrayOffsetPlusLengthOverflow() throws IOException {
        char[] buf = "abc".toCharArray();
        fastWriter.write(buf, 2, 2); // 2+2 > buf.length
    }

    // 3.4 write(char[], off, len) with len == 0 (should be a no‑op, no exception)
    @Test
    public void testWriteCharArrayZeroLength() throws IOException {
        char[] buf = "nothing".toCharArray();
        fastWriter.write(buf, 3, 0); // nothing should be written
        fastWriter.flush();
        assertEquals("", underlying.toString());
    }

    // 3.5 write(char[], null) – NPE from JVM (not caught by FastWriter)
    @Test(expected = NullPointerException.class)
    public void testWriteCharArrayNullThrowsNPE() throws IOException {
        fastWriter.write(null, 0, 1);
    }

    // 3.6 write(String, off, len) with boundary values (full string)
    @Test
    public void testWriteStringFullRange() throws IOException {
        String txt = "boundary";
        fastWriter.write(txt, 0, txt.length());
        fastWriter.flush();
        assertEquals(txt, underlying.toString());
    }

    // 3.7 write(String, off, len) with zero length (no output)
    @Test
    public void testWriteStringZeroLength() throws IOException {
        fastWriter.write("any", 1, 0);
        fastWriter.flush();
        assertEquals("", underlying.toString());
    }

    // 3.8 flush() after close – should not throw, but simply be a no‑op
    @Test
    public void testFlushAfterCloseDoesNotThrow() throws IOException {
        fastWriter.close();
        fastWriter.flush(); // must not throw
    }

    // 3.9 close() called twice – second call should be a harmless no‑op
    @Test
    public void testCloseIdempotent() throws IOException {
        fastWriter.close();
        // second close must not throw
        fastWriter.close();
    }

    // 3.10 Multiple writes that exceed buffer size – ensure proper flushing cycles
    @Test
    public void testMultipleWritesExceedingBuffer() throws IOException {
        // Use a small buffer to force several flushes.
        FastWriter small = new FastWriter(new StringWriter(), 5);
        small.write("abc".toCharArray(), 0, 3); // buffer now has 3 chars
        small.write("defg".toCharArray(), 0, 4); // exceeds remaining 2 slots -> flush then buffer 4 chars
        small.write('h');                         // buffer now 5 chars, full -> will be flushed on next write
        small.write('i');                         // triggers flush of previous full buffer
        small.flush();                            // flush any remaining data
        String result = ((StringWriter) ((FastWriter) small).out).toString();
        assertEquals("abcdefghi", result);
        small.close();
    }
}
