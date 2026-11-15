package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * JUnit 4 test suite for {@link StreamGobbler}.
 *
 * Covers:
 *  1. Normal operation with multiple lines (different newline characters).
 *  2. Empty input stream.
 *  3. Behaviour before {@code run()} is invoked.
 *  4. Exception handling when the underlying {@code InputStream} throws {@code IOException}.
 *
 * All tests use only the public API and the package‑private constructor that the class
 * provides. No external mocking frameworks are used, complying with the stated constraints.
 */
public class StreamGobblerTest
{
    /**
     * Verify that {@code getResult()} returns {@code null} before the gobbler has been executed.
     */
    @Test
    public void testGetResultBeforeRunReturnsNull()
    {
        StreamGobbler gobbler = new StreamGobbler(new ByteArrayInputStream(new byte[0]));
        assertNull("Result should be null before run()", gobbler.getResult());
    }

    /**
     * Normal case – the input contains several lines with mixed line‑break characters.
     * The gobbler must concatenate each line and append the system line separator after each line.
     */
    @Test
    public void testRunWithMultipleLines()
    {
        String line1 = "first line";
        String line2 = "second line";
        String line3 = "third line";
        // Mix LF and CRLF to ensure BufferedReader normalises them to separate lines.
        String input = line1 + "\n" + line2 + "\r\n" + line3;
        InputStream is = new ByteArrayInputStream(input.getBytes());

        StreamGobbler gobbler = new StreamGobbler(is);
        gobbler.run();

        String expected = line1 + System.getProperty("line.separator")
                        + line2 + System.getProperty("line.separator")
                        + line3 + System.getProperty("line.separator");
        assertEquals("Result should contain all lines separated by the system line separator",
                     expected, gobbler.getResult());
    }

    /**
     * Edge case – an empty stream should produce an empty string (not {@code null}).
     */
    @Test
    public void testRunWithEmptyStream()
    {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(is);
        gobbler.run();
        assertEquals("Empty input should result in an empty string", "", gobbler.getResult());
    }

    /**
     * Exception handling – when the underlying {@code InputStream} throws an {@code IOException},
     * the gobbler must capture the exception message as its result.
     */
    @Test
    public void testRunWhenIOExceptionOccurs()
    {
        InputStream faultyStream = new InputStream()
        {
            @Override
            public int read() throws IOException
            {
                throw new IOException("forced error");
            }
        };

        StreamGobbler gobbler = new StreamGobbler(faultyStream);
        gobbler.run();
        assertEquals("Result should be the exception message when an IOException occurs",
                     "forced error", gobbler.getResult());
    }
}
