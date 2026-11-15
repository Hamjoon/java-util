package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Comprehensive tests for {@link StreamGobbler}.
 *
 * The tests cover:
 *   • Normal operation with an empty stream.
 *   • Normal operation with several lines (including verification of the
 *     system‑dependent line separator).
 *   • The state of the object before {@code run()} is invoked.
 *   • The error handling path when an {@link IOException} is thrown while
 *     reading the stream.
 *
 * No mocking frameworks are used – only real objects that are constructed
 * from the classes visible to {@code StreamGobbler}.
 */
public class StreamGobblerTest
{
    private String lineSeparator;

    @Before
    public void setUp()
    {
        lineSeparator = System.getProperty("line.separator");
    }

    /**
     * Verify that a newly created {@code StreamGobbler} returns {@code null}
     * from {@code getResult()} before {@code run()} has been called.
     */
    @Test
    public void testGetResultBeforeRunReturnsNull()
    {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        assertNull("Result should be null before run()", gobbler.getResult());
    }

    /**
     * When the supplied {@code InputStream} contains no data, the result must
     * be the empty string (not {@code null} and not a single line separator).
     */
    @Test
    public void testRunWithEmptyStream()
    {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        gobbler.run();
        assertEquals("Result for empty stream should be empty string", "", gobbler.getResult());
    }

    /**
     * Provide a stream with multiple lines and verify that each line is
     * returned followed by the system line separator.
     */
    @Test
    public void testRunWithMultipleLines()
    {
        String source = "first line\nsecond line\r\nthird line";
        // Convert to bytes using UTF‑8 (the default charset used by InputStreamReader)
        InputStream in = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));

        StreamGobbler gobbler = new StreamGobbler(in);
        gobbler.run();

        // Expected: each line read by BufferedReader.readLine() loses its line terminator,
        // then StreamGobbler appends the system line separator.
        String expected = "first line" + lineSeparator +
                          "second line" + lineSeparator +
                          "third line" + lineSeparator;

        assertEquals("Result should contain all lines with system line separator",
                     expected, gobbler.getResult());
    }

    /**
     * Simulate an {@link IOException} during reading.  The {@code StreamGobbler}
     * must catch the exception and store its message as the result.
     *
     * The custom {@code FailingInputStream} throws an {@code IOException}
     * as soon as any read operation is attempted.
     */
    @Test
    public void testRunWithIOExceptionSetsResultToExceptionMessage()
    {
        InputStream failing = new FailingInputStream("simulated I/O failure");
        StreamGobbler gobbler = new StreamGobbler(failing);
        gobbler.run();

        assertEquals("Result must be the exception message from the I/O failure",
                     "simulated I/O failure", gobbler.getResult());
    }

    /**
     * Helper {@code InputStream} that throws an {@link IOException} on the first
     * read attempt.  This allows us to test the error‑handling path without any
     * external mocking framework.
     */
    private static class FailingInputStream extends InputStream
    {
        private final String message;
        private boolean firstCall = true;

        FailingInputStream(String message)
        {
            this.message = message;
        }

        @Override
        public int read() throws IOException
        {
            if (firstCall)
            {
                firstCall = false;
                throw new IOException(message);
            }
            // After the exception the stream is considered closed;
            // returning -1 signals end‑of‑stream.
            return -1;
        }
    }
}
