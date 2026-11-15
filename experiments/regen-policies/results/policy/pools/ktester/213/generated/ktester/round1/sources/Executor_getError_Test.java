package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.io.File;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class Executor_getError_Test {

    private Executor executor;

    @BeforeEach
    void setupBeforeEach() {
        executor = new Executor();
    }

    /**
     * Supplies test data for {@link #testGetErrorWithDirectFieldManipulation}.
     *
     * @return a stream of arguments: expected value and a human‑readable description.
     */
    private static Stream<Arguments> errorFieldProvider() {
        String longMessage = "A very long error message that exceeds typical lengths " + ".........................................................";
        return Stream.of(// No command executed – field never initialised → null
        Arguments.of(null, "When a new Executor instance is created, getError() returns null"), // Empty string set via reflection
        Arguments.of("", "After manually setting _error to an empty string, getError() returns empty string"), // Typical short error message
        Arguments.of("File not found", "When _error is set to a typical error message, getError() returns it unchanged"), // Very long message (>100 chars)
        Arguments.of(longMessage, "When _error contains a long string, getError() returns the full string without truncation"));
    }

    /**
     * Executes a command that is expected to write to the standard error stream
     * and verifies that {@link Executor#getError()} returns the captured text.
     */
    @Test
    void testGetErrorCapturesStderr() {
        String os = System.getProperty("os.name").toLowerCase();
        // Choose a command that reliably produces an error message.
        // On Unix-like systems, "ls /nonexistent_path" writes to stderr.
        // On Windows, "cmd.exe /c dir /nonexistent_path" does the same.
        if (os.contains("windows")) {
            executor.exec(new String[] { "cmd.exe", "/c", "dir", "/nonexistent_path" });
        } else {
            executor.exec("ls /nonexistent_path");
        }
        // The command should have a non‑zero exit code, but we are only interested in stderr.
        String errorOutput = executor.getError();
        // Ensure that some error text was captured.
        assertNotNull(errorOutput, "Error output should not be null");
        assertFalse(errorOutput.trim().isEmpty(), "Error output should contain text");
        // Optionally, check that the error contains a known keyword.
        if (os.contains("windows")) {
            assertTrue(errorOutput.toLowerCase().contains("file not found") || errorOutput.toLowerCase().contains("cannot find"), "Error message should indicate a missing file on Windows");
        } else {
            assertTrue(errorOutput.toLowerCase().contains("no such file or directory"), "Error message should indicate a missing file on Unix-like OS");
        }
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the reflection‑based scenarios.
     *  --------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("errorFieldProvider")
    void testGetErrorWithDirectFieldManipulation(String expected, String description) throws Exception {
        // For the "null" case we deliberately do NOT touch the private field.
        if (expected != null) {
            // Use reflection to set the private _error field.
            Field errorField = Executor.class.getDeclaredField("_error");
            errorField.setAccessible(true);
            errorField.set(executor, expected);
        }
        String actual = executor.getError();
        // Verify that the getter returns exactly what we stored (or null).
        assertEquals(expected, actual, "getError() should return the value set via reflection");
    }

    /* --------------------------------------------------------------
     *  Test that a failed execution populates the error field.
     *  --------------------------------------------------------------
     */
    @Test
    void testGetErrorAfterExecFailure() {
        // Attempt to run a command that does not exist.
        // The exact message varies by OS/JDK, so we assert on key fragments.
        int exitCode = executor.exec("nonexistentcommand");
        // The exec method should return a non‑zero exit code for failure.
        assertNotEquals(0, exitCode, "Exit code should be non‑zero for a nonexistent command");
        String error = executor.getError();
        assertNotNull(error, "Error output should not be null after a failed exec");
        assertFalse(error.trim().isEmpty(), "Error output should contain text after a failed exec");
        // Verify that the error contains recognizable parts of the expected system message.
        String lower = error.toLowerCase();
        assertTrue(lower.contains("cannot run program") || lower.contains("error"), "Error message should indicate that the program could not be started");
        assertTrue(lower.contains("nonexistentcommand"), "Error message should reference the command name that failed");
    }
}
