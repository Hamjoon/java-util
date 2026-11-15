package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_values_Test {

    /**
     * Cache used in the tests.
     */
    private TTLCache<Integer, String> ttlCache;

    /**
     * Create a fresh cache before every test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // TTL of 10 seconds, no size limit, default cleanup interval (60 s)
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    /**
     * Clear the cache after each test so that lingering entries do not affect the next one.
     */
    @AfterEach
    void teardownAfterEach() {
        ttlCache.clear();
    }

    /**
     * Shut down the shared background thread used by all TTLCache instances.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    @Test
    void valuesReturnsAllCurrentValues() {
        ttlCache.put(1, "Alpha");
        ttlCache.put(2, "Beta");
        ttlCache.put(3, "Gamma");
        Collection<String> values = ttlCache.values();
        // The collection should contain exactly the three values, order is not guaranteed
        assertEquals(3, values.size(), "values() should report three elements");
        Set<String> expected = Set.of("Alpha", "Beta", "Gamma");
        assertTrue(values.containsAll(expected), "values() should contain every inserted value");
    }

    @Test
    void valuesReflectsExpiration() throws InterruptedException {
        // Very short TTL and cleanup interval so expiration happens quickly
        ttlCache = new TTLCache<>(200L, -1, 50L);
        ttlCache.put(1, "One");
        ttlCache.put(2, "Two");
        // Ensure they are present initially
        assertEquals(2, ttlCache.values().size());
        // Wait longer than the TTL
        Thread.sleep(300L);
        // After expiration the cache should be empty and values() should reflect that
        Collection<String> valuesAfter = ttlCache.values();
        assertTrue(valuesAfter.isEmpty(), "values() should be empty after entries have expired");
    }

    @Test
    void valuesIsASeparateSnapshot() {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        Collection<String> snapshot = ttlCache.values();
        // Modify the underlying cache
        ttlCache.remove(10);
        ttlCache.put(30, "Z");
        // The snapshot must stay unchanged – it is a copy, not a live view
        assertEquals(2, snapshot.size(), "Snapshot size must stay the same after cache mutation");
        assertTrue(snapshot.contains("X"));
        assertTrue(snapshot.contains("Y"));
        assertFalse(snapshot.contains("Z"));
    }

    @Test
    void valuesOnEmptyCacheReturnsEmptyCollection() {
        Collection<String> empty = ttlCache.values();
        assertNotNull(empty, "values() should never return null");
        assertTrue(empty.isEmpty(), "values() on a newly created cache must be empty");
    }

    @Test
    void valuesWithNullValueIncluded() {
        ttlCache.put(1, null);
        ttlCache.put(2, "present");
        Collection<String> values = ttlCache.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(null));
        assertTrue(values.contains("present"));
    }

    @Test
    void valuesAfterReturnedListMutationDoesNotAffectCache() {
        ttlCache.put(1, "one");
        ttlCache.put(2, "two");
        Collection<String> values = ttlCache.values();
        // The returned collection is a snapshot; mutating it must not affect the cache
        if (values instanceof List) {
            ((List<String>) values).add("bogus");
        }
        Collection<String> afterMutation = ttlCache.values();
        assertEquals(2, afterMutation.size(), "Cache must still contain only the original entries");
        assertFalse(afterMutation.contains("bogus"));
    }

    @Test
    void valuesDuplicateValuesAreAllReturned() {
        ttlCache.put(1, "dup");
        ttlCache.put(2, "dup");
        Collection<String> values = ttlCache.values();
        assertEquals(2, values.size());
        // Both occurrences must be present – we check the frequency
        long count = values.stream().filter("dup"::equals).count();
        assertEquals(2, count, "Both duplicate values should appear");
    }

    @Test
    void valuesReflectRemoval() {
        ttlCache.put(1, "first");
        ttlCache.put(2, "second");
        ttlCache.put(3, "third");
        ttlCache.remove(3);
        Collection<String> values = ttlCache.values();
        assertEquals(2, values.size());
        assertTrue(values.containsAll(List.of("first", "second")));
        assertFalse(values.contains("third"));
    }

    @Test
    void valuesAfterClearIsEmpty() {
        ttlCache.put(1, "a");
        ttlCache.put(2, "b");
        ttlCache.clear();
        Collection<String> values = ttlCache.values();
        assertTrue(values.isEmpty(), "values() must be empty after clear()");
    }

    @Test
    void valuesAfterPutAllPopulatesCorrectly() {
        Map<Integer, String> source = Map.of(1, "a", 2, "b", 3, "c");
        ttlCache.putAll(source);
        Collection<String> values = ttlCache.values();
        assertEquals(3, values.size());
        assertTrue(values.containsAll(source.values()));
    }

    @Test
    void valuesReturnsReferenceToMutableObject() {
        StringBuilder sb = new StringBuilder("original");
        // store the string representation
        ttlCache.put(1, sb.toString());
        // Retrieve the value via values()
        List<String> values = new ArrayList<>(ttlCache.values());
        assertEquals(1, values.size());
        assertEquals("original", values.get(0));
        // Mutate the original StringBuilder – the stored string will not change because
        // the cache stores the immutable String, not the StringBuilder reference.
        // To really test reference semantics we store the mutable object itself.
        TTLCache<Integer, StringBuilder> mutableCache = new TTLCache<>(10_000L, -1);
        mutableCache.put(1, sb);
        List<StringBuilder> mutableValues = new ArrayList<>(mutableCache.values());
        // Mutate after retrieval
        sb.append("changed");
        assertEquals("originalchanged", mutableValues.get(0).toString(), "values() should return the same mutable object reference");
    }

    @Test
    void valuesSuccessiveCallsReturnIndependentSnapshots() {
        ttlCache.put(1, "one");
        ttlCache.put(2, "two");
        List<String> first = new ArrayList<>(ttlCache.values());
        List<String> second = new ArrayList<>(ttlCache.values());
        assertNotSame(first, second, "Each call to values() must return a new collection instance");
        // Mutate the first snapshot
        first.add("extra");
        assertFalse(second.contains("extra"), "Second snapshot must remain unchanged after mutating the first");
    }

    @Test
    void valuesHandlesLargeNumberOfEntries() {
        int count = 1_000;
        for (int i = 0; i < count; i++) {
            ttlCache.put(i, "v" + i);
        }
        Collection<String> values = ttlCache.values();
        assertEquals(count, values.size(), "values() should contain exactly 1,000 elements");
        // Spot‑check a few entries
        assertTrue(values.contains("v0"));
        assertTrue(values.contains("v" + (count - 1)));
    }

    @Test
    void valuesThrowsNPEWhenCacheMapIsNull() throws Exception {
        // Use reflection to set the internal cacheMap to null
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, null);
        assertThrows(NullPointerException.class, () -> ttlCache.values(), "Calling values() when cacheMap is null should throw NPE");
    }

    @Test
    void valuesThrowsNPEWhenCacheMapContainsNullEntry() throws Exception {
        // Obtain the real map, then inject a null entry
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> map = (ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>>) mapField.get(ttlCache);
        // Insert a null entry directly (bypassing the public API)
        map.put(999, null);
        assertThrows(NullPointerException.class, () -> ttlCache.values(), "Iterating over a null CacheEntry should cause NPE");
    }

    @Test
    void valuesThrowsNPEWhenEntryHasNullNode() throws Exception {
        // Create a mock ConcurrentMap that returns a CacheEntry with a null node
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, TTLCache.CacheEntry<Integer, String>> mockMap = mock(ConcurrentMap.class);
        // Build a CacheEntry instance via Unsafe (bypassing constructors)
        // This works even if CacheEntry has no no‑arg constructor.
        Class<?> entryClass = Class.forName("com.cedarsoftware.util.TTLCache$CacheEntry");
        // Obtain Unsafe instance
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        Object entryInstance = unsafe.allocateInstance(entryClass);
        // Set the 'node' field to null
        Field nodeField = entryClass.getDeclaredField("node");
        nodeField.setAccessible(true);
        nodeField.set(entryInstance, null);
        // Mock the map's values() collection
        Collection<?> entryColl = Collections.singleton(entryInstance);
        when(mockMap.values()).thenReturn(entryColl);
        // Replace the internal map with our mock
        Field mapField = TTLCache.class.getDeclaredField("cacheMap");
        mapField.setAccessible(true);
        mapField.set(ttlCache, mockMap);
        assertThrows(NullPointerException.class, () -> ttlCache.values(), "Accessing entry.node when node is null should cause NPE");
    }
}
