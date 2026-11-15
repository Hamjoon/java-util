package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;

/**
 * Comprehensive JUnit‑4 test suite for {@link Executor}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All overloads of {@code exec(...)} – success, failure and edge cases.</li>
 *   <li>Verification that {@code getOut()} and {@code getError()} return the expected streams.</li>
 *   <li>Behaviour when the command cannot be started (return value {@code -1}).</li>
 *   <li>Execution with custom environment variables.</li>
 *   <li>Execution with a working directory.</li>
 *   <li>Correctness of the private {@code cmdArrayToString(...)} helper (via reflection).</li>
 * </ul>
 *
 * <p>Only the public API of {@link Executor} and the classes that appear in its source
 * (or the standard JDK classes used therein) are employed – no mocks or external
 * test libraries are required.</p>
 */
public class ExecutorTest
{
    private Executor executor;
    private boolean isWindows;

    @Before
    public void setUp()
    {
        executor = new Executor();
        isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    }

    @After
    public void tearDown()
    {
        executor = null;
    }

    /**
     * Helper to build a command that prints a given string to STDOUT.
     * Works on both Windows and Unix‑like platforms.
     */
    private String[] buildEchoCommand(String message)
    {
        if (isWindows)
        {
            // cmd /c echo message
            return new String[] { "cmd", "/c", "echo", message };
        }
        else
        {
            // /bin/sh -c echo message
            return new String[] { "/bin/sh", "-c", "echo " + message };
        }
    }

    /**
     * Helper to build a command that prints a given environment variable.
     */
    private String[] buildEnvEchoCommand(String varName)
    {
        if (isWindows)
        {
            // cmd /c echo %VAR%
            return new String[] { "cmd", "/c", "echo", "%" + varName + "%" };
        }
        else
        {
            // /bin/sh -c echo $VAR
            return new String[] { "/bin/sh", "-c", "echo $" + varName };
        }
    }

    /**
     * Test the simple {@code exec(String)} overload with a command that succeeds.
     */
    @Test
    public void testExecStringSuccess()
    {
        String cmd = isWindows ? "cmd /c echo HelloWorld" : "echo HelloWorld";
        int rc = executor.exec(cmd);
        assertEquals("Process should exit with code 0", 0, rc);
        assertNotNull("StdOut should be captured", executor.getOut());
        assertTrue("StdOut must contain the echoed text",
                executor.getOut().trim().equals("HelloWorld"));
        // The command used does not write to STDERR
        assertTrue("StdErr should be empty", executor.getError() == null || executor.getError().trim().isEmpty());
    }

    /**
     * Test the {@code exec(String)} overload when the command cannot be started.
     * An invalid command should cause the method to return {@code -1}.
     */
    @Test
    public void testExecStringInvalid()
    {
        String invalidCmd = "nonexistent_command_12345";
        int rc = executor.exec(invalidCmd);
        assertEquals("Invalid command should yield -1", -1, rc);
        // When exec fails the streams are never populated
        assertNull("StdOut must be null after failure", executor.getOut());
        assertNull("StdErr must be null after failure", executor.getError());
    }

    /**
     * Test {@code exec(String[])} with a successful command.
     */
    @Test
    public void testExecArraySuccess()
    {
        String[] cmd = buildEchoCommand("ArraySuccess");
        int rc = executor.exec(cmd);
        assertEquals("Process should exit with code 0", 0, rc);
        assertNotNull("StdOut should be captured", executor.getOut());
        assertEquals("StdOut must contain the echoed text",
                "ArraySuccess", executor.getOut().trim());
        assertTrue("StdErr must be empty", executor.getError() == null || executor.getError().trim().isEmpty());
    }

    /**
     * Test {@code exec(String[])} with an invalid command array.
     */
    @Test
    public void testExecArrayInvalid()
    {
        String[] cmd = new String[] { "nonexistent_command_12345" };
        int rc = executor.exec(cmd);
        assertEquals("Invalid command array should yield -1", -1, rc);
        assertNull("StdOut must be null after failure", executor.getOut());
        assertNull("StdErr must be null after failure", executor.getError());
    }

    /**
     * Verify that an environment variable can be passed to the process and read back.
     */
    @Test
    public void testExecWithEnvironment()
    {
        final String VAR_NAME = "EXEC_TEST_VAR";
        final String VAR_VALUE = "EXEC_VALUE_987";

        String[] env = new String[] { VAR_NAME + "=" + VAR_VALUE };
        String[] cmd = buildEnvEchoCommand(VAR_NAME);

        int rc = executor.exec(cmd, env);
        assertEquals("Process should exit with code 0", 0, rc);
        assertNotNull("StdOut must be captured", executor.getOut());
        // The output may contain a trailing newline – trim before comparison
        assertEquals("StdOut must contain the environment variable value",
                VAR_VALUE, executor.getOut().trim());
        // No error expected
        assertTrue("StdErr must be empty", executor.getError() == null || executor.getError().trim().isEmpty());
    }

    /**
     * Execute a small script residing in a temporary directory.
     * The script simply prints a known token; the test verifies that the
     * {@code dir} parameter is respected.
     */
    @Test
    public void testExecWithWorkingDirectory() throws IOException
    {
        // Create temporary directory
        File tempDir = createTempDirectory("execTestDir");
        try
        {
            // Create script file appropriate for the OS
            File scriptFile;
            String[] cmd;
            if (isWindows)
            {
                scriptFile = new File(tempDir, "testExec.bat");
                try (FileWriter fw = new FileWriter(scriptFile))
                {
                    fw.write("@echo EXEC_DIR_OK\r\n");
                }
                cmd = new String[] { "cmd", "/c", "testExec.bat" };
            }
            else
            {
                scriptFile = new File(tempDir, "testExec.sh");
                try (FileWriter fw = new FileWriter(scriptFile))
                {
                    fw.write("#!/bin/sh\n");
                    fw.write("echo EXEC_DIR_OK\n");
                }
                // Make it executable
                scriptFile.setExecutable(true);
                cmd = new String[] { "/bin/sh", scriptFile.getAbsolutePath() };
            }

            // Execute the script with the directory set to the temp directory
            int rc = executor.exec(cmd, null, tempDir);
            assertEquals("Process should exit with code 0", 0, rc);
            assertNotNull("StdOut must be captured", executor.getOut());
            assertEquals("StdOut must contain the script output",
                    "EXEC_DIR_OK", executor.getOut().trim());
            assertTrue("StdErr must be empty", executor.getError() == null || executor.getError().trim().isEmpty());
        }
        finally
        {
            // Cleanup
            deleteRecursively(tempDir);
        }
    }

    /**
     * Verify the private helper {@code cmdArrayToString(...)} via reflection.
     */
    @Test
    public void testCmdArrayToStringReflection() throws Exception
    {
        Method m = Executor.class.getDeclaredMethod("cmdArrayToString", String[].class);
        m.setAccessible(true);
        String[] array = new String[] { "one", "two", "three" };
        String result = (String) m.invoke(executor, (Object) array);
        assertEquals("Result should be commands separated by a single space and a trailing space",
                "one two three ", result);
    }

    /**
     * Utility: create a temporary directory that will be deleted after the test.
     */
    private File createTempDirectory(String prefix) throws IOException
    {
        File temp = File.createTempFile(prefix, Long.toString(System.nanoTime()));
        if (!temp.delete())
        {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!temp.mkdir())
        {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp;
    }

    /**
     * Utility: delete a file or directory recursively.
     */
    private void deleteRecursively(File file)
    {
        if (file.isDirectory())
        {
            for (File child : file.listFiles())
            {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
