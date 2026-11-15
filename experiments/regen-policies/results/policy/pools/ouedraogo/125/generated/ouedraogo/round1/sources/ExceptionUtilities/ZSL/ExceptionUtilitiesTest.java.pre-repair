package com.cedarsoftware.util;

import org.junit.Test;

import java.util.concurrent.Callable;

import static org.junit.Assert.*;

public class ExceptionUtilitiesTest {

    /* -------------------------------------------------------
     *  Tests for getDeepestException
     * ------------------------------------------------------- */

    @Test
    public void getDeepestException_NoCause_ReturnsSelf() {
        Exception root = new Exception("root");
        Throwable deepest = ExceptionUtilities.getDeepestException(root);
        assertSame("When there is no cause the method should return the original exception",
                root, deepest);
    }

    @Test
    public void getDeepestException_MultipleNestedCauses_ReturnsMostNested() {
        Exception level3 = new Exception("level3");
        Exception level2 = new Exception("level2", level3);
        Exception level1 = new Exception("level1", level2);

        Throwable deepest = ExceptionUtilities.getDeepestException(level1);
        assertSame("The deepest cause should be the most nested exception", level3, deepest);
    }

    @Test
    public void getDeepestException_NullCauseAtMiddle_ReturnsFirstNonNullCause() {
        // level2 has a null cause, level1 points to level2
        Exception level2 = new Exception("level2", null);
        Exception level1 = new Exception("level1", level2);

        Throwable deepest = ExceptionUtilities.getDeepestException(level1);
        assertSame("When a cause chain terminates early, the last non‑null cause is returned",
                level2, deepest);
    }

    /* -------------------------------------------------------
     *  Tests for safelyIgnoreException(Callable,T)
     * ------------------------------------------------------- */

    @Test
    public void safelyIgnoreExceptionCallable_ReturnsCallableResultWhenNoException() throws Exception {
        Callable<String> callable = () -> "success";
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        assertEquals("When callable succeeds its result should be returned", "success", result);
    }

    @Test
    public void safelyIgnoreExceptionCallable_ReturnsDefaultWhenCheckedExceptionThrown() {
        Callable<String> callable = () -> {
            throw new Exception("checked");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        assertEquals("When callable throws a checked exception the default value must be returned",
                "fallback", result);
    }

    @Test
    public void safelyIgnoreExceptionCallable_ReturnsDefaultWhenRuntimeExceptionThrown() {
        Callable<Integer> callable = () -> {
            throw new IllegalArgumentException("runtime");
        };
        Integer result = ExceptionUtilities.safelyIgnoreException(callable, 42);
        assertEquals("When callable throws a runtime exception the default value must be returned",
                Integer.valueOf(42), result);
    }

    @Test
    public void safelyIgnoreExceptionCallable_ReturnsDefaultWhenErrorThrown() {
        Callable<String> callable = () -> {
            throw new AssertionError("error");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "default");
        assertEquals("When callable throws an Error the default value must be returned",
                "default", result);
    }

    @Test
    public void safelyIgnoreExceptionCallable_ReturnsNullDefaultWhenDefaultIsNull() {
        Callable<String> callable = () -> {
            throw new Exception("anything");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, null);
        assertNull("When the supplied default value is null it should be returned as‑is", result);
    }

    @Test(expected = NullPointerException.class)
    public void safelyIgnoreExceptionCallable_NullCallableThrowsNPE() {
        // The implementation does not guard against null, so a NPE is expected.
        ExceptionUtilities.safelyIgnoreException(null, "ignored");
    }

    /* -------------------------------------------------------
     *  Tests for safelyIgnoreException(Runnable)
     * ------------------------------------------------------- */

    @Test
    public void safelyIgnoreExceptionRunnable_ExecutesWhenNoException() {
        final boolean[] executed = {false};
        Runnable runnable = () -> executed[0] = true;

        ExceptionUtilities.safelyIgnoreException(runnable);
        assertTrue("Runnable should have been executed", executed[0]);
    }

    @Test
    public void safelyIgnoreExceptionRunnable_IgnoresThrownException() {
        final boolean[] executed = {false};
        Runnable runnable = () -> {
            executed[0] = true;
            throw new RuntimeException("intentional");
        };

        // No exception should propagate out of the method.
        ExceptionUtilities.safelyIgnoreException(runnable);
        assertTrue("Runnable should have been executed before the exception", executed[0]);
        // If an exception had bubbled up the test would have failed.
    }

    /* -------------------------------------------------------
     *  Tests for safelyIgnoreException(Throwable)
     * ------------------------------------------------------- */

    @Test
    public void safelyIgnoreExceptionThrowable_IgnoresNonCriticalThrowable() {
        // No exception should be thrown for a regular Exception.
        ExceptionUtilities.safelyIgnoreException(new Exception("just a test"));
        // If the method threw, the test would fail.
    }

    @Test(expected = OutOfMemoryError.class)
    public void safelyIgnoreExceptionThrowable_RethrowsOutOfMemoryError() {
        // The method must rethrow OutOfMemoryError unchanged.
        ExceptionUtilities.safelyIgnoreException(new OutOfMemoryError("OOM"));
    }

    @Test
    public void safelyIgnoreExceptionThrowable_DoesNotAffectOtherErrors() {
        // Verify that an Error other than OutOfMemoryError is ignored.
        try {
            ExceptionUtilities.safelyIgnoreException(new AssertionError("assert"));
        } catch (Throwable t) {
            fail("AssertionError should be ignored, not rethrown");
        }
    }
}
