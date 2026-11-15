package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.Callable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExceptionUtilities_safelyIgnoreException_Test {

    @Mock
    Callable<String> successfulCallable;

    @Mock
    Callable<String> failingCallable;

    /**
     * Sentinel object used to indicate that the test expects the returned value to be the
     * *same* instance as the supplied default value (reference equality).
     */
    private static final Object SAME_INSTANCE = new Object();

    /**
     * Custom {@link Error} subclass used in one of the test cases.
     */
    static class CustomError extends Error {

        CustomError(String message) {
            super(message);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mocks before each test to avoid interaction bleed‑through
        reset(successfulCallable, failingCallable);
    }

    /**
     * Provides a stream of arguments for the parameterised test.
     *
     * Each {@link Arguments} consists of:
     * <ol>
     *   <li>A {@link Callable} (or {@code null}) to pass to the method under test.</li>
     *   <li>The default value to supply.</li>
     *   <li>An {@code Object} representing the expected result:
     *       <ul>
     *         <li>For ordinary cases this is the expected value.</li>
     *         <li>For the “reference‑equality” case it is the {@link #SAME_INSTANCE} sentinel.</li>
     *       </ul>
     *   </li>
     *   <li>A description (used only for readability in the test report).</li>
     * </ol>
     */
    private static Stream<Arguments> provideTestCases() {
        return Stream.of(// 1. Successful Callable Execution
        Arguments.of((Callable<Object>) () -> "result", "default", "result", "Callable returns a normal value; the method should return that value and ignore the default."), // 2. Callable Throws Checked Exception
        Arguments.of((Callable<Object>) () -> {
            throw new IOException("io error");
        }, "fallback", "fallback", "Callable throws a checked exception; the method catches Throwable and returns the supplied default."), // 3. Callable Throws RuntimeException
        Arguments.of((Callable<Object>) () -> {
            throw new IllegalArgumentException("bad");
        }, 42, 42, "Callable throws an unchecked exception; the method still catches Throwable and returns the default."), // 4. Callable Throws Error (subclass of Throwable)
        Arguments.of((Callable<Object>) () -> {
            throw new AssertionError("assert");
        }, null, null, "Callable throws an Error; the catch block catches Throwable and returns the default (null)."), // 5. Null Callable Argument
        Arguments.of(null, "def", "def", "Callable reference is null; invoking call() throws NullPointerException which is caught, so the method returns the default."), // 6. Null Default Value
        Arguments.of((Callable<Object>) () -> {
            throw new RuntimeException();
        }, null, null, "Callable throws an exception and the supplied defaultValue is null; the method returns null."), // 7. Callable Returns Null Successfully
        Arguments.of((Callable<Object>) () -> null, "fallback", null, "Callable executes without throwing and returns null; the method should return the callable's null result, not the supplied default."), // 8. Callable Throws Custom Throwable (non‑Exception, non‑Error)
        Arguments.of((Callable<Object>) () -> {
            throw new Throwable("custom");
        }, 123, 123, "Callable throws a plain java.lang.Throwable; the catch block catches any Throwable and returns the default value."), // 9. Null Callable with Null Default
        Arguments.of(null, null, null, "Both the callable reference and the default value are null; invoking the null callable throws NullPointerException which is caught, so the method returns the null default."), // 10. Callable Returns Complex Object, Default Different
        Arguments.of((Callable<Object>) () -> Arrays.asList("alpha", "beta"), Collections.singletonList("gamma"), Arrays.asList("alpha", "beta"), "Callable returns a List<String> successfully; the method must return that list and ignore the different default list."), // 11. Callable Throws Exception, Default Is Mutable Object (reference equality)
        Arguments.of((Callable<Object>) () -> {
            throw new IllegalStateException();
        }, new ArrayList<>(), SAME_INSTANCE, "Callable throws an exception; the provided mutable default (an empty ArrayList) should be returned. The test asserts that the returned object is the exact same instance as the default (reference equality)."), // 12. Callable Throws Custom Error Subclass
        Arguments.of((Callable<Object>) () -> {
            throw new CustomError("custom error");
        }, null, null, "Callable throws a user‑defined subclass of java.lang.Error; the catch block catches any Throwable and the method should return the supplied default (null)."));
    }

    @Test
    void whenCallableSucceeds_thenResultIsReturned() throws Exception {
        // given
        String expected = "result";
        when(successfulCallable.call()).thenReturn(expected);
        String defaultValue = "default";
        // when
        String actual = ExceptionUtilities.safelyIgnoreException(successfulCallable, defaultValue);
        // then
        assertEquals(expected, actual, "The value returned by the callable should be propagated");
        verify(successfulCallable, times(1)).call();
        verifyNoInteractions(failingCallable);
    }

    @Test
    void whenCallableThrows_thenDefaultValueIsReturned() throws Exception {
        // given
        when(failingCallable.call()).thenThrow(new RuntimeException("boom"));
        String defaultValue = "fallback";
        // when
        String actual = ExceptionUtilities.safelyIgnoreException(failingCallable, defaultValue);
        // then
        assertEquals(defaultValue, actual, "When an exception occurs, the default value should be returned");
        verify(failingCallable, times(1)).call();
        verifyNoInteractions(successfulCallable);
    }

    @Test
    void whenCallableReturnsNull_thenNullIsReturned() throws Exception {
        // given
        when(successfulCallable.call()).thenReturn(null);
        String defaultValue = "notUsed";
        // when
        String actual = ExceptionUtilities.safelyIgnoreException(successfulCallable, defaultValue);
        // then
        assertNull(actual, "A callable that legitimately returns null should result in null, not the default");
        verify(successfulCallable, times(1)).call();
    }

    @Test
    void whenDefaultValueIsNull_andCallableFails_thenNullIsReturned() throws Exception {
        // given
        when(failingCallable.call()).thenThrow(new Exception("failure"));
        String defaultValue = null;
        // when
        String actual = ExceptionUtilities.safelyIgnoreException(failingCallable, defaultValue);
        // then
        assertNull(actual, "If the default value is null, it should be returned when the callable throws");
        verify(failingCallable, times(1)).call();
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideTestCases")
    void testSafelyIgnoreException(Callable<Object> callable, Object defaultValue, Object expected, String description) {
        // Act
        Object actual = ExceptionUtilities.safelyIgnoreException(callable, defaultValue);
        // Assert
        if (expected == SAME_INSTANCE) {
            // The test expects the exact same instance as the supplied default value.
            assertSame(defaultValue, actual, "When an exception occurs, the method should return the *same* instance as the default value");
        } else {
            assertEquals(expected, actual, "Unexpected result for scenario: " + description);
        }
    }
}
