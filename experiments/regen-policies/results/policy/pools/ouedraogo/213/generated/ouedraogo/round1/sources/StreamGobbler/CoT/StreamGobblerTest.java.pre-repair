package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link StreamGobbler}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal operation with single and multiple lines</li>
 *   <li>Empty input stream</li>
 *   <li>Behaviour before {@code run()} is invoked</li>
 *   <li>Exception handling when the underlying {@code InputStream} throws {@code IOException}</li>
 *   <li>Null {@code InputStream} handling (should propagate {@code NullPointerException})</li>
 * </ul>
 */
public class StreamGobblerTest
{
    private static final String LINE_SEP = System.getProperty("line.separator");

    private StreamGobbler gobbler;

    @Before
    public void setUp()
    {
        // Each test creates its own StreamGobbler; setUp is only used for shared constants.
    }

    /** Helper InputStream that always throws the supplied IOException on any read operation. */
    private static class ThrowingInputStream extends InputStream
    {
        private final IOException toThrow;

        ThrowingInputStream(IOException e)
        {
            this.toThrow = e;
        }

        @Override
        public int read() throws IOException
        {
            throw toThrow;
        }
    }

    /* --------------------------------------------------------------------- *
     *  1. Basic contract tests
     * --------------------------------------------------------------------- */

    @Test
    public void testGetResultBeforeRunReturnsNull()
    {
        gobbler = new StreamGobbler(new ByteArrayInputStream(new byte[0]));
        assertNull("Result should be null before run() is called", gobbler.getResult());
    }

    @Test
    public void testRunWithEmptyStreamProducesEmptyString()
    {
        gobbler = new StreamGobbler(new ByteArrayInputStream(new byte[0]));
        gobbler.run();
        assertEquals("Result for an empty stream should be empty string", "", gobbler.getResult());
    }

    @Test
    public void testRunWithSingleLine()
    {
        String input = "Hello World";
        gobbler = new StreamGobbler(new ByteArrayInputStream(input.getBytes()));
        gobbler.run();
        String expected = input + LINE_SEP;
        assertEquals("Single line should be returned with trailing line separator", expected, gobbler.getResult());
    }

    @Test
    public void testRunWithMultipleLines()
    {
        String input = "Line1\nLine2\r\nLine3";
        gobbler = new StreamGobbler(new ByteArrayInputStream(input.getBytes()));
        gobbler.run();

        StringBuilder expected = new StringBuilder();
        expected.append("Line1").append(LINE_SEP);
        expected.append("Line2").append(LINE_SEP);
        expected.append("Line3").append(LINE_SEP);

        assertEquals("All lines should be concatenated with platform line separator", expected.toString(),
                gobbler.getResult());
    }

    /* --------------------------------------------------------------------- *
     *  2. Edge‑case and exception handling tests
     * --------------------------------------------------------------------- */

    @Test
    public void testRunWhenUnderlyingStreamThrowsIOException()
    {
        String errorMessage = "simulated read error";
        IOException ioException = new IOException(errorMessage);
        gobbler = new StreamGobbler(new ThrowingInputStream(ioException));

        gobbler.run();

        assertEquals("When an IOException occurs, its message should become the result",
                errorMessage, gobbler.getResult());
    }

    @Test(expected = NullPointerException.class)
    public void testRunWithNullInputStreamThrowsNullPointerException()
    {
        // The constructor is package‑private, so we can call it from the same package.
        gobbler = new StreamGobbler(null);
        gobbler.run(); // Expected to throw NPE before entering the catch block.
    }
}
