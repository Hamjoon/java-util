package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListSet;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_iterator_Test {

    /**
     * The set under test – created once for the whole test suite.
     */
    private static ConcurrentNavigableSetNullSafe<String> sharedSet;

    /**
     * A fresh set instance for each test method.
     */
    private ConcurrentNavigableSetNullSafe<String> testSet;

    /**
     * Fresh instance for each test method.
     */
    private ConcurrentNavigableSetNullSafe<String> set;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the default (natural) ordering for the shared instance.
        sharedSet = new ConcurrentNavigableSetNullSafe<>();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a new set for each test to avoid cross‑test contamination.
        testSet = new ConcurrentNavigableSetNullSafe<>();
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared after each test.
        testSet.clear();
        set.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Explicitly clear the shared set – not strictly required but keeps the
        // test suite tidy if more tests are added later.
        sharedSet.clear();
    }

    @Test
    void iteratorReturnsElementsInNaturalOrderIncludingNull() {
        // Arrange
        testSet.add("apple");
        testSet.add("banana");
        testSet.add(null);
        testSet.add("cherry");
        // Act
        Iterator<String> it = testSet.iterator();
        // Assert – natural ordering puts null last (sentinel is ordered after all real values)
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void iteratorRespectsCustomComparatorAndUnmasksNull() {
        // Arrange – comparator based on string length (null is considered length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        testSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        testSet.add("kiwi");
        // length 5
        testSet.add("apple");
        // length 6
        testSet.add("banana");
        // length 0
        testSet.add(null);
        // Act
        Iterator<String> it = testSet.iterator();
        // Assert – order defined by the comparator, null appears first
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

    @Test
    void iteratorRemoveDeletesCurrentElementFromSet() {
        // Arrange
        testSet.add("first");
        testSet.add("second");
        testSet.add(null);
        Iterator<String> it = testSet.iterator();
        // Act & Assert – remove the first element
        assertEquals("first", it.next());
        it.remove();
        assertFalse(testSet.contains("first"));
        assertEquals(2, testSet.size());
        // Remove the null element
        assertEquals("second", it.next());
        assertEquals(null, it.next());
        // removes null
        it.remove();
        assertFalse(testSet.contains(null));
        assertEquals(1, testSet.size());
    }

    @Test
    void iteratorReflectsConcurrentModifications() throws InterruptedException {
        // Arrange – populate the set with a range of integers as strings
        for (int i = 0; i < 1000; i++) {
            testSet.add(String.valueOf(i));
        }
        Iterator<String> it = testSet.iterator();
        // Start a background thread that adds a new range while the iterator is in use
        Thread modifier = new Thread(() -> {
            for (int i = 1000; i < 2000; i++) {
                testSet.add(String.valueOf(i));
            }
        });
        modifier.start();
        // Consume a few elements from the original iterator
        for (int i = 0; i < 10; i++) {
            assertTrue(it.hasNext());
            it.next();
        }
        // Wait for the modifier to finish
        modifier.join();
        // The iterator should continue over the snapshot it captured at creation time.
        // It must not throw ConcurrentModificationException because the underlying
        // set is a ConcurrentSkipListSet.
        while (it.hasNext()) {
            // simply iterate to the end
            it.next();
        }
        // The set now contains 2000 elements (0‑1999)
        assertEquals(2000, testSet.size());
        assertTrue(testSet.contains("0"));
        assertTrue(testSet.contains("1999"));
    }

    @Test
    void iteratorEmptySet_behaviour() {
        // No elements have been added.
        Iterator<String> it = set.iterator();
        // hasNext should be false and next() must throw NoSuchElementException.
        assertFalse(it.hasNext(), "Empty iterator should report no next element");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    @Test
    void iteratorSingleElement_behaviour() {
        set.add("alpha");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext(), "Iterator should have next for a single‑element set");
        assertEquals("alpha", it.next(), "The only element should be returned");
        assertFalse(it.hasNext(), "After consuming the element, hasNext() must be false");
    }

    @Test
    void iteratorMultipleElements_naturalOrder() {
        // Add in unsorted order to verify natural ordering of the iterator.
        set.add("c");
        set.add("a");
        set.add("b");
        Iterator<String> it = set.iterator();
        List<String> observed = new ArrayList<>();
        while (it.hasNext()) {
            observed.add(it.next());
        }
        assertEquals(List.of("a", "b", "c"), observed, "Iterator must follow the natural ordering of the NavigableSet");
    }

    @Test
    void iteratorSingleNullElement_returnsNull() {
        set.add(null);
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext(), "Iterator should report a null element as present");
        assertNull(it.next(), "The element returned must be null");
        assertFalse(it.hasNext(), "After the null element, iterator must be exhausted");
    }

    @Test
    void iteratorRemoveDeletesElement() {
        set.add("beta");
        Iterator<String> it = set.iterator();
        assertEquals("beta", it.next());
        // should delete "beta"
        it.remove();
        assertFalse(set.contains("beta"), "Element must be removed from the set");
        assertEquals(0, set.size(), "Set size must be zero after removal");
        assertFalse(it.hasNext(), "Iterator should have no further elements");
    }

    @Test
    void iteratorRemoveDeletesNullElement() {
        set.add(null);
        Iterator<String> it = set.iterator();
        assertNull(it.next());
        // delete the null sentinel
        it.remove();
        assertFalse(set.contains(null), "Null sentinel must be removed");
        assertEquals(0, set.size(), "Set should be empty after removing null");
        assertFalse(it.hasNext(), "Iterator must be exhausted after removal");
    }

    @Test
    void iteratorRemoveWithoutNext_throwsIllegalStateException() {
        set.add("gamma");
        Iterator<String> it = set.iterator();
        // remove() before any next()
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    void iteratorConsecutiveRemoveCalls_throwsIllegalStateException() {
        set.add("delta");
        Iterator<String> it = set.iterator();
        assertEquals("delta", it.next());
        // first removal is legal
        it.remove();
        // second removal without an intervening next()
        assertThrows(IllegalStateException.class, it::remove, "Second remove() without next() must throw IllegalStateException");
    }

    @Test
    void iteratorWeaklyConsistentView() {
        set.add("x");
        Iterator<String> it = set.iterator();
        // Modify the set after iterator creation.
        set.add("y");
        // The iterator must not throw CME and may or may not see "y".
        List<String> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        // At minimum, the element present at creation time must be observed.
        assertTrue(collected.contains("x"), "Iterator must contain the element that existed at creation time");
        // It must not contain duplicates and must not throw.
        assertFalse(collected.contains(null), "Iterator should not contain unexpected nulls");
    }

    @Test
    void iteratorNextWithMismatchedInternalType_throwsClassCastException() throws Exception {
        // Create a set whose internalSet is a ConcurrentSkipListSet<Object> containing an Integer.
        // Use reflection to inject this malformed internal set.
        ConcurrentNavigableSetNullSafe<String> malformedSet = new ConcurrentNavigableSetNullSafe<>();
        // Access the private field 'internalSet'.
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        // Prepare a ConcurrentSkipListSet that holds an Integer (which is comparable).
        ConcurrentSkipListSet<Object> rawSet = new ConcurrentSkipListSet<>();
        // Integer element, not a String.
        rawSet.add(42);
        // Replace the internal set with the malformed one.
        internalSetField.set(malformedSet, rawSet);
        // Obtain the iterator and trigger the cast inside unmaskNull().
        Iterator<String> it = malformedSet.iterator();
        assertThrows(ClassCastException.class, it::next, "Iterating over a set containing an element of the wrong type must throw ClassCastException");
    }

    @ParameterizedTest(name = "{index} => elements={0}")
    @ValueSource(strings = { "single", "multiple" })
    void iteratorParameterizedBasicTraversal(String scenario) {
        if ("single".equals(scenario)) {
            set.add("alpha");
        } else {
            set.addAll(List.of("c", "a", "b"));
        }
        Iterator<String> it = set.iterator();
        List<String> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }
        if ("single".equals(scenario)) {
            assertEquals(List.of("alpha"), result);
        } else {
            assertEquals(List.of("a", "b", "c"), result);
        }
    }
}
