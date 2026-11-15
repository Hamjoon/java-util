package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_iterator_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private ConcurrentSet<Integer> set;

    /**
     * The set under test – created fresh for each test method.
     */
    private ConcurrentSet<Integer> intSet;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a new, empty set before each test.
        intSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to aid GC and to guarantee no cross‑test contamination.
        if (intSet != null) {
            intSet.clear();
        }
    }

    // Example provider for future parameterized tests.
    static Stream<List<Integer>> populatedDataProvider() {
        return Stream.of(Arrays.asList(1, 2, 3), Arrays.asList(10, 20, 30, 40));
    }

    /**
     * Verify that the iterator returned by {@code ConcurrentSet.iterator()}
     * traverses all elements that were added to the set and that the values
     * are correctly unwrapped (i.e., the original values are returned).
     */
    @Test
    void testIteratorTraversesAllElements() {
        List<Integer> expected = Arrays.asList(10, 20, 30, 40);
        intSet.addAll(expected);
        // Obtain the iterator from the set.
        Iterator<Integer> it = intSet.iterator();
        // Collect the values returned by the iterator.
        int index = 0;
        while (it.hasNext()) {
            Integer value = it.next();
            assertTrue(expected.contains(value), "Iterator should return only elements that were added to the set");
            // Ensure each expected element appears exactly once.
            assertEquals(expected.get(index), value, "Elements should be returned in the order of the underlying set's iterator");
            index++;
        }
        assertEquals(expected.size(), index, "Iterator should have visited exactly the number of elements added");
    }

    /**
     * Verify that {@code Iterator.remove()} removes the current element from the
     * underlying {@code ConcurrentSet}.
     */
    @Test
    void testIteratorRemoveUpdatesSet() {
        intSet.addAll(Arrays.asList(1, 2, 3, 4));
        Iterator<Integer> it = intSet.iterator();
        // Advance to the element '2' and remove it.
        while (it.hasNext()) {
            Integer val = it.next();
            if (val.equals(2)) {
                it.remove();
                break;
            }
        }
        // After removal, the set must no longer contain '2' but must still contain the others.
        assertFalse(intSet.contains(2), "Set should not contain the element that was removed via iterator");
        assertTrue(intSet.containsAll(Arrays.asList(1, 3, 4)), "Set should still contain the elements that were not removed");
        assertEquals(3, intSet.size(), "Set size should reflect the removal of a single element");
    }

    /**
     * Verify that the iterator works correctly when the set contains {@code null}
     * (the {@code ConcurrentSet} stores a sentinel internally, but the iterator
     * must unwrap it back to {@code null} for the caller).
     */
    @Test
    void testIteratorHandlesNullElements() {
        intSet.addAll(Arrays.asList(5, null, 7));
        Iterator<Integer> it = intSet.iterator();
        boolean nullSeen = false;
        int sum = 0;
        while (it.hasNext()) {
            Integer v = it.next();
            if (v == null) {
                nullSeen = true;
            } else {
                sum += v;
            }
        }
        assertTrue(nullSeen, "Iterator should return the null element that was added");
        assertEquals(12, sum, "Sum of non‑null elements should be correct");
        assertEquals(3, intSet.size(), "Set size must include the null element");
    }

    /**
     * Verify that a freshly obtained iterator reports {@code hasNext()} as {@code false}
     * when the set is empty.
     */
    @Test
    void testIteratorOnEmptySet() {
        Iterator<Integer> it = intSet.iterator();
        assertFalse(it.hasNext(), "Iterator on an empty set should report hasNext() == false");
        assertThrows(java.util.NoSuchElementException.class, it::next, "Calling next() on an empty iterator should throw NoSuchElementException");
    }

    /**
     * Iterator over a populated set – order is not guaranteed.
     */
    @Test
    void testIteratorOverPopulatedSet_Unordered() {
        Set<Integer> values = new HashSet<>(Arrays.asList(10, 20, 30));
        intSet.addAll(values);
        Iterator<Integer> it = intSet.iterator();
        // First call to hasNext must be true.
        assertTrue(it.hasNext(), "Iterator should have next element after population");
        Set<Integer> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(values, seen, "Iterator must return exactly the inserted elements (order irrelevant)");
        assertFalse(it.hasNext(), "After consuming all elements, hasNext() must be false");
    }

    /**
     * Verify that calling remove() before any next() throws IllegalStateException.
     */
    @Test
    void testIteratorRemoveWithoutNextThrows() {
        ConcurrentSet<Double> doubleSet = new ConcurrentSet<>();
        doubleSet.add(3.14);
        Iterator<Double> it = doubleSet.iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    /**
     * Verify that iterator does not throw ConcurrentModificationException
     * when the underlying set is modified after the iterator is obtained.
     */
    @Test
    void testIteratorConcurrentModificationBehaviour() {
        ConcurrentSet<Long> longSet = new ConcurrentSet<>();
        longSet.add(100L);
        Iterator<Long> it = longSet.iterator();
        // Modify the set after iterator creation.
        longSet.add(200L);
        // The iterator must still be able to return the element it already knows about.
        assertTrue(it.hasNext(), "Iterator should still have the original element");
        assertEquals(100L, it.next(), "Iterator should return the element that existed when created");
        // Depending on implementation, the newly added element may or may not be seen.
        // The important contract is that no CME is thrown.
        // We simply ensure that calling hasNext again does not explode.
        assertDoesNotThrow(it::hasNext);
    }

    /**
     * Verify that the private {@code unwrap} method correctly casts the stored
     * Object to the generic type without causing a ClassCastException.
     */
    @Test
    void testIteratorUnwrapAndTypeSafety() throws Exception {
        // Create a raw ConcurrentSet and add an Integer as Object.
        ConcurrentSet rawSet = new ConcurrentSet();
        // Use reflection to reach the private 'set' field and add an Integer.
        Field setField = ConcurrentSet.class.getDeclaredField("set");
        setField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Object> internal = (Set<Object>) setField.get(rawSet);
        // stored as Object
        internal.add(42);
        // Cast the raw set to a generic view.
        @SuppressWarnings("unchecked")
        ConcurrentSet<Integer> typedView = (ConcurrentSet<Integer>) rawSet;
        Iterator<Integer> it = typedView.iterator();
        assertTrue(it.hasNext(), "Iterator should have the inserted element");
        Integer value = it.next();
        assertEquals(42, value, "unwrap should return the original Integer value");
    }

    /**
     * Verify that a ClassCastException is thrown when the underlying set contains
     * an element that cannot be assigned to the iterator's generic type.
     */
    @Test
    void testIteratorNextWithIncompatibleElementThrows() throws Exception {
        // Create a raw ConcurrentSet<String> and inject an Integer via reflection.
        ConcurrentSet<String> stringSet = new ConcurrentSet<>();
        Field setField = ConcurrentSet.class.getDeclaredField("set");
        setField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Object> internal = (Set<Object>) setField.get(stringSet);
        // Integer inserted bypassing generics
        internal.add(99);
        Iterator<String> it = stringSet.iterator();
        // The first call to next() should attempt to cast Integer to String.
        assertThrows(ClassCastException.class, it::next, "Iterator.next() must throw ClassCastException when element type is incompatible");
    }
}
