package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.IntStream;

class CompactSet_isCaseInsensitive_Test {

    /**
     * Instance of the base class (default behaviour).
     */
    private CompactSet<String> defaultSet;

    /**
     * Instance of a subclass that overrides {@code isCaseInsensitive()} to return {@code true}.
     */
    private CompactSet<String> caseInsensitiveSet;

    /**
     * Instance of a subclass that overrides {@code isCaseInsensitive()} to throw a {@link RuntimeException}.
     */
    private CompactSet<String> throwingSet;

    /**
     * Subclass that reports case‑insensitivity as {@code true}.
     */
    private static class CaseInsensitiveCompactSet<E> extends CompactSet<E> {

        @Override
        protected boolean isCaseInsensitive() {
            return true;
        }
    }

    /**
     * Subclass that deliberately throws a {@link RuntimeException}
     * from {@code isCaseInsensitive()} to test exception propagation.
     */
    private static class ThrowingCompactSet<E> extends CompactSet<E> {

        @Override
        protected boolean isCaseInsensitive() {
            throw new RuntimeException("Forced exception");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        defaultSet = new CompactSet<>();
        caseInsensitiveSet = new CaseInsensitiveCompactSet<>();
        throwingSet = new ThrowingCompactSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly necessary.
        defaultSet = null;
        caseInsensitiveSet = null;
    }

    /**
     * Helper that uses reflection to invoke the protected {@code isCaseInsensitive()} method.
     *
     * @param set the {@link CompactSet} instance
     * @return the boolean result of {@code isCaseInsensitive()}
     */
    private static boolean invokeIsCaseInsensitive(CompactSet<?> set) {
        try {
            Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
            if (setAccessible) {
                m.setAccessible(true);
            }
            return (boolean) m.invoke(set);
        } catch (IllegalAccessException e) {
            // Propagate for tests that expect this exception.
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            // Unwrap the underlying exception.
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException(cause);
        } catch (Exception e) {
            throw new AssertionError("Unexpected reflection failure", e);
        }
    }

    /**
     * Verify that the base {@code CompactSet} reports case‑insensitivity as {@code false}.
     */
    @Test
    void defaultIsCaseInsensitiveReturnsFalse() {
        // Use reflection to call the protected method without breaking encapsulation.
        boolean result = invokeIsCaseInsensitive(defaultSet);
        assertFalse(result, "CompactSet should be case‑sensitive by default");
    }

    /**
     * Verify that a subclass overriding {@code isCaseInsensitive()} returns {@code true}.
     */
    @Test
    void overriddenIsCaseInsensitiveReturnsTrue() {
        boolean result = invokeIsCaseInsensitive(caseInsensitiveSet);
        assertTrue(result, "Subclass should report case‑insensitivity as true");
    }

    @Test
    void defaultAfterAddReturnsFalse() {
        defaultSet.add("alpha");
        assertFalse(invokeIsCaseInsensitive(defaultSet, true));
    }

    @Test
    void defaultAfterClearReturnsFalse() {
        defaultSet.add("beta");
        defaultSet.clear();
        assertFalse(invokeIsCaseInsensitive(defaultSet, true));
    }

    @Test
    void defaultAfterRemoveReturnsFalse() {
        defaultSet.add("gamma");
        defaultSet.remove("gamma");
        assertFalse(invokeIsCaseInsensitive(defaultSet, true));
    }

    @Test
    void defaultWithDifferentGenericTypeReturnsFalse() {
        CompactSet<Integer> intSet = new CompactSet<>();
        assertFalse(invokeIsCaseInsensitive(intSet, true));
    }

    @Test
    void overriddenAfterMultipleAddsReturnsTrue() {
        caseInsensitiveSet.add("one");
        caseInsensitiveSet.add("two");
        caseInsensitiveSet.add("three");
        assertTrue(invokeIsCaseInsensitive(caseInsensitiveSet, true));
    }

    @Test
    void largeNumberOfAddsDoesNotAffectResult() {
        IntStream.rangeClosed(1, 1000).forEach(i -> defaultSet.add("item" + i));
        assertFalse(invokeIsCaseInsensitive(defaultSet, true));
    }

    @Test
    void addNullElementDoesNotAffectResult() {
        defaultSet.add(null);
        assertFalse(invokeIsCaseInsensitive(defaultSet, true));
    }

    @Test
    void iteratorRemoveAllDoesNotAffectResult() {
        defaultSet.add("a");
        defaultSet.add("b");
        defaultSet.add("c");
        var it = defaultSet.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertFalse(invokeIsCaseInsensitive(defaultSet, true));
    }

    @Test
    void redundantClearOnEmptySetDoesNotAffectResult() {
        // already empty
        defaultSet.clear();
        assertFalse(invokeIsCaseInsensitive(defaultSet, true));
    }

    @Test
    void reflectionWithoutSetAccessibleThrowsIllegalAccessException() {
        Method m;
        try {
            m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
        } catch (NoSuchMethodException e) {
            fail("Method not found via reflection", e);
            return;
        }
        // Do NOT call setAccessible(true)
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> {
            try {
                m.invoke(defaultSet);
            } catch (InvocationTargetException ite) {
                // unwrap the IllegalAccessException
                Throwable cause = ite.getCause();
                if (cause instanceof IllegalAccessException) {
                    throw (IllegalAccessException) cause;
                }
                throw ite;
            }
        });
        assertTrue(thrown.getCause() instanceof IllegalAccessException, "Expected IllegalAccessException wrapped in IllegalStateException");
    }

    @Test
    void reflectionWithNullTargetThrowsNullPointerException() {
        Method m;
        try {
            m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
            m.setAccessible(true);
        } catch (Exception e) {
            fail("Failed to prepare reflective method", e);
            return;
        }
        assertThrows(NullPointerException.class, () -> m.invoke(null));
    }

    @Test
    void overriddenMethodThrowsRuntimeExceptionIsPropagated() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> invokeIsCaseInsensitive(throwingSet, true));
        assertEquals("Forced exception", ex.getMessage(), "The RuntimeException from the overridden method should be propagated");
    }
}
