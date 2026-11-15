/*
 * StreamGobblerTest.java
 *
 * Comprehensive JUnit‑4 tests for com.cedarsoftware.util.StreamGobbler.
 *
 * The tests cover:
 *   • Normal operation with several lines (including different line‑break characters)
 *   • Empty input stream
 *   • Behaviour when an IOException is thrown while reading
 *   • The initial state of getResult() before run() is invoked
 *
 * Only public JDK classes, the StreamGobbler class itself and the
 * IOUtilities class (used internally) are referenced – no additional
 * third‑party libraries or mocking frameworks are used.
 */

package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class StreamGobblerTest {

    private String lineSeparator;

    @Before
    public void setUp() {
        // Capture the system line separator once for reuse in expectations.
        lineSeparator = System.getProperty("line.separator");
    }

    /**
     * Verify that getResult() returns {@code null} before the gobbler has been executed.
     */
    @Test
    public void testResultIsNullBeforeRun() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        assertNull("Result should be null before run() is called", gobbler.getResult());
    }

    /**
     * Normal scenario: the input stream contains several lines separated by a mixture
     * of LF and CRLF. BufferedReader.readLine() strips line terminators, therefore the
     * gobbler must append the platform‑specific line separator after each line.
     */
    @Test
    public void testRunWithMultipleLines() {
        // Input contains three logical lines, using both "\n" and "\r\n".
        String input = "first line\nsecond line\r\nthird line";
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));

        StreamGobbler gobbler = new StreamGobbler(is);
        gobbler.run();                     // Directly invoke; no thread needed.

        String expected = "first line" + lineSeparator
                        + "second line" + lineSeparator
                        + "third line" + lineSeparator;

        assertEquals("Result should contain each line followed by the system line separator",
                     expected, gobbler.getResult());
    }

    /**
     * Edge case: an empty input stream should result in an empty string (not null).
     */
    @Test
    public void testRunWithEmptyStream() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);

        StreamGobbler gobbler = new StreamGobbler(empty);
        gobbler.run();

        assertEquals("Result for an empty stream should be an empty string", "", gobbler.getResult());
    }

    /**
     * Simulate an IOException while reading the stream.
     * The gobbler should catch the exception and store its message as the result.
     */
    @Test
    public void testRunWhenIOExceptionOccurs() {
        final String errorMessage = "forced read error";

        // InputStream that throws IOException on any read attempt.
        InputStream failingStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException(errorMessage);
            }
        };

        StreamGobbler gobbler = new StreamGobbler(failingStream);
        gobbler.run();

        assertEquals("When an IOException occurs, the result should be the exception message",
                     errorMessage, gobbler.getResult());
    }
}
