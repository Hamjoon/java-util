package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ExceptionUtilities_getDeepestException_Test {

    /**
     * Custom {@link Throwable} that deliberately throws an {@link IllegalStateException}
     * from its {@code getCause()} implementation. Used to verify that
     * {@link ExceptionUtilities#getDeepestException(Throwable)} does not swallow
     * such exceptions.
     */
    private static class ThrowingCauseException extends Exception {

        @Override
        public Throwable getCause() {
            throw new IllegalStateException("Cause getter failed");
        }
    }

    private static Stream<Arguments> basicTraversalProvider() {
        // case 1 – no cause
        Exception root = new Exception("root");
        // case 2 – single level cause
        IllegalArgumentException deep = new IllegalArgumentException("deep");
        RuntimeException level1 = new RuntimeException("level1", deep);
        // case 3 – multiple levels
        NullPointerException deepest = new NullPointerException("deepest");
        IllegalStateException level2 = new IllegalStateException("level2", deepest);
        IOException level1Multi = new IOException("level1", level2);
        return Stream.of(// input, expected deepest, description
        Arguments.of(root, root, "When the supplied throwable has no cause, the method should return the same throwable."), Arguments.of(level1, deep, "When the throwable has a single cause, the method should return that cause."), Arguments.of(level1Multi, deepest, "When the throwable has a chain of multiple causes, the method should traverse to the deepest cause."));
    }

    @Test
    void getDeepestException_returnsSameWhenNoCause() {
        // GIVEN
        Throwable original = new IllegalArgumentException("root");
        // WHEN
        Throwable result = ExceptionUtilities.getDeepestException(original);
        // THEN
        assertSame(original, result, "When there is no cause the original exception should be returned");
    }

    @Test
    void getDeepestException_returnsDeepestCause() {
        // GIVEN
        Throwable deepest = new IllegalStateException("deepest");
        Throwable middle = new RuntimeException("middle", deepest);
        Throwable top = new Exception("top", middle);
        // WHEN
        Throwable result = ExceptionUtilities.getDeepestException(top);
        // THEN
        assertSame(deepest, result, "The deepest cause in the chain should be returned");
    }

    @Test
    void getDeepestException_handlesSingleLevelCause() {
        // GIVEN
        Throwable cause = new NullPointerException("cause");
        Throwable wrapper = new Exception("wrapper", cause);
        // WHEN
        Throwable result = ExceptionUtilities.getDeepestException(wrapper);
        // THEN
        assertSame(cause, result, "When there is a single level cause, that cause should be returned");
    }

    @Test
    void getDeepestException_throwsNullPointerExceptionWhenInputIsNull() {
        // GIVEN
        Throwable nullThrowable = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ExceptionUtilities.getDeepestException(nullThrowable), "Passing null should result in a NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Group: getDeepestException - basic traversal
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicTraversalProvider")
    void getDeepestException_basicTraversal(Throwable input, Throwable expected, String description) {
        // WHEN
        Throwable result = ExceptionUtilities.getDeepestException(input);
        // THEN
        assertSame(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Group: getDeepestException - null input
     * -------------------------------------------------------------- */
    @Test
    void getDeepestException_nullInput_throwsNullPointerException() {
        // GIVEN
        Throwable nullThrowable = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ExceptionUtilities.getDeepestException(nullThrowable), "Passing null should result in a NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Group: getDeepestException - cause getter exception
     * -------------------------------------------------------------- */
    @Test
    void getDeepestException_causeGetterThrows_propagatesException() {
        // GIVEN
        Throwable throwing = new ThrowingCauseException();
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> ExceptionUtilities.getDeepestException(throwing), "When getCause() throws, the method should propagate that exception");
        assertEquals("Cause getter failed", ex.getMessage());
    }
}
