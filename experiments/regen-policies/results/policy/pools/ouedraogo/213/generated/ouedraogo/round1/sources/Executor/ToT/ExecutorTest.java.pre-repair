package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link Executor}.
 *
 * <p>This test class covers every public {@code exec(...)} overload, the
 * accessor methods {@code getOut()} and {@code getError()}, and the private
 * helper {@code cmdArrayToString(...)} (via reflection).  The tests are
 * written to run on both Unix‑like systems and Windows without external
 * dependencies.</p>
 *
 * <p>All tests use real processes – no mocking of {@code Runtime},
 * {@code Process} or {@code StreamGobbler} is performed, as required by the
 * exercise.</p>
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

    /*-----------------------------------------------------------------------
     * Helper methods to build OS‑specific commands
     * -----------------------------------------------------------------------*/

    private String echoCommand(String text) {
        return isWindows ? "cmd /c echo " + text : "echo " + text;
    }

    private String[] echoCommandArray(String text) {
        if (isWindows) {
            return new String[]{"cmd", "/c", "echo", text};
        } else {
            return new String[]{"sh", "-c", "echo " + text};
        }
    }

    private String[] envEchoCommandArray(String varName) {
        if (isWindows) {
            // Windows expands %VAR% inside cmd.exe
            return new String[]{"cmd", "/c", "echo", "%" + varName + "%"};
        } else {
            // Unix expands $VAR inside the shell
            return new String[]{"sh", "-c", "echo $" + varName};
        }
    }

    private String[] listFilesCommandArray(File dir) {
        if (isWindows) {
            // "dir /b" prints just the names, one per line
            return new String[]{"cmd", "/c", "dir", "/b"};
        } else {
            return new String[]{"ls"};
        }
    }

    /*-----------------------------------------------------------------------
     * Tests for exec(String)
     * -----------------------------------------------------------------------*/

    @Test
    public void testExecStringSuccess() {
        int rc = executor.exec(echoCommand("hello"));
        assertEquals("Process should exit with code 0", 0, rc);
        assertNotNull("Standard output must be captured", executor.getOut());
        assertTrue("Output should contain the echoed text",
                executor.getOut().trim().equals("hello"));
        assertNotNull("Standard error must be captured (may be empty)", executor.getError());
        assertEquals("No error output expected for a successful command", "", executor.getError().trim());
    }

    @Test
    public void testExecStringFailure() {
        // Use a command that does not exist – this forces Runtime.exec to throw
        int rc = executor.exec("nonexistent_command_12345");
        assertEquals("When exec throws, -1 should be returned", -1, rc);
        assertNull("No stdout should be set after failure", executor.getOut());
        assertNull("No stderr should be set after failure", executor.getError());
    }

    /*-----------------------------------------------------------------------
     * Tests for exec(String[])
     * -----------------------------------------------------------------------*/

    @Test
    public void testExecStringArraySuccess() {
        int rc = executor.exec(echoCommandArray("world"));
        assertEquals(0, rc);
        assertNotNull(executor.getOut());
        assertEquals("world", executor.getOut().trim());
        assertEquals("", executor.getError().trim());
    }

    @Test
    public void testExecStringArrayFailure() {
        int rc = executor.exec(new String[]{"nonexistent_command_67890"});
        assertEquals(-1, rc);
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    /*-----------------------------------------------------------------------
     * Tests for exec(String, String[])
     * -----------------------------------------------------------------------*/

    @Test
    public void testExecStringWithEnv() {
        String varName = "EXEC_TEST_VAR";
        String varValue = "testValue";
        String[] env = {varName + "=" + varValue};

        int rc = executor.exec(echoCommand("%" + varName + "%"), env);
        // On Unix the command above will literally print %EXEC_TEST_VAR%,
        // but we handle both OSes via the helper that builds a proper command.
        // Therefore we rebuild the command using the OS‑specific helper.
        // The test uses the helper to ensure the variable is expanded.
        // Re‑execute with the correct command:
        String command = isWindows ? "cmd /c echo %" + varName + "%" : "sh -c echo $" + varName;
        rc = executor.exec(command, env);
        assertEquals(0, rc);
        assertEquals(varValue, executor.getOut().trim());
        assertEquals("", executor.getError().trim());
    }

    /*-----------------------------------------------------------------------
     * Tests for exec(String[], String[])
     * -----------------------------------------------------------------------*/

    @Test
    public void testExecStringArrayWithEnv() {
        String varName = "EXEC_ARRAY_VAR";
        String varValue = "arrayValue";
        String[] env = {varName + "=" + varValue};

        int rc = executor.exec(envEchoCommandArray(varName), env);
        assertEquals(0, rc);
        assertEquals(varValue, executor.getOut().trim());
        assertEquals("", executor.getError().trim());
    }

    /*-----------------------------------------------------------------------
     * Tests for exec(String, String[], File)
     * -----------------------------------------------------------------------*/

    @Test
    public void testExecStringWithEnvAndDir() throws IOException {
        // Create a temporary directory with a single file
        File tempDir = createTempDirectory("execTestDir");
        File testFile = new File(tempDir, "sample.txt");
        assertTrue("Temp file must be created", testFile.createNewFile());

        String varName = "DIR_VAR";
        String varValue = "dirValue";
        String[] env = {varName + "=" + varValue};

        // Use a command that lists files in the directory
        String command = isWindows ? "cmd /c dir /b" : "ls";
        int rc = executor.exec(command, env, tempDir);
        assertEquals(0, rc);
        assertTrue("Output must contain the created file name",
                executor.getOut().contains("sample.txt"));
        assertEquals("", executor.getError().trim());

        deleteRecursively(tempDir);
    }

    /*-----------------------------------------------------------------------
     * Tests for exec(String[], String[], File)
     * -----------------------------------------------------------------------*/

    @Test
    public void testExecStringArrayWithEnvAndDir() throws IOException {
        File tempDir = createTempDirectory("execArrayTestDir");
        File testFile = new File(tempDir, "arraySample.txt");
        assertTrue(testFile.createNewFile());

        String varName = "ARR_DIR_VAR";
        String varValue = "arrDirValue";
        String[] env = {varName + "=" + varValue};

        int rc = executor.exec(listFilesCommandArray(tempDir), env, tempDir);
        assertEquals(0, rc);
        assertTrue("Output must list the file created in the working directory",
                executor.getOut().contains("arraySample.txt"));
        assertEquals("", executor.getError().trim());

        deleteRecursively(tempDir);
    }

    /*-----------------------------------------------------------------------
     * Test the private helper cmdArrayToString via reflection
     * -----------------------------------------------------------------------*/

    @Test
    public void testCmdArrayToString() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        String[] cmd = {"java", "-version"};
        Method method = Executor.class.getDeclaredMethod("cmdArrayToString", String[].class);
        method.setAccessible(true);
        String result = (String) method.invoke(executor, (Object) cmd);
        // Expected format: each element followed by a space, final space allowed
        assertEquals("java -version ", result);
    }

    /*-----------------------------------------------------------------------
     * Utility methods for temporary directories
     * -----------------------------------------------------------------------*/

    private File createTempDirectory(String prefix) throws IOException {
        File temp = File.createTempFile(prefix, Long.toString(System.nanoTime()));
        if (!temp.delete()) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!temp.mkdir()) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return temp;
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
