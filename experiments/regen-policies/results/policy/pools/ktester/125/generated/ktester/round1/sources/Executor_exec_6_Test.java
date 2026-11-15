package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.security.AccessControlException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Executor_exec_6_Test {

    private static final String THIS_IS_HANDY = "This is handy";

    private static final String ECHO_THIS_IS_HANDY = "echo " + THIS_IS_HANDY;

    private File tempDir;

    /**
     * Detect the operating system once for the whole test class.
     */
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    private static SecurityManager originalSecurityManager;

    /* --------------------------------------------------------------
     *  Helper data structures for parameterized tests
     * -------------------------------------------------------------- */
    private static class ExecCase {

        final String description;

        final String[] cmdarray;

        final String[] envp;

        final File dir;

        final int expectedExitCode;

        // true if getOut() should be null/empty
        final boolean expectOutEmpty;

        ExecCase(String description, String[] cmdarray, String[] envp, File dir, int expectedExitCode, boolean expectOutEmpty) {
            this.description = description;
            this.cmdarray = cmdarray;
            this.envp = envp;
            this.dir = dir;
            this.expectedExitCode = expectedExitCode;
            this.expectOutEmpty = expectOutEmpty;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeAll
    static void captureOriginalSecurityManager() {
        originalSecurityManager = System.getSecurityManager();
    }

    @BeforeEach
    void setupBeforeEach() throws IOException {
        // Create a temporary working directory for the exec calls.
        Path dir = Files.createTempDirectory("executor-test-");
        tempDir = dir.toFile();
    }

    @AfterEach
    void teardownAfterEach() throws IOException {
        // Recursively delete the temporary directory.
        if (tempDir != null && tempDir.exists()) {
            Files.walk(tempDir.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @AfterAll
    static void restoreSecurityManager() {
        System.setSecurityManager(originalSecurityManager);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for error handling
     * -------------------------------------------------------------- */
    private static Stream<ExecCase> errorCases() throws IOException {
        // Helper: a regular file (not a directory) for the "dir is a file" case.
        Path regularFilePath = Files.createTempFile("executor-test-file", ".txt");
        File regularFile = regularFilePath.toFile();
        // Helper: a non‑existent directory.
        File nonExistentDir = new File("/nonexistent-dir-" + System.nanoTime());
        // Helper: a temporary directory that we will delete to become non‑existent.
        Path tempDeletedDir = Files.createTempDirectory("executor-deleted-");
        File deletedDir = tempDeletedDir.toFile();
        // now does not exist
        Files.delete(tempDeletedDir);
        return Stream.of(new ExecCase("null cmdarray -> NullPointerException", null, null, null, -1, true), new ExecCase("empty cmdarray -> IllegalArgumentException", new String[] {}, null, null, -1, true), new ExecCase("non‑existent working directory -> IOException", new String[] { "echo", "test" }, null, nonExistentDir, -1, true), new ExecCase("environment array contains null element -> IllegalArgumentException", new String[] { "echo", "test" }, new String[] { "VAR1=VAL1", null }, null, -1, true), new ExecCase("empty environment array (treated as null) -> should succeed on most platforms", new String[] { "echo", "test" }, new String[] {}, null, // both should succeed; we keep expected 0
        IS_WINDOWS ? 0 : 0, false), new ExecCase("working directory is a regular file -> IOException", new String[] { "echo", "test" }, null, regularFile, -1, true), new ExecCase("first element empty string -> IOException (cannot locate program)", new String[] { "", "arg1" }, null, null, -1, true), new ExecCase("SecurityException via custom SecurityManager", new String[] { "echo", "secure" }, null, null, -1, true));
    }

    /**
     * Verify that {@code exec(String[], String[], File)} runs a valid command,
     * returns the expected exit code and captures the standard output.
     */
    @Test
    void testExecWithValidCommandAndWorkingDirectory() {
        // Build a command that works on both Windows and Unix‑like systems.
        String[] cmd = IS_WINDOWS ? new String[] { "cmd.exe", "/c", "echo", THIS_IS_HANDY } : new String[] { "echo", THIS_IS_HANDY };
        Executor executor = new Executor();
        int exitCode = executor.exec(cmd, null, tempDir);
        // The command should succeed (exit code 0) and its output should be captured.
        assertEquals(0, exitCode, "Expected successful execution (exit code 0)");
        assertNotNull(executor.getOut(), "Standard output should not be null");
        assertEquals(THIS_IS_HANDY, executor.getOut().trim(), "Standard output should contain the echoed string");
    }

    /**
     * Verify that when the underlying {@code Runtime.exec} throws an exception
     * (e.g., because the command does not exist), the method returns {@code -1}
     * and does not populate any output.
     */
    @Test
    void testExecWithInvalidCommandReturnsMinusOne() {
        // Use a command that is guaranteed not to exist.
        String[] cmd = new String[] { "nonexistent_command_12345" };
        Executor executor = new Executor();
        int exitCode = executor.exec(cmd, null, tempDir);
        // The method catches the exception and returns -1.
        assertEquals(-1, exitCode, "Expected -1 when command execution fails");
        // No output should have been captured.
        assertTrue(executor.getOut() == null || executor.getOut().isEmpty(), "Standard output should be empty on failure");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("errorCases")
    void testExecErrorScenarios(ExecCase c) throws Exception {
        // Install a restrictive SecurityManager only for the specific case that needs it.
        if (c.description.contains("SecurityException")) {
            System.setSecurityManager(new SecurityManager() {

                @Override
                public void checkExec(String cmd) {
                    throw new AccessControlException("Execution denied by test SecurityManager");
                }

                @Override
                public void checkPermission(java.security.Permission perm) {
                    // allow everything else
                }
            });
        }
        Executor executor = new Executor();
        int exitCode = executor.exec(c.cmdarray, c.envp, c.dir);
        assertEquals(c.expectedExitCode, exitCode, "Unexpected exit code for case: " + c.description);
        if (c.expectOutEmpty) {
            assertTrue(executor.getOut() == null || executor.getOut().isEmpty(), "Output should be empty on failure for case: " + c.description);
        } else {
            // For the "empty envp" case we expect a successful execution.
            assertNotNull(executor.getOut(), "Output should not be null for case: " + c.description);
            assertFalse(executor.getOut().isEmpty(), "Output should contain something for case: " + c.description);
        }
        // Cleanup the custom SecurityManager if we installed one.
        if (c.description.contains("SecurityException")) {
            System.setSecurityManager(originalSecurityManager);
        }
    }
}
