package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_keySet_Test {

    private TTLCache<Integer, String> ttlCache;

    class BadHash {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad hash");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadEquals {

        private final int id;

        BadEquals(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("bad equals");
        }
    }

    class CustomKey {

        final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return "CustomKey{id=" + id + '}';
        }
    }

    class BadHash {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad hash");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadEquals {

        private final int id;

        BadEquals(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("bad equals");
        }
    }

    class CustomKey {

        final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public String toString() {
            return "CustomKey{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a long TTL so entries do not expire during the normal tests.
        ttlCache = new TTLCache<>(10_000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that a fresh instance is used for the next test.
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background thread used by TTLCache.
        TTLCache.shutdown();
    }

    /**
     * Sets a private field via reflection.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void keySetContainsAllInsertedKeys() {
        Set<Integer> keys = ttlCache.keySet();
        assertEquals(3, keys.size(), "keySet should contain exactly the three inserted keys");
        assertTrue(keys.contains(1), "keySet should contain key 1");
        assertTrue(keys.contains(2), "keySet should contain key 2");
        assertTrue(keys.contains(3), "keySet should contain key 3");
    }

    @Test
    void keySetIsIndependentFromCacheModification() {
        Set<Integer> keys = ttlCache.keySet();
        // Remove a key from the returned set – this must NOT affect the cache itself.
        assertTrue(keys.remove(2), "Removal from the returned set should succeed");
        assertFalse(keys.contains(2), "Key 2 should no longer be in the returned set");
        // The cache must still contain key 2.
        assertEquals("B", ttlCache.get(2), "Cache must still contain the value for key 2 after the set is modified");
        assertEquals(3, ttlCache.size(), "Cache size must remain unchanged after modifying the returned set");
    }

    @Test
    void keySetReflectsExpiration() throws InterruptedException {
        // Create a cache with a short TTL and a short cleanup interval.
        TTLCache<Integer, String> shortTtlCache = new TTLCache<>(200, -1, 50);
        shortTtlCache.put(10, "X");
        shortTtlCache.put(20, "Y");
        // Verify keys are present initially.
        Set<Integer> initialKeys = shortTtlCache.keySet();
        assertEquals(2, initialKeys.size());
        assertTrue(initialKeys.containsAll(Set.of(10, 20)));
        // Wait for entries to expire.
        Thread.sleep(350);
        // After expiration the keySet should be empty.
        Set<Integer> afterExpiryKeys = shortTtlCache.keySet();
        assertTrue(afterExpiryKeys.isEmpty(), "keySet should be empty after entries have expired");
        // Cleanup the temporary cache.
        shortTtlCache.clear();
    }

    @Test
    void keySetReturnsNewSetEachCall() {
        Set<Integer> firstCall = ttlCache.keySet();
        Set<Integer> secondCall = ttlCache.keySet();
        // The two sets should be equal in content but not be the same instance.
        assertEquals(firstCall, secondCall, "Both calls should return equal sets");
        assertNotSame(firstCall, secondCall, "Each call should return a new Set instance");
    }

    /**
     * Creates a {@link TTLCache.Node} instance with the supplied key.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> Object newNode(K key) {
        try {
            Class<?> nodeClass = Class.forName("com.cedarsoftware.util.TTLCache$Node");
            Object node = nodeClass.getDeclaredConstructor().newInstance();
            Field keyField = nodeClass.getDeclaredField("key");
            keyField.setAccessible(true);
            keyField.set(node, key);
            return node;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link TTLCache.CacheEntry} that wraps the supplied node.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> Object newCacheEntry(Object node) {
        try {
            Class<?> entryClass = Class.forName("com.cedarsoftware.util.TTLCache$CacheEntry");
            Object entry = entryClass.getDeclaredConstructor().newInstance();
            Field nodeField = entryClass.getDeclaredField("node");
            nodeField.setAccessible(true);
            nodeField.set(entry, node);
            return entry;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void keySetEmptyCacheReturnsEmptySet() {
        TTLCache<String, String> emptyCache = new TTLCache<>(10_000, -1);
        Set<String> keys = emptyCache.keySet();
        assertTrue(keys.isEmpty(), "keySet on an empty cache must be empty");
    }

    @Test
    void keySetSingleEntryReturnsThatKey() {
        TTLCache<String, String> cache = new TTLCache<>(10_000, -1);
        cache.put("alpha", "value");
        Set<String> keys = cache.keySet();
        assertEquals(Set.of("alpha"), keys, "keySet should contain the single inserted key");
    }

    @Test
    void keySetMultipleDistinctKeys() {
        TTLCache<String, String> cache = new TTLCache<>(10_000, -1);
        cache.put("one", "1");
        cache.put("two", "2");
        cache.put("three", "3");
        Set<String> keys = cache.keySet();
        assertEquals(Set.of("one", "two", "three"), keys, "keySet must contain all three distinct keys");
    }

    @Test
    void keySetDuplicateKeyObjectsCollapse() throws Exception {
        // Build a custom map with two entries that reference the SAME key object.
        ConcurrentMap<String, Object> customMap = new ConcurrentHashMap<>();
        Object node = newNode("dup");
        Object entry1 = newCacheEntry(node);
        // same underlying key object
        Object entry2 = newCacheEntry(node);
        customMap.put("k1", entry1);
        customMap.put("k2", entry2);
        setPrivateField(ttlCache, "cacheMap", customMap);
        Set<String> keys = ttlCache.keySet();
        assertEquals(Set.of("dup"), keys, "Duplicate keys must be collapsed into a single element");
    }

    @Test
    void keySetContainsNullKey() {
        TTLCache<String, String> cache = new TTLCache<>(10_000, -1);
        cache.put(null, "nullValue");
        Set<String> keys = cache.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(null), "keySet must contain null when a null key is present");
    }

    @Test
    void keySetNodeNullThrowsNPE() throws Exception {
        // Prepare a CacheEntry whose node field is null.
        Class<?> entryClass = Class.forName("com.cedarsoftware.util.TTLCache$CacheEntry");
        // node stays null
        Object badEntry = entryClass.getDeclaredConstructor().newInstance();
        ConcurrentMap<String, Object> customMap = new ConcurrentHashMap<>();
        customMap.put("bad", badEntry);
        setPrivateField(ttlCache, "cacheMap", customMap);
        assertThrows(NullPointerException.class, ttlCache::keySet, "A null node inside a CacheEntry must cause NPE");
    }

    @Test
    void keySetNullCacheMapThrowsNPE() throws Exception {
        setPrivateField(ttlCache, "cacheMap", null);
        assertThrows(NullPointerException.class, ttlCache::keySet, "Calling keySet when cacheMap is null must throw NPE");
    }

    @Test
    void keySetNullEntryInMapThrowsNPE() throws Exception {
        ConcurrentMap<String, Object> customMap = new ConcurrentHashMap<>();
        // value is null
        customMap.put("nullEntry", null);
        setPrivateField(ttlCache, "cacheMap", customMap);
        assertThrows(NullPointerException.class, ttlCache::keySet, "A null CacheEntry value must cause NPE");
    }

    @Test
    void keySetKeyWithFaultyHashCodePropagatesException() throws Exception {
        // Key whose hashCode throws.
        class BadHash {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad hash");
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }
        }
        BadHash badKey = new BadHash();
        // Build entry manually because ttlCache.put would invoke hashCode.
        Object node = newNode(badKey);
        Object entry = newCacheEntry(node);
        ConcurrentMap<BadHash, Object> customMap = new ConcurrentHashMap<>();
        customMap.put(badKey, entry);
        setPrivateField(ttlCache, "cacheMap", customMap);
        RuntimeException ex = assertThrows(RuntimeException.class, ttlCache::keySet, "Exception from hashCode must propagate");
        assertEquals("bad hash", ex.getMessage());
    }

    @Test
    void keySetKeyWithFaultyEqualsPropagatesException() throws Exception {
        // Two distinct keys that are equal according to equals(), but equals throws.
        class BadEquals {

            private final int id;

            BadEquals(int id) {
                this.id = id;
            }

            @Override
            public int hashCode() {
                return id;
            }

            @Override
            public boolean equals(Object o) {
                throw new RuntimeException("bad equals");
            }
        }
        BadEquals key1 = new BadEquals(1);
        // different instance but same hash bucket
        BadEquals key2 = new BadEquals(2);
        // Build two entries.
        Object node1 = newNode(key1);
        Object node2 = newNode(key2);
        Object entry1 = newCacheEntry(node1);
        Object entry2 = newCacheEntry(node2);
        ConcurrentMap<BadEquals, Object> customMap = new ConcurrentHashMap<>();
        customMap.put(key1, entry1);
        customMap.put(key2, entry2);
        setPrivateField(ttlCache, "cacheMap", customMap);
        RuntimeException ex = assertThrows(RuntimeException.class, ttlCache::keySet, "Exception from equals must propagate");
        assertEquals("bad equals", ex.getMessage());
    }

    @Test
    void keySetIteratorThrowsPropagatesException(@Mock ConcurrentMap<Integer, Object> mockMap) {
        // Mock values() to return a collection whose iterator throws.
        Collection<Object> badValues = mock(Collection.class);
        Iterator<Object> badIter = mock(Iterator.class);
        when(badIter.hasNext()).thenReturn(true);
        when(badIter.next()).thenThrow(new RuntimeException("iterator failure"));
        when(badValues.iterator()).thenReturn(badIter);
        when(mockMap.values()).thenReturn(badValues);
        setPrivateField(ttlCache, "cacheMap", mockMap);
        RuntimeException ex = assertThrows(RuntimeException.class, ttlCache::keySet);
        assertEquals("iterator failure", ex.getMessage());
    }

    @Test
    void keySetConcurrentModificationSafety() throws Exception {
        // A cache that we will modify from another thread while keySet is executing.
        TTLCache<Integer, String> concurrentCache = new TTLCache<>(10_000, -1);
        // preload a few keys
        for (int i = 0; i < 5; i++) {
            concurrentCache.put(i, "V" + i);
        }
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // Background thread continuously adds entries.
        executor.submit(() -> {
            try {
                // wait until main thread starts keySet()
                startLatch.await();
                for (int i = 5; i < 15; i++) {
                    concurrentCache.put(i, "V" + i);
                    Thread.sleep(10);
                }
            } catch (InterruptedException ignored) {
            } finally {
                stopLatch.countDown();
            }
        });
        // Trigger background work.
        startLatch.countDown();
        // Call keySet while background modifications happen.
        Set<Integer> keys = concurrentCache.keySet();
        // Ensure no ConcurrentModificationException was thrown and we have at least the
        // originally inserted keys.
        for (int i = 0; i < 5; i++) {
            assertTrue(keys.contains(i), "keySet must contain initially inserted key " + i);
        }
        // Wait for background thread to finish before ending test.
        stopLatch.await();
        executor.shutdownNow();
    }

    @Test
    void keySetMutableKeyReflectsCurrentState() throws Exception {
        // Use a mutable StringBuilder as the key.
        StringBuilder mutableKey = new StringBuilder("original");
        ttlCache.put(mutableKey, "value");
        // Change the content after insertion.
        mutableKey.replace(0, mutableKey.length(), "changed");
        Set<Object> keys = ttlCache.keySet();
        assertTrue(keys.contains("changed"), "keySet should reflect the mutated state of the key object");
    }

    @Test
    void keySetMixedNullAndNonNullKeys() {
        TTLCache<String, String> cache = new TTLCache<>(10_000, -1);
        cache.put(null, "nullVal");
        cache.put("x", "X");
        cache.put("y", "Y");
        Set<String> keys = cache.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.containsAll(Set.of(null, "x", "y")), "keySet must contain null and the two non‑null keys");
    }

    @Test
    void keySetDuplicateKeysViaEqualsCustomKey() throws Exception {
        // Custom key where equality is based on an id field.
        class CustomKey {

            final int id;

            CustomKey(int id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
            }

            @Override
            public int hashCode() {
                return id;
            }

            @Override
            public String toString() {
                return "CustomKey{id=" + id + '}';
            }
        }
        CustomKey keyA = new CustomKey(1);
        // equal to keyA but distinct instance
        CustomKey keyB = new CustomKey(1);
        // Build two entries manually.
        Object nodeA = newNode(keyA);
        Object nodeB = newNode(keyB);
        Object entryA = newCacheEntry(nodeA);
        Object entryB = newCacheEntry(nodeB);
        // Use a map that allows both entries (different map keys).
        ConcurrentMap<Object, Object> customMap = new ConcurrentHashMap<>();
        customMap.put(keyA, entryA);
        customMap.put(keyB, entryB);
        setPrivateField(ttlCache, "cacheMap", customMap);
        Set<CustomKey> keys = ttlCache.keySet();
        assertEquals(1, keys.size(), "Duplicate keys according to equals must be collapsed");
        assertTrue(keys.contains(keyA) || keys.contains(keyB), "The set must contain the representative key");
    }
}
