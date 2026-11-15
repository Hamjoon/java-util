package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Executor}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>All overloaded {@code exec(...)} methods – success, failure and edge cases.</li>
 *   <li>Correct capture of {@code stdout} and {@code stderr} via {@code getOut()} and {@code getError()}.</li>
 *   <li>Handling of {@code null} arguments.</li>
 *   <li>Use of environment variables and working directories.</li>
 *   <li>Platform‑independent command construction (Windows vs. Unix‑like).</li>
 * </ul>
 * </p>
 *
 * <p>
 * No mocking frameworks are used – real processes are spawned, which is acceptable for a utility
 * that is intended to wrap {@link Runtime#exec(String)}.
 * </p>
 */
public class ExecutorTest
{
    /** Temporary folder used for tests that need a real working directory. */
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    /** Helper to decide which OS we are running on. */
    private static boolean isWindows()
    {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /** Builds a cross‑platform “echo” command that writes the supplied text to *stdout*. */
    private static String echoCommand(String text)
    {
        if (isWindows())
        {
            // Windows cmd:  cmd /c echo <text>
            return "cmd /c echo " + text;
        }
        else
        {
            // POSIX sh:      sh -c 'echo <text>'
            return "sh -c echo " + text;
        }
    }

    /** Builds a cross‑platform command that writes the supplied text to *stderr*. */
    private static String errorCommand(String text)
    {
        if (isWindows())
        {
            // Windows cmd:  cmd /c echo <text> 1>&2
            return "cmd /c echo " + text + " 1>&2";
        }
        else
        {
            // POSIX sh:      sh -c 'echo <text> 1>&2'
            return "sh -c \"echo " + text + " 1>&2\"";
        }
    }

    /** Builds a cross‑platform command array equivalent to {@link #echoCommand(String)}. */
    private static String[] echoCommandArray(String text)
    {
        if (isWindows())
        {
            return new String[]{"cmd", "/c", "echo", text};
        }
        else
        {
            return new String[]{"sh", "-c", "echo " + text};
        }
    }

    /** Builds a cross‑platform command array that writes to *stderr*. */
    private static String[] errorCommandArray(String text)
    {
        if (isWindows())
        {
            return new String[]{"cmd", "/c", "echo", text, "1>&2"};
        }
        else
        {
            return new String[]{"sh", "-c", "echo " + text + " 1>&2"};
        }
    }

    /**
     * ----------------------------------------------------------------------
     * 1️⃣  exec(String) – normal success case
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecStringSuccess()
    {
        Executor executor = new Executor();
        int exit = executor.exec(echoCommand("hello"));
        assertEquals("Process should exit with code 0", 0, exit);
        assertEquals("stdout must contain the echoed text",
                "hello" + System.lineSeparator(),
                executor.getOut());
        assertTrue("stderr must be empty", executor.getError() == null || executor.getError().isEmpty());
    }

    /**
     * ----------------------------------------------------------------------
     * 2️⃣  exec(String) – command does not exist → should return -1
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecStringCommandNotFound()
    {
        Executor executor = new Executor();
        int exit = executor.exec("nonexistent_command_12345");
        assertEquals("When exec throws, -1 must be returned", -1, exit);
        assertNull("No stdout should be captured", executor.getOut());
        assertNull("No stderr should be captured", executor.getError());
    }

    /**
     * ----------------------------------------------------------------------
     * 3️⃣  exec(String) – null argument → should return -1
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecStringNull()
    {
        Executor executor = new Executor();
        int exit = executor.exec((String) null);
        assertEquals("Null command must be handled and return -1", -1, exit);
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    /**
     * ----------------------------------------------------------------------
     * 4️⃣  exec(String[]) – success case
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecArraySuccess()
    {
        Executor executor = new Executor();
        String[] cmd = echoCommandArray("array");
        int exit = executor.exec(cmd);
        assertEquals(0, exit);
        assertEquals("array" + System.lineSeparator(), executor.getOut());
        assertTrue(executor.getError() == null || executor.getError().isEmpty());
    }

    /**
     * ----------------------------------------------------------------------
     * 5️⃣  exec(String[]) – null array → should return -1
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecArrayNull()
    {
        Executor executor = new Executor();
        int exit = executor.exec((String[]) null);
        assertEquals(-1, exit);
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    /**
     * ----------------------------------------------------------------------
     * 6️⃣  exec(String, String[]) – environment variable expansion
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecWithEnv()
    {
        Executor executor = new Executor();
        String varName = "EXEC_TEST_VAR";
        String varValue = "12345";

        String[] env = { varName + "=" + varValue };
        String command = isWindows()
                ? "cmd /c echo %" + varName + "%"
                : "sh -c echo $" + varName;

        int exit = executor.exec(command, env);
        assertEquals(0, exit);
        assertEquals(varValue + System.lineSeparator(), executor.getOut());
        assertTrue(executor.getError() == null || executor.getError().isEmpty());
    }

    /**
     * ----------------------------------------------------------------------
     * 7️⃣  exec(String[], String[]) – env + array version
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecArrayWithEnv()
    {
        Executor executor = new Executor();
        String varName = "EXEC_ARRAY_VAR";
        String varValue = "envValue";

        String[] env = { varName + "=" + varValue };
        String[] cmd = isWindows()
                ? new String[]{"cmd", "/c", "echo", "%" + varName + "%"}
                : new String[]{"sh", "-c", "echo $" + varName};

        int exit = executor.exec(cmd, env);
        assertEquals(0, exit);
        assertEquals(varValue + System.lineSeparator(), executor.getOut());
        assertTrue(executor.getError() == null || executor.getError().isEmpty());
    }

    /**
     * ----------------------------------------------------------------------
     * 8️⃣  exec(String, String[], File) – working directory handling
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecWithWorkingDirectory() throws IOException
    {
        Executor executor = new Executor();

        // Create a temporary sub‑directory with a single file inside it.
        File workDir = temporaryFolder.newFolder("workDir");
        File innerFile = new File(workDir, "inner.txt");
        assertTrue("File must be created for the test", innerFile.createNewFile());

        // Platform‑specific directory‑listing command.
        String command = isWindows() ? "cmd /c dir" : "ls";

        int exit = executor.exec(command, null, workDir);
        assertEquals(0, exit);
        assertTrue("Standard out must contain the file created in the working dir",
                executor.getOut().contains("inner.txt"));
        assertTrue(executor.getError() == null || executor.getError().isEmpty());
    }

    /**
     * ----------------------------------------------------------------------
     * 9️⃣  exec(String[], String[], File) – array + env + dir
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecArrayEnvDir() throws IOException
    {
        Executor executor = new Executor();

        // Build a temporary working directory containing a file.
        File workDir = temporaryFolder.newFolder("workDir2");
        File inner = new File(workDir, "file.txt");
        assertTrue(inner.createNewFile());

        // Command array that lists the directory.
        String[] cmd = isWindows()
                ? new String[]{"cmd", "/c", "dir"}
                : new String[]{"ls"};

        String varName = "DUMMY";
        String varValue = "value";
        String[] env = { varName + "=" + varValue };

        int exit = executor.exec(cmd, env, workDir);
        assertEquals(0, exit);
        assertTrue("Output must contain the created file name", executor.getOut().contains("file.txt"));
        assertTrue(executor.getError() == null || executor.getError().isEmpty());
    }

    /**
     * ----------------------------------------------------------------------
     * 🔟  exec(String) – command that writes to *stderr*
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecStringStderr()
    {
        Executor executor = new Executor();
        int exit = executor.exec(errorCommand("oops"));
        // Most shells exit with 0 for a simple echo to stderr.
        assertEquals(0, exit);
        assertEquals("oops" + System.lineSeparator(), executor.getError());
        assertTrue(executor.getOut() == null || executor.getOut().isEmpty());
    }

    /**
     * ----------------------------------------------------------------------
     * 1️⃣1️⃣ exec(String[]) – command that writes to *stderr*
     * ----------------------------------------------------------------------
     */
    @Test
    public void testExecArrayStderr()
    {
        Executor executor = new Executor();
        String[] cmd = errorCommandArray("arrayError");
        int exit = executor.exec(cmd);
        assertEquals(0, exit);
        assertEquals("arrayError" + System.lineSeparator(), executor.getError());
        assertTrue(executor.getOut() == null || executor.getOut().isEmpty());
    }
}
