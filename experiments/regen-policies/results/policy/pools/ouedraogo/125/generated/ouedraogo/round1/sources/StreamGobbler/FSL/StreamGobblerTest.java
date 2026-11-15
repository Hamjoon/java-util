package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * Unit tests for {@link StreamGobbler}. The tests cover:
 * <ul>
 *   <li>Reading a single line</li>
 *   <li>Reading multiple lines and verifying the platform line separator</li>
 *   <li>Reading an empty stream (result should be an empty string)</li>
 *   <li>Handling of an {@link IOException} thrown by the underlying {@link InputStream}</li>
 * </ul>
 */
public class StreamGobblerTest {

    /** Helper to run a StreamGobbler synchronously. */
    private String runGobbler(InputStream is) {
        StreamGobbler gobbler = new StreamGobbler(is);
        gobbler.run();                 // run in the current thread
        return gobbler.getResult();    // result after run finishes
    }

    @Test
    public void testSingleLine() {
        String input = "single line without newline";
        InputStream is = new ByteArrayInputStream(input.getBytes());
        String result = runGobbler(is);

        // The implementation always appends a line separator after each line read.
        String expected = input + System.getProperty("line.separator");
        assertEquals("Result should contain the line plus a trailing line separator", expected, result);
    }

    @Test
    public void testMultipleLines() {
        String line1 = "first line";
        String line2 = "second line";
        String line3 = "third line";
        String input = line1 + "\n" + line2 + "\r\n" + line3; // mixed newline characters
        InputStream is = new ByteArrayInputStream(input.getBytes());

        String result = runGobbler(is);

        // BufferedReader.readLine() strips any line‑termination characters.
        // The gobbler then adds the platform line separator after each line.
        String sep = System.getProperty("line.separator");
        String expected = line1 + sep + line2 + sep + line3 + sep;
        assertEquals("All lines should be concatenated with the platform line separator", expected, result);
    }

    @Test
    public void testEmptyStream() {
        InputStream is = new ByteArrayInputStream(new byte[0]);
        String result = runGobbler(is);
        assertEquals("Empty stream should yield an empty string (no line separator)", "", result);
    }

    /**
     * An {@link InputStream} that throws an {@link IOException} on any read operation.
     */
    private static class FailingInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            throw new IOException("forced failure");
        }
    }

    @Test
    public void testIOExceptionHandling() {
        InputStream failing = new FailingInputStream();
        StreamGobbler gobbler = new StreamGobbler(failing);
        gobbler.run();
        String result = gobbler.getResult();

        // When an IOException occurs, the gobbler stores the exception message.
        assertEquals("Result should be the exception message from the IOException",
                     "forced failure", result);
    }
}
