package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link StreamGobbler}.
 *
 * The tests cover:
 * <ul>
 *   <li>Behaviour before {@code run()} is invoked.</li>
 *   <li>Reading multiple lines with various line terminators.</li>
 *   <li>Reading a single line that does not end with a line separator.</li>
 *   <li>Handling an empty input stream.</li>
 *   <li>Proper handling of an {@link IOException} thrown by the wrapped {@link InputStream}.</li>
 * </ul>
 */
public class StreamGobblerTest {

    /** The system line separator used by {@link StreamGobbler}. */
    private static final String LS = System.getProperty("line.separator");

    @Test
    public void testResultIsNullBeforeRun() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        assertNull("Result should be null before run() is called", gobbler.getResult());
    }

    @Test
    public void testRunWithMultipleLines() {
        // Input contains a mixture of LF and CRLF line endings.
        String input = "first line\nsecond line\r\nthird line";
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        StreamGobbler gobbler = new StreamGobbler(is);

        gobbler.run(); // synchronous execution

        String expected = "first line" + LS +
                          "second line" + LS +
                          "third line" + LS;
        assertEquals("Result should contain each line followed by the system line separator",
                     expected, gobbler.getResult());
    }

    @Test
    public void testRunWithSingleLineWithoutNewline() {
        String input = "singleLineWithoutNewline";
        InputStream is = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        StreamGobbler gobbler = new StreamGobbler(is);

        gobbler.run();

        String expected = input + LS;
        assertEquals("A single line without a terminating newline should still be followed by a line separator",
                     expected, gobbler.getResult());
    }

    @Test
    public void testRunWithEmptyStream() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);

        gobbler.run();

        assertEquals("Empty stream should produce an empty result string", "", gobbler.getResult());
    }

    @Test
    public void testRunHandlesIOException() {
        InputStream failing = new FailingInputStream("forced failure");
        StreamGobbler gobbler = new StreamGobbler(failing);

        gobbler.run();

        assertEquals("When an IOException occurs, the result should be the exception message",
                     "forced failure", gobbler.getResult());
    }

    /**
     * Simple {@link InputStream} that throws an {@link IOException}
     * on every read operation.  Used to verify the error handling path
     * in {@link StreamGobbler#run()}.
     */
    private static class FailingInputStream extends InputStream {
        private final String message;

        FailingInputStream(String message) {
            this.message = message;
        }

        @Override
        public int read() throws IOException {
            throw new IOException(message);
        }
    }
}
