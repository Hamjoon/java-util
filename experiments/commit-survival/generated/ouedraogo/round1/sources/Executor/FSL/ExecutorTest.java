package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class ExecutorTest {

    private Executor executor;

    @Before
    public void setUp() {
        executor = new Executor();
    }

    @After
    public void tearDown() {
        executor = null;
    }

    /**
     * Simple command that writes to STDOUT and exits with 0.
     */
    @Test
    public void testExecStringSuccess() {
        // “echo” is available on virtually all Unix‑like platforms used by CI.
        int exit = executor.exec("echo hello");
        assertEquals("Process should exit with 0", 0, exit);
        assertEquals("STDOUT should contain the echoed text (including newline)",
                "hello\n", executor.getOut());
        assertTrue("STDERR should be empty", executor.getError() == null || executor.getError().isEmpty());
    }

    /**
     * Command array that writes to both STDOUT and STDERR and returns a non‑zero exit code.
     */
    @Test
    public void testExecStringArraySuccess() {
        // Use the POSIX shell to emit controlled output.
        String[] cmd = {"sh", "-c", "echo out; echo err 1>&2; exit 3"};
        int exit = executor.exec(cmd);
        assertEquals("Process should exit with code 3", 3, exit);
        assertEquals("STDOUT should be \"out\" followed by newline", "out\n", executor.getOut());
        assertEquals("STDERR should be \"err\" followed by newline", "err\n", executor.getError());
    }

    /**
     * Verify that the overload accepting an environment array works.
     */
    @Test
    public void testExecStringWithEnv() {
        // The variable MYVAR is defined only for the child process.
        String[] env = {"MYVAR=12345"};
        int exit = executor.exec("sh -c 'echo $MYVAR'", env);
        assertEquals("Process should exit with 0", 0, exit);
        assertEquals("STDOUT should contain the value of MYVAR", "12345\n", executor.getOut());
        assertTrue("STDERR should be empty", executor.getError() == null || executor.getError().isEmpty());
    }

    /**
     * Verify the overload that also receives a working directory.
     */
    @Test
    public void testExecStringArrayWithEnvAndDir() throws IOException {
        // Create a temporary directory to be used as the working directory.
        File tempDir = File.createTempFile("execTest", "");
        assertTrue(tempDir.delete());               // delete the file
        assertTrue(tempDir.mkdir());                // create a directory with the same name

        try {
            String[] cmd = {"sh", "-c", "pwd"};
            String[] env = {}; // no special environment needed
            int exit = executor.exec(cmd, env, tempDir);
            assertEquals("Process should exit with 0", 0, exit);
            // The output of pwd ends with a newline – trim it for comparison.
            assertEquals("STDOUT should be the temporary directory path",
                    tempDir.getAbsolutePath() + "\n", executor.getOut());
            assertTrue("STDERR should be empty", executor.getError() == null || executor.getError().isEmpty());
        } finally {
            // Cleanup the temporary directory.
            tempDir.delete();
        }
    }

    /**
     * When Runtime.exec throws an exception (e.g., null command) the method must
     * return -1 and not change previously stored output/error values.
     */
    @Test
    public void testExecExceptionHandling() {
        // First execute a successful command to have known state.
        executor.exec("echo init");
        assertEquals("init\n", executor.getOut());

        // Now invoke exec with a null command – this triggers a NullPointerException.
        int exit = executor.exec((String) null);
        assertEquals("When an exception occurs exec should return -1", -1, exit);
        // The previously captured output must remain unchanged.
        assertEquals("Previous STDOUT should stay unchanged after failure", "init\n", executor.getOut());
        // Error may be null because the failure happened before runIt was called.
        // We only assert that it is either null or unchanged.
        assertTrue(executor.getError() == null || executor.getError().isEmpty());
    }

    /**
     * Directly test the private cmdArrayToString utility via reflection.
     */
    @Test
    public void testCmdArrayToString() throws Exception {
        Method method = Executor.class.getDeclaredMethod("cmdArrayToString", String[].class);
        method.setAccessible(true);
        String[] input = {"one", "two", "three"};
        String result = (String) method.invoke(executor, (Object) input);
        assertEquals("The concatenated string must contain each element followed by a space",
                "one two three ", result);
    }

    /**
     * Verify that successive calls overwrite the stored STDOUT/STDERR fields.
     */
    @Test
    public void testSuccessiveCallsOverwriteState() {
        // First call – produce stdout only.
        executor.exec("echo first");
        assertEquals("first\n", executor.getOut());
        assertTrue(executor.getError() == null || executor.getError().isEmpty());

        // Second call – produce stderr only.
        executor.exec(new String[]{"sh", "-c", "echo err 1>&2"});
        assertTrue(executor.getOut() == null || executor.getOut().isEmpty());
        assertEquals("err\n", executor.getError());
    }
}
