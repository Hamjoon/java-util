package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListSet;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_clear_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor for each test to guarantee a fresh instance
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared after each test to avoid side‑effects
        if (set != null) {
            try {
                set.clear();
            } catch (Exception ignored) {
                // Some tests replace the internal set with a mock that throws;
                // ignore any exception during teardown.
            }
        }
    }

    @Test
    void testClearOnEmptySet() {
        assertTrue(set.isEmpty(), "Set should be empty before clear");
        // method under test
        set.clear();
        assertTrue(set.isEmpty(), "Set should remain empty after clear");
        assertEquals(0, set.size(), "Size should be zero after clear");
    }

    @Test
    void testClearRemovesAllElementsIncludingNull() {
        // Populate the set with a mixture of values and a null
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));
        assertEquals(4, set.size(), "Pre‑condition: set should contain four elements");
        assertTrue(set.contains(null), "Pre‑condition: set should contain null");
        // Invoke the method under test
        set.clear();
        // Verify the set is empty and does not contain any of the previously added elements
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size(), "Size should be zero after clear");
        assertFalse(set.contains("apple"));
        assertFalse(set.contains("banana"));
        assertFalse(set.contains("cherry"));
        assertFalse(set.contains(null));
        // Iterator should have no elements
        Iterator<String> it = set.iterator();
        assertFalse(it.hasNext(), "Iterator should have no elements after clear");
    }

    @Test
    void testClearWithCustomComparatorAndNulls() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // Add elements of varying lengths and a null
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 6
        assertTrue(set.add("banana"));
        // length 0
        assertTrue(set.add(null));
        assertEquals(4, set.size(), "Pre‑condition: set should contain four elements");
        assertTrue(set.contains(null), "Pre‑condition: set should contain null");
        // Clear the set
        set.clear();
        // Verify that the set is empty and ordering no longer matters
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size(), "Size should be zero after clear");
        assertFalse(set.contains("kiwi"));
        assertFalse(set.contains("apple"));
        assertFalse(set.contains("banana"));
        assertFalse(set.contains(null));
    }

    /**
     * Populate the set, clear it, then verify that a new element can be added.
     */
    @Test
    void testClearOnNonEmptySetAndSubsequentAdd() {
        // Populate with distinct values
        assertTrue(set.add("one"));
        assertTrue(set.add("two"));
        assertTrue(set.add("three"));
        // Verify pre‑condition
        assertEquals(3, set.size());
        assertFalse(set.isEmpty());
        // Clear the set
        set.clear();
        // Verify cleared state
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size());
        assertFalse(set.contains("one"));
        assertFalse(set.contains("two"));
        assertFalse(set.contains("three"));
        // Subsequent add must work normally
        assertTrue(set.add("new"));
        assertEquals(1, set.size());
        assertTrue(set.contains("new"));
    }

    /**
     * Attempt to add a null element (which the underlying {@link ConcurrentSkipListSet}
     * rejects with {@link NullPointerException}), catch the exception, then call {@code clear()}.
     * The clear operation must not propagate the previous exception.
     */
    @Test
    void testClearAfterFailedNullAdd() {
        // The default internal set is a ConcurrentSkipListSet which throws NPE on null add
        assertThrows(NullPointerException.class, () -> set.add(null), "Adding null should throw NPE");
        // The set should still be empty
        assertTrue(set.isEmpty());
        // clear() must not throw and must leave the set empty
        assertDoesNotThrow(() -> set.clear(), "clear() should not throw after a failed add");
        assertTrue(set.isEmpty(), "Set should remain empty after clear");
    }

    /**
     * Verify that an iterator obtained before {@code clear()} reflects the cleared state.
     */
    @Test
    void testClearAfterIteratorObtainedBeforeClear() {
        // Populate the set
        set.add("alpha");
        set.add("beta");
        set.add("gamma");
        // Obtain iterator before clear
        Iterator<String> iterator = set.iterator();
        // Clear the set
        set.clear();
        // The iterator should now report no elements
        assertFalse(iterator.hasNext(), "Iterator should have no next after clear");
        assertThrows(NoSuchElementException.class, iterator::next, "Calling next() should throw NoSuchElementException");
        // Also verify the set itself is empty
        assertTrue(set.isEmpty());
    }

    /**
     * Ensure that a second call to {@code clear()} on an already‑empty set is a no‑op.
     */
    @Test
    void testIdempotentClearDoubleCall() {
        // Populate and clear once
        set.add("x");
        set.add("y");
        set.clear();
        // Verify cleared state
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        // Second clear should not change state or throw
        assertDoesNotThrow(() -> set.clear(), "Second clear should not throw");
        assertTrue(set.isEmpty(), "Set should remain empty after second clear");
        assertEquals(0, set.size(), "Size should remain zero after second clear");
    }

    /**
     * Verify that clearing the main set also clears any subset view.
     */
    @Test
    void testClearReflectsInSubsetView() {
        // Add a range of elements
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");
        // Obtain a headSet view that includes "c"
        NavigableSet<String> headSet = set.headSet("c", true);
        // Pre‑condition checks
        assertEquals(3, headSet.size());
        assertTrue(headSet.contains("a"));
        assertTrue(headSet.contains("b"));
        assertTrue(headSet.contains("c"));
        // Clear the main set
        set.clear();
        // The subset view must also be empty
        assertTrue(headSet.isEmpty(), "Subset view should be empty after clear on main set");
        assertEquals(0, headSet.size());
        assertFalse(headSet.iterator().hasNext(), "Subset iterator should have no elements");
    }

    /**
     * Construct the wrapper with an unmodifiable internal set that throws
     * {@link UnsupportedOperationException} on {@code clear()} and verify the exception propagates.
     */
    @Test
    void testClearOnUnmodifiableInternalSet() throws Exception {
        // Replace internalSet with an unmodifiable view that throws on clear()
        NavigableSet<Object> unmodifiable = Collections.unmodifiableNavigableSet(new ConcurrentSkipListSet<>());
        set = new ConcurrentNavigableSetNullSafe<>();
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(set, unmodifiable);
        // Expect UnsupportedOperationException when clear() is invoked
        assertThrows(UnsupportedOperationException.class, () -> set.clear(), "clear() should propagate UnsupportedOperationException from internal set");
    }

    /**
     * Provide a custom internal {@link NavigableSet} whose {@code clear()} method throws
     * {@link SecurityException} and verify that the wrapper propagates it.
     */
    @Test
    void testClearOnInternalSetThrowingSecurityException() throws Exception {
        // Mock a NavigableSet that throws SecurityException on clear()
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockSet = mock(NavigableSet.class);
        doThrow(new SecurityException("Security violation")).when(mockSet).clear();
        // Inject the mock into the wrapper
        set = new ConcurrentNavigableSetNullSafe<>();
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(set, mockSet);
        // Verify that clear() propagates the SecurityException
        SecurityException ex = assertThrows(SecurityException.class, () -> set.clear(), "clear() should propagate SecurityException from internal set");
        assertEquals("Security violation", ex.getMessage());
    }

    /**
     * Use reflection to set the private final {@code internalSet} field to {@code null}
     * and verify that invoking {@code clear()} results in a {@link NullPointerException}.
     */
    @Test
    void testClearWhenInternalSetIsNullViaReflection() throws Exception {
        // Set internalSet to null via reflection
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(set, null);
        // Expect NullPointerException when clear() is called
        assertThrows(NullPointerException.class, () -> set.clear(), "clear() should throw NullPointerException when internalSet is null");
    }
}
