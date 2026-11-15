package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.io.IOException;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class Executor_exec_Test {

    private static final String THIS_IS_HANDY = "This is handy";

    private static final String ECHO_THIS_IS_HANDY = "echo " + THIS_IS_HANDY;

    private Executor executor;

    // value is irrelevant – mocked
    private static final String DUMMY_COMMAND = "dummy";

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test so state (out / err) is clean.
        executor = new Executor();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – helps the GC in long test runs.
        executor = null;
    }

    private static Stream<Arguments> successfulExecutionProvider() {
        return Stream.of(// command string, mocked exit code
        Arguments.of("echo Hello World", 5), Arguments.of("true", 0), Arguments.of("sh -c 'exit -2'", -2));
    }

    private static Stream<Arguments> runtimeExceptionProvider() {
        return Stream.of(Arguments.of("invalidCommand", new IOException("simulated IO failure")), Arguments.of("restrictedCommand", new SecurityException("security violation")), Arguments.of("", new IllegalArgumentException("empty command")), Arguments.of("unsupported", new UnsupportedOperationException("unsupported")), Arguments.of(null, new NullPointerException("null command")));
    }

    /**
     * Verifies that {@link Executor#exec(String)} runs a simple command,
     * captures its standard output and returns a non‑negative exit code.
     *
     * The test adapts to the underlying OS:
     * <ul>
     *   <li>Windows – uses {@code cmd.exe /c echo …}</li>
     *   <li>Unix‑like – uses {@code echo …}</li>
     * </ul>
     */
    @Test
    void testExecStringCommand_capturesOutput() {
        // GIVEN – an OS‑specific command that prints a known string
        String os = System.getProperty("os.name");
        String command;
        if (os != null && os.toLowerCase().contains("windows")) {
            command = "cmd.exe /c " + ECHO_THIS_IS_HANDY;
        } else {
            command = ECHO_THIS_IS_HANDY;
        }
        // WHEN – the command is executed
        int exitCode = executor.exec(command);
        // THEN – the exit code should be zero (or positive) and the output should match
        assertTrue(exitCode >= 0, () -> "Expected a non‑negative exit code, but got " + exitCode);
        assertEquals(THIS_IS_HANDY, executor.getOut().trim(), "The captured stdout does not match the expected text.");
    }

    /**
     * Verifies that a failing command returns {@code -1} and that the error
     * stream is populated. The command {@code boguscommand123} is expected to
     * be unavailable on any platform.
     */
    @Test
    void testExecStringCommand_invalidCommand_returnsMinusOne() {
        // GIVEN – a command that does not exist
        String command = "boguscommand123";
        // WHEN
        int exitCode = executor.exec(command);
        // THEN – the method should handle the exception and return -1
        assertEquals(-1, exitCode, "Executor should return -1 when an exception occurs during exec.");
        // The error message may be platform‑specific, but it should not be empty.
        assertNotNull(executor.getError(), "Error output should be non‑null after a failed exec.");
        assertFalse(executor.getError().trim().isEmpty(), "Error output should contain some text after a failed exec.");
    }

    @ParameterizedTest(name = "[{index}] exec(\"{0}\") should return {1}")
    @MethodSource("successfulExecutionProvider")
    void testExecString_successfulExecution_returnsMockedExitCode(String command, int mockedExit) throws Exception {
        // ---------------------------------------------------------
        // GIVEN – mock Runtime.exec to return a mock Process and stub runIt
        // ---------------------------------------------------------
        Process mockProcess = mock(Process.class);
        Runtime runtimeMock = mock(Runtime.class);
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(runtimeMock);
            when(runtimeMock.exec(command)).thenReturn(mockProcess);
            // Spy the executor so we can stub the private runIt method
            Executor spy = Mockito.spy(executor);
            // runIt is private – Mockito can still stub it on a spy
            doReturn(mockedExit).when(spy).runIt(any(Process.class));
            // ---------------------------------------------------------
            // WHEN
            // ---------------------------------------------------------
            int exitCode = spy.exec(command);
            // ---------------------------------------------------------
            // THEN – exec must return the value supplied by runIt
            // ---------------------------------------------------------
            assertEquals(mockedExit, exitCode, () -> "Expected exec to return the mocked exit code " + mockedExit);
        }
    }

    @ParameterizedTest(name = "[{index}] exec throws {1.class.simpleName} → returns -1")
    @MethodSource("runtimeExceptionProvider")
    void testExecString_runtimeExecThrowsException_returnsMinusOne(String command, Exception toThrow) throws Exception {
        // ---------------------------------------------------------
        // GIVEN – mock Runtime.exec to throw the supplied exception
        // ---------------------------------------------------------
        Runtime runtimeMock = mock(Runtime.class);
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(runtimeMock);
            when(runtimeMock.exec(command)).thenThrow(toThrow);
            // Spy is not needed – runIt will never be called because exec fails early
            Executor spy = Mockito.spy(executor);
            // ---------------------------------------------------------
            // WHEN
            // ---------------------------------------------------------
            int exitCode = spy.exec(command);
            // ---------------------------------------------------------
            // THEN – method must handle the exception and return -1
            // ---------------------------------------------------------
            assertEquals(-1, exitCode, () -> "When Runtime.exec throws " + toThrow.getClass().getSimpleName() + " exec should return -1");
        }
    }

    @Test
    void testExecString_runItThrowsInterruptedException_returnsMinusOne() throws Exception {
        // ---------------------------------------------------------
        // GIVEN – Runtime.exec succeeds, but runIt throws InterruptedException
        // ---------------------------------------------------------
        Process mockProcess = mock(Process.class);
        Runtime runtimeMock = mock(Runtime.class);
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(runtimeMock);
            when(runtimeMock.exec(DUMMY_COMMAND)).thenReturn(mockProcess);
            Executor spy = Mockito.spy(executor);
            doThrow(new InterruptedException("interrupted")).when(spy).runIt(any(Process.class));
            // ---------------------------------------------------------
            // WHEN
            // ---------------------------------------------------------
            int exitCode = spy.exec(DUMMY_COMMAND);
            // ---------------------------------------------------------
            // THEN – the catch‑all block should convert the exception to -1
            // ---------------------------------------------------------
            assertEquals(-1, exitCode, "When runIt throws InterruptedException exec should return -1");
        }
    }

    @Test
    void testExecString_runItThrowsRuntimeException_returnsMinusOne() throws Exception {
        // ---------------------------------------------------------
        // GIVEN – Runtime.exec succeeds, but runIt throws a RuntimeException
        // ---------------------------------------------------------
        Process mockProcess = mock(Process.class);
        Runtime runtimeMock = mock(Runtime.class);
        try (MockedStatic<Runtime> runtimeStatic = Mockito.mockStatic(Runtime.class)) {
            runtimeStatic.when(Runtime::getRuntime).thenReturn(runtimeMock);
            when(runtimeMock.exec(DUMMY_COMMAND)).thenReturn(mockProcess);
            Executor spy = Mockito.spy(executor);
            doThrow(new IllegalStateException("illegal state")).when(spy).runIt(any(Process.class));
            // ---------------------------------------------------------
            // WHEN
            // ---------------------------------------------------------
            int exitCode = spy.exec(DUMMY_COMMAND);
            // ---------------------------------------------------------
            // THEN – the generic catch block should return -1
            // ---------------------------------------------------------
            assertEquals(-1, exitCode, "When runIt throws a RuntimeException exec should return -1");
        }
    }
}
