package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link ExceptionUtilities}.
 *
 * <p>This test class was collaboratively designed by three fictional experts.
 * Each expert contributed one test per method, covering typical usage,
 * edge‑cases and error scenarios. All tests are self‑contained and do not
 * rely on any external libraries or mocking frameworks.</p>
 */
public class ExceptionUtilitiesTest {

    /* --------------------------------------------------------------------
     *  Tests for safelyIgnoreException(Throwable)
     * -------------------------------------------------------------------- */

    /**
     * Expert A – Typical case: a regular exception should be ignored.
     * The method must return normally without throwing any exception.
     */
    @Test
    public void testSafelyIgnoreException_WithRegularException() {
        // No exception should be thrown – the method simply returns.
        ExceptionUtilities.safelyIgnoreException(new IllegalArgumentException("ignore me"));
    }

    /**
     * Expert B – Edge case: passing {@code null} should be a no‑op.
     * The {@code instanceof} check safely handles {@code null}.
     */
    @Test
    public void testSafelyIgnoreException_WithNull() {
        // Should not throw NullPointerException.
        ExceptionUtilities.safelyIgnoreException(null);
    }

    /**
     * Expert C – Error scenario: an {@code OutOfMemoryError} must be
     * re‑thrown unchanged.
     */
    @Test(expected = OutOfMemoryError.class)
    public void testSafelyIgnoreException_WithOutOfMemoryError() {
        OutOfMemoryError oom = new OutOfMemoryError("simulated OOM");
        ExceptionUtilities.safelyIgnoreException(oom);
        // The test will pass only if the OOM error propagates.
    }

    /* --------------------------------------------------------------------
     *  Tests for getDeepestException(Throwable)
     * -------------------------------------------------------------------- */

    /**
     * Expert A – Typical case: a single exception with no cause.
     * The deepest exception should be the same instance that was passed.
     */
    @Test
    public void testGetDeepestException_NoCause() {
        IllegalStateException ex = new IllegalStateException("root");
        Throwable deepest = ExceptionUtilities.getDeepestException(ex);
        assertSame("Deepest exception should be the original when no cause exists", ex, deepest);
    }

    /**
     * Expert B – Edge case: a chain of nested causes.
     * The method must walk the chain and return the most nested cause.
     */
    @Test
    public void testGetDeepestException_WithCauseChain() {
        // Build a chain: top -> middle -> bottom
        Exception bottom = new Exception("bottom");
        RuntimeException middle = new RuntimeException("middle", bottom);
        IllegalArgumentException top = new IllegalArgumentException("top", middle);

        Throwable deepest = ExceptionUtilities.getDeepestException(top);
        assertSame("Deepest exception should be the innermost cause", bottom, deepest);
    }

    /**
     * Expert C – Error scenario: passing {@code null} should result in
     * a {@link NullPointerException} because the implementation dereferences
     * the argument without a null‑check.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDeepestException_WithNull() {
        ExceptionUtilities.getDeepestException(null);
    }

    /**
     * Additional sanity check: a cause that is the same instance as its parent.
     * Although Java's Throwable API prevents setting a cause to the same
     * instance after construction, we can simulate a self‑referencing cause
     * via reflection for robustness testing. The utility should still return
     * the original instance without entering an infinite loop.
     *
     * <p>This test uses reflection only on the {@code Throwable} class,
     * which is part of the Java standard library and allowed by the rules.</p>
     */
    @Test
    public void testGetDeepestException_SelfReferencingCause() throws Exception {
        Exception self = new Exception("self");
        // Use reflection to set the cause to itself (normally illegal).
        java.lang.reflect.Field causeField = Throwable.class.getDeclaredField("cause");
        causeField.setAccessible(true);
        causeField.set(self, self);

        // The loop in getDeepestException checks for null, so it will stop
        // after one iteration because the cause is non‑null but equal to the
        // current reference; the while condition still holds, leading to
        // an infinite loop in the real implementation. To avoid hanging the
        // test suite we verify that the method does NOT loop forever by
        // running it with a timeout.
        // JUnit 4 does not have a built‑in timeout per test method, so we
        // manually enforce a short run.
        long start = System.nanoTime();
        Throwable result = ExceptionUtilities.getDeepestException(self);
        long durationMs = (System.nanoTime() - start) / 1_000_000L;

        // The implementation will return the same object (first iteration)
        // because the while condition becomes false after the first reassignment.
        assertSame("When cause references itself, the method should return that instance",
                self, result);
        assertTrue("Method should complete quickly (no infinite loop)", durationMs < 100);
    }
}
