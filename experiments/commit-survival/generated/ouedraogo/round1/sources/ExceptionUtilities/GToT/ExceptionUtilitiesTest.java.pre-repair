package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit 4 test cases for {@link ExceptionUtilities}.
 *
 * The test suite covers:
 * <ul>
 *   <li>Basic functionality of each public method.</li>
 *   <li>Edge‑case handling (null arguments, single‑level vs. multi‑level causes).</li>
 *   <li>Exception‑rethrow semantics of {@code safelyIgnoreException}.</li>
 *   <li>Verification that {@code getDeepestException} returns the most‑nested cause.</li>
 * </ul>
 */
public class ExceptionUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  Tests for safelyIgnoreException                                        */
    /* ---------------------------------------------------------------------- */

    /**
     * Verify that a regular {@link Exception} is ignored (i.e. no exception is
     * thrown and the method returns normally).
     */
    @Test
    public void testSafelyIgnoreException_WithRegularException() {
        // No exception should be propagated.
        ExceptionUtilities.safelyIgnoreException(new Exception("regular"));
    }

    /**
     * Verify that passing {@code null} is safely ignored.  The {@code instanceof}
     * check returns {@code false}, so the method should simply return.
     */
    @Test
    public void testSafelyIgnoreException_WithNull() {
        ExceptionUtilities.safelyIgnoreException(null);
    }

    /**
     * Verify that an {@link OutOfMemoryError} is **not** ignored but re‑thrown.
     * The test expects the same error instance to be thrown.
     */
    @Test(expected = OutOfMemoryError.class)
    public void testSafelyIgnoreException_WithOutOfMemoryError() {
        OutOfMemoryError oom = new OutOfMemoryError("oom");
        ExceptionUtilities.safelyIgnoreException(oom);
        // The line above must throw; any code here is unreachable.
    }

    /**
     * Verify that a {@link ThreadDeath} (which is a subclass of {@link Error})
     * is ignored because the implementation only checks for {@code OutOfMemoryError}.
     */
    @Test
    public void testSafelyIgnoreException_WithThreadDeath() {
        // ThreadDeath extends Error; it should be ignored (i.e., no re‑throw).
        ExceptionUtilities.safelyIgnoreException(new ThreadDeath());
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for getDeepestException                                          */
    /* ---------------------------------------------------------------------- */

    /**
     * When the supplied throwable has no cause, the method should return the
     * same instance.
     */
    @Test
    public void testGetDeepestException_SingleLevel() {
        IllegalArgumentException leaf = new IllegalArgumentException("leaf");
        Throwable result = ExceptionUtilities.getDeepestException(leaf);
        assertSame("Should return the original throwable when no cause exists", leaf, result);
    }

    /**
     * Verify that the deepest nested cause is returned for a chain of three
     * exceptions.
     */
    @Test
    public void testGetDeepestException_MultiLevel() {
        RuntimeException deepest = new RuntimeException("deepest");
        IllegalStateException middle = new IllegalStateException("middle", deepest);
        Exception top = new Exception("top", middle);

        Throwable result = ExceptionUtilities.getDeepestException(top);
        assertSame("Should return the most nested cause", deepest, result);
    }

    /**
     * Verify that a chain with more than three levels still returns the most
     * nested cause.
     */
    @Test
    public void testGetDeepestException_DeepChain() {
        Throwable level5 = new IllegalArgumentException("level5");
        Throwable level4 = new IllegalStateException("level4", level5);
        Throwable level3 = new RuntimeException("level3", level4);
        Throwable level2 = new Exception("level2", level3);
        Throwable level1 = new Exception("level1", level2);

        Throwable result = ExceptionUtilities.getDeepestException(level1);
        assertSame("Should navigate arbitrarily deep cause chains", level5, result);
    }

    /**
     * Passing {@code null} should result in a {@link NullPointerException}
     * because the implementation attempts to call {@code e.getCause()}.
     */
    @Test(expected = NullPointerException.class)
    public void testGetDeepestException_WithNull() {
        ExceptionUtilities.getDeepestException(null);
    }

    /**
     * Ensure that the method does not alter the original exception chain.
     */
    @Test
    public void testGetDeepestException_DoesNotModifyChain() {
        Throwable leaf = new IllegalArgumentException("leaf");
        Exception middle = new Exception("middle", leaf);
        Exception top = new Exception("top", middle);

        // Capture the original structure
        Throwable originalCause = top.getCause();

        // Invoke the utility
        Throwable deepest = ExceptionUtilities.getDeepestException(top);

        // Verify deepest is leaf
        assertSame(leaf, deepest);
        // Verify the chain is unchanged
        assertSame(originalCause, top.getCause());
    }
}
