package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.File;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class Executor_exec_2_Test {

    private static final String THIS_IS_HANDY = "This is handy";

    private static final String ECHO_THIS_IS_HANDY = "echo " + THIS_IS_HANDY;

    /**
     * The Executor instance used in each test.
     */
    private Executor executor;

    @BeforeEach
    void setupBeforeEach() {
        executor = new Executor();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure any captured output does not leak between tests.
        // The Executor class does not expose a reset method, so we simply discard the instance.
        executor = null;
    }

    /**
     * Supplies a stream of command arrays that are expected to succeed.
     */
    private static Stream<String[]> successfulCommandProvider() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return Stream.of(new String[] { "cmd.exe", "/c", "echo hello world" }, new String[] { "cmd.exe", "/c", "date /t" });
        } else {
            return Stream.of(new String[] { "/bin/sh", "-c", "echo hello world" }, new String[] { "/bin/sh", "-c", "date" });
        }
    }

    /**
     * Verifies that {@code exec(String[])} correctly runs a simple command,
     * returns the process exit code and captures the standard output.
     */
    @Test
    void testExecArrayCommandSuccess() {
        String os = System.getProperty("os.name").toLowerCase();
        int exitCode;
        if (os.contains("windows")) {
            // Windows needs the command interpreter.
            exitCode = executor.exec(new String[] { "cmd.exe", "/c", ECHO_THIS_IS_HANDY });
        } else {
            // Unix‑like systems can run the command directly.
            exitCode = executor.exec(new String[] { "/bin/sh", "-c", ECHO_THIS_IS_HANDY });
        }
        // The command should succeed (exit code 0) and the output should match.
        assertEquals(0, exitCode, "Process should exit with code 0");
        assertEquals(THIS_IS_HANDY, executor.getOut().trim(), "Standard output should contain the echoed string");
    }

    /**
     * Verifies that an invalid command results in the error handling path
     * returning {@code -1} and that the error message is recorded.
     */
    @Test
    void testExecArrayCommandFailure() {
        // Use a deliberately bogus command that does not exist.
        String[] bogusCommand = { "nonexistent_command_12345" };
        int exitCode = executor.exec(bogusCommand);
        // According to the implementation, any exception results in -1.
        assertEquals(-1, exitCode, "Process should return -1 on failure");
        // The error output should contain some indication of the failure.
        String err = executor.getError();
        assertNotNull(err, "Error output should not be null");
        assertTrue(err.toLowerCase().contains("error") || err.toLowerCase().contains("cannot") || err.toLowerCase().contains("not found"), "Error output should contain a failure indication");
    }

    /**
     * Parameterized test for a few simple commands that should exit with {@code 0}.
     *
     * @param cmdarray command array to execute
     */
    @ParameterizedTest(name = "Successful execution of {0}")
    @MethodSource("successfulCommandProvider")
    void testExecArraySuccessfulCommands(String[] cmdarray) {
        int exitCode = executor.exec(cmdarray);
        assertEquals(0, exitCode, "Process should exit with code 0");
        // For commands that produce output we verify that {@code getOut()} is not null.
        // The exact content depends on the OS, so we only assert non‑null here.
        assertNotNull(executor.getOut(), "Standard output should be captured");
    }

    /**
     * Verifies that passing {@code null} as the command array results in {@code -1}.
     */
    @Test
    void testExecArrayNullArgument() {
        int exitCode = executor.exec((String[]) null);
        assertEquals(-1, exitCode, "Null cmdarray should cause exec to return -1");
    }

    /**
     * Verifies that an empty command array results in {@code -1}.
     */
    @Test
    void testExecArrayEmptyArgument() {
        int exitCode = executor.exec(new String[] {});
        assertEquals(-1, exitCode, "Empty cmdarray should cause exec to return -1");
    }

    /**
     * Verifies that a command array containing a {@code null} element causes {@code -1}.
     */
    @Test
    void testExecArrayContainsNullElement() {
        int exitCode = executor.exec(new String[] { "echo", null });
        assertEquals(-1, exitCode, "Command array with null element should return -1");
    }

    /**
     * Verifies that a command array whose first element is an empty string results in {@code -1}.
     */
    @Test
    void testExecArrayFirstElementEmptyString() {
        int exitCode = executor.exec(new String[] { "" });
        assertEquals(-1, exitCode, "Command array with empty first element should return -1");
    }

    /**
     * Simulates {@link Runtime#exec(String[])} throwing a {@link SecurityException}.
     * The {@code exec} method should catch the exception and return {@code -1}.
     */
    @Test
    void testExecArraySecurityException() {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime fakeRuntime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(fakeRuntime);
            Mockito.when(fakeRuntime.exec(any(String[].class))).thenThrow(new SecurityException("Execution denied"));
            int exitCode = executor.exec(new String[] { "echo", "test" });
            assertEquals(-1, exitCode, "SecurityException should lead to return value -1");
        }
    }

    /**
     * Simulates {@link Process#waitFor()} throwing an {@link InterruptedException}
     * (the private {@code runIt} method is expected to propagate this). The outer
     * {@code exec} method catches the exception and returns {@code -1}.
     */
    @Test
    void testExecArrayInterruptedExceptionFromRunIt() throws Exception {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime fakeRuntime = Mockito.mock(Runtime.class);
            Process fakeProcess = Mockito.mock(Process.class);
            // Runtime.exec returns our mocked Process.
            runtimeMock.when(Runtime::getRuntime).thenReturn(fakeRuntime);
            Mockito.when(fakeRuntime.exec(any(String[].class))).thenReturn(fakeProcess);
            // Simulate waitFor throwing InterruptedException.
            Mockito.doThrow(new InterruptedException("Interrupted")).when(fakeProcess).waitFor();
            int exitCode = executor.exec(new String[] { "echo", "test" });
            assertEquals(-1, exitCode, "InterruptedException from runIt should result in -1");
        }
    }

    /**
     * Simulates {@link Runtime#exec(String[])} throwing an {@link IOException}
     * (e.g., command not found). The method should return {@code -1}.
     */
    @Test
    void testExecArrayIOException() throws Exception {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime fakeRuntime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(fakeRuntime);
            Mockito.when(fakeRuntime.exec(any(String[].class))).thenThrow(new java.io.IOException("Command not found"));
            int exitCode = executor.exec(new String[] { "nonexistentcommand" });
            assertEquals(-1, exitCode, "IOException should cause exec to return -1");
        }
    }

    /**
     * Simulates {@link Runtime#exec(String[])} throwing a {@link NullPointerException}
     * because the supplied array contains a {@code null} element.
     * The method should return {@code -1}.
     */
    @Test
    void testExecArrayNullPointerException() throws Exception {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime fakeRuntime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(fakeRuntime);
            Mockito.when(fakeRuntime.exec(any(String[].class))).thenThrow(new NullPointerException("Null element"));
            int exitCode = executor.exec(new String[] { "echo", null });
            assertEquals(-1, exitCode, "NullPointerException should cause exec to return -1");
        }
    }

    /**
     * Simulates {@link Runtime#exec(String[])} throwing an {@link IllegalArgumentException}
     * for an empty command array. The method should return {@code -1}.
     */
    @Test
    void testExecArrayIllegalArgumentException() throws Exception {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime fakeRuntime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(fakeRuntime);
            Mockito.when(fakeRuntime.exec(any(String[].class))).thenThrow(new IllegalArgumentException("Empty command array"));
            int exitCode = executor.exec(new String[] {});
            assertEquals(-1, exitCode, "IllegalArgumentException should cause exec to return -1");
        }
    }
}
