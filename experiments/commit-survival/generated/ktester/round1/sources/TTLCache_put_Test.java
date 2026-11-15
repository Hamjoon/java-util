package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.locks.ReentrantLock;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_put_Test {

    /**
     * Cache instance used by the tests.
     */
    private TTLCache<Integer, String> ttlCache;

    /**
     * Runs before each test – creates a fresh cache instance.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Use a TTL that is long enough not to expire during the test.
        // No size limit for the basic put‑return‑value tests.
        ttlCache = new TTLCache<>(10_000, -1);
    }

    /**
     * Runs after each test – clears the cache to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    /**
     * Runs once after all tests – shuts down the static purge thread used by TTLCache.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    /**
     * Replace the internal {@code lock} with a mock (or any supplied instance).
     */
    private void injectLock(TTLCache<?, ?> cache, ReentrantLock newLock) throws Exception {
        Field lockField = TTLCache.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        lockField.set(cache, newLock);
    }

    /**
     * Directly insert an entry into the internal {@code cacheMap} without invoking {@code put}.
     */
    private void injectEntry(TTLCache<Integer, String> cache, Integer key, String value, long ttlMillis) throws Exception {
        // Obtain the CacheEntry and Node classes via reflection.
        Class<?> nodeClass = Class.forName("com.cedarsoftware.util.TTLCache$Node");
        Class<?> entryClass = Class.forName("com.cedarsoftware.util.TTLCache$CacheEntry");
        // Create a Node instance: new Node<>(key, value)
        Constructor<?> nodeCtor = nodeClass.getDeclaredConstructor(Object.class, Object.class);
        nodeCtor.setAccessible(true);
        Object node = nodeCtor.newInstance(key, value);
        // Compute expiry time.
        long expiry = System.currentTimeMillis() + ttlMillis;
        // Create a CacheEntry instance: new CacheEntry<>(node, expiry)
        Constructor<?> entryCtor = entryClass.getDeclaredConstructor(nodeClass, long.class);
        entryCtor.setAccessible(true);
        Object entry = entryCtor.newInstance(node, expiry);
        // Get the private cacheMap field and put the entry.
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentMap<Integer, Object> map = (java.util.concurrent.ConcurrentMap<Integer, Object>) mapField.get(cache);
        map.put(key, entry);
    }

    /**
     * Verify that putting a new key returns {@code null} and the entry can be retrieved.
     */
    @Test
    void putNewKeyReturnsNullAndStoresValue() {
        String previous = ttlCache.put(1, "one");
        assertNull(previous, "First insertion should return null");
        assertEquals("one", ttlCache.get(1), "Value should be stored and retrievable");
    }

    /**
     * Verify that putting an existing key returns the previous value and updates the mapping.
     */
    @Test
    void putExistingKeyReturnsPreviousValueAndUpdates() {
        ttlCache.put(1, "first");
        String returned = ttlCache.put(1, "second");
        assertEquals("first", returned, "Should return the previous value for the key");
        assertEquals("second", ttlCache.get(1), "Value should be updated to the new one");
    }

    /**
     * Verify that the cache respects the LRU policy when a max size is defined.
     */
    @Test
    void putTriggersLRUEvictionWhenMaxSizeExceeded() {
        // Create a cache with a max size of 3 entries.
        ttlCache = new TTLCache<>(10_000, 3);
        // LRU order: 1
        ttlCache.put(1, "A");
        // LRU order: 1,2
        ttlCache.put(2, "B");
        // LRU order: 1,2,3
        ttlCache.put(3, "C");
        // Access key 1 to make it most‑recently used.
        assertEquals("A", ttlCache.get(1));
        // LRU order now: 2,3,1
        // Adding a fourth entry should evict key 2 (the least recently used).
        ttlCache.put(4, "D");
        // Verify eviction.
        assertNull(ttlCache.get(2), "Key 2 should have been evicted as LRU");
        assertEquals("A", ttlCache.get(1), "Key 1 should still be present");
        assertEquals("C", ttlCache.get(3), "Key 3 should still be present");
        assertEquals("D", ttlCache.get(4), "Key 4 should be present");
        assertEquals(3, ttlCache.size(), "Cache size must stay at the configured max size");
    }

    /**
     * Verify that {@code putAll} delegates to {@code put} and therefore respects the same semantics.
     */
    @Test
    void putAllUsesPutSemantics() {
        Map<Integer, String> source = Map.of(10, "X", 20, "Y", 30, "Z");
        ttlCache.putAll(source);
        // All entries must be present.
        source.forEach((k, v) -> assertEquals(v, ttlCache.get(k)));
        // Adding a duplicate key via putAll should replace the old value and return the previous one.
        Map<Integer, String> replacement = Map.of(20, "Y2");
        ttlCache.putAll(replacement);
        assertEquals("Y2", ttlCache.get(20), "Value for key 20 should be replaced");
    }

    @Test
    @DisplayName("Lock acquired – unlimited size, no eviction")
    void lockAcquiredUnlimitedSizeNoEviction() {
        // ttlMillis = 5000, maxSize = -1 (unlimited)
        ttlCache = new TTLCache<>(5_000L, -1);
        String previous = ttlCache.put(1, "alpha");
        assertNull(previous, "First insertion should return null");
        assertEquals("alpha", ttlCache.get(1), "Entry must be present");
        assertEquals(1, ttlCache.size(), "Cache size should be 1");
    }

    @Test
    @DisplayName("Lock acquired – size equals maxSize, no eviction")
    void lockAcquiredSizeEqualsMaxNoEviction() {
        ttlCache = new TTLCache<>(5_000L, 3);
        ttlCache.put(1, "x");
        ttlCache.put(2, "y");
        // third insertion reaches maxSize but must not evict
        String ret = ttlCache.put(3, "z");
        assertNull(ret, "No previous mapping for key 3");
        assertEquals(3, ttlCache.size(), "Cache size should equal maxSize (3)");
        assertEquals("x", ttlCache.get(1));
        assertEquals("y", ttlCache.get(2));
        assertEquals("z", ttlCache.get(3));
    }

    @Test
    @DisplayName("Lock not acquired – LRU update skipped, entry still stored")
    void lockNotAcquiredSkipsLruUpdate() throws Exception {
        ttlCache = new TTLCache<>(1_000L, 2);
        // Populate one initial entry using normal put (lock will be acquired here).
        ttlCache.put(1, "v1");
        // Replace the internal lock with a mock that fails to acquire.
        ReentrantLock mockLock = mock(ReentrantLock.class);
        when(mockLock.tryLock()).thenReturn(false);
        // unlock should never be called because tryLock() returned false.
        injectLock(ttlCache, mockLock);
        // Perform the put that should *not* acquire the lock.
        String previous = ttlCache.put(2, "v2");
        assertNull(previous, "No previous mapping for key 2");
        assertEquals("v2", ttlCache.get(2), "Entry must be stored even without lock");
        verify(mockLock, times(1)).tryLock();
        verify(mockLock, never()).unlock();
        // Since eviction logic is skipped, both entries should remain.
        assertEquals(2, ttlCache.size(), "Both entries must remain (no eviction)");
    }

    @Test
    @DisplayName("Lock not acquired – size exceeds maxSize but eviction is bypassed")
    void lockNotAcquiredSizeExceedsMaxNoEviction() throws Exception {
        // maxSize = 1
        ttlCache = new TTLCache<>(2_000L, 1);
        // Insert first entry (lock acquired).
        ttlCache.put(1, "a");
        // Mock lock to fail for the next insertion.
        ReentrantLock mockLock = mock(ReentrantLock.class);
        when(mockLock.tryLock()).thenReturn(false);
        injectLock(ttlCache, mockLock);
        // Insert second entry – eviction should be skipped.
        ttlCache.put(2, "b");
        // Both entries should be present because eviction was not performed.
        assertEquals("a", ttlCache.get(1));
        assertEquals("b", ttlCache.get(2));
        assertEquals(2, ttlCache.size(), "Cache size should be 2 (no eviction)");
    }

    @Test
    @DisplayName("Null key – ConcurrentMap throws NullPointerException")
    void nullKeyThrowsNPE() {
        ttlCache = new TTLCache<>(5_000L, -1);
        assertThrows(NullPointerException.class, () -> ttlCache.put(null, "someValue"), "Putting a null key must throw NullPointerException");
    }

    @Test
    @DisplayName("Null value – ConcurrentMap throws NullPointerException")
    void nullValueThrowsNPE() {
        ttlCache = new TTLCache<>(5_000L, -1);
        assertThrows(NullPointerException.class, () -> ttlCache.put(42, null), "Putting a null value must throw NullPointerException");
    }

    @Test
    @DisplayName("Negative ttlMillis – entry stored with past expiry")
    void negativeTtlMillisStoresEntry() throws Exception {
        // negative TTL
        ttlCache = new TTLCache<>(-1_000L, -1);
        ttlCache.put(99, "value");
        // The entry should be present immediately after insertion.
        assertEquals("value", ttlCache.get(99), "Entry must be stored even with negative TTL");
        // Size should be 1.
        assertEquals(1, ttlCache.size());
        // No explicit way to check expiry time; we rely on the fact that no exception occurs.
    }

    @Test
    @DisplayName("maxSize zero – insertion triggers immediate eviction when lock acquired")
    void maxSizeZeroImmediateEviction() throws Exception {
        // maxSize = 0
        ttlCache = new TTLCache<>(5_000L, 0);
        // Ensure the real lock is used (it will be acquired).
        String previous = ttlCache.put(1, "V0");
        assertNull(previous, "First insertion should return null");
        // Because maxSize == 0, the newly inserted node becomes LRU and must be evicted.
        assertNull(ttlCache.get(1), "Entry should have been evicted immediately");
        assertEquals(0, ttlCache.size(), "Cache size must stay at 0 after eviction");
    }
}
