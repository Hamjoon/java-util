package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_isEmpty_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        intSet = null;
        stringSet = null;
    }

    // ---------- parameterized test for basic behavior group ----------
    private static Stream<Arguments> basicBehaviorProvider() {
        return Stream.of(Arguments.of(() -> new ConcurrentSet<Integer>(), true, "newly created set"), Arguments.of(() -> {
            ConcurrentSet<Integer> s = new ConcurrentSet<>();
            s.add(1);
            return s;
        }, false, "after adding a single element"), Arguments.of(() -> {
            ConcurrentSet<Integer> s = new ConcurrentSet<>();
            s.add(2);
            s.remove(2);
            return s;
        }, true, "add then remove same element"), Arguments.of(() -> {
            ConcurrentSet<Integer> s = new ConcurrentSet<>();
            s.addAll(Arrays.asList(1, 2, 3));
            s.clear();
            return s;
        }, true, "add several then clear"));
    }

    @Test
    void testIsEmptyOnNewSet() {
        assertTrue(intSet.isEmpty(), "A newly created set must be empty");
        assertEquals(0, intSet.size(), "Size of a new set must be 0");
    }

    @Test
    void testIsEmptyAfterAdd() {
        assertTrue(intSet.add(42), "add should return true for a new element");
        assertFalse(intSet.isEmpty(), "Set should not be empty after adding an element");
        assertEquals(1, intSet.size(), "Size must be 1 after one addition");
    }

    @Test
    void testIsEmptyAfterClear() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertFalse(intSet.isEmpty(), "Set should not be empty before clear");
        intSet.clear();
        assertTrue(intSet.isEmpty(), "Set must be empty after clear");
        assertEquals(0, intSet.size(), "Size must be 0 after clear");
    }

    @Test
    void testIsEmptyAfterRemoveAll() {
        intSet.addAll(Arrays.asList(10, 20, 30));
        assertFalse(intSet.isEmpty(), "Set should not be empty before removeAll");
        intSet.removeAll(Arrays.asList(10, 20, 30));
        assertTrue(intSet.isEmpty(), "Set must be empty after removing all elements");
    }

    @Test
    void testIsEmptyWithNullElement() {
        // ConcurrentSet supports null via a sentinel value
        assertTrue(stringSet.add(null), "Adding null should succeed");
        assertFalse(stringSet.isEmpty(), "Set should not be empty after adding null");
        assertTrue(stringSet.contains(null), "Set must contain null");
        assertTrue(stringSet.remove(null), "Removing null should succeed");
        assertTrue(stringSet.isEmpty(), "Set must be empty after removing null");
    }

    @Test
    void testIsEmptyWithCollectionConstructor() {
        Set<Integer> source = new HashSet<>(Arrays.asList(5, 6, 7));
        ConcurrentSet<Integer> fromCollection = new ConcurrentSet<>(source);
        assertFalse(fromCollection.isEmpty(), "Set constructed from a non‑empty collection must not be empty");
        fromCollection.clear();
        assertTrue(fromCollection.isEmpty(), "Set must be empty after clear");
    }

    @Test
    void testIsEmptyConcurrentAddRemove() throws InterruptedException {
        final int threadCount = 4;
        final int opsPerThread = 5000;
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        // Threads that add numbers
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    intSet.add(j);
                }
                latch.countDown();
            }).start();
            // Threads that remove numbers
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    intSet.remove(j);
                }
                latch.countDown();
            }).start();
        }
        // wait for all threads to finish
        latch.await();
        // After all concurrent activity, the set may be empty or contain a subset,
        // but calling isEmpty must not throw and must be consistent with size().
        assertEquals(intSet.isEmpty(), intSet.size() == 0, "isEmpty() result must match size() == 0 after concurrent modifications");
    }

    // ---------- duplicate element addition ----------
    @Test
    void testDuplicateAddDoesNotMakeSetNonEmpty() {
        assertTrue(stringSet.add("A"), "First addition of 'A' should succeed");
        assertFalse(stringSet.add("A"), "Second addition of duplicate 'A' must return false");
        assertFalse(stringSet.isEmpty(), "Set must not be empty after duplicate adds");
        assertEquals(1, stringSet.size(), "Size should remain 1 after duplicate addition");
    }

    @Test
    void testDuplicateAddAndRemoveBehaviour() {
        assertTrue(stringSet.add("B"), "First addition of 'B' should succeed");
        assertFalse(stringSet.add("B"), "Duplicate add returns false");
        assertTrue(stringSet.remove("B"), "First removal of 'B' should succeed");
        // element still present because duplicate add didn't create a second entry
        assertFalse(stringSet.isEmpty(), "Set must still contain 'B' after one removal");
        assertTrue(stringSet.remove("B"), "Second removal should finally empty the set");
        assertTrue(stringSet.isEmpty(), "Set must be empty after removing the only element");
    }

    // ---------- remove non‑existent element ----------
    @Test
    void testRemoveNonExistentFromEmptySet() {
        assertFalse(intSet.remove(999), "Removing a never‑added element should return false");
        assertTrue(intSet.isEmpty(), "Set remains empty after failed removal");
    }

    @Test
    void testRemoveDifferentElementWhenOneExists() {
        assertTrue(stringSet.add("X"), "Add element X");
        assertFalse(stringSet.remove("Y"), "Attempt to remove non‑existent Y should return false");
        assertFalse(stringSet.isEmpty(), "Set must still be non‑empty because X is present");
        assertTrue(stringSet.contains("X"), "X should still be present");
    }

    // ---------- retainAll ----------
    @Test
    void testRetainAllEmptyCollectionEmptiesSet() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertFalse(intSet.isEmpty(), "Set should not be empty before retainAll");
        assertTrue(intSet.retainAll(Collections.emptySet()), "retainAll(empty) must modify the set");
        assertTrue(intSet.isEmpty(), "Set must be empty after retainAll with empty collection");
    }

    @Test
    void testRetainAllPartialKeepsElements() {
        stringSet.addAll(Arrays.asList("a", "b"));
        assertTrue(stringSet.retainAll(Collections.singleton("a")), "retainAll with 'a' should modify the set");
        assertFalse(stringSet.isEmpty(), "Set must not be empty after partial retainAll");
        assertEquals(1, stringSet.size(), "Only one element should remain");
        assertTrue(stringSet.contains("a"));
    }

    // ---------- removeAll ----------
    @Test
    void testRemoveAllClearsSet() {
        stringSet.addAll(Arrays.asList("alpha", "beta"));
        assertTrue(stringSet.removeAll(Arrays.asList("alpha", "beta")), "removeAll should modify the set");
        assertTrue(stringSet.isEmpty(), "Set must be empty after removing all elements");
    }

    @Test
    void testRemoveAllPartialRemoval() {
        stringSet.addAll(Arrays.asList("cat", "dog"));
        assertTrue(stringSet.removeAll(Collections.singleton("cat")), "removeAll should modify the set");
        assertFalse(stringSet.isEmpty(), "Set must not be empty after partial removal");
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("dog"));
    }

    // ---------- addAll with empty collection ----------
    @Test
    void testAddAllEmptyCollectionLeavesSetEmpty() {
        assertFalse(intSet.addAll(Collections.emptyList()), "addAll(empty) must return false");
        assertTrue(intSet.isEmpty(), "Set must stay empty after addAll with empty collection");
    }

    @Test
    void testAddAllNonEmptyCollectionMakesSetNonEmpty() {
        assertTrue(stringSet.addAll(Arrays.asList("elem")), "addAll with one element must return true");
        assertFalse(stringSet.isEmpty(), "Set must be non‑empty after addAll");
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("elem"));
    }

    // ---------- iterator remove ----------
    @Test
    void testIteratorRemoveAllElementsEmptiesSet() {
        stringSet.addAll(Arrays.asList("x", "y"));
        Iterator<String> it = stringSet.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertTrue(stringSet.isEmpty(), "Set must be empty after iterator removes all elements");
    }

    @Test
    void testIteratorRemoveSingleElementLeavesSetNonEmpty() {
        stringSet.addAll(Arrays.asList("p", "q", "r"));
        Iterator<String> it = stringSet.iterator();
        assertTrue(it.hasNext());
        // points to first element
        it.next();
        // remove first element only
        it.remove();
        assertFalse(stringSet.isEmpty(), "Set must still contain elements after single iterator removal");
        assertEquals(2, stringSet.size());
    }

    // ---------- concurrent modifications ----------
    @Test
    void testConcurrentAddThenRemoveSameElementsResultsInEmpty() throws InterruptedException {
        final int elements = 100;
        CountDownLatch latch = new CountDownLatch(2);
        Thread adder = new Thread(() -> {
            for (int i = 0; i < elements; i++) {
                intSet.add(i);
            }
            latch.countDown();
        });
        Thread remover = new Thread(() -> {
            for (int i = 0; i < elements; i++) {
                intSet.remove(i);
            }
            latch.countDown();
        });
        adder.start();
        remover.start();
        latch.await();
        assertTrue(intSet.isEmpty(), "Set should be empty when adds and removes are perfectly matched");
    }

    @Test
    void testConcurrentAddDifferentElementsResultsInNonEmpty() throws InterruptedException {
        final int elements = 100;
        CountDownLatch latch = new CountDownLatch(2);
        Thread adder1 = new Thread(() -> {
            for (int i = 0; i < elements; i++) {
                intSet.add(i);
            }
            latch.countDown();
        });
        Thread adder2 = new Thread(() -> {
            for (int i = elements; i < elements * 2; i++) {
                intSet.add(i);
            }
            latch.countDown();
        });
        adder1.start();
        adder2.start();
        latch.await();
        assertFalse(intSet.isEmpty(), "Set must be non‑empty when two threads add distinct elements");
        assertEquals(elements * 2, intSet.size(), "Size must reflect all distinct additions");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicBehaviorProvider")
    void testBasicBehaviorParameterized(Supplier<ConcurrentSet<?>> supplier, boolean expectedEmpty, String description) {
        ConcurrentSet<?> set = supplier.get();
        assertEquals(expectedEmpty, set.isEmpty(), "isEmpty() must return " + expectedEmpty + " for case: " + description);
    }
}
