package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.IOException;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

// retained from the template; not used directly
@ExtendWith(MockitoExtension.class)
class Executor_exec_4_Test {

    private Executor executor;

    @BeforeEach
    void setupBeforeEach() {
        executor = new Executor();
        executor = new Executor();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference; helps the GC in long test suites.
        executor = null;
        executor = null;
    }

    /**
     * Provides a stream of arguments for {@link #testExecSuccessful}.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>command array</li>
     *   <li>environment array (may be {@code null})</li>
     *   <li>the exit code that the mocked {@link Process} should return</li>
     * </ul>
     */
    private static Stream<Arguments> successfulExecutionProvider() {
        // case 1 – simple echo, envp null, exit 0
        String[] case1Cmd = { "echo", "hello" };
        // case 2 – java -version with one env variable, exit 5
        String[] case2Cmd = { "java", "-version" };
        String[] case2Env = { "JAVA_HOME=/opt/java" };
        // case 3 – empty command array, exit 0
        String[] case3Cmd = {};
        // case 4 – envp contains an empty string, exit 0
        String[] case4Cmd = { "echo", "test" };
        String[] case4Env = { "" };
        // case 5 – large command array (100 args), exit 0
        String[] case5Cmd = new String[101];
        case5Cmd[0] = "cmd";
        for (int i = 1; i < case5Cmd.length; i++) {
            case5Cmd[i] = "arg" + (i - 1);
        }
        // case 6 – empty envp with non‑empty cmdarray, exit 0
        String[] case6Cmd = { "echo", "hi" };
        String[] case6Env = new String[0];
        // case 7 – multiple environment variables, exit 0
        String[] case7Cmd = { "env" };
        String[] case7Env = { "A=1", "B=2", "C=3" };
        return Stream.of(Arguments.of(case1Cmd, null, 0), Arguments.of(case2Cmd, case2Env, 5), Arguments.of(case3Cmd, new String[0], 0), Arguments.of(case4Cmd, case4Env, 0), Arguments.of(case5Cmd, null, 0), Arguments.of(case6Cmd, case6Env, 0), Arguments.of(case7Cmd, case7Env, 0));
    }

    /**
     * Verify that a valid command executed via {@code exec(String[], String[])} returns
     * a zero exit code and captures the expected standard output.
     */
    @Test
    void testExecValidCommand() {
        // Build a cross‑platform echo command.
        String os = System.getProperty("os.name").toLowerCase();
        String[] cmd;
        if (os.contains("windows")) {
            // Windows: cmd.exe /c echo hello
            cmd = new String[] { "cmd.exe", "/c", "echo", "hello" };
        } else {
            // Unix‑like: /bin/sh -c "echo hello"
            cmd = new String[] { "/bin/sh", "-c", "echo hello" };
        }
        int exitCode = executor.exec(cmd, null);
        assertEquals(0, exitCode, "Process should exit with code 0");
        assertEquals("hello", executor.getOut().trim(), "Standard output should contain the echoed text");
    }

    /**
     * Verify that an invalid command results in the error handling path,
     * returning {@code -1} and leaving the captured output empty.
     */
    @Test
    void testExecInvalidCommand() {
        // An obviously invalid command – the OS will fail to locate it.
        String[] cmd = { "nonexistentcommand_xyz" };
        int exitCode = executor.exec(cmd, null);
        assertEquals(-1, exitCode, "When execution fails, method should return -1");
        // The class clears its output buffers on error; we only assert that nothing useful was captured.
        assertTrue(executor.getOut() == null || executor.getOut().trim().isEmpty(), "No standard output should be captured on failure");
    }

    /* --------------------------------------------------------------
     *  Successful execution paths (runIt returns the mocked exit code)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => cmd={0}, envp={1}, expected={2}")
    @MethodSource("successfulExecutionProvider")
    void testExecSuccessful(String[] cmdarray, String[] envp, int expectedExit) {
        // Mock a Process that simply returns the expected exit code.
        Process mockProcess = Mockito.mock(Process.class);
        try {
            Mockito.when(mockProcess.waitFor()).thenReturn(expectedExit);
        } catch (InterruptedException e) {
            // This will never happen because we are stubbing the method.
        }
        // Mock Runtime.exec to return the above Process.
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime runtime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            // Match exact arguments (null is allowed for envp).
            if (envp == null) {
                Mockito.when(runtime.exec(eq(cmdarray), isNull())).thenReturn(mockProcess);
            } else {
                Mockito.when(runtime.exec(eq(cmdarray), eq(envp))).thenReturn(mockProcess);
            }
            int exitCode = executor.exec(cmdarray, envp);
            assertEquals(expectedExit, exitCode, "exec should return the exit code produced by runIt (mocked waitFor)");
        }
    }

    /* --------------------------------------------------------------
     *  Error handling – Runtime.exec throws IOException
     * -------------------------------------------------------------- */
    @Test
    void testExecRuntimeThrowsIOException() {
        String[] cmd = { "invalidCommand" };
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime runtime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            Mockito.when(runtime.exec(eq(cmd), any())).thenThrow(new IOException("simulated I/O error"));
            int exit = executor.exec(cmd, null);
            assertEquals(-1, exit, "When Runtime.exec throws IOException, exec should return -1");
        }
    }

    /* --------------------------------------------------------------
     *  Error handling – Runtime.exec throws IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void testExecRuntimeThrowsIllegalArgumentException() {
        String[] emptyCmd = {};
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime runtime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            Mockito.when(runtime.exec(eq(emptyCmd), any())).thenThrow(new IllegalArgumentException("empty command array"));
            int exit = executor.exec(emptyCmd, null);
            assertEquals(-1, exit, "When Runtime.exec throws IllegalArgumentException, exec should return -1");
        }
    }

    /* --------------------------------------------------------------
     *  Error handling – Null cmdarray argument (causes NPE)
     * -------------------------------------------------------------- */
    @Test
    void testExecNullCmdArray() {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime runtime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            Mockito.when(runtime.exec(isNull(), any())).thenThrow(new NullPointerException("cmdarray is null"));
            int exit = executor.exec(null, null);
            assertEquals(-1, exit, "When cmdarray is null, exec should return -1");
        }
    }

    /* --------------------------------------------------------------
     *  Error handling – runIt throws InterruptedException (via Process.waitFor)
     * -------------------------------------------------------------- */
    @Test
    void testExecRunItInterruptedException() throws Exception {
        String[] cmd = { "sleep", "1" };
        Process mockProcess = Mockito.mock(Process.class);
        // Simulate waitFor throwing InterruptedException
        Mockito.when(mockProcess.waitFor()).thenThrow(new InterruptedException("interrupted"));
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime runtime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            Mockito.when(runtime.exec(eq(cmd), any())).thenReturn(mockProcess);
            int exit = executor.exec(cmd, null);
            assertEquals(-1, exit, "When runIt (waitFor) throws InterruptedException, exec should return -1");
        }
    }

    /* --------------------------------------------------------------
     *  Edge case – cmdarray contains a null element
     * -------------------------------------------------------------- */
    @Test
    void testExecCmdArrayWithNullElement() {
        String[] cmd = { "echo", null };
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime runtime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            // Runtime.exec will throw NPE when it encounters a null element.
            Mockito.when(runtime.exec(eq(cmd), any())).thenThrow(new NullPointerException("null element in cmdarray"));
            int exit = executor.exec(cmd, null);
            assertEquals(-1, exit, "Null element inside cmdarray should cause exec to return -1");
        }
    }
}
