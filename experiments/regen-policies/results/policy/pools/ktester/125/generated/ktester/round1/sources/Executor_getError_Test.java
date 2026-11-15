package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.File;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class Executor_getError_Test {

    private Executor executor;

    @BeforeEach
    void setupBeforeEach() {
        executor = new Executor();
    }

    /**
     * Executes a command that is expected to succeed and therefore
     * should produce no error output. {@code getError()} is expected
     * to return {@code null} or an empty string.
     */
    @Test
    void testGetErrorWhenCommandSucceeds() {
        String os = System.getProperty("os.name").toLowerCase();
        // Choose a command that is guaranteed to exist on the platform.
        if (os.contains("windows")) {
            // "cmd.exe /c echo Hello"
            executor.exec(new String[] { "cmd.exe", "/c", "echo Hello" });
        } else {
            // Unix‑like: "echo Hello"
            executor.exec("echo Hello");
        }
        // The command succeeded; error output should be absent.
        String error = executor.getError();
        // The implementation may return null or an empty string.
        assertTrue(error == null || error.isEmpty(), () -> "Expected no error output, but got: " + error);
    }

    /**
     * Executes a command that is expected to fail, producing error output.
     * {@code getError()} should contain a non‑empty string describing the failure.
     */
    @Test
    void testGetErrorWhenCommandFails() {
        String os = System.getProperty("os.name").toLowerCase();
        // Use a command that does not exist or deliberately fails.
        if (os.contains("windows")) {
            // "cmd.exe /c dir nonexistent_folder"
            executor.exec(new String[] { "cmd.exe", "/c", "dir", "nonexistent_folder_12345" });
        } else {
            // Unix‑like: "ls nonexistent_folder_12345"
            executor.exec("ls nonexistent_folder_12345");
        }
        String error = executor.getError();
        // The command should have produced some error text.
        assertNotNull(error, "Error output should not be null for a failing command");
        assertFalse(error.trim().isEmpty(), () -> "Error output should not be empty for a failing command, but got: '" + error + "'");
    }

    @Test
    @DisplayName("When a new Executor is created, getError() returns null")
    void testGetErrorInitiallyNull() {
        assertNull(executor.getError(), "A freshly created Executor should have a null error field.");
    }

    @Test
    @DisplayName("Getter returns the exact string set via reflection")
    void testGetErrorReflectivelySetNonNull() throws Exception {
        String expected = "File not found";
        Field f = Executor.class.getDeclaredField("_error");
        f.setAccessible(true);
        f.set(executor, expected);
        assertEquals(expected, executor.getError(), "getError() must return the exact string stored in the private field.");
    }

    @Test
    @DisplayName("Getter returns empty string when field is set to empty via reflection")
    void testGetErrorReflectivelySetEmpty() throws Exception {
        Field f = Executor.class.getDeclaredField("_error");
        f.setAccessible(true);
        f.set(executor, "");
        assertEquals("", executor.getError(), "getError() should return the empty string that was set.");
    }

    @Test
    @DisplayName("After a successful exec, getError() returns null or empty")
    void testGetErrorAfterSuccessfulExec() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            // Windows: use cmd.exe to echo
            executor.exec(new String[] { "cmd.exe", "/c", "echo", "hello" });
        } else {
            // Unix‑like
            executor.exec("echo hello");
        }
        String error = executor.getError();
        assertTrue(error == null || error.isEmpty(), () -> "Expected no error output after successful command, but got: " + error);
    }

    @Test
    @DisplayName("After a failing exec with an unknown command, getError() is non‑null")
    void testGetErrorAfterFailedExecInvalidCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            executor.exec(new String[] { "nonexistent_command_12345" });
        } else {
            executor.exec("nonexistent_command_12345");
        }
        String error = executor.getError();
        assertNotNull(error, "Error output should not be null for a failing command");
        assertFalse(error.trim().isEmpty(), () -> "Error output should not be empty, but got: '" + error + "'");
    }

    @Test
    @DisplayName("exec(null) stores an error message and getError() returns it")
    void testGetErrorAfterExecNullCommand() {
        // The implementation is expected to catch the exception and store its message.
        executor.exec((String) null);
        String error = executor.getError();
        assertNotNull(error, "Calling exec(null) should result in a stored error message");
        assertFalse(error.trim().isEmpty(), () -> "Stored error message should not be empty, but got: '" + error + "'");
    }

    @Test
    @DisplayName("Error is cleared after a successful exec following a failure")
    void testErrorClearedAfterSuccess() {
        String os = System.getProperty("os.name").toLowerCase();
        // First, cause a failure.
        if (os.contains("windows")) {
            executor.exec(new String[] { "invalid_cmd" });
        } else {
            executor.exec("invalid_cmd");
        }
        assertNotNull(executor.getError(), "First failing exec should produce a non‑null error.");
        // Then, a successful command.
        if (os.contains("windows")) {
            executor.exec(new String[] { "cmd.exe", "/c", "echo", "ok" });
        } else {
            executor.exec("echo ok");
        }
        // After success, error must be cleared.
        assertTrue(executor.getError() == null || executor.getError().isEmpty(), "Error should be cleared after a successful exec.");
    }

    @Test
    @DisplayName("Returned error string cannot affect internal state (String immutability)")
    void testGetErrorImmutability() throws Exception {
        String original = "original";
        Field f = Executor.class.getDeclaredField("_error");
        f.setAccessible(true);
        f.set(executor, original);
        String returned = executor.getError();
        // Attempt to "modify" – strings are immutable, so we just reassign the variable.
        returned = "modified";
        // Internal field must still hold the original value.
        assertEquals(original, executor.getError(), "Internal error string must remain unchanged after external reference manipulation.");
    }

    @Test
    @DisplayName("exec with null envp succeeds and produces no error")
    void testGetErrorWithNullEnvp() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            executor.exec("echo test", null);
        } else {
            executor.exec("echo test", null);
        }
        assertTrue(executor.getError() == null || executor.getError().isEmpty(), "No error should be recorded when envp is null and command succeeds.");
    }

    @Test
    @DisplayName("exec with null working directory succeeds and produces no error")
    void testGetErrorWithNullWorkingDirectory() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            executor.exec("echo dirTest", new String[0], null);
        } else {
            executor.exec("echo dirTest", new String[0], null);
        }
        assertTrue(executor.getError() == null || executor.getError().isEmpty(), "No error should be recorded when working directory is null and command succeeds.");
    }

    @Test
    @DisplayName("exec with an invalid command array element records an error")
    void testGetErrorWithInvalidCommandArray() {
        executor.exec(new String[] { "invalid_cmd_abc" });
        String error = executor.getError();
        assertNotNull(error, "Error should be non‑null for an invalid command array");
        assertFalse(error.trim().isEmpty(), () -> "Error should contain a description, but got: '" + error + "'");
    }

    @Test
    @DisplayName("Repeated getError() calls without exec consistently return null")
    void testRepeatedGetErrorWithoutExec() {
        assertNull(executor.getError(), "First call should return null");
        assertNull(executor.getError(), "Second call should still return null");
        assertNull(executor.getError(), "Third call should still return null");
    }

    @ParameterizedTest(name = "Successful command \"{0}\" should produce no error")
    @ValueSource(strings = { "echo hello", "cmd.exe /c echo hello" })
    void testParameterizedSuccessCommands(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("windows");
        // Filter commands according to OS
        if (isWindows && command.startsWith("cmd.exe")) {
            executor.exec(command.split(" "));
        } else if (!isWindows && !command.startsWith("cmd.exe")) {
            executor.exec(command);
        } else {
            // Skip incompatible command
            return;
        }
        String error = executor.getError();
        assertTrue(error == null || error.isEmpty(), () -> "Expected no error for command '" + command + "', but got: " + error);
    }
}
