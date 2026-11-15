package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * Unit tests for {@link StreamGobbler}.
 *
 * The tests cover:
 * <ul>
 *   <li>Reading a single line of text.</li>
 *   <li>Reading multiple lines with mixed line‑break characters.</li>
 *   <li>Handling an empty input stream.</li>
 *   <li>Capturing the message of an {@link IOException} thrown while reading.</li>
 * </ul>
 */
public class StreamGobblerTest {

    /**
     * Helper that creates an {@link InputStream} from the given string using the platform default charset.
     */
    private static InputStream stringToStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

    @Test
    public void testSingleLine() {
        String input = "hello world";
        StreamGobbler gobbler = new StreamGobbler(stringToStream(input));
        gobbler.run();

        String expected = input + System.getProperty("line.separator");
        assertEquals("Result should contain the single line followed by a line separator",
                     expected, gobbler.getResult());
    }

    @Test
    public void testMultipleLines() {
        // Use a mixture of LF and CRLF to verify BufferedReader normalises them.
        String input = "first line\nsecond line\r\nthird line";
        StreamGobbler gobbler = new StreamGobbler(stringToStream(input));
        gobbler.run();

        String sep = System.getProperty("line.separator");
        String expected = "first line" + sep + "second line" + sep + "third line" + sep;
        assertEquals("Result should contain all lines, each terminated by the platform line separator",
                     expected, gobbler.getResult());
    }

    @Test
    public void testEmptyStream() {
        StreamGobbler gobbler = new StreamGobbler(stringToStream(""));
        gobbler.run();

        assertEquals("Result for an empty stream should be an empty string", "", gobbler.getResult());
    }

    @Test
    public void testIOExceptionDuringRead() {
        // InputStream that always throws IOException
        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated failure");
            }
        };

        StreamGobbler gobbler = new StreamGobbler(failingStream);
        gobbler.run();

        assertEquals("Result should be the IOException message",
                     "simulated failure", gobbler.getResult());
    }

    @Test
    public void testResultIsStableAfterRun() {
        String input = "stable test";
        StreamGobbler gobbler = new StreamGobbler(stringToStream(input));
        gobbler.run();

        String firstCall = gobbler.getResult();
        String secondCall = gobbler.getResult();

        assertSame("Subsequent calls to getResult() should return the same String instance",
                   firstCall, secondCall);
    }
}
