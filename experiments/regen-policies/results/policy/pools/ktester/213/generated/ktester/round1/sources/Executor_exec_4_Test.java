package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.io.File;
import java.util.stream.Stream;

class Executor_exec_4_Test {

    private static final String THIS_IS_HANDY = "This is handy";

    private static final String ECHO_THIS_IS_HANDY = "echo " + THIS_IS_HANDY;

    private static Stream<Arguments> provideSuccessfulCommands() {
        String os = System.getProperty("os.name").toLowerCase();
        // 1. Simple echo command – works on both platforms via a shell.
        String[] echoCmd = os.contains("windows") ? new String[] { "cmd.exe", "/c", "echo hello" } : new String[] { "sh", "-c", "echo hello" };
        // 2. Windows dir command with a custom PATH (only used on Windows).
        String[] dirCmd = os.contains("windows") ? new String[] { "cmd.exe", "/c", "dir" } : // fallback for non‑Windows
        new String[] { "sh", "-c", "ls" };
        String[] pathEnv = os.contains("windows") ? // typical system path
        new String[] { "PATH=C:\\Windows\\System32" } : // no special env needed on Unix
        null;
        return Stream.of(Arguments.of("Simple echo command", echoCmd, null), Arguments.of("Directory listing with custom PATH (Windows)", dirCmd, pathEnv));
    }

    private static Stream<Arguments> provideErrorCommands() {
        return Stream.of(// 1. Non‑existent command → IOException
        Arguments.of("Non‑existent command", new String[] { "this_command_does_not_exist_12345" }, null), // 2. Null cmdarray → NullPointerException inside Runtime.exec
        Arguments.of("Null cmdarray", null, null), // 3. Empty cmdarray → IllegalArgumentException
        Arguments.of("Empty cmdarray", new String[] {}, null));
    }

    /**
     * Helper that determines whether the current OS is Windows.
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * Executes a simple echo command using the {@code exec(String[], String[])} overload.
     * The test adapts to Windows and Unix‑like platforms.
     */
    @Test
    void testExecArrayCommandSuccess() {
        Executor executor = new Executor();
        String os = System.getProperty("os.name").toLowerCase();
        String[] cmdArray;
        if (os.contains("windows")) {
            // Windows requires cmd.exe /c to run built‑in commands like echo
            cmdArray = new String[] { "cmd.exe", "/c", ECHO_THIS_IS_HANDY };
        } else {
            // Unix‑like shells can run echo directly
            cmdArray = new String[] { "sh", "-c", ECHO_THIS_IS_HANDY };
        }
        int exitCode = executor.exec(cmdArray, null);
        // The command should succeed; the exact exit code may be 0 on most platforms.
        assertTrue(exitCode == 0 || exitCode == 1, "Unexpected exit code: " + exitCode);
        assertEquals(THIS_IS_HANDY, executor.getOut().trim(), "The command output should match the expected string.");
    }

    /**
     * Executes a non‑existent command to verify that the method returns {@code -1}
     * and does not throw an exception.
     */
    @Test
    void testExecArrayCommandFailure() {
        Executor executor = new Executor();
        // Use a command that is very unlikely to exist on any platform.
        String[] cmdArray = new String[] { "this_command_does_not_exist_12345" };
        int exitCode = executor.exec(cmdArray, null);
        assertEquals(-1, exitCode, "Execution of an invalid command should return -1.");
        // When execution fails, no standard output is expected.
        assertTrue(executor.getOut().isEmpty() || executor.getOut().trim().isEmpty(), "Standard output should be empty on failure.");
    }

    /**
     * Parameterized test for the “Successful execution – normal path” group.
     *
     * <p>Two cases are exercised:
     * <ol>
     *   <li>Simple {@code echo hello} command.</li>
     *   <li>Windows {@code dir} command with a custom {@code PATH} environment variable.</li>
     * </ol>
     *
     * <p>Both commands are expected to complete without error, therefore {@code exec}
     * must return the exit value produced by {@code runIt} (normally {@code 0}).
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulCommands")
    void testExecArraySuccess(String description, String[] cmdArray, String[] envp) {
        Executor executor = new Executor();
        int exitCode = executor.exec(cmdArray, envp);
        // Successful execution must not return the error sentinel (-1).
        assertNotEquals(-1, exitCode, "Expected a non‑error exit code for: " + description);
        // The command should have produced some output (unless the command is silent).
        // For the echo case we verify the exact output; for others we only check that
        // something was captured (non‑null string).
        if (description.contains("echo")) {
            assertEquals("hello", executor.getOut().trim(), "Echo output should match the expected string.");
        } else if (description.contains("handy")) {
            assertEquals(THIS_IS_HANDY, executor.getOut().trim(), "Echo output should match the expected string.");
        } else {
            assertNotNull(executor.getOut(), "Standard output should not be null for successful commands.");
        }
    }

    /**
     * Additional success case – a single‑argument command array.
     *
     * <p>Running {@code echo} without any arguments should still succeed and produce
     * an empty line (or just a newline). The test verifies that {@code exec}
     * returns a non‑error code and that {@code getOut()} is not {@code null}.
     */
    @Test
    void testExecArraySingleArgumentSuccess() {
        Executor executor = new Executor();
        String os = System.getProperty("os.name").toLowerCase();
        String[] cmdArray = os.contains("windows") ? // Windows echo without args
        new String[] { "cmd.exe", "/c", "echo" } : // Unix echo without args
        new String[] { "sh", "-c", "echo" };
        int exitCode = executor.exec(cmdArray, null);
        assertNotEquals(-1, exitCode, "Single‑argument echo should not fail.");
        assertNotNull(executor.getOut(), "Output string must be non‑null.");
    }

    /**
     * Successful execution with quoted arguments.
     *
     * <p>This verifies that {@code cmdArrayToString} can handle complex arguments.
     * The command prints a quoted word; we check that the output contains the quotes.
     */
    @Test
    void testExecArrayQuotedArgumentSuccess() {
        Executor executor = new Executor();
        String os = System.getProperty("os.name").toLowerCase();
        String[] cmdArray = os.contains("windows") ? new String[] { "cmd.exe", "/c", "echo", "\"quoted\"" } : new String[] { "sh", "-c", "echo", "\"quoted\"" };
        int exitCode = executor.exec(cmdArray, null);
        assertNotEquals(-1, exitCode, "Command with quoted argument should succeed.");
        assertTrue(executor.getOut().contains("quoted"), "Output should contain the quoted word.");
    }

    /**
     * Successful execution with a custom environment variable.
     *
     * <p>Runs {@code echo $HOME} (or {@code echo %HOME%} on Windows) after setting
     * {@code HOME=/tmp} (or {@code HOME=C:\\Temp} on Windows). The command should
     * print the value we supplied, proving that {@code envp} is forwarded correctly.
     */
    @Test
    void testExecArrayWithCustomEnvSuccess() {
        Executor executor = new Executor();
        String os = System.getProperty("os.name").toLowerCase();
        String[] cmdArray;
        String[] envp;
        if (os.contains("windows")) {
            // Windows uses %VAR% syntax; we set HOME to a known value.
            cmdArray = new String[] { "cmd.exe", "/c", "echo", "%HOME%" };
            envp = new String[] { "HOME=C:\\Temp" };
        } else {
            cmdArray = new String[] { "sh", "-c", "echo $HOME" };
            envp = new String[] { "HOME=/tmp" };
        }
        int exitCode = executor.exec(cmdArray, envp);
        assertNotEquals(-1, exitCode, "Command with custom env should succeed.");
        assertEquals(os.contains("windows") ? "C:\\Temp" : "/tmp", executor.getOut().trim(), "The echoed HOME variable should match the value supplied in envp.");
    }

    /**
     * Parameterized test for error conditions that must result in {@code -1}.
     *
     * <p>Each case triggers a different exception from {@code Runtime.exec}:
     * <ul>
     *   <li>Invalid command → {@code IOException}</li>
     *   <li>Null command array → {@code NullPointerException}</li>
     *   <li>Empty command array → {@code IllegalArgumentException}</li>
     * </ul>
     *
     * <p>The method should catch the exception, print an error message, and return {@code -1}
     * without propagating the exception.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideErrorCommands")
    void testExecArrayErrorReturnsMinusOne(String description, String[] cmdArray, String[] envp) {
        Executor executor = new Executor();
        int exitCode = executor.exec(cmdArray, envp);
        assertEquals(-1, exitCode, "Expected -1 for error case: " + description);
        // On error we expect no standard output.
        assertTrue(executor.getOut() == null || executor.getOut().trim().isEmpty(), "Standard output should be empty for error case: " + description);
    }

    /**
     * Simulates {@code runIt} throwing {@code InterruptedException}.
     *
     * <p>Because {@code runIt} is private we cannot mock it directly.
     * Instead we start a process that sleeps for a long time and then interrupt
     * the current thread while {@code exec} is waiting for the process to finish.
     *
     * <p>The expected behaviour is that the {@code InterruptedException}
     * is caught by the outer {@code catch} block and {@code exec} returns {@code -1}.
     */
    @Test
    void testExecArrayRunItInterruptedException() throws Exception {
        Executor executor = new Executor();
        String os = System.getProperty("os.name").toLowerCase();
        String[] cmdArray = os.contains("windows") ? // approx 9 sec wait
        new String[] { "cmd.exe", "/c", "ping", "-n", "10", "127.0.0.1" } : // 5 sec wait
        new String[] { "sh", "-c", "sleep 5" };
        // Run exec in a separate thread so we can interrupt it.
        Thread execThread = new Thread(() -> {
            int exit = executor.exec(cmdArray, null);
            // The method should return -1 after interruption.
            assertEquals(-1, exit, "Interrupted execution must return -1.");
        });
        execThread.start();
        // Give the exec thread a moment to start the process.
        Thread.sleep(500);
        // Interrupt the thread while it is waiting for the process.
        execThread.interrupt();
        // Wait for the thread to finish.
        execThread.join(3000);
        assertFalse(execThread.isAlive(), "Exec thread should have terminated after interruption.");
    }
}
