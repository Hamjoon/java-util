package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

class TTLCache_clear_Test {

    /**
     * Cache instance used in each test.
     */
    private TTLCache<Integer, String> cache;

    class FaultyLock extends ReentrantLock {

        @Override
        public void lock() {
            throw new IllegalMonitorStateException("cannot lock");
        }
    }

    class FaultyLock extends ReentrantLock {

        @Override
        public void lock() {
            throw new IllegalMonitorStateException("cannot lock");
        }
    }

    /**
     * Creates a fresh cache before each test.  The TTL is set to a
     * relatively large value so that entries do not expire during the
     * test execution.  No size limit is used (maxSize = -1).
     */
    @BeforeEach
    void setupBeforeEach() {
        // 60‑second TTL, unlimited size
        cache = new TTLCache<>(60_000L, -1);
        // Populate the cache with a few entries that will be used by the tests
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
    }

    /**
     * Clears the reference to the cache after each test so that the
     * next test gets a clean instance from {@link #setupBeforeEach()}.
     */
    @AfterEach
    void teardownAfterEach() {
        cache = null;
    }

    /**
     * Shuts down the shared background purge thread once all tests have
     * finished.  This mirrors the behaviour of the existing test suite.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    /**
     * Verifies that invoking {@code clear()} removes all entries and
     * leaves the cache in an empty state.
     */
    @Test
    void clearEmptiesCache() {
        // pre‑condition: cache contains three entries
        assertEquals(3, cache.size());
        assertFalse(cache.isEmpty());
        // exercise
        cache.clear();
        // verify
        assertEquals(0, cache.size(), "Cache size should be zero after clear");
        assertTrue(cache.isEmpty(), "Cache should be empty after clear");
        assertNull(cache.get(1), "Getting a previously stored key should return null");
        assertFalse(cache.containsKey(2), "containsKey should be false after clear");
        assertFalse(cache.containsValue("three"), "containsValue should be false after clear");
    }

    /**
     * Ensures that the cache continues to operate normally after a clear:
     * new entries can be added and retrieved.
     */
    @Test
    void clearAllowsSubsequentPutAndGet() {
        // clear the pre‑populated cache
        cache.clear();
        assertTrue(cache.isEmpty());
        // add new entries
        cache.put(10, "ten");
        cache.put(20, "twenty");
        // verify new entries are present
        assertEquals(2, cache.size());
        assertEquals("ten", cache.get(10));
        assertEquals("twenty", cache.get(20));
        // ensure old keys are still absent
        assertNull(cache.get(1));
        assertFalse(cache.containsKey(2));
    }

    /**
     * Checks that calling {@code clear()} on an already empty cache does
     * not cause any side effects or exceptions.
     */
    @Test
    void clearOnEmptyCacheIsNoOp() {
        // make the cache empty first
        cache.clear();
        assertTrue(cache.isEmpty());
        // second clear – should be safe
        assertDoesNotThrow(() -> cache.clear());
        // still empty
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    /**
     * Verifies that after {@code clear()} the internal linked‑list
     * (head/tail) is reset to its initial configuration and that the
     * lock is not held.
     */
    @Test
    void clearResetsLinkedListAndReleasesLock() throws Exception {
        // pre‑condition: map not empty
        assertFalse(cache.isEmpty());
        cache.clear();
        // Access private fields via reflection
        Field headField = TTLCache.class.getDeclaredField("head");
        Field tailField = TTLCache.class.getDeclaredField("tail");
        Field lockField = TTLCache.class.getDeclaredField("lock");
        headField.setAccessible(true);
        tailField.setAccessible(true);
        lockField.setAccessible(true);
        Object head = headField.get(cache);
        Object tail = tailField.get(cache);
        ReentrantLock lock = (ReentrantLock) lockField.get(cache);
        // head.next should be tail, tail.prev should be head
        Field nextField = head.getClass().getDeclaredField("next");
        Field prevField = tail.getClass().getDeclaredField("prev");
        nextField.setAccessible(true);
        prevField.setAccessible(true);
        assertSame(tail, nextField.get(head), "head.next must point to tail after clear");
        assertSame(head, prevField.get(tail), "tail.prev must point to head after clear");
        // lock must be free
        assertFalse(lock.isLocked(), "Lock must be released after clear");
    }

    /**
     * Same verification as above but when the cache is already empty.
     */
    @Test
    void clearOnEmptyResetsLinkedListAndReleasesLock() throws Exception {
        // make it empty
        cache.clear();
        // second clear – still empty
        cache.clear();
        // reflection checks (identical to previous test)
        Field headField = TTLCache.class.getDeclaredField("head");
        Field tailField = TTLCache.class.getDeclaredField("tail");
        Field lockField = TTLCache.class.getDeclaredField("lock");
        headField.setAccessible(true);
        tailField.setAccessible(true);
        lockField.setAccessible(true);
        Object head = headField.get(cache);
        Object tail = tailField.get(cache);
        ReentrantLock lock = (ReentrantLock) lockField.get(cache);
        Field nextField = head.getClass().getDeclaredField("next");
        Field prevField = tail.getClass().getDeclaredField("prev");
        nextField.setAccessible(true);
        prevField.setAccessible(true);
        assertSame(tail, nextField.get(head));
        assertSame(head, prevField.get(tail));
        assertFalse(lock.isLocked());
    }

    @Test
    void clearIsThreadSafeWhenInvokedConcurrently() throws Exception {
        // Populate with a few entries again (already done in @BeforeEach)
        assertEquals(3, cache.size());
        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        Runnable clearTask = () -> {
            try {
                // wait for both threads to be ready
                startLatch.await();
                cache.clear();
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        };
        exec.submit(clearTask);
        exec.submit(clearTask);
        // Release both threads simultaneously
        startLatch.countDown();
        // Wait for both to finish (no deadlock)
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Both clear calls should finish quickly");
        exec.shutdownNow();
        // final state must be identical to a single clear
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
        // verify linked list reset (reuse helper method)
        Field headField = TTLCache.class.getDeclaredField("head");
        Field tailField = TTLCache.class.getDeclaredField("tail");
        headField.setAccessible(true);
        tailField.setAccessible(true);
        Object head = headField.get(cache);
        Object tail = tailField.get(cache);
        Field nextField = head.getClass().getDeclaredField("next");
        Field prevField = tail.getClass().getDeclaredField("prev");
        nextField.setAccessible(true);
        prevField.setAccessible(true);
        assertSame(tail, nextField.get(head));
        assertSame(head, prevField.get(tail));
    }

    @Test
    void clearThenPutCreatesCorrectLinkedListStructure() throws Exception {
        cache.clear();
        // Insert a single entry after clear
        cache.put(99, "ninety‑nine");
        // Verify size
        assertEquals(1, cache.size());
        assertEquals("ninety‑nine", cache.get(99));
        // Access internal nodes
        Field headField = TTLCache.class.getDeclaredField("head");
        Field tailField = TTLCache.class.getDeclaredField("tail");
        headField.setAccessible(true);
        tailField.setAccessible(true);
        Object head = headField.get(cache);
        Object tail = tailField.get(cache);
        Field nextField = head.getClass().getDeclaredField("next");
        Field prevField = tail.getClass().getDeclaredField("prev");
        nextField.setAccessible(true);
        prevField.setAccessible(true);
        Object node = nextField.get(head);
        assertSame(tail, node.getClass().getDeclaredField("next").get(node), "The new node's next must be tail");
        assertSame(head, node.getClass().getDeclaredField("prev").get(node), "The new node's prev must be head");
        assertSame(node, prevField.get(tail), "tail.prev must point to the new node");
    }

    @Test
    void clearAfterPartialRemovalsResetsState() throws Exception {
        // Remove a subset of entries
        cache.remove(1);
        cache.remove(2);
        // At this point cache contains only key 3, but internal list may be altered
        assertEquals(1, cache.size());
        cache.clear();
        // Verify map empty and linked list reset
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
        Field headField = TTLCache.class.getDeclaredField("head");
        Field tailField = TTLCache.class.getDeclaredField("tail");
        headField.setAccessible(true);
        tailField.setAccessible(true);
        Object head = headField.get(cache);
        Object tail = tailField.get(cache);
        Field nextField = head.getClass().getDeclaredField("next");
        Field prevField = tail.getClass().getDeclaredField("prev");
        nextField.setAccessible(true);
        prevField.setAccessible(true);
        assertSame(tail, nextField.get(head));
        assertSame(head, prevField.get(tail));
    }

    @Test
    void clearWithNullHeadThrowsNPE() throws Exception {
        // Use reflection to set head = null
        Field headField = TTLCache.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(cache, null);
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NPE when head is null");
    }

    @Test
    void clearWithNullTailThrowsNPE() throws Exception {
        Field tailField = TTLCache.class.getDeclaredField("tail");
        tailField.setAccessible(true);
        tailField.set(cache, null);
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NPE when tail is null");
    }

    @Test
    void clearWithNullCacheMapThrowsNPE() throws Exception {
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(cache, null);
        assertThrows(NullPointerException.class, cache::clear, "clear() should throw NPE when cacheMap is null");
    }

    @Test
    void clearPropagatesExceptionFromCacheMapAndReleasesLock() throws Exception {
        // Mock a ConcurrentMap that throws on clear()
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> mockMap = (ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>>) mock(ConcurrentMap.class);
        doThrow(new IllegalStateException("boom")).when(mockMap).clear();
        // Inject the mock map
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(cache, mockMap);
        // Capture the lock to verify its state after exception
        Field lockField = TTLCache.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        ReentrantLock lock = (ReentrantLock) lockField.get(cache);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, cache::clear);
        assertEquals("boom", thrown.getMessage());
        // The lock must not be held by the current thread after the exception
        assertFalse(lock.isHeldByCurrentThread(), "Lock must be released even when cacheMap.clear() throws");
    }

    @Test
    void clearPropagatesExceptionFromLockAcquisition() throws Exception {
        // Create a ReentrantLock subclass that throws when lock() is called
        class FaultyLock extends ReentrantLock {

            @Override
            public void lock() {
                throw new IllegalMonitorStateException("cannot lock");
            }
        }
        // Replace the internal lock with the faulty one
        Field lockField = TTLCache.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        lockField.set(cache, new FaultyLock());
        // Even though the lock fails, cacheMap.clear() should have been called.
        // We'll spy on the map to verify that.
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> spyMap = (ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>>) spy(cache.cacheMap);
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(cache, spyMap);
        IllegalMonitorStateException thrown = assertThrows(IllegalMonitorStateException.class, cache::clear);
        assertEquals("cannot lock", thrown.getMessage());
        // verify that cacheMap.clear() was invoked before the lock exception propagated
        verify(spyMap, times(1)).clear();
    }
}
