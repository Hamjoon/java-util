package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_values_Test {

    private TTLCache<Integer, String> ttlCache;

    /* -------------------------------------------------------------
     * Helper class to instantiate objects without invoking constructors
     * ------------------------------------------------------------- */
    private static class UnsafeAllocator {

        /**
         * Creates an instance of the given class without calling any constructor.
         * This is used only for test purposes to fabricate malformed CacheEntry objects.
         */
        static Object create(Class<?> clazz) throws Exception {
            // Use sun.misc.Unsafe if available; fall back to reflection on default constructor.
            // The code below works on OpenJDK/Oracle JVMs.
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            return unsafe.allocateInstance(clazz);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // TTL of 10 seconds, no LRU, default cleanup interval (60 000 ms)
        ttlCache = new TTLCache<>(10_000, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            // ensure the map is empty for the next test
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache
        TTLCache.shutdown();
    }

    @Test
    void valuesReturnsAllValuesInTheCache() {
        // given
        ttlCache.put(1, "Alpha");
        ttlCache.put(2, "Beta");
        ttlCache.put(3, "Gamma");
        // when
        Collection<String> values = ttlCache.values();
        // then
        assertEquals(3, values.size(), "The collection should contain three values");
        assertTrue(values.contains("Alpha"));
        assertTrue(values.contains("Beta"));
        assertTrue(values.contains("Gamma"));
    }

    @Test
    void valuesOnEmptyCacheReturnsEmptyCollection() {
        // when
        Collection<String> values = ttlCache.values();
        // then
        assertNotNull(values, "values() must never return null");
        assertTrue(values.isEmpty(), "A newly‑created cache should return an empty collection");
    }

    @Test
    void valuesAfterEntriesExpireReturnsEmptyCollection() throws InterruptedException {
        // Use a short TTL and a short cleanup interval so expiration happens quickly
        // 200 ms TTL, cleanup every 100 ms
        ttlCache = new TTLCache<>(200, -1, 100);
        ttlCache.put(1, "One");
        ttlCache.put(2, "Two");
        // Ensure entries are present initially
        assertEquals(2, ttlCache.size());
        assertFalse(ttlCache.values().isEmpty());
        // Wait long enough for the TTL to expire and the background task to purge
        TimeUnit.MILLISECONDS.sleep(350);
        // when
        Collection<String> values = ttlCache.values();
        // then
        assertTrue(values.isEmpty(), "All entries should have been purged, so values() must be empty");
        assertEquals(0, ttlCache.size(), "Cache size should be zero after expiration");
    }

    @Test
    void valuesReflectsCurrentStateAfterRemovals() {
        // given
        ttlCache.put(1, "First");
        ttlCache.put(2, "Second");
        ttlCache.put(3, "Third");
        // when
        // remove one entry
        ttlCache.remove(2);
        Collection<String> values = ttlCache.values();
        // then
        assertEquals(2, values.size());
        assertTrue(values.contains("First"));
        assertTrue(values.contains("Third"));
        assertFalse(values.contains("Second"));
    }

    @Test
    void valuesWhenCacheIsEmptyReturnsEmptyList() {
        Collection<Object> values = ttlCache.values();
        assertTrue(values.isEmpty(), "Empty cache must return an empty collection");
    }

    @Test
    void valuesWithSingleEntryReturnsExactlyThatValue() {
        ttlCache.put(10, "value1");
        Collection<Object> values = ttlCache.values();
        assertEquals(1, values.size());
        assertTrue(values.contains("value1"));
    }

    @Test
    void valuesWithMultipleEntriesContainsAllValuesRegardlessOfOrder() {
        ttlCache.put(1, "v1");
        ttlCache.put(2, "v2");
        ttlCache.put(3, "v3");
        Collection<Object> values = ttlCache.values();
        assertEquals(3, values.size());
        assertTrue(values.containsAll(Arrays.asList("v1", "v2", "v3")));
    }

    @Test
    void valuesIncludesNullWhenNullValueIsStored() {
        ttlCache.put(1, null);
        Collection<Object> values = ttlCache.values();
        assertEquals(1, values.size());
        assertTrue(values.contains(null));
    }

    @Test
    void valuesReturnsIndependentSnapshot() {
        ttlCache.put(1, "a");
        ttlCache.put(2, "b");
        Collection<Object> snapshot = ttlCache.values();
        // modify the snapshot
        ((List<Object>) snapshot).remove("a");
        // cache must stay unchanged
        assertEquals(2, ttlCache.size());
        assertTrue(ttlCache.values().containsAll(Arrays.asList("a", "b")));
    }

    @Test
    void valuesSnapshotDoesNotSeeSubsequentPuts() {
        ttlCache.put(1, "old");
        Collection<Object> snapshot = ttlCache.values();
        ttlCache.put(2, "new");
        // snapshot must not contain the newly added value
        assertFalse(snapshot.contains("new"));
        assertTrue(snapshot.contains("old"));
    }

    @Test
    void valuesReturnsDuplicateValuesForDifferentKeys() {
        String shared = "shared";
        ttlCache.put(1, shared);
        ttlCache.put(2, shared);
        Collection<Object> values = ttlCache.values();
        // Two occurrences of the same object reference
        assertEquals(2, values.size());
        long count = values.stream().filter(v -> v == shared).count();
        assertEquals(2, count, "Both keys should contribute the same value instance");
    }

    @Test
    void valuesHandlesHeterogeneousValueTypes() {
        // Integer
        ttlCache.put(1, 1);
        // String
        ttlCache.put(2, "two");
        // Boolean
        ttlCache.put(3, true);
        Collection<Object> values = ttlCache.values();
        assertEquals(3, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains("two"));
        assertTrue(values.contains(true));
    }

    @Test
    void valuesAfterEntryRemovalDoesNotContainRemovedValue() {
        ttlCache.put(1, "value1");
        ttlCache.put(2, "value2");
        ttlCache.remove(1);
        Collection<Object> values = ttlCache.values();
        assertEquals(1, values.size());
        assertTrue(values.contains("value2"));
        assertFalse(values.contains("value1"));
    }

    @Test
    void valuesWithLargeDataSetReturnsAllElements() {
        int count = 1_000;
        for (int i = 0; i < count; i++) {
            ttlCache.put(i, "v" + i);
        }
        Collection<Object> values = ttlCache.values();
        assertEquals(count, values.size());
        // Spot‑check a few entries
        assertTrue(values.contains("v0"));
        assertTrue(values.contains("v999"));
    }

    @Test
    void valuesReturnsReferenceToMutableObject() {
        StringBuilder sb = new StringBuilder("initial");
        ttlCache.put(1, sb);
        Collection<Object> values = ttlCache.values();
        StringBuilder retrieved = (StringBuilder) values.iterator().next();
        retrieved.append("-changed");
        // The change must be visible through the cache as well
        assertEquals("initial-changed", ttlCache.get(1).toString());
    }

    @Test
    void valuesSnapshotUnderConcurrentModification() throws Exception {
        // Populate with an initial entry
        ttlCache.put(1, "first");
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicReference<Collection<Object>> snapshotRef = new AtomicReference<>();
        Thread reader = new Thread(() -> {
            try {
                // wait until writer is ready
                startLatch.await();
                // take snapshot
                snapshotRef.set(ttlCache.values());
                // signal completion
                doneLatch.countDown();
            } catch (InterruptedException ignored) {
            }
        });
        Thread writer = new Thread(() -> {
            // Signal reader to start, then add a new entry
            startLatch.countDown();
            ttlCache.put(2, "second");
        });
        reader.start();
        writer.start();
        // Wait for the snapshot to be taken
        doneLatch.await(1, TimeUnit.SECONDS);
        Collection<Object> snapshot = snapshotRef.get();
        assertNotNull(snapshot);
        // The snapshot must not contain the entry added after the call started
        assertFalse(snapshot.contains("second"));
        assertTrue(snapshot.contains("first"));
    }

    @Test
    void valuesThrowsNPEWhenInternalCacheMapIsNull() throws Exception {
        // Break the internal cacheMap via reflection
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, null);
        assertThrows(NullPointerException.class, () -> ttlCache.values(), "Calling values() with a null internal map should throw NPE");
    }

    @Test
    void valuesThrowsNPEWhenCacheMapContainsNullEntry() throws Exception {
        // Create a real ConcurrentHashMap with a null entry
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, Object>> brokenMap = new ConcurrentHashMap<>();
        // null entry
        brokenMap.put(1, null);
        // Inject the broken map
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, brokenMap);
        assertThrows(NullPointerException.class, () -> ttlCache.values(), "Iterating over a null CacheEntry should cause NPE");
    }

    @Test
    void valuesThrowsNPEWhenCacheEntryNodeIsNull() throws Exception {
        // Create a CacheEntry with a null node via reflection
        Class<?> entryClass = Class.forName("com.cedarsoftware.util.TTLCache$CacheEntry");
        // helper to bypass constructor
        Object cacheEntry = UnsafeAllocator.create(entryClass);
        // set the 'node' field to null
        Field nodeField = entryClass.getDeclaredField("node");
        nodeField.setAccessible(true);
        nodeField.set(cacheEntry, null);
        // Put the malformed entry into a real map
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, Object>> map = (ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, Object>>) new ConcurrentHashMap<>();
        map.put(1, (TTLCache.CacheEntry<Integer, Object>) cacheEntry);
        // Inject the map
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, map);
        assertThrows(NullPointerException.class, () -> ttlCache.values(), "Accessing entry.node.value when node is null must throw NPE");
    }

    @Test
    void valuesPropagatesRuntimeExceptionFromUnderlyingMap(@Mock ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, Object>> mockMap) throws Exception {
        // Configure mock to throw on values()
        when(mockMap.values()).thenThrow(new RuntimeException("boom"));
        // Inject mock map
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, mockMap);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ttlCache.values(), "RuntimeException from underlying map should propagate");
        assertEquals("boom", ex.getMessage());
    }
}
