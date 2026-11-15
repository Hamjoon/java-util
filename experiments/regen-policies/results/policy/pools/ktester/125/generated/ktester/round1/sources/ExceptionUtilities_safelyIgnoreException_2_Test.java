package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExceptionUtilities_safelyIgnoreException_2_Test {

    // illustration and future extensions.
    @Mock
    Runnable mockRunnable;

    /**
     * A custom {@link Throwable} that does **not** extend {@link Exception}.
     * It behaves like an {@link Error} for the purpose of the test.
     */
    static class CustomError extends Error {

        CustomError(String message) {
            super(message);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test to avoid interaction leakage.
        reset(mockRunnable);
    }

    /**
     * Supplies runnables that each throw a different {@link Throwable}.
     */
    private static Stream<Runnable> throwableRunnablesProvider() {
        return Stream.of(// RuntimeException
        () -> {
            throw new IllegalStateException("runtime");
        }, // Error
        () -> {
            throw new AssertionError("error");
        }, // Custom Error subclass
        () -> {
            throw new CustomError("custom error");
        });
    }

    @Test
    void shouldExecuteRunnableWhenNoExceptionIsThrown() {
        // GIVEN
        doNothing().when(mockRunnable).run();
        // WHEN / THEN
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(mockRunnable));
        // Verify that the runnable was indeed invoked.
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void shouldIgnoreRuntimeExceptionThrownByRunnable() {
        // GIVEN
        doThrow(new IllegalArgumentException("test exception")).when(mockRunnable).run();
        // WHEN / THEN
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(mockRunnable));
        // Verify that the runnable was invoked despite the exception.
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void shouldIgnoreErrorThrownByRunnable() {
        // GIVEN
        doThrow(new OutOfMemoryError("oom")).when(mockRunnable).run();
        // WHEN / THEN
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(mockRunnable));
        // Verify that the runnable was invoked.
        verify(mockRunnable, times(1)).run();
    }

    @Test
    void shouldIgnoreThrowableWhenRunnableIsNull() {
        // GIVEN
        Runnable nullRunnable = null;
        // WHEN / THEN
        // The method catches the NullPointerException that occurs when trying to call run()
        // on a null reference, so no exception should escape.
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(nullRunnable));
    }

    @Test
    void shouldNotPropagateCheckedExceptionWrappedInRuntimeException() {
        // GIVEN
        doThrow(new RuntimeException(new Exception("checked inside runtime"))).when(mockRunnable).run();
        // WHEN / THEN
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(mockRunnable));
        // Verify invocation.
        verify(mockRunnable, times(1)).run();
    }

    // ------------------------------------------------------------------------
    @Test
    void shouldExecuteSideEffectBeforeExceptionAndIgnoreTheException() {
        // GIVEN
        AtomicBoolean flag = new AtomicBoolean(false);
        Runnable runnable = () -> {
            // side‑effect
            flag.set(true);
            // exception after side‑effect
            throw new IllegalStateException("boom");
        };
        // WHEN / THEN
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(runnable));
        // THEN the side‑effect must have been applied despite the exception.
        assertTrue(flag.get(), "The flag should be true after the runnable runs");
    }

    @Test
    void shouldIgnoreCustomThrowableSubclass() {
        // GIVEN
        Runnable runnable = () -> {
            throw new CustomError("custom error");
        };
        // WHEN / THEN
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(runnable));
    }

    @ParameterizedTest
    @MethodSource("throwableRunnablesProvider")
    void shouldIgnoreVariousThrowables(Runnable runnable) {
        // WHEN / THEN
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(runnable));
    }
}
