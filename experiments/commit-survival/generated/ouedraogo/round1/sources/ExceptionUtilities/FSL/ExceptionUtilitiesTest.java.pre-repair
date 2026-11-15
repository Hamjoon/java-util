package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link ExceptionUtilities}.
 */
public class ExceptionUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /* Tests for safelyIgnoreException                                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void safelyIgnoreException_nullDoesNotThrow() {
        // Passing null should be safely ignored (no exception thrown)
        ExceptionUtilities.safelyIgnoreException(null);
    }

    @Test
    public void safelyIgnoreException_genericThrowableIsIgnored() {
        Throwable t = new Exception("generic");
        // Should not throw any exception
        ExceptionUtilities.safelyIgnoreException(t);
    }

    @Test
    public void safelyIgnoreException_genericErrorIsIgnored() {
        // An Error that is NOT OutOfMemoryError should be ignored
        Throwable t = new AssertionError("assertion error");
        ExceptionUtilities.safelyIgnoreException(t);
    }

    @Test(expected = OutOfMemoryError.class)
    public void safelyIgnoreException_outOfMemoryErrorIsRethrown() {
        // OutOfMemoryError must be re‑thrown
        Throwable t = new OutOfMemoryError("oom");
        ExceptionUtilities.safelyIgnoreException(t);
        // The test will fail if the exception is not re‑thrown
    }

    /* --------------------------------------------------------------------- */
    /* Tests for getDeepestException                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void getDeepestException_singleExceptionHasNoCause() {
        Exception e = new IllegalArgumentException("root");
        Throwable deepest = ExceptionUtilities.getDeepestException(e);
        assertSame("When there is no cause the same exception should be returned", e, deepest);
    }

    @Test
    public void getDeepestException_multipleNestedCauses() {
        IllegalArgumentException deepest = new IllegalArgumentException("deepest");
        RuntimeException middle = new RuntimeException("middle", deepest);
        Exception top = new Exception("top", middle);

        Throwable result = ExceptionUtilities.getDeepestException(top);
        assertSame("Should return the most nested cause", deepest, result);
    }

    @Test
    public void getDeepestException_deepChainWithFourLevels() {
        NullPointerException level4 = new NullPointerException("level4");
        IllegalStateException level3 = new IllegalStateException("level3", level4);
        RuntimeException level2 = new RuntimeException("level2", level3);
        Exception level1 = new Exception("level1", level2);

        Throwable deepest = ExceptionUtilities.getDeepestException(level1);
        assertSame(level4, deepest);
    }

    @Test(expected = NullPointerException.class)
    public void getDeepestException_nullArgumentThrowsNPE() {
        // The implementation does not guard against null, so a NPE is expected
        ExceptionUtilities.getDeepestException(null);
    }
}
