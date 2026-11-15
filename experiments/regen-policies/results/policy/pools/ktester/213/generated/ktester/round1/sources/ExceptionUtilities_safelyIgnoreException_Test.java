package com.cedarsoftware.util;

import java.util.concurrent.Callable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class ExceptionUtilities_safelyIgnoreException_Test {

    /**
     * Simple holder for parameterized test arguments.
     */
    private static class ArgumentsHolder {

        final Callable<Object> callable;

        final Object defaultValue;

        final Object expectedResult;

        final String description;

        ArgumentsHolder(Callable<Object> callable, Object defaultValue, Object expectedResult, String description) {
            this.callable = callable;
            this.defaultValue = defaultValue;
            this.expectedResult = expectedResult;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the groups defined in the JSON spec
     * -------------------------------------------------------------- */
    static java.util.stream.Stream<ArgumentsHolder> provideScenarios() {
        return java.util.stream.Stream.of(// Normal execution
        new ArgumentsHolder(() -> "hello", "default", "hello", "Callable executes without throwing; the method should return the value produced by the callable, ignoring the default."), // Checked exception handling (IOException)
        new ArgumentsHolder(() -> {
            throw new IOException("io");
        }, "fallback", "fallback", "The callable throws a checked exception (IOException). The method should catch the Throwable and return the supplied default value."), // Unchecked exception handling (RuntimeException)
        new ArgumentsHolder(() -> {
            throw new RuntimeException("runtime");
        }, "defaultRuntime", "defaultRuntime", "The callable throws an unchecked exception (RuntimeException). The method must swallow it and return the default value."), // Error handling (AssertionError)
        new ArgumentsHolder(() -> {
            throw new AssertionError("assert");
        }, "errorDefault", "errorDefault", "The callable throws an Error (AssertionError). Since the catch block catches Throwable, the method should return the default value."));
    }

    @Test
    void whenCallableReturnsValue_thenResultIsReturned() throws Exception {
        // given
        @SuppressWarnings("unchecked")
        Callable<String> callable = mock(Callable.class);
        when(callable.call()).thenReturn("actual");
        // when
        String result = ExceptionUtilities.safelyIgnoreException(callable, "default");
        // then
        assertEquals("actual", result);
        verify(callable, times(1)).call();
    }

    @Test
    void whenCallableThrowsRuntimeException_thenDefaultIsReturned() throws Exception {
        // given
        @SuppressWarnings("unchecked")
        Callable<Integer> callable = mock(Callable.class);
        when(callable.call()).thenThrow(new IllegalStateException("boom"));
        // when
        Integer result = ExceptionUtilities.safelyIgnoreException(callable, 42);
        // then
        assertEquals(42, result);
        verify(callable, times(1)).call();
    }

    @Test
    void whenCallableThrowsCheckedException_thenDefaultIsReturned() throws Exception {
        // given
        @SuppressWarnings("unchecked")
        Callable<String> callable = mock(Callable.class);
        when(callable.call()).thenThrow(new Exception("checked"));
        // when
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        // then
        assertEquals("fallback", result);
        verify(callable, times(1)).call();
    }

    @Test
    void whenCallableThrowsError_thenDefaultIsReturned() throws Exception {
        // given
        @SuppressWarnings("unchecked")
        Callable<Double> callable = mock(Callable.class);
        when(callable.call()).thenThrow(new OutOfMemoryError("oom"));
        // when
        Double result = ExceptionUtilities.safelyIgnoreException(callable, 3.14);
        // then
        assertEquals(3.14, result);
        verify(callable, times(1)).call();
    }

    @Test
    void whenDefaultIsNull_andCallableThrows_thenNullReturned() throws Exception {
        // given
        @SuppressWarnings("unchecked")
        Callable<Object> callable = mock(Callable.class);
        when(callable.call()).thenThrow(new RuntimeException("oops"));
        // when
        Object result = ExceptionUtilities.safelyIgnoreException(callable, null);
        // then
        assertNull(result);
        verify(callable, times(1)).call();
    }

    @Test
    void whenCallableReturnsNull_thenNullReturnedEvenIfDefaultIsNonNull() throws Exception {
        // given
        @SuppressWarnings("unchecked")
        Callable<String> callable = mock(Callable.class);
        when(callable.call()).thenReturn(null);
        // when
        String result = ExceptionUtilities.safelyIgnoreException(callable, "default");
        // then
        assertNull(result);
        verify(callable, times(1)).call();
    }

    /* --------------------------------------------------------------
     *  Null callable scenario
     * -------------------------------------------------------------- */
    @Test
    void whenCallableIsNull_thenDefaultIsReturned() {
        // given
        Callable<String> nullCallable = null;
        String defaultValue = "nullCallableDefault";
        // when
        String result = ExceptionUtilities.safelyIgnoreException(nullCallable, defaultValue);
        // then
        // The call to nullCallable.call() throws NPE which is caught,
        // therefore the default value must be returned.
        assertEquals(defaultValue, result);
    }

    /* --------------------------------------------------------------
     *  Reference‑equality of default value
     * -------------------------------------------------------------- */
    @Test
    void whenCallableThrows_thenReturnsExactDefaultInstance() {
        // given
        @SuppressWarnings("unchecked")
        Callable<Object> callable = mock(Callable.class);
        when(callable.call()).thenThrow(new Exception("boom"));
        StringBuilder defaultBuilder = new StringBuilder("shared");
        // when
        Object result = ExceptionUtilities.safelyIgnoreException(callable, defaultBuilder);
        // then
        // The method should return the same object instance (not a copy)
        assertSame(defaultBuilder, result);
        verify(callable, times(1)).call();
    }

    /* --------------------------------------------------------------
     *  Callable returns null (default non‑null)
     * -------------------------------------------------------------- */
    @Test
    void whenCallableReturnsNull_thenResultIsNullEvenIfDefaultNonNull() throws Exception {
        // given
        @SuppressWarnings("unchecked")
        Callable<String> callable = mock(Callable.class);
        when(callable.call()).thenReturn(null);
        String defaultValue = "nonNullDefault";
        // when
        String result = ExceptionUtilities.safelyIgnoreException(callable, defaultValue);
        // then
        assertNull(result);
        verify(callable, times(1)).call();
    }

    /* --------------------------------------------------------------
     *  Default value null when exception occurs
     * -------------------------------------------------------------- */
    @Test
    void whenExceptionAndDefaultIsNull_thenNullReturned() {
        // given
        @SuppressWarnings("unchecked")
        Callable<Object> callable = mock(Callable.class);
        when(callable.call()).thenThrow(new IllegalStateException("oops"));
        Object defaultValue = null;
        // when
        Object result = ExceptionUtilities.safelyIgnoreException(callable, defaultValue);
        // then
        assertNull(result);
        verify(callable, times(1)).call();
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideScenarios")
    void parameterizedScenarios(ArgumentsHolder holder) {
        // given
        @SuppressWarnings("unchecked")
        Callable<Object> callable = mock(Callable.class);
        try {
            when(callable.call()).thenAnswer(invocation -> holder.callable.call());
        } catch (Throwable ignored) {
            // This block will never be executed because we are configuring the mock,
            // not invoking it yet.
        }
        // when
        Object result = ExceptionUtilities.safelyIgnoreException(callable, holder.defaultValue);
        // then
        assertEquals(holder.expectedResult, result);
        verify(callable, times(1)).call();
    }
}
