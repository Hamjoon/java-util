package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class Executor_exec_6_Test {

    private Executor executor;

    private static File workingDir;

    // used for “file instead of directory” test
    private static File regularFile;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Temporary working directory for the tests
        workingDir = Files.createTempDirectory("executor-test-dir").toFile();
        workingDir.deleteOnExit();
        // Temporary regular file (not a directory) for a specific test case
        regularFile = File.createTempFile("executor-test-file", ".txt");
        regularFile.deleteOnExit();
    }

    @BeforeEach
    void setupBeforeEach() {
        executor = new Executor();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Recursively delete the temporary directory created for the tests
        deleteRecursively(workingDir);
    }

    /**
     * Utility method to delete a directory tree recursively.
     */
    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    /**
     * Tests the {@code exec(String[], String[], File)} method using a simple echo command.
     * The test works on both Windows and Unix‑like platforms.
     */
    @Test
    void testExecWithCmdArrayAndWorkingDirectory() {
        String[] cmd;
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("windows")) {
            // Windows: use cmd.exe /c echo <message>
            cmd = new String[] { "cmd.exe", "/c", "echo", "ExecutorTest" };
        } else {
            // Unix / Linux / macOS
            cmd = new String[] { "sh", "-c", "echo ExecutorTest" };
        }
        // No special environment variables for this test
        String[] env = null;
        int exitCode = executor.exec(cmd, env, workingDir);
        // The process should finish successfully
        assertEquals(0, exitCode, "Process should exit with code 0");
        // The captured standard output should contain the echoed text
        assertEquals("ExecutorTest", executor.getOut().trim(), "Standard output should match the echoed string");
    }

    @Test
    void testSuccessfulExecution_realEcho() {
        // Real execution of a simple echo command – works on both Windows and Unix‑like OSes.
        String[] cmd;
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("windows")) {
            cmd = new String[] { "cmd.exe", "/c", "echo", "ExecutorTest" };
        } else {
            cmd = new String[] { "sh", "-c", "echo ExecutorTest" };
        }
        int exitCode = executor.exec(cmd, null, workingDir);
        assertEquals(0, exitCode, "Process should exit with code 0");
        assertEquals("ExecutorTest", executor.getOut().trim(), "Standard output should match the echoed string");
    }

    @Test
    void testSuccessfulExecution_customExitCode_mockedProcess() {
        // Mock Runtime.exec to return a Process whose waitFor() returns 5.
        Process mockProcess = Mockito.mock(Process.class);
        try {
            Mockito.when(mockProcess.waitFor()).thenReturn(5);
        } catch (InterruptedException ignored) {
        }
        try (MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class)) {
            mockedRuntime.when(() -> Runtime.getRuntime().exec(any(String[].class), any(String[].class), any(File.class))).thenReturn(mockProcess);
            // command content is irrelevant – we mock it
            String[] cmd = { "cmd", "/c", "exit", "5" };
            String[] env = { "VAR1=VALUE1", "VAR2=VALUE2" };
            // any existing dir
            File dir = new File(System.getProperty("java.io.tmpdir"));
            int exitCode = executor.exec(cmd, env, dir);
            assertEquals(5, exitCode, "Mocked process should return exit code 5");
        }
    }

    @Test
    void testException_nullCmdArray() {
        int result = executor.exec(null, null, null);
        assertEquals(-1, result, "Null cmdarray should cause -1");
    }

    @Test
    void testException_emptyCmdArray() {
        int result = executor.exec(new String[] {}, null, null);
        assertEquals(-1, result, "Empty cmdarray should cause -1");
    }

    @Test
    void testException_nonexistentCommand() {
        int result = executor.exec(new String[] { "nonexistentcommand" }, null, null);
        assertEquals(-1, result, "Non‑existent command should cause -1");
    }

    @Test
    void testException_restrictedDirectory() {
        // Simulate a SecurityException by mocking Runtime.exec to throw it.
        try (MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class)) {
            mockedRuntime.when(() -> Runtime.getRuntime().exec(any(String[].class), any(String[].class), any(File.class))).thenThrow(new SecurityException("Access denied"));
            int result = executor.exec(new String[] { "echo", "test" }, null, new File("/restricted"));
            assertEquals(-1, result, "SecurityException should result in -1");
        }
    }

    @Test
    void testEnvironment_emptyEnvArray() {
        String[] cmd = { "echo", "envtest" };
        // empty but non‑null
        String[] env = {};
        int exitCode = executor.exec(cmd, env, null);
        assertEquals(0, exitCode, "Empty environment array should still succeed");
    }

    @Test
    void testEnvironment_nullElementInEnvArray() {
        String[] cmd = { "echo", "envnull" };
        // contains a null element
        String[] env = { null };
        int result = executor.exec(cmd, env, null);
        assertEquals(-1, result, "Null element in env array should cause -1");
    }

    @Test
    void testWorkingDirectory_nonexistentDir() {
        File nonExistent = new File(workingDir, "doesNotExist");
        int result = executor.exec(new String[] { "echo", "dirtest" }, null, nonExistent);
        assertEquals(-1, result, "Non‑existent working directory should cause -1");
    }

    @Test
    void testWorkingDirectory_fileInsteadOfDir() {
        int result = executor.exec(new String[] { "echo", "fileDir" }, null, regularFile);
        assertEquals(-1, result, "Passing a regular file as working directory should cause -1");
    }

    @Test
    void testCommandArray_singleTrueCommand_unixOnly() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("windows")) {
            // Windows does not have a built‑in “true” command – skip this test.
            return;
        }
        int exitCode = executor.exec(new String[] { "true" }, null, null);
        assertEquals(0, exitCode, "Command \"true\" should exit with 0 on Unix");
    }

    @Test
    void testCommandArray_nullElement() {
        String[] cmd = { null, "echo" };
        int result = executor.exec(cmd, null, null);
        assertEquals(-1, result, "Null element in cmdarray should cause -1");
    }

    @Test
    void testCommandArray_emptyString() {
        String[] cmd = { "" };
        int result = executor.exec(cmd, null, null);
        assertEquals(-1, result, "Empty command string should cause -1");
    }

    @Test
    void testInterruptedExecution_handledAsError() {
        // Mock a Process whose waitFor() throws InterruptedException.
        Process mockProcess = Mockito.mock(Process.class);
        try {
            Mockito.when(mockProcess.waitFor()).thenThrow(new InterruptedException("mock interrupt"));
        } catch (InterruptedException ignored) {
        }
        try (MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class)) {
            mockedRuntime.when(() -> Runtime.getRuntime().exec(any(String[].class), any(String[].class), any(File.class))).thenReturn(mockProcess);
            int result = executor.exec(new String[] { "echo", "interrupt" }, null, null);
            assertEquals(-1, result, "InterruptedException should result in -1");
        }
    }

    @Test
    void testCommandPath_directoryInsteadOfExecutable() {
        // Pass a directory path as the command – Runtime.exec should fail.
        String dirPath = workingDir.getAbsolutePath();
        int result = executor.exec(new String[] { dirPath }, null, null);
        assertEquals(-1, result, "Directory as command should cause -1");
    }

    @Test
    void testCommandPath_nonExecutableFile() throws IOException {
        // Create a temporary file without execute permission.
        File nonExecFile = File.createTempFile("noexec", ".txt");
        nonExecFile.deleteOnExit();
        // Ensure it is not executable (best‑effort – on Windows the flag is ignored).
        nonExecFile.setExecutable(false);
        int result = executor.exec(new String[] { nonExecFile.getAbsolutePath() }, null, null);
        // On many platforms this will throw IOException and be caught → -1.
        assertEquals(-1, result, "Non‑executable file should cause -1");
    }

    @Test
    void testSecurityManagerDenial() {
        // Install a SecurityManager that denies exec.
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkExec(String cmd) {
                throw new SecurityException("exec denied");
            }

            @Override
            public void checkPermission(java.security.Permission perm) {
                // Allow everything else.
            }
        });
        try {
            int result = executor.exec(new String[] { "echo", "secure" }, null, null);
            assertEquals(-1, result, "SecurityException should cause -1");
        } finally {
            // Restore original SecurityManager to avoid side effects on other tests.
            System.setSecurityManager(original);
        }
    }
}
