package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.File;
import java.lang.reflect.Field;
import java.util.stream.Stream;

// No Mockito usage now, but kept per template
@ExtendWith(MockitoExtension.class)
class Executor_getOut_Test {

    private Executor executor;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test to avoid state leakage
        executor = new Executor();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no lingering process handles remain
        executor = null;
    }

    /**
     * Supplies test data for {@link #testGetOut_afterExecString(String, String)}.
     * <ul>
     *   <li>Successful echo command – expects the echoed text.</li>
     *   <li>Invalid command – expects {@code null} because no stdout is produced.</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> provideExecStringCases() {
        String os = System.getProperty("os.name", "").toLowerCase();
        boolean isWindows = os.contains("windows");
        // Successful echo command
        String echoCommand = isWindows ? "cmd.exe /c echo Hello" : "echo Hello";
        String echoExpected = "Hello";
        // Invalid command – on Windows we still use cmd.exe to ensure the process starts
        String invalidCommand = isWindows ? "cmd.exe /c invalidCommand" : "invalidCommand";
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(echoCommand, echoExpected), org.junit.jupiter.params.provider.Arguments.of(invalidCommand, null));
    }

    /**
     * Verifies that {@link Executor#getOut()} returns the standard‑output
     * produced by a simple command. The command is chosen based on the host
     * operating system to work on both Windows and Unix‑like platforms.
     */
    @Test
    void testGetOut() {
        // given
        final String expected = "This is handy";
        final String command = "echo " + expected;
        // when
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().contains("windows")) {
            // Windows needs to be invoked via cmd.exe
            executor.exec(new String[] { "cmd.exe", "/c", command });
        } else {
            executor.exec(command);
        }
        // then
        assertNotNull(executor.getOut(), "Standard output should not be null");
        assertEquals(expected, executor.getOut().trim(), "Executor should capture the exact command output");
    }

    /**
     * When a newly created {@link Executor} has not executed any command,
     * the internal {@code _out} field should be {@code null}.  Hence,
     * {@link Executor#getOut()} must return {@code null}.
     */
    @Test
    void testGetOut_beforeAnyExec_returnsNull() {
        assertNull(executor.getOut(), "getOut() should return null before any exec() call");
    }

    /**
     * Directly set the private {@code _out} field via reflection and verify
     * that {@link Executor#getOut()} returns the exact value that was set.
     */
    @Test
    void testGetOut_afterReflectionSet_returnsExactValue() throws Exception {
        // Arrange – set private field "_out"
        String expected = "Sample output";
        Field outField = Executor.class.getDeclaredField("_out");
        outField.setAccessible(true);
        outField.set(executor, expected);
        // Act & Assert
        assertEquals(expected, executor.getOut(), "getOut() must return the value that was set via reflection");
    }

    /**
     * Executes a single‑string command and checks the captured stdout.
     * The command differs per OS so that the test works on Windows and Unix‑like systems.
     *
     * @param command   the command to execute
     * @param expected  the expected stdout (trimmed)
     */
    @ParameterizedTest(name = "[{index}] exec(String) ''{0}'' → ''{1}''")
    @MethodSource("provideExecStringCases")
    void testGetOut_afterExecString(String command, String expected) {
        // Act – run the command
        executor.exec(command);
        // Assert – getOut() should contain the expected output (or null)
        if (expected == null) {
            assertNull(executor.getOut(), "When the command fails, getOut() should stay null");
        } else {
            assertNotNull(executor.getOut(), "Standard output should not be null for a successful command");
            assertEquals(expected, executor.getOut().trim(), "Captured stdout does not match expected value");
        }
    }

    /**
     * Executes a command supplied as a {@code String[]} and verifies that
     * {@link Executor#getOut()} returns the expected stdout.
     * The command array is built according to the host OS.
     */
    @Test
    void testGetOut_afterExecStringArray_simpleCommand() {
        // Arrange – build command array that prints "World"
        String os = System.getProperty("os.name", "").toLowerCase();
        String[] cmdArray;
        if (os.contains("windows")) {
            // Windows requires cmd.exe /c
            cmdArray = new String[] { "cmd.exe", "/c", "echo", "World" };
        } else {
            cmdArray = new String[] { "echo", "World" };
        }
        String expected = "World";
        // Act
        executor.exec(cmdArray);
        // Assert
        assertNotNull(executor.getOut(), "Standard output should not be null after exec(String[])");
        assertEquals(expected, executor.getOut().trim(), "Executor should capture the exact output from exec(String[])");
    }

    /**
     * Verifies that {@link Executor#getOut()} returns the standard‑output
     * produced by a simple command. The command is chosen based on the host
     * operating system to work on both Windows and Unix‑like platforms.
     */
    @Test
    void testGetOut_existingExample() {
        // given
        final String expected = "This is handy";
        final String command = "echo " + expected;
        // when
        String os = System.getProperty("os.name");
        if (os != null && os.toLowerCase().contains("windows")) {
            // Windows needs to be invoked via cmd.exe
            executor.exec(new String[] { "cmd.exe", "/c", command });
        } else {
            executor.exec(command);
        }
        // then
        assertNotNull(executor.getOut(), "Standard output should not be null");
        assertEquals(expected, executor.getOut().trim(), "Executor should capture the exact command output");
    }
}
