package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StreamGobbler_run_Test {

    /**
     * Data provider for {@link #run_multipleLines_shouldCollectCorrectly}.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideMultipleLinesData() {
        String sep = System.getProperty("line.separator");
        return Stream.of(// single line
        arguments("only line", "only line" + sep), // two lines (same as the non‑parameterised test)
        arguments("line1\nline2", "line1" + sep + "line2" + sep), // three lines – matches the JSON example
        arguments("line1\nline2\nline3", "line1" + sep + "line2" + sep + "line3" + sep), // empty input – should result in an empty string
        arguments("", ""));
    }

    /**
     * ---------------------------------------------------------------
     *  Test that `run()` reads all lines from the supplied InputStream,
     *  concatenates them and appends the platform line separator after each
     *  line.
     *  ---------------------------------------------------------------
     */
    @Test
    void run_collectsLinesFromInputStream() throws IOException {
        // GIVEN – an InputStream containing two lines
        String input = "first line\nsecond line";
        InputStream is = new ByteArrayInputStream(input.getBytes());
        // AND – a StreamGobbler built around that stream
        StreamGobbler gobbler = new StreamGobbler(is);
        // WHEN – the runnable is executed
        gobbler.run();
        // THEN – the result should contain the two lines each followed by the
        //        system line separator
        String expected = "first line" + System.getProperty("line.separator") + "second line" + System.getProperty("line.separator");
        assertEquals(expected, gobbler.getResult(), "The gobbler should return the concatenated lines with separators");
    }

    /**
     * ---------------------------------------------------------------
     *  Test that `run()` gracefully handles an IOException thrown while
     *  reading from the InputStream and stores the exception message as the
     *  result.
     *  ---------------------------------------------------------------
     */
    @Test
    void run_handlesIOExceptionAndStoresMessage() {
        // GIVEN – an InputStream that throws IOException on any read()
        InputStream faultyStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("simulated read error");
            }
        };
        // AND – a StreamGobbler built around the faulty stream
        StreamGobbler gobbler = new StreamGobbler(faultyStream);
        // AND – mock static calls to IOUtilities.close so they don't interfere
        try (MockedStatic<IOUtilities> utilities = mockStatic(IOUtilities.class)) {
            // WHEN – the runnable is executed
            gobbler.run();
            // THEN – the result should be the exception message
            assertEquals("simulated read error", gobbler.getResult(), "The gobbler should store the IOException message as its result");
            // AND – IOUtilities.close should have been invoked for both the
            //        InputStreamReader and the BufferedReader (even if they are null)
            utilities.verify(() -> IOUtilities.close(any()), times(2));
        }
    }

    /* -----------------------------------------------------------------
     *  2. Parameterised test for several line‑counts (covers the JSON
     *     group run_MultipleLines).
     *  ----------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] lines={0}")
    @MethodSource("provideMultipleLinesData")
    void run_multipleLines_shouldCollectCorrectly(String inputData, String expectedResult) {
        InputStream is = new ByteArrayInputStream(inputData.getBytes());
        StreamGobbler gobbler = new StreamGobbler(is);
        // Execute the method under test
        gobbler.run();
        // Verify the concatenated output
        assertEquals(expectedResult, gobbler.getResult(), "Result must contain each line followed by the platform line separator");
    }

    /* -----------------------------------------------------------------
     *  3. Null InputStream – expects a NullPointerException to propagate.
     *     The finally block should still invoke IOUtilities.close() twice.
     *  ----------------------------------------------------------------- */
    @Test
    void run_nullInputStream_throwsNullPointerException() {
        // GIVEN – a StreamGobbler constructed with a null InputStream
        StreamGobbler gobbler = new StreamGobbler(null);
        // Mock static close calls to verify they are still executed
        try (MockedStatic<IOUtilities> utilities = mockStatic(IOUtilities.class)) {
            // WHEN / THEN – run() should throw NPE
            NullPointerException npe = assertThrows(NullPointerException.class, gobbler::run, "Running with a null InputStream must throw NullPointerException");
            // OPTIONAL: verify the message contains the field name (implementation detail)
            assertTrue(npe.getMessage() == null || npe.getMessage().contains("_inputStream"), "Exception message should reference the null input stream");
            // AND – the finally block must have attempted to close both resources
            utilities.verify(() -> IOUtilities.close(any()), times(2));
        }
    }

    /* -----------------------------------------------------------------
     *  4. IOException while reading – the exception message becomes the result.
     *     This mirrors the JSON group run_IOExceptionDuringRead and also
     *     verifies that the static close calls happen.
     *  ----------------------------------------------------------------- */
    @Test
    void run_ioExceptionDuringRead_storesExceptionMessage() {
        // GIVEN – an InputStream that throws IOException on any read()
        InputStream faultyStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("forced read error");
            }
        };
        StreamGobbler gobbler = new StreamGobbler(faultyStream);
        // Mock static close calls
        try (MockedStatic<IOUtilities> utilities = mockStatic(IOUtilities.class)) {
            // WHEN – execute run()
            gobbler.run();
            // THEN – the result must be the exception message
            assertEquals("forced read error", gobbler.getResult(), "The gobbler should store the IOException message as its result");
            // AND – verify that close() was invoked for both the reader and the buffer
            utilities.verify(() -> IOUtilities.close(any()), times(2));
        }
    }

    /* -----------------------------------------------------------------
     *  5. Additional sanity check: when the InputStream is empty,
     *     the result should be an empty string (no trailing separator).
     *  ----------------------------------------------------------------- */
    @Test
    void run_emptyInputStream_returnsEmptyString() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        StreamGobbler gobbler = new StreamGobbler(empty);
        gobbler.run();
        assertEquals("", gobbler.getResult(), "An empty stream should produce an empty result string");
    }
}
