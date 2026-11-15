package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.Permission;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StreamGobbler_run_Test {

    private static class NormalCase {

        final String input;

        final String expected;

        NormalCase(String input, String expected) {
            this.input = input;
            this.expected = expected;
        }
    }

    private static class MixedCase {

        final String input;

        final String expected;

        MixedCase(String input, String expected) {
            this.input = input;
            this.expected = expected;
        }
    }

    /**
     * SecurityManager that throws {@link SecurityException} when the
     * {@code line.separator} property is read.
     */
    private static class DenyLineSeparatorSecurityManager extends SecurityManager {

        @Override
        public String getProperty(String key) {
            if ("line.separator".equals(key)) {
                throw new SecurityException("Access denied to line.separator");
            }
            return super.getProperty(key);
        }

        @Override
        public void checkPermission(Permission perm) {
            // allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // allow everything else
        }
    }

    private static Stream<NormalCase> normalCases() {
        return Stream.of(new NormalCase("line1\nline2", "line1" + System.getProperty("line.separator") + "line2" + System.getProperty("line.separator")), new NormalCase("", ""), new NormalCase("single line without newline", "single line without newline" + System.getProperty("line.separator")));
    }

    private static Stream<MixedCase> mixedCases() {
        return Stream.of(new MixedCase("a\r\nb\r\nc", "a" + System.getProperty("line.separator") + "b" + System.getProperty("line.separator") + "c" + System.getProperty("line.separator")), new MixedCase("x\ry", "x" + System.getProperty("line.separator") + "y" + System.getProperty("line.separator")));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void runCollectsAllLinesIntoResult() {
        // GIVEN – an InputStream containing two lines
        String inputData = "first line\nsecond line";
        InputStream is = new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
        // Mock the static IOUtilities class so we can verify that close() is invoked.
        try (MockedStatic<IOUtilities> ioUtilitiesMock = mockStatic(IOUtilities.class)) {
            // WHEN – the StreamGobbler is executed
            StreamGobbler gobbler = new StreamGobbler(is);
            gobbler.run();
            // THEN – the result contains each line followed by the system line separator
            String expected = "first line" + System.getProperty("line.separator") + "second line" + System.getProperty("line.separator");
            assertEquals(expected, gobbler.getResult(), "The concatenated result should match the input lines with separators.");
            // Verify that both the InputStreamReader and BufferedReader were closed.
            ioUtilitiesMock.verify(() -> IOUtilities.close(any()), times(2));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void runHandlesIOExceptionAndStoresMessage() throws IOException {
        // GIVEN – a mocked InputStream that throws IOException on any read attempt
        InputStream mockInput = mock(InputStream.class);
        when(mockInput.read()).thenThrow(new IOException("simulated failure"));
        // Some readers call the three‑argument read(byte[],int,int) overload,
        // so we stub that as well to be safe.
        when(mockInput.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("simulated failure"));
        // Mock the static IOUtilities class to verify close() calls.
        try (MockedStatic<IOUtilities> ioUtilitiesMock = mockStatic(IOUtilities.class)) {
            // WHEN – the StreamGobbler is executed with the faulty stream
            StreamGobbler gobbler = new StreamGobbler(mockInput);
            gobbler.run();
            // THEN – the result should be the exception message
            assertEquals("simulated failure", gobbler.getResult(), "When an IOException occurs, the result should be the exception message.");
            // Verify that close() was still invoked for both the reader and the buffer.
            ioUtilitiesMock.verify(() -> IOUtilities.close(any()), times(2));
        }
    }

    @ParameterizedTest
    @MethodSource("mixedCases")
    void runHandlesMixedLineSeparators(MixedCase tc) {
        InputStream is = new ByteArrayInputStream(tc.input.getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<IOUtilities> ioMock = mockStatic(IOUtilities.class)) {
            StreamGobbler gobbler = new StreamGobbler(is);
            gobbler.run();
            assertEquals(tc.expected, gobbler.getResult(), "BufferedReader.readLine() should normalize any line terminator to the system separator.");
            ioMock.verify(() -> IOUtilities.close(any()), times(2));
        }
    }

    @Test
    void runUsesCustomSystemLineSeparator() {
        String original = System.getProperty("line.separator");
        try {
            System.setProperty("line.separator", "\n---\n");
            InputStream is = new ByteArrayInputStream("lineA\nlineB".getBytes(StandardCharsets.UTF_8));
            try (MockedStatic<IOUtilities> ioMock = mockStatic(IOUtilities.class)) {
                StreamGobbler gobbler = new StreamGobbler(is);
                gobbler.run();
                String expected = "lineA" + "\n---\n" + "lineB" + "\n---\n";
                assertEquals(expected, gobbler.getResult(), "Result must use the custom line separator set in the system properties.");
                ioMock.verify(() -> IOUtilities.close(any()), times(2));
            }
        } finally {
            // restore original separator to avoid side‑effects on other tests
            System.setProperty("line.separator", original);
        }
    }

    @Test
    void runHandlesIOExceptionWithNullMessage() throws IOException {
        InputStream mockInput = mock(InputStream.class);
        IOException ex = new IOException(null);
        when(mockInput.read()).thenThrow(ex);
        when(mockInput.read(any(byte[].class), anyInt(), anyInt())).thenThrow(ex);
        try (MockedStatic<IOUtilities> ioMock = mockStatic(IOUtilities.class)) {
            StreamGobbler gobbler = new StreamGobbler(mockInput);
            gobbler.run();
            assertNull(gobbler.getResult(), "If the caught IOException has a null message, _result should be null.");
            ioMock.verify(() -> IOUtilities.close(any()), times(2));
        }
    }

    @Test
    void runWithNullInputStreamThrowsNPE() {
        InputStream nullStream = null;
        StreamGobbler gobbler = new StreamGobbler(nullStream);
        // The NPE is thrown inside the try block when InputStreamReader is created.
        assertThrows(NullPointerException.class, gobbler::run, "Passing a null InputStream should result in a NullPointerException.");
    }

    @Test
    void runThrowsIOExceptionWhenCloseFails() throws IOException {
        // Create a real InputStream that works, but mock IOUtilities.close to throw.
        InputStream is = new ByteArrayInputStream("ok".getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<IOUtilities> ioMock = mockStatic(IOUtilities.class)) {
            // First close (for InputStreamReader) succeeds, second (for BufferedReader) fails.
            ioMock.when(() -> IOUtilities.close(any())).thenAnswer(// first call – do nothing
            invocation -> null).thenThrow(// second call – throw
            new IOException("forced close error"));
            StreamGobbler gobbler = new StreamGobbler(is);
            IOException thrown = assertThrows(IOException.class, gobbler::run, "If IOUtilities.close throws, run() should let the IOException propagate.");
            assertEquals("forced close error", thrown.getMessage());
        }
    }

    @Test
    void runPropagatesSecurityExceptionOnLineSeparatorRead() {
        // Install a SecurityManager that denies access to "line.separator"
        SecurityManager originalSm = System.getSecurityManager();
        System.setSecurityManager(new DenyLineSeparatorSecurityManager());
        InputStream is = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
        try (MockedStatic<IOUtilities> ioMock = mockStatic(IOUtilities.class)) {
            StreamGobbler gobbler = new StreamGobbler(is);
            SecurityException se = assertThrows(SecurityException.class, gobbler::run, "Access to the line.separator property should cause a SecurityException.");
            assertNotNull(se);
            // Ensure close is still attempted (it will be called before the exception propagates)
            ioMock.verify(() -> IOUtilities.close(any()), atLeastOnce());
        } finally {
            // Restore original SecurityManager to avoid affecting other tests
            System.setSecurityManager(originalSm);
        }
    }
}
