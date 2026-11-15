package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListSet;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_iterator_Test {

    /**
     * Instance under test – created fresh for each test method.
     */
    private ConcurrentNavigableSetNullSafe<String> set;

    /**
     * Runs before each test – initialise a new set instance.
     */
    @BeforeEach
    void setupBeforeEach() {
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    /**
     * Runs after each test – clear the set to aid GC and avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        set.clear();
    }

    /**
     * Verify iterator order for natural ordering, including {@code null}.
     */
    @Test
    void iteratorNaturalOrderWithNull() {
        // Arrange
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));
        // Act
        Iterator<String> it = set.iterator();
        // Assert – natural ordering puts null *last* (sentinel is ordered after all real elements)
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Verify iterator order when a custom comparator is supplied.
     */
    @Test
    void iteratorWithLengthComparator() {
        // Arrange – replace the set with one that uses a length‑based comparator
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        assertTrue(set.add("kiwi"));
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        // Act
        Iterator<String> it = set.iterator();
        // Assert – comparator orders by length, null (length 0) first
        // length 0
        assertNull(it.next());
        // length 4
        assertEquals("kiwi", it.next());
        // length 5
        assertEquals("apple", it.next());
        // length 6
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Verify that the iterator's {@code remove()} method deletes the last returned element.
     */
    @Test
    void iteratorRemoveDeletesElement() {
        // Arrange
        assertTrue(set.add("alpha"));
        assertTrue(set.add("beta"));
        assertTrue(set.add(null));
        Iterator<String> it = set.iterator();
        // Act – advance to "beta" and remove it
        assertEquals("alpha", it.next());
        assertEquals("beta", it.next());
        // should remove "beta"
        it.remove();
        // Assert – "beta" is gone, iterator continues correctly
        // next element after removal is null
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
        // The set now contains only "alpha" and null
        assertEquals(2, set.size());
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains(null));
        assertFalse(set.contains("beta"));
    }

    /**
     * Verify that the iterator works correctly after the set has been cleared and re‑populated.
     */
    @Test
    void iteratorAfterClearAndRepopulate() {
        // Arrange – populate, clear, then add new elements
        assertTrue(set.add("x"));
        assertTrue(set.add("y"));
        set.clear();
        assertTrue(set.isEmpty());
        assertTrue(set.add("first"));
        assertTrue(set.add(null));
        assertTrue(set.add("second"));
        // Act
        Iterator<String> it = set.iterator();
        // Assert – ordering reflects natural order (null last)
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Verify that the iterator is independent of the original set – modifications to the set after iterator creation do not affect the iterator's view.
     */
    @Test
    void iteratorSnapshotBehaviour() {
        // Arrange
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        Iterator<String> it = set.iterator();
        // Modify the set after iterator creation
        assertTrue(set.add("c"));
        assertTrue(set.remove("a"));
        // The iterator should still see the original snapshot (a, b) in order
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertFalse(it.hasNext());
        // The underlying set now contains b and c
        assertEquals(2, set.size());
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
        assertFalse(set.contains("a"));
    }

    /**
     * Iteration over a non‑empty set – natural order without null.
     */
    @Test
    @DisplayName("Iteration over non‑empty set – natural order")
    void iterationOverNonEmptySet() {
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        Iterator<String> it = set.iterator();
        // three elements should be present in sorted order
        assertTrue(it.hasNext());
        assertEquals("apple", it.next());
        assertTrue(it.hasNext());
        assertEquals("banana", it.next());
        assertTrue(it.hasNext());
        assertEquals("cherry", it.next());
        // after consuming all, hasNext must be false
        assertFalse(it.hasNext());
    }

    /**
     * Iteration over an empty set – hasNext() returns false immediately.
     */
    @Test
    @DisplayName("Iteration over empty set")
    void iterationOverEmptySet() {
        Iterator<Integer> emptyIt = new ConcurrentNavigableSetNullSafe<Integer>().iterator();
        assertFalse(emptyIt.hasNext());
    }

    /**
     * Iterator returns {@code null} for the masked null element.
     */
    @Test
    @DisplayName("Iterator returns null for masked null element")
    void iteratorReturnsNullForMaskedNull() {
        assertTrue(set.add(null));
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Iterator remove operation reduces the set size.
     */
    @Test
    @DisplayName("Iterator remove operation")
    void iteratorRemoveOperation() {
        assertTrue(set.add("x"));
        assertTrue(set.add("y"));
        Iterator<String> it = set.iterator();
        assertEquals("x", it.next());
        // remove "x"
        it.remove();
        assertEquals(1, set.size());
        assertFalse(set.contains("x"));
        assertTrue(set.contains("y"));
    }

    /**
     * Iterator remove without preceding next throws IllegalStateException.
     */
    @Test
    @DisplayName("Iterator remove without preceding next")
    void iteratorRemoveWithoutNextThrows() {
        assertTrue(set.add("elem"));
        Iterator<String> it = set.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    /**
     * Iterator reflects external removal – hasNext becomes false after element removed directly.
     */
    @Test
    @DisplayName("Iterator reflects external removal")
    void iteratorReflectsExternalRemoval() {
        assertTrue(set.add("only"));
        Iterator<String> it = set.iterator();
        // Remove directly from the set before using the iterator
        assertTrue(set.remove("only"));
        // The iterator should now report no remaining elements
        assertFalse(it.hasNext());
    }

    /**
     * Iterator order with a reverse‑order comparator.
     */
    @Test
    @DisplayName("Iterator order with custom reverse comparator")
    void iteratorOrderWithReverseComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        set = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertTrue(set.add("c"));
        Iterator<String> it = set.iterator();
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * iterator() when internal set is null should throw NullPointerException.
     */
    @Test
    @DisplayName("iterator() when internal set is null")
    void iteratorWhenInternalSetIsNull() throws Exception {
        // Use reflection to nullify the private field
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(set, null);
        assertThrows(NullPointerException.class, set::iterator);
    }

    /**
     * iterator() on an unmodifiable backing set should propagate UnsupportedOperationException.
     */
    @Test
    @DisplayName("iterator() on unmodifiable backing set")
    void iteratorOnUnmodifiableBackingSet() throws Exception {
        // Create a normal backing set, then wrap it as unmodifiable
        NavigableSet<String> backing = new ConcurrentSkipListSet<>();
        backing.add("x");
        backing.add("y");
        NavigableSet<String> unmodifiable = Collections.unmodifiableNavigableSet(backing);
        // Inject the unmodifiable set via reflection
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(set, unmodifiable);
        // The iterator() call should delegate to the unmodifiable set's iterator,
        // which throws UnsupportedOperationException on remove()
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals("x", it.next());
        // remove() on this iterator must throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /**
     * next() after iterator exhaustion should throw NoSuchElementException.
     */
    @Test
    @DisplayName("next() after iterator exhaustion")
    void nextAfterExhaustionThrows() {
        assertTrue(set.add("first"));
        assertTrue(set.add("second"));
        Iterator<String> it = set.iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * remove() called twice after a single next() should throw IllegalStateException.
     */
    @Test
    @DisplayName("remove() called twice after a single next()")
    void removeCalledTwiceThrows() {
        assertTrue(set.add("elem"));
        Iterator<String> it = set.iterator();
        assertEquals("elem", it.next());
        // first removal succeeds
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
    }

    /**
     * remove() on iterator backed by an unmodifiable set should propagate UnsupportedOperationException.
     */
    @Test
    @DisplayName("remove() on iterator backed by unmodifiable set")
    void removeOnIteratorWithUnmodifiableSetThrows() throws Exception {
        NavigableSet<String> backing = new ConcurrentSkipListSet<>();
        backing.add("a");
        NavigableSet<String> unmodifiable = Collections.unmodifiableNavigableSet(backing);
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(set, unmodifiable);
        Iterator<String> it = set.iterator();
        assertEquals("a", it.next());
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /**
     * next() returns element of incompatible type, causing ClassCastException.
     */
    @Test
    @DisplayName("next() returns element of incompatible type")
    void nextReturnsIncompatibleTypeThrows() throws Exception {
        // Insert an Integer directly into the internal set via reflection
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        @SuppressWarnings("unchecked")
        NavigableSet<Object> internal = (NavigableSet<Object>) internalSetField.get(set);
        // raw integer, not a String
        internal.add(123);
        Iterator<String> it = set.iterator();
        assertThrows(ClassCastException.class, it::next);
    }
}
