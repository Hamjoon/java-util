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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class Executor_exec_5_Test {

    /**
     * Temporary working directory used for the {@code dir} argument.
     */
    private static Path tempDir;

    /**
     * Instance under test – created fresh for each test method.
     */
    private Executor executor;

    /**
     * A simple string we expect the command to echo back.
     */
    private static final String THIS_IS_HANDY = "This is handy";

    /**
     * Platform‑specific echo command.
     */
    private static String echoCommand;

    @BeforeAll
    static void setupBeforeAll() throws IOException {
        // Create a temporary directory that will be used as the working directory.
        tempDir = java.nio.file.Files.createTempDirectory("executor-test-");
        // Determine the appropriate echo command for the current OS.
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            // Windows needs to invoke cmd.exe /c echo ...
            echoCommand = "cmd.exe /c echo " + THIS_IS_HANDY;
        } else {
            // Unix‑like shells support a plain echo command.
            echoCommand = "echo " + THIS_IS_HANDY;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh Executor for each test ensures no state leakage between tests.
        executor = new Executor();
    }

    @AfterAll
    static void teardownAfterAll() throws IOException {
        // Recursively delete the temporary directory created in setupBeforeAll().
        if (tempDir != null && java.nio.file.Files.exists(tempDir)) {
            java.nio.file.Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
        }
    }

    /**
     * Provides parameters for the successful‑execution group.
     *
     * Each element is an array: {command, envp, dir, stubbedRunItResult, expectedReturn}
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> successfulExecutionProvider() {
        return Stream.of(// case 1 – normal command, non‑null env, non‑null dir, runIt returns 0
        org.junit.jupiter.params.provider.Arguments.of("echo Hello", new String[] { "PATH=/usr/bin" }, new File("/tmp"), 0, 0), // case 2 – command with Windows style, null env and dir, runIt returns 5
        org.junit.jupiter.params.provider.Arguments.of("cmd /c dir", null, null, 5, 5));
    }

    /**
     * Provides parameters for error‑handling cases where Runtime.exec throws.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> execThrowsProvider() {
        return Stream.of(// empty command – IllegalArgumentException
        org.junit.jupiter.params.provider.Arguments.of("", null, null, IllegalArgumentException.class), // null command – NullPointerException
        org.junit.jupiter.params.provider.Arguments.of(null, null, null, NullPointerException.class), // nonexistent command – IOException
        org.junit.jupiter.params.provider.Arguments.of("nonexistentcommand", null, null, IOException.class), // non‑existent working directory – IOException
        org.junit.jupiter.params.provider.Arguments.of("echo Test", null, new File("/nonexistent/path"), IOException.class), // malformed env – IOException (simulated)
        org.junit.jupiter.params.provider.Arguments.of("echo Test", new String[] { "INVALID_ENV" }, null, IOException.class), // env array containing null – NullPointerException
        org.junit.jupiter.params.provider.Arguments.of("echo Test", new String[] { "PATH=/usr/bin", null }, null, NullPointerException.class), // file instead of directory – IOException
        org.junit.jupiter.params.provider.Arguments.of("echo Test", null, createTempFile(), IOException.class), // security manager restriction – SecurityException
        org.junit.jupiter.params.provider.Arguments.of("echo Test", null, null, SecurityException.class));
    }

    /**
     * Helper that creates a temporary regular file (not a directory) for the
     * “file instead of directory” test case.
     */
    private static File createTempFile() throws IOException {
        java.nio.file.Path p = java.nio.file.Files.createTempFile("executor-file-", ".txt");
        p.toFile().deleteOnExit();
        return p.toFile();
    }

    @Test
    void testExecWithEnvAndDir_successfulEcho() {
        // GIVEN – a valid command, no extra environment variables and a working directory.
        String command = echoCommand;
        // null means inherit the current environment.
        String[] envp = null;
        File dir = tempDir.toFile();
        // WHEN – the command is executed.
        int exitCode = executor.exec(command, envp, dir);
        // THEN – the process should finish successfully and the output should match.
        assertTrue(exitCode >= 0, "Expected a non‑negative exit code for a successful command");
        assertEquals(THIS_IS_HANDY, executor.getOut().trim(), "The captured stdout should contain the echoed string");
        assertEquals("", executor.getError().trim(), "There should be no error output for a successful command");
    }

    @Test
    void testExecWithEnvAndDir_invalidCommand_returnsMinusOne() {
        // GIVEN – a command that does not exist.
        String command = "nonexistent_command_12345";
        String[] envp = new String[] { "FOO=BAR" };
        File dir = tempDir.toFile();
        // WHEN – the command is executed.
        int exitCode = executor.exec(command, envp, dir);
        // THEN – the method should return -1 and capture the error message.
        assertEquals(-1, exitCode, "Expected -1 for a command that cannot be executed");
        assertTrue(executor.getError().toLowerCase().contains("error occurred"), "Error output should contain the generic error message");
    }

    @ParameterizedTest(name = "[{index}] exec returns {4} when runIt returns {3}")
    @MethodSource("successfulExecutionProvider")
    void testExec_successfulMocked(String command, String[] envp, File dir, int stubbedRunItResult, int expectedReturn) throws Exception {
        // ----- Arrange -------------------------------------------------
        // Mock the Process that Runtime.exec will return.
        Process mockProcess = mock(Process.class);
        // Mock static Runtime.getRuntime()
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            when(mockRuntime.exec(eq(command), eq(envp), eq(dir))).thenReturn(mockProcess);
            // Spy the Executor so we can stub the private runIt method.
            Executor spyExecutor = Mockito.spy(new Executor());
            // Use reflection to make the private runIt method return the desired value.
            java.lang.reflect.Method runItMethod = Executor.class.getDeclaredMethod("runIt", Process.class);
            runItMethod.setAccessible(true);
            // Stub the call – when runIt is invoked with any Process, return stubbedRunItResult.
            doReturn(stubbedRunItResult).when(spyExecutor, runItMethod).invoke(spyExecutor, mockProcess);
            // ----- Act ----------------------------------------------------
            int actual = spyExecutor.exec(command, envp, dir);
            // ----- Assert -------------------------------------------------
            assertEquals(expectedReturn, actual, "exec should propagate the value returned by runIt");
        }
    }

    @ParameterizedTest(name = "[{index}] exec returns -1 when Runtime.exec throws {3}")
    @MethodSource("execThrowsProvider")
    void testExec_errorHandlingMocked(String command, String[] envp, File dir, Class<? extends Exception> exceptionClass) throws Exception {
        // ----- Arrange -------------------------------------------------
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // Configure the mock to throw the desired exception.
            when(mockRuntime.exec(eq(command), eq(envp), eq(dir))).thenThrow(exceptionClass);
            // Use a plain Executor (no need to spy – runIt will never be called).
            Executor exec = new Executor();
            // ----- Act ----------------------------------------------------
            int result = exec.exec(command, envp, dir);
            // ----- Assert -------------------------------------------------
            assertEquals(-1, result, "When Runtime.exec throws " + exceptionClass.getSimpleName() + " exec should return -1");
            // The error message is printed to System.err; we only verify the return code here.
        }
    }

    /**
     * Simulates the situation where {@code Runtime.exec} succeeds but the private
     * {@code runIt} method throws an {@link InterruptedException}.
     */
    @Test
    void testExec_interruptedDuringRunIt_returnsMinusOne() throws Exception {
        // ----- Arrange -------------------------------------------------
        Process mockProcess = mock(Process.class);
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            when(mockRuntime.exec(anyString(), any(), any())).thenReturn(mockProcess);
            Executor spyExecutor = Mockito.spy(new Executor());
            // Stub private runIt to throw InterruptedException.
            java.lang.reflect.Method runItMethod = Executor.class.getDeclaredMethod("runIt", Process.class);
            runItMethod.setAccessible(true);
            doThrow(new InterruptedException("simulated interruption")).when(spyExecutor, runItMethod).invoke(spyExecutor, mockProcess);
            // ----- Act ----------------------------------------------------
            int result = spyExecutor.exec("echo Test", null, null);
            // ----- Assert -------------------------------------------------
            assertEquals(-1, result, "If runIt throws InterruptedException exec should return -1");
        }
    }
}
