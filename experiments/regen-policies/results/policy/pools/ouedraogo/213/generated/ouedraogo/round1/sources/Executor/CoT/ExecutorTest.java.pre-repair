package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Executor}.
 *
 * The tests exercise every public method, verify normal behaviour,
 * and cover edge‑cases such as null arguments, invalid commands and
 * the handling of environment variables / working directory.
 *
 * Note: the tests use only the public API of {@link Executor} and the
 * standard JDK classes that appear in the source file. No mocking
 * frameworks are used, as required by the task description.
 */
public class ExecutorTest {

    private Executor exec;
    private boolean isWindows;

    @Before
    public void setUp() {
        exec = new Executor();
        isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    }

    @After
    public void tearDown() {
        exec = null;
    }

    /* -------------------------------------------------
     *  Helper methods
     * ------------------------------------------------- */

    /**
     * Returns a simple command that prints the supplied text to STDOUT.
     * Works on both Windows and Unix‑like systems.
     */
    private String[] buildEchoCommand(String text) {
        if (isWindows) {
            // cmd /c echo text
            return new String[]{"cmd", "/c", "echo", text};
        } else {
            // sh -c echo text
            return new String[]{"sh", "-c", "echo " + text};
        }
    }

    /**
     * Returns a command that is guaranteed to fail (non‑existent executable).
     */
    private String invalidCommand() {
        return isWindows ? "nonexistentcommand_xyz" : "nonexistentcommand_xyz";
    }

    /* -------------------------------------------------
     *  1. exec(String)
     * ------------------------------------------------- */

    @Test
    public void testExecString_success() {
        int exit = exec.exec("java -version"); // java prints version to stderr but exits 0
        assertEquals("Process should exit with code 0", 0, exit);
        // getError should contain something, getOut may be empty
        assertNotNull("Error output should not be null", exec.getError());
        assertNotNull("Standard output should not be null (may be empty)", exec.getOut());
    }

    @Test
    public void testExecString_invalidCommand_returnsMinusOne() {
        int exit = exec.exec(invalidCommand());
        assertEquals("Invalid command should return -1", -1, exit);
        // Since the process never started, both getters should stay null
        assertNull("Error output should be null after failure to start", exec.getError());
        assertNull("Stdout should be null after failure to start", exec.getOut());
    }

    @Test
    public void testExecString_nullCommand_returnsMinusOne() {
        int exit = exec.exec((String) null);
        assertEquals("Null command should return -1", -1, exit);
        assertNull(exec.getError());
        assertNull(exec.getOut());
    }

    /* -------------------------------------------------
     *  2. exec(String[])
     * ------------------------------------------------- */

    @Test
    public void testExecStringArray_success() {
        String message = "HelloExecutor";
        String[] cmd = buildEchoCommand(message);
        int exit = exec.exec(cmd);
        assertEquals("Process should exit with 0", 0, exit);
        assertNotNull(exec.getOut());
        // The output may contain a newline; trim before assertion
        assertTrue("Stdout should contain the echoed text",
                exec.getOut().trim().endsWith(message));
        assertEquals("Stderr should be empty for echo", "", exec.getError().trim());
    }

    @Test
    public void testExecStringArray_invalidCommand_returnsMinusOne() {
        String[] cmd = new String[]{invalidCommand()};
        int exit = exec.exec(cmd);
        assertEquals("Invalid command array should return -1", -1, exit);
        assertNull(exec.getError());
        assertNull(exec.getOut());
    }

    @Test
    public void testExecStringArray_nullArray_returnsMinusOne() {
        int exit = exec.exec((String[]) null);
        assertEquals("Null command array should return -1", -1, exit);
        assertNull(exec.getError());
        assertNull(exec.getOut());
    }

    /* -------------------------------------------------
     *  3. exec(String, String[])
     * ------------------------------------------------- */

    @Test
    public void testExecStringWithEnv_success() throws IOException {
        // Use a command that prints an environment variable
        String varName = "EXECUTOR_TEST_VAR";
        String varValue = "12345";

        String command;
        if (isWindows) {
            // Windows: cmd /c echo %VAR%
            command = "cmd /c echo %" + varName + "%";
        } else {
            // Unix: sh -c 'echo $VAR'
            command = "sh -c 'echo $" + varName + "'";
        }

        String[] env = new String[]{varName + "=" + varValue};

        int exit = exec.exec(command, env);
        assertEquals("Process should exit with 0", 0, exit);
        assertNotNull(exec.getOut());
        assertTrue("Stdout should contain the environment variable value",
                exec.getOut().trim().equals(varValue));
    }

    @Test
    public void testExecStringWithEnv_nullEnv_usesParentEnv() {
        // Run a simple command that always succeeds; null env should not cause failure
        int exit = exec.exec("java -version", null);
        assertEquals(0, exit);
        assertNotNull(exec.getError());
    }

    @Test
    public void testExecStringWithEnv_invalidCommand_returnsMinusOne() {
        int exit = exec.exec(invalidCommand(), new String[]{"FOO=BAR"});
        assertEquals(-1, exit);
        assertNull(exec.getError());
        assertNull(exec.getOut());
    }

    /* -------------------------------------------------
     *  4. exec(String[], String[])
     * ------------------------------------------------- */

    @Test
    public void testExecStringArrayWithEnv_success() {
        String varName = "EXECUTOR_ARR_VAR";
        String varValue = "arrValue";

        String[] cmd = buildEchoCommand("$" + varName); // the shell will expand it
        String[] env = new String[]{varName + "=" + varValue};

        int exit = exec.exec(cmd, env);
        assertEquals(0, exit);
        assertNotNull(exec.getOut());
        // The output should be the value of the environment variable
        assertTrue(exec.getOut().trim().endsWith(varValue));
    }

    @Test
    public void testExecStringArrayWithEnv_nullEnv() {
        String[] cmd = buildEchoCommand("noEnv");
        int exit = exec.exec(cmd, null);
        assertEquals(0, exit);
        assertNotNull(exec.getOut());
    }

    @Test
    public void testExecStringArrayWithEnv_invalidCommand_returnsMinusOne() {
        String[] cmd = new String[]{invalidCommand()};
        int exit = exec.exec(cmd, new String[]{"A=B"});
        assertEquals(-1, exit);
        assertNull(exec.getError());
        assertNull(exec.getOut());
    }

    /* -------------------------------------------------
     *  5. exec(String, String[], File)
     * ------------------------------------------------- */

    @Test
    public void testExecStringWithEnvAndDir_success() throws IOException {
        // Create a temporary directory and a simple file inside it
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "execTestDir");
        assertTrue("Temp dir should be created", tempDir.mkdir());

        // Command that lists files in the directory; output should contain the file we create
        File sample = new File(tempDir, "sample.txt");
        assertTrue("Sample file should be created", sample.createNewFile());

        String command;
        if (isWindows) {
            command = "cmd /c dir";
        } else {
            command = "ls";
        }

        int exit = exec.exec(command, null, tempDir);
        assertEquals(0, exit);
        assertNotNull(exec.getOut());
        assertTrue("Stdout should list the sample file",
                exec.getOut().contains("sample.txt"));

        // cleanup
        assertTrue(sample.delete());
        assertTrue(tempDir.delete());
    }

    @Test
    public void testExecStringWithEnvAndDir_nullDir_usesCurrent() {
        // Null dir should not cause an exception; command should still run
        int exit = exec.exec("java -version", null, null);
        assertEquals(0, exit);
        assertNotNull(exec.getError());
    }

    @Test
    public void testExecStringWithEnvAndDir_invalidCommand_returnsMinusOne() {
        int exit = exec.exec(invalidCommand(), null, new File("."));
        assertEquals(-1, exit);
        assertNull(exec.getError());
        assertNull(exec.getOut());
    }

    /* -------------------------------------------------
     *  6. exec(String[], String[], File)
     * ------------------------------------------------- */

    @Test
    public void testExecArrayWithEnvAndDir_success() throws IOException {
        // Use the same temporary directory technique as above
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "execTestDir2");
        assertTrue(tempDir.mkdir());

        File sample = new File(tempDir, "sample2.txt");
        assertTrue(sample.createNewFile());

        String[] cmd;
        if (isWindows) {
            cmd = new String[]{"cmd", "/c", "dir"};
        } else {
            cmd = new String[]{"ls"};
        }

        int exit = exec.exec(cmd, null, tempDir);
        assertEquals(0, exit);
        assertNotNull(exec.getOut());
        assertTrue(exec.getOut().contains("sample2.txt"));

        // cleanup
        assertTrue(sample.delete());
        assertTrue(tempDir.delete());
    }

    @Test
    public void testExecArrayWithEnvAndDir_nullDir() {
        String[] cmd = buildEchoCommand("nullDirTest");
        int exit = exec.exec(cmd, null, null);
        assertEquals(0, exit);
        assertNotNull(exec.getOut());
        assertTrue(exec.getOut().trim().endsWith("nullDirTest"));
    }

    @Test
    public void testExecArrayWithEnvAndDir_invalidCommand_returnsMinusOne() {
        String[] cmd = new String[]{invalidCommand()};
        int exit = exec.exec(cmd, null, new File("."));
        assertEquals(-1, exit);
        assertNull(exec.getError());
        assertNull(exec.getOut());
    }

    /* -------------------------------------------------
     *  7. getError() and getOut()
     * ------------------------------------------------- */

    @Test
    public void testGettersBeforeAnyExecReturnNull() {
        assertNull("Error should be null before any exec", exec.getError());
        assertNull("Out should be null before any exec", exec.getOut());
    }

    @Test
    public void testGettersAfterSuccessfulExecContainData() {
        // Use echo to produce known stdout and no stderr
        String[] cmd = buildEchoCommand("getterTest");
        exec.exec(cmd);
        assertNotNull("Error should not be null after exec (may be empty)", exec.getError());
        assertEquals("Error should be empty for echo", "", exec.getError().trim());
        assertNotNull("Out should contain the echoed text", exec.getOut());
        assertTrue(exec.getOut().trim().endsWith("getterTest"));
    }

    @Test
    public void testGettersAfterFailedExecRemainNull() {
        exec.exec(invalidCommand());
        assertNull("Error should stay null after failed start", exec.getError());
        assertNull("Out should stay null after failed start", exec.getOut());
    }
}
