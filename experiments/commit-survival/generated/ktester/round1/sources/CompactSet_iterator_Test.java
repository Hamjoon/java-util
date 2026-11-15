package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.NoSuchElementException;

class CompactSet_iterator_Test {

    private CompactSet<String> compactSet;

    @BeforeEach
    void setupBeforeEach() {
        compactSet = new CompactSet<>();
        // Populate with a few elements for the tests
        compactSet.add("alpha");
        compactSet.add("beta");
        compactSet.add("gamma");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid cross‑test contamination
        compactSet.clear();
    }

    @Test
    void iteratorTraversesAllElements() {
        Iterator<String> it = compactSet.iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            String next = it.next();
            assertNotNull(next, "Iterator should not return null for stored elements");
            seen.add(next);
        }
        // The iterator should have visited exactly the elements we added
        assertEquals(compactSet.size(), seen.size(), "Iterator should have visited the same number of elements as the set size");
        assertTrue(seen.containsAll(compactSet), "All elements returned by the iterator must be present in the original set");
    }

    @Test
    void iteratorRemoveDeletesCurrentElement() {
        Iterator<String> it = compactSet.iterator();
        // Capture the first element returned by the iterator
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        String first = it.next();
        // Remove that element via the iterator
        it.remove();
        // The set must no longer contain the removed element
        assertFalse(compactSet.contains(first), "Set should not contain the element removed via iterator");
        // Size must be reduced by one
        assertEquals(2, compactSet.size(), "Set size should be decremented after a successful iterator.remove()");
    }

    @Test
    void iteratorRemoveWithoutNextThrows() {
        Iterator<String> it = compactSet.iterator();
        IllegalStateException ex = assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
        assertEquals("remove() called on an Iterator before calling next()", ex.getMessage());
    }

    @Test
    void iteratorRemoveCalledTwiceWithoutNextThrows() {
        Iterator<String> it = compactSet.iterator();
        // First valid removal
        assertTrue(it.hasNext());
        String element = it.next();
        it.remove();
        assertFalse(compactSet.contains(element));
        // Second removal without another next()
        IllegalStateException ex = assertThrows(IllegalStateException.class, it::remove, "Calling remove() twice in a row without a subsequent next() must throw IllegalStateException");
        assertEquals("remove() called on an Iterator before calling next()", ex.getMessage());
    }

    /**
     * Group: Iterator on empty CompactSet
     * Verify that an iterator over an empty set reports no next element,
     * throws NoSuchElementException on next(), and throws IllegalStateException
     * on remove() before any next().
     */
    @Test
    void iteratorOnEmptySetBehavesCorrectly() {
        CompactSet<String> empty = new CompactSet<>();
        Iterator<String> it = empty.iterator();
        assertFalse(it.hasNext(), "hasNext() must be false for an empty set");
        assertThrows(NoSuchElementException.class, it::next, "next() on an empty iterator should throw NoSuchElementException");
        IllegalStateException ex = assertThrows(IllegalStateException.class, it::remove, "remove() before next() on an empty iterator must throw IllegalStateException");
        assertEquals("remove() called on an Iterator before calling next()", ex.getMessage());
    }

    /**
     * Group: Iterator behavior on non‑empty CompactSet
     * Verify hasNext()/next() contract on a freshly created iterator.
     */
    @Test
    void iteratorHasNextAndNextContract() {
        CompactSet<String> set = new CompactSet<>();
        set.add("a");
        set.add("b");
        set.add("c");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext(), "hasNext() should be true before any next() call");
        Set<String> visited = new HashSet<>();
        while (it.hasNext()) {
            String element = it.next();
            assertNotNull(element);
            // ensure we don't see duplicates
            assertFalse(visited.contains(element), "Iterator should not return duplicate elements");
            visited.add(element);
        }
        // After the loop, iterator must be exhausted
        assertFalse(it.hasNext(), "hasNext() must be false after all elements have been visited");
        assertEquals(set.size(), visited.size(), "Iterator should have visited exactly the number of elements present in the set");
    }

    /**
     * Verify that remove() after a successful next() deletes the element
     * from the underlying CompactSet and that the iterator can continue.
     */
    @Test
    void iteratorRemoveAfterNextUpdatesSetAndIterator() {
        CompactSet<String> set = new CompactSet<>();
        set.add("a");
        set.add("b");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        String first = it.next();
        // Perform removal via iterator
        it.remove();
        // Underlying set must reflect the removal
        assertEquals(1, set.size(), "Set size should decrease after iterator.remove()");
        assertFalse(set.contains(first), "Removed element must no longer be in the set");
        // Iterator should still be able to iterate over remaining elements
        assertTrue(it.hasNext(), "Iterator should still have elements after removal");
        String remaining = it.next();
        assertTrue(set.contains(remaining), "Remaining element returned by iterator must be present in set");
        assertFalse(it.hasNext(), "Iterator should be exhausted after returning the last element");
    }

    /**
     * Verify that calling remove() twice consecutively without an intervening next()
     * throws IllegalStateException.
     */
    @Test
    void iteratorRemoveTwiceConsecutiveThrows() {
        CompactSet<String> set = new CompactSet<>();
        set.add("only");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        it.next();
        // first removal is OK
        it.remove();
        IllegalStateException ex = assertThrows(IllegalStateException.class, it::remove, "Second remove() without next() must throw IllegalStateException");
        assertEquals("remove() called on an Iterator before calling next()", ex.getMessage());
    }

    /**
     * Verify that after a successful removal, a subsequent remove() without next()
     * also throws IllegalStateException (same as previous test but with multiple elements).
     */
    @Test
    void iteratorRemoveAfterValidRemovalThenAgainThrows() {
        CompactSet<String> set = new CompactSet<>();
        set.add("x");
        set.add("y");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        // retrieve first
        it.next();
        // valid removal
        it.remove();
        // immediate second remove should fail
        IllegalStateException ex = assertThrows(IllegalStateException.class, it::remove);
        assertEquals("remove() called on an Iterator before calling next()", ex.getMessage());
    }

    /**
     * Group: Iterator reflects concurrent modifications.
     * The iterator is based on a snapshot copy, so modifications to the set after
     * the iterator is obtained must not cause a ConcurrentModificationException.
     * The iterator should still iterate over the elements that were present at the
     * moment of its creation.
     */
    @Test
    void iteratorSnapshotIgnoresConcurrentModification() {
        CompactSet<Integer> set = new CompactSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        // snapshot taken here
        Iterator<Integer> it = set.iterator();
        // Consume the first element (order is not guaranteed)
        assertTrue(it.hasNext());
        Integer first = it.next();
        // Remove a different element directly from the set
        Integer toRemove = (first == 1) ? 2 : 1;
        set.remove(toRemove);
        // Continue iteration – should not throw ConcurrentModificationException
        Set<Integer> iterated = new HashSet<>();
        iterated.add(first);
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // The iterator must have visited exactly three elements (the snapshot size)
        assertEquals(3, iterated.size(), "Iterator should iterate over the snapshot (original three elements) even after concurrent removal");
        // The underlying set now has only two elements
        assertEquals(2, set.size(), "Set size should reflect the direct removal");
        assertFalse(set.contains(toRemove), "Set must no longer contain the directly removed element");
    }

    /**
     * Group: Iterator next() after full traversal.
     * After all elements have been consumed, calling next() must throw NoSuchElementException.
     */
    @Test
    void iteratorNextAfterExhaustionThrows() {
        CompactSet<String> set = new CompactSet<>();
        set.add("single");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals("single", it.next());
        // Iterator is now exhausted
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an exhausted iterator should throw NoSuchElementException");
    }

    /**
     * Group: Iterator remove() when current entry equals sentinel NO_ENTRY.
     * Add the internal sentinel value (NO_ENTRY) to the set, retrieve it via next(),
     * and then call remove(). The iterator should treat this as a pre‑next state and
     * throw IllegalStateException.
     */
    @Test
    void iteratorRemoveWhenCurrentEntryIsSentinelThrows() throws Exception {
        // Use a raw CompactSet to hold the sentinel (type is Object)
        CompactSet<Object> set = new CompactSet<>();
        // Obtain the private static NO_ENTRY field via reflection
        Field noEntryField = CompactSet.class.getDeclaredField("NO_ENTRY");
        noEntryField.setAccessible(true);
        Object sentinel = noEntryField.get(null);
        // Add the sentinel to the set
        set.add(sentinel);
        Iterator<Object> it = set.iterator();
        assertTrue(it.hasNext());
        Object retrieved = it.next();
        assertSame(sentinel, retrieved, "The iterator should return the sentinel value we added");
        // Because currentEntry == NO_ENTRY, remove() must throw IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, it::remove, "remove() should throw IllegalStateException when currentEntry equals NO_ENTRY");
        assertEquals("remove() called on an Iterator before calling next()", ex.getMessage());
    }
}
