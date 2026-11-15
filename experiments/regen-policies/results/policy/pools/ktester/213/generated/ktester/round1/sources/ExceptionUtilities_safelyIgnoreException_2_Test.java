package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExceptionUtilities_safelyIgnoreException_2_Test {

    /**
     * Simple custom {@link Throwable} used to verify that {@code safelyIgnoreException}
     * catches any {@link Throwable}, not just {@link Exception} or {@link Error}.
     */
    private static class MyThrowable extends Throwable {

        MyThrowable(String message) {
            super(message);
        }
    }

    /**
     * Supplies a stream of {@link Runnable}s each configured to throw a different
     * {@link Throwable}. The description is used only for the test name.
     */
    private static Stream<Arguments> exceptionThrowingRunnables() {
        // 1. RuntimeException
        Runnable runtime = mock(Runnable.class);
        doThrow(new RuntimeException("runtime")).when(runtime).run();
        // 2. Error (OutOfMemoryError)
        Runnable error = mock(Runnable.class);
        doThrow(new OutOfMemoryError("simulated OOM")).when(error).run();
        // 3. Custom Throwable subclass (not Exception nor Error)
        Runnable customThrowable = mock(Runnable.class);
        doThrow(new MyThrowable("my throwable")).when(customThrowable).run();
        // 4. ThreadDeath (subclass of Error)
        Runnable threadDeath = mock(Runnable.class);
        doThrow(new ThreadDeath()).when(threadDeath).run();
        // 5. RuntimeException that wraps another exception
        Runnable wrapped = mock(Runnable.class);
        doThrow(new RuntimeException(new IllegalArgumentException("inner"))).when(wrapped).run();
        // 6. RuntimeException with suppressed exception
        RuntimeException withSuppressed = new RuntimeException("primary");
        withSuppressed.addSuppressed(new IllegalArgumentException("suppressed"));
        Runnable suppressed = mock(Runnable.class);
        doThrow(withSuppressed).when(suppressed).run();
        return Stream.of(Arguments.of(runtime, "RuntimeException is ignored"), Arguments.of(error, "Error (OutOfMemoryError) is ignored"), Arguments.of(customThrowable, "Custom Throwable subclass is ignored"), Arguments.of(threadDeath, "ThreadDeath is ignored"), Arguments.of(wrapped, "RuntimeException wrapping another exception is ignored"), Arguments.of(suppressed, "RuntimeException with suppressed exceptions is ignored"));
    }

    /**
     * Verify that the supplied {@link Runnable} is executed exactly once.
     */
    @Test
    void testRunnableIsExecuted() {
        Runnable runnable = mock(Runnable.class);
        ExceptionUtilities.safelyIgnoreException(runnable);
        verify(runnable, times(1)).run();
    }

    /**
     * Verify that a runtime exception thrown by the {@link Runnable}
     * is swallowed and does not propagate to the caller.
     */
    @Test
    void testRuntimeExceptionIsIgnored() {
        Runnable runnable = mock(Runnable.class);
        doThrow(new IllegalArgumentException("test exception")).when(runnable).run();
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(runnable));
        verify(runnable, times(1)).run();
    }

    /**
     * Verify that an {@link Error} (e.g., {@link OutOfMemoryError}) thrown by the
     * {@link Runnable} is also swallowed.
     */
    @Test
    void testErrorIsIgnored() {
        Runnable runnable = mock(Runnable.class);
        doThrow(new OutOfMemoryError("simulated OOM")).when(runnable).run();
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(runnable));
        verify(runnable, times(1)).run();
    }

    /* --------------------------------------------------------------
     *  1. Normal execution (no exception)
     * -------------------------------------------------------------- */
    @Test
    void testRunnableDoesNothing() {
        // A lambda that does nothing and never throws.
        Runnable runnable = () -> {
            /* intentionally empty */
        };
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(runnable));
    }

    /* --------------------------------------------------------------
     *  2. Exception handling scenarios – parameterized
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("exceptionThrowingRunnables")
    void testExceptionIsIgnored(Runnable runnable, String description) {
        // Verify that the runnable is executed once and that no exception propagates.
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(runnable));
        // If the runnable is a Mockito mock we can verify the interaction.
        if (mockingDetails(runnable).isMock()) {
            verify(runnable, times(1)).run();
        }
    }

    /* --------------------------------------------------------------
     *  3. Finally block execution verification
     * -------------------------------------------------------------- */
    @Test
    void testRunnableWithFinallyBlock() {
        AtomicBoolean finallyExecuted = new AtomicBoolean(false);
        Runnable runnable = () -> {
            try {
                throw new IllegalStateException("forced");
            } finally {
                finallyExecuted.set(true);
            }
        };
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(runnable));
        assertTrue(finallyExecuted.get(), "The finally block must have been executed");
    }

    /* --------------------------------------------------------------
     *  4. Conditional exception path
     * -------------------------------------------------------------- */
    @Test
    void testConditionalExceptionIsIgnored() {
        Runnable runnable = () -> {
            if (System.currentTimeMillis() > 0) {
                throw new NullPointerException("conditional");
            }
        };
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(runnable));
    }

    /* --------------------------------------------------------------
     *  5. Null Runnable argument
     * -------------------------------------------------------------- */
    @Test
    void testNullRunnableThrowsNPE() {
        Runnable nullRunnable = null;
        // The method attempts to invoke run() on a null reference, which should
        // result in a NullPointerException that is NOT caught by the internal try‑catch.
        assertThrows(NullPointerException.class, () -> ExceptionUtilities.safelyIgnoreException(nullRunnable));
    }
}
