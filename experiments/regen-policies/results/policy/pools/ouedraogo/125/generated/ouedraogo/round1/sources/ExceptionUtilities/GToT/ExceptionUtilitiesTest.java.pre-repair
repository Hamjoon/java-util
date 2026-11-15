package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.concurrent.Callable;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ExceptionUtilities}.
 *
 * The tests are organised as follows:
 *
 * <ul>
 *   <li>Extraction of public method signatures (documented in the class comment).</li>
 *   <li>Basic “happy‑path” tests that verify the documented behaviour.</li>
 *   <li>Edge‑case and exception‑handling tests that probe null arguments,
 *       deep exception chains, and the special handling of {@link OutOfMemoryError}.</li>
 *   <li>All tests are written without any mocking frameworks – only the
 *       standard JDK classes that appear in the source are used.</li>
 * </ul>
 */
public class ExceptionUtilitiesTest {

    // ------------------------------------------------------------------------
    // 1. Public method signatures (extracted from ExceptionUtilities)
    // ------------------------------------------------------------------------
    // public static Throwable getDeepestException(Throwable e)
    // public static <T> T safelyIgnoreException(Callable<T> callable, T defaultValue)
    // public static void safelyIgnoreException(Runnable runnable)
    // public static void safelyIgnoreException(Throwable t)

    // ------------------------------------------------------------------------
    // 2. Basic functionality tests
    // ------------------------------------------------------------------------

    /** Test that {@code getDeepestException} returns the original throwable
     *  when it has no cause. */
    @Test
    public void testGetDeepestException_NoCause() {
        IllegalArgumentException iae = new IllegalArgumentException("root");
        Throwable deepest = ExceptionUtilities.getDeepestException(iae);
        assertSame("When no cause exists, the same instance should be returned",
                iae, deepest);
    }

    /** Test that {@code getDeepestException} walks a chain of causes
     *  and returns the most nested throwable. */
    @Test
    public void testGetDeepestException_WithCauseChain() {
        Exception leaf = new Exception("leaf");
        RuntimeException middle = new RuntimeException("middle", leaf);
        IllegalStateException root = new IllegalStateException("root", middle);

        Throwable deepest = ExceptionUtilities.getDeepestException(root);
        assertSame("The deepest throwable should be the leaf exception",
                leaf, deepest);
    }

    /** Verify that {@code safelyIgnoreException(Callable, T)} returns the
     *  callable's result when no exception is thrown. */
    @Test
    public void testSafelyIgnoreExceptionCallable_Success() throws Exception {
        Callable<String> callable = () -> "OK";
        String result = ExceptionUtilities.safelyIgnoreException(callable, "DEFAULT");
        assertEquals("OK", result);
    }

    /** Verify that the callable‑variant returns the supplied default value
     *  when the callable throws a checked exception. */
    @Test
    public void testSafelyIgnoreExceptionCallable_CheckedException() {
        Callable<String> callable = () -> {
            throw new Exception("checked");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "DEFAULT");
        assertEquals("DEFAULT", result);
    }

    /** Verify that the callable‑variant returns the default value when an
     *  {@link Error} (including {@link OutOfMemoryError}) is thrown. */
    @Test
    public void testSafelyIgnoreExceptionCallable_Error() {
        Callable<String> callable = () -> {
            throw new OutOfMemoryError("OOM");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, "DEFAULT");
        assertEquals("DEFAULT", result);
    }

    /** Verify that {@code safelyIgnoreException(Runnable)} does not propagate
     *  a runtime exception thrown by the runnable. */
    @Test
    public void testSafelyIgnoreExceptionRunnable_ExceptionIsSwallowed() {
        Runnable runnable = () -> {
            throw new IllegalStateException("boom");
        };
        // The method should swallow the exception – the test passes if no
        // exception escapes.
        ExceptionUtilities.safelyIgnoreException(runnable);
    }

    /** Verify that {@code safelyIgnoreException(Runnable)} executes a runnable
     *  that completes normally. */
    @Test
    public void testSafelyIgnoreExceptionRunnable_Success() {
        final boolean[] executed = {false};
        Runnable runnable = () -> executed[0] = true;
        ExceptionUtilities.safelyIgnoreException(runnable);
        assertTrue("Runnable should have been executed", executed[0]);
    }

    /** Verify that {@code safelyIgnoreException(Throwable)} re‑throws an
     *  {@link OutOfMemoryError}. */
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSafelyIgnoreExceptionThrowable_OutOfMemoryErrorRethrown() {
        OutOfMemoryError oome = new OutOfMemoryError("OOM");
        thrown.expect(OutOfMemoryError.class);
        thrown.expectMessage("OOM");
        ExceptionUtilities.safelyIgnoreException(oome);
    }

    /** Verify that {@code safelyIgnoreException(Throwable)} silently ignores
     *  any other {@link Throwable}. */
    @Test
    public void testSafelyIgnoreExceptionThrowable_OtherThrowableIgnored() {
        Exception ex = new Exception("ignored");
        // Should not throw
        ExceptionUtilities.safelyIgnoreException(ex);
    }

    // ------------------------------------------------------------------------
    // 3. Edge‑case / exceptional‑scenario tests
    // ------------------------------------------------------------------------

    /** Passing {@code null} to {@code getDeepestException} should result in a
     *  {@link NullPointerException}. The method does not guard against null. */
    @Test(expected = NullPointerException.class)
    public void testGetDeepestException_NullArgument() {
        ExceptionUtilities.getDeepestException(null);
    }

    /** Passing {@code null} as the {@code Callable} argument should cause a
     *  {@link NullPointerException} when the method attempts to invoke {@code call()}.
     *  (The Javadoc mentions {@code IllegalArgumentException}, but the
     *   current implementation throws NPE.) */
    @Test(expected = NullPointerException.class)
    public void testSafelyIgnoreExceptionCallable_NullCallable() {
        ExceptionUtilities.safelyIgnoreException(null, "default");
    }

    /** Verify that a {@code null} default value is returned when the callable
     *  throws an exception and the supplied default is {@code null}. */
    @Test
    public void testSafelyIgnoreExceptionCallable_NullDefault() {
        Callable<String> callable = () -> {
            throw new RuntimeException("fail");
        };
        String result = ExceptionUtilities.safelyIgnoreException(callable, null);
        assertNull("Result should be null when defaultValue is null", result);
    }

    /** Passing {@code null} as the {@code Runnable} argument should result in a
     *  {@link NullPointerException}. */
    @Test(expected = NullPointerException.class)
    public void testSafelyIgnoreExceptionRunnable_NullRunnable() {
        ExceptionUtilities.safelyIgnoreException((Runnable) null);
    }

    /** Verify that {@code safelyIgnoreException(Throwable)} does nothing
     *  when passed a {@link ThreadDeath} (which is a subclass of {@link Error}
     *  but not {@link OutOfMemoryError}). The method is deliberately silent. */
    @Test
    public void testSafelyIgnoreExceptionThrowable_ThreadDeathIgnored() {
        ThreadDeath td = new ThreadDeath();
        // No exception should be thrown
        ExceptionUtilities.safelyIgnoreException(td);
    }

    /** Verify that the callable‑variant works with a non‑String generic type
     *  (e.g., {@link Integer}). */
    @Test
    public void testSafelyIgnoreExceptionCallable_GenericInteger() {
        Callable<Integer> callable = () -> 42;
        Integer result = ExceptionUtilities.safelyIgnoreException(callable, -1);
        assertEquals(Integer.valueOf(42), result);
    }

    /** Verify that a callable returning {@code null} is handled correctly
     *  (the method should simply return {@code null}, not the default). */
    @Test
    public void testSafelyIgnoreExceptionCallable_ReturnsNull() {
        Callable<String> callable = () -> null;
        String result = ExceptionUtilities.safelyIgnoreException(callable, "default");
        assertNull("Callable returned null, so result should be null", result);
    }
}
