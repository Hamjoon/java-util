package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class StreamGobblerTest {

    private String lineSeparator;

    @Before
    public void setUp() {
        lineSeparator = System.getProperty("line.separator");
    }

    /**
     * Verify that before {@code run()} is called the result is {@code null}
     * and after processing a normal multi‑line stream the result contains each
     * line followed by the platform line separator.
     */
    @Test
    public void testRunWithMultipleLines() {
        String input = "first line\nsecond line\nthird line";
        ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes());

        StreamGobbler gobbler = new StreamGobbler(bais);

        // before execution result must be null
        assertNull("Result should be null before run()", gobbler.getResult());

        gobbler.run();

        // Build the expected result: each line + line separator
        StringBuilder expected = new StringBuilder();
        expected.append("first line").append(lineSeparator);
        expected.append("second line").append(lineSeparator);
        expected.append("third line").append(lineSeparator);

        assertEquals("Result should contain all lines with system line separators",
                expected.toString(), gobbler.getResult());
    }

    /**
     * Verify that an empty input stream results in an empty string (not {@code null}).
     */
    @Test
    public void testRunWithEmptyStream() {
        ByteArrayInputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);

        gobbler.run();

        assertEquals("Result for empty stream should be empty string", "", gobbler.getResult());
    }

    /**
     * Verify that a stream that throws an {@link IOException} during reading
     * causes the gobbler to store the exception message as the result.
     */
    @Test
    public void testRunWhenIOExceptionOccurs() {
        final String message = "Simulated read error";
        InputStream failingStream = new ExceptionThrowingInputStream(new IOException(message));

        StreamGobbler gobbler = new StreamGobbler(failingStream);
        gobbler.run();

        assertEquals("Result should be the IOException message", message, gobbler.getResult());
    }

    /**
     * Verify that the gobbler correctly handles Windows style line endings (\r\n).
     * BufferedReader.readLine() strips both characters, so the result should still
     * contain a single platform line separator after each logical line.
     */
    @Test
    public void testRunWithWindowsLineEndings() {
        String input = "line1\r\nline2\r\nline3\r\n";
        ByteArrayInputStream bais = new ByteArrayInputStream(input.getBytes());

        StreamGobbler gobbler = new StreamGobbler(bais);
        gobbler.run();

        StringBuilder expected = new StringBuilder();
        expected.append("line1").append(lineSeparator);
        expected.append("line2").append(lineSeparator);
        expected.append("line3").append(lineSeparator);

        assertEquals("Result should normalize Windows line endings to platform separator",
                expected.toString(), gobbler.getResult());
    }

    /**
     * Simple InputStream that throws the supplied IOException on any read attempt.
     */
    private static class ExceptionThrowingInputStream extends InputStream {
        private final IOException exception;

        ExceptionThrowingInputStream(IOException exception) {
            this.exception = exception;
        }

        @Override
        public int read() throws IOException {
            throw exception;
        }
    }
}
