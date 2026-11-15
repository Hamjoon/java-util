package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * JUnit‑4 test suite for {@link StreamGobbler}.
 *
 * The tests cover:
 *   • Normal single‑line and multi‑line input
 *   • Empty input
 *   • Very long lines
 *   • IOException handling via a custom InputStream
 *   • Pre‑run state of {@code getResult()}
 */
public class StreamGobblerTest
{
    private static final String LINE_SEP = System.getProperty("line.separator");

    /** Helper that creates a StreamGobbler from a UTF‑8 byte array. */
    private StreamGobbler gobblerFromString(String data)
    {
        InputStream is = new ByteArrayInputStream(data.getBytes());
        return new StreamGobbler(is);
    }

    /** Helper that creates a StreamGobbler from a raw InputStream (used for error‑case tests). */
    private StreamGobbler gobblerFromInputStream(InputStream is)
    {
        return new StreamGobbler(is);
    }

    /** ------------------------------------------------------------
     *  BASIC BEHAVIOUR TESTS
     * ------------------------------------------------------------ */

    @Test
    public void testGetResultBeforeRun_isNull()
    {
        StreamGobbler gobbler = gobblerFromString("anything");
        assertNull("Result must be null before run() is invoked", gobbler.getResult());
    }

    @Test
    public void testRunSingleLine()
    {
        StreamGobbler gobbler = gobblerFromString("hello");
        gobbler.run();
        assertEquals("hello" + LINE_SEP, gobbler.getResult());
    }

    /** ------------------------------------------------------------
     *  EDGE CASES
     * ------------------------------------------------------------ */

    @Test
    public void testRunEmptyStream()
    {
        StreamGobbler gobbler = gobblerFromString("");
        gobbler.run();
        assertEquals("", gobbler.getResult());
    }

    @Test
    public void testRunMultipleLines()
    {
        String input = "first line\nsecond line\r\nthird line";
        // BufferedReader.readLine() treats \n, \r, \r\n as line terminators,
        // so we expect three calls to readLine().
        StreamGobbler gobbler = gobblerFromString(input);
        gobbler.run();

        String expected = "first line" + LINE_SEP +
                          "second line" + LINE_SEP +
                          "third line" + LINE_SEP;
        assertEquals(expected, gobbler.getResult());
    }

    @Test
    public void testRunVeryLongLine()
    {
        // 10 KB of 'a' characters
        StringBuilder sb = new StringBuilder(10 * 1024);
        for (int i = 0; i < 10 * 1024; i++) sb.append('a');
        String longLine = sb.toString();

        StreamGobbler gobbler = gobblerFromString(longLine);
        gobbler.run();

        assertEquals(longLine + LINE_SEP, gobbler.getResult());
    }

    /** ------------------------------------------------------------
     *  EXCEPTION HANDLING
     * ------------------------------------------------------------ */

    /** InputStream that throws an IOException on the first read(). */
    private static class IOExceptionInputStream extends InputStream
    {
        private final IOException ioException;

        IOExceptionInputStream(IOException e) { this.ioException = e; }

        @Override public int read() throws IOException
        {
            throw ioException;
        }
    }

    @Test
    public void testRunWhenInputStreamThrowsIOException()
    {
        String message = "simulated read failure";
        InputStream badStream = new IOExceptionInputStream(new IOException(message));
        StreamGobbler gobbler = gobblerFromInputStream(badStream);

        gobbler.run();

        // The gobbler stores e.getMessage() when an IOException occurs.
        assertEquals(message, gobbler.getResult());
    }

    /** ------------------------------------------------------------
     *  INVALID USAGE
     * ------------------------------------------------------------ */

    @Test(expected = NullPointerException.class)
    public void testRunWithNullInputStream_throwsNPE()
    {
        // The constructor is package‑private, but we can still call it from the same package.
        StreamGobbler gobbler = new StreamGobbler(null);
        gobbler.run(); // Expected to throw NPE when InputStreamReader tries to use the null stream.
    }
}
