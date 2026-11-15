package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_clear_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a set with a mixture of values including null
        set = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(set.add("alpha"));
        assertTrue(set.add("beta"));
        assertTrue(set.add(null));
        assertTrue(set.add("gamma"));
        assertEquals(4, set.size());
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the reference is cleared for GC; the clear() test itself already empties the set
        set = null;
    }

    @Test
    void testClearEmptiesSet() {
        // Preconditions verified in @BeforeEach
        assertFalse(set.isEmpty());
        // Invoke the method under test
        set.clear();
        // Verify the set is now empty
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        // Verify that navigation methods behave as on an empty set
        assertNull(set.first());
        assertNull(set.last());
        assertFalse(set.iterator().hasNext());
        // Adding after clear should work normally
        assertTrue(set.add("delta"));
        assertEquals(1, set.size());
        assertTrue(set.contains("delta"));
    }

    @Test
    void testClearWhenAlreadyEmpty() {
        // Clear the set first
        set.clear();
        assertTrue(set.isEmpty());
        // Call clear again – should remain empty and not throw
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    void testClearRemovesNullElement() {
        assertTrue(set.contains(null));
        // Clear the set
        set.clear();
        // Ensure null is no longer present
        assertFalse(set.contains(null));
        assertFalse(set.contains("alpha"));
        assertFalse(set.contains("beta"));
        assertFalse(set.contains("gamma"));
    }

    @Test
    void testClearDoesNotAffectComparatorBehaviour() {
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableSet<String> compSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        compSet.add("apple");
        compSet.add("banana");
        compSet.add(null);
        // Verify ordering before clear
        Iterator<String> it = compSet.iterator();
        // null is treated as smallest sentinel
        assertEquals(null, it.next());
        // reverse order: banana > apple
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
        // Clear and re‑add to ensure comparator is still applied
        compSet.clear();
        assertTrue(compSet.isEmpty());
        compSet.add("cherry");
        compSet.add("date");
        Iterator<String> it2 = compSet.iterator();
        // reverse order
        assertEquals("date", it2.next());
        assertEquals("cherry", it2.next());
        assertFalse(it2.hasNext());
    }

    /* -------------------------------------------------------------
     *  Delegation verification using a Mockito spy
     * ------------------------------------------------------------- */
    @Test
    void testClearDelegatesToInternalSetExactlyOnce() throws Exception {
        // Create a mock NavigableSet and spy on it
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockInternal = mock(NavigableSet.class);
        when(mockInternal.iterator()).thenReturn(Collections.emptyIterator());
        // Instantiate the class via reflection and inject the mock
        ConcurrentNavigableSetNullSafe<String> spySet = new ConcurrentNavigableSetNullSafe<>();
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(spySet, mockInternal);
        // Call clear() – should delegate exactly once
        spySet.clear();
        // Verify delegation
        verify(mockInternal, times(1)).clear();
    }

    /* -------------------------------------------------------------
     *  Exceptional scenarios
     * ------------------------------------------------------------- */
    @Test
    void testClearThrowsWhenInternalSetIsNull() throws Exception {
        // Build an instance and force its internalSet to null via reflection
        ConcurrentNavigableSetNullSafe<String> broken = new ConcurrentNavigableSetNullSafe<>();
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(broken, null);
        // Expect NPE when clear() is invoked
        assertThrows(NullPointerException.class, broken::clear);
    }

    @Test
    void testClearPropagatesUnsupportedOperationException() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockInternal = mock(NavigableSet.class);
        doThrow(new UnsupportedOperationException("clear not supported")).when(mockInternal).clear();
        ConcurrentNavigableSetNullSafe<String> setUnderTest = new ConcurrentNavigableSetNullSafe<>();
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(setUnderTest, mockInternal);
        // The exception should propagate unchanged
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, setUnderTest::clear);
        assertEquals("clear not supported", ex.getMessage());
    }

    @Test
    void testClearPropagatesRuntimeException() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockInternal = mock(NavigableSet.class);
        doThrow(new IllegalStateException("illegal state")).when(mockInternal).clear();
        ConcurrentNavigableSetNullSafe<String> setUnderTest = new ConcurrentNavigableSetNullSafe<>();
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(setUnderTest, mockInternal);
        IllegalStateException ex = assertThrows(IllegalStateException.class, setUnderTest::clear);
        assertEquals("illegal state", ex.getMessage());
    }

    /* -------------------------------------------------------------
     *  Post‑clear state checks (size, navigation, poll, add)
     * ------------------------------------------------------------- */
    @Test
    void testPostClearState() {
        // Populate, clear, then verify size/isEmpty
        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
        // first() and last() should throw NoSuchElementException on an empty set
        assertThrows(NoSuchElementException.class, set::first);
        assertThrows(NoSuchElementException.class, set::last);
        // pollFirst / pollLast must return null
        assertNull(set.pollFirst());
        assertNull(set.pollLast());
        // Adding after clear should increase size to 1
        assertTrue(set.add("newElement"));
        assertEquals(1, set.size());
        assertTrue(set.contains("newElement"));
    }

    /* -------------------------------------------------------------
     *  View consistency after clear
     * ------------------------------------------------------------- */
    @Test
    void testViewsAreEmptyAfterClear() {
        // Obtain various views before clearing
        NavigableSet<String> descending = set.descendingSet();
        SortedSet<String> head = set.headSet("beta");
        SortedSet<String> tail = set.tailSet("beta");
        SortedSet<String> sub = set.subSet("alpha", "gamma");
        // Ensure views contain elements initially
        assertFalse(descending.isEmpty());
        assertFalse(head.isEmpty());
        assertFalse(tail.isEmpty());
        assertFalse(sub.isEmpty());
        // Clear the original set
        set.clear();
        // All views must now report empty
        assertTrue(descending.isEmpty(), "descendingSet should be empty after clear");
        assertTrue(head.isEmpty(), "headSet should be empty after clear");
        assertTrue(tail.isEmpty(), "tailSet should be empty after clear");
        assertTrue(sub.isEmpty(), "subSet should be empty after clear");
    }

    /* -------------------------------------------------------------
     *  Concurrent clear behavior
     * ------------------------------------------------------------- */
    @Test
    void testConcurrentClearDoesNotThrowCMException() throws Exception {
        final int THREAD_COUNT = 4;
        final int ELEMENTS_PER_THREAD = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        // Workers that keep adding elements
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < ELEMENTS_PER_THREAD; j++) {
                        set.add("t" + threadId + "-v" + j);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Thread that performs clear while additions are happening
        Future<Void> clearFuture = executor.submit(() -> {
            // wait for workers to be ready
            startLatch.await();
            // Small pause to increase likelihood of interleaving
            Thread.sleep(10);
            // the operation under test
            set.clear();
            return null;
        });
        // Start everything
        startLatch.countDown();
        // Wait for workers to finish
        doneLatch.await();
        // Wait for clear to finish (any exception would be re‑thrown here)
        clearFuture.get();
        // No ConcurrentModificationException should have escaped; verify final size is sensible
        int finalSize = set.size();
        assertTrue(finalSize >= 0 && finalSize <= THREAD_COUNT * ELEMENTS_PER_THREAD, "Final size should be within expected bounds after concurrent clear");
        executor.shutdownNow();
    }

    /* -------------------------------------------------------------
     *  Additional sanity checks for iterator after clear
     * ------------------------------------------------------------- */
    @Test
    void testIteratorAfterClearDoesNotThrowCMException() {
        // Obtain iterator before clear
        Iterator<String> it = set.iterator();
        // Clear the set while iterator exists
        set.clear();
        // The iterator should behave as an empty iterator without throwing
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }
}
