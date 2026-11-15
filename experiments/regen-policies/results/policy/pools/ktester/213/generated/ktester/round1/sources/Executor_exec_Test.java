package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Executor_exec_Test {

    // Helper field used by the interrupted‑execution test to capture the return value.
    @SuppressWarnings("unused")
    private int // default sentinel
    interruptedResult = -999;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure no custom SecurityManager interferes with a test.
        System.setSecurityManager(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore default SecurityManager after each test.
        System.setSecurityManager(null);
    }

    /**
     * Returns the operating system name in lower case.
     */
    private static String os() {
        return System.getProperty("os.name", "generic").toLowerCase();
    }

    /**
     * Wraps a raw Unix‑style command for Windows when necessary.
     *
     * @param unixCmd command that works on Unix‑like shells
     * @return a command string that can be executed on the current OS
     */
    private static String adaptCommand(String unixCmd) {
        if (os().contains("win")) {
            // Windows needs to be executed via cmd.exe /c
            return "cmd.exe /c " + unixCmd;
        }
        return unixCmd;
    }

    /**
     * Generates a stream of arguments for the successful‑execution parameterized test.
     *
     * <p>Each entry is a {@code String[2]} where element 0 is the command to execute
     * and element 1 is the expected exit code (as a string for easy parsing).</p>
     */
    private static Stream<String[]> successfulCommands() {
        List<String[]> data = new ArrayList<>();
        // Simple echo – should exit with 0
        data.add(new String[] { adaptCommand("echo hello"), "0" });
        // /bin/true (or Windows equivalent) – exit 0
        data.add(new String[] { adaptCommand("/bin/true"), "0" });
        // Explicit exit 42 – verifies that exec forwards the exact exit value
        if (os().contains("win")) {
            data.add(new String[] { adaptCommand("cmd /c exit 42"), "42" });
        } else {
            data.add(new String[] { adaptCommand("sh -c 'exit 42'"), "42" });
        }
        // Command with leading/trailing/multiple spaces
        data.add(new String[] { adaptCommand("   echo    spaced   "), "0" });
        // Special characters
        data.add(new String[] { adaptCommand("echo 'special chars !@#$%^&*()'"), "0" });
        // Very long command (≈1000 characters)
        StringBuilder longCmd = new StringBuilder("echo ");
        for (int i = 0; i < 200; i++) {
            // 5 chars * 200 ≈ 1000 chars
            longCmd.append("abcde");
        }
        data.add(new String[] { adaptCommand(longCmd.toString()), "0" });
        // Compound command using shell operators
        if (os().contains("win")) {
            data.add(new String[] { adaptCommand("cmd /c \"echo first && echo second\""), "0" });
        } else {
            data.add(new String[] { adaptCommand("sh -c 'echo first && echo second'"), "0" });
        }
        return data.stream();
    }

    /**
     * Generates a stream of arguments for the error‑handling parameterized test.
     *
     * <p>Each entry is a {@code String[]} where element 0 is the command (may be {@code null}
     * or empty) and element 1 is the expected exit code (always {@code -1}).</p>
     */
    private static Stream<String[]> errorCommands() {
        List<String[]> data = new ArrayList<>();
        // null command
        data.add(new String[] { null, "-1" });
        // empty command
        data.add(new String[] { "", "-1" });
        // nonsense command
        data.add(new String[] { adaptCommand("nonexistentcommand123"), "-1" });
        // Self‑kill simulation – on Unix we can use a command that kills the JVM process.
        // On Windows we simply use a command that fails fast.
        if (os().contains("win")) {
            // will not throw but we treat as error path later
            data.add(new String[] { adaptCommand("cmd /c exit 1"), "-1" });
        } else {
            data.add(new String[] { adaptCommand("/bin/sh -c 'kill -9 $$'"), "-1" });
        }
        return data.stream();
    }

    /**
     * Verifies that a simple command is executed, returns the expected exit code
     * (0) and that the standard‑output is captured correctly.
     */
    @Test
    void testExecStringSuccess() {
        Executor executor = new Executor();
        // Build a platform‑independent echo command.
        String command;
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().contains("windows")) {
            // Windows needs the command interpreter.
            command = "cmd.exe /c echo HelloWorld";
        } else {
            command = "echo HelloWorld";
        }
        int exitCode = executor.exec(command);
        // The echo command should succeed and return 0.
        assertEquals(0, exitCode, "Expected successful execution to return 0");
        // The captured output should contain the echoed text.
        assertEquals("HelloWorld", executor.getOut().trim(), "Standard output should contain the echoed string");
    }

    /**
     * Verifies that when {@code Runtime.exec} throws an exception (e.g. because the
     * supplied command is {@code null}), the method returns {@code -1} and does not
     * propagate the exception.
     */
    @Test
    void testExecStringHandlesException() {
        Executor executor = new Executor();
        int exitCode = executor.exec(null);
        // The contract states that any exception results in a -1 return value.
        assertEquals(-1, exitCode, "When an exception occurs, exec should return -1");
    }

    /**
     * Simulates an {@link InterruptedException} thrown from the private {@code runIt}
     * method by interrupting the thread while it is waiting for a long‑running process.
     *
     * <p>The test spawns a separate thread that calls {@code exec} on a command that
     * sleeps for several seconds. The main test thread sleeps briefly and then
     * interrupts the worker thread. The expected result is {@code -1}.</p>
     */
    @Test
    void testExecStringInterruptedExecution() throws Exception {
        // Skip on Windows where the sleep syntax differs; use PowerShell instead.
        String sleepCmd = os().contains("win") ? adaptCommand("powershell -Command \"Start-Sleep -Seconds 5\"") : adaptCommand("/bin/sleep 5");
        Executor executor = new Executor();
        // Worker thread that invokes exec()
        Thread worker = new Thread(() -> {
            int rc = executor.exec(sleepCmd);
            // Store result in a field via reflection for the assertion later.
            try {
                Field resultField = Executor_exec_Test.class.getDeclaredField("interruptedResult");
                resultField.setAccessible(true);
                resultField.setInt(Executor_exec_Test.this, rc);
            } catch (Exception ignored) {
            }
        });
        worker.start();
        // Give the process a moment to start and block inside runIt()
        Thread.sleep(200);
        // This should cause waitFor() → InterruptedException
        worker.interrupt();
        // wait for the thread to finish
        worker.join(2000);
        // Retrieve the result stored by the worker thread.
        Field resultField = Executor_exec_Test.class.getDeclaredField("interruptedResult");
        resultField.setAccessible(true);
        int result = resultField.getInt(this);
        assertEquals(-1, result, "When the waiting thread is interrupted, exec should catch the exception and return -1");
    }

    /**
     * Verifies that a {@link SecurityException} thrown by a custom {@link SecurityManager}
     * is handled correctly – {@code exec} must return {@code -1}.
     */
    @Test
    void testExecStringSecurityManagerDenial() {
        // Install a SecurityManager that forbids exec().
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkExec(String cmd) {
                throw new SecurityException("exec denied for testing");
            }

            @Override
            public void checkPermission(java.security.Permission perm) {
                // allow everything else
            }
        });
        Executor executor = new Executor();
        int exitCode = executor.exec(adaptCommand("/bin/true"));
        assertEquals(-1, exitCode, "When SecurityManager blocks exec, the method should return -1");
    }
}
