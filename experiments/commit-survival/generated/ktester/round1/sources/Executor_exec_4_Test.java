package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.File;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Executor_exec_4_Test {

    /**
     * The class under test.
     */
    private Executor executor;

    /**
     * Captures anything written to {@code System.err} so that we can verify error handling.
     */
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    private PrintStream originalErr;

    @BeforeEach
    void setupBeforeEach() {
        executor = new Executor();
        // Redirect System.err for the duration of each test.
        originalErr = System.err;
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore original System.err so that subsequent tests (or the test runner) are unaffected.
        System.setErr(originalErr);
        errContent.reset();
    }

    /**
     * Provides command arrays for the successful execution test.
     *
     * @return a stream of arguments: command array, environment array (may be {@code null}),
     *         and a flag indicating whether the expected exit code is zero.
     */
    private static Stream<Arguments> successfulCommandProvider() {
        // Simple command that always exists and returns 0.
        String[] echoCmd = { "echo", "hello" };
        // Environment variable example – we simply forward the current PATH.
        String[] envWithPath = { "PATH=" + System.getenv("PATH") };
        // Command that is guaranteed to exist (java) but exits with a non‑zero code
        // because of an invalid option.
        String[] javaInvalidOption = { "java", "-invalidOption" };
        return Stream.of(// 0‑exit‑code case, with explicit environment.
        Arguments.of(echoCmd, envWithPath, true), // 0‑exit‑code case, without environment (null).
        Arguments.of(echoCmd, null, true), // Non‑zero exit‑code case, without environment.
        Arguments.of(javaInvalidOption, null, false));
    }

    /**
     * Checks whether the first element of a command array is available on the current OS.
     * This is a best‑effort guard to avoid false failures on platforms where the test command
     * does not exist (e.g., Windows vs. Unix).
     *
     * @param command the executable name (e.g., {@code "echo"} or {@code "java"}).
     * @return {@code true} if the command can be found in the {@code PATH}, {@code false} otherwise.
     */
    private static boolean isCommandAvailable(String command) {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        String[] dirs = path.split(File.pathSeparator);
        for (String dir : dirs) {
            File file = new File(dir, command);
            if (file.isFile() && file.canExecute()) {
                return true;
            }
            // Windows executables may have .exe extension.
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                File exe = new File(dir, command + ".exe");
                if (exe.isFile() && exe.canExecute()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verifies that a valid command array results in a non‑negative exit code.
     *
     * The command {@code java -version} is expected to exist on any system that can run this
     * test suite (the library itself depends on the JRE). The exact exit code is not mandated
     * by the JDK, but it is guaranteed to be {@code 0} for a successful execution.
     */
    @Test
    void exec_validCommand_returnsZero() {
        // GIVEN
        String[] cmd = { "java", "-version" };
        // inherit current environment
        String[] env = null;
        // WHEN
        int result = executor.exec(cmd, env);
        // THEN
        assertEquals(0, result, "A successful execution of a known command should return exit code 0");
        // No error output should have been produced.
        assertTrue(errContent.toString().isEmpty(), "System.err should be empty for a successful execution");
    }

    /**
     * Verifies that when {@code Runtime.exec} throws an exception (e.g. because the command
     * does not exist) the method returns {@code -1} and writes a helpful message to
     * {@code System.err}.
     */
    @Test
    void exec_invalidCommand_returnsMinusOneAndLogsError() {
        // GIVEN
        String[] cmd = { "this-command-should-not-exist-12345" };
        String[] env = null;
        // WHEN
        int result = executor.exec(cmd, env);
        // THEN
        assertEquals(-1, result, "When the underlying exec fails the method should return -1");
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Error occurred executing command"), "Error message should be printed to System.err");
        assertTrue(errOutput.contains("this-command-should-not-exist-12345"), "The failing command should be part of the error message");
    }

    /**
     * Verifies that passing {@code null} for the command array is handled gracefully.
     * The JDK throws a {@code NullPointerException} which is caught by the method,
     * resulting in a return value of {@code -1}.
     */
    @Test
    void exec_nullCommandArray_returnsMinusOne() {
        // GIVEN
        String[] cmd = null;
        String[] env = null;
        // WHEN
        int result = executor.exec(cmd, env);
        // THEN
        assertEquals(-1, result, "A null command array should cause the method to return -1");
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Error occurred executing command"), "An error should be logged when the command array is null");
    }

    /**
     * Executes a command that is expected to finish successfully.
     *
     * <p>Two sub‑cases are supplied via {@link #successfulCommandProvider()}:
     * <ul>
     *   <li>a command that returns exit code {@code 0} (e.g. {@code echo hello})</li>
     *   <li>a command that returns a non‑zero exit code (e.g. {@code java -invalidOption})</li>
     * </ul>
     *
     * The test asserts that {@code exec} returns the same exit code produced by the
     * underlying process and that nothing is written to {@code System.err}.
     */
    @ParameterizedTest(name = "[{index}] cmd={0}, env={1}, expectZero={2}")
    @MethodSource("successfulCommandProvider")
    void exec_successfulCommand_returnsProcessExitCode(String[] cmd, String[] env, boolean expectZero) {
        // Guard against platforms where the chosen command does not exist.
        Assumptions.assumeTrue(isCommandAvailable(cmd[0]), "Command not available on this platform");
        int result = executor.exec(cmd, env);
        if (expectZero) {
            assertEquals(0, result, "Expected exit code 0 for command: " + String.join(" ", cmd));
        } else {
            assertNotEquals(0, result, "Expected a non‑zero exit code for command: " + String.join(" ", cmd));
        }
        // No error output should have been produced.
        assertTrue(errContent.toString().isEmpty(), "System.err should be empty for a successful execution");
    }

    /**
     * An empty command array causes {@code Runtime.exec} to throw an
     * {@link IllegalArgumentException}. The method should return {@code -1}
     * and log the error.
     */
    @Test
    void exec_emptyCommandArray_returnsMinusOne() {
        String[] cmd = {};
        String[] env = null;
        int result = executor.exec(cmd, env);
        assertEquals(-1, result, "Empty command array should result in -1");
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Error occurred executing command"), "Error message should be printed for empty command array");
    }

    /**
     * A command array that contains a {@code null} element is illegal and results in a
     * {@link NullPointerException} from {@code Runtime.exec}. The method must return {@code -1}.
     */
    @Test
    void exec_commandArrayWithNullElement_returnsMinusOne() {
        String[] cmd = { null, "arg" };
        String[] env = null;
        int result = executor.exec(cmd, env);
        assertEquals(-1, result, "Command array containing null should result in -1");
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Error occurred executing command"), "Error message should be printed when command array contains null");
    }

    /**
     * A command that does not exist (e.g. {@code nonexistentcommand}) triggers an {@link java.io.IOException}.
     * The method should handle the exception and return {@code -1}.
     */
    @Test
    void exec_nonexistentCommand_returnsMinusOne() {
        String[] cmd = { "nonexistentcommand" };
        String[] env = null;
        int result = executor.exec(cmd, env);
        assertEquals(-1, result, "Non‑existent command should cause exec to return -1");
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Error occurred executing command"), "Error message should be printed for a nonexistent command");
    }
}
