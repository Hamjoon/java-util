package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_size_Test {

    /**
     * The set under test – created fresh before each test method.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor gives us a natural‑ordering set that accepts null.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help GC; not strictly necessary but keeps the pattern clear.
        if (set != null) {
            set.clear();
        }
    }

    static Stream<String> duplicateElementsProvider() {
        return Stream.of("a", "b", "c");
    }

    /**
     * Utility to replace the private {@code internalSet} field of a {@link ConcurrentNavigableSetNullSafe}
     * instance with a provided set.
     */
    private static void replaceInternalSet(NavigableSet<?> target, NavigableSet<Object> newInternal) throws Exception {
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(target, newInternal);
    }

    @Test
    void sizeEmptySet() {
        assertEquals(0, set.size(), "A newly created set should be empty");
    }

    @Test
    void sizeAfterAddingElementsIncludingNull() {
        assertTrue(set.add("apple"));
        assertTrue(set.add(null));
        assertTrue(set.add("banana"));
        assertEquals(3, set.size(), "Size should reflect three distinct elements (including null)");
    }

    @Test
    void sizeAfterRemovingElements() {
        set.add("apple");
        set.add("banana");
        set.add(null);
        assertEquals(3, set.size());
        assertTrue(set.remove("banana"));
        assertEquals(2, set.size());
        assertTrue(set.remove(null));
        assertEquals(1, set.size());
        assertTrue(set.remove("apple"));
        assertEquals(0, set.size());
    }

    @Test
    void sizeAfterClear() {
        set.add("alpha");
        set.add("beta");
        set.add(null);
        assertEquals(3, set.size());
        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }

    @Test
    void sizeWithCollectionConstructor() {
        Collection<String> source = Arrays.asList("one", "two", null, "three", "two");
        // Duplicate "two" should be ignored by the Set semantics.
        NavigableSet<String> collSet = new ConcurrentNavigableSetNullSafe<>(source);
        // Expected distinct elements: "one", "two", "three", null  => 4
        assertEquals(4, collSet.size());
        assertTrue(collSet.containsAll(Arrays.asList("one", "two", "three", null)));
    }

    @Test
    void sizeWithComparatorConstructor() {
        Comparator<String> reverse = Comparator.nullsFirst(Comparator.reverseOrder());
        NavigableSet<String> compSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertEquals(0, compSet.size());
        compSet.add("zeta");
        compSet.add("alpha");
        compSet.add(null);
        assertEquals(3, compSet.size());
        // Verify ordering does not affect size
        Iterator<String> it = compSet.iterator();
        // nullsFirst puts null first
        assertEquals(null, it.next());
        // reverse order: zeta > alpha
        assertEquals("zeta", it.next());
        assertEquals("alpha", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void sizeUnderConcurrentAdds() throws InterruptedException {
        // Use the default constructor (thread‑safe implementation).
        NavigableSet<Integer> concurrentSet = new ConcurrentNavigableSetNullSafe<>();
        final int threadCount = 4;
        // 4 * 250 = 1000 total elements
        final int perThread = 250;
        Thread[] workers = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int start = i * perThread;
            workers[i] = new Thread(() -> {
                for (int j = start; j < start + perThread; j++) {
                    concurrentSet.add(j);
                }
            });
            workers[i].start();
        }
        // Wait for all threads to finish
        for (Thread t : workers) {
            t.join();
        }
        assertEquals(threadCount * perThread, concurrentSet.size(), "Size must reflect all elements added concurrently");
    }

    @Test
    void sizeRemainsAccurateAfterMixedOperations() {
        set.add("first");
        set.add(null);
        set.add("second");
        assertEquals(3, set.size());
        set.remove("first");
        assertEquals(2, set.size());
        set.add("third");
        assertEquals(3, set.size());
        // removes the smallest element according to natural order (null is smallest)
        set.pollFirst();
        assertEquals(2, set.size());
        set.clear();
        assertEquals(0, set.size());
    }

    @Test
    void sizeAfterAddingThreeDistinctElements() {
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals(3, set.size(), "After adding three distinct elements size should be 3");
    }

    @ParameterizedTest(name = "{index} => duplicate ''{0}'' does not increase size")
    @MethodSource("duplicateElementsProvider")
    void sizeAfterAddingDuplicateElement(String duplicate) {
        set.add("a");
        set.add("b");
        set.add("c");
        int sizeBefore = set.size();
        assertTrue(set.add(duplicate) == false, "Adding a duplicate should return false");
        assertEquals(sizeBefore, set.size(), "Size must remain unchanged after adding duplicate");
    }

    @Test
    void sizeAfterRemovingExistingElement() {
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals(3, set.size());
        assertTrue(set.remove("b"));
        assertEquals(2, set.size());
    }

    @Test
    void sizeAfterReplacingInternalSetWithEmptySetViaReflection() throws Exception {
        // Replace the private internalSet with a brand‑new empty ConcurrentSkipListSet
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        // empty set
        f.set(set, new ConcurrentSkipListSet<>());
        assertEquals(0, set.size(), "After swapping internalSet with an empty set, size() must be 0");
    }

    @Test
    void sizeThrowsNullPointerExceptionWhenInternalSetIsNull() throws Exception {
        // Nullify the internalSet via reflection
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(set, null);
        assertThrows(NullPointerException.class, set::size, "Calling size() when internalSet is null should throw NPE");
    }

    @Test
    void sizeThrowsClassCastExceptionWhenInternalSetContainsWrongType() throws Exception {
        // Create a set with a comparator that expects Strings
        Comparator<String> cmp = Comparator.naturalOrder();
        NavigableSet<Object> badInternal = new ConcurrentSkipListSet<>(Comparator.comparing(Object::toString));
        // Insert an Integer directly via reflection – this will break the comparator's expectations
        // Integer
        badInternal.add(42);
        // Swap the internalSet
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(set, badInternal);
        // The size() call itself may not trigger the comparator, but many implementations
        // of ConcurrentSkipListSet evaluate the comparator when the set is non‑empty.
        // Therefore we assert that a ClassCastException is thrown.
        assertThrows(ClassCastException.class, set::size, "If internalSet contains an element of incompatible type, size() should raise ClassCastException");
    }

    @Test
    void sizeAfterAddAllCollectionWithMixedExistingAndNewElements() {
        set.add("x");
        set.add("y");
        set.add("z");
        // Collection contains two existing elements (x, y) and one new element (newElem)
        Collection<String> toAdd = Arrays.asList("x", "y", "newElem");
        boolean changed = set.addAll(toAdd);
        assertTrue(changed, "addAll should report a change when new elements are added");
        assertEquals(4, set.size(), "Size should be 4 after adding a collection with two duplicates and one new element");
    }
}
