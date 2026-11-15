package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.File;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// No mocking is actually required, but kept per template
@ExtendWith(MockitoExtension.class)
class Executor_exec_2_Test {

    private static final String THIS_IS_HANDY = "This is handy";

    private static final String ECHO_THIS_IS_HANDY = "echo " + THIS_IS_HANDY;

    private Executor executor;

    @BeforeEach
    void setupBeforeEach() {
        executor = new Executor();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean‑up per‑test resources
        executor = null;
    }

    /**
     * Supplies all test cases.  OS‑specific cases are filtered inside the test
     * method using {@link Assumptions#assumeTrue(boolean)}.
     */
    private static Stream<ExecCase> execCasesProvider() {
        return Stream.of(// Successful execution – simple echo (works on both platforms)
        new ExecCase("Valid command array – echo hello (mocked to 0)", new String[] { "echo", "hello" }, 0), // Null command array – should cause NullPointerException inside Runtime.exec
        new ExecCase("Null command array – expect -1", null, -1), // Empty command array – IllegalArgumentException
        new ExecCase("Empty command array – expect -1", new String[] {}, -1), // Non‑existent command – IOException from Runtime.exec
        new ExecCase("Non‑existent command – expect -1", new String[] { "nonexistentcommand" }, -1), // Command array containing a null element – NullPointerException
        new ExecCase("Array with null element – expect -1", new String[] { "echo", null }, -1), // Long‑running command (sleep) – we rely on the OS to finish quickly in the test
        new ExecCase("Long‑running command (sleep 1) – expect 0", // Use a short sleep to keep test fast; on Windows we use 'timeout /t 1'
        isWindows() ? new String[] { "cmd.exe", "/c", "timeout", "/t", "1", "/nobreak" } : new String[] { "sh", "-c", "sleep 1" }, 0), // Too many arguments – may throw IOException (argument list too long)
        new ExecCase("Too many arguments – expect -1", new String[] { "echo", "hello", "extra", "args" }, -1), // Command consisting only of whitespace – invalid
        new ExecCase("Whitespace command – expect -1", new String[] { " " }, -1), // /bin/false – exists on Unix, exits with non‑zero status (0 expected per spec)
        new ExecCase("/bin/false – exit code should be returned (non‑zero)", new String[] { "/bin/false" }, // The real exit code of /bin/false is 1; we assert that exec returns that value,
        // not -1.
        1), // /bin/true – exists on Unix, exits with zero
        new ExecCase("/bin/true – exit code 0", new String[] { "/bin/true" }, 0), // Command with embedded null character – IllegalArgumentException
        new ExecCase("Command with null character – expect -1", new String[] { "echo", "\u0000" }, -1));
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * Verify that {@link Executor#exec(String[])} correctly executes a simple command
     * and captures its standard output.
     */
    @Test
    void testExecStringArraySuccess() {
        String os = System.getProperty("os.name").toLowerCase();
        // Build the command array appropriate for the OS
        String[] cmd;
        if (os.contains("windows")) {
            // Windows needs to invoke cmd.exe with /c
            cmd = new String[] { "cmd.exe", "/c", ECHO_THIS_IS_HANDY };
        } else {
            // Unix‑like shells can execute the command directly
            cmd = new String[] { "sh", "-c", ECHO_THIS_IS_HANDY };
        }
        int exitCode = executor.exec(cmd);
        assertEquals(0, exitCode, "Process should exit with code 0");
        assertEquals(THIS_IS_HANDY, executor.getOut().trim(), "Standard output should contain the echoed string");
    }

    /**
     * Verify that a non‑existent command results in the error path returning {@code -1}.
     */
    @Test
    void testExecStringArrayFailure() {
        String[] bogusCmd = new String[] { "this-command-does-not-exist-12345" };
        int exitCode = executor.exec(bogusCmd);
        assertEquals(-1, exitCode, "Execution of an invalid command should return -1");
        // The error message is printed to System.err; we only verify the return code here.
    }

    /**
     * Parameterised test that iterates over all cases defined in {@link #execCasesProvider()}.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("execCasesProvider")
    void testExecStringArrayVarious(ExecCase execCase) {
        // Skip Unix‑only cases on Windows and vice‑versa where appropriate
        if (execCase.cmdarray != null && execCase.cmdarray.length > 0) {
            // Cases involving absolute Unix paths or Unix shell syntax
            boolean unixSpecific = execCase.cmdarray[0].startsWith("/") || execCase.cmdarray[0].equals("sh") || execCase.cmdarray[0].equals("timeout");
            assumeTrue(!isWindows() || !unixSpecific, "Skipping Unix‑specific test on Windows");
        }
        int exitCode = executor.exec(execCase.cmdarray);
        assertEquals(execCase.expectedExit, exitCode, () -> "Unexpected exit code for case: " + execCase.description);
        // Additional verification for successful executions that produce output
        if (exitCode == 0 && execCase.cmdarray != null && execCase.cmdarray.length > 0) {
            String first = execCase.cmdarray[0];
            // For echo commands we expect the echoed argument in stdout
            if (first.equals("echo") || first.equals("sh") || first.equals("cmd.exe")) {
                // The exact output depends on the OS command used; we simply ensure stdout is non‑empty
                assertNotNull(executor.getOut(), "Stdout should not be null on success");
                assertFalse(executor.getOut().isBlank(), "Stdout should contain something for echo‑like commands");
            }
        }
        // For error paths we expect getError() to be populated (implementation detail)
        if (exitCode == -1) {
            assertNotNull(executor.getError(), "Error message should be captured when exec returns -1");
        }
    }
}
