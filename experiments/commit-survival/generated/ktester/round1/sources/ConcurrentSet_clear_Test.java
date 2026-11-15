package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_clear_Test {

    /**
     * Instance under test – recreated before each test to guarantee isolation.
     */
    private ConcurrentSet<Integer> set;

    @BeforeEach
    void setupBeforeEach() {
        // GIVEN: a fresh, empty ConcurrentSet for every test case.
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – not strictly required but clarifies intent.
        set = null;
    }

    @Test
    @DisplayName("clear() on a populated set empties the set and resets size to zero")
    void testClearOnPopulatedSet() {
        // GIVEN: a set containing three distinct elements
        set.addAll(Arrays.asList(1, 2, 3));
        assertEquals(3, set.size(), "Pre‑condition: set should contain 3 elements");
        // WHEN: clear() is invoked
        set.clear();
        // THEN: the set reports as empty and size is zero
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertEquals(0, set.size(), "Size should be 0 after clear()");
    }

    @Test
    @DisplayName("clear() on an already empty set leaves it empty without error")
    void testClearOnEmptySet() {
        // GIVEN: an empty set (already satisfied by @BeforeEach)
        // WHEN: clear() is called
        set.clear();
        // THEN: the set remains empty and size stays at zero
        assertTrue(set.isEmpty(), "Empty set should remain empty after clear()");
        assertEquals(0, set.size(), "Size should remain 0 after clear() on empty set");
    }

    @Test
    @DisplayName("clear() is idempotent – calling it twice leaves set empty")
    void testClearIdempotent() {
        set.addAll(Arrays.asList(10, 20));
        set.clear();
        // first clear already verified; now call again
        set.clear();
        assertTrue(set.isEmpty(), "Set should stay empty after second clear()");
        assertEquals(0, set.size(), "Size should stay 0 after second clear()");
    }

    @Test
    @DisplayName("clear() removes a null element if present")
    void testClearWithNullElement() {
        // The implementation permits null via a sentinel.
        set.add(null);
        assertTrue(set.contains(null), "Set should contain null before clear()");
        assertEquals(1, set.size());
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertFalse(set.contains(null), "Null should no longer be present");
    }

    @Test
    @DisplayName("clear() empties a set populated via addAll")
    void testClearAfterAddAll() {
        List<Integer> many = Arrays.asList(5, 6, 7, 8, 9);
        set.addAll(many);
        assertEquals(many.size(), set.size());
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertEquals(0, set.size());
    }

    @Test
    @DisplayName("Iterator obtained before clear() reflects cleared state without CME")
    void testIteratorBeforeClear() {
        set.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = set.iterator();
        // Advance iterator once to ensure it is not at the end
        assertTrue(it.hasNext());
        assertEquals(1, it.next());
        // Clear the set while iterator is still alive
        set.clear();
        // The iterator should now report no further elements but must not throw
        assertFalse(it.hasNext(), "Iterator should see no remaining elements after clear()");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() should throw NoSuchElementException");
    }

    @Test
    @DisplayName("clear() completes correctly when another thread adds elements concurrently")
    void testClearWithConcurrentAdd() throws Exception {
        // Fill set with initial data
        set.addAll(Arrays.asList(1, 2, 3));
        // Thread that will add elements after a short pause
        Runnable adder = () -> {
            try {
                // ensure overlap with clear()
                Thread.sleep(10);
                set.add(99);
                set.add(100);
            } catch (InterruptedException ignored) {
            }
        };
        Thread t = new Thread(adder);
        t.start();
        // Perform clear while the other thread may be adding
        set.clear();
        // wait for adder to finish
        t.join();
        // After clear, elements added after clear may remain
        int finalSize = set.size();
        assertTrue(finalSize >= 0 && finalSize <= 2, "Final size should be 0, 1, or 2 depending on timing, but never negative");
        // Verify that the set is still functional
        set.add(42);
        assertTrue(set.contains(42));
    }

    @Test
    @DisplayName("Clearing one instance does not affect another independent instance")
    void testIndependenceBetweenInstances() {
        ConcurrentSet<Integer> other = new ConcurrentSet<>();
        other.addAll(Arrays.asList(1, 2, 3));
        set.addAll(Arrays.asList(10, 20));
        assertEquals(2, set.size());
        assertEquals(3, other.size());
        set.clear();
        assertTrue(set.isEmpty(), "Cleared set should be empty");
        assertEquals(0, set.size());
        assertEquals(3, other.size(), "Other set should retain its elements");
        assertTrue(other.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    @DisplayName("clear() empties set after retainAll operation")
    void testClearAfterRetainAll() {
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        set.retainAll(Arrays.asList(2, 3));
        assertEquals(2, set.size());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    @DisplayName("clear() empties set after removeAll operation")
    void testClearAfterRemoveAll() {
        set.addAll(Arrays.asList(1, 2, 3, 4));
        set.removeAll(Arrays.asList(2, 4));
        assertEquals(2, set.size());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    @DisplayName("Calling toArray() before clear() does not affect clear()")
    void testToArrayBeforeClear() {
        set.addAll(Arrays.asList(7, 8, 9));
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[] { 7, 8, 9 }, arr);
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    @DisplayName("hashCode reflects empty set after clear()")
    void testHashCodeAfterClear() {
        set.addAll(Arrays.asList(1, 2, 3));
        int hashBefore = set.hashCode();
        assertNotEquals(0, hashBefore, "Non‑empty set should have non‑zero hashCode");
        set.clear();
        int hashAfter = set.hashCode();
        assertEquals(0, hashAfter, "Empty set should have hashCode == 0");
    }

    @Test
    @DisplayName("clear() changes equality semantics as expected")
    void testEqualsConsistency() {
        ConcurrentSet<Integer> other = new ConcurrentSet<>();
        other.addAll(Arrays.asList(1, 2, 3));
        set.addAll(Arrays.asList(1, 2, 3));
        assertEquals(set, other, "Sets with identical elements should be equal");
        set.clear();
        assertNotEquals(set, other, "Cleared set should no longer be equal to non‑cleared set");
        assertTrue(other.equals(other), "Other set should remain equal to itself");
    }

    @Test
    @DisplayName("After clear(), the set can be reused with add()")
    void testAddAfterClear() {
        set.addAll(Arrays.asList(1, 2));
        set.clear();
        assertTrue(set.isEmpty());
        boolean added = set.add(99);
        assertTrue(added, "add() should return true for new element");
        assertEquals(1, set.size());
        assertTrue(set.contains(99));
    }

    @ParameterizedTest(name = "clear() on set initially {0} elements")
    @ValueSource(ints = { 0, 5 })
    @DisplayName("Parameterized clear test for different initial sizes")
    void testParameterizedClear(int initialSize) {
        // Populate set with the requested number of distinct elements
        for (int i = 0; i < initialSize; i++) {
            set.add(i);
        }
        assertEquals(initialSize, set.size());
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertEquals(0, set.size(), "Size must be zero after clear()");
    }
}
