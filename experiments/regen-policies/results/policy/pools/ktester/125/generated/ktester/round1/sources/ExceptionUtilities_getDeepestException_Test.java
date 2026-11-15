package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opentest4j.TestAbortedException;
import java.time.Duration;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ExceptionUtilities_getDeepestException_Test {

    /**
     * Throwable whose {@code getCause()} returns itself.
     */
    private static class SelfReferencingThrowable extends Throwable {

        @Override
        public synchronized Throwable getCause() {
            // self‑reference creates a loop
            return this;
        }
    }

    /**
     * Throwable that throws an exception when {@code getCause()} is invoked.
     */
    private static class ThrowingCauseThrowable extends Throwable {

        private final Throwable inner;

        ThrowingCauseThrowable(Throwable inner) {
            this.inner = inner;
        }

        @Override
        public synchronized Throwable getCause() {
            throw new IllegalStateException("Intentional exception from getCause()", inner);
        }
    }

    /**
     * Simple throwable that can be wired to another instance to build arbitrary
     * cause graphs (including cycles).  The default {@code getCause()} returns the
     * stored cause, which may be {@code null}.
     */
    private static class CycleThrowable extends Throwable {

        private Throwable cause;

        CycleThrowable() {
            // no‑arg constructor – cause initially null
        }

        CycleThrowable(Throwable cause) {
            this.cause = cause;
        }

        void setCause(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public synchronized Throwable getCause() {
            return cause;
        }
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> basicScenarioProvider() {
        IllegalArgumentException cause = new IllegalArgumentException("cause");
        Exception rootWithCause = new Exception("root", cause);
        Exception rootNoCause = new Exception("root");
        // chain: level1 -> level2 -> deepest
        IllegalStateException deepest = new IllegalStateException("deepest");
        RuntimeException level2 = new RuntimeException("level2", deepest);
        Exception level1 = new Exception("level1", level2);
        return Stream.of(arguments(rootNoCause, rootNoCause, "When the supplied Throwable has no cause, the original instance is returned."), arguments(rootWithCause, cause, "When the supplied Throwable has a single non‑null cause, that cause is returned."), arguments(level1, deepest, "When the supplied Throwable has a chain of causes, the deepest cause is returned."), // null argument – expect NPE
        arguments(null, null, "Passing null should result in a NullPointerException."));
    }

    @Test
    void shouldReturnDeepestCauseWhenMultipleCausesExist() {
        // given: an exception chain A -> B -> C (C is the deepest)
        IllegalArgumentException deepest = new IllegalArgumentException("Root cause");
        Exception middle = new Exception("Middle", deepest);
        Exception top = new Exception("Top", middle);
        // when: we ask for the deepest exception
        Throwable result = ExceptionUtilities.getDeepestException(top);
        // then: the result should be the deepest exception (C) and not the original (A)
        assertNotSame(top, result, "Returned throwable should not be the original top exception");
        assertSame(deepest, result, "Returned throwable should be the deepest cause");
        assertTrue(result.getMessage().contains("Root cause"));
    }

    @Test
    void shouldReturnSameThrowableWhenNoCauseIsPresent() {
        // given: a throwable with no cause
        NullPointerException single = new NullPointerException("Alone");
        // when: we ask for the deepest exception
        Throwable result = ExceptionUtilities.getDeepestException(single);
        // then: the result should be the same instance
        assertSame(single, result, "When no cause exists, the original throwable should be returned");
    }

    @Test
    void shouldHandleThrowableThatIsItsOwnCauseGracefully() {
        // given: a throwable whose cause points to itself (should not loop forever)
        RuntimeException selfReferencing = new RuntimeException("Self");
        // manually create a self‑reference (not typical, but possible via reflection)
        try {
            java.lang.reflect.Field causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
            causeField.set(selfReferencing, selfReferencing);
        } catch (Exception ignored) {
            // If reflection fails, the test is not applicable; skip the rest.
            return;
        }
        // when: we call getDeepestException
        Throwable result = ExceptionUtilities.getDeepestException(selfReferencing);
        // then: the method should return the original throwable (it stops when cause is null or same)
        assertSame(selfReferencing, result, "Self‑referencing cause should result in the original throwable");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicScenarioProvider")
    void basicScenarios(Throwable input, Throwable expected, String description) {
        // when
        Throwable result = ExceptionUtilities.getDeepestException(input);
        // then
        assertSame(expected, result, description);
    }

    @Test
    void basicScenario_nullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ExceptionUtilities.getDeepestException(null), "Method should dereference the argument without a null‑check and thus throw NPE.");
    }

    @Test
    void shouldPropagateExceptionWhenCauseGetterThrows() {
        // given: a throwable whose getCause() throws IllegalStateException
        ThrowingCauseThrowable throwing = new ThrowingCauseThrowable(new IllegalArgumentException("inner"));
        // when / then
        assertThrows(IllegalStateException.class, () -> ExceptionUtilities.getDeepestException(throwing), "If getCause() throws, the method should let the exception propagate.");
    }

    @Test
    void shouldReturnDeepestCauseWhenRootIsError() {
        // given: AssertionError (subclass of Error) with a nested cause chain
        NullPointerException deepest = new NullPointerException("deep");
        RuntimeException mid = new RuntimeException("mid", deepest);
        AssertionError root = new AssertionError("error root", mid);
        // when
        Throwable result = ExceptionUtilities.getDeepestException(root);
        // then
        assertSame(deepest, result, "Errors should be treated like any other Throwable; deepest cause must be returned.");
    }

    @Test
    void shouldTimeoutWhenSelfReferencingCause() {
        // given: a throwable whose getCause() returns itself (infinite loop)
        SelfReferencingThrowable self = new SelfReferencingThrowable();
        // when / then: the call should not terminate → JUnit reports a timeout.
        // We assert that a timeout (TestAbortedException) is thrown.
        assertThrows(TestAbortedException.class, () -> Assertions.assertTimeoutPreemptively(Duration.ofMillis(200), () -> ExceptionUtilities.getDeepestException(self)), "Self‑referencing cause should cause the method to loop forever, resulting in a timeout.");
    }

    @Test
    void shouldTimeoutWhenTwoNodeCycleExists() {
        // given: two throwables referencing each other, forming a cycle A ↔ B
        CycleThrowable a = new CycleThrowable();
        CycleThrowable b = new CycleThrowable(a);
        // complete the cycle
        a.setCause(b);
        // when / then: expect timeout
        assertThrows(TestAbortedException.class, () -> Assertions.assertTimeoutPreemptively(Duration.ofMillis(200), () -> ExceptionUtilities.getDeepestException(a)), "Two‑node cycle should cause infinite looping, leading to a timeout.");
    }

    @Test
    void shouldTimeoutWhenThreeNodeCycleExists() {
        // given: three throwables forming a cycle A → B → C → A
        CycleThrowable a = new CycleThrowable();
        CycleThrowable b = new CycleThrowable(a);
        CycleThrowable c = new CycleThrowable(b);
        // close the loop
        a.setCause(c);
        // when / then: expect timeout
        assertThrows(TestAbortedException.class, () -> Assertions.assertTimeoutPreemptively(Duration.ofMillis(200), () -> ExceptionUtilities.getDeepestException(a)), "Three‑node cycle should cause infinite looping, leading to a timeout.");
    }
}
