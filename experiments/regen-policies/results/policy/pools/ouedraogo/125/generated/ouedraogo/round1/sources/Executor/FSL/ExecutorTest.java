package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link Executor}.
 *
 * <p>The tests exercise every public {@code exec(...)} overload, as well as the
 * {@code getOut()} and {@code getError()} accessor methods.  Real system
 * processes are launched – no mocking frameworks are used because the
 * assignment forbids mocking or subclassing library classes.</p>
 *
 * <p>Because the behaviour of some commands differs between operating systems,
 * the suite detects the platform at runtime and selects appropriate commands.
 * All temporary files / directories created during the tests are cleaned up.</p>
 */
public class ExecutorTest {

    private Executor executor;
    private boolean isWindows;

    @Before
    public void setUp() {
        executor = new Executor();
        isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    }

    @After
    public void tearDown() {
        executor = null;
    }

    /** Helper to run a command that prints a known string and exits with 0. */
    private String[] getEchoCommand(String message) {
        if (isWindows) {
            // cmd /c echo <message>
            return new String[] { "cmd", "/c", "echo", message };
        } else {
            // sh -c echo <message>
            return new String[] { "sh", "-c", "echo " + message };
        }
    }

    /** Helper to get a command that prints the current working directory. */
    private String[] getPwdCommand() {
        if (isWindows) {
            // cmd /c cd
            return new String[] { "cmd", "/c", "cd" };
        } else {
            // pwd
            return new String[] { "pwd" };
        }
    }

    /** Helper to get a command that prints the value of an environment variable named VAR. */
    private String[] getEnvPrintCommand(String varName) {
        if (isWindows) {
            // cmd /c echo %VAR%
            return new String[] { "cmd", "/c", "echo", "%" + varName + "%" };
        } else {
            // sh -c echo $VAR
            return new String[] { "sh", "-c", "echo $" + varName };
        }
    }

    @Test
    public void testExecStringSuccess() {
        // Use a simple cross‑platform command that always succeeds.
        String command = isWindows ? "cmd /c echo HelloWorld" : "echo HelloWorld";

        int exit = executor.exec(command);
        assertEquals("Process should exit with code 0", 0, exit);

        // The stdout should contain the message (trim to ignore line‑break differences).
        assertNotNull("stdout should not be null after successful exec", executor.getOut());
        assertEquals("HelloWorld", executor.getOut().trim());

        // No error output is expected.
        assertTrue("stderr should be empty or null", executor.getError() == null || executor.getError().trim().isEmpty());
    }

    @Test
    public void testExecStringFailure() {
        // An obviously invalid command – Runtime.exec will throw IOException.
        String bogusCommand = "this_command_does_not_exist_12345";

        int exit = executor.exec(bogusCommand);
        assertEquals("When command cannot be started, exec should return -1", -1, exit);

        // Since the process never started, getters must stay null.
        assertNull("stdout should be null after failed exec", executor.getOut());
        assertNull("stderr should be null after failed exec", executor.getError());
    }

    @Test
    public void testExecStringArraySuccess() {
        String[] cmd = getEchoCommand("ArraySuccess");
        int exit = executor.exec(cmd);
        assertEquals(0, exit);
        assertNotNull(executor.getOut());
        assertEquals("ArraySuccess", executor.getOut().trim());
        assertTrue(executor.getError() == null || executor.getError().trim().isEmpty());
    }

    @Test
    public void testExecStringArrayFailure() {
        // Command array with a non‑existent executable.
        String[] cmd = new String[] { "nonexistent_executable_9876" };
        int exit = executor.exec(cmd);
        assertEquals(-1, exit);
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    @Test
    public void testExecWithEnv() {
        // Define an environment variable and verify that the child process can read it.
        String varName = "EXECUTOR_TEST_VAR";
        String varValue = "ExecutorValue";

        String[] env = new String[] { varName + "=" + varValue };
        String[] cmd = getEnvPrintCommand(varName);

        int exit = executor.exec(cmd, env);
        assertEquals(0, exit);
        assertNotNull(executor.getOut());
        // The command prints the variable value plus a newline; trim to compare.
        assertEquals(varValue, executor.getOut().trim());
        assertTrue(executor.getError() == null || executor.getError().trim().isEmpty());
    }

    @Test
    public void testExecWithEnvAndDir() throws IOException {
        // Create a temporary directory and run a command that reports its cwd.
        File tempDir = File.createTempFile("execTestDir", "");
        tempDir.delete();
        assertTrue("Temporary directory could not be created", tempDir.mkdir());

        try {
            String[] cmd = getPwdCommand();

            int exit = executor.exec(cmd, null, tempDir);
            assertEquals(0, exit);
            assertNotNull(executor.getOut());

            // The output should be the absolute path of the temporary directory.
            assertEquals(tempDir.getAbsolutePath(), executor.getOut().trim());

            assertTrue(executor.getError() == null || executor.getError().trim().isEmpty());
        } finally {
            // Cleanup
            tempDir.delete();
        }
    }

    @Test
    public void testExecStringArrayWithEnvAndDir() throws IOException {
        // Combine array command, env var, and working directory.
        String varName = "EXECUTOR_COMBO_VAR";
        String varValue = "ComboValue";

        String[] env = new String[] { varName + "=" + varValue };
        File tempDir = File.createTempFile("execComboDir", "");
        tempDir.delete();
        assertTrue(tempDir.mkdir());

        try {
            // Command prints the env var; we also verify cwd via a second command.
            String[] cmdPrintEnv = getEnvPrintCommand(varName);

            int exitEnv = executor.exec(cmdPrintEnv, env, tempDir);
            assertEquals(0, exitEnv);
            assertEquals(varValue, executor.getOut().trim());

            // Now verify cwd using the same executor instance (previous getters are overwritten).
            String[] cmdPwd = getPwdCommand();
            int exitPwd = executor.exec(cmdPwd, null, tempDir);
            assertEquals(0, exitPwd);
            assertEquals(tempDir.getAbsolutePath(), executor.getOut().trim());
        } finally {
            tempDir.delete();
        }
    }

    @Test
    public void testGettersAfterMultipleCalls() {
        // First successful call
        String[] cmd1 = getEchoCommand("First");
        int exit1 = executor.exec(cmd1);
        assertEquals(0, exit1);
        assertEquals("First", executor.getOut().trim());

        // Second successful call with different output
        String[] cmd2 = getEchoCommand("Second");
        int exit2 = executor.exec(cmd2);
        assertEquals(0, exit2);
        assertEquals("Second", executor.getOut().trim());

        // Verify that error remains empty after successful executions
        assertTrue(executor.getError() == null || executor.getError().trim().isEmpty());
    }

    @Test
    public void testExecStringArrayWithNullEnvAndDir() {
        // Ensure that passing null for env and dir does not throw and behaves like the simple overload.
        String[] cmd = getEchoCommand("NullEnvDir");
        int exit = executor.exec(cmd, null, null);
        assertEquals(0, exit);
        assertEquals("NullEnvDir", executor.getOut().trim());
        assertTrue(executor.getError() == null || executor.getError().trim().isEmpty());
    }
}
