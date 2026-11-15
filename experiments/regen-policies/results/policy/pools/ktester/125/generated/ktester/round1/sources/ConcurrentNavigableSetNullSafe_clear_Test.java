package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_clear_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor for all tests
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        set = null;
    }

    @Test
    void clearOnEmptySetShouldLeaveItEmpty() {
        assertTrue(set.isEmpty(), "Set should be empty before clear");
        assertEquals(0, set.size(), "Size should be 0 before clear");
        set.clear();
        assertTrue(set.isEmpty(), "Set should remain empty after clear");
        assertEquals(0, set.size(), "Size should still be 0 after clear");
    }

    @Test
    void clearAfterAddingElementsIncludingNullShouldEmptySet() {
        // Populate the set with normal elements and a null
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));
        assertEquals(4, set.size(), "Set should contain 4 elements before clear");
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
        assertTrue(set.contains("cherry"));
        assertTrue(set.contains(null));
        // Verify iteration order (natural ordering with null sentinel at the end)
        Iterator<String> it = set.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
        // Perform the clear operation
        set.clear();
        // After clear, the set must be empty and contain no elements
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size(), "Size should be 0 after clear");
        assertFalse(set.contains("apple"));
        assertFalse(set.contains(null));
    }

    @Test
    void clearAfterLargeNumberOfElementsShouldResultInEmptySet() {
        // Add a large number of distinct elements
        for (int i = 0; i < 1_000; i++) {
            assertTrue(set.add("value-" + i));
        }
        assertEquals(1_000, set.size(), "Set should contain 1,000 elements before clear");
        // Clear the set
        set.clear();
        // Verify that the set is empty
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size(), "Size should be 0 after clear");
        // Ensure that previously added elements are no longer present
        assertFalse(set.contains("value-0"));
        assertFalse(set.contains("value-999"));
    }

    @Test
    void clearDoesNotThrowWhenSetContainsOnlyNull() {
        assertTrue(set.add(null));
        assertEquals(1, set.size(), "Set should contain the null element before clear");
        assertTrue(set.contains(null));
        // Clear should succeed without exceptions
        assertDoesNotThrow(() -> set.clear());
        assertTrue(set.isEmpty(), "Set should be empty after clearing the only null element");
        assertEquals(0, set.size(), "Size should be 0 after clear");
        assertFalse(set.contains(null));
    }

    @Test
    void clearInvokedTwiceShouldRemainSafe() {
        set.add("x");
        set.add("y");
        set.clear();
        assertTrue(set.isEmpty());
        assertDoesNotThrow(() -> set.clear());
        assertTrue(set.isEmpty(), "Set should stay empty after second clear");
        assertEquals(0, set.size(), "Size must remain 0 after second clear");
    }

    @Test
    void clearOnHeadSetViewClearsOriginalSet() {
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        NavigableSet<String> head = set.headSet("c", true);
        assertEquals(3, head.size());
        head.clear();
        assertTrue(head.isEmpty(), "HeadSet view should be empty after clear");
        assertTrue(set.isEmpty(), "Original set must be empty after clearing headSet view");
        assertEquals(0, set.size());
    }

    @Test
    void clearOnTailSetViewClearsOriginalSet() {
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        NavigableSet<String> tail = set.tailSet("b", false);
        assertEquals(2, tail.size());
        tail.clear();
        assertTrue(tail.isEmpty(), "TailSet view should be empty after clear");
        assertTrue(set.isEmpty(), "Original set must be empty after clearing tailSet view");
        assertEquals(0, set.size());
    }

    @Test
    void clearOnDescendingSetViewClearsOriginalSet() {
        set.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> descending = set.descendingSet();
        assertEquals(3, descending.size());
        descending.clear();
        assertTrue(descending.isEmpty(), "DescendingSet view should be empty after clear");
        assertTrue(set.isEmpty(), "Original set must be empty after clearing descendingSet view");
        assertEquals(0, set.size());
    }

    @Test
    void clearAfterDuplicateAdditionsLeavesSetEmpty() {
        assertTrue(set.add("dup"));
        assertFalse(set.add("dup"));
        assertFalse(set.add("dup"));
        assertEquals(1, set.size());
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size());
    }

    @Test
    void clearAfterConcurrentAdditionsResultsInEmptySet() throws InterruptedException {
        final int THREAD_COUNT = 4;
        final int PER_THREAD = 250;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger added = new AtomicInteger();
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < PER_THREAD; j++) {
                        String element = "t" + threadId + "-v" + j;
                        if (set.add(element)) {
                            added.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await();
        executor.shutdownNow();
        assertEquals(THREAD_COUNT * PER_THREAD, added.get(), "All elements should have been added before clear");
        assertEquals(THREAD_COUNT * PER_THREAD, set.size(), "Set size must reflect all concurrently added elements");
        assertDoesNotThrow(() -> set.clear());
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size());
    }

    @Test
    void clearWhenInternalSetIsNullThrowsNullPointerException() throws Exception {
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(wrapper, null);
        Executable callClear = wrapper::clear;
        assertThrows(NullPointerException.class, callClear, "Calling clear() with a null internalSet should throw NPE");
    }

    @Test
    void clearOnUnmodifiableInternalSetPropagatesUnsupportedOperationException() throws Exception {
        NavigableSet<Object> modifiable = new ConcurrentSkipListSet<>();
        NavigableSet<Object> unmodifiable = Collections.unmodifiableNavigableSet(modifiable);
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(wrapper, unmodifiable);
        Executable callClear = wrapper::clear;
        assertThrows(UnsupportedOperationException.class, callClear, "clear() should propagate UnsupportedOperationException from an unmodifiable internal set");
    }

    @Test
    void clearPropagatesRuntimeExceptionFromInternalSet() throws Exception {
        ???;
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(wrapper, throwingSet);
        Executable callClear = wrapper::clear;
        IllegalStateException ex = assertThrows(IllegalStateException.class, callClear, "clear() should propagate IllegalStateException from the underlying set");
        assertEquals("forced failure", ex.getMessage());
    }
}
