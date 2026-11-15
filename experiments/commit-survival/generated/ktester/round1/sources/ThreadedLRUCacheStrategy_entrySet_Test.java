package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.*;

class ThreadedLRUCacheStrategy_entrySet_Test {

    private ThreadedLRUCacheStrategy<String, Integer> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Small capacity and short cleanup delay – the cleanup task is not relevant for entrySet tests
        cache = new ThreadedLRUCacheStrategy<>(10, 50);
        cache.put("one", 1);
        cache.put("two", 2);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid side‑effects between tests
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void entrySetContainsAllInsertedEntries() {
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertEquals(2, entrySet.size(), "entrySet should contain exactly the two inserted entries");
        // Verify that the expected entries are present
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("one", 1)), "entrySet should contain entry (one,1)");
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("two", 2)), "entrySet should contain entry (two,2)");
    }

    @Test
    void entrySetIsUnmodifiable() {
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> entrySet.add(new AbstractMap.SimpleEntry<>("three", 3)), "entrySet must be unmodifiable for add()"), () -> assertThrows(UnsupportedOperationException.class, () -> entrySet.remove(new AbstractMap.SimpleEntry<>("one", 1)), "entrySet must be unmodifiable for remove()"), () -> assertThrows(UnsupportedOperationException.class, entrySet::clear, "entrySet must be unmodifiable for clear()"));
    }

    @Test
    void entrySetReflectsRemovalWhenRequeried() {
        // Remove one entry and obtain a fresh entrySet
        cache.remove("one");
        Set<Map.Entry<String, Integer>> entrySetAfterRemoval = cache.entrySet();
        assertEquals(1, entrySetAfterRemoval.size(), "After removal, entrySet should contain only the remaining entry");
        assertTrue(entrySetAfterRemoval.contains(new AbstractMap.SimpleEntry<>("two", 2)), "Remaining entry should be (two,2)");
        assertFalse(entrySetAfterRemoval.contains(new AbstractMap.SimpleEntry<>("one", 1)), "Removed entry (one,1) should no longer be present");
    }

    @Test
    void entrySetOnEmptyCacheReturnsEmptySet() {
        cache.clear();
        Set<Map.Entry<String, Integer>> emptySet = cache.entrySet();
        assertTrue(emptySet.isEmpty(), "entrySet on an empty cache must be empty");
        assertThrows(UnsupportedOperationException.class, () -> emptySet.add(new AbstractMap.SimpleEntry<>("x", 99)), "Even an empty entrySet must be unmodifiable");
    }

    @Test
    void singleEntryCacheSnapshot() {
        cache.clear();
        cache.put("A", 1);
        Set<Map.Entry<String, Integer>> set = cache.entrySet();
        assertEquals(1, set.size(), "Set should contain exactly one entry");
        assertTrue(set.contains(new AbstractMap.SimpleEntry<>("A", 1)), "Set must contain the inserted entry");
        assertThrows(UnsupportedOperationException.class, () -> set.add(new AbstractMap.SimpleEntry<>("B", 2)));
    }

    @Test
    void multipleEntriesCacheSnapshot() {
        cache.clear();
        cache.put("X", 10);
        cache.put("Y", 20);
        cache.put("Z", 30);
        Set<Map.Entry<String, Integer>> set = cache.entrySet();
        assertEquals(3, set.size(), "Set should contain three entries");
        assertAll(() -> assertTrue(set.contains(new AbstractMap.SimpleEntry<>("X", 10))), () -> assertTrue(set.contains(new AbstractMap.SimpleEntry<>("Y", 20))), () -> assertTrue(set.contains(new AbstractMap.SimpleEntry<>("Z", 30))));
    }

    @Test
    void entrySetHandlesNullKey() {
        cache.clear();
        cache.put(null, 42);
        Set<Map.Entry<String, Integer>> set = cache.entrySet();
        assertEquals(1, set.size());
        // AbstractMap.SimpleEntry permits null keys, so we compare manually
        Map.Entry<String, Integer> entry = set.iterator().next();
        assertNull(entry.getKey(), "Key should be null");
        assertEquals(42, entry.getValue());
    }

    @Test
    void entrySetHandlesNullValue() {
        cache.clear();
        cache.put("NIL", null);
        Set<Map.Entry<String, Integer>> set = cache.entrySet();
        assertEquals(1, set.size());
        Map.Entry<String, Integer> entry = set.iterator().next();
        assertEquals("NIL", entry.getKey());
        assertNull(entry.getValue(), "Value should be null");
    }

    @Test
    void snapshotIsImmutableAfterUnderlyingCacheChanges() {
        cache.clear();
        cache.put("K1", 100);
        cache.put("K2", 200);
        Set<Map.Entry<String, Integer>> snapshot = cache.entrySet();
        // mutate the cache after snapshot creation
        cache.put("K3", 300);
        cache.remove("K1");
        // snapshot must still contain only the original two entries
        assertEquals(2, snapshot.size());
        assertTrue(snapshot.contains(new AbstractMap.SimpleEntry<>("K1", 100)));
        assertTrue(snapshot.contains(new AbstractMap.SimpleEntry<>("K2", 200)));
        assertFalse(snapshot.contains(new AbstractMap.SimpleEntry<>("K3", 300)));
    }

    @Test
    void entrySetIsSafeAgainstConcurrentModifications() throws Exception {
        cache.clear();
        for (int i = 0; i < 100; i++) {
            cache.put("k" + i, i);
        }
        Set<Map.Entry<String, Integer>> snapshot = cache.entrySet();
        // Concurrently modify the cache
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            for (int i = 100; i < 200; i++) {
                cache.put("k" + i, i);
                cache.remove("k" + (i - 100));
            }
        });
        // wait for modifications to finish
        future.get();
        executor.shutdown();
        // Iterating over the previously obtained snapshot must not throw CME
        assertDoesNotThrow(() -> {
            int count = 0;
            for (Map.Entry<String, Integer> e : snapshot) {
                count++;
                // just read the entry – no mutation
                Objects.requireNonNull(e.getKey());
            }
            assertEquals(100, count, "Snapshot should still contain the original 100 entries");
        });
    }

    @ParameterizedTest
    @ValueSource(ints = { 1000 })
    void largeCacheSnapshot(int entryCount) {
        cache.clear();
        for (int i = 0; i < entryCount; i++) {
            cache.put("key" + i, i);
        }
        Set<Map.Entry<String, Integer>> set = cache.entrySet();
        assertEquals(entryCount, set.size(), "Set size must equal number of inserted entries");
        // Spot‑check a few entries
        assertTrue(set.contains(new AbstractMap.SimpleEntry<>("key0", 0)));
        assertTrue(set.contains(new AbstractMap.SimpleEntry<>("key" + (entryCount / 2), entryCount / 2)));
        assertTrue(set.contains(new AbstractMap.SimpleEntry<>("key" + (entryCount - 1), entryCount - 1)));
    }

    @Test
    void entrySetEqualsAndHashCodeConsistency() {
        ThreadedLRUCacheStrategy<String, Integer> cache1 = new ThreadedLRUCacheStrategy<>(10, 50);
        ThreadedLRUCacheStrategy<String, Integer> cache2 = new ThreadedLRUCacheStrategy<>(10, 50);
        cache1.put("a", 1);
        cache1.put("b", 2);
        cache2.put("a", 1);
        cache2.put("b", 2);
        Set<Map.Entry<String, Integer>> set1 = cache1.entrySet();
        Set<Map.Entry<String, Integer>> set2 = cache2.entrySet();
        assertEquals(set1, set2, "Entry sets from identical caches must be equal");
        assertEquals(set1.hashCode(), set2.hashCode(), "Hash codes must match for equal sets");
    }

    @Test
    void entrySetThrowsWhenInternalCacheIsNull() throws Exception {
        // Use reflection to set the private 'cache' field to null
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.entrySet(), "Calling entrySet() when internal cache is null should throw NPE");
    }
}
