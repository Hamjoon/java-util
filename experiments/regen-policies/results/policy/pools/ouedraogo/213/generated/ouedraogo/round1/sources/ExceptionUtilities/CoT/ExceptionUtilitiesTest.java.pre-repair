package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.concurrent.Callable;

/**
 * JUnit 4 test cases for {@link ExceptionUtilities}.
 */
public class ExceptionUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /* 1. Public method signatures extracted from ExceptionUtilities          */
    /* ---------------------------------------------------------------------- */
    // public static Throwable getDeepestException(Throwable e)
    // public static <T> T safelyIgnoreException(Callable<T> callable, T defaultValue)
    // public static void safelyIgnoreException(Runnable runnable)
    // public static void safelyIgnoreException(Throwable t)

    /* ---------------------------------------------------------------------- */
    /* 2. Basic functionality tests                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDeepestException_SingleException() {
        Throwable ex = new IllegalArgumentException("single");
        Throwable deepest = ExceptionUtilities.getDeepestException(ex);
        // should return the same instance when there is no cause
        org.junit.Assert.assertSame("Deepest exception should be the original when no cause",
                ex, deepest);
    }

    @Test
    public void testGetDeepestException_NestedExceptions() {
        Throwable root = new IllegalStateException("root");
        Throwable middle = new RuntimeException("middle", root);
        Throwable top = new Exception("top", middle);

        Throwable deepest = ExceptionUtilities.getDeepestException(top);
        org.junit.Assert.assertSame("Deepest exception should be the innermost cause", root, deepest);
    }

    @Test(expected = NullPointerException.class)
    public void testGetDeepestException_NullArgument() {
        ExceptionUtilities.getDeepestException(null);
    }

    @Test
    public void testSafelyIgnoreException_CallableSuccess() throws Exception {
        Callable<String> callable = () -> "OK";
        String result = ExceptionUtilities.safelyIgnoreException(callable, "DEFAULT");
        org.junit.Assert.assertEquals("Result should be the callable's return value", "OK", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsRuntimeException() {
        Callable<String> callable = () -> {
            throw new IllegalArgumentException("boom");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "DEFAULT");
        org.junit.Assert.assertEquals("Result should be the default when callable throws", "DEFAULT", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsError() {
        Callable<String> callable = () -> {
            throw new OutOfMemoryError("oom");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "DEFAULT");
        org.junit.Assert.assertEquals("Result should be the default when callable throws an Error", "DEFAULT", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableReturnsNull() {
        Callable<String> callable = () -> null;
        String result = ExceptionUtilities.safelyIgnoreException(callable, "DEFAULT");
        org.junit.Assert.assertNull("Result should be null when callable returns null", result);
    }

    @Test(expected = NullPointerException.class)
    public void testSafelyIgnoreException_CallableIsNull() {
        ExceptionUtilities.safelyIgnoreException(null, "DEFAULT");
    }

    @Test
    public void testSafelyIgnoreException_RunnableRunsNormally() {
        final boolean[] executed = {false};
        Runnable runnable = () -> executed[0] = true;
        ExceptionUtilities.safelyIgnoreException(runnable);
        org.junit.Assert.assertTrue("Runnable should have been executed", executed[0]);
    }

    @Test
    public void testSafelyIgnoreException_RunnableThrowsException() {
        Runnable runnable = () -> {
            throw new IllegalStateException("oops");
        };
        // No exception should propagate
        ExceptionUtilities.safelyIgnoreException(runnable);
        // If we reach here the test passes
    }

    @Test(expected = NullPointerException.class)
    public void testSafelyIgnoreException_RunnableIsNull() {
        ExceptionUtilities.safelyIgnoreException((Runnable) null);
    }

    @Test(expected = OutOfMemoryError.class)
    public void testSafelyIgnoreException_ThrowableOutOfMemoryError() {
        OutOfMemoryError oom = new OutOfMemoryError("out of memory");
        ExceptionUtilities.safelyIgnoreException(oom);
    }

    @Test
    public void testSafelyIgnoreException_ThrowableOtherError() {
        // Should be ignored (i.e., no rethrow)
        StackOverflowError error = new StackOverflowError("stack overflow");
        ExceptionUtilities.safelyIgnoreException(error);
        // If no exception propagates, test passes
    }

    @Test
    public void testSafelyIgnoreException_ThrowableRuntimeException() {
        RuntimeException rex = new RuntimeException("runtime");
        ExceptionUtilities.safelyIgnoreException(rex);
        // No exception should be thrown
    }

    @Test
    public void testSafelyIgnoreException_ThrowableNull() {
        // Passing null should be a no‑op and must not throw
        ExceptionUtilities.safelyIgnoreException((Throwable) null);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. Edge‑case and exception handling scenarios                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDeepestException_CycleInCauses() {
        // Create a cycle: a -> b -> a
        Throwable a = new IllegalArgumentException("a");
        Throwable b = new IllegalStateException("b", a);
        // artificially create a cycle
        a.initCause(b);

        // The method will loop until getCause() == null, but because of the cycle,
        // it will become an infinite loop in real code. To avoid hanging the test,
        // we run it in a separate thread with a timeout.
        Thread t = new Thread(() -> {
            ExceptionUtilities.getDeepestException(a);
        });
        t.start();
        try {
            t.join(100); // wait 100 ms
        } catch (InterruptedException ignored) {
        }
        org.junit.Assert.assertTrue("Method should not hang indefinitely on a cause cycle", !t.isAlive());
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsCheckedException() {
        Callable<String> callable = () -> {
            throw new java.io.IOException("checked");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        org.junit.Assert.assertEquals("Fallback should be returned when callable throws checked exception",
                "fallback", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableWithNullDefault() {
        Callable<String> callable = () -> {
            throw new RuntimeException("fail");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, null);
        org.junit.Assert.assertNull("Result should be null when defaultValue is null", result);
    }
}
