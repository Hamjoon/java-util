package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.CountDownLatch;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ConcurrentSet_size_Test {

    /**
     * The set instance used in each test.
     */
    private ConcurrentSet<Integer> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for every test to guarantee isolation.
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set to aid garbage collection.
        set.clear();
    }

    @Test
    void sizeOnNewSetIsZero() {
        Assertions.assertEquals(0, set.size(), "A newly created ConcurrentSet must report size 0.");
    }

    @Test
    void sizeAfterAddingElements() {
        set.add(10);
        set.add(20);
        set.add(30);
        Assertions.assertEquals(3, set.size(), "Size should reflect the number of distinct elements added.");
    }

    @Test
    void sizeIgnoresDuplicates() {
        set.add(5);
        // duplicate – should not increase size
        set.add(5);
        Assertions.assertEquals(1, set.size(), "Adding a duplicate element must not change the size.");
    }

    @Test
    void sizeAfterRemovingElements() {
        set.addAll(Arrays.asList(1, 2, 3, 4));
        Assertions.assertEquals(4, set.size(), "Pre‑condition check.");
        set.remove(2);
        set.remove(4);
        Assertions.assertEquals(2, set.size(), "Size must decrease when elements are removed.");
    }

    @Test
    void sizeAfterClear() {
        set.addAll(Arrays.asList(7, 8, 9));
        Assertions.assertTrue(set.size() > 0, "Pre‑condition check.");
        set.clear();
        Assertions.assertEquals(0, set.size(), "Clear must reset the size to zero.");
    }

    @Test
    void sizeAfterAddAll() {
        set.addAll(Arrays.asList(11, 12, 13));
        Assertions.assertEquals(3, set.size(), "addAll should increase size according to the number of unique elements.");
    }

    @Test
    void sizeRemainsConsistentWithConcurrentModifications() throws InterruptedException {
        final int threadCount = 4;
        final int perThreadOps = 5_000;
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        // Threads that add numbers
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < perThreadOps; j++) {
                    set.add(j);
                }
                latch.countDown();
            }).start();
            // Threads that remove numbers
            new Thread(() -> {
                for (int j = 0; j < perThreadOps; j++) {
                    set.remove(j);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        // After all threads finish, size must be between 0 and perThreadOps
        // (because each number may be present at most once).
        int actualSize = set.size();
        Assertions.assertTrue(actualSize >= 0 && actualSize <= perThreadOps, "Concurrent modifications should leave size in the expected range.");
    }

    @Test
    void sizeMatchesUnderlyingSetAfterConstructionFromCollection() {
        Set<Integer> source = new HashSet<>(Arrays.asList(100, 200, 300));
        ConcurrentSet<Integer> fromCollection = new ConcurrentSet<>(source);
        Assertions.assertEquals(source.size(), fromCollection.size(), "Constructor that accepts a Collection must initialise size correctly.");
    }

    @Test
    void sizeMatchesUnderlyingSetAfterConstructionFromSet() {
        Set<Integer> source = new HashSet<>(Arrays.asList(1, 2, 3, 4));
        ConcurrentSet<Integer> fromSet = new ConcurrentSet<>(source);
        Assertions.assertEquals(source.size(), fromSet.size(), "Constructor that accepts a Set must initialise size correctly.");
    }

    @Test
    void sizeAfterAddAllWithOverlappingElements() {
        // Add initial elements 1 and 2
        set.addAll(Arrays.asList(1, 2));
        // AddAll with overlapping element 2 and new elements 3,4
        set.addAll(Arrays.asList(2, 3, 4));
        Assertions.assertEquals(4, set.size(), "Duplicate elements supplied to addAll must not increase the size.");
    }

    @Test
    void sizeAfterRemoveAllWithNonExistentElements() {
        set.addAll(Arrays.asList(1, 2, 3));
        // Attempt to remove elements that are not present
        set.removeAll(Arrays.asList(4, 5));
        Assertions.assertEquals(3, set.size(), "removeAll with non‑existent elements must leave the size unchanged.");
    }

    @Test
    void sizeAfterRetainAllReducesSet() {
        set.addAll(Arrays.asList(1, 2, 3, 4));
        // Retain only 2 and 3
        set.retainAll(Arrays.asList(2, 3));
        Assertions.assertEquals(2, set.size(), "retainAll should shrink the set to only the retained elements.");
    }

    @Test
    void sizeAfterIteratorRemove() {
        set.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            Integer val = it.next();
            if (val.equals(2)) {
                // remove element 2 via iterator
                it.remove();
                break;
            }
        }
        Assertions.assertEquals(2, set.size(), "Iterator.remove() must correctly delete the element and reflect in size.");
    }

    @Test
    void sizeWhenAddingNullThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> set.add(null), "Adding null should throw NullPointerException.");
        Assertions.assertEquals(0, set.size(), "After failed null addition the set must remain empty.");
    }

    @Test
    void sizeAfterConcurrentRemovalsResultsInZero() throws InterruptedException {
        final int totalElements = 1_000;
        // Populate the set
        for (int i = 0; i < totalElements; i++) {
            set.add(i);
        }
        Assertions.assertEquals(totalElements, set.size(), "Pre‑condition check.");
        final int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        // Each thread removes a distinct slice of the range
        int slice = totalElements / threadCount;
        for (int t = 0; t < threadCount; t++) {
            final int start = t * slice;
            final int end = (t == threadCount - 1) ? totalElements : start + slice;
            executor.submit(() -> {
                for (int i = start; i < end; i++) {
                    set.remove(i);
                }
                latch.countDown();
            });
        }
        latch.await();
        executor.shutdownNow();
        Assertions.assertEquals(0, set.size(), "After all disjoint removals the set must be empty.");
    }

    @Test
    void sizeAfterMixedAddRemoveOperations() {
        // Add 1,2,3
        set.addAll(Arrays.asList(1, 2, 3));
        // Remove 2
        set.remove(2);
        // Add 4
        set.add(4);
        // Attempt to remove non‑existent 5
        set.remove(5);
        Assertions.assertEquals(3, set.size(), "Final size should reflect only the existing elements {1,3,4}.");
    }

    @Test
    void sizeAfterClearFollowedByNewAdditions() {
        set.addAll(Arrays.asList(1, 2));
        set.clear();
        Assertions.assertEquals(0, set.size(), "Size after clear must be 0.");
        set.add(3);
        Assertions.assertEquals(1, set.size(), "After clear and a single new addition size should be 1.");
    }

    @Test
    void sizeWhenInternalBackingSetIsNullThrowsNullPointerException() throws Exception {
        // Corrupt the internal backing set via reflection
        Field backingSetField = ConcurrentSet.class.getDeclaredField("set");
        backingSetField.setAccessible(true);
        backingSetField.set(set, null);
        Assertions.assertThrows(NullPointerException.class, () -> set.size(), "Invoking size() when the internal set is null must throw NullPointerException.");
    }
}
