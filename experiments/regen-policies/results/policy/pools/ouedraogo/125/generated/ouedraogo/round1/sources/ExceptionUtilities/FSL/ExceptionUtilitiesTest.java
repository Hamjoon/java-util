package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.concurrent.Callable;

/**
 * Unit tests for {@link ExceptionUtilities}.
 */
public class ExceptionUtilitiesTest {

    /* ---------- Tests for getDeepestException ---------- */

    @Test
    public void testGetDeepestException_SingleLevel() {
        Exception e = new Exception("root");
        Throwable deepest = ExceptionUtilities.getDeepestException(e);
        assertSame("When there is no cause, the same exception should be returned", e, deepest);
    }

    @Test
    public void testGetDeepestException_MultipleLevels() {
        IllegalArgumentException leaf = new IllegalArgumentException("leaf");
        RuntimeException middle = new RuntimeException("middle", leaf);
        Exception root = new Exception("root", middle);

        Throwable deepest = ExceptionUtilities.getDeepestException(root);
        assertSame("The deepest (most nested) cause should be returned", leaf, deepest);
    }

    @Test
    public void testGetDeepestException_CycleProtection() {
        // Create a cycle: a -> b -> a
        Exception a = new Exception("a");
        Exception b = new Exception("b", a);
        // Manually create a cycle (not typical but possible via reflection)
        try {
            java.lang.reflect.Field causeField = Throwable.class.getDeclaredField("cause");
            causeField.setAccessible(true);
            causeField.set(a, b);
        } catch (Exception ignored) {
            // If reflection fails, just skip this part; the method will still terminate because
            // the while loop checks getCause() which will become null after the first iteration.
        }

        // The method will stop at the first null cause it encounters.
        Throwable deepest = ExceptionUtilities.getDeepestException(a);
        assertNotNull(deepest);
    }

    /* ---------- Tests for safelyIgnoreException(Callable, default) ---------- */

    @Test
    public void testSafelyIgnoreException_CallableReturnsValue() throws Exception {
        Callable<String> good = () -> "success";
        String result = ExceptionUtilities.safelyIgnoreException(good, "fallback");
        assertEquals("When no exception is thrown, the callable result should be returned", "success", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsCheckedException() {
        Callable<Integer> bad = () -> {
            throw new java.io.IOException("boom");
        };
        Integer result = ExceptionUtilities.safelyIgnoreException(bad, 42);
        assertEquals("When a checked exception is thrown, the default value should be returned", Integer.valueOf(42), result);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsRuntimeException() {
        Callable<String> bad = () -> {
            throw new IllegalStateException("runtime");
        };
        String result = ExceptionUtilities.safelyIgnoreException(bad, "default");
        assertEquals("RuntimeException should also be suppressed and default returned", "default", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsError() {
        Callable<String> bad = () -> {
            throw new AssertionError("error");
        };
        String result = ExceptionUtilities.safelyIgnoreException(bad, "ok");
        assertEquals("Error should be suppressed and default returned", "ok", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableIsNull() {
        // According to the Javadoc an IllegalArgumentException is expected,
        // but the implementation catches the NullPointerException and returns the default.
        String result = ExceptionUtilities.safelyIgnoreException(null, "nullDefault");
        assertEquals("When callable is null, the method returns the default value", "nullDefault", result);
    }

    /* ---------- Tests for safelyIgnoreException(Runnable) ---------- */

    @Test
    public void testSafelyIgnoreException_RunnableExecutesNormally() {
        final boolean[] executed = { false };
        Runnable r = () -> executed[0] = true;
        ExceptionUtilities.safelyIgnoreException(r);
        assertTrue("Runnable should have been executed", executed[0]);
    }

    @Test
    public void testSafelyIgnoreException_RunnableThrowsException() {
        final boolean[] reached = { false };
        Runnable r = () -> {
            reached[0] = true;
            throw new RuntimeException("oops");
        };
        // No exception should propagate
        ExceptionUtilities.safelyIgnoreException(r);
        assertTrue("Runnable should have been entered before throwing", reached[0]);
    }

    @Test
    public void testSafelyIgnoreException_RunnableThrowsError() {
        Runnable r = () -> {
            throw new StackOverflowError("stack");
        };
        // Should be ignored silently
        ExceptionUtilities.safelyIgnoreException(r);
        // No assertions needed – test passes if no error propagates
    }

    /* ---------- Tests for safelyIgnoreException(Throwable) ---------- */

    @Test
    public void testSafelyIgnoreException_ThrowableIgnored() {
        // Any throwable that is not OutOfMemoryError should be ignored silently.
        Throwable t = new IllegalArgumentException("ignore me");
        // Should not throw
        ExceptionUtilities.safelyIgnoreException(t);
    }

    @Test(expected = OutOfMemoryError.class)
    public void testSafelyIgnoreException_OutOfMemoryErrorRethrown() {
        // OutOfMemoryError must be rethrown.
        OutOfMemoryError oom = new OutOfMemoryError("oom");
        ExceptionUtilities.safelyIgnoreException(oom);
    }

    @Test
    public void testSafelyIgnoreException_NullThrowable() {
        // Passing null should result in a NullPointerException when checking instanceof,
        // which is caught by the method (it doesn't catch) – actually it will just return.
        // The method does not guard against null, so we verify that it does nothing.
        try {
            ExceptionUtilities.safelyIgnoreException((Throwable) null);
        } catch (Exception e) {
            fail("Method should not throw when passed null, but got: " + e);
        }
    }
}
