/****************************************************************************************
 * StreamGobblerTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@link com.cedarsoftware.util.StreamGobbler}.
 *
 * The tests cover:
 *   • Construction and default state (result is null before run)
 *   • Normal operation with empty, single‑line and multi‑line streams
 *   • Proper handling of an IOException thrown by the underlying InputStream
 *
 * The test class resides in the same package as {@code StreamGobbler} to gain access to
 * its package‑private constructor.
 *
 * Author:  OpenAI ChatGPT
 * Date:    2026‑11‑14
 ****************************************************************************************/

package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class StreamGobblerTest {

    /** The platform line separator – used to build expected strings. */
    private static final String LS = System.getProperty("line.separator");

    private StreamGobbler gobbler;

    /** Helper that creates a StreamGobbler from a UTF‑8 string. */
    private StreamGobbler gobblerFromString(String data) {
        InputStream is = new ByteArrayInputStream(data.getBytes());
        return new StreamGobbler(is);
    }

    /** Helper that creates a StreamGobbler from a custom InputStream. */
    private StreamGobbler gobblerFromInputStream(InputStream is) {
        return new StreamGobbler(is);
    }

    @Before
    public void setUp() {
        // No shared state needed – each test creates its own gobbler.
        gobbler = null;
    }

    /** The result is null before {@code run()} is invoked. */
    @Test
    public void testResultIsNullBeforeRun() {
        gobbler = gobblerFromString("any");
        assertNull("Result should be null before run()", gobbler.getResult());
    }

    /** Empty input stream should produce an empty string (no line separator). */
    @Test
    public void testEmptyStream() {
        gobbler = gobblerFromString("");
        gobbler.run();
        assertEquals("Empty stream should yield empty result", "", gobbler.getResult());
    }

    /** Single line without a trailing newline should still have a line separator appended. */
    @Test
    public void testSingleLine() {
        String line = "Hello, World!";
        gobbler = gobblerFromString(line);
        gobbler.run();

        String expected = line + LS;
        assertEquals("Single line result mismatch", expected, gobbler.getResult());
    }

    /** Multiple lines with mixed line terminators are normalized to the platform separator. */
    @Test
    public void testMultipleLines() {
        // Use a mixture of LF and CRLF in the source string.
        String source = "first line\nsecond line\r\nthird line";
        gobbler = gobblerFromString(source);
        gobbler.run();

        String expected = "first line" + LS +
                          "second line" + LS +
                          "third line" + LS;
        assertEquals("Multi‑line result mismatch", expected, gobbler.getResult());
    }

    /**
     * Simulate an IOException while reading the stream.
     * The gobbler should capture the exception message as its result.
     */
    @Test
    public void testIOExceptionDuringRead() {
        final String errorMessage = "forced I/O error";

        InputStream throwingStream = new InputStream() {
            private boolean firstCall = true;

            @Override
            public int read() throws IOException {
                if (firstCall) {
                    firstCall = false;
                    throw new IOException(errorMessage);
                }
                // Should never be reached, but return -1 to indicate EOF.
                return -1;
            }

            @Override
            public void close() throws IOException {
                // No‑op – just to satisfy the close call.
            }
        };

        gobbler = gobblerFromInputStream(throwingStream);
        gobbler.run();

        assertEquals("Result should be the exception message", errorMessage, gobbler.getResult());
    }
}
