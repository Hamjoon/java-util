package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class Executor_exec_Test {

    @Test
    @DisplayName("exec(String) returns -1 and logs error when Runtime.exec throws an exception")
    void exec_ShouldReturnMinusOne_WhenRuntimeExecThrowsException() throws Exception {
        // Arrange
        Class<? extends Throwable> exClass = (Class<? extends Throwable>) Class.forName(exceptionClassName);
        Throwable toThrow = exClass.getConstructor(String.class).newInstance("simulated failure");
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime runtime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            // Runtime.exec(null) will be called with the literal string "null" in the test data,
            // so we need to handle that specially.
            String actualCommand = "null".equals(command) ? null : command;
            when(runtime.exec(actualCommand)).thenThrow(toThrow);
            Executor executor = new Executor();
            // Capture System.err output
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));
            // Act
            int result = executor.exec(actualCommand);
            // Restore original System.err
            System.setErr(originalErr);
            // Assert
            assertEquals(-1, result, "exec should return -1 when Runtime.exec throws " + exceptionClassName);
            String errOutput = errContent.toString();
            assertTrue(errOutput.contains("Error occurred executing command: " + actualCommand), "Error message should contain the command that failed");
            assertTrue(errOutput.contains("simulated failure"), "Stack trace should contain the simulated exception message");
        }
    }

    @ParameterizedTest(name = "[{index}] command={0}, waitFor={1}, expected={2}")
    @CsvSource({ "'echo hello', 0, 0", "'/bin/true', 42, 42" })
    @DisplayName("exec(String) returns the value produced by runIt when Runtime.exec succeeds")
    void exec_ShouldReturnRunItResult_WhenRuntimeExecSucceeds(String command, int waitForResult, int expected) throws Exception {
        // Arrange
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor()).thenReturn(waitForResult);
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime runtime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            when(runtime.exec(command)).thenReturn(mockProcess);
            Executor executor = new Executor();
            // Act
            int result = executor.exec(command);
            // Assert
            assertEquals(expected, result, "exec should propagate the value returned by runIt (simulated via Process.waitFor)");
        }
    }

    @Test
    @DisplayName("exec(String) returns -1 when runIt (Process.waitFor) throws InterruptedException")
    void exec_ShouldReturnMinusOne_WhenRunItThrowsInterruptedException() throws Exception {
        // Arrange
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor()).thenThrow(new InterruptedException("interrupted"));
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime runtime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            when(runtime.exec("/bin/sleep 1")).thenReturn(mockProcess);
            Executor executor = new Executor();
            // Capture System.err to verify logging
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));
            // Act
            int result = executor.exec("/bin/sleep 1");
            // Restore System.err
            System.setErr(originalErr);
            // Assert
            assertEquals(-1, result, "exec should return -1 when runIt throws InterruptedException");
            String errOutput = errContent.toString();
            assertTrue(errOutput.contains("Error occurred executing command: /bin/sleep 1"), "Error message should be logged");
        }
    }

    @Test
    @DisplayName("exec(String) returns -1 when runIt (Process.waitFor) throws unchecked RuntimeException")
    void exec_ShouldReturnMinusOne_WhenRunItThrowsRuntimeException() throws Exception {
        // Arrange
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor()).thenThrow(new IllegalStateException("illegal state"));
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime runtime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            when(runtime.exec("/bin/true")).thenReturn(mockProcess);
            Executor executor = new Executor();
            // Capture System.err
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));
            // Act
            int result = executor.exec("/bin/true");
            // Restore System.err
            System.setErr(originalErr);
            // Assert
            assertEquals(-1, result, "exec should return -1 when runIt throws an unchecked RuntimeException");
            String errOutput = errContent.toString();
            assertTrue(errOutput.contains("Error occurred executing command: /bin/true"), "Error message should be logged");
        }
    }

    @Test
    @DisplayName("exec(String) propagates Error thrown by runIt (Process.waitFor)")
    void exec_ShouldPropagateError_WhenRunItThrowsError() throws Exception {
        // Arrange
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor()).thenThrow(new AssertionError("assertion failed"));
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime runtime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(runtime);
            when(runtime.exec("/bin/true")).thenReturn(mockProcess);
            Executor executor = new Executor();
            // Act & Assert
            AssertionError thrown = assertThrows(AssertionError.class, () -> executor.exec("/bin/true"), "exec should let Errors propagate");
            assertEquals("assertion failed", thrown.getMessage(), "The propagated AssertionError should retain the original message");
        }
    }
}
