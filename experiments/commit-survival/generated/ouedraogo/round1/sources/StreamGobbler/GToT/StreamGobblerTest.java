package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * JUnit‑4 tests for {@link StreamGobbler}.
 *
 * The tests cover:
 *   • normal usage (single line, multiple lines, empty stream)
 *   • edge cases (very long line, IOException during read)
 *   • initial state of {@code getResult()}
 *   • constructor null‑argument defensive check
 *
 * The test class lives in the same package as {@code StreamGobbler}
 * so it can access the package‑private constructor.
 */
public class StreamGobblerTest {

    /** Helper to create a StreamGobbler from a string. */
    private StreamGobbler gobblerFromString(String content) {
        InputStream is = new ByteArrayInputStream(content.getBytes());
        return new StreamGobbler(is);
    }

    /** Helper to create a StreamGobbler from a raw InputStream. */
    private StreamGobbler gobblerFromInputStream(InputStream is) {
        return new StreamGobbler(is);
    }

    /** Basic sanity check – before run() the result must be null. */
    @Test
    public void testGetResultBeforeRun_isNull() {
        StreamGobbler gobbler = gobblerFromString("anything");
        assertNull("Result should be null before run()", gobbler.getResult());
    }

    /** Single line – result must be the line plus the platform line separator. */
    @Test
    public void testRun_singleLine() {
        StreamGobbler gobbler = gobblerFromString("hello world");
        gobbler.run();
        String expected = "hello world" + System.getProperty("line.separator");
        assertEquals("Result should contain the line followed by a line separator", expected,
                     gobbler.getResult());
    }

    /** Empty stream – result must be the empty string (no line separator). */
    @Test
    public void testRun_emptyStream() {
        StreamGobbler gobbler = gobblerFromString("");
        gobbler.run();
        assertEquals("Empty input should produce empty result", "", gobbler.getResult());
    }

    /** Multiple lines – each line must be followed by a line separator, including the last line. */
    @Test
    public void testRun_multipleLines() {
        String content = "first line\nsecond line\r\nthird line";
        // Use the system line separator as the expected delimiter, regardless of the line break chars in the source.
        String sep = System.getProperty("line.separator");
        String expected = "first line" + sep +
                          "second line" + sep +
                          "third line" + sep;

        StreamGobbler gobbler = gobblerFromString(content);
        gobbler.run();
        assertEquals("All lines should be concatenated with system line separator", expected,
                     gobbler.getResult());
    }

    /** Very long line (greater than typical buffer size) – ensure no truncation occurs. */
    @Test
    public void testRun_veryLongLine() {
        // 10,000 'a' characters
        StringBuilder sb = new StringBuilder(10_000);
        for (int i = 0; i < 10_000; i++) {
            sb.append('a');
        }
        String longLine = sb.toString();

        StreamGobbler gobbler = gobblerFromString(longLine);
        gobbler.run();

        String expected = longLine + System.getProperty("line.separator");
        assertEquals("Long line should be fully read", expected, gobbler.getResult());
    }

    /** InputStream that throws IOException – result must be the exception message. */
    @Test
    public void testRun_ioException_isCaptured() {
        final String errorMsg = "forced read error";

        InputStream badStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException(errorMsg);
            }
        };

        StreamGobbler gobbler = gobblerFromInputStream(badStream);
        gobbler.run();

        assertEquals("Result should be the exception message when an IOException occurs",
                     errorMsg, gobbler.getResult());
    }

    /** Passing null to the constructor should throw NullPointerException immediately. */
    @Test(expected = NullPointerException.class)
    public void testConstructor_nullInputStream_throwsNPE() {
        new StreamGobbler(null);
    }
}
