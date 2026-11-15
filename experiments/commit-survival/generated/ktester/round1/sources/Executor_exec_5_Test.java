package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class Executor_exec_5_Test {

    /**
     * Instance of the class under test.
     */
    private Executor executor;

    /**
     * Temporary directory used for the *dir* argument – created once for all tests.
     */
    private static File tempDir;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a temporary directory that can be passed to exec(..., ..., File)
        tempDir = new File(System.getProperty("java.io.tmpdir"));
        // Ensure the directory actually exists (it always does, but we guard against odd environments)
        assertTrue(tempDir.isDirectory(), "Temporary directory must exist for tests");
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for each test – Executor has no state, but this follows the pattern.
        executor = new Executor();
    }

    private static Stream<Arguments> successfulCases() {
        return Stream.of(// simple command, no env, no dir
        Arguments.of("echo hello", null, null, 0), // command with env and explicit dir
        Arguments.of("java -version", new String[] { "PATH=/usr/bin", "LANG=en_US.UTF-8" }, new File("/tmp"), 5), // empty env array and current directory
        Arguments.of("cmd /c dir", new String[0], new File("."), 42), // command with multiple spaces
        Arguments.of("echo   spaced   out", new String[0], null, 0), // env with a single variable
        Arguments.of("echo $HOME", new String[] { "HOME=/home/test" }, null, 7), // valid temporary directory obtained at runtime
        Arguments.of("echo hello", null, new File(System.getProperty("java.io.tmpdir")), 3));
    }

    private static Stream<Arguments> runtimeExceptionCases() {
        return Stream.of(// null command → NullPointerException
        Arguments.of(null, null, null, NullPointerException.class), // empty command → IOException
        Arguments.of("", null, null, IOException.class), // envp contains null element → NullPointerException
        Arguments.of("echo test", new String[] { null }, null, NullPointerException.class), // non‑existent directory → IOException
        Arguments.of("echo test", null, new File("/nonexistent/path"), IOException.class), // dir points to a regular file → IOException
        Arguments.of("echo test", null, new File("src/main/java/com/cedarsoftware/util/Executor.java"), IOException.class), // command not found → IOException
        Arguments.of("nonexistentcommand", null, null, IOException.class), // directory without permission → IOException (could also be SecurityException)
        Arguments.of("echo test", null, new File("/root"), IOException.class), // SecurityManager denies execution → SecurityException
        Arguments.of("echo test", null, null, SecurityException.class));
    }

    /**
     * Verifies that when {@link Runtime#exec(String, String[], File)} throws an exception,
     * the {@code exec} method catches it, logs the error and returns {@code -1}.
     *
     * This test uses Mockito's ability to mock static methods (requires mockito‑inline on the
     * classpath) to replace the call to {@code Runtime.getRuntime()} with a mock that throws
     * an {@link IOException}.
     */
    @Test
    void exec_returnsMinusOne_whenRuntimeExecThrowsException() {
        // Arrange
        String command = "someCommand";
        String[] env = new String[] { "VAR=VALUE" };
        // Mock the static Runtime class
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime mockedRuntime = Mockito.mock(Runtime.class);
            // When Runtime.getRuntime() is called, return our mocked Runtime instance
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            // When exec(...) is invoked on the mocked Runtime, throw an IOException
            Mockito.when(mockedRuntime.exec(command, env, tempDir)).thenThrow(new IOException("simulated I/O failure"));
            // Act
            int result = executor.exec(command, env, tempDir);
            // Assert
            assertEquals(-1, result, "Executor.exec should return -1 when Runtime.exec throws an exception");
        }
    }

    /**
     * Executes the method with a variety of valid arguments.
     * The mocked {@link Process#waitFor()} returns the expected exit code,
     * which is then returned by {@code exec}.
     */
    @ParameterizedTest(name = "[{index}] command=''{0}'', envp={1}, dir={2} → expected={3}")
    @MethodSource("successfulCases")
    void exec_returnsExpectedValue_onSuccessfulExecution(String command, String[] envp, File dir, int expectedExit) {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime mockedRuntime = Mockito.mock(Runtime.class);
            Process mockedProcess = Mockito.mock(Process.class);
            // Runtime.getRuntime() → mockedRuntime
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            // Runtime.exec(...) → mockedProcess
            Mockito.when(mockedRuntime.exec(eq(command), eq(envp), eq(dir))).thenReturn(mockedProcess);
            // runIt() internally calls Process.waitFor()
            Mockito.when(mockedProcess.waitFor()).thenReturn(expectedExit);
            int result = executor.exec(command, envp, dir);
            assertEquals(expectedExit, result, "Executor.exec should return the exit code produced by runIt");
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }

    /**
     * Verifies that {@code exec} returns {@code -1} when {@link Runtime#exec}
     * throws any exception (e.g., NPE, IOException, SecurityException).
     */
    @ParameterizedTest(name = "[{index}] command=''{0}'', envp={1}, dir={2} → throws {3}")
    @MethodSource("runtimeExceptionCases")
    void exec_returnsMinusOne_whenRuntimeExecThrows(String command, String[] envp, File dir, Class<? extends Exception> exceptionClass) {
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime mockedRuntime = Mockito.mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            // Configure Runtime.exec to throw the supplied exception type
            Exception toThrow = exceptionClass.getDeclaredConstructor(String.class).newInstance("simulated exception");
            Mockito.when(mockedRuntime.exec(eq(command), eq(envp), eq(dir))).thenThrow(toThrow);
            int result = executor.exec(command, envp, dir);
            assertEquals(-1, result, "Executor.exec should return -1 when Runtime.exec throws " + exceptionClass.getSimpleName());
        } catch (Exception e) {
            fail("Unexpected setup exception: " + e);
        }
    }

    /**
     * Simulates a situation where {@code Runtime.exec} succeeds but the
     * private {@code runIt} method throws {@link InterruptedException}
     * (by having {@link Process#waitFor()} throw it).  The expected result
     * is {@code -1}.
     */
    @Test
    void exec_returnsMinusOne_whenRunItThrowsInterruptedException() {
        String command = "echo test";
        String[] env = null;
        File dir = null;
        try (MockedStatic<Runtime> runtimeMock = Mockito.mockStatic(Runtime.class)) {
            Runtime mockedRuntime = Mockito.mock(Runtime.class);
            Process mockedProcess = Mockito.mock(Process.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockedRuntime);
            Mockito.when(mockedRuntime.exec(eq(command), eq(env), eq(dir))).thenReturn(mockedProcess);
            // Simulate waitFor throwing InterruptedException
            Mockito.when(mockedProcess.waitFor()).thenThrow(new InterruptedException("simulated"));
            int result = executor.exec(command, env, dir);
            assertEquals(-1, result, "Executor.exec should return -1 when runIt (waitFor) throws InterruptedException");
        } catch (Exception e) {
            fail("Unexpected exception during test setup: " + e);
        }
    }
}
