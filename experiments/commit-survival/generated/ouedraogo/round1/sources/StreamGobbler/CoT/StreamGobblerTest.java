package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link StreamGobbler}.
 *
 * The tests cover:
 * <ul>
 *   <li>Public API discovery</li>
 *   <li>Normal functional behaviour</li>
 *   <li>Edge cases (empty stream, multiple lines, null input)</li>
 *   <li>Exception handling (IO exception during read)</li>
 * </ul>
 */
public class StreamGobblerTest
{
    /** The system line separator – used to build the expected strings. */
    private static final String LS = System.getProperty("line.separator");

    /* ---------------------------------------------------------------------- */
    /* 1.  Public methods of StreamGobbler (extracted from the source)         */
    /* ---------------------------------------------------------------------- */
    // public String getResult()
    // public void run()

    /* ---------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    /** Creates a StreamGobbler for the supplied byte array. */
    private StreamGobbler newGobbler(String content)
    {
        InputStream is = new ByteArrayInputStream(content.getBytes());
        return new StreamGobbler(is);
    }

    /** Creates a StreamGobbler whose InputStream always throws IOException. */
    private StreamGobbler newFailingGobbler()
    {
        InputStream failing = new InputStream()
        {
            @Override
            public int read() throws IOException
            {
                throw new IOException("forced error");
            }
        };
        return new StreamGobbler(failing);
    }

    /* ---------------------------------------------------------------------- */
    /* 2.  Basic functional tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetResultBeforeRun_returnsNull()
    {
        StreamGobbler gobbler = new StreamGobbler(new ByteArrayInputStream(new byte[0]));
        assertNull("Result must be null before run() is called", gobbler.getResult());
    }

    @Test
    public void testRunWithSingleLine()
    {
        String input = "single line";
        StreamGobbler gobbler = newGobbler(input);
        gobbler.run();

        String expected = input + LS;
        assertEquals("Result should contain the line followed by the system line separator",
                     expected, gobbler.getResult());
    }

    @Test
    public void testRunWithMultipleLines()
    {
        String input = "first line\nsecond line\r\nthird line";
        // BufferedReader.readLine() strips all line terminators, so we only care about the logical lines.
        StreamGobbler gobbler = newGobbler(input);
        gobbler.run();

        String expected = "first line" + LS +
                          "second line" + LS +
                          "third line" + LS;
        assertEquals("Result should contain each line terminated by the system line separator",
                     expected, gobbler.getResult());
    }

    @Test
    public void testRunWithEmptyStream_returnsEmptyString()
    {
        StreamGobbler gobbler = newGobbler("");
        gobbler.run();

        assertEquals("Empty input should result in an empty string (not null)",
                     "", gobbler.getResult());
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  Edge‑case & exception handling tests                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRunWhenIOExceptionOccurs_setsResultToExceptionMessage()
    {
        StreamGobbler gobbler = newFailingGobbler();
        gobbler.run();

        assertEquals("When an IOException occurs, the result must be the exception message",
                     "forced error", gobbler.getResult());
    }

    @Test(expected = NullPointerException.class)
    public void testRunWithNullInputStream_throwsNullPointerException()
    {
        // The constructor is package‑private; we can still instantiate it here because the test
        // class resides in the same package.
        StreamGobbler gobbler = new StreamGobbler(null);
        gobbler.run(); // Expected to throw NPE from InputStreamReader
    }
}
