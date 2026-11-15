package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class Executor_exec_5_Test {

    private Executor executor;

    private static final String MESSAGE = "Executor test message";

    @BeforeEach
    void setupBeforeEach() {
        // fresh instance for each test
        executor = new Executor();
        // fresh instance for each test
        executor = new Executor();
    }

    /**
     * Supplies the successful execution test data.
     *
     * <ul>
     *   <li>Case 1 – normal command with environment and working directory, runIt returns 0.</li>
     *   <li>Case 2 – command with null envp and dir, runIt returns 5.</li>
     * </ul>
     */
    private static Stream<Arguments> successfulExecutionProvider() {
        return Stream.of(Arguments.of("ls -l", new String[] { "PATH=/usr/bin", "HOME=/home/user" }, new File("/tmp"), 0), Arguments.of("echo Hello", null, null, 5));
    }

    /**
     * Executes a simple command that prints {@value #MESSAGE} to standard output.
     * The command is built according to the underlying OS.
     */
    @Test
    void exec_withCommandEnvAndDirectory_returnsExpectedOutput() {
        // GIVEN
        String os = System.getProperty("os.name").toLowerCase();
        String command;
        // inherit current environment
        String[] env = null;
        File workingDir = new File(System.getProperty("java.io.tmpdir"));
        if (os.contains("win")) {
            // Windows: use cmd /c echo ...
            command = "cmd.exe /c echo " + MESSAGE;
        } else {
            // Unix‑like: use sh -c echo ...
            command = "sh -c \"echo " + MESSAGE + "\"";
        }
        // WHEN
        int exitCode = executor.exec(command, env, workingDir);
        // THEN
        assertTrue(exitCode >= 0, "Exit code should be non‑negative");
        assertEquals(MESSAGE, executor.getOut().trim(), "The captured stdout should match the echoed message");
        // Errors are not expected for a successful echo command
        assertTrue(executor.getError().isEmpty(), "Standard error should be empty for a successful command");
    }

    /* --------------------------------------------------------------
     *  Successful execution path – two parameterised test cases
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: command={0}, envp={1}, dir={2}, expectedExit={3}")
    @MethodSource("successfulExecutionProvider")
    void exec_successfulExecution_returnsRunItResult(String command, String[] envp, File dir, int expectedExit) throws Exception {
        // ----------------------------------------------------------
        // GIVEN – a mocked Runtime that returns a mocked Process.
        // ----------------------------------------------------------
        Process mockedProcess = Mockito.mock(Process.class);
        // runIt (indirectly) uses Process.waitFor() to obtain the exit code.
        Mockito.when(mockedProcess.waitFor()).thenReturn(expectedExit);
        // Provide empty streams so that getOut()/getError() do not fail.
        Mockito.when(mockedProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(mockedProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Runtime mockedRuntime = Mockito.mock(Runtime.class);
        Mockito.when(mockedRuntime.exec(eq(command), eq(envp), eq(dir))).thenReturn(mockedProcess);
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            // ------------------------------------------------------
            // WHEN – invoke the method under test.
            // ------------------------------------------------------
            int actual = executor.exec(command, envp, dir);
            // ------------------------------------------------------
            // THEN – the exit code propagated from runIt is returned.
            // ------------------------------------------------------
            assertEquals(expectedExit, actual, "exec should return the value produced by runIt (Process.waitFor())");
            // No output or error is expected for these synthetic processes.
            assertTrue(executor.getOut().isEmpty(), "Standard output should be empty");
            assertTrue(executor.getError().isEmpty(), "Standard error should be empty");
        }
    }

    /* --------------------------------------------------------------
     *  Null command – Runtime.exec throws NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void exec_nullCommand_returnsMinusOne() {
        // GIVEN
        Runtime mockedRuntime = Mockito.mock(Runtime.class);
        Mockito.when(mockedRuntime.exec(isNull(), isNull(), isNull())).thenThrow(new NullPointerException("command is null"));
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            // WHEN
            int result = executor.exec(null, null, null);
            // THEN
            assertEquals(-1, result, "When command is null, exec should return -1");
        }
    }

    /* --------------------------------------------------------------
     *  Empty command – Runtime.exec throws IOException
     * -------------------------------------------------------------- */
    @Test
    void exec_emptyCommand_returnsMinusOne() {
        // GIVEN
        Runtime mockedRuntime = Mockito.mock(Runtime.class);
        Mockito.when(mockedRuntime.exec(eq(""), isNull(), isNull())).thenThrow(new IOException("Cannot run program \"\""));
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            // WHEN
            int result = executor.exec("", null, null);
            // THEN
            assertEquals(-1, result, "Empty command should cause exec to return -1");
        }
    }

    /* --------------------------------------------------------------
     *  Invalid directory – Runtime.exec throws IOException
     * -------------------------------------------------------------- */
    @Test
    void exec_invalidDirectory_returnsMinusOne() {
        // GIVEN
        String command = "ls";
        // exists but not a directory
        File invalidDir = new File("/etc/passwd");
        Runtime mockedRuntime = Mockito.mock(Runtime.class);
        Mockito.when(mockedRuntime.exec(eq(command), isNull(), eq(invalidDir))).thenThrow(new IOException("Invalid working directory"));
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            // WHEN
            int result = executor.exec(command, null, invalidDir);
            // THEN
            assertEquals(-1, result, "Invalid working directory should cause exec to return -1");
        }
    }

    /* --------------------------------------------------------------
     *  Non‑existent directory – Runtime.exec throws IOException
     * -------------------------------------------------------------- */
    @Test
    void exec_nonexistentDirectory_returnsMinusOne() {
        // GIVEN
        String command = "ls";
        File nonExistent = new File("/path/does/not/exist");
        Runtime mockedRuntime = Mockito.mock(Runtime.class);
        Mockito.when(mockedRuntime.exec(eq(command), isNull(), eq(nonExistent))).thenThrow(new IOException("Directory does not exist"));
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            // WHEN
            int result = executor.exec(command, null, nonExistent);
            // THEN
            assertEquals(-1, result, "Non‑existent directory should cause exec to return -1");
        }
    }

    /* --------------------------------------------------------------
     *  Security manager – Runtime.exec throws SecurityException
     * -------------------------------------------------------------- */
    @Test
    void exec_securityManagerDeniesExecution_returnsMinusOne() {
        // GIVEN
        String command = "ls";
        Runtime mockedRuntime = Mockito.mock(Runtime.class);
        Mockito.when(mockedRuntime.exec(eq(command), isNull(), isNull())).thenThrow(new SecurityException("Execution denied"));
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            // WHEN
            int result = executor.exec(command, null, null);
            // THEN
            assertEquals(-1, result, "SecurityException should cause exec to return -1");
        }
    }

    /* --------------------------------------------------------------
     *  InterruptedException while waiting for the process – runIt throws
     * -------------------------------------------------------------- */
    @Test
    void exec_runItInterruptedException_returnsMinusOne() throws Exception {
        // GIVEN
        Process mockedProcess = Mockito.mock(Process.class);
        // Simulate waitFor throwing InterruptedException
        Mockito.when(mockedProcess.waitFor()).thenThrow(new InterruptedException("interrupted"));
        Mockito.when(mockedProcess.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(mockedProcess.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Runtime mockedRuntime = Mockito.mock(Runtime.class);
        Mockito.when(mockedRuntime.exec(eq("sleep 1"), isNull(), isNull())).thenReturn(mockedProcess);
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            // WHEN
            int result = executor.exec("sleep 1", null, null);
            // THEN
            assertEquals(-1, result, "When runIt (waitFor) throws InterruptedException, exec should return -1");
        }
    }
}
