package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link StreamGobbler}.
 *
 * <p>Each test method targets a specific behaviour of the class:
 *
 * <ul>
 *   <li>state before {@code run()} is invoked</li>
 *   <li>reading from an empty stream</li>
 *   <li>reading a single line without a line‑separator</li>
 *   <li>reading several lines with mixed line‑separator characters</li>
 *   <li>handling an {@link IOException} thrown by the underlying stream</li>
 * </ul>
 *
 * <p>No mocking framework is used – only concrete classes that are
 * visible from the source code or from the JDK are instantiated.
 */
public class StreamGobblerTest
{
    private String lineSeparator;

    @Before
    public void setUp()
    {
        // System line separator is used by StreamGobbler, capture it once.
        lineSeparator = System.getProperty("line.separator");
    }

    /**
     * Verify that {@code getResult()} returns {@code null}
     * before the gobbler has been executed.
     */
    @Test
    public void testGetResultBeforeRun_IsNull()
    {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        assertNull("Result must be null before run()", gobbler.getResult());
    }

    /**
     * An empty stream should produce an empty string (no line separator).
     */
    @Test
    public void testRun_WithEmptyStream_ReturnsEmptyString()
    {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        gobbler.run();
        assertEquals("Empty stream must yield empty result", "", gobbler.getResult());
    }

    /**
     * A stream that contains exactly one line (no terminating newline)
     * should have that line followed by a line separator in the result.
     */
    @Test
    public void testRun_WithSingleLine_NoTrailingNewline()
    {
        String text = "Hello World";
        InputStream is = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        StreamGobbler gobbler = new StreamGobbler(is);
        gobbler.run();
        assertEquals("Single line should be followed by system line separator",
                text + lineSeparator,
                gobbler.getResult());
    }

    /**
     * Multiple lines with different newline conventions (\n, \r\n) must all be
     * returned with the system line separator appended after each line.
     */
    @Test
    public void testRun_WithMultipleLines_MixedSeparators()
    {
        // The input contains three lines: LF, CRLF and a final line without terminator.
        String input = "line1\nline2\r\nline3";
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        StreamGobbler gobbler = new StreamGobbler(is);
        gobbler.run();

        String expected = "line1" + lineSeparator
                        + "line2" + lineSeparator
                        + "line3" + lineSeparator;
        assertEquals("All lines must be concatenated with system line separator", expected, gobbler.getResult());
    }

    /**
     * Simulate a failure while reading the stream.  The {@code run()} method should
     * catch the {@link IOException} and store its message as the result.
     */
    @Test
    public void testRun_WhenIOExceptionOccurs_StoresExceptionMessage()
    {
        final String errorMessage = "simulated read error";

        // Custom InputStream that throws IOException on the first read attempt.
        InputStream faulty = new InputStream()
        {
            private boolean firstCall = true;

            @Override
            public int read() throws IOException
            {
                if (firstCall)
                {
                    firstCall = false;
                    throw new IOException(errorMessage);
                }
                return -1; // end of stream after the exception (won't be reached)
            }
        };

        StreamGobbler gobbler = new StreamGobbler(faulty);
        gobbler.run();

        assertEquals("Result must contain the IOException message", errorMessage, gobbler.getResult());
    }
}
