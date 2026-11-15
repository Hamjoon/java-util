package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import java.util.AbstractMap;

@ExtendWith(MockitoExtension.class)
class TTLCache_entrySet_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Create a fresh cache before each test.
     * A long TTL is used so that entries do not expire unintentionally during the test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // 60‑second TTL, no size limit
        ttlCache = new TTLCache<>(60_000, -1);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    /**
     * Shut down the shared purge‑task thread after all tests have run.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    @Test
    void entrySetReflectsPutOperations() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        assertEquals(2, entrySet.size(), "entrySet size should match number of put entries");
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals(1) && e.getValue().equals("A")), "entrySet should contain entry (1, \"A\")");
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey().equals(2) && e.getValue().equals("B")), "entrySet should contain entry (2, \"B\")");
    }

    @Test
    void entrySetIteratorIteratesAllEntries() {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        ttlCache.put(30, "Z");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        int count = 0;
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            assertNotNull(e.getKey(), "Key must not be null");
            assertNotNull(e.getValue(), "Value must not be null");
            count++;
        }
        assertEquals(3, count, "Iterator should have visited exactly three entries");
    }

    @Test
    void entrySetRemoveUpdatesCache() {
        ttlCache.put(5, "Five");
        ttlCache.put(6, "Six");
        ttlCache.put(7, "Seven");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        // Remove the entry with key 6 via the Set view
        entrySet.removeIf(entry -> entry.getKey().equals(6));
        assertEquals(2, ttlCache.size(), "Cache size should decrease after removal via entrySet");
        assertNull(ttlCache.get(6), "Key 6 should have been removed from the cache");
        assertEquals("Five", ttlCache.get(5));
        assertEquals("Seven", ttlCache.get(7));
    }

    @Test
    void entrySetIsLiveAfterExpiration() throws InterruptedException {
        // Use a short TTL and a short cleanup interval so that expiration happens quickly.
        TTLCache<Integer, String> shortTtlCache = new TTLCache<>(200, -1, 50);
        shortTtlCache.put(1, "A");
        shortTtlCache.put(2, "B");
        // Verify entries are present initially.
        assertEquals(2, shortTtlCache.size());
        assertTrue(shortTtlCache.containsKey(1));
        assertTrue(shortTtlCache.containsKey(2));
        // Wait for the TTL to expire and the background purge task to run.
        Thread.sleep(300);
        // entrySet should now be empty.
        Set<Map.Entry<Integer, String>> entrySet = shortTtlCache.entrySet();
        assertTrue(entrySet.isEmpty(), "entrySet should be empty after entries have expired");
        // Clean up the short‑TTL cache explicitly (its purge thread will be stopped by the global shutdown).
        shortTtlCache.clear();
    }

    @Test
    @DisplayName("Empty cache returns a non‑null Set with size 0")
    void entrySetEmptyCache() {
        // No puts – cache is freshly created.
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        assertNotNull(entrySet, "entrySet must never be null");
        assertEquals(0, entrySet.size(), "A newly created cache should expose an empty entry set");
    }

    @Test
    @DisplayName("Live view: Set size reflects a later put operation")
    void entrySetLiveAfterPut() {
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        assertEquals(0, entrySet.size(), "Initially the set should be empty");
        ttlCache.put(42, "Answer");
        // The previously obtained Set must now see the new entry.
        assertEquals(1, entrySet.size(), "Set size should increase after a put on the underlying cache");
        assertTrue(entrySet.iterator().next().equals(new AbstractMap.SimpleEntry<>(42, "Answer")), "Set must contain the newly added entry");
    }

    @Test
    @DisplayName("Removing an entry via Set.remove(Object) updates the cache")
    void entrySetRemoveObjectReflectsCache() {
        ttlCache.put(1, "One");
        ttlCache.put(2, "Two");
        ttlCache.put(3, "Three");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        // Build an entry equal to the one we want to remove.
        Map.Entry<Integer, String> toRemove = new AbstractMap.SimpleEntry<>(2, "Two");
        boolean removed = entrySet.remove(toRemove);
        assertTrue(removed, "Set.remove should return true when the element existed");
        assertEquals(2, entrySet.size(), "Set size must shrink after removal");
        assertFalse(ttlCache.containsKey(2), "Underlying cache must no longer contain the removed key");
        assertNull(ttlCache.get(2), "Cache get should return null for the removed key");
    }

    @Test
    @DisplayName("Iterator.remove() deletes the underlying entry")
    void entrySetIteratorRemoveUpdatesCache() {
        ttlCache.put(10, "Ten");
        ttlCache.put(20, "Twenty");
        ttlCache.put(30, "Thirty");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        // Remove the entry with key 20 using iterator.remove()
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey().equals(20)) {
                it.remove();
                break;
            }
        }
        assertEquals(2, ttlCache.size(), "Cache size should be reduced after iterator.remove()");
        assertFalse(ttlCache.containsKey(20), "Key 20 must be absent from the cache after removal");
        assertTrue(entrySet.stream().noneMatch(e -> e.getKey().equals(20)), "entrySet must no longer contain the removed entry");
    }

    @Test
    @DisplayName("Two successive entrySet() calls return distinct Set objects but share data")
    void entrySetDistinctObjectsSameUnderlyingData() {
        ttlCache.put(5, "Five");
        ttlCache.put(6, "Six");
        Set<Map.Entry<Integer, String>> set1 = ttlCache.entrySet();
        Set<Map.Entry<Integer, String>> set2 = ttlCache.entrySet();
        assertNotSame(set1, set2, "Two calls to entrySet() must return different Set instances");
        assertEquals(set1.size(), set2.size(), "Both sets should initially report the same size");
        // Remove an element via set1 and verify set2 sees the change.
        set1.removeIf(e -> e.getKey().equals(5));
        assertEquals(1, set1.size(), "set1 size after removal");
        assertEquals(1, set2.size(), "set2 must reflect the removal performed through set1");
        assertFalse(set2.stream().anyMatch(e -> e.getKey().equals(5)), "set2 must not contain the entry that was removed via set1");
    }

    @Test
    @DisplayName("Adding directly to the entrySet throws UnsupportedOperationException")
    void entrySetAddUnsupported() {
        ttlCache.put(1, "One");
        Set<Map.Entry<Integer, String>> entrySet = ttlCache.entrySet();
        Map.Entry<Integer, String> newEntry = new AbstractMap.SimpleEntry<>(99, "Ninety‑Nine");
        assertThrows(UnsupportedOperationException.class, () -> entrySet.add(newEntry), "entrySet.add should throw UnsupportedOperationException because the view is read‑only for additions");
    }
}
