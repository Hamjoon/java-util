package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link Executor}.
 *
 * <p>The tests exercise every public {@code exec(...)} overload, the
 * {@code getOut()}, {@code getError()} accessors and the private helper
 * {@code cmdArrayToString(...)} (via reflection). Real OS processes are
 * launched – no mocking is used, as required by the task description.</p>
 *
 * <p>Because the test suite must run on both Windows and Unix‑like systems,
 * the command strings are chosen dynamically based on {@code os.name}.</p>
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

    /* --------------------------------------------------------------------- */
    /* Helper methods to build platform‑specific commands                    */
    /* --------------------------------------------------------------------- */

    private String[] echoCommandArray(String message) {
        if (isWindows) {
            // cmd /c echo message
            return new String[]{"cmd", "/c", "echo", message};
        } else {
            // /bin/sh -c echo message
            return new String[]{"/bin/sh", "-c", "echo " + message};
        }
    }

    private String echoCommand(String message) {
        if (isWindows) {
            return "cmd /c echo " + message;
        } else {
            return "/bin/sh -c \"echo " + message + "\"";
        }
    }

    private String[] echoStderrCommandArray(String message) {
        if (isWindows) {
            // cmd /c echo message 1>&2
            return new String[]{"cmd", "/c", "echo", message, "1>&2"};
        } else {
            // /bin/sh -c 'echo message 1>&2'
            return new String[]{"/bin/sh", "-c", "echo " + message + " 1>&2"};
        }
    }

    private String echoStderrCommand(String message) {
        if (isWindows) {
            return "cmd /c echo " + message + " 1>&2";
        } else {
            return "/bin/sh -c \"echo " + message + " 1>&2\"";
        }
    }

    private String envPrintCommand(String varName) {
        if (isWindows) {
            // cmd /c echo %VAR%
            return "cmd /c echo %" + varName + "%";
        } else {
            // /bin/sh -c 'echo $VAR'
            return "/bin/sh -c \"echo $" + varName + "\"";
        }
    }

    private String[] envPrintCommandArray(String varName) {
        if (isWindows) {
            return new String[]{"cmd", "/c", "echo", "%" + varName + "%"};
        } else {
            return new String[]{"/bin/sh", "-c", "echo $" + varName};
        }
    }

    private String pwdCommand() {
        if (isWindows) {
            return "cmd /c cd";
        } else {
            return "/bin/sh -c pwd";
        }
    }

    private String[] pwdCommandArray() {
        if (isWindows) {
            return new String[]{"cmd", "/c", "cd"};
        } else {
            return new String[]{"/bin/sh", "-c", "pwd"};
        }
    }

    /* --------------------------------------------------------------------- */
    /* Tests for the simple exec(String) overload                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testExecSimpleCommandSuccess() {
        int exit = executor.exec(echoCommand("hello"));
        assertEquals("Process should exit with code 0", 0, exit);
        assertNotNull("Standard output must be captured", executor.getOut());
        assertTrue("Output should contain the echoed text",
                executor.getOut().trim().equals("hello"));
        assertEquals("No error output expected", "", executor.getError().trim());
    }

    @Test
    public void testExecSimpleCommandStderr() {
        int exit = executor.exec(echoStderrCommand("oops"));
        assertEquals("Process should exit with code 0", 0, exit);
        assertEquals("Stdout should be empty", "", executor.getOut().trim());
        assertTrue("Stderr must contain the message",
                executor.getError().trim().equals("oops"));
    }

    @Test
    public void testExecSimpleCommandInvalid() {
        // An obviously invalid command – Runtime.exec will throw IOException
        int exit = executor.exec("nonexistent_command_12345");
        assertEquals("Invalid command should return -1", -1, exit);
        // Since runIt() never ran, getters should stay null
        assertNull("Stdout should be null after failure", executor.getOut());
        assertNull("Stderr should be null after failure", executor.getError());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for exec(String[]) overload                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testExecArrayCommandSuccess() {
        String[] cmd = echoCommandArray("arrayTest");
        int exit = executor.exec(cmd);
        assertEquals(0, exit);
        assertEquals("arrayTest", executor.getOut().trim());
        assertEquals("", executor.getError().trim());
    }

    @Test
    public void testExecArrayCommandStderr() {
        String[] cmd = echoStderrCommandArray("arrayError");
        int exit = executor.exec(cmd);
        assertEquals(0, exit);
        assertEquals("", executor.getOut().trim());
        assertEquals("arrayError", executor.getError().trim());
    }

    @Test
    public void testExecArrayCommandInvalid() {
        String[] cmd = new String[]{"nonexistent_cmd_9876"};
        int exit = executor.exec(cmd);
        assertEquals(-1, exit);
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for exec(String, String[]) overload                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testExecWithEnvSuccess() {
        String varName = "EXECUTOR_TEST_VAR";
        String varValue = "EnvValue123";
        String[] env = {varName + "=" + varValue};

        int exit = executor.exec(envPrintCommand(varName), env);
        assertEquals(0, exit);
        assertEquals(varValue, executor.getOut().trim());
        assertEquals("", executor.getError().trim());
    }

    @Test
    public void testExecWithEnvInvalid() {
        String[] env = {"FOO=BAR"};
        int exit = executor.exec("nonexistent_cmd_555", env);
        assertEquals(-1, exit);
        assertNull(executor.getOut());
        assertNull(executor.getError());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for exec(String[], String[]) overload                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testExecArrayWithEnvSuccess() {
        String varName = "EXECUTOR_ARRAY_VAR";
        String varValue = "ArrayEnv";
        String[] env = {varName + "=" + varValue};

        int exit = executor.exec(envPrintCommandArray(varName), env);
        assertEquals(0, exit);
        assertEquals(varValue, executor.getOut().trim());
        assertEquals("", executor.getError().trim());
    }

    /* --------------------------------------------------------------------- */
    /* Tests for exec(String, String[], File) overload                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testExecWithEnvAndDirectory() throws IOException {
        // Create a temporary directory and a file inside it
        Path tempDir = Files.createTempDirectory("execTestDir");
        Path tempFile = Files.createTempFile(tempDir, "sample", ".txt");

        // Command that prints the current working directory
        int exit = executor.exec(pwdCommand(), null, tempDir.toFile());
        assertEquals(0, exit);
        String cwd = executor.getOut().trim();

        // The cwd reported by the process must be the temporary directory
        assertEquals(tempDir.toAbsolutePath().toString(), cwd);
        assertEquals("", executor.getError().trim());

        // Cleanup
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(tempDir);
    }

    /* --------------------------------------------------------------------- */
    /* Tests for exec(String[], String[], File) overload                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testExecArrayWithEnvAndDirectory() throws IOException {
        Path tempDir = Files.createTempDirectory("execArrayDir");
        String varName = "DIR_VAR";
        String varValue = "DirValue";

        String[] env = {varName + "=" + varValue};

        // Use the pwd command array to verify directory handling
        int exit = executor.exec(pwdCommandArray(), env, tempDir.toFile());
        assertEquals(0, exit);
        assertEquals(tempDir.toAbsolutePath().toString(), executor.getOut().trim());
        // No error expected
        assertEquals("", executor.getError().trim());

        Files.deleteIfExists(tempDir);
    }

    /* --------------------------------------------------------------------- */
    /* Reflection test for the private cmdArrayToString method                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCmdArrayToStringViaReflection() throws Exception {
        Method m = Executor.class.getDeclaredMethod("cmdArrayToString", String[].class);
        m.setAccessible(true);

        String[] sample = {"one", "two", "three"};
        String result = (String) m.invoke(executor, (Object) sample);
        // Expected: each element followed by a space, trailing space included
        assertEquals("one two three ", result);
    }

    /* --------------------------------------------------------------------- */
    /* Additional sanity check: after a successful call, subsequent call   */
    /* with a failing command should reset the stored output to null.       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testOutputClearedAfterFailure() {
        // First a successful command
        int ok = executor.exec(echoCommand("first"));
        assertEquals(0, ok);
        assertNotNull(executor.getOut());

        // Then an invalid command
        int fail = executor.exec("invalid_cmd_xyz");
        assertEquals(-1, fail);
        assertNull("Out should be cleared after failure", executor.getOut());
        assertNull("Error should be cleared after failure", executor.getError());
    }
}
