package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.concurrent.Callable;

/**
 * JUnit 4 test suite for {@link ExceptionUtilities}.
 *
 * Covers:
 * <ul>
 *   <li>{@link ExceptionUtilities#getDeepestException(Throwable)}</li>
 *   <li>{@link ExceptionUtilities#safelyIgnoreException(Callable, Object)}</li>
 *   <li>{@link ExceptionUtilities#safelyIgnoreException(Runnable)}</li>
 *   <li>{@link ExceptionUtilities#safelyIgnoreException(Throwable)}</li>
 * </ul>
 *
 * The tests include typical, edge‑case and error‑scenario verification without using any
 * external mocking frameworks – only the APIs that appear in the source file are used.
 */
public class ExceptionUtilitiesTest {

    /* -------------------------------------------------------------
     *  Tests for getDeepestException(...)
     * ------------------------------------------------------------- */

    @Test
    public void testGetDeepestException_SingleException() {
        Exception e = new IllegalArgumentException("root");
        Throwable deepest = ExceptionUtilities.getDeepestException(e);
        assertSame("When there is no cause, the deepest exception should be the original one",
                e, deepest);
    }

    @Test
    public void testGetDeepestException_NestedCauses() {
        Exception leaf = new IllegalStateException("leaf");
        Exception middle = new RuntimeException("middle", leaf);
        Exception top = new Exception("top", middle);

        Throwable deepest = ExceptionUtilities.getDeepestException(top);
        assertSame("Should return the most nested cause (leaf)", leaf, deepest);
    }

    @Test
    public void testGetDeepestException_MultipleLevelsWithNullCause() {
        // Build a chain where an intermediate exception has a null cause
        Exception leaf = new IllegalArgumentException("leaf");
        Exception middle = new RuntimeException("middle", leaf);
        Exception top = new Exception("top", middle);
        // Break the chain deliberately
        middle.initCause(null);

        Throwable deepest = ExceptionUtilities.getDeepestException(top);
        // After breaking, 'middle' becomes the deepest because its cause is null
        assertSame("When an intermediate cause is null, the method should stop at that point",
                middle, deepest);
    }

    /* -------------------------------------------------------------
     *  Tests for safelyIgnoreException(Callable<T>, T)
     * ------------------------------------------------------------- */

    @Test
    public void testSafelyIgnoreExceptionCallable_SuccessfulCall() throws Exception {
        Callable<String> callable = () -> "result";
        String value = ExceptionUtilities.safelyIgnoreException(callable, "default");
        assertEquals("Callable should return its actual value when no exception occurs",
                "result", value);
    }

    @Test
    public void testSafelyIgnoreExceptionCallable_ThrowsCheckedException() {
        Callable<Integer> callable = () -> {
            throw new java.io.IOException("IO failure");
        };
        Integer value = ExceptionUtilities.safelyIgnoreException(callable, 42);
        assertEquals("When Callable throws, default value must be returned",
                Integer.valueOf(42), value);
    }

    @Test
    public void testSafelyIgnoreExceptionCallable_ThrowsRuntimeException() {
        Callable<String> callable = () -> {
            throw new IllegalStateException("runtime problem");
        };
        String value = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        assertEquals("RuntimeException should also be swallowed and default returned",
                "fallback", value);
    }

    @Test
    public void testSafelyIgnoreExceptionCallable_ThrowsError() {
        Callable<String> callable = () -> {
            throw new AssertionError("serious error");
        };
        // Even an Error is caught and default is used
        String value = ExceptionUtilities.safelyIgnoreException(callable, "default");
        assertEquals("Error should be caught and default value returned", "default", value);
    }

    @Test(expected = NullPointerException.class)
    public void testSafelyIgnoreExceptionCallable_NullCallable() {
        // The implementation does not guard against null, so a NPE is expected
        ExceptionUtilities.safelyIgnoreException(null, "ignored");
    }

    @Test
    public void testSafelyIgnoreExceptionCallable_NullDefaultValue() {
        Callable<String> callable = () -> {
            throw new Exception("force default");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, null);
        assertNull("When defaultValue is null, null should be returned after exception", result);
    }

    @Test
    public void testSafelyIgnoreExceptionCallable_ReturnsNullResult() {
        Callable<String> callable = () -> null;
        String result = ExceptionUtilities.safelyIgnoreException(callable, "fallback");
        assertNull("If Callable legitimately returns null, that null must be propagated", result);
    }

    /* -------------------------------------------------------------
     *  Tests for safelyIgnoreException(Runnable)
     * ------------------------------------------------------------- */

    @Test
    public void testSafelyIgnoreExceptionRunnable_NormalExecution() {
        final boolean[] executed = {false};
        Runnable r = () -> executed[0] = true;

        ExceptionUtilities.safelyIgnoreException(r);
        assertTrue("Runnable should have been executed", executed[0]);
    }

    @Test
    public void testSafelyIgnoreExceptionRunnable_ThrowsException() {
        Runnable r = () -> {
            throw new IllegalArgumentException("bad argument");
        };
        // No exception should escape the method
        ExceptionUtilities.safelyIgnoreException(r);
        // If we reach here, the exception was correctly ignored
    }

    @Test
    public void testSafelyIgnoreExceptionRunnable_ThrowsError() {
        Runnable r = () -> {
            throw new AssertionError("assertion failure");
        };
        // Errors are also swallowed by the utility
        ExceptionUtilities.safelyIgnoreException(r);
        // Successful completion means the error was ignored
    }

    @Test(expected = NullPointerException.class)
    public void testSafelyIgnoreExceptionRunnable_NullRunnable() {
        // Passing null will cause a NPE when trying to invoke run()
        ExceptionUtilities.safelyIgnoreException((Runnable) null);
    }

    /* -------------------------------------------------------------
     *  Tests for safelyIgnoreException(Throwable)
     * ------------------------------------------------------------- */

    @Test
    public void testSafelyIgnoreExceptionThrowable_OtherThrowableIgnored() {
        // No exception should be thrown from this method
        ExceptionUtilities.safelyIgnoreException(new IllegalStateException("ignore me"));
        // If execution reaches here, the throwable was ignored as intended
    }

    @Test
    public void testSafelyIgnoreExceptionThrowable_NullIgnored() {
        // Null is safely ignored (instanceof returns false)
        ExceptionUtilities.safelyIgnoreException((Throwable) null);
        // No exception expected
    }

    @Test(expected = OutOfMemoryError.class)
    public void testSafelyIgnoreExceptionThrowable_OutOfMemoryErrorRethrown() {
        // OutOfMemoryError must be re‑thrown, not ignored
        ExceptionUtilities.safelyIgnoreException(new OutOfMemoryError("oom"));
    }

    @Test
    public void testSafelyIgnoreExceptionThrowable_SubclassOfOutOfMemoryError() {
        // A custom subclass of OutOfMemoryError should also be re‑thrown
        class MyOOM extends OutOfMemoryError {
            MyOOM(String msg) { super(msg); }
        }
        try {
            ExceptionUtilities.safelyIgnoreException(new MyOOM("custom oom"));
            fail("Expected OutOfMemoryError (or subclass) to be re‑thrown");
        } catch (OutOfMemoryError e) {
            assertEquals("custom oom", e.getMessage());
        }
    }
}
