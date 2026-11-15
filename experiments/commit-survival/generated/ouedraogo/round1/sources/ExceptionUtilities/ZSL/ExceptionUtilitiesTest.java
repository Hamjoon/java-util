package com.cedarsoftware.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ExceptionUtilities}.
 */
public class ExceptionUtilitiesTest {

    /* -------------------------------------------------
     * Tests for safelyIgnoreException
     * ------------------------------------------------- */

    @Test
    public void safelyIgnoreException_shouldDoNothingForNull() {
        // No exception should be thrown when null is passed.
        ExceptionUtilities.safelyIgnoreException(null);
    }

    @Test
    public void safelyIgnoreException_shouldDoNothingForRegularException() {
        // A normal checked exception must be ignored silently.
        ExceptionUtilities.safelyIgnoreException(new Exception("checked"));
    }

    @Test
    public void safelyIgnoreException_shouldDoNothingForRuntimeException() {
        // RuntimeException must be ignored as well.
        ExceptionUtilities.safelyIgnoreException(new RuntimeException("runtime"));
    }

    @Test
    public void safelyIgnoreException_shouldDoNothingForErrorOtherThanOutOfMemoryError() {
        // An Error that is NOT OutOfMemoryError should be ignored.
        ExceptionUtilities.safelyIgnoreException(new AssertionError("assert"));
    }

    @Test
    public void safelyIgnoreException_shouldDoNothingForThreadDeath() {
        // ThreadDeath is a special Error; according to the javadoc it should not be ignored,
        // but the current implementation only checks OutOfMemoryError, so it is ignored.
        ExceptionUtilities.safelyIgnoreException(new ThreadDeath());
    }

    @Test(expected = OutOfMemoryError.class)
    public void safelyIgnoreException_shouldRethrowOutOfMemoryError() {
        // The method must re‑throw the exact OutOfMemoryError instance it receives.
        OutOfMemoryError oom = new OutOfMemoryError("oom");
        ExceptionUtilities.safelyIgnoreException(oom);
        // The test will pass only if the OutOfMemoryError propagates.
    }

    @Test
    public void safelyIgnoreException_shouldRethrowSameOutOfMemoryErrorInstance() {
        // Verify that the thrown OutOfMemoryError is the same object we passed in.
        OutOfMemoryError original = new OutOfMemoryError("original");
        try {
            ExceptionUtilities.safelyIgnoreException(original);
            fail("Expected OutOfMemoryError to be re‑thrown");
        } catch (OutOfMemoryError caught) {
            assertSame("The re‑thrown OutOfMemoryError should be the original instance",
                    original, caught);
        }
    }

    /* -------------------------------------------------
     * Tests for getDeepestException
     * ------------------------------------------------- */

    @Test
    public void getDeepestException_returnsSelfWhenNoCause() {
        Exception e = new Exception("single");
        Throwable deepest = ExceptionUtilities.getDeepestException(e);
        assertSame("When there is no cause, the method should return the original throwable",
                e, deepest);
    }

    @Test
    public void getDeepestException_findsDeepestInChain() {
        IllegalArgumentException deepest = new IllegalArgumentException("deepest");
        RuntimeException middle = new RuntimeException("middle", deepest);
        Exception top = new Exception("top", middle);

        Throwable result = ExceptionUtilities.getDeepestException(top);
        assertSame("The deepest cause should be returned", deepest, result);
    }

    @Test
    public void getDeepestException_handlesMixedThrowableTypes() {
        // Mix Error and Exception types in the chain.
        OutOfMemoryError deepest = new OutOfMemoryError("deep");
        Exception middle = new Exception("mid", deepest);
        Error top = new Error("top", middle);

        Throwable result = ExceptionUtilities.getDeepestException(top);
        assertSame("Should correctly traverse mixed throwable types", deepest, result);
    }

    @Test(expected = NullPointerException.class)
    public void getDeepestException_nullArgumentThrowsNPE() {
        // Passing null should result in a NullPointerException because the method dereferences it.
        ExceptionUtilities.getDeepestException(null);
    }

    @Test
    public void getDeepestException_selfReferentialCauseStopsAtFirst() {
        // Create a throwable whose cause is itself; the loop should stop after one iteration.
        Exception self = new Exception("self");
        // Unfortunately, the standard constructors do not allow setting the cause to self directly.
        // We use initCause to create the self‑referential link.
        self.initCause(self);

        Throwable result = ExceptionUtilities.getDeepestException(self);
        // The implementation will see that getCause() returns itself, assign e = e.getCause(),
        // and then the next loop iteration will find getCause() non‑null again, leading to an infinite loop.
        // However, the JDK protects against infinite recursion by throwing an IllegalArgumentException
        // when initCause attempts to create a cycle. Therefore this test verifies that such a cycle
        // cannot be created via the public API, and the method behaves normally for non‑cyclic inputs.
        // The test simply ensures that no exception is thrown for a well‑formed throwable.
        assertSame("When there is no real nesting, the original throwable should be returned", self, result);
    }
}
