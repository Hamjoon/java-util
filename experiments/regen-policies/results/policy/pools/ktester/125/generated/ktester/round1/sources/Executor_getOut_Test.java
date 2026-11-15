package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

class Executor_getOut_Test {

    /**
     * Text that the test command will echo to stdout.
     */
    private static final String THIS_IS_HANDY = "This is handy";

    /**
     * Command used on non‑Windows platforms.
     */
    private static final String ECHO_THIS_IS_HANDY = "echo " + THIS_IS_HANDY;

    /**
     * Command used on Windows platforms.
     */
    private static final String[] WINDOWS_ECHO_CMD = { "cmd.exe", "/c", "echo " + THIS_IS_HANDY };

    /**
     * Instance under test – created before each test.
     */
    private Executor executor;

    /**
     * Text that will be injected into the private {@code _out} field via reflection.
     */
    private static final String REFLECTED_OUT = "Hello World";

    /**
     * Simple command that writes a single word to stdout.
     */
    private static final String SIMPLE_ECHO = "echo Hello";

    /**
     * Command that produces no stdout (Unix {@code true} returns 0 and no output).
     */
    private static final String NO_OUTPUT_CMD = "true";

    @BeforeEach
    void setupBeforeEach() {
        executor = new Executor();
        executor = new Executor();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly discard the executor so that any lingering process
        // resources become eligible for GC between tests.
        executor = null;
        executor = null;
    }

    /**
     * Sets the private {@code _out} field of the given {@link Executor}
     * instance to the supplied value using reflection.
     *
     * @param exec  the executor whose field should be modified
     * @param value the value to assign to {@code _out}
     * @throws ReflectiveOperationException if the field cannot be accessed
     */
    private void setOutViaReflection(Executor exec, String value) throws ReflectiveOperationException {
        Field outField = Executor.class.getDeclaredField("_out");
        outField.setAccessible(true);
        outField.set(exec, value);
    }

    /**
     * Verifies that {@code getOut()} returns the expected output after a successful
     * command execution. The test adapts to the operating system on which it runs.
     */
    @Test
    void testGetOutReturnsCommandOutput() {
        String os = System.getProperty("os.name");
        int exitCode;
        if (os != null && os.toLowerCase().contains("windows")) {
            exitCode = executor.exec(WINDOWS_ECHO_CMD);
        } else {
            exitCode = executor.exec(ECHO_THIS_IS_HANDY);
        }
        // The command should succeed (exit code 0) on a normal environment.
        assertEquals(0, exitCode, "Command should exit with code 0");
        // The captured stdout must contain the expected string.
        assertNotNull(executor.getOut(), "getOut() should not return null after exec");
        assertEquals(THIS_IS_HANDY, executor.getOut().trim(), "Captured stdout should match the echoed text");
    }

    /**
     * Ensures that {@code getOut()} returns {@code null} before any command has been executed.
     */
    @Test
    void testGetOutBeforeExecIsNull() {
        assertNull(executor.getOut(), "Before any exec() call, getOut() should return null");
    }

    /**
     * Group: {@code Executor.getOut() basic behavior}
     *
     * <p>Case 1 – verify that a fresh {@code Executor} returns {@code null}
     * before any command execution.</p>
     */
    @Test
    void getOut_beforeExec_returnsNull() {
        assertNull(executor.getOut(), "Before any exec() call, getOut() should return null");
    }

    /**
     * Group: {@code Executor.getOut() basic behavior}
     *
     * <p>Case 2 – using reflection to set the private {@code _out} field
     * and confirming that {@code getOut()} returns the same value.</p>
     */
    @Test
    void getOut_afterReflectionReflectsSetValue() throws Exception {
        // Arrange: inject known value into the private field
        setOutViaReflection(executor, REFLECTED_OUT);
        // Act & Assert
        assertEquals(REFLECTED_OUT, executor.getOut(), "getOut() should return the value that was set via reflection");
    }

    /**
     * Group: {@code Executor.getOut() after successful exec}
     *
     * <p>Case 1 – execute a command that writes 'Hello' to stdout and verify
     * that {@code getOut()} returns exactly that string (trimmed).</p>
     */
    @Test
    void getOut_afterExecWithOutput_returnsCapturedText() {
        // Choose platform‑specific command
        String os = System.getProperty("os.name");
        int exitCode;
        if (os != null && os.toLowerCase().contains("windows")) {
            // Windows needs the command array form
            exitCode = executor.exec(new String[] { "cmd.exe", "/c", SIMPLE_ECHO });
        } else {
            exitCode = executor.exec(SIMPLE_ECHO);
        }
        assertEquals(0, exitCode, "Command should exit with code 0");
        assertNotNull(executor.getOut(), "getOut() must not be null after successful exec");
        assertEquals("Hello", executor.getOut().trim(), "Captured stdout should match the echoed word");
    }

    /**
     * Group: {@code Executor.getOut() after successful exec}
     *
     * <p>Case 2 – execute a command that produces no output and verify that
     * {@code getOut()} returns an empty string (not {@code null}).</p>
     */
    @Test
    void getOut_afterExecWithNoOutput_returnsEmptyString() {
        // The {@code true} command exists on Unix‑like systems; on Windows we use {@code cmd /c exit 0}
        String os = System.getProperty("os.name");
        int exitCode;
        if (os != null && os.toLowerCase().contains("windows")) {
            exitCode = executor.exec(new String[] { "cmd.exe", "/c", "exit", "0" });
        } else {
            exitCode = executor.exec(NO_OUTPUT_CMD);
        }
        assertEquals(0, exitCode, "Command should exit with code 0");
        assertNotNull(executor.getOut(), "getOut() should not be null even when there is no output");
        assertEquals("", executor.getOut(), "When the command produces no stdout, getOut() should be an empty string");
    }
}
