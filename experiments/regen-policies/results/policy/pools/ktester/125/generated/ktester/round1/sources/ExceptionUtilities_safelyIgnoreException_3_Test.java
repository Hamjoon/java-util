package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExceptionUtilities_safelyIgnoreException_3_Test {

    /**
     * Subclass of {@link OutOfMemoryError} to verify that the {@code instanceof}
     *  check also matches subclasses.
     */
    private static class CustomOutOfMemoryError extends OutOfMemoryError {

        CustomOutOfMemoryError(String message) {
            super(message);
        }
    }

    /**
     * Arbitrary user‑defined {@link Throwable} subclass.
     */
    private static class MyCustomThrowable extends Throwable {

        MyCustomThrowable(String message) {
            super(message);
        }
    }

    /**
     * Supplies the arguments for {@link #shouldHandleVariousThrowables}.
     *
     * The matrix mirrors the JSON test cases.
     */
    private static Stream<Arguments> throwableProvider() {
        // Regular checked exception – should be ignored
        Exception genericException = new Exception("generic");
        // RuntimeException – should be ignored
        RuntimeException runtimeException = new RuntimeException("runtime");
        // OutOfMemoryError – must be re‑thrown
        OutOfMemoryError oome = new OutOfMemoryError("oom");
        // Subclass of OutOfMemoryError – must be re‑thrown
        CustomOutOfMemoryError customOome = new CustomOutOfMemoryError("custom oom");
        // Null reference – should be ignored
        Throwable nullThrowable = null;
        // AssertionError (subclass of Error, not OOME) – ignored
        AssertionError assertionError = new AssertionError("assert");
        // StackOverflowError – ignored
        StackOverflowError stackOverflowError = new StackOverflowError("stack");
        // Plain Throwable – ignored
        Throwable plainThrowable = new Throwable("plain throwable");
        // User‑defined subclass of Throwable – ignored
        MyCustomThrowable myCustomThrowable = new MyCustomThrowable("custom");
        // RuntimeException whose cause is an OutOfMemoryError – ignored because the instance itself is not OOME
        RuntimeException runtimeWithCause = new RuntimeException("runtime", new OutOfMemoryError("cause"));
        // Separate case for reference‑equality verification (same instance as supplied)
        OutOfMemoryError sameInstanceOOME = new OutOfMemoryError("same instance");
        return Stream.of(// 1. regular checked exception
        Arguments.of(genericException, false, null, false), // 2. RuntimeException
        Arguments.of(runtimeException, false, null, false), // 3. OutOfMemoryError – re‑throw
        Arguments.of(oome, true, OutOfMemoryError.class, true), // 4. Subclass of OutOfMemoryError – re‑throw
        Arguments.of(customOome, true, CustomOutOfMemoryError.class, true), // 5. null argument – ignore
        Arguments.of(nullThrowable, false, null, false), // 6. AssertionError – ignore
        Arguments.of(assertionError, false, null, false), // 7. StackOverflowError – ignore
        Arguments.of(stackOverflowError, false, null, false), // 8. Plain Throwable – ignore
        Arguments.of(plainThrowable, false, null, false), // 9. User‑defined Throwable subclass – ignore
        Arguments.of(myCustomThrowable, false, null, false), // 10. RuntimeException with OOME as cause – ignore
        Arguments.of(runtimeWithCause, false, null, false), // 11. Same instance verification (already covered by case 3 but kept for clarity)
        Arguments.of(sameInstanceOOME, true, OutOfMemoryError.class, true));
    }

    @Test
    void shouldRethrowOutOfMemoryError() {
        // GIVEN an OutOfMemoryError instance
        OutOfMemoryError oome = new OutOfMemoryError("simulated OOME");
        // THEN the utility must re‑throw it
        assertThatExceptionOfType(OutOfMemoryError.class).isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(oome)).withMessageContaining("simulated OOME");
    }

    @Test
    void shouldIgnoreNonOutOfMemoryThrowables() {
        // GIVEN a regular exception type
        IllegalArgumentException iae = new IllegalArgumentException("bad argument");
        // THEN no exception should be propagated
        assertThatNoException().isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(iae));
    }

    @Test
    void shouldIgnoreNullThrowable() {
        // GIVEN a null argument (allowed by the method – instanceof returns false)
        Throwable nullThrowable = null;
        // THEN no exception should be thrown
        assertThatNoException().isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(nullThrowable));
    }

    /**
     * Parameterised test that checks the behaviour of {@code safelyIgnoreException}
     * for a variety of {@link Throwable} instances.
     *
     * @param throwable   the input to the method under test
     * @param shouldThrow true if the method is expected to re‑throw the throwable
     * @param expectedCls the exact class of the expected exception (null when no exception)
     * @param sameInstance true if the thrown exception must be the same instance
     */
    @ParameterizedTest(name = "[{index}] {0} → expectThrow={1}")
    @MethodSource("throwableProvider")
    void shouldHandleVariousThrowables(Throwable throwable, boolean shouldThrow, Class<? extends Throwable> expectedCls, boolean sameInstance) {
        if (shouldThrow) {
            // Capture the thrown exception and optionally assert reference equality
            assertThatExceptionOfType(expectedCls).isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(throwable)).satisfies(ex -> {
                if (sameInstance) {
                    // The utility must re‑throw the exact same object
                    assertThat(ex).isSameAs(throwable);
                }
            });
        } else {
            // No exception must be propagated
            assertThatNoException().isThrownBy(() -> ExceptionUtilities.safelyIgnoreException(throwable));
        }
    }
}
