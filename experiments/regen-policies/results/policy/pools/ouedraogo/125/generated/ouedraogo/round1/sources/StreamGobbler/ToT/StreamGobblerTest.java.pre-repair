/********************************************************************
 * JUnit 4 test suite for com.cedarsoftware.util.StreamGobbler
 *
 * The tests cover:
 *   • Normal operation with several lines of text
 *   • An empty input stream
 *   • The state of the object before run() is invoked
 *   • Handling of an IOException thrown by the underlying InputStream
 *
 * The test class resides in the same package as StreamGobbler so that it
 * can invoke the package‑private constructor.
 ********************************************************************/

package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;

/**
 * Test cases for {@link StreamGobbler}.
 */
public class StreamGobblerTest
{
    /** System line separator – cached once for readability. */
    private static final String LS = System.getProperty("line.separator");

    private StreamGobbler gobbler;

    /** Helper InputStream that always throws an IOException. */
    private static class FaultyInputStream extends InputStream
    {
        private final String message;

        FaultyInputStream(String message)
        {
            this.message = message;
        }

        @Override
        public int read() throws IOException
        {
            throw new IOException(message);
        }
    }

    @Before
    public void setUp()
    {
        // the gobbler will be (re)created in each test method
        gobbler = null;
    }

    @After
    public void tearDown()
    {
        gobbler = null;
    }

    /**
     * Verify that getResult() returns {@code null} before the runnable is executed.
     */
    @Test
    public void testResultBeforeRun()
    {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        gobbler = new StreamGobbler(empty);
        assertNull("Result should be null before run() is called", gobbler.getResult());
    }

    /**
     * Normal case – the input stream contains two lines.
     * The expected result should contain each line followed by the platform line separator.
     */
    @Test
    public void testRunWithMultipleLines()
    {
        String input = "first line\nsecond line";
        InputStream in = new ByteArrayInputStream(input.getBytes());
        gobbler = new StreamGobbler(in);

        gobbler.run();   // direct call – no thread needed for the test

        String expected = "first line" + LS + "second line" + LS;
        assertEquals("Result should contain both lines separated by the system line separator", 
                     expected, gobbler.getResult());
    }

    /**
     * Edge case – the input stream is empty.
     * The result should be an empty string (no line separators added).
     */
    @Test
    public void testRunWithEmptyStream()
    {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        gobbler = new StreamGobbler(empty);

        gobbler.run();

        assertEquals("Result for an empty stream should be an empty string", 
                     "", gobbler.getResult());
    }

    /**
     * Error scenario – the underlying InputStream throws an IOException.
     * StreamGobbler should capture the exception message as its result.
     */
    @Test
    public void testRunWithIOException()
    {
        final String errorMessage = "Simulated read failure";
        InputStream faulty = new FaultyInputStream(errorMessage);
        gobbler = new StreamGobbler(faulty);

        gobbler.run();

        assertEquals("Result should be the IOException message when a read error occurs",
                     errorMessage, gobbler.getResult());
    }
}
