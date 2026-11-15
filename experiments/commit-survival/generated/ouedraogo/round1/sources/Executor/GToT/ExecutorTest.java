package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Executor}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public {@code exec(...)} overloads – normal execution, invalid command,
 *       empty command and non‑existent working directory.</li>
 *   <li>Correct capture of STDOUT via {@link Executor#getOut()}.</li>
 *   <li>Correct capture of STDERR via {@link Executor#getError()}.</li>
 *   <li>Edge‑case handling such as {@code null} environment arrays.</li>
 * </ul>
 *
 * The suite deliberately avoids any mocking; it runs real OS commands.
 * To stay platform‑independent the command strings are built according to the
 * operating system detected at runtime.
 */
public class ExecutorTest
{
    private Executor executor;
    private boolean isWindows;

    @Before
    public void setUp()
    {
        executor = new Executor();
        isWindows = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win");
    }

    /* -------------------------------------------------------------
     * Helper utilities – build OS‑specific commands
     * ------------------------------------------------------------- */
    private String[] echoCommand(String text)
    {
        if (isWindows)
        {
            // Windows: cmd /c echo <text>
            return new String[]{"cmd", "/c", "echo", text};
        }
        else
        {
            // Unix‑like: echo <text>
            return new String[]{"echo", text};
        }
    }

    private String echoCommandString(String text)
    {
        if (isWindows)
        {
            return "cmd /c echo " + text;
        }
        else
        {
            return "echo " + text;
        }
    }

    private String javaVersionCommand()
    {
        // java -version prints to STDERR on most JDKs – useful for getError()
        return "java -version";
    }

    private File existingTempDir()
    {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    private File nonExistingDir()
    {
        // A directory that (almost) certainly does not exist.
        return new File("this-directory-should-not-exist-12345");
    }

    /* -------------------------------------------------------------
     * 1. exec(String) – successful execution
     * ------------------------------------------------------------- */
    @Test
    public void testExecStringSuccess()
    {
        String cmd = echoCommandString("hello");
        int exit = executor.exec(cmd);
        assertEquals("Process should exit with code 0", 0, exit);
        assertEquals("StdOut should contain the echoed text", "hello", executor.getOut().trim());
        assertTrue("StdErr should be empty for a normal echo", executor.getError() == null || executor.getError().isEmpty());
    }

    /* -------------------------------------------------------------
     * 2. exec(String) – invalid command (should return -1)
     * ------------------------------------------------------------- */
    @Test
    public void testExecStringInvalidCommand()
    {
        int exit = executor.exec("nonexistent_command_9876");
        assertEquals("Invalid command must return -1", -1, exit);
        // When exec fails, both streams remain null
        assertNull("Error stream should be null on failure", executor.getError());
        assertNull("Out stream should be null on failure", executor.getOut());
    }

    /* -------------------------------------------------------------
     * 3. exec(String[]) – successful execution
     * ------------------------------------------------------------- */
    @Test
    public void testExecStringArraySuccess()
    {
        String[] cmd = echoCommand("world");
        int exit = executor.exec(cmd);
        assertEquals(0, exit);
        assertEquals("world", executor.getOut().trim());
        assertTrue(executor.getError() == null || executor.getError().isEmpty());
    }

    /* -------------------------------------------------------------
     * 4. exec(String[], String[]) – success with custom environment
     * ------------------------------------------------------------- */
    @Test
    public void testExecStringArrayWithEnv()
    {
        String[] cmd = echoCommand("envtest");
        // Define a simple environment variable; the echo command does not use it,
        // but the call must succeed without throwing.
        String[] env = new String[]{"MY_VAR=12345"};
        int exit = executor.exec(cmd, env);
        assertEquals(0, exit);
        assertEquals("envtest", executor.getOut().trim());
    }

    /* -------------------------------------------------------------
     * 5. exec(String, String[], File) – success with working directory
     * ------------------------------------------------------------- */
    @Test
    public void testExecStringWithEnvAndDir()
    {
        String cmd = echoCommandString("dirtest");
        String[] env = null; // null is allowed – means inherit parent env
        File dir = existingTempDir();

        int exit = executor.exec(cmd, env, dir);
        assertEquals(0, exit);
        assertEquals("dirtest", executor.getOut().trim());
    }

    /* -------------------------------------------------------------
     * 6. exec(String[], String[], File) – success with working directory
     * ------------------------------------------------------------- */
    @Test
    public void testExecStringArrayWithEnvAndDir()
    {
        String[] cmd = echoCommand("arraydir");
        String[] env = null;
        File dir = existingTempDir();

        int exit = executor.exec(cmd, env, dir);
        assertEquals(0, exit);
        assertEquals("arraydir", executor.getOut().trim());
    }

    /* -------------------------------------------------------------
     * 7. exec(String) – empty command (should return -1)
     * ------------------------------------------------------------- */
    @Test
    public void testExecStringEmpty()
    {
        int exit = executor.exec("");
        assertEquals(-1, exit);
        assertNull(executor.getError());
        assertNull(executor.getOut());
    }

    /* -------------------------------------------------------------
     * 8. exec(String, String[], File) – non‑existent working directory
     * ------------------------------------------------------------- */
    @Test
    public void testExecWithNonExistingDir()
    {
        String cmd = echoCommandString("badDir");
        File dir = nonExistingDir();
        // The directory does not exist, Runtime.exec will throw IOException,
        // causing Executor to return -1.
        int exit = executor.exec(cmd, null, dir);
        assertEquals(-1, exit);
        assertNull(executor.getError());
        assertNull(executor.getOut());
    }

    /* -------------------------------------------------------------
     * 9. getError() – capture of STDERR (java -version prints to STDERR)
     * ------------------------------------------------------------- */
    @Test
    public void testGetErrorFromStderr()
    {
        // java -version is expected to exit with 0 and write version info to STDERR
        int exit = executor.exec(javaVersionCommand());
        assertEquals(0, exit);
        String err = executor.getError();
        assertNotNull("STDERR should not be null after java -version", err);
        assertTrue("STDERR should contain the word 'java' (case‑insensitive)",
                err.toLowerCase(Locale.ENGLISH).contains("java"));
        // STDOUT from this command is typically empty
        assertTrue(executor.getOut() == null || executor.getOut().isEmpty());
    }

    /* -------------------------------------------------------------
     * 10. getOut() – capture of STDOUT when command writes nothing
     * ------------------------------------------------------------- */
    @Test
    public void testGetOutWhenNoStdout()
    {
        // The command `java -version` writes to STDERR only.
        executor.exec(javaVersionCommand());
        assertTrue("STDOUT should be empty for java -version", executor.getOut() == null || executor.getOut().isEmpty());
    }
}
