package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_size_Test {

    /**
     * Instance used for the majority of the tests (Integer values).
     */
    private ConcurrentSet<Integer> intSet;

    /**
     * Instance used for tests that need to store null values (String values).
     */
    private ConcurrentSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a clean set.
        intSet.clear();
        stringSet.clear();
    }

    @Test
    void sizeOfNewSetIsZero() {
        assertEquals(0, intSet.size(), "A newly created set must have size 0");
    }

    @Test
    void sizeIncrementsWhenAddingElements() {
        assertTrue(intSet.add(10));
        assertEquals(1, intSet.size(), "Size should be 1 after adding a single element");
        assertTrue(intSet.add(20));
        assertEquals(2, intSet.size(), "Size should be 2 after adding a second distinct element");
    }

    @Test
    void sizeDoesNotChangeWhenAddingDuplicates() {
        assertTrue(intSet.add(5));
        assertEquals(1, intSet.size());
        // Adding the same element again must return false and leave size unchanged
        assertFalse(intSet.add(5));
        assertEquals(1, intSet.size(), "Size must stay unchanged when a duplicate is added");
    }

    @Test
    void sizeDecrementsWhenRemovingElements() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertEquals(3, intSet.size());
        assertTrue(intSet.remove(2));
        assertEquals(2, intSet.size());
        // Removing a non‑existent element must not affect the size
        assertFalse(intSet.remove(99));
        assertEquals(2, intSet.size());
    }

    @Test
    void sizeAfterClearIsZero() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertEquals(3, intSet.size());
        intSet.clear();
        assertEquals(0, intSet.size(), "Size must be 0 after clear()");
        assertTrue(intSet.isEmpty());
    }

    @Test
    void sizeReflectsInitialCollection() {
        ConcurrentSet<Integer> fromCollection = new ConcurrentSet<>(Arrays.asList(7, 8, 9));
        assertEquals(3, fromCollection.size(), "Constructor with Collection should reflect its size");
    }

    @Test
    void sizeReflectsInitialSet() {
        HashSet<Integer> source = new HashSet<>(Arrays.asList(11, 12));
        ConcurrentSet<Integer> fromSet = new ConcurrentSet<>(source);
        assertEquals(2, fromSet.size(), "Constructor with Set should reflect its size");
    }

    @Test
    void sizeCountsNullElements() {
        assertTrue(stringSet.add(null), "Adding null must succeed");
        assertEquals(1, stringSet.size(), "Size must count the null element");
        assertTrue(stringSet.add("a"));
        assertEquals(2, stringSet.size(), "Size must count both null and a regular element");
    }

    @Test
    void sizeAfterRemovingNull() {
        stringSet.addAll(Arrays.asList(null, "x", "y"));
        assertEquals(3, stringSet.size());
        assertTrue(stringSet.remove(null));
        assertEquals(2, stringSet.size(), "Size must decrement after removing the null sentinel");
    }

    @Test
    void sizeUnderConcurrentAddAndRemove() throws InterruptedException {
        int threadCount = 4;
        int opsPerThread = 5_000;
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        // Threads that add numbers 0..opsPerThread-1
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    intSet.add(j);
                }
                latch.countDown();
            }).start();
            // Threads that remove the same numbers
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    intSet.remove(j);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        int finalSize = intSet.size();
        assertTrue(finalSize >= 0 && finalSize <= opsPerThread, "After concurrent adds/removes the size must be between 0 and " + opsPerThread);
    }

    @Test
    void sizeWithEmptyCollectionConstructor() {
        ConcurrentSet<Integer> empty = new ConcurrentSet<>(Collections.emptyList());
        assertEquals(0, empty.size(), "Constructor given an empty collection must produce size 0");
    }

    @Test
    void sizeWhenConstructorCollectionContainsDuplicates() {
        ConcurrentSet<Integer> dup = new ConcurrentSet<>(Arrays.asList(1, 1, 2, 2, 2));
        // Set semantics remove duplicates
        assertEquals(2, dup.size(), "Constructor must respect Set semantics and eliminate duplicates");
    }

    @Test
    void sizeUnderConcurrentMixedAdds() throws InterruptedException {
        // Five threads each add a distinct string value.
        String[] values = { "T1", "T2", "T3", "T4", "T5" };
        CountDownLatch latch = new CountDownLatch(values.length);
        for (String v : values) {
            new Thread(() -> {
                intSet.add(v);
                latch.countDown();
            }).start();
        }
        latch.await();
        assertEquals(5, intSet.size(), "After five distinct concurrent adds size must be 5");
    }

    @Test
    void sizeUnderConcurrentMixAddRemoveSpecificElements() throws InterruptedException {
        // Three threads add unique elements, two threads remove two of them.
        String[] toAdd = { "A1", "A2", "A3" };
        String[] toRemove = { "A1", "A2" };
        CountDownLatch latch = new CountDownLatch(toAdd.length + toRemove.length);
        for (String a : toAdd) {
            new Thread(() -> {
                intSet.add(a);
                latch.countDown();
            }).start();
        }
        for (String r : toRemove) {
            new Thread(() -> {
                // small sleep to increase chance that add happens first
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
                intSet.remove(r);
                latch.countDown();
            }).start();
        }
        latch.await();
        assertEquals(1, intSet.size(), "Only \"A3\" should remain after mixed concurrent adds/removes");
        assertTrue(intSet.contains("A3"));
    }

    @Test
    void sizeAfterAddAllWithDuplicates() {
        // addAll should only count unique elements
        intSet.addAll(Arrays.asList("X", "Y", "X", "Z"));
        assertEquals(3, intSet.size(), "addAll must ignore duplicate entries");
    }

    @Test
    void sizeAfterRetainAll() {
        intSet.addAll(Arrays.asList("A", "B", "C", "D"));
        // retain only B and C (and a non‑existent E)
        intSet.retainAll(Arrays.asList("B", "C", "E"));
        assertEquals(2, intSet.size(), "retainAll should shrink set to elements present in the argument");
        assertTrue(intSet.containsAll(Arrays.asList("B", "C")));
    }

    @Test
    void sizeAfterRemoveAll() {
        intSet.addAll(Arrays.asList("A", "B", "C", "D"));
        // remove A, C and a non‑existent Z
        intSet.removeAll(Arrays.asList("A", "C", "Z"));
        assertEquals(2, intSet.size(), "removeAll must delete matching elements only");
        assertTrue(intSet.containsAll(Arrays.asList("B", "D")));
    }

    @Test
    void sizeAfterIteratorRemove() {
        intSet.addAll(Arrays.asList("M", "N"));
        Iterator<String> it = (Iterator<String>) intSet.iterator();
        assertEquals("M", it.next());
        // removes "M"
        it.remove();
        assertEquals(1, intSet.size(), "Iterator.remove must affect underlying set");
        assertFalse(intSet.contains("M"));
        assertTrue(intSet.contains("N"));
    }

    @Test
    void sizeStabilityWhenCalledRepeatedly() {
        intSet.addAll(Arrays.asList("P", "Q", "R", "S"));
        for (int i = 0; i < 4; i++) {
            assertEquals(4, intSet.size(), "Repeated size() calls must return a stable value");
        }
    }

    @Test
    void sizeScalesWithLargeNumberOfElements() {
        for (int i = 0; i < 1_000; i++) {
            intSet.add(i);
        }
        assertEquals(1_000, intSet.size(), "Size must correctly reflect a large number of distinct elements");
    }

    @Test
    void sizeThrowsWhenInternalSetIsNull() throws Exception {
        // Use reflection to corrupt the internal 'set' field.
        Field field = ConcurrentSet.class.getDeclaredField("set");
        field.setAccessible(true);
        field.set(intSet, null);
        assertThrows(NullPointerException.class, intSet::size, "If the internal Set reference is null, size() should throw NPE");
    }
}
