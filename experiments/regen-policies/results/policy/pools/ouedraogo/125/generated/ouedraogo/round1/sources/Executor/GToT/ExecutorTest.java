package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Comprehensive JUnit‑4 test suite for {@link Executor}.
 *
 * The tests are written to run on any POSIX‑compatible system (Linux/macOS).
 * For Windows the shell commands are automatically wrapped with {@code cmd /c}.
 *
 * NOTE: The tests execute real OS processes; they are deliberately lightweight
 * (echo, env, ls, etc.) and should complete in a fraction of a second.
 */
public class ExecutorTest {

    private Executor exec;
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    /** Helper – builds a command string that works on both platforms. */
    private String wrap(String command) {
        return IS_WINDOWS ? "cmd /c " + command : command;
    }

    /** Helper – builds a command array that works on both platforms. */
    private String[] wrapArray(String... parts) {
        if (IS_WINDOWS) {
            // Windows needs "cmd", "/c", then the whole command as a single string
            String joined = String.join(" ", parts);
            return new String[]{"cmd", "/c", joined};
        }
        return parts;
    }

    @Before
    public void setUp() {
        exec = new Executor();
    }

    @After
    public void tearDown() {
        exec = null;
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Basic exec(String) – echo */
    @Test
    public void testExecSingleStringEcho() {
        int rc = exec.exec(wrap("echo hello"));
        assertEquals("Process should exit with 0", 0, rc);
        assertEquals("stdout should contain hello", "hello" + System.lineSeparator(), exec.getOut());
        assertTrue("stderr should be empty", exec.getError() == null || exec.getError().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 2. exec(String[]) – echo via array */
    @Test
    public void testExecArrayEcho() {
        String[] cmd = wrapArray("sh", "-c", "echo array");
        int rc = exec.exec(cmd);
        assertEquals(0, rc);
        assertEquals("array" + System.lineSeparator(), exec.getOut());
        assertTrue(exec.getError() == null || exec.getError().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 3. exec(String, env) – custom environment */
    @Test
    public void testExecWithEnvVariable() {
        String varName = "EXEC_TEST_VAR";
        String varValue = "12345";
        String[] env = { varName + "=" + varValue };
        int rc = exec.exec(wrap("env"), env);
        assertEquals(0, rc);
        assertTrue("stdout must contain the custom variable",
                exec.getOut().contains(varName + "=" + varValue));
    }

    /* ---------------------------------------------------------------------- */
    /* 4. exec(String[], env) – custom environment with array */
    @Test
    public void testExecArrayWithEnvVariable() {
        String varName = "EXEC_ARRAY_VAR";
        String varValue = "abcde";
        String[] env = { varName + "=" + varValue };
        String[] cmd = wrapArray("sh", "-c", "env");
        int rc = exec.exec(cmd, env);
        assertEquals(0, rc);
        assertTrue(exec.getOut().contains(varName + "=" + varValue));
    }

    /* ---------------------------------------------------------------------- */
    /* 5. exec(String, env, dir) – working directory */
    @Test
    public void testExecWithWorkingDirectory() throws IOException {
        // create temp dir and a file inside it
        File tmpDir = new File(System.getProperty("java.io.tmpdir"),
                "ExecutorTestDir_" + UUID.randomUUID());
        assertTrue("temp dir created", tmpDir.mkdir());
        File innerFile = new File(tmpDir, "inner.txt");
        assertTrue("inner file created", innerFile.createNewFile());

        try {
            int rc = exec.exec(wrap("ls"), null, tmpDir);
            assertEquals(0, rc);
            assertTrue("stdout must contain the file name", exec.getOut().contains("inner.txt"));
        } finally {
            innerFile.delete();
            tmpDir.delete();
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 6. exec(String[], env, dir) – array version with working directory */
    @Test
    public void testExecArrayWithWorkingDirectory() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"),
                "ExecutorTestDirArr_" + UUID.randomUUID());
        assertTrue(tmpDir.mkdir());
        File innerFile = new File(tmpDir, "inner2.txt");
        assertTrue(innerFile.createNewFile());

        try {
            String[] cmd = wrapArray("sh", "-c", "ls");
            int rc = exec.exec(cmd, null, tmpDir);
            assertEquals(0, rc);
            assertTrue(exec.getOut().contains("inner2.txt"));
        } finally {
            innerFile.delete();
            tmpDir.delete();
        }
    }

    /* ---------------------------------------------------------------------- */
    /* 7. exec(String) – command that writes to stderr */
    @Test
    public void testExecWritesToStderr() {
        int rc = exec.exec(wrap("sh -c \"echo err >&2\""));
        assertEquals(0, rc);
        assertTrue("stderr must contain the text",
                exec.getError() != null && exec.getError().contains("err"));
        assertTrue("stdout should be empty", exec.getOut() == null || exec.getOut().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 8. exec(String) – non‑zero exit code */
    @Test
    public void testExecNonZeroExitCode() {
        int rc = exec.exec(wrap("sh -c \"exit 42\""));
        assertEquals("Process must return the explicit exit code", 42, rc);
        assertTrue(exec.getOut() == null || exec.getOut().isEmpty());
        assertTrue(exec.getError() == null || exec.getError().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* Edge case – null command string */
    @Test
    public void testExecNullCommand() {
        int rc = exec.exec((String) null);
        assertEquals("Null command should yield -1", -1, rc);
        assertNull(exec.getOut());
        assertNull(exec.getError());
    }

    /* Edge case – empty command array */
    @Test
    public void testExecEmptyArray() {
        int rc = exec.exec(new String[0]);
        assertEquals(-1, rc);
        assertNull(exec.getOut());
        assertNull(exec.getError());
    }

    /* Edge case – non‑existent executable */
    @Test
    public void testExecNonexistentCommand() {
        int rc = exec.exec("nonexistent12345");
        assertEquals(-1, rc);
        assertNull(exec.getOut());
        assertNull(exec.getError());
    }

    /* Edge case – empty environment array (no variables) */
    @Test
    public void testExecEmptyEnvArray() {
        // The parent environment contains USER (or USERNAME). With an empty env it should disappear.
        String[] emptyEnv = new String[0];
        int rc = exec.exec(wrap("env"), emptyEnv);
        assertEquals(0, rc);
        // At least one line must be present, but we cannot guarantee which.
        // Ensure that a known variable from the parent is NOT present.
        String knownVar = IS_WINDOWS ? "USERNAME" : "USER";
        assertFalse(exec.getOut().contains(knownVar + "="));
    }

    /* Edge case – null working directory (should inherit) */
    @Test
    public void testExecNullWorkingDirectory() {
        int rc = exec.exec(wrap("pwd"), null, null);
        assertEquals(0, rc);
        // The output should be the current working directory; we just check that something was returned.
        assertNotNull(exec.getOut());
        assertFalse(exec.getOut().trim().isEmpty());
    }

    /* Edge case – non‑existent working directory */
    @Test
    public void testExecNonexistentWorkingDirectory() {
        File bogus = new File("/this/does/not/exist");
        int rc = exec.exec(wrap("ls"), null, bogus);
        assertEquals(-1, rc);
        assertNull(exec.getOut());
        assertNull(exec.getError());
    }

    /* Edge case – successive executions overwrite previous output */
    @Test
    public void testSuccessiveExecutionsOverwriteState() {
        exec.exec(wrap("echo first"));
        assertEquals("first" + System.lineSeparator(), exec.getOut());

        exec.exec(wrap("echo second"));
        assertEquals("second" + System.lineSeparator(), exec.getOut());

        // error should still be empty after both runs
        assertTrue(exec.getError() == null || exec.getError().isEmpty());
    }
}
