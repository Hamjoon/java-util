package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.concurrent.Callable;

/**
 * Comprehensive JUnit‑4 test suite for {@link ExceptionUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal behaviour of each public method.</li>
 *   <li>Edge‑case scenarios such as {@code null} arguments, deep cause chains,
 *       and the special handling of {@link OutOfMemoryError}.</li>
 *   <li>Verification that {@code safelyIgnoreException} truly suppresses all
 *       {@link Throwable}s (including {@link Error}s) except when the
 *       {@link Throwable} overload is given an {@code OutOfMemoryError},
 *       which must be re‑thrown.</li>
 * </ul>
 *
 * The test class uses only the APIs that appear in the supplied source
 * (no mocking frameworks, no external libraries).
 */
public class ExceptionUtilitiesTest {

    /* --------------------------------------------------------------
     *  Tests for getDeepestException(Throwable)
     * -------------------------------------------------------------- */

    @Test
    public void testGetDeepestException_SingleException() {
        Throwable leaf = new IllegalArgumentException("leaf");
        Throwable result = ExceptionUtilities.getDeepestException(leaf);
        Assert.assertSame("Should return the same object when there is no cause",
                leaf, result);
    }

    @Test
    public void testGetDeepestException_CauseChain() {
        Throwable deepest = new IllegalStateException("deepest");
        Throwable middle = new RuntimeException("middle", deepest);
        Throwable top = new Exception("top", middle);

        Throwable result = ExceptionUtilities.getDeepestException(top);
        Assert.assertSame("Should walk the chain and return the most nested cause",
                deepest, result);
    }

    @Test(expected = NullPointerException.class)
    public void testGetDeepestException_NullArgument() {
        ExceptionUtilities.getDeepestException(null);
    }

    /* --------------------------------------------------------------
     *  Tests for safelyIgnoreException(Callable<T>, T)
     * -------------------------------------------------------------- */

    @Test
    public void testSafelyIgnoreException_Callable_ReturnsValue() throws Exception {
        Callable<String> callable = () -> "success";
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        Assert.assertEquals("Should return the value produced by the callable",
                "success", result);
    }

    @Test
    public void testSafelyIgnoreException_Callable_ThrowsCheckedException() {
        Callable<String> callable = () -> {
            throw new java.io.IOException("checked");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        Assert.assertEquals("When callable throws, default value must be returned",
                "fallback", result);
    }

    @Test
    public void testSafelyIgnoreException_Callable_ThrowsRuntimeException() {
        Callable<Integer> callable = () -> {
            throw new IllegalArgumentException("runtime");
        };
        Integer result = ExceptionUtilities.safelyIgnoreException(callable, -1);
        Assert.assertEquals("RuntimeException should be swallowed and default returned",
                Integer.valueOf(-1), result);
    }

    @Test
    public void testSafelyIgnoreException_Callable_ThrowsError() {
        Callable<String> callable = () -> {
            throw new AssertionError("error");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "default");
        Assert.assertEquals("Error should be swallowed and default returned",
                "default", result);
    }

    @Test
    public void testSafelyIgnoreException_Callable_ThrowsOutOfMemoryError() {
        Callable<String> callable = () -> {
            throw new OutOfMemoryError("oom");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        Assert.assertEquals("OutOfMemoryError is also swallowed by this overload",
                "fallback", result);
    }

    @Test
    public void testSafelyIgnoreException_Callable_NullCallable() {
        try {
            ExceptionUtilities.safelyIgnoreException(null, "fallback");
            Assert.fail("Expected NullPointerException because callable is null");
        } catch (NullPointerException e) {
            // Expected – the implementation does not guard against null.
        }
    }

    @Test
    public void testSafelyIgnoreException_Callable_NullDefaultValue() {
        Callable<String> callable = () -> {
            throw new Exception("boom");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, null);
        Assert.assertNull("When defaultValue is null it should be returned as null",
                result);
    }

    /* --------------------------------------------------------------
     *  Tests for safelyIgnoreException(Runnable)
     * -------------------------------------------------------------- */

    @Test
    public void testSafelyIgnoreException_Runnable_ExecutesNormally() {
        final boolean[] executed = {false};
        Runnable runnable = () -> executed[0] = true;
        ExceptionUtilities.safelyIgnoreException(runnable);
        Assert.assertTrue("Runnable should have been executed", executed[0]);
    }

    @Test
    public void testSafelyIgnoreException_Runnable_ThrowsRuntimeException() {
        Runnable runnable = () -> {
            throw new IllegalStateException("runtime");
        };
        // No exception should propagate
        ExceptionUtilities.safelyIgnoreException(runnable);
    }

    @Test
    public void testSafelyIgnoreException_Runnable_ThrowsError() {
        Runnable runnable = () -> {
            throw new StackOverflowError("error");
        };
        // Should be swallowed
        ExceptionUtilities.safelyIgnoreException(runnable);
    }

    @Test
    public void testSafelyIgnoreException_Runnable_NullRunnable() {
        try {
            ExceptionUtilities.safelyIgnoreException((Runnable) null);
            Assert.fail("Expected NullPointerException because runnable is null");
        } catch (NullPointerException e) {
            // Expected – the implementation does not guard against null.
        }
    }

    /* --------------------------------------------------------------
     *  Tests for safelyIgnoreException(Throwable)
     * -------------------------------------------------------------- */

    @Test
    public void testSafelyIgnoreException_Throwable_OtherThrowable() {
        // Should be ignored – no exception propagates
        ExceptionUtilities.safelyIgnoreException(new IllegalArgumentException("ignore me"));
    }

    @Test(expected = OutOfMemoryError.class)
    public void testSafelyIgnoreException_Throwable_OutOfMemoryError() {
        // Must be re‑thrown
        ExceptionUtilities.safelyIgnoreException(new OutOfMemoryError("oom"));
    }

    @Test
    public void testSafelyIgnoreException_Throwable_Null() {
        // instanceof works with null; method should simply return
        ExceptionUtilities.safelyIgnoreException((Throwable) null);
    }
}
