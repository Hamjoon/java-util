package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_iterator_Test {

    /**
     * iterator traversal - multiple elements (parameterized)
     */
    static Stream<List<Integer>> multipleElementsProvider() {
        return Stream.of(Arrays.asList(1, 2, 3), Arrays.asList(42), Arrays.asList(-5, 0, 5, 10));
    }

    /**
     * Verify that the iterator returns exactly the elements that were added,
     * regardless of order (a Set has no guaranteed iteration order).
     */
    @Test
    void testIteratorReturnsAllElements() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        List<Integer> added = Arrays.asList(10, 20, 30, 40);
        set.addAll(added);
        // Collect elements via the iterator
        List<Integer> iterated = new ArrayList<>();
        for (Integer i : set) {
            iterated.add(i);
        }
        // The iterator should have produced the same number of elements
        assertEquals(added.size(), iterated.size(), "Iterator should produce the same number of elements as were added");
        // All added elements must be present in the iterated collection
        assertTrue(iterated.containsAll(added), "Iterator must return all elements that were added to the set");
    }

    /**
     * Verify that the {@code remove()} method of the iterator removes the
     * element from the underlying set.
     */
    @Test
    void testIteratorRemove() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("alpha", "beta", "gamma"));
        Iterator<String> it = set.iterator();
        boolean removedBeta = false;
        while (it.hasNext()) {
            String value = it.next();
            if ("beta".equals(value)) {
                // remove via iterator
                it.remove();
                removedBeta = true;
                break;
            }
        }
        assertTrue(removedBeta, "Iterator should have found and removed 'beta'");
        assertFalse(set.contains("beta"), "Set must no longer contain the removed element");
        assertEquals(2, set.size(), "Set size should reflect the removal");
    }

    /**
     * Verify that the iterator works correctly when the set contains {@code null}
     * (the implementation uses a sentinel internally). The iterator should unwrap
     * the sentinel and return a real {@code null} reference.
     */
    @Test
    void testIteratorWithNullElement() {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("x", null, "y"));
        List<Object> iterated = new ArrayList<>();
        for (Object o : set) {
            iterated.add(o);
        }
        assertEquals(3, iterated.size(), "Iterator must return three elements");
        assertTrue(iterated.contains(null), "Iterator must return the null element");
        assertTrue(iterated.containsAll(Arrays.asList("x", "y")), "Iterator must return the non‑null elements");
    }

    /**
     * Verify that calling {@code iterator()} multiple times yields independent
     * iterators that can each traverse the set from the beginning.
     */
    @Test
    void testMultipleIteratorsIndependence() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it1 = set.iterator();
        Iterator<Integer> it2 = set.iterator();
        // Advance first iterator once
        assertTrue(it1.hasNext());
        Integer first = it1.next();
        assertNotNull(first);
        // Second iterator should still start from the beginning
        List<Integer> secondIterated = new ArrayList<>();
        while (it2.hasNext()) {
            secondIterated.add(it2.next());
        }
        assertEquals(3, secondIterated.size(), "Second iterator must see all elements regardless of first iterator's progress");
        assertTrue(secondIterated.containsAll(Arrays.asList(1, 2, 3)), "Second iterator must contain the full set of elements");
    }

    /**
     * iterator traversal - empty set
     */
    @Test
    void testEmptySetIterator() {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        Iterator<Object> it = set.iterator();
        assertFalse(it.hasNext(), "Empty set iterator must report no next element");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    @ParameterizedTest
    @MethodSource("multipleElementsProvider")
    void testMultipleElementsTraversal(List<Integer> values) {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(values);
        Iterator<Integer> it = set.iterator();
        Set<Integer> seen = new HashSet<>();
        while (it.hasNext()) {
            Integer v = it.next();
            assertTrue(values.contains(v), "Iterator returned an unexpected element");
            assertFalse(seen.contains(v), "Iterator returned duplicate element");
            seen.add(v);
        }
        assertEquals(new HashSet<>(values), seen, "Iterator must have visited each inserted element exactly once");
        assertFalse(it.hasNext(), "hasNext() must be false after all elements are consumed");
    }

    /**
     * iterator remove - valid removal (already covered, but exercised with size > 2)
     */
    @Test
    void testIteratorRemoveValid() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        // could be any element
        String first = it.next();
        it.remove();
        assertFalse(set.contains(first), "Element removed via iterator must disappear from set");
        assertEquals(2, set.size(), "Set size must decrease after removal");
    }

    /**
     * iterator remove - illegal state (remove before next)
     */
    @Test
    void testIteratorRemoveIllegalState() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2));
        Iterator<Integer> it = set.iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    /**
     * iterator weak consistency under concurrent modification (addition)
     */
    @Test
    void testIteratorWeakConsistencyAddDuringIteration() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("initial");
        Iterator<String> it = set.iterator();
        // Add a new element after iterator creation
        set.add("concurrent");
        // The iterator must not throw CME; it may or may not see the new element.
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertTrue(iterated.contains("initial"), "Iterator must at least return the element that existed when created");
        // No assertion about presence of "concurrent" – both outcomes are acceptable.
    }

    /**
     * iterator next after removal
     */
    @Test
    void testIteratorNextAfterRemoval() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("one", "two", "three"));
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        // e.g., "one"
        String first = it.next();
        // remove the element just returned
        it.remove();
        // The next element must be different from the removed one
        assertTrue(it.hasNext(), "Iterator should still have elements after removal");
        String second = it.next();
        assertNotEquals(first, second, "Next element must not be the one that was removed");
        // Continue to the end and verify hasNext() behavior
        while (it.hasNext()) {
            it.next();
        }
        assertFalse(it.hasNext(), "Iterator must report no more elements after consumption");
    }

    /**
     * iterator remove called twice consecutively
     */
    @Test
    void testIteratorRemoveTwiceConsecutive() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(10, 20));
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        // first element
        it.next();
        // first valid removal
        it.remove();
        // Second remove without an intervening next() must fail
        assertThrows(IllegalStateException.class, it::remove, "Second consecutive remove() must throw IllegalStateException");
    }

    /**
     * iterator with null element (already covered, but add explicit hasNext check)
     */
    @Test
    void testIteratorWithNullElementHasNext() {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(null, "z"));
        Iterator<Object> it = set.iterator();
        // The iterator must report true for the first element (null)
        assertTrue(it.hasNext(), "Iterator should have a next element (null)");
        Object first = it.next();
        assertNull(first, "First element returned should be null");
        // Second element
        assertTrue(it.hasNext(), "Iterator should have a second element");
        assertEquals("z", it.next(), "Second element should be the non‑null value");
        assertFalse(it.hasNext(), "Iterator must be exhausted after two elements");
    }

    /**
     * iterator after clear operation
     */
    @Test
    void testIteratorAfterClear() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = set.iterator();
        // Clear the set before using the iterator
        set.clear();
        assertFalse(it.hasNext(), "Iterator created before clear must behave as empty after clear");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after clear must throw NoSuchElementException");
    }

    /**
     * iterator weak consistency under concurrent removal
     */
    @Test
    void testIteratorWeakConsistencyRemovalDuringIteration() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2));
        Iterator<Integer> it = set.iterator();
        // Remove an element directly from the set after iterator creation
        set.remove(1);
        // The iterator must not throw CME; it may return the still‑present element or skip the removed one.
        List<Integer> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        // The only possible values are [] or [2] (or [1] if the iterator captured it before removal)
        assertTrue(collected.isEmpty() || collected.contains(2) || collected.contains(1), "Iterator should not fail and may or may not contain the removed element");
    }

    /**
     * iterator remove after iteration completed
     */
    @Test
    void testIteratorRemoveAfterExhaustion() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("x", "y"));
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            // consume all elements
            it.next();
        }
        // Now iterator is exhausted; remove() should fail
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() after iterator is exhausted must throw IllegalStateException");
    }

    /**
     * iterator next throws NoSuchElementException after exhaustion
     */
    @Test
    void testIteratorNextThrowsAfterExhaustion() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(99);
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals(99, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next, "Calling next() when hasNext() is false must throw NoSuchElementException");
    }

    /**
     * iterator generic type safety
     */
    @Test
    void testIteratorGenericTypeSafety() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("foo", "bar"));
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            // compile‑time type is String, but we double‑check at runtime
            Object obj = it.next();
            assertTrue(obj instanceof String, "Each element returned by the iterator must be an instance of the declared generic type");
        }
    }

    /**
     * iterator with null internal set (reflection)
     */
    @Test
    void testIteratorWithNullInternalSet() throws Exception {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        // Use reflection to set the private field `set` to null
        Field f = ConcurrentSet.class.getDeclaredField("set");
        f.setAccessible(true);
        f.set(set, null);
        assertThrows(NullPointerException.class, set::iterator, "Invoking iterator() when internal set is null must throw NullPointerException");
    }

    /**
     * iterator next type mismatch (reflection)
     */
    @Test
    void testIteratorNextTypeMismatch() throws Exception {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        // Obtain the internal Set<Object> and inject a String directly
        Field f = ConcurrentSet.class.getDeclaredField("set");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Object> internal = (Set<Object>) f.get(set);
        // The internal implementation uses a ConcurrentHashMap-backed set; we can safely add a String.
        internal.add("not-an-integer");
        Iterator<Integer> it = set.iterator();
        // The first call to next() should attempt to cast the String to Integer and fail.
        assertThrows(ClassCastException.class, it::next, "Calling next() when the underlying element type does not match generic type must throw ClassCastException");
    }

    /**
     * iterator remove unsupported operation (unmodifiable backing set)
     */
    @Test
    void testIteratorRemoveUnsupportedOperation() throws Exception {
        // Create a normal ConcurrentSet and then replace its backing set with an unmodifiable one.
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b"));
        // Replace the internal set via reflection
        Field f = ConcurrentSet.class.getDeclaredField("set");
        f.setAccessible(true);
        Set<Object> unmodifiable = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a", "b")));
        f.set(set, unmodifiable);
        Iterator<String> it = set.iterator();
        // Advance to first element
        assertTrue(it.hasNext());
        it.next();
        // remove() on an iterator of an unmodifiable set should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() on an unmodifiable backing set must throw UnsupportedOperationException");
    }
}
