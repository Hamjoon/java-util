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
import java.security.Permission;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StreamGobbler_run_Test {

    /**
     * Mocked static helper for {@link IOUtilities}.
     */
    private MockedStatic<IOUtilities> ioUtilitiesMock;

    @BeforeEach
    void setupBeforeEach() {
        // Mock the static close methods so that we can verify they are invoked
        // without depending on their actual implementation.
        ioUtilitiesMock = mockStatic(IOUtilities.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock.
        if (ioUtilitiesMock != null) {
            ioUtilitiesMock.close();
        }
    }

    /**
     * Creates a {@link StreamGobbler} for the given string content.
     */
    private static StreamGobbler gobblerFor(String content) throws UnsupportedEncodingException {
        InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
        return new StreamGobbler(is);
    }

    /**
     * Provides test data for the successful‑read parameterized test.
     */
    private static Stream<Arguments> successfulReadProvider() {
        return Stream.of(// multiple lines
        Arguments.of("line1\nline2\nline3", "line1" + System.lineSeparator() + "line2" + System.lineSeparator() + "line3" + System.lineSeparator()), // empty stream
        Arguments.of("", ""), // single line without trailing newline
        Arguments.of("onlyLine", "onlyLine" + System.lineSeparator()), // Windows CRLF line endings (BufferedReader normalises them)
        Arguments.of("winLine1\r\nwinLine2\r\n", "winLine1" + System.lineSeparator() + "winLine2" + System.lineSeparator()));
    }

    /**
     * Verifies that {@link StreamGobbler#run()} reads all lines from the supplied
     * {@link InputStream}, concatenates them with the platform line separator and
     * stores the result in the {@code _result} field (exposed via {@code getResult()}).
     */
    @Test
    void testRunReadsAllLines() throws IOException {
        // GIVEN – an InputStream containing two lines separated by '\n'
        String content = "first line\nsecond line";
        InputStream is = new ByteArrayInputStream(content.getBytes("UTF-8"));
        StreamGobbler gobbler = new StreamGobbler(is);
        // WHEN – the gobbler is executed
        gobbler.run();
        // THEN – the result should contain each line followed by the system line separator
        String expected = "first line" + System.lineSeparator() + "second line" + System.lineSeparator();
        assertEquals(expected, gobbler.getResult());
        // AND – the utility close methods should have been called exactly once each
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(InputStreamReader.class)), times(1));
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(BufferedReader.class)), times(1));
    }

    /**
     * Verifies that when an {@link IOException} occurs while reading the stream,
     * the exception message is captured as the result.
     */
    @Test
    void testRunHandlesIOException() throws IOException {
        // GIVEN – a mocked InputStream that throws IOException on any read attempt
        InputStream brokenStream = mock(InputStream.class);
        when(brokenStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("simulated I/O failure"));
        // InputStreamReader may also call the single‑byte read method; make it fail as well
        when(brokenStream.read()).thenThrow(new IOException("simulated I/O failure"));
        StreamGobbler gobbler = new StreamGobbler(brokenStream);
        // WHEN – the gobbler is executed
        gobbler.run();
        // THEN – the result should be the exception message
        assertEquals("simulated I/O failure", gobbler.getResult());
        // AND – the utility close methods should still be invoked despite the error
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(InputStreamReader.class)), times(1));
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(BufferedReader.class)), times(1));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("successfulReadProvider")
    void testRunSuccessfulRead(String input, String expected) throws Exception {
        StreamGobbler gobbler = gobblerFor(input);
        gobbler.run();
        assertEquals(expected, gobbler.getResult());
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(InputStreamReader.class)), times(1));
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(BufferedReader.class)), times(1));
    }

    @Test
    void testRunNullInputStreamThrowsNPE() {
        // GIVEN a null InputStream
        StreamGobbler gobbler = new StreamGobbler(null);
        // WHEN/THEN – constructing the InputStreamReader inside run() throws NPE
        assertThrows(NullPointerException.class, gobbler::run);
    }

    @Test
    void testRunCustomSystemLineSeparator() throws Exception {
        // Preserve original property
        String original = System.getProperty("line.separator");
        System.setProperty("line.separator", "<CUSTOM>");
        try {
            StreamGobbler gobbler = gobblerFor("a\nb");
            gobbler.run();
            String expected = "a<CUSTOM>b<CUSTOM>";
            assertEquals(expected, gobbler.getResult());
        } finally {
            // Restore original line separator
            if (original != null) {
                System.setProperty("line.separator", original);
            } else {
                System.clearProperty("line.separator");
            }
        }
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(InputStreamReader.class)), times(1));
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(BufferedReader.class)), times(1));
    }

    @Test
    void testRunRuntimeExceptionDuringClosePropagates() throws Exception {
        // GIVEN – a normal stream
        StreamGobbler gobbler = gobblerFor("content");
        // AND – the static close method is forced to throw a RuntimeException
        ioUtilitiesMock.when(() -> IOUtilities.close(any(InputStreamReader.class))).thenThrow(new RuntimeException("close failure"));
        ioUtilitiesMock.when(() -> IOUtilities.close(any(BufferedReader.class))).thenThrow(new RuntimeException("close failure"));
        // WHEN/THEN – the RuntimeException from the finally block propagates out of run()
        RuntimeException ex = assertThrows(RuntimeException.class, gobbler::run);
        assertEquals("close failure", ex.getMessage());
    }

    @Test
    void testRunUncheckedExceptionDuringReadPropagates() throws Exception {
        // GIVEN – an InputStream that throws NullPointerException on read()
        InputStream badStream = mock(InputStream.class);
        when(badStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new NullPointerException("read failure"));
        when(badStream.read()).thenThrow(new NullPointerException("read failure"));
        StreamGobbler gobbler = new StreamGobbler(badStream);
        // WHEN/THEN – the unchecked exception is not caught by run()
        NullPointerException ex = assertThrows(NullPointerException.class, gobbler::run);
        assertEquals("read failure", ex.getMessage());
        // The close calls are still attempted (and verified via the static mock)
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(InputStreamReader.class)), times(1));
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(BufferedReader.class)), times(1));
    }

    @Test
    void testRunSecurityExceptionOnLineSeparatorProperty() throws Exception {
        // Preserve the original security manager
        SecurityManager originalSm = System.getSecurityManager();
        // Install a SecurityManager that denies read access to "line.separator"
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // allow everything else
            }

            @Override
            public void checkPermission(Permission perm, Object context) {
                // allow everything else
            }

            @Override
            public void checkPropertyAccess(String key) {
                if ("line.separator".equals(key)) {
                    throw new SecurityException("access denied");
                }
            }
        });
        try {
            StreamGobbler gobbler = gobblerFor("test");
            // WHEN/THEN – the SecurityException propagates out of run()
            SecurityException se = assertThrows(SecurityException.class, gobbler::run);
            assertEquals("access denied", se.getMessage());
        } finally {
            // Restore original security manager
            System.setSecurityManager(originalSm);
        }
        // Even though the exception occurs before the finally block, the static mock
        // should still have been invoked (the close calls are in finally, which is
        // executed when the exception propagates).
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(InputStreamReader.class)), times(1));
        ioUtilitiesMock.verify(() -> IOUtilities.close(any(BufferedReader.class)), times(1));
    }
}
