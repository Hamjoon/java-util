package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

class Executor_exec_3_Test {

    private Executor executor;

    /**
     * Simple holder for the arguments used in the parameterised success tests.
     */
    private static class ArgumentsHolder {

        final String command;

        final String[] envp;

        final int expectedExit;

        ArgumentsHolder(String command, String[] envp, int expectedExit) {
            this.command = command;
            this.envp = envp;
            this.expectedExit = expectedExit;
        }

        @Override
        public String toString() {
            return "command='" + command + '\'' + ", envp=" + (envp == null ? "null" : java.util.Arrays.toString(envp)) + ", expectedExit=" + expectedExit;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        executor = new Executor();
    }

    /**
     * Creates a dummy {@link Process} that returns the supplied exit code
     *  when {@code waitFor()} is called.  All other methods are stubbed with
     *  harmless defaults.
     */
    private static Process mockProcessReturning(int exitCode) throws InterruptedException {
        Process p = mock(Process.class);
        when(p.waitFor()).thenReturn(exitCode);
        // The real implementation may read streams – provide empty streams.
        when(p.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(p.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(p.getOutputStream()).thenReturn(mock(java.io.OutputStream.class));
        return p;
    }

    /**
     * Supplies a {@link MockedStatic} for {@link Runtime} that returns the
     *  given {@link Process} when {@code exec(String, String[])} is invoked.
     */
    private static MockedStatic<Runtime> mockRuntimeExec(Process proc) {
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(anyString(), any(String[].class))).thenReturn(proc);
        } catch (IOException e) {
            // This branch will never be hit because we are mocking.
            throw new RuntimeException(e);
        }
        return mockedRuntime;
    }

    /**
     * Parameter source for successful executions where we expect the
     *  exit code returned by {@code Process.waitFor()} to be forwarded
     *  unchanged by {@code Executor.exec}.
     */
    private static Stream<ArgumentsHolder> successfulCases() {
        return Stream.of(// Simple echo – expect 0
        new ArgumentsHolder("echo Hello", new String[] { "PATH=/usr/bin" }, 0), // ls -l with null env – expect 42 (mocked)
        new ArgumentsHolder("ls -l", null, 42), // Empty envp array – expect 0
        new ArgumentsHolder("echo Hello", new String[] {}, 0), // Multiple environment variables – expect 0
        new ArgumentsHolder("pwd", new String[] { "USER=test", "HOME=/home/test" }, 0), // Large envp array (100 entries) – expect 0
        new ArgumentsHolder("echo LargeEnv", generateLargeEnv(), 0));
    }

    /**
     * Helper to generate a 100‑element envp array for the large‑env test.
     */
    private static String[] generateLargeEnv() {
        String[] env = new String[100];
        for (int i = 0; i < 100; i++) {
            env[i] = "VAR" + i + "=" + i;
        }
        return env;
    }

    /**
     * Verify that a valid command returns a non‑negative exit code.
     * The implementation of {@code runIt} is not visible, but the
     * typical contract of an executor is to return the process exit
     * value (0 for success). Using a simple, platform‑independent
     * command – {@code echo} – ensures the test works on Windows,
     * Linux and macOS.
     */
    @Test
    void testExecSuccess() {
        // "echo" works on all major OSes; on Windows it is a built‑in
        // command of cmd.exe, but Runtime.exec will invoke it via
        // the system shell automatically.
        String command = "echo HelloWorld";
        // inherit current environment
        String[] env = null;
        int result = executor.exec(command, env);
        // Successful execution should yield a non‑negative exit code.
        // Most implementations return 0 for success.
        assertTrue(result >= 0, "Expected a non‑negative exit code, got: " + result);
    }

    /**
     * Verify that when the underlying {@code Runtime.exec} throws an
     * exception (e.g., because the command does not exist), the method
     * returns {@code -1}.
     */
    @Test
    void testExecFailure() {
        // Use a deliberately bogus command that is guaranteed not to exist.
        String command = "this_command_does_not_exist_12345";
        String[] env = null;
        int result = executor.exec(command, env);
        assertEquals(-1, result, "Expected -1 when execution fails");
    }

    @ParameterizedTest(name = "[{index}] command=''{0}'', envp={1}, expectedExit={2}")
    @MethodSource("successfulCases")
    void testExecSuccessForwardedExitCode(ArgumentsHolder holder) throws Exception {
        Process mockedProcess = mockProcessReturning(holder.expectedExit);
        try (MockedStatic<Runtime> mockedRuntime = mockRuntimeExec(mockedProcess)) {
            int result = executor.exec(holder.command, holder.envp);
            assertEquals(holder.expectedExit, result, "Executor should forward the exit code returned by runIt");
        }
    }

    @Test
    void testExecIOExceptionReturnsMinusOne() {
        // Simulate a command that does not exist – Runtime.exec throws IOException
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(anyString(), any(String[].class))).thenThrow(new IOException("cmd not found"));
        } catch (IOException e) {
            // unreachable – we are configuring the mock
        }
        int result = executor.exec("invalid command", new String[] { "INVALID=TRUE" });
        assertEquals(-1, result, "When Runtime.exec throws IOException, exec should return -1");
        mockedRuntime.close();
    }

    @Test
    void testExecNullCommandReturnsMinusOne() {
        // Passing null as the command triggers NullPointerException inside Runtime.exec
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(isNull(), any(String[].class))).thenThrow(new NullPointerException("null command"));
        } catch (IOException e) {
            // unreachable
        }
        int result = executor.exec(null, new String[] {});
        assertEquals(-1, result, "Null command should result in -1");
        mockedRuntime.close();
    }

    @Test
    void testExecSecurityExceptionReturnsMinusOne() {
        // Simulate a SecurityException (e.g., insufficient permissions)
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(anyString(), any(String[].class))).thenThrow(new SecurityException("no permission"));
        } catch (IOException e) {
            // unreachable
        }
        int result = executor.exec("echo", new String[] { "BAD_ENV" });
        assertEquals(-1, result, "SecurityException should cause exec to return -1");
        mockedRuntime.close();
    }

    @Test
    void testExecIllegalArgumentExceptionReturnsMinusOne() {
        // Empty command string leads to IllegalArgumentException
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(eq(""), any(String[].class))).thenThrow(new IllegalArgumentException("empty"));
        } catch (IOException e) {
            // unreachable
        }
        int result = executor.exec("", null);
        assertEquals(-1, result, "Empty command should result in -1");
        mockedRuntime.close();
    }

    @Test
    void testExecWhitespaceOnlyCommandReturnsMinusOne() {
        // Command consisting only of whitespace – IllegalArgumentException
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(eq("   "), any(String[].class))).thenThrow(new IllegalArgumentException("whitespace"));
        } catch (IOException e) {
            // unreachable
        }
        int result = executor.exec("   ", new String[] {});
        assertEquals(-1, result, "Whitespace‑only command should result in -1");
        mockedRuntime.close();
    }

    @Test
    void testExecNullElementInEnvpReturnsMinusOne() {
        // envp containing a null element triggers NullPointerException
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(anyString(), argThat(arr -> arr != null && arr[1] == null))).thenThrow(new NullPointerException("null env element"));
        } catch (IOException e) {
            // unreachable
        }
        String[] env = new String[] { "PATH=/bin", null };
        int result = executor.exec("echo", env);
        assertEquals(-1, result, "Null element inside envp should cause -1");
        mockedRuntime.close();
    }

    @Test
    void testExecMalformedEnvEntryReturnsMinusOne() {
        // env entry without '=' causes IllegalArgumentException
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(anyString(), argThat(arr -> arr != null && arr[1].equals("BADENV")))).thenThrow(new IllegalArgumentException("malformed env"));
        } catch (IOException e) {
            // unreachable
        }
        String[] env = new String[] { "PATH=/bin", "BADENV" };
        int result = executor.exec("echo Hello", env);
        assertEquals(-1, result, "Malformed env entry should result in -1");
        mockedRuntime.close();
    }

    @Test
    void testExecEmptyEnvEntryReturnsMinusOne() {
        // Empty string in envp array – IllegalArgumentException
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(anyString(), argThat(arr -> arr != null && arr[0].isEmpty()))).thenThrow(new IllegalArgumentException("empty env entry"));
        } catch (IOException e) {
            // unreachable
        }
        String[] env = new String[] { "" };
        int result = executor.exec("echo Hello", env);
        assertEquals(-1, result, "Empty env entry should result in -1");
        mockedRuntime.close();
    }

    @Test
    void testExecDirectoryPathReturnsMinusOne() {
        // Supplying a directory path as the command – IOException
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(eq("/tmp"), any(String[].class))).thenThrow(new IOException("is a directory"));
        } catch (IOException e) {
            // unreachable
        }
        int result = executor.exec("/tmp", null);
        assertEquals(-1, result, "Command that is a directory should return -1");
        mockedRuntime.close();
    }

    @Test
    void testExecNullCharInCommandReturnsMinusOne() {
        // Command containing a null character – IllegalArgumentException
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(eq("echo\0Hello"), any(String[].class))).thenThrow(new IllegalArgumentException("null char"));
        } catch (IOException e) {
            // unreachable
        }
        int result = executor.exec("echo\0Hello", null);
        assertEquals(-1, result, "Command with null character should return -1");
        mockedRuntime.close();
    }

    @Test
    void testExecNonexistentCommandWithNullEnvReturnsMinusOne() {
        // Non‑existent command with null envp – IOException
        MockedStatic<Runtime> mockedRuntime = Mockito.mockStatic(Runtime.class);
        Runtime runtimeMock = mock(Runtime.class);
        mockedRuntime.when(Runtime::getRuntime).thenReturn(runtimeMock);
        try {
            when(runtimeMock.exec(eq("nonexistentcommand"), isNull())).thenThrow(new IOException("not found"));
        } catch (IOException e) {
            // unreachable
        }
        int result = executor.exec("nonexistentcommand", null);
        assertEquals(-1, result, "Non‑existent command with null envp should return -1");
        mockedRuntime.close();
    }
}
