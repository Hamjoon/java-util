package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * JUnit 4 test cases for {@link StreamGobbler}.
 *
 * The test class is placed in the same package as {@code StreamGobbler}
 * so that the package‑private constructor can be accessed.
 */
public class StreamGobblerTest
{
    private String lineSeparator;

    @Before
    public void setUp()
    {
        lineSeparator = System.getProperty("line.separator");
    }

    /* -----------------------------------------------------------------
     *  Public methods of StreamGobbler
     * -----------------------------------------------------------------
     *  public String getResult()
     *  public void run()
     */

    /**
     * Verify that {@code getResult()} returns {@code null} before {@code run()}
     * has been executed.
     */
    @Test
    public void testGetResultBeforeRun()
    {
        StreamGobbler gobbler = new StreamGobbler(new ByteArrayInputStream(new byte[0]));
        assertNull("Result should be null before run()", gobbler.getResult());
    }

    /**
     * A simple single‑line stream should be returned with a trailing
     * platform‑specific line separator.
     */
    @Test
    public void testSingleLine()
    {
        String input = "Hello World";
        StreamGobbler gobbler = new StreamGobbler(
                new ByteArrayInputStream(input.getBytes()));
        gobbler.run();

        String expected = input + lineSeparator;
        assertEquals("Single line result mismatch", expected, gobbler.getResult());
    }

    /**
     * Multiple lines separated by various newline characters must be
     * concatenated with the system line separator after each line.
     */
    @Test
    public void testMultipleLines()
    {
        String raw = "first line\nsecond line\r\nthird line";
        StreamGobbler gobbler = new StreamGobbler(
                new ByteArrayInputStream(raw.getBytes()));
        gobbler.run();

        StringBuilder expected = new StringBuilder();
        expected.append("first line").append(lineSeparator);
        expected.append("second line").append(lineSeparator);
        expected.append("third line").append(lineSeparator);

        assertEquals("Multiple line result mismatch",
                     expected.toString(),
                     gobbler.getResult());
    }

    /**
     * An empty input stream should produce an empty string (no line separator).
     */
    @Test
    public void testEmptyStream()
    {
        StreamGobbler gobbler = new StreamGobbler(
                new ByteArrayInputStream(new byte[0]));
        gobbler.run();

        assertEquals("Result for empty stream should be empty string",
                     "", gobbler.getResult());
    }

    /**
     * Simulate an {@link IOException} while reading from the stream.
     * The gobbler must capture the exception message as its result.
     */
    @Test
    public void testIOExceptionDuringRead()
    {
        InputStream failingStream = new InputStream()
        {
            @Override
            public int read() throws IOException
            {
                throw new IOException("forced error");
            }
        };

        StreamGobbler gobbler = new StreamGobbler(failingStream);
        gobbler.run();

        assertEquals("Result should contain the exception message",
                     "forced error",
                     gobbler.getResult());
    }

    /**
     * Passing {@code null} as the {@link InputStream} should cause a
     * {@link NullPointerException} when {@code run()} attempts to create an
     * {@link InputStreamReader}. The exception is not caught inside the
     * class, so the test expects it to propagate.
     */
    @Test(expected = NullPointerException.class)
    public void testRunWithNullInputStream()
    {
        StreamGobbler gobbler = new StreamGobbler(null);
        gobbler.run();   // should throw NPE
    }
}
