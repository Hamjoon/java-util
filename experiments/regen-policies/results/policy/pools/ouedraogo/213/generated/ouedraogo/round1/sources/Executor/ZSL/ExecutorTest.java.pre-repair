package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Executor}.
 *
 * <p>The tests exercise every public {@code exec(...)} overload, as well as the
 * {@code getOut()}, {@code getError()} accessors and the internal handling of
 * working directories and environment variables.  The tests use real system
 * commands – no mocking is performed – because the assignment explicitly
 * forbids mocking the {@code Runtime} or {@code Process} classes.</p>
 *
 * <p>All commands are chosen to be portable: the test detects the host OS
 * (Windows vs. Unix‑like) and selects the appropriate shell command syntax.
 * Temporary files / directories are cleaned up after each test.</p>
 */
public class ExecutorTest {

    private static final boolean IS_WINDOWS = System.getProperty("os.name")
            .toLowerCase(Locale.ENGLISH).contains("win");

    private Executor executor;
    private File tempDir;   // used for the directory‑related tests

    @Before
    public void setUp() throws IOException {
        executor = new Executor();
        // create a temporary directory for tests that need a working directory
        tempDir = Files.createTempDirectory("execTestDir").toFile();
    }

    @After
    public void tearDown() {
        // delete the temporary directory recursively
        deleteRecursively(tempDir);
    }

    private void deleteRecursively(File f) {
        if (f == null || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                deleteRecursively(child);
            }
        }
        f.delete();
    }

    /** Helper to build a simple “print text” command that works on both OS families. */
    private String buildEchoCommand(String text) {
        if (IS_WINDOWS) {
            // cmd /c echo <text>
            return "cmd /c echo " + text;
        } else {
            // sh -c echo <text>
            return "sh -c \"echo " + text + "\"";
        }
    }

    /** Helper to build an array version of the same echo command. */
    private String[] buildEchoCommandArray(String text) {
        if (IS_WINDOWS) {
            return new String[]{"cmd", "/c", "echo", text};
        } else {
            return new String[]{"sh", "-c", "echo " + text};
        }
    }

    /** Helper to build a command that prints the value of an environment variable. */
    private String buildEnvPrintCommand(String varName) {
        if (IS_WINDOWS) {
            // cmd /c echo %VAR%
            return "cmd /c echo %" + varName + "%";
        } else {
            // sh -c echo $VAR
            return "sh -c \"echo $" + varName + "\"";
        }
    }

    /** Helper to build an array version of the env‑print command. */
    private String[] buildEnvPrintCommandArray(String varName) {
        if (IS_WINDOWS) {
            return new String[]{"cmd", "/c", "echo", "%" + varName + "%"};
        } else {
            return new String[]{"sh", "-c", "echo $" + varName};
        }
    }

    /** Helper to build a command that prints the current working directory. */
    private String buildPwdCommand() {
        if (IS_WINDOWS) {
            return "cmd /c cd";
        } else {
            return "pwd";
        }
    }

    /** Helper to build an array version of the pwd command. */
    private String[] buildPwdCommandArray() {
        if (IS_WINDOWS) {
            return new String[]{"cmd", "/c", "cd"};
        } else {
            return new String[]{"pwd"};
        }
    }

    @Test
    public void testGettersBeforeAnyExec() {
        assertNull("Error output should be null before any exec()", executor.getError());
        assertNull("Standard output should be null before any exec()", executor.getOut());
    }

    @Test
    public void testExecStringSuccess() {
        String expected = "hello‑world";
        int rc = executor.exec(buildEchoCommand(expected));
        assertEquals("Process should exit with code 0", 0, rc);
        assertNotNull("Stdout should not be null", executor.getOut());
        assertTrue("Stdout should contain the echoed text",
                executor.getOut().trim().equals(expected));
        // echo normally writes nothing to stderr
        assertNotNull("Stderr should be non‑null (may be empty string)", executor.getError());
        assertEquals("Stderr should be empty", "", executor.getError().trim());
    }

    @Test
    public void testExecStringArraySuccess() {
        String expected = "array‑test";
        int rc = executor.exec(buildEchoCommandArray(expected));
        assertEquals("Process should exit with code 0", 0, rc);
        assertEquals("Stdout should match expected text",
                expected, executor.getOut().trim());
        assertEquals("Stderr should be empty", "", executor.getError().trim());
    }

    @Test
    public void testExecStringWithEnv() {
        String varName = "EXECUTOR_TEST_VAR";
        String varValue = "env‑value";
        String[] env = new String[]{varName + "=" + varValue};

        int rc = executor.exec(buildEnvPrintCommand(varName), env);
        assertEquals("Process should exit with code 0", 0, rc);
        assertEquals("Stdout should contain the environment variable value",
                varValue, executor.getOut().trim());
        assertEquals("Stderr should be empty", "", executor.getError().trim());
    }

    @Test
    public void testExecStringArrayWithEnv() {
        String varName = "EXECUTOR_ARRAY_ENV";
        String varValue = "array‑env";
        String[] env = new String[]{varName + "=" + varValue};

        int rc = executor.exec(buildEnvPrintCommandArray(varName), env);
        assertEquals("Process should exit with code 0", 0, rc);
        assertEquals("Stdout should contain the environment variable value",
                varValue, executor.getOut().trim());
        assertEquals("Stderr should be empty", "", executor.getError().trim());
    }

    @Test
    public void testExecStringWithWorkingDirectory() {
        // The command prints its current working directory; we compare it with tempDir.
        int rc = executor.exec(buildPwdCommand(), null, tempDir);
        assertEquals("Process should exit with code 0", 0, rc);
        String out = executor.getOut().trim();
        // On Windows the path may contain backslashes; normalise both sides.
        String expected = tempDir.getAbsolutePath().replace('\\', '/');
        String actual = out.replace('\\', '/');
        assertEquals("Working directory reported by command should match supplied dir",
                expected, actual);
        assertEquals("Stderr should be empty", "", executor.getError().trim());
    }

    @Test
    public void testExecStringArrayWithWorkingDirectory() {
        int rc = executor.exec(buildPwdCommandArray(), null, tempDir);
        assertEquals("Process should exit with code 0", 0, rc);
        String out = executor.getOut().trim();
        String expected = tempDir.getAbsolutePath().replace('\\', '/');
        String actual = out.replace('\\', '/');
        assertEquals("Working directory reported by command should match supplied dir",
                expected, actual);
        assertEquals("Stderr should be empty", "", executor.getError().trim());
    }

    @Test
    public void testExecStringArrayWithEnvAndWorkingDirectory() {
        // Combine env and dir: print env var from within the temp directory.
        String varName = "COMBINED_TEST";
        String varValue = "combo‑value";
        String[] env = new String[]{varName + "=" + varValue};

        int rc = executor.exec(buildEnvPrintCommandArray(varName), env, tempDir);
        assertEquals("Process should exit with code 0", 0, rc);
        assertEquals("Stdout should contain the env variable value",
                varValue, executor.getOut().trim());
        assertEquals("Stderr should be empty", "", executor.getError().trim());
    }

    @Test
    public void testExecInvalidCommandReturnsMinusOne() {
        // Use a command that definitely does not exist.
        String bogus = IS_WINDOWS ? "nonexistent_cmd_xyz" : "nonexistent_cmd_xyz";
        int rc = executor.exec(bogus);
        assertEquals("Executor should return -1 when Runtime.exec throws an exception",
                -1, rc);
        // In the error path the executor prints a stack trace to System.err; we only
        // verify that the stored stdout/stderr are null because the command never started.
        assertNull("Stdout should be null after failure to start process", executor.getOut());
        assertNull("Stderr should be null after failure to start process", executor.getError());
    }
}
