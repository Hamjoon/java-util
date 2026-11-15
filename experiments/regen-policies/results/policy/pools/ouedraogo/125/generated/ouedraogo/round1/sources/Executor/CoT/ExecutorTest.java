package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

public class ExecutorTest {

    private Executor executor;

    @Before
    public void setUp() {
        executor = new Executor();
    }

    /* -----------------------------------------------------------------
     * Helper methods to build OS‑specific commands
     * ----------------------------------------------------------------- */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /** command that prints exactly "hello-world" followed by a newline */
    private static String[] helloCommandArray() {
        if (isWindows()) {
            // cmd /c echo hello-world
            return new String[]{"cmd", "/c", "echo", "hello-world"};
        } else {
            // sh -c 'echo hello-world'
            return new String[]{"sh", "-c", "echo hello-world"};
        }
    }

    /** single‑string version of the above command */
    private static String helloCommandString() {
        if (isWindows()) {
            return "cmd /c echo hello-world";
        } else {
            return "sh -c \"echo hello-world\"";
        }
    }

    /** command that prints the value of environment variable FOO */
    private static String[] envPrintCommandArray() {
        if (isWindows()) {
            // cmd /c echo %FOO%
            return new String[]{"cmd", "/c", "echo", "%FOO%"};
        } else {
            // sh -c 'echo $FOO'
            return new String[]{"sh", "-c", "echo $FOO"};
        }
    }

    /** command that lists files in current directory (used for working‑dir test) */
    private static String[] listFilesCommandArray() {
        if (isWindows()) {
            return new String[]{"cmd", "/c", "dir", "/b"};
        } else {
            return new String[]{"sh", "-c", "ls"};
        }
    }

    /* -----------------------------------------------------------------
     * 1. Basic functionality – single string command
     * ----------------------------------------------------------------- */
    @Test
    public void testExecStringSuccess() {
        int rc = executor.exec(helloCommandString());
        assertEquals("Process should exit with code 0", 0, rc);
        assertNotNull("stdout should not be null", executor.getOut());
        assertTrue("stdout must contain hello-world",
                executor.getOut().trim().equals("hello-world"));
        assertNull("stderr should be null or empty", executor.getError());
    }

    /* -----------------------------------------------------------------
     * 2. Basic functionality – array command
     * ----------------------------------------------------------------- */
    @Test
    public void testExecArraySuccess() {
        int rc = executor.exec(helloCommandArray());
        assertEquals(0, rc);
        assertNotNull(executor.getOut());
        assertEquals("hello-world", executor.getOut().trim());
        assertNull(executor.getError());
    }

    /* -----------------------------------------------------------------
     * 3. Environment‑variable handling
     * ----------------------------------------------------------------- */
    @Test
    public void testExecWithEnv() {
        String[] env = new String[]{"FOO=env-value"};
        int rc = executor.exec(envPrintCommandArray(), env);
        assertEquals(0, rc);
        assertNotNull(executor.getOut());
        assertEquals("env-value", executor.getOut().trim());
        assertNull(executor.getError());
    }

    /* -----------------------------------------------------------------
     * 4. Working‑directory handling
     * ----------------------------------------------------------------- */
    @Test
    public void testExecWithWorkingDirectory() throws IOException {
        // create a temporary folder with a single file inside
        File tmpDir = new File(System.getProperty("java.io.tmpdir"),
                "executor_test_" + System.nanoTime());
        assertTrue(tmpDir.mkdir());
        File innerFile = new File(tmpDir, "unique_file.txt");
        assertTrue(innerFile.createNewFile());

        try {
            int rc = executor.exec(listFilesCommandArray(), null, tmpDir);
            assertEquals(0, rc);
            assertNotNull(executor.getOut());
            // output should contain the name of the file we just created
            assertTrue("Output must contain the file we created",
                    executor.getOut().contains("unique_file.txt"));
            assertNull(executor.getError());
        } finally {
            // cleanup
            innerFile.delete();
            tmpDir.delete();
        }
    }

    /* -----------------------------------------------------------------
     * 5. Non‑existent command – should give non‑zero exit and error text
     * ----------------------------------------------------------------- */
    @Test
    public void testExecNonExistingCommand() {
        String bogus = isWindows() ? "nonexistent-cmd-xyz.exe" : "nonexistent-cmd-xyz";
        int rc = executor.exec(bogus);
        assertTrue("Exit code should be non‑zero for a bad command", rc != 0);
        assertNotNull("stderr should contain something", executor.getError());
        // stdout is expected to be empty
        assertTrue("stdout should be empty or null",
                executor.getOut() == null || executor.getOut().trim().isEmpty());
    }

    /* -----------------------------------------------------------------
     * 6. Null command string – should be caught and return -1
     * ----------------------------------------------------------------- */
    @Test
    public void testExecNullString() {
        int rc = executor.exec((String) null);
        assertEquals(-1, rc);
        assertNull("stdout must stay null after failure", executor.getOut());
        assertNull("stderr must stay null after failure", executor.getError());
    }

    /* -----------------------------------------------------------------
     * 7. Empty command string – should be caught and return -1
     * ----------------------------------------------------------------- */
    @Test
    public void testExecEmptyString() {
        int rc = executor.exec("");
        assertEquals(-1, rc);
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    /* -----------------------------------------------------------------
     * 8. Null array command – should be caught and return -1
     * ----------------------------------------------------------------- */
    @Test
    public void testExecNullArray() {
        int rc = executor.exec((String[]) null);
        assertEquals(-1, rc);
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    /* -----------------------------------------------------------------
     * 9. Empty array command – should be caught and return -1
     * ----------------------------------------------------------------- */
    @Test
    public void testExecEmptyArray() {
        int rc = executor.exec(new String[0]);
        assertEquals(-1, rc);
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    /* -----------------------------------------------------------------
     * 10. exec(String, envp) with null env – should behave like normal exec
     * ----------------------------------------------------------------- */
    @Test
    public void testExecStringWithNullEnv() {
        int rc = executor.exec(helloCommandString(), null);
        assertEquals(0, rc);
        assertEquals("hello-world", executor.getOut().trim());
        assertNull(executor.getError());
    }

    /* -----------------------------------------------------------------
     * 11. exec(String[], envp, dir) with null env and null dir – normal exec
     * ----------------------------------------------------------------- */
    @Test
    public void testExecArrayNullEnvNullDir() {
        int rc = executor.exec(helloCommandArray(), null, null);
        assertEquals(0, rc);
        assertEquals("hello-world", executor.getOut().trim());
        assertNull(executor.getError());
    }
}
