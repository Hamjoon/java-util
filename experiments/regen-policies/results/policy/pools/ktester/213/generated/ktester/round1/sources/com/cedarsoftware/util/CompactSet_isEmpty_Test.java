package com.cedarsoftware.util;

import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class CompactSet_isEmpty_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private CompactSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Default (empty) CompactSet for the majority of tests.
        set = new CompactSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly necessary but keeps the pattern clear.
        set = null;
    }

    /**
     * Removes every element from the supplied set using its iterator.
     *
     * @param s the set to be emptied
     */
    private static void clearViaIterator(Set<?> s) {
        Iterator<?> it = s.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertTrue(s.isEmpty(), "Set should be empty after iterator removal");
    }

    @Test
    void newSetIsEmpty() {
        // Newly created CompactSet should be empty.
        assertTrue(set.isEmpty(), "A newly created CompactSet must be empty");
        assertEquals(0, set.size(), "Size of a new set must be 0");
    }

    @Test
    void isEmptyAfterAddAndRemove() {
        assertTrue(set.isEmpty(), "Pre‑condition: set should start empty");
        assertTrue(set.add("alpha"), "First add must succeed");
        assertFalse(set.isEmpty(), "Set should not be empty after adding an element");
        assertEquals(1, set.size());
        assertTrue(set.remove("alpha"), "Removal of the existing element must succeed");
        assertTrue(set.isEmpty(), "Set must be empty after the sole element is removed");
        assertEquals(0, set.size());
    }

    @Test
    void isEmptyAfterClear() {
        set.addAll(Arrays.asList("one", "two", "three"));
        assertFalse(set.isEmpty(), "Set should not be empty after adding elements");
        assertEquals(3, set.size());
        set.clear();
        assertTrue(set.isEmpty(), "Set must be empty after clear()");
        assertEquals(0, set.size());
    }

    @Test
    void isEmptyWhenCreatedFromCollection() {
        Collection<String> source = new HashSet<>(Arrays.asList("x", "y"));
        CompactSet<String> fromCollection = new CompactSet<>(source);
        assertFalse(fromCollection.isEmpty(), "Set built from a non‑empty collection must not be empty");
        assertEquals(2, fromCollection.size());
        // Clean up
        fromCollection.clear();
        assertTrue(fromCollection.isEmpty(), "Set must become empty after clear()");
    }

    @Test
    void isEmptyWithBuilderConfiguration() {
        // Build a case‑insensitive, insertion‑ordered set with a custom compact size.
        CompactSet<String> built = CompactSet.<String>builder().caseSensitive(false).insertionOrder().compactSize(10).build();
        assertTrue(built.isEmpty(), "Builder‑created set should start empty");
        assertTrue(built.add("Apple"));
        assertFalse(built.isEmpty(), "Set should not be empty after adding an element");
        assertTrue(built.add("Banana"));
        assertEquals(2, built.size());
        // Adding a duplicate (case‑insensitive) does not change emptiness.
        assertFalse(built.add("apple"), "Duplicate (case‑insensitive) add should return false");
        assertFalse(built.isEmpty(), "Set should still be non‑empty");
        // Remove all elements via iterator to verify that iterator‑based removal also updates emptiness.
        clearViaIterator(built);
        assertTrue(built.isEmpty(), "Set must be empty after iterator‑based removal of all elements");
    }

    @Test
    void isEmptyAfterIteratorRemoval() {
        set.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(set.isEmpty(), "Set should not be empty before iterator removal");
        clearViaIterator(set);
        assertTrue(set.isEmpty(), "Set must be empty after iterator removal of all elements");
    }

    @Test
    void isEmptyAfterSingleAdd() {
        assertTrue(set.isEmpty(), "Pre‑condition: set should start empty");
        assertTrue(set.add("first"), "Adding the first element must succeed");
        assertFalse(set.isEmpty(), "Set should not be empty after adding an element");
        assertEquals(1, set.size());
    }

    @Test
    void isEmptyAfterRemoveAllCollection() {
        set.addAll(Arrays.asList("alpha", "beta", "gamma"));
        assertFalse(set.isEmpty(), "Set should not be empty before removeAll");
        assertEquals(3, set.size());
        Collection<String> toRemove = Arrays.asList("alpha", "beta", "gamma");
        assertTrue(set.removeAll(toRemove), "removeAll should report that the set changed");
        assertTrue(set.isEmpty(), "Set must be empty after removeAll removes all elements");
        assertEquals(0, set.size());
    }

    @Test
    void isEmptyAfterAddingNullElement() {
        // CompactSet permits null values – this is verified by the test.
        assertTrue(set.isEmpty(), "Pre‑condition: set should start empty");
        assertTrue(set.add(null), "Adding null should succeed if the implementation permits it");
        assertFalse(set.isEmpty(), "Set should not be empty after adding null");
        assertEquals(1, set.size());
    }

    @Test
    void isEmptyAfterAddAll() {
        Collection<String> many = Arrays.asList("x", "y", "z");
        assertTrue(set.isEmpty(), "Pre‑condition: set should start empty");
        assertTrue(set.addAll(many), "addAll should report that the set changed");
        assertFalse(set.isEmpty(), "Set should not be empty after addAll");
        assertEquals(3, set.size());
    }
}