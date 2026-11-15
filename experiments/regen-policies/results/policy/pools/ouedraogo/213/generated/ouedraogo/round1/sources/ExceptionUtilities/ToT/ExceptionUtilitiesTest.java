package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.Callable;

/**
 * Comprehensive JUnit‑4 test suite for {@link ExceptionUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical successful executions.</li>
 *   <li>Edge cases such as null arguments and null return values.</li>
 *   <li>Exception‑suppression behaviour for {@code Callable}, {@code Runnable} and {@code Throwable}.</li>
 *   <li>Verification of the deepest cause extraction logic.</li>
 * </ul>
 */
public class ExceptionUtilitiesTest {

    /* --------------------------------------------------------------
     *  getDeepestException(...)
     * -------------------------------------------------------------- */

    @Test
    public void getDeepestException_WithSingleCause_ReturnsDeepest() {
        Throwable root = new IllegalArgumentException("root");
        Throwable middle = new RuntimeException("middle", root);
        Throwable top = new Exception("top", middle);

        Throwable result = ExceptionUtilities.getDeepestException(top);
        assertSame("Should return the innermost cause", root, result);
    }

    @Test
    public void getDeepestException_WithNoCause_ReturnsSameInstance() {
        Throwable single = new IllegalStateException("lonely");
        Throwable result = ExceptionUtilities.getDeepestException(single);
        assertSame("When there is no cause, the original throwable must be returned", single, result);
    }

    @Test
    public void getDeepestException_WithCircularCause_ShouldBreakLoop() {
        // Create a circular cause chain: a -> b -> a
        Throwable a = new Exception("a");
        Throwable b = new Exception("b", a);
        // Introduce circular reference
        a.initCause(b);

        // The method uses a simple while loop; it will loop forever if the chain is circular.
        // To avoid an infinite test, we run it in a separate thread with a timeout.
        Thread t = new Thread(() -> ExceptionUtilities.getDeepestException(a));
        t.start();
        try {
            t.join(100); // 100 ms timeout
        } catch (InterruptedException ignored) { }

        assertTrue("Method should have terminated quickly despite circular reference", !t.isAlive());
    }

    /* --------------------------------------------------------------
     *  safelyIgnoreException(Callable<T>, T)
     * -------------------------------------------------------------- */

    @Test
    public void safelyIgnoreExceptionCallable_ReturnsCallableResult_OnSuccess() throws Exception {
        Callable<String> callable = () -> "expected";
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        assertEquals("Should return the callable's result when no exception is thrown", "expected", result);
    }

    @Test
    public void safelyIgnoreExceptionCallable_ReturnsDefault_OnCheckedException() {
        Callable<Integer> callable = () -> {
            throw new java.io.IOException("checked");
        };
        Integer result = ExceptionUtilities.safelyIgnoreException(callable, 42);
        assertEquals("Should return default value when callable throws a checked exception", Integer.valueOf(42), result);
    }

    @Test
    public void safelyIgnoreExceptionCallable_ReturnsDefault_OnRuntimeException() {
        Callable<String> callable = () -> {
            throw new IllegalArgumentException("runtime");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        assertEquals("Should return default value when callable throws a runtime exception", "fallback", result);
    }

    @Test
    public void safelyIgnoreExceptionCallable_ReturnsDefault_OnError() {
        Callable<String> callable = () -> {
            throw new AssertionError("error");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        assertEquals("Should return default value when callable throws an Error", "fallback", result);
    }

    @Test
    public void safelyIgnoreExceptionCallable_AllowsNullDefault() {
        Callable<String> callable = () -> {
            throw new RuntimeException("any");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, null);
        assertNull("When defaultValue is null, null must be returned on exception", result);
    }

    @Test(expected = NullPointerException.class)
    public void safelyIgnoreExceptionCallable_NullCallable_ThrowsNPE() {
        // The implementation does not guard against null, so a NPE is expected.
        ExceptionUtilities.safelyIgnoreException(null, "ignored");
    }

    @Test
    public void safelyIgnoreExceptionCallable_CallableReturnsNull() {
        Callable<Object> callable = () -> null;
        Object result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        assertNull("When callable returns null, null should be propagated (not the fallback)", result);
    }

    /* --------------------------------------------------------------
     *  safelyIgnoreException(Runnable)
     * -------------------------------------------------------------- */

    @Test
    public void safelyIgnoreExceptionRunnable_SucceedsWhenNoException() {
        final boolean[] executed = {false};
        Runnable runnable = () -> executed[0] = true;
        ExceptionUtilities.safelyIgnoreException(runnable);
        assertTrue("Runnable should have been executed", executed[0]);
    }

    @Test
    public void safelyIgnoreExceptionRunnable_IgnoresRuntimeException() {
        Runnable runnable = () -> {
            throw new IllegalStateException("runtime");
        };
        // No exception should escape
        ExceptionUtilities.safelyIgnoreException(runnable);
    }

    @Test
    public void safelyIgnoreExceptionRunnable_IgnoresError() {
        Runnable runnable = () -> {
            throw new StackOverflowError("error");
        };
        ExceptionUtilities.safelyIgnoreException(runnable);
    }

    @Test(expected = NullPointerException.class)
    public void safelyIgnoreExceptionRunnable_NullRunnable_ThrowsNPE() {
        ExceptionUtilities.safelyIgnoreException((Runnable) null);
    }

    /* --------------------------------------------------------------
     *  safelyIgnoreException(Throwable)
     * -------------------------------------------------------------- */

    @Test
    public void safelyIgnoreExceptionThrowable_DoesNothingForRegularException() {
        // No exception should be thrown; the method simply returns.
        ExceptionUtilities.safelyIgnoreException(new Exception("regular"));
    }

    @Test
    public void safelyIgnoreExceptionThrowable_RethrowsOutOfMemoryError() {
        OutOfMemoryError oome = new OutOfMemoryError("oom");
        try {
            ExceptionUtilities.safelyIgnoreException(oome);
            fail("OutOfMemoryError must be re‑thrown");
        } catch (OutOfMemoryError e) {
            assertSame("The same OutOfMemoryError instance should be re‑thrown", oome, e);
        }
    }

    @Test
    public void safelyIgnoreExceptionThrowable_IgnoresOtherErrorTypes() {
        // For any other Error subclass, the method silently ignores it.
        AssertionError error = new AssertionError("assert");
        ExceptionUtilities.safelyIgnoreException(error);
        // If we reach here, the method behaved as expected.
    }

    @Test(expected = NullPointerException.class)
    public void safelyIgnoreExceptionThrowable_NullArgument_ThrowsNPE() {
        ExceptionUtilities.safelyIgnoreException((Throwable) null);
    }
}
