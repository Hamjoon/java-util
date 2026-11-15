package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.Callable;

/**
 * JUnit 4 test suite for {@link ExceptionUtilities}.
 */
public class ExceptionUtilitiesTest {

    /* -------------------------------------------------
     *  Public method signatures extracted from ExceptionUtilities
     * -------------------------------------------------
     *
     * public static Throwable getDeepestException(Throwable e)
     *
     * public static <T> T safelyIgnoreException(Callable<T> callable, T defaultValue)
     *
     * public static void safelyIgnoreException(Runnable runnable)
     *
     * public static void safelyIgnoreException(Throwable t)
     *
     */

    /* -------------------------------------------------
     *  Tests for getDeepestException
     * ------------------------------------------------- */

    @Test
    public void testGetDeepestException_SingleException() {
        Throwable root = new IllegalArgumentException("root");
        Throwable result = ExceptionUtilities.getDeepestException(root);
        assertSame("Should return the same exception when there is no cause", root, result);
    }

    @Test
    public void testGetDeepestException_NestedCauses() {
        Throwable deepest = new IllegalStateException("deepest");
        Throwable middle = new RuntimeException("middle", deepest);
        Throwable top = new Exception("top", middle);

        Throwable result = ExceptionUtilities.getDeepestException(top);
        assertSame("Should return the most nested cause", deepest, result);
    }

    @Test(expected = NullPointerException.class)
    public void testGetDeepestException_NullArgument() {
        ExceptionUtilities.getDeepestException(null);
    }

    /* -------------------------------------------------
     *  Tests for safelyIgnoreException(Callable<T>, T)
     * ------------------------------------------------- */

    @Test
    public void testSafelyIgnoreException_CallableSuccess() throws Exception {
        Callable<String> callable = () -> "success";
        String result = ExceptionUtilities.safelyIgnoreException(callable, "default");
        assertEquals("Should return callable's result when no exception is thrown", "success", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsCheckedException() {
        Callable<String> callable = () -> {
            throw new Exception("checked");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        assertEquals("Should return default value when callable throws a checked exception", "fallback", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsRuntimeException() {
        Callable<Integer> callable = () -> {
            throw new IllegalArgumentException("runtime");
        };
        Integer result = ExceptionUtilities.safelyIgnoreException(callable, 42);
        assertEquals("Should return default value when callable throws a RuntimeException", Integer.valueOf(42), result);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsError() {
        Callable<Double> callable = () -> {
            throw new AssertionError("error");
        };
        Double result = ExceptionUtilities.safelyIgnoreException(callable, 3.14);
        assertEquals("Should return default value when callable throws an Error", Double.valueOf(3.14), result);
    }

    @Test(expected = NullPointerException.class)
    public void testSafelyIgnoreException_CallableNull() {
        ExceptionUtilities.safelyIgnoreException(null, "default");
    }

    @Test
    public void testSafelyIgnoreException_CallableReturnsNullDefaultIsNull() {
        Callable<Object> callable = () -> null;
        Object result = ExceptionUtilities.safelyIgnoreException(callable, null);
        assertNull("Should correctly return null when callable returns null and default is null", result);
    }

    /* -------------------------------------------------
     *  Tests for safelyIgnoreException(Runnable)
     * ------------------------------------------------- */

    @Test
    public void testSafelyIgnoreException_RunnableSuccess() {
        final boolean[] ran = {false};
        Runnable runnable = () -> ran[0] = true;
        ExceptionUtilities.safelyIgnoreException(runnable);
        assertTrue("Runnable should have been executed", ran[0]);
    }

    @Test
    public void testSafelyIgnoreException_RunnableThrowsRuntimeException() {
        Runnable runnable = () -> { throw new IllegalStateException("runtime"); };
        // No exception should propagate
        ExceptionUtilities.safelyIgnoreException(runnable);
    }

    @Test
    public void testSafelyIgnoreException_RunnableThrowsError() {
        Runnable runnable = () -> { throw new StackOverflowError("error"); };
        // No exception should propagate
        ExceptionUtilities.safelyIgnoreException(runnable);
    }

    @Test(expected = NullPointerException.class)
    public void testSafelyIgnoreException_RunnableNull() {
        ExceptionUtilities.safelyIgnoreException((Runnable) null);
    }

    /* -------------------------------------------------
     *  Tests for safelyIgnoreException(Throwable)
     * ------------------------------------------------- */

    @Test(expected = OutOfMemoryError.class)
    public void testSafelyIgnoreException_ThrowableOutOfMemoryErrorRethrown() {
        OutOfMemoryError oom = new OutOfMemoryError("oom");
        ExceptionUtilities.safelyIgnoreException(oom);
    }

    @Test
    public void testSafelyIgnoreException_ThrowableOtherIgnored() {
        RuntimeException runtime = new RuntimeException("runtime");
        // Should be ignored – no exception propagates
        ExceptionUtilities.safelyIgnoreException(runtime);
    }

    @Test
    public void testSafelyIgnoreException_ThrowableNullIgnored() {
        // Passing null should simply do nothing (no NPE)
        ExceptionUtilities.safelyIgnoreException((Throwable) null);
    }
}
