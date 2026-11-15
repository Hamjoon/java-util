package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_keySet_Test {

    private LockingLRUCacheStrategy<String, Integer> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Create a cache with a modest capacity and populate it with a few entries
        cache = new LockingLRUCacheStrategy<>(5);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure each test gets a fresh instance
        cache = null;
    }

    /**
     * Retrieves the private {@code lock} field via reflection.
     */
    private Lock getInternalLock() throws Exception {
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        return (Lock) lockField.get(cache);
    }

    /**
     * Retrieves the private {@code head} field via reflection.
     */
    private Object getHeadNode() throws Exception {
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        return headField.get(cache);
    }

    /**
     * Retrieves the private {@code tail} field via reflection.
     */
    private Object getTailNode() throws Exception {
        Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
        tailField.setAccessible(true);
        return tailField.get(cache);
    }

    /**
     * Sets a private field (head or tail) to the supplied value via reflection.
     */
    private void setPrivateField(String fieldName, Object value) throws Exception {
        Field f = LockingLRUCacheStrategy.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(cache, value);
    }

    @Test
    void keySetReturnsAllKeysInCache() {
        // GIVEN the cache is populated in setupBeforeEach()
        Set<String> expected = new HashSet<>();
        expected.add("one");
        expected.add("two");
        expected.add("three");
        // WHEN we retrieve the key set
        Set<String> actual = cache.keySet();
        // THEN the returned set contains exactly the keys we put in
        assertEquals(expected.size(), actual.size(), "Key set size should match expected");
        assertTrue(actual.containsAll(expected), "Key set should contain all expected keys");
    }

    @Test
    void keySetOnEmptyCacheReturnsEmptySet() {
        // GIVEN an empty cache
        cache.clear();
        // WHEN we retrieve the key set
        Set<String> actual = cache.keySet();
        // THEN the set is empty
        assertNotNull(actual, "Key set should never be null");
        assertTrue(actual.isEmpty(), "Key set of an empty cache should be empty");
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetOnNeverPopulatedCacheReturnsEmptySet() {
        // GIVEN a freshly created cache (no puts)
        // WHEN we call keySet()
        Set<String> keys = cache.keySet();
        // THEN the set is empty
        assertNotNull(keys);
        assertTrue(keys.isEmpty(), "Expected empty set for never‑populated cache");
    }

    @Test
    void keySetAfterClearReturnsEmptySet() {
        // GIVEN a populated cache
        cache.put("a", 1);
        cache.put("b", 2);
        // AND it is cleared
        cache.clear();
        // WHEN we call keySet()
        Set<String> keys = cache.keySet();
        // THEN the set is empty
        assertNotNull(keys);
        assertTrue(keys.isEmpty(), "Expected empty set after clear()");
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetWithSingleEntry() {
        cache.put("alpha", 1);
        Set<String> keys = cache.keySet();
        assertEquals(Set.of("alpha"), keys, "Key set should contain exactly the single inserted key");
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetPreservesInsertionOrder() {
        cache.put("first", 1);
        cache.put("second", 2);
        cache.put("third", 3);
        Set<String> keys = cache.keySet();
        assertEquals(List.of("first", "second", "third"), new ArrayList<>(keys), "Insertion order must be preserved");
    }

    @Test
    void keySetAfterUpdatingExistingKeyKeepsOriginalOrder() {
        cache.put("first", 1);
        cache.put("second", 2);
        cache.put("third", 3);
        // Update "second"
        cache.put("second", 22);
        Set<String> keys = cache.keySet();
        assertEquals(List.of("first", "second", "third"), new ArrayList<>(keys), "Updating a value must not change the key order");
    }

    // --------------------------------------------------------------------- //
    @Test
    void removalFromReturnedSetDoesNotAffectCache() {
        cache.put("x", 10);
        cache.put("y", 20);
        Set<String> returned = cache.keySet();
        // The set is backed by a *copy* (LinkedHashMap) – removal should not affect the cache.
        assertTrue(returned.remove("x"), "Removal from the returned set should succeed");
        assertFalse(returned.contains("x"), "Returned set no longer contains 'x'");
        // Cache must still contain both keys
        Set<String> after = cache.keySet();
        assertTrue(after.containsAll(List.of("x", "y")), "Cache must still contain both keys after set removal");
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetAcquiresAndReleasesLock() throws Exception {
        Lock lock = getInternalLock();
        // Ensure lock is free before call
        assertTrue(lock.tryLock(), "Lock should be free before test");
        lock.unlock();
        // Call keySet()
        cache.keySet();
        // After call the lock must be free again
        assertTrue(lock.tryLock(), "Lock must be released after keySet()");
        lock.unlock();
    }

    @Test
    void keySetConcurrentCallsDoNotDeadlock() throws Exception {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        Callable<Set<String>> task = () -> {
            // wait for both threads to be ready
            startLatch.await();
            return cache.keySet();
        };
        Future<Set<String>> f1 = executor.submit(task);
        Future<Set<String>> f2 = executor.submit(task);
        // let both threads run
        startLatch.countDown();
        Set<String> set1 = f1.get(5, TimeUnit.SECONDS);
        Set<String> set2 = f2.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertEquals(Set.of("a", "b", "c"), set1);
        assertEquals(Set.of("a", "b", "c"), set2);
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetIncludesNullKey() {
        cache.put(null, 0);
        cache.put("notNull", 1);
        Set<String> keys = cache.keySet();
        assertTrue(keys.contains(null), "Returned set must contain the null key");
        assertEquals(2, keys.size());
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetReflectsEvictionWhenCapacityExceeded() {
        // capacity 3
        cache = new LockingLRUCacheStrategy<>(3);
        cache.put("first", 1);
        cache.put("second", 2);
        cache.put("third", 3);
        // should evict "first"
        cache.put("fourth", 4);
        Set<String> keys = cache.keySet();
        assertEquals(Set.of("second", "third", "fourth"), keys, "After exceeding capacity, the eldest entry must be evicted");
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetReflectsAccessOrderAfterGet() {
        cache.put("alpha", 1);
        cache.put("beta", 2);
        cache.put("gamma", 3);
        // Access "beta" – it becomes most‑recently used
        cache.get("beta");
        Set<String> keys = cache.keySet();
        // According to the Javadoc, keySet returns LRU order (most recent first)
        assertEquals(List.of("beta", "alpha", "gamma"), new ArrayList<>(keys), "After get(\"beta\"), order must be beta -> alpha -> gamma");
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetAddThrowsUnsupportedOperationException() {
        cache.put("one", 1);
        Set<String> keys = cache.keySet();
        assertThrows(UnsupportedOperationException.class, () -> keys.add("newKey"), "Adding to the returned key set must throw UnsupportedOperationException");
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetMaintainsOrderWithManyEntries() {
        int count = 10;
        for (int i = 0; i < count; i++) {
            cache.put("k" + i, i);
        }
        Set<String> keys = cache.keySet();
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            expected.add("k" + i);
        }
        assertEquals(expected, new ArrayList<>(keys), "Key order must match insertion order for many entries");
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetThrowsWhenHeadIsNull() throws Exception {
        // Corrupt the internal head reference
        setPrivateField("head", null);
        assertThrows(NullPointerException.class, () -> cache.keySet(), "Null head should cause NullPointerException");
    }

    @Test
    void keySetThrowsWhenTailIsNull() throws Exception {
        // Corrupt the internal tail reference
        setPrivateField("tail", null);
        assertThrows(NullPointerException.class, () -> cache.keySet(), "Null tail should cause NullPointerException");
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetDetectsConcurrentModification() throws Exception {
        // Populate cache
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        // Obtain the lock and start a background thread that corrupts the list while keySet is iterating
        CountDownLatch iterationStarted = new CountDownLatch(1);
        CountDownLatch corruptionDone = new CountDownLatch(1);
        Thread background = new Thread(() -> {
            try {
                // Acquire the same lock that keySet will use
                Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                Lock lock = (Lock) lockField.get(cache);
                // hold the lock to manipulate internal structure safely
                lock.lock();
                try {
                    // Signal that we have the lock and are about to corrupt
                    iterationStarted.countDown();
                    // Corrupt the linked list: set head.next = null (break the chain)
                    Object head = getHeadNode();
                    Field nextField = head.getClass().getDeclaredField("next");
                    nextField.setAccessible(true);
                    nextField.set(head, null);
                } finally {
                    lock.unlock();
                    corruptionDone.countDown();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        background.start();
        // Wait until background thread has corrupted the list
        iterationStarted.await();
        // Now call keySet – it should detect the broken structure
        assertThrows(IllegalStateException.class, () -> cache.keySet(), "Corrupted internal structure should result in IllegalStateException");
        // Ensure background thread finishes
        corruptionDone.await();
        background.join();
    }

    // --------------------------------------------------------------------- //
    @Test
    void keySetSnapshotIsImmutableAfterCacheChanges() {
        cache.put("initial", 1);
        Set<String> snapshot = cache.keySet();
        // Change cache after snapshot
        cache.put("newKey", 2);
        cache.remove("initial");
        // Snapshot must remain unchanged
        assertEquals(Set.of("initial"), snapshot, "Snapshot must not reflect subsequent cache modifications");
    }
}
