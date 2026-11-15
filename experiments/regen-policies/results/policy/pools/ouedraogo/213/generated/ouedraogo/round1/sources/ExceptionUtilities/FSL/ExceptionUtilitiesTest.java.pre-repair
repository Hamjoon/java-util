package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.concurrent.Callable;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ExceptionUtilities}.
 */
public class ExceptionUtilitiesTest {

    /* --------------------------------------------------------------
     * Tests for getDeepestException
     * ------------------------------------------------------------ */

    @Test
    public void getDeepestException_SingleException_NoCause() {
        Throwable e = new IllegalArgumentException("root");
        Throwable deepest = ExceptionUtilities.getDeepestException(e);
        assertSame("When there is no cause, the deepest exception should be the original one",
                e, deepest);
    }

    @Test
    public void getDeepestException_MultiLevelCause() {
        Throwable leaf = new IllegalStateException("leaf");
        Throwable middle = new RuntimeException("middle", leaf);
        Throwable top = new Exception("top", middle);

        Throwable deepest = ExceptionUtilities.getDeepestException(top);
        assertSame("The deepest exception must be the most nested cause", leaf, deepest);
    }

    @Test(expected = NullPointerException.class)
    public void getDeepestException_NullArgument_ThrowsNPE() {
        ExceptionUtilities.getDeepestException(null);
    }

    /* --------------------------------------------------------------
     * Tests for safelyIgnoreException(Callable<T>, T)
     * ------------------------------------------------------------ */

    @Test
    public void safelyIgnoreException_CallableReturnsValue() throws Exception {
        Callable<String> callable = () -> "expected";
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        assertEquals("When callable succeeds, its result must be returned", "expected", result);
    }

    @Test
    public void safelyIgnoreException_CallableThrowsRuntimeException_ReturnsDefault() {
        Callable<Integer> callable = () -> {
            throw new IllegalArgumentException("boom");
        };
        Integer result = ExceptionUtilities.safelyIgnoreException(callable, 42);
        assertEquals("When callable throws a RuntimeException, default value should be returned", Integer.valueOf(42), result);
    }

    @Test
    public void safelyIgnoreException_CallableThrowsError_ReturnsDefault() {
        Callable<String> callable = () -> {
            throw new AssertionError("serious error");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "default");
        assertEquals("When callable throws an Error, default value should be returned", "default", result);
    }

    @Test
    public void safelyIgnoreException_CallableThrowsCheckedException_ReturnsDefault() {
        Callable<Double> callable = () -> {
            throw new Exception("checked");
        };
        Double result = ExceptionUtilities.safelyIgnoreException(callable, 3.14);
        assertEquals("When callable throws a checked Exception, default value should be returned", Double.valueOf(3.14), result);
    }

    @Test(expected = NullPointerException.class)
    public void safelyIgnoreException_CallableIsNull_ThrowsNPE() {
        // The implementation does not guard against null, so a NPE is expected.
        ExceptionUtilities.safelyIgnoreException(null, "default");
    }

    /* --------------------------------------------------------------
     * Tests for safelyIgnoreException(Runnable)
     * ------------------------------------------------------------ */

    @Test
    public void safelyIgnoreException_RunnableRunsNormally() {
        final boolean[] executed = {false};
        Runnable r = () -> executed[0] = true;
        ExceptionUtilities.safelyIgnoreException(r);
        assertTrue("Runnable should have been executed", executed[0]);
    }

    @Test
    public void safelyIgnoreException_RunnableThrowsRuntimeException_IsIgnored() {
        Runnable r = () -> {
            throw new IllegalStateException("ignore me");
        };
        // No exception should escape the method.
        ExceptionUtilities.safelyIgnoreException(r);
    }

    @Test
    public void safelyIgnoreException_RunnableThrowsError_IsIgnored() {
        Runnable r = () -> {
            throw new StackOverflowError("ignore this error");
        };
        // The error is swallowed; test passes if no exception propagates.
        ExceptionUtilities.safelyIgnoreException(r);
    }

    /* --------------------------------------------------------------
     * Tests for safelyIgnoreException(Throwable)
     * ------------------------------------------------------------ */

    @Test(expected = OutOfMemoryError.class)
    public void safelyIgnoreException_OutOfMemoryError_IsRethrown() {
        OutOfMemoryError oom = new OutOfMemoryError("critical");
        ExceptionUtilities.safelyIgnoreException(oom);
    }

    @Test
    public void safelyIgnoreException_NonCriticalThrowable_IsIgnored() {
        // A generic Exception should be ignored (no re‑throw).
        Exception ex = new Exception("non‑critical");
        ExceptionUtilities.safelyIgnoreException(ex);
        // If we reach this line, the method behaved as expected.
    }

    @Test
    public void safelyIgnoreException_ThreadDeath_IsIgnored() {
        // ThreadDeath extends Error but is not specifically re‑thrown by the method.
        ThreadDeath td = new ThreadDeath();
        ExceptionUtilities.safelyIgnoreException(td);
        // No exception should be propagated.
    }

    @Test
    public void safelyIgnoreException_NullThrowable_IsIgnored() {
        // Passing null should be a no‑op (instanceof returns false).
        ExceptionUtilities.safelyIgnoreException((Throwable) null);
        // No exception is expected.
    }
}
