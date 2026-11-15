package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExceptionUtilitiesTest {

    /* -------------------------------------------------------------
       Basic functionality tests
       ------------------------------------------------------------- */

    @Test
    public void testSafelyIgnoreException_IgnoresRegularException() {
        // An ordinary Exception must be ignored (no re‑throw)
        ExceptionUtilities.safelyIgnoreException(new Exception("regular"));
    }

    @Test
    public void testSafelyIgnoreException_IgnoresErrorOtherThanOOM() {
        // An Error that is NOT OutOfMemoryError must be ignored
        ExceptionUtilities.safelyIgnoreException(new AssertionError("assert"));
    }

    @Test
    public void testGetDeepestException_SingleThrowable() {
        RuntimeException ex = new RuntimeException("only");
        assertSame("When there is no cause, the same instance must be returned",
                ex, ExceptionUtilities.getDeepestException(ex));
    }

    /* -------------------------------------------------------------
       Edge‑case / exception handling tests
       ------------------------------------------------------------- */

    @Test
    public void testSafelyIgnoreException_NullArgument() {
        // Null is safely ignored – no exception should be thrown
        ExceptionUtilities.safelyIgnoreException(null);
    }

    @Test
    public void testSafelyIgnoreException_OutOfMemoryErrorIsRethrown() {
        OutOfMemoryError oom = new OutOfMemoryError("OOM simulated");
        try {
            ExceptionUtilities.safelyIgnoreException(oom);
            fail("OutOfMemoryError should have been re‑thrown");
        } catch (OutOfMemoryError e) {
            assertSame("The same OutOfMemoryError instance must be re‑thrown", oom, e);
        }
    }

    @Test
    public void testSafelyIgnoreException_OtherErrorIgnored() {
        // StackOverflowError is an Error but must be ignored (not re‑thrown)
        ExceptionUtilities.safelyIgnoreException(new StackOverflowError("stack"));
    }

    @Test
    public void testGetDeepestException_TwoLevelChain() {
        IllegalArgumentException inner = new IllegalArgumentException("inner");
        RuntimeException outer = new RuntimeException("outer", inner);
        assertSame("Deepest cause should be the inner exception",
                inner, ExceptionUtilities.getDeepestException(outer));
    }

    @Test
    public void testGetDeepestException_ThreeLevelChain() {
        NullPointerException deepest = new NullPointerException("deep");
        IllegalStateException middle = new IllegalStateException("middle", deepest);
        RuntimeException top = new RuntimeException("top", middle);
        assertSame("Deepest cause should be the deepest exception in the chain",
                deepest, ExceptionUtilities.getDeepestException(top));
    }

    @Test(expected = NullPointerException.class)
    public void testGetDeepestException_NullArgument() {
        // Passing null must result in NPE because the method dereferences it
        ExceptionUtilities.getDeepestException(null);
    }
}
