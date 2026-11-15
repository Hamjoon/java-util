package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;

/**
 * JUnit 4 test suite for {@link Executor}.
 *
 * The tests exercise all overloads of {@code exec(...)} and the
 * accessor methods {@code getOut()} and {@code getError()}.  The
 * test cases use only real commands that are expected to be present on
 * any POSIX‑compatible system (the test class assumes it runs on a
 * Unix‑like environment; on Windows the command {@code cmd /c echo …}
 * could be used instead).  No mocking or subclassing is performed – the
 * tests instantiate {@link Executor} directly and let the JVM create
 * the real {@link Process} objects.
 */
public class ExecutorTest {

    /** Helper that decides which simple command works on the current platform. */
    private static String[] getEchoCommand(String message) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: use cmd /c echo …
            return new String[]{"cmd", "/c", "echo", message};
        } else {
            // Unix/Linux/macOS: use /bin/sh -c "echo …"
            return new String[]{"/bin/sh", "-c", "echo " + message};
        }
    }

    /** Simple command that succeeds and writes a known string to stdout. */
    private static String getEchoStringCommand(String message) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "cmd /c echo " + message;
        } else {
            return "echo " + message;
        }
    }

    @Test
    public void testExecString_successfulCommand() {
        Executor exec = new Executor();
        String expected = "hello world";

        int rc = exec.exec(getEchoStringCommand(expected));
        assertEquals("Process should exit with code 0", 0, rc);
        // The echo command appends a line‑separator; trim it for comparison.
        assertEquals(expected, exec.getOut().trim());
        // No error output expected.
        assertTrue(exec.getError() == null || exec.getError().isEmpty());
    }

    @Test
    public void testExecStringArray_successfulCommand() {
        Executor exec = new Executor();
        String expected = "array‑test";

        String[] cmd = getEchoCommand(expected);
        int rc = exec.exec(cmd);
        assertEquals(0, rc);
        assertEquals(expected, exec.getOut().trim());
        assertTrue(exec.getError() == null || exec.getError().isEmpty());
    }

    @Test
    public void testExecWithEnvp_stringCommand() {
        Executor exec = new Executor();
        // Use a command that prints the value of a custom environment variable.
        String varName = "EXECUTER_TEST_VAR";
        String varValue = "env‑value";

        String command;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows: use cmd /c echo %VAR%
            command = "cmd /c echo %" + varName + "%";
        } else {
            // Unix: use sh -c "echo $VAR"
            command = "/bin/sh -c \"echo $" + varName + "\"";
        }

        int rc = exec.exec(command, new String[]{varName + "=" + varValue});
        assertEquals(0, rc);
        assertEquals(varValue, exec.getOut().trim());
        assertTrue(exec.getError() == null || exec.getError().isEmpty());
    }

    @Test
    public void testExecWithEnvp_stringArrayCommand() {
        Executor exec = new Executor();
        String varName = "EXECUTER_TEST_VAR2";
        String varValue = "array‑env";

        String[] cmd;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            cmd = new String[]{"cmd", "/c", "echo", "%" + varName + "%"};
        } else {
            cmd = new String[]{"/bin/sh", "-c", "echo $" + varName};
        }

        int rc = exec.exec(cmd, new String[]{varName + "=" + varValue});
        assertEquals(0, rc);
        assertEquals(varValue, exec.getOut().trim());
        assertTrue(exec.getError() == null || exec.getError().isEmpty());
    }

    @Test
    public void testExecWithWorkingDirectory() {
        Executor exec = new Executor();
        // Create a temporary directory and ask the process to list it.
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        assertTrue("Temp directory must exist", tmpDir.isDirectory());

        String command;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            command = "cmd /c dir";
        } else {
            command = "ls";
        }

        int rc = exec.exec(command, null, tmpDir);
        assertEquals(0, rc);
        // The output should contain the name of the directory (or be non‑empty).
        assertNotNull(exec.getOut());
        assertFalse(exec.getOut().isEmpty());
        // No error expected for a valid directory.
        assertTrue(exec.getError() == null || exec.getError().isEmpty());
    }

    @Test
    public void testExecWithWorkingDirectory_stringArray() {
        Executor exec = new Executor();
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        assertTrue(tmpDir.isDirectory());

        String[] cmd;
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            cmd = new String[]{"cmd", "/c", "dir"};
        } else {
            cmd = new String[]{"ls"};
        }

        int rc = exec.exec(cmd, null, tmpDir);
        assertEquals(0, rc);
        assertNotNull(exec.getOut());
        assertFalse(exec.getOut().isEmpty());
        assertTrue(exec.getError() == null || exec.getError().isEmpty());
    }

    @Test
    public void testExecInvalidCommand_returnsMinusOneAndLeavesOutputsNull() {
        Executor exec = new Executor();
        // Use a command that is guaranteed not to exist.
        String bogus = "nonexistent_command_12345";

        int rc = exec.exec(bogus);
        assertEquals("Execution of a non‑existent command should return -1", -1, rc);
        // After a failure the getters should still be null because runIt was never called.
        assertNull(exec.getOut());
        assertNull(exec.getError());
    }

    @Test
    public void testExecInvalidCommandArray_returnsMinusOneAndLeavesOutputsNull() {
        Executor exec = new Executor();
        String[] bogus = new String[]{"nonexistent_command_12345", "arg1"};

        int rc = exec.exec(bogus);
        assertEquals(-1, rc);
        assertNull(exec.getOut());
        assertNull(exec.getError());
    }

    @Test
    public void testCmdArrayToString_isUsedInErrorLogging() {
        // This test forces an exception path to ensure that the private
        // cmdArrayToString method is exercised (it is called only when an
        // exec overload fails).  We capture the standard error stream to
        // verify that the generated string contains the array elements.
        Executor exec = new Executor();
        String[] bogus = new String[]{"nonexistent_command_12345", "foo", "bar"};

        // Redirect System.err temporarily.
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream originalErr = System.err;
        System.setErr(new java.io.PrintStream(baos));

        try {
            int rc = exec.exec(bogus);
            assertEquals(-1, rc);
        } finally {
            // Restore original System.err
            System.setErr(originalErr);
        }

        String logged = baos.toString();
        // The logged message must contain each element of the array separated by spaces.
        assertTrue("Logged message should contain the first element",
                logged.contains("nonexistent_command_12345"));
        assertTrue("Logged message should contain the second element",
                logged.contains("foo"));
        assertTrue("Logged message should contain the third element",
                logged.contains("bar"));
    }
}
