package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class ExceptionUtilities_getDeepestException_Test {

    /**
     * Simple custom exception used for the “custom exception types” test cases.
     */
    static class MyCustomException extends Exception {

        MyCustomException(String message) {
            super(message);
        }

        MyCustomException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * A {@link Throwable} whose {@code getCause()} method deliberately throws
     * an {@link IllegalStateException}.  This mimics a badly‑behaving exception
     * implementation and lets us verify that {@code getDeepestException} does
     * not swallow such errors.
     */
    static class ThrowingCauseException extends Throwable {

        @Override
        public synchronized Throwable getCause() {
            throw new IllegalStateException("Intentional exception from getCause()");
        }
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of(new Exception("root"), Exception.class, "When the Throwable has no cause, the method should return the original Throwable."), Arguments.of(new Exception("level1", new IllegalArgumentException("level2")), IllegalArgumentException.class, "Single cause – should return the deepest IllegalArgumentException."), Arguments.of(new RuntimeException("top", new Exception("mid", new NullPointerException("bottom"))), NullPointerException.class, "Multiple causes – should return the deepest NullPointerException."));
    }

    private static Stream<Arguments> customExceptionProvider() {
        return Stream.of(Arguments.of(new Exception("top", new MyCustomException("deep")), MyCustomException.class, "Custom exception should be returned as deepest cause."), Arguments.of(new RuntimeException("outer", new MyCustomException("inner", new IllegalArgumentException("root"))), IllegalArgumentException.class, "Mixed custom and standard exceptions – deepest should be IllegalArgumentException."));
    }

    /**
     * Verify that a single exception (no cause) is returned unchanged.
     */
    @Test
    void shouldReturnSameExceptionWhenNoCause() {
        IllegalArgumentException ex = new IllegalArgumentException("root cause");
        Throwable deepest = ExceptionUtilities.getDeepestException(ex);
        assertSame(ex, deepest, "When there is no cause the original exception should be returned");
    }

    /**
     * Verify that the deepest exception in a chain is returned.
     */
    @Test
    void shouldReturnDeepestExceptionInChain() {
        IllegalArgumentException leaf = new IllegalArgumentException("leaf message");
        RuntimeException middle = new RuntimeException("middle", leaf);
        Exception top = new Exception("top", middle);
        Throwable deepest = ExceptionUtilities.getDeepestException(top);
        assertSame(leaf, deepest, "The deepest (leaf) exception should be returned");
        assertTrue(deepest.getMessage().contains("leaf message"));
    }

    /**
     * Verify that a chain with a null cause somewhere in the middle stops correctly.
     */
    @Test
    void shouldStopAtFirstNullCause() {
        // leaf has no cause
        NullPointerException leaf = new NullPointerException("leaf");
        // middle wraps leaf but its cause is explicitly set to null afterwards
        IllegalStateException middle = new IllegalStateException("middle", leaf);
        // break the chain
        middle.initCause(null);
        // top wraps middle
        Exception top = new Exception("top", middle);
        Throwable deepest = ExceptionUtilities.getDeepestException(top);
        // Because middle's cause was cleared, deepest should be middle, not leaf.
        assertSame(middle, deepest, "The deepest exception should be the first one whose cause is null");
    }

    /**
     * Verify that passing {@code null} results in a {@link NullPointerException}.
     * The utility method does not guard against a null argument.
     */
    @Test
    void shouldThrowNPEWhenArgumentIsNull() {
        assertThrows(NullPointerException.class, () -> ExceptionUtilities.getDeepestException(null), "Calling getDeepestException with null should throw NPE");
    }

    /**
     * Parameterized test covering the basic functionality scenarios
     * (no cause, single cause, multi‑level cause).
     */
    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("basicFunctionalityProvider")
    void shouldFindDeepestException_BasicFunctionality(Throwable input, Class<? extends Throwable> expectedClass, String description) {
        Throwable result = ExceptionUtilities.getDeepestException(input);
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedClass, result.getClass(), () -> "Expected deepest exception of type " + expectedClass.getSimpleName() + " for case: " + description);
        // For the simple “no‑cause” case we also verify that the same instance is returned.
        if (expectedClass == input.getClass()) {
            assertSame(input, result, "When there is no cause the original instance should be returned");
        }
    }

    /**
     * Parameterized test for custom exception types that extend {@link Exception}.
     */
    @ParameterizedTest(name = "{index} => {2}")
    @MethodSource("customExceptionProvider")
    void shouldFindDeepestException_CustomExceptionTypes(Throwable input, Class<? extends Throwable> expectedClass, String description) {
        Throwable result = ExceptionUtilities.getDeepestException(input);
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedClass, result.getClass(), () -> "Expected deepest exception of type " + expectedClass.getSimpleName() + " for case: " + description);
    }

    /**
     * Verify that a {@link Throwable} whose {@code getCause()} method throws an
     * {@link IllegalStateException} propagates that exception out of
     * {@code getDeepestException}.
     */
    @Test
    void shouldPropagateExceptionWhenGetCauseThrows() {
        // Directly passing the throwing throwable.
        assertThrows(IllegalStateException.class, () -> ExceptionUtilities.getDeepestException(new ThrowingCauseException()), "The IllegalStateException thrown by getCause() must propagate.");
        // Wrapped inside another exception – the traversal should still propagate.
        Exception outer = new Exception("outer", new ThrowingCauseException());
        assertThrows(IllegalStateException.class, () -> ExceptionUtilities.getDeepestException(outer), "Propagation must work even when the throwing throwable is a cause of another exception.");
    }
}
