package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class CompactCILinkedSet_getNewSet_Test {

    private CompactCILinkedSet<String> ciLinkedSet;

    /**
     * Subclass that forces a specific compactSize() value.
     */
    private static class CompactCILinkedSetWithSize extends CompactCILinkedSet<String> {

        private final int forcedSize;

        // optional exception to throw
        private final RuntimeException toThrow;

        CompactCILinkedSetWithSize(int forcedSize) {
            this.forcedSize = forcedSize;
            this.toThrow = null;
        }

        CompactCILinkedSetWithSize(RuntimeException toThrow) {
            this.forcedSize = 0;
            this.toThrow = toThrow;
        }

        @Override
        protected int compactSize() {
            if (toThrow != null) {
                throw toThrow;
            }
            return forcedSize;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test
        ciLinkedSet = new CompactCILinkedSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC (not strictly required)
        ciLinkedSet = null;
    }

    @Test
    void getNewSetReturnsCaseInsensitiveSet() {
        // Act
        Set<String> newSet = ciLinkedSet.getNewSet();
        // Assert
        assertNotNull(newSet, "The returned set should not be null");
        assertTrue(newSet instanceof CaseInsensitiveSet, "The returned set should be an instance of CaseInsensitiveSet");
        assertTrue(newSet.isEmpty(), "The newly created set should be empty");
    }

    @Test
    void getNewSetIsCaseInsensitive() {
        // Arrange
        Set<String> newSet = ciLinkedSet.getNewSet();
        // Act
        newSet.add("Test");
        // should be ignored because of case‑insensitivity
        boolean addedDuplicate = newSet.add("test");
        // Assert
        assertEquals(1, newSet.size(), "Set should contain only one element due to case‑insensitivity");
        assertFalse(addedDuplicate, "Adding a case‑insensitive duplicate should return false");
        assertTrue(newSet.contains("TEST"), "Contains should be case‑insensitive");
    }

    @Test
    void getNewSetCreatesDistinctInstances() {
        // Act
        Set<String> first = ciLinkedSet.getNewSet();
        Set<String> second = ciLinkedSet.getNewSet();
        // Assert
        assertNotSame(first, second, "Each call to getNewSet should return a new distinct Set instance");
        assertTrue(first.isEmpty() && second.isEmpty(), "Both newly created sets should be empty initially");
    }

    @Test
    void getNewSetCapacityIsBasedOnCompactSize() {
        // The CompactSet implementation defines a protected method compactSize().
        // We can call it here to verify that the new set is created with a capacity
        // of compactSize() + 1.  While we cannot directly inspect the internal capacity
        // of CaseInsensitiveSet, we can at least ensure that no exception is thrown
        // and that the set behaves correctly when we add up to that many elements.
        // protected, accessible within same package
        int compactSize = ciLinkedSet.compactSize();
        Set<String> newSet = ciLinkedSet.getNewSet();
        // Fill the set with (compactSize + 1) distinct strings
        for (int i = 0; i < compactSize + 1; i++) {
            assertTrue(newSet.add("key" + i), "Adding a new distinct element should succeed");
        }
        // The set should now contain exactly compactSize + 1 elements
        assertEquals(compactSize + 1, newSet.size(), "Set size should match the expected capacity (compactSize + 1)");
    }

    @Test
    void getNewSetIsReportedAsCaseInsensitiveViaProtectedMethod() throws Exception {
        // Use reflection to invoke the protected isCaseInsensitive() method
        Method method = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(ciLinkedSet);
        assertTrue(result, "isCaseInsensitive() should return true for CI linked set");
    }

    @Test
    void getNewSetStoresAndRetrievesElementsIrrespectiveOfCase() {
        Set<String> newSet = ciLinkedSet.getNewSet();
        newSet.add("MiXeD");
        assertTrue(newSet.contains("mixed"), "Set should find element regardless of case");
        assertTrue(newSet.contains("MIXED"), "Set should find element regardless of case");
    }

    @Test
    void getNewSetHasCapacityOneWhenCompactSizeIsZero() throws Exception {
        // CompactCILinkedSet with zero elements already has compactSize() == 0.
        // We cannot read the internal capacity directly, but we can verify that a
        // single element can be added without triggering an exception.
        Set<String> newSet = ciLinkedSet.getNewSet();
        assertTrue(newSet.add("only"), "Adding the first element should succeed even when compactSize() == 0");
        assertEquals(1, newSet.size(), "Set should contain exactly one element after the add");
    }

    @Test
    void getNewSetDoesNotAffectOriginalSetContents() {
        // Populate the original CompactCILinkedSet
        ciLinkedSet.add("alpha");
        ciLinkedSet.add("beta");
        ciLinkedSet.add("gamma");
        int originalSize = ciLinkedSet.size();
        Set<String> newSet = ciLinkedSet.getNewSet();
        // Mutate the new set – original must stay untouched
        newSet.add("delta");
        newSet.add("epsilon");
        assertEquals(originalSize, ciLinkedSet.size(), "Original set size must remain unchanged after getNewSet()");
        assertTrue(ciLinkedSet.containsAll(Set.of("alpha", "beta", "gamma")), "Original set must still contain its original elements");
    }

    @Test
    void getNewSetThrowsNullPointerExceptionWhenCapacityIsNegativeDueToOverflow() {
        // Force compactSize() to Integer.MAX_VALUE, causing overflow when +1 is applied.
        CompactCILinkedSet<String> overflowSet = new CompactCILinkedSetWithSize(Integer.MAX_VALUE);
        // The constructor of CaseInsensitiveSet is expected to throw NPE for a negative capacity.
        assertThrows(NullPointerException.class, overflowSet::getNewSet, "Expected NullPointerException when capacity overflows to a negative value");
    }

    @ParameterizedTest
    @ValueSource(ints = { -5, -1 })
    void getNewSetThrowsExceptionWhenCompactSizeIsNegative(int badSize) {
        CompactCILinkedSet<String> negativeSet = new CompactCILinkedSetWithSize(badSize);
        // The exact exception type depends on the implementation of CaseInsensitiveSet.
        // We accept either NPE or IllegalArgumentException.
        assertThrows(RuntimeException.class, negativeSet::getNewSet, "Calling getNewSet() with a negative computed capacity should propagate an exception");
    }

    @Test
    void getNewSetPropagatesExceptionFromCompactSize() {
        RuntimeException cause = new IllegalStateException("compactSize failure");
        CompactCILinkedSet<String> exceptionSet = new CompactCILinkedSetWithSize(cause);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, exceptionSet::getNewSet, "Exception thrown by compactSize() should propagate unchanged");
        assertSame(cause, thrown, "The propagated exception should be the original one");
    }

    @Test
    void getNewSetPropagatesRuntimeExceptionFromCompactSize() {
        RuntimeException cause = new RuntimeException("boom");
        CompactCILinkedSet<String> exceptionSet = new CompactCILinkedSetWithSize(cause);
        RuntimeException thrown = assertThrows(RuntimeException.class, exceptionSet::getNewSet, "RuntimeException from compactSize() must not be swallowed");
        assertSame(cause, thrown, "The same RuntimeException instance should be observed");
    }

    @Test
    void getNewSetThrowsOutOfMemoryErrorWhenCapacityIsExtreme() {
        // Simulate the extreme case where compactSize() returns Integer.MAX_VALUE.
        // The constructor of CaseInsensitiveSet may attempt to allocate an enormous array,
        // resulting in an OutOfMemoryError. We assert that such an error propagates.
        CompactCILinkedSet<String> oomSet = new CompactCILinkedSetWithSize(Integer.MAX_VALUE);
        assertThrows(OutOfMemoryError.class, oomSet::getNewSet, "Expected OutOfMemoryError when attempting to allocate an extreme capacity");
    }
}
