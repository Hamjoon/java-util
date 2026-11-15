package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Executor}.
 *
 * <p>The tests exercise every public {@code exec(...)} overload, verify that
 * {@code getOut()} and {@code getError()} return the data produced by the
 * spawned process, and also cover the private helper {@code cmdArrayToString}
 * via reflection.  All tests use real {@link Process} objects – no mocking or
 * subclassing of library classes is performed, as required.</p>
 */
public class ExecutorTest
{
    private Executor executor;
    /** Path to the java executable used to launch the helper process. */
    private String javaCommand;
    /** Class‑path for the helper process (the same class‑path used to run the tests). */
    private String classPath;
    /** Fully qualified name of the helper class. */
    private String helperClassName;

    @Before
    public void setUp() throws Exception
    {
        executor = new Executor();

        // Build a platform‑independent command that runs a tiny helper class.
        // The helper prints a known string to stdout and stderr and exits with 0.
        String javaHome = System.getProperty("java.home");
        String exe = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        javaCommand = javaHome + File.separator + "bin" + File.separator + exe;

        // sanity check – if the executable is not found the tests will fail early.
        assertTrue("Java executable not found: " + javaCommand, new File(javaCommand).exists());

        classPath = System.getProperty("java.class.path");
        helperClassName = Helper.class.getName();
    }

    /**
     * Helper process executed by the tests.  It writes deterministic data to
     * both stdout and stderr and terminates with exit code 0.
     */
    public static class Helper
    {
        public static void main(String[] args)
        {
            System.out.print("STDOUT");
            System.err.print("STDERR");
            System.exit(0);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Successful executions – all overloads
     * --------------------------------------------------------------------- */

    @Test
    public void testExecStringSuccess() throws Exception
    {
        String command = javaCommand + " -cp " + classPath + " " + helperClassName;
        int exit = executor.exec(command);
        assertEquals(0, exit);
        assertEquals("STDOUT", executor.getOut());
        assertEquals("STDERR", executor.getError());
    }

    @Test
    public void testExecStringArraySuccess() throws Exception
    {
        String[] cmd = new String[]{javaCommand, "-cp", classPath, helperClassName};
        int exit = executor.exec(cmd);
        assertEquals(0, exit);
        assertEquals("STDOUT", executor.getOut());
        assertEquals("STDERR", executor.getError());
    }

    @Test
    public void testExecStringWithEnvSuccess() throws Exception
    {
        String command = javaCommand + " -cp " + classPath + " " + helperClassName;
        int exit = executor.exec(command, null);
        assertEquals(0, exit);
        assertEquals("STDOUT", executor.getOut());
        assertEquals("STDERR", executor.getError());
    }

    @Test
    public void testExecStringArrayWithEnvSuccess() throws Exception
    {
        String[] cmd = new String[]{javaCommand, "-cp", classPath, helperClassName};
        int exit = executor.exec(cmd, null);
        assertEquals(0, exit);
        assertEquals("STDOUT", executor.getOut());
        assertEquals("STDERR", executor.getError());
    }

    @Test
    public void testExecStringWithEnvAndDirSuccess() throws Exception
    {
        String command = javaCommand + " -cp " + classPath + " " + helperClassName;
        File workDir = new File("."); // current directory – guaranteed to exist
        int exit = executor.exec(command, null, workDir);
        assertEquals(0, exit);
        assertEquals("STDOUT", executor.getOut());
        assertEquals("STDERR", executor.getError());
    }

    @Test
    public void testExecStringArrayWithEnvAndDirSuccess() throws Exception
    {
        String[] cmd = new String[]{javaCommand, "-cp", classPath, helperClassName};
        File workDir = new File("."); // current directory – guaranteed to exist
        int exit = executor.exec(cmd, null, workDir);
        assertEquals(0, exit);
        assertEquals("STDOUT", executor.getOut());
        assertEquals("STDERR", executor.getError());
    }

    /* --------------------------------------------------------------------- *
     *  Failure handling – invalid arguments cause Runtime.exec to throw
     * --------------------------------------------------------------------- */

    @Test
    public void testExecStringWithNullCommandReturnsMinusOne()
    {
        int exit = executor.exec((String) null);
        assertEquals(-1, exit);
        // No process was started – both buffers should stay null.
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    @Test
    public void testExecStringArrayWithNullCommandReturnsMinusOne()
    {
        int exit = executor.exec((String[]) null);
        assertEquals(-1, exit);
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    /* --------------------------------------------------------------------- *
     *  Private helper method – cmdArrayToString
     * --------------------------------------------------------------------- */

    @Test
    public void testCmdArrayToString() throws Exception
    {
        // Use reflection to access the private method.
        Method m = Executor.class.getDeclaredMethod("cmdArrayToString", String[].class);
        m.setAccessible(true);

        String[] input = new String[]{"one", "two", "three"};
        String result = (String) m.invoke(executor, (Object) input);
        // Expected format: each element followed by a space, including a trailing space.
        assertEquals("one two three ", result);
    }
}
