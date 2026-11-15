package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExceptionUtilities_safelyIgnoreException_Test {

    /* --------------------------------------------------------------
     *  Helper class – a custom subclass of OutOfMemoryError used in
     *  several test cases to verify that the method respects inheritance.
     * -------------------------------------------------------------- */
    private static class MyOutOfMemoryError extends OutOfMemoryError {

        MyOutOfMemoryError() {
            super();
        }

        MyOutOfMemoryError(String message) {
            super(message);
        }

        MyOutOfMemoryError(String message, Throwable cause) {
            super(message);
            initCause(cause);
        }
    }

    /**
     * Supplies the arguments for {@link #safelyIgnoreException_variousThrowables}.
     *
     * <p>Each {@link Arguments} row contains:
     * <ul>
     *   <li>The {@link Throwable} to pass to the method.</li>
     *   <li>A {@code boolean} indicating whether the call is expected to re‑throw.</li>
     *   <li>If re‑throwing, the exact {@link Class} of the expected exception.</li>
     * </ul>
     * </p>
     */
    private static Stream<Arguments> safelyIgnoreExceptionProvider() {
        return Stream.of(// null – should be ignored
        Arguments.of(null, false, null), // Regular checked exception – ignored
        Arguments.of(new Exception("generic"), false, null), // Generic Error (not OOM) – ignored
        Arguments.of(new Error("generic error"), false, null), // StackOverflowError – ignored
        Arguments.of(new StackOverflowError(), false, null), // Direct OutOfMemoryError – must be re‑thrown
        Arguments.of(new OutOfMemoryError("oom"), true, OutOfMemoryError.class), // Subclass of OutOfMemoryError – must be re‑thrown
        Arguments.of(new MyOutOfMemoryError("my oom"), true, MyOutOfMemoryError.class), // AssertionError – ignored
        Arguments.of(new AssertionError("assert"), false, null), // RuntimeException – ignored
        Arguments.of(new RuntimeException("runtime"), false, null), // Plain Throwable – ignored
        Arguments.of(new Throwable("plain"), false, null), // LinkageError – ignored
        Arguments.of(new LinkageError("linkage"), false, null), // OutOfMemoryError without a message – re‑throw
        Arguments.of(new OutOfMemoryError(), true, OutOfMemoryError.class), // Subclass with null message – re‑throw
        Arguments.of(new MyOutOfMemoryError((String) null), true, MyOutOfMemoryError.class));
    }

    @Test
    void safelyIgnoreException_doesNotThrow_forNonOutOfMemoryError() {
        // given
        Throwable runtime = new RuntimeException("just a test");
        Throwable error = new AssertionError("some error");
        // when / then – the method should simply return without throwing
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(runtime));
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(error));
    }

    @Test
    void safelyIgnoreException_rethrows_OutOfMemoryError() {
        // given
        OutOfMemoryError oom = new OutOfMemoryError("OOM for testing");
        // when / then – the method must re‑throw the same OutOfMemoryError instance
        OutOfMemoryError thrown = assertThrows(OutOfMemoryError.class, () -> ExceptionUtilities.safelyIgnoreException(oom));
        assertSame(oom, thrown, "The thrown OOM should be the original instance");
    }

    @Test
    void safelyIgnoreException_handlesNullGracefully() {
        // given
        Throwable nullThrowable = null;
        // when / then – passing null must not cause a NullPointerException
        assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(nullThrowable));
    }

    /* --------------------------------------------------------------
     *  Parameterized test covering all JSON‑specified cases.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] t={0}")
    @MethodSource("safelyIgnoreExceptionProvider")
    void safelyIgnoreException_variousThrowables(Throwable t, boolean shouldRethrow, Class<? extends Throwable> expectedExceptionClass) {
        if (shouldRethrow) {
            // The method must re‑throw the exact same instance.
            Throwable thrown = assertThrows(expectedExceptionClass, () -> ExceptionUtilities.safelyIgnoreException(t), "Expected a re‑thrown " + expectedExceptionClass.getSimpleName());
            assertSame(t, thrown, "The thrown instance should be the original object");
        } else {
            // For every non‑OutOfMemoryError (including null) the method must swallow it.
            assertDoesNotThrow(() -> ExceptionUtilities.safelyIgnoreException(t), "Method should not throw for " + (t == null ? "null" : t.getClass().getSimpleName()));
        }
    }
}
