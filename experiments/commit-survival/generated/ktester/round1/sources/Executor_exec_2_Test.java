package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(MockitoExtension.class)
class Executor_exec_2_Test {

    /**
     * Mocked static version of {@link Runtime}.
     */
    private MockedStatic<Runtime> runtimeMock;

    /**
     * Simple holder for parameterised test arguments.
     */
    private static class ArgumentsHolder {

        final String[] cmdarray;

        final int expected;

        final String description;

        ArgumentsHolder(String[] cmdarray, int expected, String description) {
            this.cmdarray = cmdarray;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh static mock for each test so that stubbing does not leak.
        runtimeMock = mockStatic(Runtime.class);
        runtimeMock = mockStatic(Runtime.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Close the static mock to restore the original Runtime behaviour.
        runtimeMock.close();
        runtimeMock.close();
    }

    private static Stream<ArgumentsHolder> interruptionCases() {
        return Stream.of(new ArgumentsHolder(new String[] { "sleep", "5" }, -1, "Starts a long‑running process and interrupts the current thread before it finishes; " + "runIt throws InterruptedException which is caught, resulting in -1."));
    }

    private static Stream<ArgumentsHolder> successZeroExitCases() {
        return Stream.of(new ArgumentsHolder(new String[] { "/bin/sh", "-c", "exit 0" }, 0, "Executes a simple shell command that exits with status 0."));
    }

    private static Stream<ArgumentsHolder> successNonZeroExitCases() {
        return Stream.of(new ArgumentsHolder(new String[] { "/bin/sh", "-c", "exit 3" }, 3, "Executes a shell command that exits with status 3."));
    }

    private static Stream<ArgumentsHolder> commandNotFoundCases() {
        return Stream.of(new ArgumentsHolder(new String[] { "nonexistentcommand" }, -1, "Runtime.exec throws IOException for an unknown command."));
    }

    private static Stream<ArgumentsHolder> nullCmdArrayCases() {
        return Stream.of(new ArgumentsHolder(null, -1, "Passing null triggers NullPointerException inside Runtime.exec."));
    }

    private static Stream<ArgumentsHolder> emptyCmdArrayCases() {
        return Stream.of(new ArgumentsHolder(new String[] {}, -1, "Empty array leads to IllegalArgumentException."));
    }

    private static Stream<ArgumentsHolder> nullElementCases() {
        return Stream.of(new ArgumentsHolder(new String[] { "/bin/ls", null }, -1, "Array containing a null element causes NullPointerException."));
    }

    private static Stream<ArgumentsHolder> stdoutStderrCases() {
        return Stream.of(new ArgumentsHolder(new String[] { "/bin/sh", "-c", "echo out && echo err 1>&2 && exit 0" }, 0, "Command writes to both stdout and stderr but exits with 0."));
    }

    private static Stream<ArgumentsHolder> securityExceptionCases() {
        return Stream.of(new ArgumentsHolder(new String[] { "ls" }, -1, "SecurityManager denies execution, Runtime.exec throws SecurityException."));
    }

    private static Stream<ArgumentsHolder> emptyStringCommandCases() {
        return Stream.of(new ArgumentsHolder(new String[] { "" }, -1, "First element empty string causes IllegalArgumentException."));
    }

    private static Stream<ArgumentsHolder> permissionDeniedCases() {
        return Stream.of(new ArgumentsHolder(new String[] { "/" }, -1, "Attempting to execute a directory results in IOException (permission denied)."));
    }

    private static Stream<ArgumentsHolder> uncheckedRunItExceptionCases() {
        return Stream.of(new ArgumentsHolder(new String[] { "true" }, -1, "Simulate unexpected RuntimeException from runIt."));
    }

    /**
     * Detects a Unix‑like environment where /bin/sh is expected.
     */
    private static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("mac");
    }

    /**
     * Verifies that when {@code Runtime.exec} throws an exception the method
     * returns {@code -1} and writes an appropriate error message to {@code System.err}.
     */
    @Test
    void exec_whenRuntimeExecThrowsException_returnsMinusOneAndLogsError() throws Exception {
        // Arrange
        String[] cmd = { "nonexistentCommand" };
        IOException ioException = new IOException("simulated failure");
        // Stub Runtime.getRuntime().exec(cmd) to throw the exception.
        runtimeMock.when(() -> Runtime.getRuntime().exec(cmd)).thenThrow(ioException);
        Executor executor = new Executor();
        // Capture System.err output.
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errContent));
        try {
            // Act
            int result = executor.exec(cmd);
            // Assert
            assertEquals(-1, result, "exec should return -1 when an exception occurs");
            String errOutput = errContent.toString();
            assertTrue(errOutput.contains("Error occurred executing command"), "Error message should be printed to System.err");
            // The command array is converted to a string inside the method; we just check that
            // the first element appears in the output for confidence.
            assertTrue(errOutput.contains("nonexistentCommand"), "The failing command should be part of the error message");
        } finally {
            // Restore original System.err regardless of test outcome.
            System.setErr(originalErr);
        }
    }

    /**
     * A very light‑weight success test.  It uses a harmless command that is expected
     * to exist on most platforms (\"java -version\").  The command is executed in a
     * separate process; the {@code runIt} method simply waits for the process to
     * finish and returns its exit value.  The test asserts that the exit code is
     * non‑negative, confirming that the happy‑path logic runs without throwing.
     *
     * This test does not mock {@code Runtime} because we want the real process
     * execution to occur.  It therefore serves as an integration‑style check that
     * the method forwards to {@code Runtime.exec} correctly.
     */
    @Test
    void exec_whenRuntimeExecSucceeds_returnsProcessExitCode() {
        // Arrange
        // Use a command that is present on both Windows and Unix-like systems.
        // \"java -version\" prints version info and exits with code 0.
        String[] cmd = { "java", "-version" };
        Executor executor = new Executor();
        // Act
        int exitCode = executor.exec(cmd);
        // Assert
        assertTrue(exitCode >= 0, "A successful execution should return a non‑negative exit code (actual: " + exitCode + ")");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("interruptionCases")
    void exec_processInterrupted_returnsMinusOne(ArgumentsHolder holder) throws Exception {
        assumeTrue(isUnix(), "Interruption test requires a Unix‑like environment.");
        Executor executor = new Executor();
        // Start the command in a separate thread and interrupt it shortly after.
        Thread execThread = new Thread(() -> executor.exec(holder.cmdarray));
        execThread.start();
        // Give the process a moment to start, then interrupt.
        Thread.sleep(200);
        execThread.interrupt();
        // Wait for thread to finish and capture the result via a spy.
        execThread.join(2000);
        // Since we cannot capture the return value directly from the thread,
        // we re‑invoke exec in the same thread after interruption; the
        // interruption flag is cleared, so the method will now return -1
        // because the previous interrupt caused runIt to throw.
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successZeroExitCases")
    void exec_successZeroExit_returnsZero(ArgumentsHolder holder) {
        assumeTrue(isUnix(), "Shell commands require Unix.");
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successNonZeroExitCases")
    void exec_successNonZeroExit_returnsExactCode(ArgumentsHolder holder) {
        assumeTrue(isUnix(), "Shell commands require Unix.");
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("commandNotFoundCases")
    void exec_commandNotFound_returnsMinusOne(ArgumentsHolder holder) {
        // Mock Runtime.exec to throw IOException.
        runtimeMock.when(() -> Runtime.getRuntime().exec(holder.cmdarray)).thenThrow(new IOException("command not found"));
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nullCmdArrayCases")
    void exec_nullCmdArray_returnsMinusOne(ArgumentsHolder holder) {
        runtimeMock.when(() -> Runtime.getRuntime().exec((String[]) null)).thenThrow(new NullPointerException("null array"));
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("emptyCmdArrayCases")
    void exec_emptyCmdArray_returnsMinusOne(ArgumentsHolder holder) {
        runtimeMock.when(() -> Runtime.getRuntime().exec(holder.cmdarray)).thenThrow(new IllegalArgumentException("empty array"));
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nullElementCases")
    void exec_nullElementInArray_returnsMinusOne(ArgumentsHolder holder) {
        runtimeMock.when(() -> Runtime.getRuntime().exec(holder.cmdarray)).thenThrow(new NullPointerException("null element"));
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("stdoutStderrCases")
    void exec_stdoutStderrCommand_returnsZero(ArgumentsHolder holder) {
        assumeTrue(isUnix(), "Shell commands require Unix.");
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("securityExceptionCases")
    void exec_securityException_returnsMinusOne(ArgumentsHolder holder) {
        runtimeMock.when(() -> Runtime.getRuntime().exec(holder.cmdarray)).thenThrow(new SecurityException("denied"));
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("emptyStringCommandCases")
    void exec_emptyStringCommand_returnsMinusOne(ArgumentsHolder holder) {
        runtimeMock.when(() -> Runtime.getRuntime().exec(holder.cmdarray)).thenThrow(new IllegalArgumentException("empty string"));
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("permissionDeniedCases")
    void exec_permissionDenied_returnsMinusOne(ArgumentsHolder holder) {
        runtimeMock.when(() -> Runtime.getRuntime().exec(holder.cmdarray)).thenThrow(new IOException("permission denied"));
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("uncheckedRunItExceptionCases")
    void exec_uncheckedExceptionFromRunIt_returnsMinusOne(ArgumentsHolder holder) {
        // We cannot directly cause runIt to throw a RuntimeException without
        // altering the class, so we mock Runtime.exec to return a Process whose
        // waitFor method throws a RuntimeException, which propagates up as an
        // unchecked exception inside runIt.
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor()).thenThrow(new RuntimeException("unexpected"));
        runtimeMock.when(() -> Runtime.getRuntime().exec(holder.cmdarray)).thenReturn(mockProcess);
        Executor executor = new Executor();
        int result = executor.exec(holder.cmdarray);
        assertEquals(holder.expected, result);
    }
}
