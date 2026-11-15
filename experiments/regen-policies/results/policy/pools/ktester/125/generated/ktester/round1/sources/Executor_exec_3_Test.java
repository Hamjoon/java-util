package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.File;
import java.security.Permission;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@ExtendWith(MockitoExtension.class)
class Executor_exec_3_Test {

    private static final String THIS_IS_HANDY = "This is handy";

    private static final String ECHO_THIS_IS_HANDY = "echo " + THIS_IS_HANDY;

    private static final String HELLO = "hello";

    private static final String ECHO_HELLO = "echo " + HELLO;

    private static final String ECHO_QUOTED = "echo \"hello world\"";

    /**
     * Adjusts a command for Windows where commands must be executed through {@code cmd.exe}.
     */
    private static String adaptCommandForOs(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            // Windows needs to run through cmd.exe /c
            return "cmd.exe /c " + command;
        }
        return command;
    }

    /**
     * Returns {@code true} if the current OS is Unix‑like (Linux/macOS).
     */
    private static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return !(os.contains("windows"));
    }

    /**
     * Test data for successful executions (exit code >= 0).
     */
    private static Stream<Arguments> successfulExecProvider() {
        return Stream.of(// simple echo, no env
        Arguments.of(adaptCommandForOs(ECHO_HELLO), null, true), // env command with custom environment (Unix only)
        Arguments.of(adaptCommandForOs("env"), new String[] { "VAR1=VALUE1", "VAR2=VALUE2" }, isUnix()), // empty env array, simple echo
        Arguments.of(adaptCommandForOs(ECHO_HELLO), new String[0], true), // command with spaces and quotes
        Arguments.of(adaptCommandForOs(ECHO_QUOTED), null, true));
    }

    /**
     * Test data for commands that should return -1 (error path).
     */
    private static Stream<Arguments> errorExecProvider() {
        return Stream.of(// null command
        Arguments.of(null, null, true), // empty command string
        Arguments.of("", null, true), // non‑existent command
        Arguments.of("nonexistentcommand123", null, true), // malformed environment entry
        Arguments.of(adaptCommandForOs(ECHO_HELLO), new String[] { "BAD_ENTRY" }, true), // env array containing a null element
        Arguments.of(adaptCommandForOs(ECHO_HELLO), new String[] { "VAR1=VALUE1", null }, true), // security manager blocks execution
        Arguments.of(adaptCommandForOs("echo blocked"), null, true));
    }

    /**
     * Test data for commands that exit with a specific non‑zero status.
     */
    private static Stream<Arguments> nonZeroExitProvider() {
        // Unix: sh -c 'exit 2'   Windows: cmd.exe /c exit 2
        String cmd = isUnix() ? "sh -c 'exit 2'" : "cmd.exe /c exit 2";
        return Stream.of(Arguments.of(adaptCommandForOs(cmd), null, 2));
    }

    /**
     * Executes a simple echo command using the {@code exec(String, String[])} overload.
     * The test verifies that the command runs successfully and that the captured
     * standard output matches the expected string.
     */
    @Test
    void testExecWithEnvArraySuccess() {
        Executor executor = new Executor();
        String os = System.getProperty("os.name").toLowerCase();
        String command;
        // no special environment variables needed
        String[] envp = null;
        if (os.contains("windows")) {
            // Windows needs to run through cmd.exe
            command = "cmd.exe /c " + ECHO_THIS_IS_HANDY;
        } else {
            command = ECHO_THIS_IS_HANDY;
        }
        int exitCode = executor.exec(command, envp);
        assertEquals(0, exitCode, "The command should exit with code 0");
        assertEquals(THIS_IS_HANDY, executor.getOut().trim(), "The captured stdout should contain the echoed text");
    }

    /**
     * Executes a command that is guaranteed to fail. The {@code exec} method
     * should catch the exception and return {@code -1}.
     */
    @Test
    void testExecWithInvalidCommandReturnsMinusOne() {
        Executor executor = new Executor();
        // Use a command name that is highly unlikely to exist on any platform.
        String bogusCommand = "this_command_does_not_exist_12345";
        int exitCode = executor.exec(bogusCommand, null);
        assertEquals(-1, exitCode, "When execution fails, exec should return -1");
        // In failure case the output buffers are untouched; they may be empty or contain prior data.
        // We simply assert that they are not null.
        assertNotNull(executor.getOut(), "getOut() should never return null");
        assertNotNull(executor.getError(), "getError() should never return null");
    }

    @ParameterizedTest(name = "[{index}] exec \"{0}\" with env {1}")
    @MethodSource("successfulExecProvider")
    void testExecSuccessful(String command, String[] envp, boolean shouldRun) {
        assumeTrue(shouldRun, "Test skipped on this platform");
        Executor executor = new Executor();
        int exitCode = executor.exec(command, envp);
        assertTrue(exitCode >= 0, "A successful execution should return a non‑negative exit code");
        // For echo commands we can verify the captured stdout.
        if (command != null && command.contains("echo")) {
            assertEquals(HELLO, executor.getOut().trim(), "Standard output should contain the echoed text");
        }
    }

    @ParameterizedTest(name = "[{index}] exec \"{0}\" expects exit {2}")
    @MethodSource("nonZeroExitProvider")
    void testExecNonZeroExit(String command, String[] envp, int expectedExit) {
        Executor executor = new Executor();
        int exitCode = executor.exec(command, envp);
        assertEquals(expectedExit, exitCode, "The command should exit with the expected non‑zero status");
    }

    @ParameterizedTest(name = "[{index}] exec error case \"{0}\"")
    @MethodSource("errorExecProvider")
    void testExecErrorPath(String command, String[] envp, boolean shouldRun) {
        assumeTrue(shouldRun, "Test skipped on this platform");
        // Install a restrictive SecurityManager only for the specific case that
        // tests blocking execution.
        SecurityManager original = System.getSecurityManager();
        if ("echo blocked".equals(command) || command != null && command.contains("echo blocked")) {
            System.setSecurityManager(new SecurityManager() {

                @Override
                public void checkExec(String cmd) {
                    throw new SecurityException("exec blocked by test SecurityManager");
                }

                @Override
                public void checkPermission(Permission perm) {
                    // allow everything else
                }
            });
        }
        try {
            Executor executor = new Executor();
            int exitCode = executor.exec(command, envp);
            assertEquals(-1, exitCode, "When execution fails, exec should return -1");
            assertNotNull(executor.getOut(), "getOut() should never return null");
            assertNotNull(executor.getError(), "getError() should never return null");
        } finally {
            // Restore original SecurityManager to avoid side‑effects on other tests.
            System.setSecurityManager(original);
        }
    }

    @Test
    void testExecInterruptedWhileWaiting() throws Exception {
        assumeTrue(isUnix(), "Interrupted‑wait test uses 'sleep' which is Unix‑specific");
        Executor executor = new Executor();
        String command = adaptCommandForOs("sh -c 'sleep 5'");
        Thread execThread = new Thread(() -> {
            int exit = executor.exec(command, null);
            // The thread should be interrupted; the method catches the exception
            // and returns -1, which we assert after the thread finishes.
            assertEquals(-1, exit, "Interrupted execution should result in -1");
        });
        execThread.start();
        // Give the process a moment to start, then interrupt the thread.
        Thread.sleep(500);
        execThread.interrupt();
        // Wait for the thread to finish (with a timeout to avoid hanging tests).
        execThread.join(3000);
        assertFalse(execThread.isAlive(), "Exec thread should have terminated after interruption");
    }
}
