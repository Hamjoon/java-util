package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Executor_exec_6_Test {

    /**
     * Mocked static view of {@link Runtime}.
     */
    private MockedStatic<Runtime> runtimeStaticMock;

    /**
     * Capture of {@code System.err} output for assertions.
     */
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    private PrintStream originalErr;

    /**
     * Simple holder for a single test case.
     */
    private static class ExecExceptionCase {

        final String description;

        final String[] cmdarray;

        final String[] envp;

        final File dir;

        // exception that Runtime.exec should throw
        final Class<? extends Exception> exceptionToThrow;

        ExecExceptionCase(String description, String[] cmdarray, String[] envp, File dir, Class<? extends Exception> exceptionToThrow) {
            this.description = description;
            this.cmdarray = cmdarray;
            this.envp = envp;
            this.dir = dir;
            this.exceptionToThrow = exceptionToThrow;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Redirect System.err so we can assert on error messages.
        originalErr = System.err;
        System.setErr(new PrintStream(errContent));
        // Prepare a fresh static mock for each test.
        runtimeStaticMock = mockStatic(Runtime.class);
        // Redirect System.err so we can assert on error messages.
        originalErr = System.err;
        System.setErr(new PrintStream(errContent));
        // Fresh static mock for each test.
        runtimeStaticMock = mockStatic(Runtime.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore System.err.
        System.setErr(originalErr);
        errContent.reset();
        // Close the static mock.
        if (runtimeStaticMock != null) {
            runtimeStaticMock.close();
        }
        // Restore System.err.
        System.setErr(originalErr);
        errContent.reset();
        // Close the static mock.
        if (runtimeStaticMock != null) {
            runtimeStaticMock.close();
        }
    }

    /**
     * Stream of all exception‑related test cases.
     */
    private static Stream<ExecExceptionCase> exceptionCases() {
        return Stream.of(// ----- Exception Handling -------------------------------------------------
        new ExecExceptionCase("Runtime.exec throws IOException because command does not exist", new String[] { "nonexistentcommand" }, null, null, IOException.class), new ExecExceptionCase("Runtime.exec throws NullPointerException because cmdarray is null", null, null, null, NullPointerException.class), // ----- Edge Cases ---------------------------------------------------------
        new ExecExceptionCase("Runtime.exec throws IllegalArgumentException because cmdarray contains null", new String[] { "echo", null }, null, null, IllegalArgumentException.class), // ----- Invalid Environment Array -------------------------------------------
        new ExecExceptionCase("Runtime.exec throws IllegalArgumentException because envp contains null", new String[] { "echo", "hello" }, new String[] { "PATH=/usr/bin", null }, null, IllegalArgumentException.class), // ----- Invalid Working Directory -------------------------------------------
        new ExecExceptionCase("Runtime.exec throws IOException because working directory does not exist", new String[] { "echo", "hello" }, null, new File("/nonexistent"), IOException.class), new ExecExceptionCase("Runtime.exec throws IOException because working directory is a file, not a directory", new String[] { "echo", "hello" }, null, // a regular file that exists – using a temporary file created at runtime
        // (the file will be created in the test method itself)
        new File("src/test/resources/sample.txt"), IOException.class), // ----- Empty Command Array -------------------------------------------------
        new ExecExceptionCase("Runtime.exec throws IllegalArgumentException because command array is empty", new String[] {}, null, null, IllegalArgumentException.class), // ----- Empty String Command Element ----------------------------------------
        new ExecExceptionCase("Runtime.exec throws IllegalArgumentException because first command element is empty string", new String[] { "", "arg" }, null, null, IllegalArgumentException.class), // ----- Security Manager Denial --------------------------------------------
        new ExecExceptionCase("Runtime.exec throws SecurityException because a SecurityManager denies execution", new String[] { "echo", "secure" }, null, null, SecurityException.class));
    }

    @Test
    void exec_whenRuntimeExecThrowsException_returnsMinusOneAndLogsError() throws Exception {
        // --------------------------------------------------------------
        // GIVEN – a Runtime mock that throws the requested exception
        // --------------------------------------------------------------
        Runtime runtimeMock = mock(Runtime.class);
        Exception toThrow;
        if (tc.exceptionToThrow == IOException.class) {
            toThrow = new IOException("simulated I/O failure");
        } else if (tc.exceptionToThrow == NullPointerException.class) {
            toThrow = new NullPointerException("cmdarray is null");
        } else if (tc.exceptionToThrow == IllegalArgumentException.class) {
            toThrow = new IllegalArgumentException("illegal argument");
        } else if (tc.exceptionToThrow == SecurityException.class) {
            toThrow = new SecurityException("execution denied by SecurityManager");
        } else {
            // Fallback – should never happen with the current data set
            toThrow = new RuntimeException("unexpected");
        }
        // Configure the mock to throw when exec is called with the exact arguments.
        when(runtimeMock.exec(tc.cmdarray, tc.envp, tc.dir)).thenThrow(toThrow);
        runtimeStaticMock.when(Runtime::getRuntime).thenReturn(runtimeMock);
        // --------------------------------------------------------------
        // WHEN – invoke the method under test
        // --------------------------------------------------------------
        Executor executor = new Executor();
        int result = executor.exec(tc.cmdarray, tc.envp, tc.dir);
        // --------------------------------------------------------------
        // THEN – verify contract
        // --------------------------------------------------------------
        assertEquals(-1, result, "Executor should return -1 when Runtime.exec throws " + tc.exceptionToThrow.getSimpleName());
        String errOutput = errContent.toString();
        // The generic prefix must always be present.
        assertTrue(errOutput.contains("Error occurred executing command"), "Standard error should contain the generic error prefix");
        // The original exception message must be propagated.
        assertTrue(errOutput.contains(toThrow.getMessage()), "Standard error should contain the original exception message");
        // A representation of the command array (or the fact that it was null) should appear.
        if (tc.cmdarray != null) {
            // The helper method cmdArrayToString is private; we only need to ensure that
            // something from the array appears in the log.
            for (String part : tc.cmdarray) {
                if (part != null && !part.isEmpty()) {
                    assertTrue(errOutput.contains(part), "Error output should contain command element: " + part);
                }
            }
        } else {
            assertTrue(errOutput.contains("null"), "Error output should indicate that cmdarray was null");
        }
    }

    /**
     * Demonstrates that when {@code Runtime.exec} succeeds and {@code runIt}
     * returns {@code 0}, the {@code exec} method propagates the value.
     *
     * <p>
     * We cannot mock the private {@code runIt} directly, but we can let it
     * execute against a {@link Process} mock that behaves like a finished
     * process (i.e. {@code waitFor()} returns {@code 0}).  This gives us a
     * deterministic, fast execution path without launching a real OS process.
     * </p>
     */
    @Test
    void exec_whenRuntimeExecSucceedsAndProcessFinishes_returnsZero() throws Exception {
        // GIVEN
        String[] cmd = { "echo", "hello" };
        Runtime runtimeMock = mock(Runtime.class);
        Process procMock = mock(Process.class);
        // Simulate a process that finishes immediately with exit code 0.
        when(procMock.waitFor()).thenReturn(0);
        // Some implementations of runIt may read streams; make them no‑op.
        when(procMock.getInputStream()).thenReturn(mock(java.io.InputStream.class));
        when(procMock.getErrorStream()).thenReturn(mock(java.io.InputStream.class));
        when(runtimeMock.exec(cmd, null, null)).thenReturn(procMock);
        runtimeStaticMock.when(Runtime::getRuntime).thenReturn(runtimeMock);
        Executor executor = new Executor();
        // WHEN
        int result = executor.exec(cmd, null, null);
        // THEN
        assertEquals(0, result, "When the mocked process finishes with exit code 0, exec should return 0");
        assertTrue(errContent.toString().isEmpty(), "No error output should be produced on successful execution");
    }
}
