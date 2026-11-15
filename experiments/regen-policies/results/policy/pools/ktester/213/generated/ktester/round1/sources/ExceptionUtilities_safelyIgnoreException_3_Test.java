package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ExceptionUtilities_safelyIgnoreException_3_Test {

    /**
     * Custom subclass of {@link OutOfMemoryError} used to verify that the
     * {@code instanceof} check also matches subclasses.
     */
    private static class MyOutOfMemoryError extends OutOfMemoryError {

        MyOutOfMemoryError(String s) {
            super(s);
        }
    }

    /**
     * Provides a stream of arguments: the {@link Throwable} instance to pass
     * to {@code safelyIgnoreException} and a boolean indicating whether the
     * call is expected to re‑throw the supplied throwable.
     *
     * The data mirrors the JSON test cases supplied in the task description.
     */
    static Stream<Arguments> throwableProvider() {
        return Stream.of(// Regular Exception – should be ignored
        arguments(new Exception("generic exception"), false), // AssertionError – not an OutOfMemoryError, should be ignored
        arguments(new AssertionError("some error"), false), // Subclass of OutOfMemoryError – must be re‑thrown
        arguments(new MyOutOfMemoryError("custom OOM"), true), // StackOverflowError – not OutOfMemoryError, ignored
        arguments(new StackOverflowError("SOE test"), false), // InternalError – subclass of VirtualMachineError, ignored
        arguments(new InternalError("internal error"), false), // RuntimeException – ignored
        arguments(new RuntimeException("runtime"), false), // Plain Throwable – ignored
        arguments(new Throwable("plain throwable"), false), // Generic Error – ignored
        arguments(new Error("generic error"), false), // Null – ignored (no exception should be thrown)
        arguments(null, false));
    }

    @Test
    void shouldRethrowOutOfMemoryError() {
        OutOfMemoryError oom = new OutOfMemoryError("simulated OOM");
        assertThatExceptionOfType(OutOfMemoryError.class).isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(oom));
    }

    @Test
    void shouldIgnoreNonOutOfMemoryThrowable() {
        IllegalArgumentException illegalArg = new IllegalArgumentException("bad argument");
        assertThatNoException().isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(illegalArg));
    }

    @Test
    void shouldIgnoreNullThrowable() {
        // The method is expected to do nothing when passed null.
        assertThatNoException().isThrownBy(() -> ExceptionUtilities.safelyIgnoreException((Throwable) null));
    }

    @ParameterizedTest(name = "[{index}] {0} → expect re‑throw: {1}")
    @MethodSource("throwableProvider")
    void shouldBehaveAccordingToSpecification(Throwable t, boolean expectRethrow) {
        if (expectRethrow) {
            // The expected exception type is the concrete class of the throwable.
            assertThatExceptionOfType(t.getClass()).isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(t));
        } else {
            assertThatNoException().isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(t));
        }
    }
}
