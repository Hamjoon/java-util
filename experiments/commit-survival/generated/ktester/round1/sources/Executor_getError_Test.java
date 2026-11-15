package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Executor_getError_Test {

    private Executor executor;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        executor = new Executor();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
        executor = null;
    }

    /* --------------------------------------------------------------------- *
     * Helper to set the private _error field via reflection
     * --------------------------------------------------------------------- */
    private void setInternalError(String value) {
        try {
            Field errorField = Executor.class.getDeclaredField("_error");
            errorField.setAccessible(true);
            errorField.set(executor, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to set _error via reflection: " + e.getMessage());
        }
    }

    private static Stream<Arguments> basicGetterProvider() {
        return Stream.of(// case: newly instantiated executor (no error set) – we simulate by leaving it null
        Arguments.of(null, null), // case: exec fails and sets a specific message
        Arguments.of("File not found", "File not found"), // case: exec succeeds but explicitly sets empty string
        Arguments.of("", ""));
    }

    private static Stream<Arguments> subsequentExecutionsProvider() {
        return Stream.of(// First exec fails with "Error A"
        Arguments.of(new String[] { "Error A" }, "Error A"), // Second exec fails with a different message "Error B"
        Arguments.of(new String[] { "Error A", "Error B" }, "Error B"), // Exec succeeds and clears the error (null)
        Arguments.of(new String[] { "Error A", null }, null), // Exec sets whitespace-only error
        Arguments.of(new String[] { "   " }, "   "));
    }

    @Test
    @DisplayName("getError() should return null for a newly created Executor")
    void testGetErrorInitiallyNull() {
        // Given – a freshly instantiated Executor (handled in @BeforeEach)
        // When
        String error = executor.getError();
        // Then
        assertNull(error, "Expected getError() to return null before any execution");
    }

    @Test
    @DisplayName("getError() should retain its value after successive calls")
    void testGetErrorIdempotent() {
        // Given – a freshly instantiated Executor
        String firstCall = executor.getError();
        // When – call getError() again
        String secondCall = executor.getError();
        // Then – both calls should return the same value (null in this case)
        assertEquals(firstCall, secondCall, "Subsequent calls to getError() should return the same result");
    }

    /* --------------------------------------------------------------------- *
     * Group: getError() - basic getter behavior
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] _error={0} → expected={1}")
    @MethodSource("basicGetterProvider")
    @DisplayName("Basic getter behaviour for various internal _error values")
    void testGetErrorBasicBehaviour(String injectedError, String expected) {
        // Given – set the private field to the injected value
        setInternalError(injectedError);
        // When
        String actual = executor.getError();
        // Then – the getter should return exactly what we injected (including null)
        assertEquals(expected, actual, () -> "Expected getError() to return " + (expected == null ? "null" : "\"" + expected + "\""));
    }

    /* --------------------------------------------------------------------- *
     * Group: getError() - subsequent executions
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] after exec error={0}")
    @MethodSource("subsequentExecutionsProvider")
    @DisplayName("getError() reflects the most recent error after several exec calls")
    void testGetErrorAfterMultipleExecutions(String[] sequence, String expectedFinal) {
        // Simulate a series of exec calls by repeatedly setting the internal field.
        // The sequence array contains the error value that each exec would have left behind.
        for (String err : sequence) {
            setInternalError(err);
        }
        // After the last simulated exec, getError() should return the last value.
        String actual = executor.getError();
        assertEquals(expectedFinal, actual, () -> "After sequence " + java.util.Arrays.toString(sequence) + " expected final error to be \"" + expectedFinal + "\"");
    }

    /* --------------------------------------------------------------------- *
     * Group: getError() - idempotence after a specific error
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("Calling getError() repeatedly after an error does not change the value")
    void testGetErrorIdempotenceAfterError() {
        // Given – exec (simulated) set the error to "Error X"
        setInternalError("Error X");
        // When – call getError() multiple times
        String first = executor.getError();
        String second = executor.getError();
        String third = executor.getError();
        // Then – all calls must return the same string and must not modify internal state
        assertAll(() -> assertEquals("Error X", first, "First call should return the injected error"), () -> assertEquals(first, second, "Second call should equal first call"), () -> assertEquals(second, third, "Third call should equal second call"));
    }
}
