package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_clear_Test {

    /**
     * Instance under test – created fresh before each test method.
     */
    private ConcurrentSet<Integer> set;

    private static class FaultySet<E> extends AbstractSet<E> {

        private final Set<E> backing = new HashSet<>();

        @Override
        public Iterator<E> iterator() {
            return backing.iterator();
        }

        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public boolean add(E e) {
            return backing.add(e);
        }

        @Override
        public void clear() {
            throw new IllegalStateException("simulated failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a brand‑new, empty ConcurrentSet.
        set = new ConcurrentSet<>();
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set to aid garbage collection; not strictly needed.
        if (set != null) {
            set.clear();
        }
        if (set != null) {
            set.clear();
        }
    }

    @Test
    void clearRemovesAllElements() {
        // given: a set populated with several distinct values
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // when: clear is invoked
        set.clear();
        // then: the set reports being empty and its size is zero
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertEquals(0, set.size(), "Size should be 0 after clear()");
    }

    @Test
    void clearOnAlreadyEmptySetIsIdempotent() {
        // given: an empty set (no explicit population required)
        // when: clear is called on the empty set
        set.clear();
        // then: the set remains empty and size stays zero
        assertTrue(set.isEmpty(), "Empty set should stay empty after clear()");
        assertEquals(0, set.size(), "Size should remain 0 after clear() on empty set");
    }

    @Test
    void clearAfterConcurrentOperationsLeavesSetEmpty() throws InterruptedException {
        // given: a set that has been concurrently populated and then emptied
        int threadCount = 3;
        int operationsPerThread = 1000;
        var latch = new java.util.concurrent.CountDownLatch(threadCount * 2);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    set.add(j);
                }
                latch.countDown();
            }).start();
            new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    set.remove(j);
                }
                latch.countDown();
            }).start();
        }
        // wait for all threads to finish
        latch.await();
        // when: clear is invoked after the concurrent activity
        set.clear();
        // then: the set must be empty
        assertTrue(set.isEmpty(), "Set should be empty after clear() following concurrent ops");
        assertEquals(0, set.size(), "Size should be 0 after clear() following concurrent ops");
    }

    @Test
    void clearThenAddAndContainsWorks() {
        // Populate, clear, then reuse the set.
        set.addAll(Arrays.asList(10, 20, 30));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
        assertEquals(0, set.size());
        // Re‑add an element and verify normal behaviour.
        assertTrue(set.add(99), "Adding after clear should succeed");
        assertTrue(set.contains(99), "Newly added element must be present");
        assertEquals(1, set.size());
    }

    @Test
    void clearWithNullInternalSetThrowsNPE() throws Exception {
        // Use reflection to corrupt the internal delegate.
        Field f = ConcurrentSet.class.getDeclaredField("set");
        f.setAccessible(true);
        f.set(set, null);
        // The clear call should propagate the NPE.
        assertThrows(NullPointerException.class, set::clear, "clear() should throw NPE when internal set is null");
    }

    @Test
    void clearWhenUnderlyingSetIsUnmodifiableThrowsUOE() throws Exception {
        Set<Object> unmodifiable = Collections.unmodifiableSet(new HashSet<>());
        Field f = ConcurrentSet.class.getDeclaredField("set");
        f.setAccessible(true);
        f.set(set, unmodifiable);
        assertThrows(UnsupportedOperationException.class, set::clear, "clear() should propagate UnsupportedOperationException from an unmodifiable delegate");
    }

    @Test
    void clearPropagatesRuntimeExceptionFromDelegate() throws Exception {
        FaultySet<Integer> faulty = new FaultySet<>();
        faulty.add(1);
        Field f = ConcurrentSet.class.getDeclaredField("set");
        f.setAccessible(true);
        f.set(set, faulty);
        IllegalStateException ex = assertThrows(IllegalStateException.class, set::clear);
        assertEquals("simulated failure", ex.getMessage());
    }

    @Test
    void clearRemovesNullElements() {
        assertTrue(set.add(null), "ConcurrentSet should accept null");
        assertTrue(set.contains(null));
        assertEquals(1, set.size());
        set.clear();
        assertFalse(set.contains(null), "Null element must be removed after clear()");
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
        // iterator should not yield any elements
        Iterator<Integer> it = set.iterator();
        assertFalse(it.hasNext(), "Iterator should be empty after clear()");
    }

    @Test
    void concurrentClearCallsLeaveSetEmpty() throws InterruptedException {
        // Populate the set first.
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        int threadCount = 8;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            exec.submit(() -> {
                try {
                    // synchronize start
                    startGate.await();
                    set.clear();
                } catch (InterruptedException ignored) {
                } finally {
                    doneGate.countDown();
                }
            });
        }
        // let all threads run
        startGate.countDown();
        // wait for completion
        doneGate.await();
        exec.shutdownNow();
        assertTrue(set.isEmpty(), "Set must be empty after concurrent clears");
        assertEquals(0, set.size());
    }

    @Test
    void iteratorSnapshotIsUnaffectedByClear() {
        // Populate set with known elements.
        set.addAll(Arrays.asList(100, 200, 300));
        // Obtain iterator BEFORE clearing.
        Iterator<Integer> iterator = set.iterator();
        // Clear the set while iterator is still alive.
        set.clear();
        // The iterator should still iterate over the snapshot (3 elements).
        List<Integer> iterated = new ArrayList<>();
        while (iterator.hasNext()) {
            iterated.add(iterator.next());
        }
        Collections.sort(iterated);
        assertEquals(Arrays.asList(100, 200, 300), iterated, "Iterator must reflect state at creation, not be affected by later clear()");
        // After iteration, the set must remain empty.
        assertTrue(set.isEmpty(), "Set should stay empty after clear() and iterator consumption");
    }
}
