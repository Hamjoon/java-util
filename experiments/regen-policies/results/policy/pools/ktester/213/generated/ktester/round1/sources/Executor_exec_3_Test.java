package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Executor_exec_3_Test {

    /**
     * Mocked static access to {@link Runtime}.
     */
    private static MockedStatic<Runtime> runtimeMock;

    /**
     * Instance under test.
     */
    private Executor executor;

    @BeforeAll
    static void setupBeforeAll() {
        // Mock the static call Runtime.getRuntime()
        runtimeMock = mockStatic(Runtime.class);
    }

    @BeforeEach
    void setupBeforeEach() {
        executor = new Executor();
    }

    @AfterEach
    void teardownAfterEach() {
        executor = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static mock
        runtimeMock.close();
    }

    /**
     * Supplies the arguments for {@link #exec_variousScenarios(String, String[], int)}.
     *
     * @return a stream of {@link Arguments} (command, envp, expectedResult)
     */
    private static Stream<Arguments> provideExecTestCases() {
        return Stream.of(// exec_validCommand_noEnv
        Arguments.of("echo HelloWorld", null, 0), // exec_validCommand_emptyEnv
        Arguments.of("echo Test", new String[] {}, 0), // exec_validCommand_withEnv
        Arguments.of("cmd /c set MY_VAR", new String[] { "MY_VAR=ExecutorTest" }, 0), // exec_invalidCommand
        Arguments.of("nonexistent_command_12345", null, -1), // exec_nullCommand
        Arguments.of(null, null, -1), // exec_emptyCommandString
        Arguments.of("", null, -1), // exec_envpContainsNullEntry
        Arguments.of("echo NullEnv", new String[] { null, "PATH=/usr/bin" }, -1), // exec_largeEnvArray (30 entries)
        Arguments.of("echo LargeEnv", generateLargeEnv(), 0), // exec_whitespaceOnlyCommand
        Arguments.of("   ", null, -1), // exec_veryLongCommand (overly long string)
        Arguments.of(generateVeryLongCommand(), null, -1), // exec_securityException
        Arguments.of("echo SecureTest", null, -1), // exec_interruptedThread
        Arguments.of("echo InterruptTest", null, -1));
    }

    /**
     * Utility to check whether an env array contains a {@code null} element.
     */
    private static boolean containsNull(String[] env) {
        for (String s : env) {
            if (s == null)
                return true;
        }
        return false;
    }

    /**
     * Generates a 30‑element environment array for the large‑env test case.
     */
    private static String[] generateLargeEnv() {
        String[] env = new String[30];
        for (int i = 0; i < 30; i++) {
            env[i] = "VAR" + i + "=" + i;
        }
        return env;
    }

    /**
     * Generates an excessively long command string (several thousand characters).
     */
    private static String generateVeryLongCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append('a');
        for (int i = 0; i < 5000; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    /**
     * Verify that {@code exec(String, String[])} returns {@code -1} when
     * {@link Runtime#exec(String, String[])} throws an exception.
     */
    @Test
    void exec_shouldReturnMinusOneWhenRuntimeExecThrows() throws Exception {
        // Arrange
        String command = "someCommand";
        String[] env = new String[] { "VAR=VALUE" };
        // Create a mock Runtime that throws an IOException on exec(...)
        Runtime mockRuntime = mock(Runtime.class);
        when(mockRuntime.exec(eq(command), eq(env))).thenThrow(new IOException("forced failure"));
        // Make Runtime.getRuntime() return our mock Runtime
        runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
        // Act
        int result = executor.exec(command, env);
        // Assert
        assertEquals(-1, result, "exec should return -1 when Runtime.exec throws an exception");
    }

    /**
     * Parameterized test that drives all remaining scenarios described in the
     * JSON test matrix.  Each argument set contains the command, the environment
     * array (may be {@code null}), and the expected integer result.
     */
    @ParameterizedTest(name = "{index}: command=''{0}'', expected={2}")
    @MethodSource("provideExecTestCases")
    void exec_variousScenarios(String command, String[] envp, int expected) throws Exception {
        // --------------------------------------------------------------------
        // 1️⃣  Prepare the mock Runtime according to the scenario.
        // --------------------------------------------------------------------
        Runtime mockRuntime = mock(Runtime.class);
        runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
        // Helper to create a dummy Process that behaves like a successful command.
        Process successfulProcess = mock(Process.class);
        when(successfulProcess.waitFor()).thenReturn(0);
        // Empty streams – runIt will simply read them and finish.
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        when(successfulProcess.getInputStream()).thenReturn(emptyStream);
        when(successfulProcess.getErrorStream()).thenReturn(emptyStream);
        when(successfulProcess.getOutputStream()).thenReturn(emptyStream);
        // --------------------------------------------------------------------
        // 2️⃣  Decide which exception (if any) Runtime.exec should throw.
        // --------------------------------------------------------------------
        if (expected == -1) {
            // Determine the appropriate exception based on the input values.
            Exception toThrow;
            if (command == null) {
                toThrow = new NullPointerException("command is null");
            } else if (command.isEmpty() || command.trim().isEmpty()) {
                toThrow = new IllegalArgumentException("command is empty or whitespace");
            } else if (command.equals("nonexistent_command_12345")) {
                toThrow = new IOException("command not found");
            } else if (command.startsWith("aaaaaaaa")) {
                // very long command
                toThrow = new IOException("command too long");
            } else if (envp != null && containsNull(envp)) {
                toThrow = new NullPointerException("envp contains null entry");
            } else if (command.equals("echo SecureTest")) {
                toThrow = new SecurityException("execution denied by SecurityManager");
            } else if (command.equals("echo InterruptTest")) {
                // Simulate an interruption during waitFor()
                when(successfulProcess.waitFor()).thenThrow(new InterruptedException("interrupted"));
                // no Runtime.exec exception – the interruption occurs later
                toThrow = null;
            } else {
                // Fallback – treat any other case as a generic IOException.
                toThrow = new IOException("forced failure");
            }
            if (toThrow != null) {
                when(mockRuntime.exec(eq(command), eq(envp))).thenThrow(toThrow);
            } else {
                // No exception from Runtime.exec – return the process that will be
                // interrupted when runIt invokes waitFor().
                when(mockRuntime.exec(eq(command), eq(envp))).thenReturn(successfulProcess);
            }
        } else {
            // Expected success (exit code 0) – return the dummy successful process.
            when(mockRuntime.exec(eq(command), eq(envp))).thenReturn(successfulProcess);
        }
        // --------------------------------------------------------------------
        // 3️⃣  If the test case requires the current thread to be interrupted,
        //      do it now.  The Executor will catch the resulting
        //      {@link InterruptedException} and return -1.
        // --------------------------------------------------------------------
        if (command.equals("echo InterruptTest")) {
            Thread.currentThread().interrupt();
        }
        // --------------------------------------------------------------------
        // 4️⃣  Invoke the method under test.
        // --------------------------------------------------------------------
        int actual = executor.exec(command, envp);
        // --------------------------------------------------------------------
        // 5️⃣  Verify the result.
        // --------------------------------------------------------------------
        assertEquals(expected, actual, () -> String.format("exec('%s', %s) expected %d but got %d", command, envp == null ? "null" : java.util.Arrays.toString(envp), expected, actual));
        // Clear interrupt flag for subsequent tests.
        Thread.interrupted();
    }
}
