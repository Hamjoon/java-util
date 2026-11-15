/***************************************************************************************************
 *  JUnit 4 test suite for {@link com.cedarsoftware.util.ExceptionUtilities}
 *
 *  The tests cover:
 *   • {@link ExceptionUtilities#getDeepestException(Throwable)}
 *   • {@link ExceptionUtilities#safelyIgnoreException(java.util.concurrent.Callable, Object)}
 *   • {@link ExceptionUtilities#safelyIgnoreException(Runnable)}
 *   • {@link ExceptionUtilities#safelyIgnoreException(Throwable)}
 *
 *  No external libraries are used – only JUnit 4 and the JDK.
 **************************************************************************************************/
package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.util.concurrent.Callable;

/**
 * Comprehensive unit tests for {@code ExceptionUtilities}.
 */
public class ExceptionUtilitiesTest {

    /*======================================================================
     *  getDeepestException()
     *======================================================================*/

    @Test
    public void testGetDeepestException_SingleException_NoCause() {
        IllegalArgumentException leaf = new IllegalArgumentException("leaf");
        Throwable deepest = ExceptionUtilities.getDeepestException(leaf);
        Assert.assertSame("When there is no cause, the method should return the original exception",
                leaf, deepest);
    }

    @Test
    public void testGetDeepestException_MultipleNestedCauses() {
        // Build a chain: top -> middle -> leaf
        NullPointerException leaf = new NullPointerException("leaf");
        IllegalStateException middle = new IllegalStateException("middle", leaf);
        RuntimeException top = new RuntimeException("top", middle);

        Throwable deepest = ExceptionUtilities.getDeepestException(top);
        Assert.assertSame("The deepest exception should be the leaf of the chain", leaf, deepest);
    }

    @Test
    public void testGetDeepestException_DeepChain_30Levels() {
        // Create a long chain (30 levels) to ensure the loop works for many iterations
        Throwable cause = new Exception("level-0");
        for (int i = 1; i <= 30; i++) {
            cause = new Exception("level-" + i, cause);
        }
        Throwable deepest = ExceptionUtilities.getDeepestException(cause);
        // The deepest should be the original level-0 exception
        Assert.assertEquals("level-0", deepest.getMessage());
    }

    /*======================================================================
     *  safelyIgnoreException(Callable<T>, T)
     *======================================================================*/

    @Test
    public void testSafelyIgnoreException_CallableReturnsValue() throws Exception {
        Callable<String> ok = () -> "result";
        String value = ExceptionUtilities.safelyIgnoreException(ok, "fallback");
        Assert.assertEquals("When the callable succeeds, its result must be returned", "result", value);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsCheckedException() {
        Callable<Integer> failing = () -> {
            throw new Exception("checked");
        };
        Integer result = ExceptionUtilities.safelyIgnoreException(failing, 42);
        Assert.assertEquals("When the callable throws, the default value must be returned", Integer.valueOf(42), result);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsRuntimeException() {
        Callable<String> failing = () -> {
            throw new IllegalArgumentException("runtime");
        };
        String result = ExceptionUtilities.safelyIgnoreException(failing, "default");
        Assert.assertEquals("RuntimeException should also be swallowed", "default", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableThrowsError() {
        Callable<String> failing = () -> {
            throw new AssertionError("error");
        };
        String result = ExceptionUtilities.safelyIgnoreException(failing, "fallback");
        Assert.assertEquals("Error should be swallowed just like any other Throwable", "fallback", result);
    }

    @Test
    public void testSafelyIgnoreException_CallableReturnsNullAndDefaultIsNull() {
        Callable<Object> returnsNull = () -> null;
        Object result = ExceptionUtilities.safelyIgnoreException(returnsNull, null);
        Assert.assertNull("Both callable result and default are null – method should return null", result);
    }

    @Test(expected = NullPointerException.class)
    public void testSafelyIgnoreException_NullCallable() {
        // The implementation does not perform an explicit null‑check, so a NPE is expected
        ExceptionUtilities.safelyIgnoreException(null, "unused");
    }

    /*======================================================================
     *  safelyIgnoreException(Runnable)
     *======================================================================*/

    @Test
    public void testSafelyIgnoreException_RunnableExecutesNormally() {
        final boolean[] executed = {false};
        Runnable r = () -> executed[0] = true;
        ExceptionUtilities.safelyIgnoreException(r);
        Assert.assertTrue("Runnable should have been executed", executed[0]);
    }

    @Test
    public void testSafelyIgnoreException_RunnableThrowsCheckedException() {
        Runnable r = () -> {
            try {
                throw new Exception("checked");
            } catch (Exception e) {
                // Wrap to a RuntimeException because Runnable.run() cannot throw checked exceptions
                throw new RuntimeException(e);
            }
        };
        // No exception should propagate out of the utility method
        ExceptionUtilities.safelyIgnoreException(r);
    }

    @Test
    public void testSafelyIgnoreException_RunnableThrowsRuntimeException() {
        Runnable r = () -> {
            throw new IllegalStateException("runtime");
        };
        // The method must swallow the RuntimeException
        ExceptionUtilities.safelyIgnoreException(r);
    }

    @Test
    public void testSafelyIgnoreException_RunnableThrowsError() {
        Runnable r = () -> {
            throw new StackOverflowError("error");
        };
        // Even Errors are caught and ignored
        ExceptionUtilities.safelyIgnoreException(r);
    }

    @Test
    public void testSafelyIgnoreException_RunnableIsNull() {
        // Passing null results in a NullPointerException when invoking run()
        try {
            ExceptionUtilities.safelyIgnoreException((Runnable) null);
            Assert.fail("Expected NullPointerException when runnable is null");
        } catch (NullPointerException expected) {
            // Expected path – nothing to do
        }
    }

    /*======================================================================
     *  safelyIgnoreException(Throwable)
     *======================================================================*/

    @Test
    public void testSafelyIgnoreException_ThrowableIsOutOfMemoryError_Rethrown() {
        OutOfMemoryError oom = new OutOfMemoryError("oom");
        try {
            ExceptionUtilities.safelyIgnoreException(oom);
            Assert.fail("OutOfMemoryError must be rethrown");
        } catch (OutOfMemoryError caught) {
            Assert.assertSame("The rethrown error should be the same instance", oom, caught);
        }
    }

    @Test
    public void testSafelyIgnoreException_ThrowableIsOtherError_NotRethrown() {
        // Any other Error should be ignored silently
        AssertionError error = new AssertionError("assertion");
        ExceptionUtilities.safelyIgnoreException(error); // should not throw
    }

    @Test
    public void testSafelyIgnoreException_ThrowableIsException_Ignored() {
        Exception ex = new Exception("generic");
        ExceptionUtilities.safelyIgnoreException(ex); // no exception should escape
    }

    @Test
    public void testSafelyIgnoreException_ThrowableIsNull_NoException() {
        // Null is a legal argument – the method simply returns
        ExceptionUtilities.safelyIgnoreException((Throwable) null);
    }
}
